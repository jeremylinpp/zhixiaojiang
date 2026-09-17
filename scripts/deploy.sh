#!/usr/bin/env bash
# 智小匠本机一键上线：本地构建 → 打包 → 上传 → 服务器构建镜像并重启 → 健康检查。
#
# 为什么用本机脚本而不是纯 GitHub 流水线（2026-09-16 实测）：
#   GitHub 云端运行器 → 服务器：0.83 MB/s 且反复停顿（56 MB 发布包约一小时）
#   服务器 → GitHub：约 21 KB/s，连并发多连接也跑不完
#   本机 → 服务器：1.6 MB/s（56 MB 约 35 秒）
#   本机 ↔ GitHub：快但不稳定（TLS 握手间歇失败，会让自托管运行器的作业会话中断）
# 因此构建与质量门禁交给 GitHub 云端 CI，上线这一步在本机执行更可靠。
#
# 用法：
#   scripts/deploy.sh                 # 跑后端测试 + 前端类型检查，然后上线
#   scripts/deploy.sh --skip-tests    # 跳过测试（仅本地快速发布）
#   scripts/deploy.sh --tag v1.2.0    # 本次构建使用指定镜像标签（默认取当前提交短 SHA）
#   scripts/deploy.sh --rollback <标签>  # 回滚：不构建不传包，直接在服务器上用旧镜像重启
#   scripts/deploy.sh --online        # 首次在新机器上构建时，允许联网拉取 Maven 依赖
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_HOST="${DEPLOY_HOST:-117.72.13.19}"
DEPLOY_USER="${DEPLOY_USER:-root}"
DEPLOY_DIR="/opt/zhixiaojiang"
STAGING="$(mktemp -d)"
SKIP_TESTS=0
TAG=""
# 默认离线构建：本机 Maven 仓库已是热的，而 Maven Central 在该网络下极慢。
# 首次在新机器上构建时用 --online 拉取依赖。
MVN_MODE=(-o)
ROLLBACK=0

while [ $# -gt 0 ]; do
  case "$1" in
    --skip-tests) SKIP_TESTS=1 ;;
    --online) MVN_MODE=() ;;
    --rollback)
      shift
      ROLLBACK=1
      TAG="${1:-}"
      [ -n "${TAG}" ] || { echo "--rollback 需要指定镜像标签" >&2; exit 2; }
      ;;
    --tag) shift; TAG="${1:-}" ;;
    *) echo "未知参数：$1" >&2; exit 2 ;;
  esac
  shift
done

[ -n "${TAG}" ] || TAG="$(git -C "${REPO_ROOT}" rev-parse --short HEAD 2>/dev/null || date +%Y%m%d%H%M%S)"
SSH_OPTS=(-o BatchMode=yes -o ConnectTimeout=10)

cleanup() { rm -rf "${STAGING}"; }
trap cleanup EXIT

step() { printf '\n=== %s ===\n' "$1"; }

step "0/6 预检：服务器连通性与运行配置"
ssh "${SSH_OPTS[@]}" "${DEPLOY_USER}@${DEPLOY_HOST}" \
  'test -f /opt/zhixiaojiang/app.env && echo "  服务器可达，app.env 已就绪 ✅" || { echo "  缺少 /opt/zhixiaojiang/app.env"; exit 1; }'

if [ "${ROLLBACK}" = "1" ]; then
  step "回滚到镜像标签 ${TAG}（不构建、不上传）"
  ssh "${SSH_OPTS[@]}" "${DEPLOY_USER}@${DEPLOY_HOST}" "set -e
    cd ${DEPLOY_DIR}
    docker image inspect zhixiaojiang-app:${TAG} >/dev/null 2>&1 || {
      echo '  服务器上没有该标签的镜像，当前可用标签：'
      docker images --format '  {{.Repository}}:{{.Tag}}' | grep zhixiaojiang | sort -u
      exit 1
    }
    printf 'IMAGE_TAG=%s\n' '${TAG}' > .env
    chmod 600 .env
    IMAGE_TAG=${TAG} docker compose up -d
    docker compose ps --format 'table {{.Name}}\t{{.Status}}'"

  step "回滚后健康检查"
  for attempt in $(seq 1 12); do
    if ssh "${SSH_OPTS[@]}" "${DEPLOY_USER}@${DEPLOY_HOST}" 'curl -fsS -m 5 http://127.0.0.1/api/v1/auth/csrf >/dev/null'; then
      echo "  回滚完成，健康检查通过 ✅（当前镜像标签：${TAG}）"
      echo "  站点：http://${DEPLOY_HOST}/"
      exit 0
    fi
    echo "  第 ${attempt} 次探测未就绪，等待 5 秒…"
    sleep 5
  done
  echo "回滚后健康检查失败，最近日志："
  ssh "${SSH_OPTS[@]}" "${DEPLOY_USER}@${DEPLOY_HOST}" "cd ${DEPLOY_DIR} && docker compose logs --tail=50 app"
  exit 1
fi

step "1/6 构建后端 jar（镜像标签 ${TAG}）"
cd "${REPO_ROOT}/zhixiaojiang-server"
if [ "${SKIP_TESTS}" = "1" ]; then
  echo "  已跳过测试（--skip-tests）"
  mvn "${MVN_MODE[@]}" -B -ntp -q -DskipTests clean package
else
  mvn "${MVN_MODE[@]}" -B -ntp -q clean package
fi
JAR="$(ls -t target/zhixiaojiang-server-*.jar | grep -v "\.original$" | head -1)"
echo "  产物：${JAR}（$(du -h "${JAR}" | cut -f1)）"

step "2/6 构建前端"
cd "${REPO_ROOT}/zhixiaojiang-web"
if [ ! -d node_modules ]; then npm ci; fi
npm run build
echo "  产物：dist（$(du -sh dist | cut -f1)）"

step "3/6 组装发布包"
mkdir -p "${STAGING}/build/server" "${STAGING}/build/web"
cp "${REPO_ROOT}/zhixiaojiang-server/${JAR}" "${STAGING}/build/server/app.jar"
cp "${REPO_ROOT}/deploy/Dockerfile.server" "${STAGING}/build/server/Dockerfile"
cp -R "${REPO_ROOT}/zhixiaojiang-web/dist" "${STAGING}/build/web/dist"
cp "${REPO_ROOT}/deploy/Dockerfile.web" "${STAGING}/build/web/Dockerfile"
cp "${REPO_ROOT}/deploy/nginx.conf" "${STAGING}/build/web/nginx.conf"
cp "${REPO_ROOT}/deploy/docker-compose.yml" "${STAGING}/docker-compose.yml"
cp "${REPO_ROOT}/deploy/app.env.example" "${STAGING}/app.env.example"
printf 'IMAGE_TAG=%s\n' "${TAG}" > "${STAGING}/.env"
echo "  发布包大小：$(du -sh "${STAGING}" | cut -f1)"

step "4/6 上传到服务器（分块 + 重试）"
# 实测跨境/家宽链路会在长传输中途被重置（一次 9 分钟后断开、全部重来）。
# 因此先压成一个包、切成小块逐个上传，单块失败只重传该块。
ARCHIVE="${STAGING}/release.tar.gz"
COPYFILE_DISABLE=1 tar --no-xattrs -czf "${ARCHIVE}" -C "${STAGING}" build docker-compose.yml app.env.example .env 2>/dev/null \
  || COPYFILE_DISABLE=1 tar -czf "${ARCHIVE}" -C "${STAGING}" build docker-compose.yml app.env.example .env
ARCHIVE_SIZE=$(stat -f %z "${ARCHIVE}" 2>/dev/null || stat -c %s "${ARCHIVE}")
CHUNK_MB=8
split -b "${CHUNK_MB}m" -a 3 "${ARCHIVE}" "${STAGING}/part."
PARTS=$(ls "${STAGING}"/part.* | wc -l | tr -d ' ')
echo "  发布包 $(echo "scale=1; ${ARCHIVE_SIZE}/1048576" | bc) MB，切成 ${PARTS} 块（每块 ${CHUNK_MB} MB）"

ssh "${SSH_OPTS[@]}" "${DEPLOY_USER}@${DEPLOY_HOST}" "rm -rf ${DEPLOY_DIR}/build ${DEPLOY_DIR}/upload && mkdir -p ${DEPLOY_DIR}/upload"
for part in "${STAGING}"/part.*; do
  name=$(basename "${part}")
  for attempt in 1 2 3 4 5; do
    if scp "${SSH_OPTS[@]}" -q "${part}" "${DEPLOY_USER}@${DEPLOY_HOST}:${DEPLOY_DIR}/upload/${name}"; then
      printf '    %s 上传成功\n' "${name}"
      break
    fi
    if [ "${attempt}" = "5" ]; then echo "    ${name} 连续 5 次失败，中止" >&2; exit 1; fi
    echo "    ${name} 第 ${attempt} 次失败，5 秒后重试…"
    sleep 5
  done
done

ssh "${SSH_OPTS[@]}" "${DEPLOY_USER}@${DEPLOY_HOST}" "set -e
  cd ${DEPLOY_DIR}/upload
  cat part.* > ../release.tar.gz
  cd ${DEPLOY_DIR}
  tar -xzf release.tar.gz
  rm -rf upload release.tar.gz
  echo \"  已在服务器解包\""

step "5/6 服务器构建镜像并重启容器"
ssh "${SSH_OPTS[@]}" "${DEPLOY_USER}@${DEPLOY_HOST}" "set -e
  cd ${DEPLOY_DIR}
  head -c 2 build/server/app.jar | grep -q PK || { echo '  app.jar 不是有效 jar，上传可能被截断'; exit 1; }
  size=\$(stat -c %s build/server/app.jar)
  [ \"\$size\" -ge 20000000 ] || { echo \"  app.jar 体积异常：\$size 字节\"; exit 1; }
  test -f build/web/dist/index.html || { echo '  缺少前端 index.html'; exit 1; }
  echo \"  发布包校验通过：app.jar=\$size 字节\"
  IMAGE_TAG=${TAG} docker compose up -d --build
  docker compose ps --format 'table {{.Name}}\t{{.Status}}'"

step "6/6 健康检查"
for attempt in $(seq 1 12); do
  if ssh "${SSH_OPTS[@]}" "${DEPLOY_USER}@${DEPLOY_HOST}" 'curl -fsS -m 5 http://127.0.0.1/api/v1/auth/csrf >/dev/null'; then
    echo "  第 ${attempt} 次探测：应用已就绪 ✅"
    echo
    echo "上线完成："
    echo "  站点：http://${DEPLOY_HOST}/"
    echo "  镜像标签：${TAG}"
    echo "  回滚：scripts/deploy.sh --rollback <上一个标签>（复用服务器上已构建的镜像）"
    exit 0
  fi
  echo "  第 ${attempt} 次探测未就绪，等待 5 秒…"
  sleep 5
done
echo "健康检查失败，最近日志："
ssh "${SSH_OPTS[@]}" "${DEPLOY_USER}@${DEPLOY_HOST}" "cd ${DEPLOY_DIR} && docker compose logs --tail=50 app"
exit 1

# 上线与回滚手册

本文说明智小匠的自动化上线流程：GitHub Actions 在云端构建与测试，再把发布包传到服务器重启容器。
尚未执行上线；下面「一次性准备」完成前，`Deploy` 流水线不会改动线上环境。

## 两种上线方式（按当前网络条件实测选择）

| 方式 | 命令 | 实测耗时 | 适用 |
| --- | --- | --- | --- |
| **本机一键脚本（推荐）** | `scripts/deploy.sh` | **1 分 11 秒** | 日常上线与回滚；只用已测通的本机→服务器通道（1.6 MB/s） |
| GitHub Deploy 流水线 | Actions → Deploy → Run workflow | 取决于链路 | 网络状况好时使用；云端构建 + 本机自托管运行器上传 |
| （曾评估）镜像仓库拉取 | — | — | 需要国内仓库账号，暂未采用 |

链路实测（2026-09-16，决定了上面的取舍）：

| 链路 | 速度 | 结论 |
| --- | --- | --- |
| GitHub 云端运行器 → 服务器 | 0.83 MB/s 且反复停顿 | ❌ 56 MB 发布包约一小时 |
| 服务器 → GitHub | 约 21 KB/s，并发也跑不完 | ❌ 服务器主动拉取不可行 |
| 本机 → 服务器 | 1.6 MB/s | ✅ 56 MB 约 35 秒 |
| GitHub → 本机 | 6.2 MB/s，但 TLS 握手间歇失败 | ⚠️ 自托管运行器的作业会话会被中断 |

因此：**构建与质量门禁交给 GitHub 云端 CI**（`CI` 流水线，稳定），**上线用本机脚本**。

## 本机一键上线

```sh
scripts/deploy.sh                 # 跑后端测试 + 前端构建后上线
scripts/deploy.sh --skip-tests    # 跳过测试，快速发布
scripts/deploy.sh --tag v1.2.0    # 指定镜像标签（默认取当前提交短 SHA，回滚时用旧标签）
scripts/deploy.sh --online        # 新机器首次构建时允许联网拉取 Maven 依赖
```

脚本步骤：预检服务器与 `app.env` → 本地构建 jar 与前端 → 组装发布包（56 MB）→
**切成 8 MB 分块逐块上传，单块失败自动重试**（长传输会被链路重置，整包上传会前功尽弃）→
服务器校验 `app.jar` 完整性 → `docker compose up -d --build` → 内网健康检查。
失败时打印应用最近 50 行日志。

## 上线形态

```text
GitHub Actions（ubuntu-latest）
  ├─ backend  : mvn verify（隔离 H2 测试）→ 可执行 jar
  ├─ frontend : npm ci + npm run build（类型检查）→ dist（同源 /api）
  └─ images   : 构建两个镜像并推送到国内仓库（阿里云 ACR 等）
  └─ deploy   : 只上传编排文件与 .env（几 KB）→ 服务器 docker compose pull → 重启 → 内网健康检查
                     │
        ┌────────────┴─────────────────────────────────────────┐
        │ 京东服务器 117.72.13.19                              │
        │   /opt/zhixiaojiang                                  │
        │     ├─ docker-compose.yml      流水线更新             │
        │     ├─ app.env                 仅服务器保留（600）    │
        │     ├─ build/server/app.jar    流水线更新             │
        │     └─ build/web/dist          流水线更新             │
        │   容器（zhixiaojiang 网络）                            │
        │     zhixiaojiang-app  ── 内网 ──> zhixiaojiang-mysql   │
        │                        └─────────> zhixiaojiang-redis │
        │     zhixiaojiang-web  nginx: 静态页面 + /api 反代      │
        └──────────────────────────────────────────────────────┘
```

为什么走镜像仓库而不是直接传文件：实测 GitHub 运行器到服务器的跨境上行只有 ~0.83 MB/s 且反复停顿
（56 MB 发布包需要约一小时），而国内仓库被服务器拉取是内网级速度。因此构建产物先变成镜像进仓库，
部署只传几 KB。

要点：

- **前端与后端同源**：nginx 托管静态文件并把 `/api` 反代到后端容器。浏览器只访问一个地址，
  因此不需要 CORS 白名单，HttpOnly 会话 Cookie 也不会被跨站策略拦截（设计文档里的 CORS 配置只服务于本机开发）。
- **数据库与 Redis 走 Docker 内网**：容器名 `zhixiaojiang-mysql` / `zhixiaojiang-redis`，不经过公网，
  也不依赖 3306/6379 的本机 IP 白名单（该白名单继续保护「从公网直连数据库」的场景）。
- **数据不会被重新初始化**：compose 固定 `DB_INIT_MODE=never`，上线只替换应用与前端，不动数据卷。
- **口令只在服务器上**：`/opt/zhixiaojiang/app.env`（600）保存 `DB_PASSWORD`、`REDIS_PASSWORD`、
  `JWT_SECRET` 与可选的 `AI_*`；GitHub 里只放部署用的 SSH 私钥，不含任何数据库凭据。
- **JWT 密钥跨发布保持一致**：否则每次上线都会让所有人重新登录。

## 一次性准备（在服务器与 GitHub 各做一次）

### 1. 生成应用环境文件

```sh
ssh root@117.72.13.19
cd /opt/zhixiaojiang
umask 077
# 数据库与 Redis 口令取自迁移时已配置的容器（不要重新生成，必须与容器一致）
DB_PW=$(grep -oP '(?<=MYSQL_PASSWORD=).*' /opt/zhixiaojiang/mysql.env | head -1)
cat > app.env <<EOF
DB_PASSWORD=${DB_PW}
REDIS_PASSWORD=<与 redis 容器一致的密码>
JWT_SECRET=$(openssl rand -base64 48)
EOF
chmod 600 app.env
```

`AI_BASE_URL` / `AI_MODEL` / `AI_API_KEY` 不填时，助手页自动降级为规则模板，功能不受影响。

### 1.1 自托管运行器（可选，仅流水线方式需要）

流水线方式需要本机常驻一个 GitHub 运行器，已安装为**用户级 LaunchAgent**（无需 sudo）：

```sh
cd ~/github-runner-zhixiaojiang
./svc.sh status      # 查看状态
./svc.sh stop        # 暂停（本机脚本方式不受影响）
./svc.sh start       # 恢复
./svc.sh uninstall   # 卸载（并可在仓库 Settings → Actions → Runners 删除）
```

该运行器与用户共用系统账户，因此流水线把部署私钥写入 `RUNNER_TEMP` 临时目录并用独立的
`-i` 参数调用 SSH，**不会覆盖你自己的 `~/.ssh`**。

### 2. 配置部署专用 SSH 密钥

```sh
# 在本机生成一次性部署密钥（不要复用个人密钥）
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/zhixiaojiang_deploy -N ''
# 公钥加入服务器
ssh root@117.72.13.19 'cat >> /root/.ssh/authorized_keys' < ~/.ssh/zhixiaojiang_deploy.pub
```

在仓库 `Settings → Secrets and variables → Actions` 添加：

| 类型 | 名称 | 值 |
| --- | --- | --- |
| Secret | `DEPLOY_SSH_KEY` | 上面私钥的完整内容 |
| Variable | `DEPLOY_HOST` | `117.72.13.19` |
| Variable | `DEPLOY_USER` | `root`（如需更小权限，可建专用用户并加入 docker 组） |

### 3. 准备镜像仓库（国内，推荐阿里云 ACR）

1. 在容器镜像服务控制台创建**命名空间**（例如 `zhixiaojiang`）与两个**私有仓库**：
   `zhixiaojiang-app`、`zhixiaojiang-web`；
2. 设置**访问凭证**（固定密码），并在仓库 `Settings → Secrets and variables` 中添加：

| 类型 | 名称 | 示例 |
| --- | --- | --- |
| Variable | `REGISTRY` | `registry.cn-hangzhou.aliyuncs.com` |
| Variable | `REGISTRY_NAMESPACE` | `zhixiaojiang` |
| Secret | `REGISTRY_USERNAME` | ACR 用户名 |
| Secret | `REGISTRY_PASSWORD` | ACR 访问凭证密码 |

3. 在服务器上登录一次，使拉取私有镜像可用（凭据保存在 root 的 docker 配置中）：

```sh
ssh root@117.72.13.19
docker login registry.cn-hangzhou.aliyuncs.com -u <用户名>
```

### 4. 放通访问端口

服务器的 `DOCKER-USER` 规则目前只限制 3306/6379，80 端口默认可用；还需在**京东云安全组**放通 80。

- 若只想自己访问：安全组仅放行你的公网出口 IP，做法与数据库端口一致；
- 线上站点已对公网开放：**2026-09-16 已轮换教师口令**（原演示口令 `teacher/password` 已作废，
  新口令存于服务器 `/opt/zhixiaojiang/teacher-credential.txt`，仅 root 可读），
  并同时轮换 `JWT_SECRET` 使所有旧会话失效；登录页不再展示任何口令提示。
- 后续如需 HTTPS：可用 1Panel 签发证书并在 nginx 配置里监听 443，
  然后把 compose 里的 `COOKIE_SECURE` 改为 `"true"`。

## 日常发布与回滚

```text
GitHub → Actions → Deploy → Run workflow
  confirm   : 输入 deploy
  image_tag : 留空 = 构建并部署当前提交；填上一次成功的 SHA = 回滚
```

部署过程：构建 → 测试 → 推送镜像（`:SHA` 与 `:latest`）→ 上传编排文件与 `.env` →
服务器 `docker compose pull && docker compose up -d` → 内网健康检查
（服务器上探测 `http://127.0.0.1/api/v1/auth/csrf`，最多重试 12 次；服务器未装 rsync，故用 tar over ssh）。失败时流水线打印 app 容器最近 50 行日志。

服务器侧常用命令：

```sh
cd /opt/zhixiaojiang
docker compose ps                     # 容器状态与健康
docker compose logs -f --tail=100 app # 后端日志
docker compose logs -f --tail=100 web # nginx 日志
IMAGE_TAG=<旧SHA> docker compose up -d   # 手动回滚到已存在的旧镜像
docker images | grep zhixiaojiang      # 本地镜像标签
cat .env                              # 当前生效的镜像地址与标签
```

自动上线：确认稳定后，把 `.github/workflows/deploy.yml` 里 `push` 触发器取消注释，
即可改为「合入 main 自动上线」。同一时刻只允许一个部署在跑（`concurrency: deploy-production`）。

## 排障

| 现象 | 排查方向 |
| --- | --- |
| 健康检查超时 | `docker compose logs app`；确认 `app.env` 的 `DB_PASSWORD` 与 MySQL 容器一致 |
| 页面能开但接口 502 | app 容器未就绪或崩溃，看 app 日志；确认两者在同一 `zhixiaojiang` 网络 |
| 登录后立刻失效 | `JWT_SECRET` 是否在多次发布间被改动 |
| 时间显示偏差 | 容器 `TZ`、JDBC `connectionTimeZone` 与数据库时区需一致（当前均按东八区） |
| 部署未触发 | `DEPLOY_*` 变量/密钥是否配置；`confirm` 是否输入 `deploy` |

## 安全基线（2026-09-16 已落实）

| 项 | 状态 |
| --- | --- |
| SSH 密码认证 | 已关闭（`PasswordAuthentication no`，仅密钥登录；root 用密钥、mysql 账号已锁定） |
| 数据库/Redis 端口 | 由 `DOCKER-USER` 白名单限制为当前公网出口 IP |
| 应用口令 | 已轮换，旧演示口令作废；凭据文件 600 仅 root 可读 |
| 会话失效 | 轮换 `JWT_SECRET` + 重启应用容器，旧会话立即失效 |
| 接口鉴权 | 全部业务接口需登录；越权访问统一返回 404（有隔离测试覆盖） |
| 敏感信息 | 口令只存服务器 `app.env`（600）；仓库不含任何口令；AI 送模型数据不含身份信息 |

## 尚未包含

- 未做数据库迁移工具（当前表结构由 `schema.sql` 维护，且线上库禁止重新初始化）；
- 未做多实例与灰度发布（单实例，回滚依赖仓库中的旧镜像标签）；
- 未接入监控告警（仅容器 `restart: unless-stopped` 与健康检查）；
- 未做镜像瘦身（应用镜像含 294 MB 的 JRE 基础镜像，可换更小的基础镜像缩短推送时间）。

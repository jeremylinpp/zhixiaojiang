# 上线与回滚手册

本文说明智小匠的自动化上线流程：GitHub Actions 在云端构建与测试，再把发布包传到服务器重启容器。
尚未执行上线；下面「一次性准备」完成前，`Deploy` 流水线不会改动线上环境。

## 上线形态

```text
GitHub Actions（ubuntu-latest）
  ├─ backend  : mvn verify（隔离 H2 测试）→ 可执行 jar
  ├─ frontend : npm ci + npm run build（类型检查）→ dist（同源 /api）
  └─ deploy   : rsync 发布包 → ssh 执行 docker compose up -d --build → 内网健康检查
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
DB_PW=$(grep -oP '(?<=MYSQL_ROOT_PASSWORD=).*' /opt/zhixiaojiang/mysql.env | head -1)
cat > app.env <<EOF
DB_PASSWORD=${DB_PW}
REDIS_PASSWORD=<与 redis 容器一致的密码>
JWT_SECRET=$(openssl rand -base64 48)
EOF
chmod 600 app.env
```

`AI_BASE_URL` / `AI_MODEL` / `AI_API_KEY` 不填时，助手页自动降级为规则模板，功能不受影响。

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

### 3. 放通访问端口

服务器的 `DOCKER-USER` 规则目前只限制 3306/6379，80 端口默认可用；还需在**京东云安全组**放通 80。

- 若只想自己访问：安全组仅放行你的公网出口 IP，做法与数据库端口一致；
- 若要公开访问：建议先改掉演示口令 `teacher/password`，并接上 HTTPS（1Panel 可签发证书；
  启用 HTTPS 后把 compose 里的 `COOKIE_SECURE` 改为 `"true"`）。

## 日常发布与回滚

```text
GitHub → Actions → Deploy → Run workflow
  confirm   : 输入 deploy
  image_tag : 留空 = 构建并部署当前提交；填上一次成功的 SHA = 回滚
```

部署过程：构建 → 测试 → 上传发布包 → `docker compose up -d --build` → 内网健康检查
（探测 `http://127.0.0.1/api/v1/auth/csrf`，最多重试 12 次）。失败时流水线打印 app 容器最近 50 行日志。

服务器侧常用命令：

```sh
cd /opt/zhixiaojiang
docker compose ps                     # 容器状态与健康
docker compose logs -f --tail=100 app # 后端日志
docker compose logs -f --tail=100 web # nginx 日志
IMAGE_TAG=<旧SHA> docker compose up -d   # 手动回滚到已存在的旧镜像
docker images | grep zhixiaojiang      # 可用镜像标签（按提交 SHA）
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

## 尚未包含

- 未做数据库迁移工具（当前表结构由 `schema.sql` 维护，且线上库禁止重新初始化）；
- 未做镜像仓库与多实例发布（当前在服务器本地构建，单实例，可用旧镜像标签回滚）；
- 未接入监控告警（仅容器 `restart: unless-stopped` 与健康检查）。

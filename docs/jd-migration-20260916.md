# 智小匠京东数据库迁移记录

日期：2026-09-16。迁移范围仅为智小匠 MySQL、Redis 及本机后端连接；未发布公网网站。

## 当前运行方式

| 项目 | 位置 |
| --- | --- |
| 前端 | 本机 `http://127.0.0.1:5173/` |
| 后端 | 本机 `http://127.0.0.1:8081/` |
| MySQL 8.4.9 | 京东 `117.72.13.19:3306`，容器 `zhixiaojiang-mysql` |
| Redis 8.8.0 | 京东 `117.72.13.19:6379`，容器 `zhixiaojiang-redis` |
| SSH 管理 | `root@117.72.13.19`，使用本机现有认证方式 |

京东原有比赛平台的 MySQL 5.7、Redis 4.0 及其他容器未被替换、迁移或清空。新服务位于独立 `zhixiaojiang` Docker 网络，使用独立持久卷 `zhixiaojiang-mysql-data`、`zhixiaojiang-redis-data`。MySQL 容器内存上限 1 GiB，Redis 160 MiB；Redis 开启 AOF。

## 数据与校验

- 暂停本机后端写入后，从 Oracle 导出全部 22 张 InnoDB 表，导入京东空库，保留字符集、排序规则、中文内容、原始记录 ID 和审计数据。
- 完整规范化 SQL 导出在导入后再次生成，源、目标 SHA-256 一致：`c4a4b53bb2fa852609ff17f64afce468238592d6b79b8f52297046bbb292138a`。
- 迁移时共 42 名学生。未重新执行 `data.sql`，初始化配置仍为 `never`。
- Oracle 中 `zhixiaojiang:` 前缀键数为 0；未扫描其他前缀内容，未执行 `FLUSHDB` 或 `FLUSHALL`。
- 本机原 Redis 密码未能通过 Oracle 认证；已用服务器实际配置核验源数据，并为京东设置独立随机密码。
- JWT 签名密钥已轮换，旧登录需重新登录，避免原进程内存中的撤销状态在重启后丢失而恢复旧会话。
- 9 项隔离后端测试全部通过。实际京东环境验证登录、教师信息、学生列表、驾驶舱和积分查询成功；退出后复用原 Cookie 请求返回 401。
- 已直接确认退出撤销标记写入京东 Redis 且带有效期；后端的实际 MySQL、Redis 连接均指向京东。前端页面与前端 API 代理均返回 HTTP 200。

单次接口耗时对照（不是性能基准，网络及首次加载会影响结果）：

| 接口 | Oracle 切换前 | 京东切换后 |
| --- | ---: | ---: |
| 学生列表 | 7087 ms | 182 ms |
| 班级驾驶舱 | 8069 ms | 337 ms |
| 学生积分 | 7450 ms | 221 ms |

## 配置与访问限制

- 应用账号 `zhixiaojiang` 仅有 `zhixiaojiang.*` 的 SELECT、INSERT、UPDATE、DELETE 权限。
- 迁移账号 `zhixiaojiang_migrate@localhost` 仅管理该库；MySQL root 不用于应用运行。
- 本机密码和 JWT 位于被忽略的 `zhixiaojiang-server/src/main/resources/application-local.yml`，权限为 600。不要分享或提交这个文件、构建产物或 `.run/` 目录。
- 京东部署目录为 `/opt/zhixiaojiang`，仅 root 可遍历；MySQL 凭据文件为 `mysql.env`，Redis 配置为 `redis-config/redis.conf`。此文档不记录口令。
- 本轮白名单为 `101.204.76.188`，这是部署时 SSH 连接实际来源。仅新数据库端口 3306、6379 的外部转发受专用规则限制；SSH 和其他应用端口未修改。
- 专用规则脚本：`/opt/zhixiaojiang/firewall.sh`；开机服务：`zhixiaojiang-firewall.service`，配置为在 Docker 启动前执行。未重启服务器测试开机顺序。
- 更换网络或公网 IP 后，先核对新来源，再定向更新该脚本及原白名单规则；不要清空整机防火墙或改成全网开放。仅重跑脚本不会删除旧规则，更新时必须同时处理旧规则。
- 当前是公网直连，并未新增 Redis TLS。IP 白名单不等同于传输加密；涉及真实敏感学生数据前，应另行配置 TLS 或私网连接。

## 备份与回退

本机受保护备份目录：`.run/jd-migration-20260916/`（已被忽略）。其中包含：

- `zhixiaojiang.sql`：Oracle 切换时完整备份。
- `zhixiaojiang.target.sql`：导入后用于校验的京东导出。
- `redis-prefix.json`：仅智小匠前缀的快照。
- `application.before.yml`、`application-local.before.yml`：原连接配置及本机密钥备份。
- `secrets.json`：本轮数据库管理凭据，仅限本机保管，不应分享。

Oracle 原数据库与原服务仍保留，没有删除或停用。Oracle 保留的是切换时的数据，不会自动收到京东的新写入。

回退不能只改 IP：先停止本机后端写入，备份京东最新数据；若切换后已有新增或修改，需核对并同步这些增量后再回退，否则会丢失新数据。回退时应重新核验 Oracle Redis 实际凭据，不要直接恢复已知错误的旧 Redis 密码；重新生成 JWT 签名密钥并要求重新登录。恢复连接后再次验证数据量、登录、退出和业务查询。不要删除京东持久卷。

镜像下载曾受京东外网访问限制，最终将官方 amd64 MySQL 镜像通过 SSH 从 Oracle 中转到京东；Oracle 原 ARM 镜像标签保持不变，仅额外缓存了一份 amd64 镜像。官方 amd64 清单摘要为 `sha256:14690274f4967ba207939c97dd79a8d6604f1d532218ea6be69c0001ddd2d45f`。

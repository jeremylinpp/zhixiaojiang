# 智小匠上线文档

**上线就是在本机跑一条命令**：`scripts/deploy.sh`（约 1-2 分钟）。回滚约 30 秒。

- 站点：<http://117.72.13.19/>　服务器：京东 `117.72.13.19`
- 本文命令都在本机 `~/IdeaProjects/zhixiaojiang` 目录执行；服务器命令会写明 `ssh root@117.72.13.19`
- 平时只需要看第 1～3 节；附录是背景，不用看

---

## 1. 上线

### 1.1 上线前检查（30 秒）

```sh
cd ~/IdeaProjects/zhixiaojiang
git status          # 确认这次要发的改动；工作区应干净或只含本次改动
git pull            # 如果代码是在别的机器上改的
```

> 如果本次改了表结构，先做 [第 4 节：数据库备份](#4-数据库备份与恢复)；没改表结构就跳过。

### 1.2 执行上线

```sh
scripts/deploy.sh
```

预期输出（约 1-2 分钟）：

```text
0/6 预检：服务器连通性与运行配置      → 服务器可达，app.env 已就绪 ✅
1/6 构建后端 jar（离线）             → 产物 target/zhixiaojiang-server-*.jar
2/6 构建前端                        → 产物 dist
3/6 组装发布包                      → 约 56 MB
4/6 分块上传（8 MB × N，逐块重试）    → 每块一行「上传成功」
5/6 服务器校验完整性并重启容器        → 发布包校验通过 + 容器列表
6/6 健康检查                        → 应用已就绪 ✅
上线完成：站点 http://117.72.13.19/   镜像标签 <当前提交短SHA>
```

**看到「应用已就绪 ✅」就是成功。** 失败时脚本会自动打印应用最近 50 行日志，处理办法见 [第 3 节](#3-出问题怎么办)。

常用参数：

| 参数 | 用途 |
| --- | --- |
| `--skip-tests` | 跳过本地后端测试（CI 已跑过时用，省约 1 分钟） |
| `--tag <标签>` | 指定本次镜像标签（默认取当前提交短 SHA） |
| `--rollback <标签>` | 回滚，见 [第 2 节](#2-回滚) |
| `--online` | 新机器首次构建时允许联网拉取 Maven 依赖 |

### 1.3 上线后验收（1 分钟）

```sh
# ① 站点与接口都应返回 200
curl -s -o /dev/null -w '首页 %{http_code}\n' http://117.72.13.19/
curl -s -o /dev/null -w '接口 %{http_code}\n' http://117.72.13.19/api/v1/auth/csrf

# ② 应用容器必须是 (healthy)
ssh root@117.72.13.19 'cd /opt/zhixiaojiang && docker compose ps'

# ③ 确认当前生效的镜像标签就是刚才上线的标签
ssh root@117.72.13.19 'cd /opt/zhixiaojiang && cat .env'
```

再用浏览器打开 <http://117.72.13.19/>，用 `teacher` 登录一次。口令见服务器：

```sh
ssh root@117.72.13.19 'cat /opt/zhixiaojiang/teacher-credential.txt'
```

---

## 2. 回滚

```sh
# ① 查看服务器上有哪些可用标签
ssh root@117.72.13.19 'docker images --format "{{.Repository}}:{{.Tag}}" | grep zhixiaojiang | sort -u'

# ② 回滚（约 30 秒，不重新构建、不上传）
scripts/deploy.sh --rollback <上一个标签>
```

两点必须知道：

- **回滚只切代码/镜像，不回滚数据**。如果这次上线改过表结构或写入了错误数据，按 [第 4 节](#4-数据库备份与恢复) 恢复备份。
- 镜像按提交 SHA 打标签，服务器保留历史镜像，所以旧标签随时可用。

脚本本身出问题时的**手工回滚**：

```sh
ssh root@117.72.13.19
cd /opt/zhixiaojiang
printf 'IMAGE_TAG=<标签>\n' > .env
IMAGE_TAG=<标签> docker compose up -d
docker compose logs -f --tail=50 app
```

---

## 3. 出问题怎么办

第一步永远是看日志：

```sh
ssh root@117.72.13.19 'cd /opt/zhixiaojiang && docker compose logs --tail=100 app'
ssh root@117.72.13.19 'cd /opt/zhixiaojiang && docker compose logs --tail=100 web'
```

| 现象 | 原因与处理 |
| --- | --- |
| 健康检查失败，日志里有数据库报错 | `app.env` 里的 `DB_PASSWORD` 与 MySQL 容器不一致；改好 `/opt/zhixiaojiang/app.env` 后执行 `docker compose up -d --force-recreate app` |
| 页面能打开，接口 502 | 应用容器未就绪或崩溃 → 看 app 日志 |
| 站点完全打不开 | 京东云安全组的 80 端口是否放通 |
| 上传中途断开 | 脚本已分块重试；直接重跑 `scripts/deploy.sh` |
| 本机构建报缺依赖 | 加 `--online` 重新执行（或先 `cd zhixiaojiang-server && mvn -o test` 预热本地仓库） |
| 登录后立刻失效 | `JWT_SECRET` 在两次上线之间被改过；改回后重启应用容器 |
| 页面时间差 8 小时 | 容器 `TZ` 与 JDBC `connectionTimeZone` 必须都是 `Asia/Shanghai` |

---

## 4. 数据库备份与恢复

线上数据是唯一真相（容器启动不会建表、也不会写演示数据），**改表结构前必须备份**。

```sh
# 备份：生成 /opt/zhixiaojiang/backup/zhixiaojiang-<时间>.sql（约 62 KB / 22 张表）
ssh root@117.72.13.19 'cd /opt/zhixiaojiang && mkdir -p backup && chmod 700 backup && PW=$(grep -oP "(?<=^MYSQL_PASSWORD=).*" mysql.env | tr -d "\r") && docker exec zhixiaojiang-mysql mysqldump --single-transaction --no-tablespaces -uzhixiaojiang -p"$PW" zhixiaojiang > backup/zhixiaojiang-$(date +%Y%m%d-%H%M%S).sql && ls -lh backup/ | tail -1'
```

```sh
# 恢复（会覆盖当前数据）：先停写入 → 导入 → 再启动
ssh root@117.72.13.19 'cd /opt/zhixiaojiang && docker compose stop app && PW=$(grep -oP "(?<=^MYSQL_PASSWORD=).*" mysql.env | tr -d "\r") && docker exec -i zhixiaojiang-mysql mysql -uzhixiaojiang -p"$PW" zhixiaojiang' < /opt/zhixiaojiang/backup/<备份文件>.sql
ssh root@117.72.13.19 'cd /opt/zhixiaojiang && docker compose start app'
```

> 备份与「导入」这两步都已在临时库演练通过（22 张表、42 名学生都能完整导入）。
> 「恢复」本身没有在生产库上试过覆盖，真要执行前先确认备份文件的时间点。

---

## 5. 改了表结构时的上线顺序

1. 备份数据库（[第 4 节](#4-数据库备份与恢复)）
2. 修改代码里的 `zhixiaojiang-server/src/main/resources/schema.sql`（作为新建库的基线）
3. 在线上手工执行 DDL（`ALTER` / `CREATE`），保证**新旧代码都能读**
4. 执行 `scripts/deploy.sh` 上线新代码
5. 按 [1.3](#13-上线后验收1-分钟) 验收

> **任何情况下都不要把 `DB_INIT_MODE` 改成 `always`**：那会重新执行演示数据初始化。

---
---

# 附录（背景信息，平时不用看）

## 附录 A：上线是怎么运作的

```text
本机（你的 Mac）                          京东服务器 117.72.13.19
scripts/deploy.sh
  ├─ 构建 jar + 前端 dist
  ├─ 打包 56 MB，切成 8 MB 分块上传  ──▶  /opt/zhixiaojiang/
  │                                       ├─ build/server/app.jar + Dockerfile
  │                                       ├─ build/web/dist + nginx.conf
  │                                       └─ docker-compose.yml + .env
  ├─ 服务器校验发布包完整性                │
  └─ 健康检查（内网探测）  ◀────────  docker compose up -d --build
                                          ├─ zhixiaojiang-app-1  ─ 内网 → MySQL / Redis
                                          └─ zhixiaojiang-web-1  ─ nginx:80（静态页 + /api 反代）
```

为什么用本机脚本而不是纯 GitHub 流水线（2026-09-16 实测）：

| 链路 | 速度 | 结论 |
| --- | --- | --- |
| GitHub 云端运行器 → 服务器 | 0.83 MB/s 且反复停顿 | 56 MB 要一小时，不可用 |
| 服务器 → GitHub | 约 21 KB/s | 服务器主动拉取不可行 |
| **本机 → 服务器** | **1.6 MB/s** | ✅ 采用 |
| GitHub → 本机 | 6.2 MB/s 但 TLS 不稳定 | 自托管运行器的作业会话会被中断 |

因此：**构建与测试交给 GitHub 云端 CI（稳定）**，**上线在本机执行**。仓库里的 `CI` / `Deploy` 流水线仍然保留，网络状况好时也可用。

## 附录 B：首次准备（已完成，仅在换机器/换服务器时需要）

1. 服务器 `/opt/zhixiaojiang/app.env`（600）：`DB_PASSWORD`、`REDIS_PASSWORD`、`JWT_SECRET`；变量名模板见 `deploy/app.env.example`
2. 本机 SSH 密钥 `~/.ssh/zhixiaojiang_deploy`，公钥已加入服务器 `~/.ssh/authorized_keys`
3. 仅流水线方式需要：GitHub 仓库变量/密钥（`DEPLOY_HOST`、`DEPLOY_USER`、`DEPLOY_SSH_KEY`、`REGISTRY*`）
4. 仅流水线方式需要：自托管运行器，命令为 `cd ~/github-runner-zhixiaojiang && ./svc.sh status|start|stop|uninstall`

## 附录 C：服务器上有什么

| 位置 | 内容 |
| --- | --- |
| `/opt/zhixiaojiang/app.env` | 应用口令（600，不要提交） |
| `/opt/zhixiaojiang/.env` | 当前生效的镜像标签 |
| `/opt/zhixiaojiang/build/` | 上传的构建产物（jar、前端 dist、Dockerfile） |
| `/opt/zhixiaojiang/backup/` | 数据库备份 |
| `/opt/zhixiaojiang/teacher-credential.txt` | 教师登录口令（600） |
| 容器 | `zhixiaojiang-app-1`、`zhixiaojiang-web-1`、`zhixiaojiang-mysql`、`zhixiaojiang-redis` |
| 数据卷 | `zhixiaojiang-mysql-data`、`zhixiaojiang-redis-data` |
| 网络 | `zhixiaojiang`（应用与 MySQL/Redis 同网，走内网） |

服务器日常命令：

```sh
ssh root@117.72.13.19
cd /opt/zhixiaojiang
docker compose ps                       # 容器状态
docker compose logs -f --tail=100 app   # 应用日志
docker compose restart app              # 只重启应用
docker stats --no-stream                # 资源占用
```

本机开发环境（与线上无关，各自连各自的配置）：

```sh
cd zhixiaojiang-server && mvn -o spring-boot:run    # 后端 8081
cd zhixiaojiang-web   && npm run dev                # 前端 5173
```

## 附录 D：不要做

- 不要把 `DB_INIT_MODE` 改成 `always`（会重跑演示数据初始化）
- 不要删除 `zhixiaojiang-mysql-data` / `zhixiaojiang-redis-data` 数据卷
- 不要操作同一台服务器上的 `fw-*` 容器（那是另一套平台）
- 不要把 `app.env`、`application-local.yml`、备份 SQL 提交进仓库
- 清理进程按 PID 精确操作，不要用模糊 `pkill`

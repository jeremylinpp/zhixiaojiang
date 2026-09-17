# 上线操作手册

日常发布与回滚的操作步骤。部署形态、一次性准备与安全基线见 [deployment.md](deployment.md)。

## 快速开始

```sh
cd ~/IdeaProjects/zhixiaojiang
git pull                # 确认代码是最新的
scripts/deploy.sh       # 构建 → 上传 → 服务器重启 → 健康检查（实测 1 分 11 秒）
```

成功后打开 <http://117.72.13.19/> 验证。上线期间站点会重启约 10 秒。

## 上线前（每次）

1. `git status` 确认改动范围（工作区应干净，或只含本次要发的改动）；
2. 若本次涉及表结构或数据变更，先备份数据库（见「数据库备份与恢复」）；
3. 确认此刻没有其他人在使用站点。

## 上线命令：`scripts/deploy.sh`

| 参数 | 作用 |
| --- | --- |
| 无 | 标准上线：跑后端测试 + 前端构建，然后发布 |
| `--skip-tests` | 跳过后端测试（CI 已跑过时用，可省约 1 分钟） |
| `--tag <标签>` | 指定本次构建的镜像标签（默认取当前提交短 SHA） |
| `--rollback <标签>` | **回滚**：不构建、不上传，直接用服务器上已有的旧镜像重启 |
| `--online` | 新机器首次构建时允许联网拉取 Maven 依赖（默认离线构建） |

可用环境变量覆盖目标：`DEPLOY_HOST`（默认 `117.72.13.19`）、`DEPLOY_USER`（默认 `root`）。

脚本六个步骤与预期输出：

```text
0/6 预检：服务器连通性与运行配置       → 服务器可达，app.env 已就绪 ✅
1/6 构建后端 jar（离线）              → 产物 target/zhixiaojiang-server-*.jar
2/6 构建前端                          → 产物 zhixiaojiang-web/dist
3/6 组装发布包                        → 约 56 MB
4/6 分块上传（8 MB × N，逐块重试）     → 每块一行「上传成功」
5/6 服务器校验完整性并重启容器         → 发布包校验通过 + 容器表
6/6 健康检查                          → 应用已就绪 ✅
```

失败时脚本会打印应用最近 50 行日志并以非零码退出；**直接重跑即可**（上传已分块重试，重复执行是安全的）。

## 上线后验收

```sh
# 1) 站点与接口
curl -s -o /dev/null -w '首页 %{http_code}\n'        http://117.72.13.19/
curl -s -o /dev/null -w '接口 %{http_code}\n'        http://117.72.13.19/api/v1/auth/csrf

# 2) 容器状态（app 必须是 healthy）
ssh root@117.72.13.19 'cd /opt/zhixiaojiang && docker compose ps'

# 3) 当前生效的镜像标签
ssh root@117.72.13.19 'cd /opt/zhixiaojiang && cat .env'
```

再用浏览器登录一次（口令见服务器 `/opt/zhixiaojiang/teacher-credential.txt`，仅 root 可读）。

## 回滚

```sh
# 查看服务器上有哪些可用标签
ssh root@117.72.13.19 'docker images --format "{{.Repository}}:{{.Tag}}" | grep zhixiaojiang | sort -u'

# 回滚（约 30 秒，不重新构建）
scripts/deploy.sh --rollback <上一个标签>
```

注意事项：

- **回滚只切代码/镜像，不回滚数据**。若本次上线改过表结构或写入了错误数据，需要用备份恢复（见下）。
- 镜像按提交 SHA 打标签，服务器保留历史镜像，所以旧标签随时可用。
- 脚本不可用时的手工回滚：

```sh
ssh root@117.72.13.19
cd /opt/zhixiaojiang
docker images | grep zhixiaojiang                   # 找标签
printf 'IMAGE_TAG=<标签>\n' > .env
IMAGE_TAG=<标签> docker compose up -d
docker compose logs -f --tail=50 app
```

## 数据库备份与恢复

线上数据是唯一真相（容器启动不会重建表、也不会写演示数据），涉及结构变更前务必备份。

```sh
# 备份：写到服务器 /opt/zhixiaojiang/backup/（实测 22 张表、约 62 KB）
ssh root@117.72.13.19 'cd /opt/zhixiaojiang && mkdir -p backup && chmod 700 backup && PW=$(grep -oP "(?<=^MYSQL_PASSWORD=).*" mysql.env | tr -d "\r") && docker exec zhixiaojiang-mysql mysqldump --single-transaction --no-tablespaces -uzhixiaojiang -p"$PW" zhixiaojiang > backup/zhixiaojiang-$(date +%Y%m%d-%H%M%S).sql && ls -lh backup/ | tail -1'
```

```sh
# 恢复：先停应用写入，再导入，最后启动
ssh root@117.72.13.19 'cd /opt/zhixiaojiang && docker compose stop app && PW=$(grep -oP "(?<=^MYSQL_PASSWORD=).*" mysql.env | tr -d "\r") && docker exec -i zhixiaojiang-mysql mysql -uzhixiaojiang -p"$PW" zhixiaojiang' < 备份文件.sql
ssh root@117.72.13.19 'cd /opt/zhixiaojiang && docker compose start app'
```

恢复前先确认备份文件的时间点与内容，恢复会覆盖当前数据。

## 修改表结构时的上线顺序

线上 `DB_INIT_MODE=never`，容器启动既不建表也不写演示数据，因此按下面顺序做：

1. 备份数据库（上一节）；
2. 修改代码中的 `zhixiaojiang-server/src/main/resources/schema.sql`（作为新建库的基线）；
3. 在线上手工执行 DDL（`ALTER` / `CREATE`），新旧代码都要能读这张表；
4. 执行 `scripts/deploy.sh` 发布新代码；
5. 按「上线后验收」检查。

**任何情况下都不要把 `DB_INIT_MODE` 改成 `always`**：那会重新执行演示数据初始化。

## 排障速查

| 现象 | 处理 |
| --- | --- |
| 上传中途断开 | 脚本已分块重试；直接重跑 `scripts/deploy.sh` |
| 健康检查失败 | `ssh root@117.72.13.19 'cd /opt/zhixiaojiang && docker compose logs --tail=100 app'`；常见原因是 `app.env` 里的数据库口令与容器不一致 |
| 页面能开但接口 502 | 应用容器未就绪或崩溃，看 app 日志 |
| 登录后立刻失效 | `JWT_SECRET` 在两次上线之间被改动（同一环境必须保持一致） |
| 后端构建失败提示缺依赖 | 用 `--online`；或在有网环境下预热本机 `~/.m2` |
| 站点打不开但容器正常 | 检查京东云安全组的 80 端口是否放通 |
| 接口时间偏移 8 小时 | 容器 `TZ`、JDBC `connectionTimeZone` 必须都是 `Asia/Shanghai` |

## 服务器侧常用命令

```sh
ssh root@117.72.13.19
cd /opt/zhixiaojiang

docker compose ps                       # 容器状态（app 应为 healthy）
docker compose logs -f --tail=100 app   # 应用日志
docker compose logs -f --tail=100 web   # nginx 日志
docker compose restart app              # 仅重启应用
docker stats --no-stream                # 资源占用
cat .env                                # 当前镜像标签
docker images | grep zhixiaojiang       # 可回滚的镜像标签
ls -lh backup/                          # 数据库备份
```

## 本机开发环境（与线上无关）

```sh
cd zhixiaojiang-server && mvn -o spring-boot:run    # 后端 8081
cd zhixiaojiang-web   && npm run dev                # 前端 5173
```

本机后端使用 `zhixiaojiang-server/src/main/resources/application-local.yml`（已 gitignore），
与线上 `app.env` 相互独立；两边口令或 `JWT_SECRET` 不同属正常现象。

## 不要做

- 不要把 `DB_INIT_MODE` 改成 `always`（会重跑演示数据初始化）；
- 不要删除 `zhixiaojiang-mysql-data` / `zhixiaojiang-redis-data` 数据卷；
- 不要操作同服务器上的 `fw-*` 容器（那是另一套平台）；
- 不要把 `app.env`、`application-local.yml`、备份 SQL 提交进仓库；
- 清理进程请按 PID 精确操作，不要用模糊 `pkill` 匹配。

## 换机器或交接

1. 克隆仓库，前端 `cd zhixiaojiang-web && npm ci`，后端预热 `cd zhixiaojiang-server && mvn -o test`（首次用 `--online`）；
2. 准备 SSH 私钥（复制 `~/.ssh/zhixiaojiang_deploy`，或自建密钥并把公钥追加到服务器 `~/.ssh/authorized_keys`）；
3. 确认 `DEPLOY_HOST=117.72.13.19`、`DEPLOY_USER=root`（或通过环境变量覆盖）；
4. 执行 `scripts/deploy.sh --skip-tests` 验证一次；
5. 该机器的 GitHub 访问若稳定，可另外启用 CI/Deploy 流水线（见 deployment.md）。

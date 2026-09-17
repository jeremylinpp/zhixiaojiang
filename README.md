# 智小匠 MVP

班主任端学生成长与班级治理平台。仓库包含 Spring Boot 3 后端与 Vue 3 前端两个工程。

## 工程结构

后端用 Maven 父子工程管理，前端是独立 npm 工程：

```text
zhixiaojiang/                   Maven 聚合父工程（packaging=pom，统一 Java 版本、依赖版本与插件版本）
├── pom.xml                     父 POM：modules、dependencyManagement、pluginManagement
├── docs/                       设计与运维记录（含后端模块说明 README.md）
├── scripts/                    运维与数据库脚本（人工执行，不进构建产物）
│   ├── README.md               “谁执行就放哪”的目录判据
│   └── db/create-app-user.sql  首次部署：建库、建账号、授权
├── zhixiaojiang-server/        后端子模块（Spring Boot 可执行 jar，仅保留构建输入）
│   ├── src/main/resources/     应用启动加载的 schema.sql / data.sql 等构建输入
│   └── config/                 仅本机使用的私密配置（已 gitignore）
└── zhixiaojiang-web/           前端工程（Vue 3 + Vite，npm 独立构建，不参与 Maven 生命周期）
```

模块目录只保留构建输入（`src/`、`pom.xml`）：运维脚本放仓库根级 `scripts/`，应用启动要加载的 SQL 必须留在 `src/main/resources/`，两者不混放。后端模块说明见 [docs/README.md](docs/README.md)。

后端命令在**仓库根目录**执行：

```sh
mvn test                                      # 聚合构建并运行全部后端测试
mvn -DskipTests package                       # 产出 zhixiaojiang-server/target/zhixiaojiang-server-0.1.0.jar
mvn -pl zhixiaojiang-server spring-boot:run   # 只启动后端子模块
```

前端命令在 `zhixiaojiang-web` 目录执行：`npm install`、`npm run dev`、`npm run build`。

前端不登记为 Maven module：它由 npm 独立构建，避免 Maven 构建依赖 Node 环境。若后续需要一次构建前后端，可用 frontend-maven-plugin 增加一个 web 模块。

## 本地预览

```sh
cd zhixiaojiang-web
npm install
npm run dev
```

打开 `http://127.0.0.1:5173/?view=school` 查看智小匠工作台。侧栏可进入学生档案、成长画像、机智币、智能预警、一人一策、六机任务和班级诊改页面；未连接后端时显示明确标识的演示数据。

## 后端运行

当前已将数据库迁移至京东服务器 `117.72.13.19`，前后端仍在本机运行，无需重复建库或初始化演示数据。密码通过环境变量或被忽略的 `zhixiaojiang-server/src/main/resources/application-local.yml` 注入，不应提交版本库。新环境首次建库可参考 [scripts/db/create-app-user.sql](scripts/db/create-app-user.sql)。迁移校验、访问白名单与回退说明见 [京东迁移记录](docs/jd-migration-20260916.md)。

```sh
cd zhixiaojiang-server
export DB_URL='jdbc:mysql://117.72.13.19:3306/zhixiaojiang?useUnicode=true&characterEncoding=utf8&connectionTimeZone=Asia/Shanghai&forceConnectionTimeZoneToSession=true'
export DB_USERNAME='zhixiaojiang'
export DB_PASSWORD='<SQL 中设置的密码>'
export REDIS_HOST='117.72.13.19'
export REDIS_PORT='6379'
mvn spring-boot:run
```

也可在仓库根目录执行 `mvn -pl zhixiaojiang-server spring-boot:run`，效果相同。

直连模式不会在本机监听数据库端口；应用向京东发起出站连接。数据库端口仅允许当前本机公网 IP，更换网络后需更新白名单。登录会话使用 HttpOnly Cookie，退出时仅在 `zhixiaojiang:` 命名空间写入撤销标记。Oracle 原库保留用于回退，不再作为当前写入库。默认演示账号为 `teacher / password`，正式使用前请替换。

## 配置与边界

- 本机密钥只通过环境变量（`DB_PASSWORD`、`JWT_SECRET`、`AI_API_KEY` 等）或被 gitignore 的 `zhixiaojiang-server/src/main/resources/application-local.yml` 注入，仓库不保存任何口令；变量名清单见 `docs/README.md`。
- AI 通过 `AI_BASE_URL`、`AI_MODEL`、`AI_API_KEY` 接入；未配置或异常时返回标记为 `TEMPLATE` 的规则模板建议。
- 演示数据使用幂等初始化，不覆盖已有记录。
- 技术设计与数据口径见 [docs/zhixiaojiang-mvp-technical-design.md](docs/zhixiaojiang-mvp-technical-design.md)。
- 上线与回滚操作见 [docs/deployment.md](docs/deployment.md)。

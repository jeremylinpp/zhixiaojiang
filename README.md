# 智小匠 MVP

班主任端学生成长与班级治理平台。仓库包含 Spring Boot 3 后端与 Vue 3 前端两个工程。

## 工程结构

后端用 Maven 父子工程管理，前端是独立 npm 工程：

```text
zhixiaojiang/                   Maven 聚合父工程（packaging=pom，统一 Java 版本、依赖版本与插件版本）
├── pom.xml                     父 POM：modules、dependencyManagement、pluginManagement
├── zhixiaojiang-server/        后端子模块（Spring Boot 可执行 jar）
└── zhixiaojiang-web/           前端工程（Vue 3 + Vite，npm 独立构建，不参与 Maven 生命周期）
```

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

先使用管理员账号执行 [create-app-user.sql](zhixiaojiang-server/sql/create-app-user.sql)，创建独立的 `zhixiaojiang` 数据库及专用账号。脚本中的 `<DB_PASSWORD>` 需替换为你自己生成的高强度密码，并通过 `DB_PASSWORD` 环境变量或本机 `config/application-local.yml` 注入应用；仓库不保存任何口令。当前配置直接连接 Oracle 公网地址 `192.9.244.190` 的 MySQL 和 Redis 端口。

```sh
cd zhixiaojiang-server
export DB_URL='jdbc:mysql://192.9.244.190:3306/zhixiaojiang?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
export DB_USERNAME='zhixiaojiang'
export DB_PASSWORD='<SQL 中设置的密码>'
export REDIS_HOST='192.9.244.190'
export REDIS_PORT='6379'
mvn spring-boot:run
```

也可在仓库根目录执行 `mvn -pl zhixiaojiang-server spring-boot:run`，效果相同。

直连模式不会在本机监听数据库端口；应用只向 Oracle 发起出站连接。登录会话使用 HttpOnly Cookie，退出时仅在 `zhixiaojiang:` 命名空间写入撤销标记，不会清理共享 Redis，也不会修改 Oracle 上已有应用。默认演示账号为 `teacher / password`，正式使用前请替换。

## 配置与边界

- `zhixiaojiang-server/.env.example` 仅保留变量名，不提交密码或模型密钥。
- AI 通过 `AI_BASE_URL`、`AI_MODEL`、`AI_API_KEY` 接入；未配置或异常时返回标记为 `TEMPLATE` 的规则模板建议。
- 演示数据使用幂等初始化，不覆盖已有记录。
- 技术设计与数据口径见 [docs/zhixiaojiang-mvp-technical-design.md](docs/zhixiaojiang-mvp-technical-design.md)。

# 智小匠后端

`zhixiaojiang-server` 是 `zhixiaojiang` Maven 父工程的子模块：版本号、Java 版本、依赖版本与插件版本均由仓库根目录的父 POM 管理，子模块只声明自己的依赖。

## 本地运行

1. 当前京东服务器已配置独立数据库 `zhixiaojiang` 和非 root 应用账号，不要重复执行建库或演示初始化。新环境可参考 `scripts/db/create-app-user.sql`。
2. 通过环境变量或本机被忽略的 `src/main/resources/application-local.yml` 提供账号、密码与随机 JWT 密钥（变量名：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`、`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`）。
3. 确认京东防火墙/安全组允许你的本机访问 3306、6379，再启动后端：

```bash
# 在仓库根目录：只启动后端子模块
mvn -pl zhixiaojiang-server spring-boot:run

# 或在当前模块目录内启动
mvn spring-boot:run
```

两种方式的工作目录都是 `zhixiaojiang-server`，因此本机配置文件的相对位置不变。

默认连接京东公网地址 `117.72.13.19`。迁移校验与回退说明见 [京东迁移记录](jd-migration-20260916.md)。

默认教师演示账号：`teacher` / `password`。新库首次启动前需把 `DB_INIT_MODE` 设为 `always`，执行幂等建表与演示数据初始化；初始化完成后恢复默认 `never`，避免每次启动重复初始化。

启动前请设置 `DB_PASSWORD`（SQL 中创建的应用账号密码）和 `JWT_SECRET`；Redis 可通过 `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD` 覆盖默认值。当前默认直连京东公网地址，应用不需要建立 SSH 隧道。退出登录只写入 `zhixiaojiang:` 前缀下的令牌撤销标记，不会清空 Redis。

后端单元/接口测试使用隔离的 H2 `demo` profile，不会连接或清理远程业务数据库、Redis，覆盖登录、学生档案、成长工作台、规则预警、积分幂等与撤销、帮扶状态流转、六机任务与班级诊改链路。在仓库根目录执行 `mvn test` 会聚合构建全部模块；在模块目录执行 `mvn -q test` 只构建本模块。

```bash
mvn test
```

如需在本机验证真实京东连接：

```bash
DB_PASSWORD='你的数据库密码' JWT_SECRET='至少 32 位随机字符串' mvn spring-boot:run
```

## 已实现接口

登录、当前教师、班级驾驶舱、学生列表与档案、趋势预警、模板 AI 分析、一人一策过程记录、六机任务和班级诊改。

AI 未配置时只返回模板建议，并在返回体中标识 `source: TEMPLATE`。

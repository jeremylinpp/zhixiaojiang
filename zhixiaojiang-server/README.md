# 智小匠后端

## 本地运行

1. 在 Oracle 创建独立数据库 `zhixiaojiang` 和非 root 应用账号。
2. 复制 `.env.example` 为本地环境配置并填入账号、密码和随机 JWT 密钥。
3. 确认 Oracle 防火墙/安全组允许你的本机访问 3306、6379，再运行 `mvn spring-boot:run`。

默认连接 Oracle 公网地址 `192.9.244.190`；如后续关闭公网数据库端口，可改用仓库根目录 `scripts/oracle-tunnel.sh` 的本地隧道方式。

默认教师演示账号：`teacher` / `password`。首次启动会执行幂等 schema 和演示数据初始化。

启动前请设置 `DB_PASSWORD`（SQL 中创建的应用账号密码）和 `JWT_SECRET`；Redis 可通过 `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD` 覆盖默认值。当前默认直连 Oracle 公网地址，应用不需要建立 SSH 隧道。退出登录只写入 `zhixiaojiang:` 前缀下的令牌撤销标记，不会清空共享 Redis。

后端单元/接口冒烟测试使用隔离的 H2 `demo` profile，不会连接或清理共享 Oracle、Redis：

```bash
mvn -q test
```

如需在本机验证真实 Oracle 连接：

```bash
DB_PASSWORD='你的数据库密码' JWT_SECRET='至少 32 位随机字符串' mvn spring-boot:run
```

后端启动后，另开终端运行仓库根目录的 `scripts/smoke-test.sh` 完成登录、学生、预警、积分、帮扶、任务和诊改链路检查。

## 已实现接口

登录、当前教师、班级驾驶舱、学生列表与档案、趋势预警、模板 AI 分析、一人一策过程记录、六机任务和班级诊改。

AI 未配置时只返回模板建议，并在返回体中标识 `source: TEMPLATE`。

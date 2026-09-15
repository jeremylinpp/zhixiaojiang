# 智小匠 MVP

班主任端学生成长与班级治理平台。当前仓库包含 Vue 3 前端、Spring Boot 3 后端、数据库迁移脚本与 Oracle SSH 隧道脚本。

## 本地预览

```sh
cd zhixiaojiang-web
npm install
npm run dev
```

打开 `http://127.0.0.1:5173/?view=school` 查看智小匠工作台。侧栏可进入学生档案、成长画像、机智币、智能预警、一人一策、六机任务和班级诊改页面；未连接后端时显示明确标识的演示数据。

## 后端运行

先使用管理员账号执行 [create-app-user.sql](zhixiaojiang-server/sql/create-app-user.sql)，创建独立的 `zhixiaojiang` 数据库及专用账号。脚本中的密码为本次生成的初始密码，首次登录后建议按你的密钥管理规范轮换。当前配置直接连接 Oracle 公网地址 `192.9.244.190` 的 MySQL 和 Redis 端口；SSH 隧道脚本仍保留作备用。

```sh
cd zhixiaojiang-server
export DB_URL='jdbc:mysql://192.9.244.190:3306/zhixiaojiang?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
export DB_USERNAME='zhixiaojiang'
export DB_PASSWORD='<SQL 中设置的密码>'
export REDIS_HOST='192.9.244.190'
export REDIS_PORT='6379'
mvn spring-boot:run
```

直连模式不会在本机监听数据库端口；应用只向 Oracle 发起出站连接。登录会话使用 HttpOnly Cookie，退出时仅在 `zhixiaojiang:` 命名空间写入撤销标记，不会清理共享 Redis，也不会修改 Oracle 上已有应用。默认演示账号为 `teacher / password`，正式使用前请替换。

## 配置与边界

- `zhixiaojiang-server/.env.example` 仅保留变量名，不提交密码或模型密钥。
- AI 通过 `AI_BASE_URL`、`AI_MODEL`、`AI_API_KEY` 接入；未配置或异常时返回标记为 `TEMPLATE` 的规则模板建议。
- 演示数据使用幂等初始化，不覆盖已有记录。
- 技术设计与数据口径见 [docs/zhixiaojiang-mvp-technical-design.md](docs/zhixiaojiang-mvp-technical-design.md)。

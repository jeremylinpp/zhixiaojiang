-- 智小匠 MVP：首次部署时执行一次
-- 执行方式：使用 MySQL 管理员账号在 Oracle 的 MySQL 8.4.9 中执行
-- 密码不在仓库中保存：先把下方 <DB_PASSWORD> 替换为本机生成的高强度随机密码，
-- 再通过 DB_PASSWORD 环境变量（或 zhixiaojiang-server/config/application-local.yml）注入应用。

CREATE DATABASE IF NOT EXISTS `zhixiaojiang`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

-- 应用账号仅访问自己的数据库；应用本身仍只在本机运行。
CREATE USER IF NOT EXISTS 'zhixiaojiang'@'%' IDENTIFIED BY '<DB_PASSWORD>';

-- 当前 Spring Boot 配置会在首次启动时执行幂等建表脚本，因此需要以下 DDL/DML 权限。
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON `zhixiaojiang`.* TO 'zhixiaojiang'@'%';

FLUSH PRIVILEGES;

-- 如果该账号已经存在且需要重置密码，请单独执行：
-- ALTER USER 'zhixiaojiang'@'%' IDENTIFIED BY '<DB_PASSWORD>';

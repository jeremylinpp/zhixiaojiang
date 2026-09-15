# scripts

本目录存放**由人工执行**的运维与数据库脚本：它们不是构建输入，不会进入 jar 或前端产物，因此按仓库根级目录统一管理，不再放在 `zhixiaojiang-server/` 模块内部。

## 判据：谁执行，就放哪

| 脚本 | 执行者 | 位置 |
|---|---|---|
| `db/create-app-user.sql` | 管理员手工执行一次：建库、建应用账号、授权 | 本目录（运维脚本） |
| `zhixiaojiang-server/src/main/resources/schema.sql`、`data.sql`、`demo-schema.sql`、`demo-data.sql` | 应用启动时由 `spring.sql.init` 从 classpath 加载 | 留在模块的 `src/main/resources/`（构建输入） |

即：**应用启动要加载的 SQL 属于构建输入，必须留在 `src/main/resources/`；人工执行的脚本属于运维资产，统一放在本目录。** 不要把二者混在同一个目录里，也不要为了“目录好看”把启动脚本移出 classpath。

## 执行方式

```sh
# 首次部署：替换 <DB_PASSWORD> 后，用管理员账号执行
mysql -h <host> -u <admin> -p < scripts/db/create-app-user.sql
```

口令只通过 `DB_PASSWORD` 环境变量或本机被忽略的 `zhixiaojiang-server/config/application-local.yml` 注入仓库之外，本目录不保存任何口令。

## 目录约定

- 数据库相关脚本放 `db/`；
- 其他运维脚本（隧道、冒烟测试、一次性数据修复）直接放本目录或其子目录，不放回模块内部；
- 脚本一律使用相对仓库根目录的路径或环境变量，避免硬编码本机绝对路径。

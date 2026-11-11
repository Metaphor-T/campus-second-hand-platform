# 数据库统一导入说明

更新日期：2026-11-11。

第一次运行请先看[成员本机运行教程：数据库连接到前后端启动](member-local-setup.md)，其中包含 DataGrip 字段、新建空库、账号授权、后端 YAML、IDEA/终端启动和常见报错。

仓库只提供一个 SQL 入口：[`sql/db.sql`](../sql/db.sql)。它来自已验收稳定项目的数据库快照，包含 17 张表、504 条记录，以及订单库存记录字段、个人校园资料、校园认证预留表。不需要再执行旧迁移脚本。

## 首次安装

1. 安装并启动 MySQL 8.0。本次还原验证使用 MySQL 8.0；未验证其他数据库版本。
2. 在 DataGrip 中新建一个自己的空数据库，例如 `campus_demo`，字符集使用 `utf8mb4`。
3. 打开 `sql/db.sql`，确认执行目标是这个空数据库，完整执行文件。
4. 在自己的 `api/src/main/resources/application-local.yml` 中配置数据库 URL、账号、密码。该文件被 Git 忽略，不要提交。本地默认模式首次启动会自动生成并保存 JWT 密钥；已有独立密钥继续使用。正式部署和旧版本升级见[JWT 配置与升级说明](jwt-configuration-and-upgrade.md)。
5. 在 `view` 目录运行 `npm ci` 安装依赖，再运行 `npm run dev`；在 `api` 目录运行 `mvn spring-boot:run`。后端需要 Java 21；前端此前使用 Node 24 验证。

前端默认地址为 `http://localhost:21091`，API、图片和聊天 WebSocket 默认统一连接 `http://localhost:21090`，未创建前端环境文件也可使用这一默认值。若后端运行在其他地址，将 `view/.env.example` 复制为 `view/.env.local`，修改 `VITE_API_BASE_URL` 后重启前端。`npm run build` 同样使用这项配置；部署给其他电脑访问时，应填写它们能够访问的后端地址。

`.env.local` 被 Git 忽略，`.env.example` 是随仓库提供的非敏感示例。所有 `VITE_` 变量都会进入浏览器代码，不要在其中填写数据库密码、JWT 密钥等后端凭据。

本机配置示例（将占位内容换成自己的配置）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_demo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8
    username: "自己的数据库账号"
    password: "自己的数据库密码"
```

本地无需复制 JWT 占位示例。正式部署时必须显式启用 `prod` 并提供独立密钥，不能依赖默认本地模式。

## 已经有数据库的成员

更新项目并保留原库时，先按[已有项目更新步骤](member-local-setup.md#已有项目更新保留自己的数据库)迁移自己的连接配置、核对环境变量覆盖和数据库结构；无需套用首次安装的示例库名或账号。

`db.sql` 是完整快照，不是可反复执行的增量升级脚本。已有数据库需要先备份；建议新建另一个空库导入，再修改自己的本机连接配置。原库保留，成员自己新增的用户、商品、订单不会自动合并到新库。

SQL 中没有创建或切换数据库的命令，也没有删表命令，但这不表示可以安全导入非空库；表名、主键等仍会冲突。出现错误时停止执行并检查目标库，不要忽略错误继续导入。

## 演示账号与数据

- `admin` 为管理员演示账号。
- `test`、`test1`、`test2`、`test3` 的展示昵称分别为成员1、成员2、成员3、成员4。
- 快照包含稳定库当前保存的其他业务数据，并非空白模板；本次所含系统账号密码均为原演示密码 `123456`。仅用于课程演示，公开部署前应自行修改。
- 不含数据库连接密码或 JWT 密钥。SQL 中仍有系统账号、联系方式、地址和聊天等业务记录，请按演示用途使用并保管。
- SQL 只保存图片路径；头像、商品图、评价图等上传文件需另行从项目维护者取得并放入自己的 `api/file`。仅导入 SQL 不会自动获得这些图片。

## 本次核对

完整 SQL 已在独立开发测试库的隔离临时表中还原，逐表核对字段、约束和全部数据指纹：17 张表、504 条记录一致，临时表已删除，原稳定数据库未修改。见[验证结果](accessibility-evidence/full-database-sql-results.json)。

## Git 历史清理后的成员同步

旧 SQL 文件已从 `main` 和 `player-2` 的历史中移除；非 SQL 文件、提交作者和提交说明保留，相关提交号发生变化。最新完整快照只重新加入 `main` 的最新提交。

成员应备份自己的未提交修改，然后将仓库重新克隆到新目录。不要直接合并或推送旧仓库的分支，否则可能把旧 SQL 历史重新带回 GitHub。尚未完成的个人改动，应仅迁移需要的代码文件到新克隆，再检查、提交。

此次操作清理仓库分支可达的旧 SQL 历史，不能保证已存在的其他克隆、分叉、拉取请求引用和服务器缓存一并消失；如需处理旧提交链接的缓存，须进一步联系 GitHub Support。

## 开发测试配置

后端普通测试：在 `api` 目录运行 `mvn test`。未设置 `CAMPUS_STATS_DB_PASSWORD` 时，数据库集成测试会明确跳过；跳过不能视为集成测试已通过。

需要集成测试时，单独准备 `campus_system_dev_test`，导入完整 SQL，并创建仅有该库权限的 `campus_dev_test` 账号，通过环境变量 `CAMPUS_STATS_DB_PASSWORD` 提供其数据库密码。这不是系统管理员的登录密码。测试不得连接成员自己的稳定或正式数据库。

旧浏览器验收脚本现通过 `CAMPUS_TEST_ADMIN_ACCOUNT`、`CAMPUS_TEST_ADMIN_PASSWORD` 读取独立测试环境的系统管理员登录身份，不读取 SQL、数据库连接配置或默认密码。缺失时在启动浏览器前明确报错，不输出密码。

这些浏览器脚本使用 Windows Edge，工具依赖需要在仓库根目录单独准备一次：

```powershell
npm install --prefix .local-backups/a11y-tools --no-save playwright @axe-core/playwright
```

先启动连接独立测试库的开发前后端，再在仓库根目录设置本终端的测试登录身份并执行所需脚本，例如：

```powershell
$adminLogin = Get-Credential -UserName admin -Message '独立开发测试库的系统管理员登录身份'
$env:CAMPUS_TEST_ADMIN_ACCOUNT = $adminLogin.UserName
$env:CAMPUS_TEST_ADMIN_PASSWORD = $adminLogin.GetNetworkCredential().Password
node view/tests/admin-sidebar-browser.cjs
Remove-Item Env:CAMPUS_TEST_ADMIN_PASSWORD
Remove-Item Env:CAMPUS_TEST_ADMIN_ACCOUNT
Remove-Variable adminLogin
```

账号创建、资料修改、订单等验收脚本可能创建临时业务记录，务必使用独立测试库；以前留存的结果属于对应版本，不代表本次重新执行过所有浏览器测试。

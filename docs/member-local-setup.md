# 成员本机运行教程：数据库连接到前后端启动

适用于 Windows 本机课程演示。全程使用成员自己电脑上的 MySQL。示例数据库名统一为 `campusdemo`，可以自行换名，但创建、授权、DataGrip 和后端配置中的名称必须一致。

新版本地 JWT 自动生成功能的配置与旧部署升级方法见[JWT 说明](jwt-configuration-and-upgrade.md)。若使用的旧代码提示缺少 `CAMPUS_JWT_SECRET`，需先按该说明手工配置，或在新版发布后更新；不能仅凭新版文档判断旧代码已具备自动生成功能。

## 已有项目更新：保留自己的数据库

已经部署过的成员优先执行本节，不需要照首次安装步骤重新建库、创建示例账号或导入完整 SQL。

本次库存和七天售后更新还需执行[版本升级说明中的完整增量 SQL](project-upgrade-20261111.md#保留原数据库增量升级)。先停止旧后端、备份并执行增量，再启动新版，不能直接跳过数据库升级。

1. 更新前备份本机配置、上传文件、个人代码修改和数据库，记下原来的数据库主机、端口、库名及账号。密码自行保管，不发送到群聊或提交 Git。
2. 取得新版代码后，在自己的 `api/src/main/resources/application-local.yml` 中显式写全数据库 URL、账号和密码。旧配置位于 `application.yml` 时，只迁移这些本机连接值，不用旧文件整份覆盖新版。
3. 使用原来的数据库名和账号。示例中的 `campusdemo`、`campus_app` 只为首次安装演示，不能直接替换原来正常使用的连接值。
4. 在 DataGrip 中使用与后端完全相同的主机、端口、数据库账号和密码测试连接，并选择原数据库执行下面的只读核对。
5. 检查启动终端、IDEA 运行配置或服务器服务中是否设置了 `SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`、`SPRING_PROFILES_ACTIVE`、`SPRING_CONFIG_LOCATION` 等覆盖项；这些设置可能让新文件没有按预期生效。使用 `prod` 或自定义配置位置的部署，应将连接写到实际生效的配置中，不能依赖只在 local 加载的文件。
6. 停止旧后端，按升级说明执行并核对增量 SQL 后，再启动新版后端，避免旧进程继续占用 21090，导致网页实际访问的仍是旧程序。IDEA 工作目录使用 `api`，保留同一套上传文件。
7. 按下方核对后端接口和已知业务记录；如发现缺表或缺字段，停止相关业务测试，先核对版本并准备对应增量迁移，不要以导入完整 SQL 代替升级。

后端本机配置结构如下，三个值均填写更新前自己的实际连接：

```yaml
spring:
  datasource:
    url: "原来实际使用的完整jdbc:mysql连接地址"
    username: "原来实际使用的数据库账号"
    password: "该数据库账号自己的密码"
```

这里的字符串都是需要替换的说明，不是可直接运行的配置。若是在自己的另一台服务器上部署，`127.0.0.1` 表示服务器自身；数据库不在同机时，需要使用真实数据库主机和相应账号授权。

只读核对 SQL：

```sql
SELECT DATABASE() AS connected_database,
       CURRENT_USER() AS authenticated_account,
       VERSION() AS mysql_version;
SHOW COLUMNS FROM `order` LIKE 'stock_deducted_quantity';
SHOW TABLES LIKE 'user_campus_profile';
SHOW TABLES LIKE 'campus_verification';
SELECT COUNT(*) AS users_in_this_database FROM base_user;
```

确认库名、账号与更新前一致，三个结构检查都有结果。原数据库记录数不必等于快照；更新代码不会让所有成员的业务数据相同。JWT 更新本身不需要修改数据库。

后端启动后打开 `http://127.0.0.1:21090/guest/category/list`，确认返回 `code: 200`，再核对一个自己原来已有的商品或订单。若业务数据与 DataGrip 不一致，先检查环境变量覆盖、后端启动日志和端口进程，不要马上复制或覆盖数据库。

以上步骤用于明确连接来源和验证结果；无法自动获知成员电脑上的原始密码或网络权限。遇到错误可按本页末尾的排查表定位。

## 1. 准备环境和代码

需要 MySQL 8.0、Java 21、Node 24，以及 Maven（或项目附带的 Maven Wrapper）。DataGrip 是数据库客户端，不能代替 MySQL 服务。

在终端确认：

```powershell
java -version
node -v
npm -v
mvn -v
```

没有安装 Maven 时，可使用后面给出的 `mvnw.cmd` 命令，首次使用需联网下载。IDEA 的 Project SDK 和 Maven Runner JRE 都选择 Java 21。

第一次取得代码，在准备保存项目的目录执行：

```powershell
git clone https://github.com/Metaphor-T/campus-second-hand-platform.git
cd campus-second-hand-platform
```

保留着历史清理前旧副本的成员，先备份个人代码和本机配置，再克隆到新目录；不要直接覆盖旧目录或合并旧 Git 历史。已经使用清理后历史的成员，可以保存自己的修改后正常更新。

## 2. 用 DataGrip 连接自己的 MySQL

先确认 Windows“服务”里的 MySQL 服务已启动，常见服务名为 `MySQL80`，以自己安装时的名称为准。

在 DataGrip 新增 MySQL 数据源，先使用自己的数据库管理员账号准备空库：

| 字段 | 本机填写示例 |
| --- | --- |
| 名称 | 校园项目－数据库初始化 |
| 驱动 | MySQL；提示缺少驱动时先下载 |
| 主机 | `127.0.0.1` |
| 端口 | `3306`，与自己的安装一致 |
| 用户 | 自己的 MySQL 管理员账号，例如 `root` |
| 密码 | 安装 MySQL 时设置的数据库密码 |
| 数据库 | 创建数据库前可以先留空 |

点击“测试连接”，成功后保存。这里的数据库密码与网页管理员账号 `admin` 的密码是两回事，也不使用项目维护者电脑上的数据库密码。

## 3. 新建空库，导入唯一 SQL

在这个管理员连接的查询控制台执行：

```sql
CREATE DATABASE campusdemo
  CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
USE campusdemo;
SELECT DATABASE();
```

最后一条结果应为 `campusdemo`。如果提示库已存在，先确认它是否已有数据；需要全新演示环境时换一个新库名，不要删掉原库。

打开仓库里的 `sql/db.sql`，在 DataGrip 中明确选择刚创建的数据库作为执行目标，运行**整个文件**，不是只执行光标所在的一条语句。遇到错误先停止并检查，不要忽略错误继续导入。

本 SQL 是完整快照，只适用于空库。已有用户、商品或订单的数据库不要重新导入；它们不会自动与快照合并。无需逐个执行旧迁移脚本。

导入后刷新 DataGrip 的表列表，并在 `campusdemo` 控制台核对：

```sql
SELECT DATABASE() AS current_database, VERSION() AS mysql_version;
SELECT COUNT(*) AS table_count
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE';
SHOW COLUMNS FROM `order` LIKE 'stock_deducted_quantity';
SHOW TABLES LIKE 'user_campus_profile';
SHOW TABLES LIKE 'campus_verification';
```

当前快照应有 **18 张表**，三个结构核对项都应有结果；库存和售后新增字段另按版本升级说明核对。快照数据共 505 条，实际运行后业务数据会增加，不以总记录数固定为验收条件。

## 4. 为程序配置数据库账号

建议网页程序使用独立账号，初始化仍由前面的管理员连接执行。在自己电脑的管理员控制台执行以下命令，先将密码占位内容替换为自己选择的数据库密码：

```sql
CREATE USER 'campus_app'@'127.0.0.1'
  IDENTIFIED BY '替换为自己设置的数据库密码';
GRANT SELECT, INSERT, UPDATE, DELETE ON campusdemo.*
  TO 'campus_app'@'127.0.0.1';
```

这组权限用于程序读写该演示库，不包含创建其他数据库、建表或管理账号的权限。若账号已存在，先核对它的用途和密码，或选择新的账号名；不要盲目删除或重置已有账号。MySQL 的账号创建与授权规则可参考[官方说明](https://dev.mysql.com/doc/refman/8.0/en/creating-accounts.html)。

可以再新增一个 DataGrip 连接，主机同样填写 `127.0.0.1`，用户填 `campus_app`，密码填刚设置的值，数据库填 `campusdemo`。测试连接成功后执行：

```sql
SELECT DATABASE(), CURRENT_USER();
SHOW GRANTS;
SELECT COUNT(*) AS users_in_demo FROM base_user;
```

确认正在访问 `campusdemo`，授权账号与预期一致。若自己的 MySQL 端口不是 3306，下一节的 JDBC 地址也要使用同一个端口。

## 5. 配置后端连接

在项目中创建或编辑 `api/src/main/resources/application-local.yml`。保留自己的其他配置，不要重复创建两个顶层 `spring` 节点。

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/campusdemo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8
    username: "campus_app"
    password: "刚才为campus_app设置的数据库密码"
```

注意文件名为 `application-local.yml`，使用空格缩进，不使用 Tab。填写自己的真实密码，保留外层引号；如果密码中包含双引号或反斜杠，需要正确进行 YAML 转义。

新版默认 `local` 模式会自动生成并保存 JWT 密钥；已经有独立 JWT 配置的成员继续使用原配置。生产服务器必须显式启用生产模式和配置密钥，见[JWT 配置与升级说明](jwt-configuration-and-upgrade.md)。

本机数据库配置和生成的密钥文件都被 Git 忽略，不上传。`application.yml` 的默认库名不必与自己的库名相同，本机配置会覆盖它；也应检查 IDEA 中是否另外设置了 `SPRING_DATASOURCE_*`，避免环境变量把本机配置覆盖成其他库。

## 6. 启动后端

在终端进入项目的 `api` 目录：

```powershell
cd api
mvn spring-boot:run
```

没有独立 Maven 时，改为：

```powershell
.\mvnw.cmd spring-boot:run
```

IDEA 运行方式：打开后端 `api/pom.xml` 导入 Maven 项目，选择 Java 21，运行 `com.example.ApiApplication`；运行配置的工作目录设为项目的 **`api` 目录**，保证上传文件路径一致。

后端成功时应显示 `Started ApiApplication`，监听 21090。浏览器打开以下只读接口检查：

```text
http://127.0.0.1:21090/guest/category/list
```

预期返回 JSON 且 `code` 为 `200`，包含商品分类。这一步既检查后端服务，也检查它能否读到数据库。后端根地址没有网页，直接打开 `http://127.0.0.1:21090/` 出现 404 不等同于后端未启动。

## 7. 启动前端并登录

保持后端终端运行，再开一个终端，从项目根目录进入 `view`：

```powershell
cd view
npm ci
npm run dev
```

打开前端日志给出的地址，默认：`http://localhost:21091/login`。

前端 API、图片和 WebSocket 默认统一连接 `http://localhost:21090`。若自己的后端用了其他端口或主机，将 `view/.env.example` 复制为 `view/.env.local` 并修改：

```dotenv
VITE_API_BASE_URL=http://127.0.0.1:21090
```

修改后重启前端。填写后端 HTTP 地址，不是数据库地址；不在这里填写数据库密码或 JWT 密钥。其他机器访问时，`localhost` 表示访问者自己，需要填写实际可访问的后端地址。

快照的演示登录信息如下；如果成员自行改过密码或昵称，以自己的数据库为准：

| 网页登录身份 | 账号 | 演示密码 |
| --- | --- | --- |
| 管理员 | `admin` | `123456` |
| 用户 | `test`、`test1`、`test2`、`test3` | `123456` |

普通用户登录前要在登录页切换为“用户”，管理员选择“管理员”。演示密码仅用于本机课程展示。

## 8. 准备图片并检查完整流程

数据库只保存图片路径，不包含图片文件。向项目维护者取得对应上传文件，按原来的相对目录放进自己的 `api/file`，保留 `avatars`、`products` 等子目录。Git 下载通常不包含这批文件。

检查以下项目，再认为本机安装完成：

- DataGrip 能以程序账号连接目标数据库，表结构核对通过。
- 后端分类接口返回成功；网页管理员和普通用户分别能登录。
- 首页能读取商品，订单中心能进入订单详情。
- 管理员仪表盘能读取本机库统计。
- 头像和商品图正常；若只有图片缺失，先核对上传文件和工作目录。

## 9. 常见问题对照

| 现象 | 优先检查 |
| --- | --- |
| `Communications link failure`、连接被拒绝 | MySQL 服务是否启动、主机与端口是否正确 |
| `Access denied for user` | 数据库账号、密码及账号对应的连接主机；先用完全相同的配置测试 DataGrip |
| `Unknown database` | 是否创建了库，JDBC 中的库名是否与 DataGrip 一致 |
| 表不存在或 `stock_deducted_quantity` 字段不存在 | 是否完整导入了新版 SQL，后端是否连接到了另一个旧库 |
| `CREATE command denied` | 初始化使用管理员连接；程序账号只用于运行，不能用它建表 |
| YAML 解析错误 | 文件名、空格缩进、重复节点和密码引号转义 |
| 缺少 `CAMPUS_JWT_SECRET` | 代码版本是否已包含自动配置；旧版需按 JWT 说明手工配置 |
| JWT 文件损坏或不能写入 | 查看日志文件路径，核对权限，优先恢复原密钥备份 |
| `UnsupportedClassVersionError`、Java 版本不符 | 终端与 IDEA Maven Runner 是否都使用 Java 21 |
| `mvn` 未找到 | 安装并配置 Maven，或在 `api` 目录使用 `mvnw.cmd` |
| `npm ci` 下载失败 | Node/npm 是否可用、网络能否访问锁定依赖；不要通过删除锁文件随意换版本 |
| 21090/21091 端口被占用 | 是否已有另一份项目运行；确认进程后再处理，不同时启动多份 |
| 页面能打开但接口请求 404 或网络错误 | 后端是否启动；前端 API 地址是否正确；环境文件修改后是否重启前端 |
| 接口数据与 DataGrip 中不一致 | 执行 `SELECT DATABASE()`，核对后端实际连接配置及环境变量覆盖 |
| 更新后提示登录失效 | 密钥或登录凭据机制更新后重新登录 |
| 商品图或头像不显示 | 是否取得 `api/file` 文件，是否在 `api` 工作目录启动后端 |

本教程覆盖项目已验证的安装方式，成员机器上的端口、权限、依赖下载和服务状态仍需按以上检查点确认。旧库保留自己的数据，是否升级其他表结构应单独检查，不以重新导入完整 SQL 代替。

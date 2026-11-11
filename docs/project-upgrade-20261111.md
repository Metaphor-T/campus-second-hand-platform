# 已有项目升级说明：库存与七天售后

本次更新发布到 `main`。首次安装只使用 [`sql/db.sql`](../sql/db.sql)；仓库只保留这一个完整 SQL 文件。已有数据库使用本文的增量 SQL，不重复导入完整快照。

## 升级前准备

1. 停止旧后端，备份数据库结构和数据，并保存本机 `api/src/main/resources/application-local.yml`、JWT 密钥、上传目录 `api/file` 和自己的未提交代码。
2. 确认当前代码已是此前能正常运行的 `main`，数据库具有 `order.stock_deducted_quantity`、`user_campus_profile`、`campus_verification`。更早的分支如果缺少这些结构，先核对版本；可以将完整 SQL 导入另一个新空库，但不能覆盖旧库或忽略错误继续执行。
3. 已经按历史清理说明重新克隆过的成员，在自己的 `main` 分支执行下列命令。有个人修改先提交或另行备份；命令不能快进时先处理差异，不强制覆盖。

```powershell
git switch main
git pull --ff-only origin main
```

仍使用清理 SQL 历史前的旧克隆时，按[历史清理后的同步说明](database-installation.md#git-历史清理后的成员同步)保留个人修改并重新克隆，避免带回旧历史。

## 保留原数据库增量升级

在 DataGrip 中连接自己的原数据库，执行 `SELECT DATABASE();` 核实目标。整个升级期间保持旧后端停止。下方 SQL 是已在独立测试库隔离表验证过的完整增量，复制整个代码块到控制台一次执行；可以重复运行。MySQL DDL 不能靠普通事务回滚，必须先备份。

开发人员只能在独立测试库操作；本机稳定项目仅在用户验收授权后升级。成员自己的库名、账号和密码不需改为维护者的配置。

```sql
-- 先备份并选择目标数据库；开发阶段仅在 campus_system_dev_test 执行。
-- 0=有限库存，1=不限库存，NULL=旧数据待确认。不能根据旧的零库存猜测原模式。
-- 可重复执行；保留原库存、上下架状态、业务时间、订单及账号数据。
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='product' AND COLUMN_NAME='stock_mode');
SET @migration_sql = IF(@column_exists=0,
 'ALTER TABLE `product` ADD COLUMN `stock_mode` TINYINT NULL DEFAULT NULL COMMENT ''0有限库存1不限库存NULL历史待确认''',
 'SELECT ''stock_mode already exists'' AS migration_status');
PREPARE stock_migration FROM @migration_sql;
EXECUTE stock_migration;
DEALLOCATE PREPARE stock_migration;
UPDATE `product` SET stock_mode=0, update_time=update_time
 WHERE stock_mode IS NULL AND stock BETWEEN 1 AND 999999;
-- 此处只输出待确认的编号、数量和状态；不自动恢复旧的已下架商品。
SELECT id, stock, status FROM `product` WHERE stock_mode IS NULL ORDER BY id;


-- 2026-11-11 售后流程增量升级。先停止后端并备份，再选择目标库。
-- 开发阶段仅允许 campus_system_dev_test；不重建或覆盖原业务表。
-- 保留原订单状态、时间及数量；缺失时间不补为当前时间。可重复执行。
SET @exists_col=(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='order' AND COLUMN_NAME='stock_mode_snapshot');
SET @ddl=IF(@exists_col=0,'ALTER TABLE `order` ADD COLUMN `stock_mode_snapshot` TINYINT NULL DEFAULT NULL COMMENT ''下单时库存模式快照，历史不明为NULL''','SELECT 1');
PREPARE after_sale_migration FROM @ddl; EXECUTE after_sale_migration; DEALLOCATE PREPARE after_sale_migration;
SET @exists_col=(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='order' AND COLUMN_NAME='after_sale_deadline');
SET @ddl=IF(@exists_col=0,'ALTER TABLE `order` ADD COLUMN `after_sale_deadline` DATETIME NULL DEFAULT NULL COMMENT ''确认收货后七天售后截止''','SELECT 1');
PREPARE after_sale_migration FROM @ddl; EXECUTE after_sale_migration; DEALLOCATE PREPARE after_sale_migration;
SET @exists_col=(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='order' AND COLUMN_NAME='stock_released_quantity');
SET @ddl=IF(@exists_col=0,'ALTER TABLE `order` ADD COLUMN `stock_released_quantity` INT NOT NULL DEFAULT 0 COMMENT ''已释放或记录的原扣库数量''','SELECT 1');
PREPARE after_sale_migration FROM @ddl; EXECUTE after_sale_migration; DEALLOCATE PREPARE after_sale_migration;
SET @exists_col=(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='order' AND COLUMN_NAME='stock_release_disposition');
SET @ddl=IF(@exists_col=0,'ALTER TABLE `order` ADD COLUMN `stock_release_disposition` VARCHAR(40) NULL DEFAULT NULL COMMENT ''库存释放处理方式''','SELECT 1');
PREPARE after_sale_migration FROM @ddl; EXECUTE after_sale_migration; DEALLOCATE PREPARE after_sale_migration;

CREATE TABLE IF NOT EXISTS `order_after_sale` (
 `id` INT NOT NULL AUTO_INCREMENT,
 `order_id` INT NOT NULL,
 `request_type` VARCHAR(24) NOT NULL,
 `state` VARCHAR(24) NOT NULL,
 `reason` VARCHAR(1000) NOT NULL,
 `evidence_json` TEXT NULL,
 `original_order_status` INT NULL,
 `return_required` TINYINT(1) NOT NULL DEFAULT 1,
 `applicant_id` INT NULL,
 `seller_agree` TINYINT(1) NULL,
 `seller_note` VARCHAR(1000) NULL,
 `seller_id` INT NULL,
 `seller_responded_at` DATETIME NULL,
 `admin_id` INT NULL,
 `admin_note` VARCHAR(1000) NULL,
 `admin_reviewed_at` DATETIME NULL,
 `return_confirmed_by` INT NULL,
 `return_note` VARCHAR(1000) NULL,
 `return_confirmed_at` DATETIME NULL,
 `return_quantity` INT NULL,
 `stock_credited_quantity` INT NOT NULL DEFAULT 0,
 `stock_disposition` VARCHAR(40) NULL,
 `created_at` DATETIME NULL,
 `updated_at` DATETIME NULL,
 PRIMARY KEY (`id`),
 KEY `idx_after_sale_order` (`order_id`,`id`),
 KEY `idx_after_sale_state` (`state`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='订单售后申请及卖家回应、管理员审核、退货处理记录';

UPDATE `order` SET after_sale_deadline=DATE_ADD(receive_time,INTERVAL 7 DAY),update_time=update_time
 WHERE after_sale_deadline IS NULL AND receive_time IS NOT NULL;
UPDATE `order` SET stock_mode_snapshot=CASE WHEN stock_deducted_quantity>0 THEN 0 ELSE 1 END,update_time=update_time
 WHERE stock_mode_snapshot IS NULL AND stock_deducted_quantity IS NOT NULL
 AND (stock_deducted_quantity>0 OR (stock_deducted_quantity=0 AND order_status!=4));
-- 旧的售后中订单缺少申请时间及原状态，转入管理员待核对队列，不猜测历史事实。
INSERT INTO order_after_sale(order_id,request_type,state,reason,return_required,applicant_id)
 SELECT o.id,'LEGACY','WAIT_ADMIN','历史售后申请：原申请时间、原交易状态和双方意见待管理员核对',1,o.buyer_id
 FROM `order` o WHERE o.order_status=5 AND NOT EXISTS(SELECT 1 FROM order_after_sale r WHERE r.order_id=o.id);
SELECT COUNT(*) AS legacy_after_sale_to_review FROM order_after_sale WHERE request_type='LEGACY' AND state='WAIT_ADMIN';
```

脚本只增加 `product.stock_mode`、订单的四个字段及 `order_after_sale` 表，并按明确的原记录补充信息；不删除原商品、用户、订单，不更新其原数量、状态和时间，不导入开发测试数据。

- 历史正数库存标为有限库存；旧零库存不能区分原来的不限或售罄，留待卖家编辑确认。
- 有真实收货时间的订单按原时间计算七天期限；时间缺失不补为当前时间。
- 旧“退款/售后中”记录进入管理员待核对队列；卖家可补充意见，管理员驳回前需确认原交易阶段。

## 配置、依赖和启动

1. 保留自己的数据库地址、账号、密码，不用仓库默认的 `project_db/root` 覆盖本机连接；完整教程见[成员本机运行](member-local-setup.md)。
2. 保留原 JWT 密钥和上传目录。旧部署如果仍用源码中的密钥，按[JWT 升级说明](jwt-configuration-and-upgrade.md)迁移到环境变量或私有配置；正式部署使用 `prod` 并显式配置密钥。
3. 在 `view` 目录执行 `npm ci`。前端 API 地址不同于本机默认端口时，保留自己的 `.env.local` 并修改 `VITE_API_BASE_URL`；部署前执行 `npm run build`。
4. 在 `api` 目录启动 `mvn spring-boot:run`（Java 21）；前端在 `view` 目录执行 `npm run dev`。IDEA 的后端工作目录使用 `api`。不要让旧后端占用 21090，也不要把旧 `dist` 当成新版部署。
5. 默认前端 `http://localhost:21091`，后端 `http://localhost:21090`。分别检查普通用户订单、卖家售后和管理员订单管理。正式服务器按原服务方式重启后端、部署新前端构建，并保留数据库连接和密钥配置。

## 更新后功能与检查

- 有限库存售罄后保持可查看；重新上架先确认完整信息，再等管理员审核。选择不限库存不等于已审核上架，待审核时买家暂不能查看当前商品内容。
- 普通售后从确认收货起七天；超期或时间缺失可申请管理员介入，期限前提交的申请继续处理。
- 卖家同意或拒绝都保存意见并交管理员审核；需退货的申请确认实物收回后才处理库存，仅退款不自动补库存。
- 商品改为不限库存后，旧订单按原扣库事实记录释放或退回数量，不往当前不限库存字段累加。
- 订单详情显示售后进度和时间线；失败时只显示一个中文提示，保留填写内容。

详细规则：[库存与重新上架](product-stock.md)、[售后与七天期限](after-sale-flow.md)。如果提示缺表、缺字段或数据库连接错误，先停止业务操作核对升级结果，不用导入完整快照补救。

本说明包含运行与升级步骤；测试报告、截图及本机凭据不随本次发布上传。

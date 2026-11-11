-- 校园二手交易平台：完整数据库快照（2026-11-11）
-- 来源：已验收稳定项目的数据库；包含表结构与当前业务数据。
-- 仅导入新建的空数据库！已有数据请先备份，不要直接覆盖。
-- 在 DataGrip 中选择自己的目标数据库后，完整执行本文件。
-- 无需另行执行旧迁移；不包含数据库连接凭据、JWT 密钥或上传图片文件。
-- 含本系统账号记录和联系方式等业务数据，请按课程演示用途保管。


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `address` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '地址主键ID',
  `user_id` int NOT NULL COMMENT '用户ID（关联user.id）',
  `receiver_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收货人姓名',
  `receiver_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收货人手机号',
  `province` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '省份',
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '城市',
  `district` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '区/县',
  `detail_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '详细地址（街道、门牌号等）',
  `postal_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '邮政编码',
  `is_default` tinyint(1) DEFAULT '0' COMMENT '是否为默认地址：1是，0否',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='用户收货地址表';
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `address` VALUES (1,2,'张三','12332321212','广东省','广州市','天河区','栗子街道100号','',0,'2026-06-21 21:09:25','2026-06-21 21:10:12'),(2,2,'李四','18798987676','广东省','广州市','白云区','街道1','',1,'2026-06-21 21:09:58','2026-06-21 21:10:03'),(3,2,'栗子','17089891212','广东省','广州市','天河区','栗子街道','',0,'2026-06-21 21:11:33','2026-06-21 21:11:33'),(4,2,'李莉莉','16787673432','广东省','广州市','天河区','栗子街道100号','',0,'2026-06-23 18:28:42','2026-06-23 18:28:42');
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '管理员主键ID',
  `base_user_id` int DEFAULT NULL COMMENT '基础用户信息主键ID，外键',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='管理员信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `admin` VALUES (1,1);
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_user` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '基础用户信息表主键ID',
  `account` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '账号',
  `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '密码',
  `avatar` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '头像',
  `username` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '用户名',
  `gender` tinyint(1) DEFAULT NULL COMMENT '性别（1：男；2-女）',
  `phone` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '手机号',
  `login_status` tinyint(1) DEFAULT NULL COMMENT '账号状态（0：正常；1：封号）',
  `create_time` datetime DEFAULT NULL COMMENT '注册时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='基础用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `base_user` VALUES (1,'admin','123456','/files/1402bb5f-d4e2-46cc-8651-3ba4aa181135.jpg','Metaphor',1,NULL,1,'2026-06-21 16:10:18'),(2,'test','123456','/files/320fb5d9-335a-454e-bb33-879676b79922.png','成员1',1,'',1,'2026-06-21 16:10:41'),(3,'test1','123456','/files/eb173723-2f4b-40e2-b41b-85f33535a32d.png','成员2',1,'',1,'2026-06-21 16:17:29'),(5,'test2','123456','/files/4ed2a6af-4949-468e-b2f3-44c294432c4e.png','成员3',1,'',1,'2026-06-22 17:19:35'),(6,'test3','123456','/files/7ec5e327-e702-4236-aa0e-94b2a5d26284.png','成员4',1,'',1,'2026-06-23 16:31:40'),(7,'ttt','123456','/files/avatars/7/9ce09823-3f47-40d1-8919-fb1c29dc3256.jpg','TT',1,NULL,1,'2026-10-27 10:32:14');
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `campus_verification` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `base_user_id` int unsigned NOT NULL,
  `request_id` char(36) COLLATE utf8mb4_bin NOT NULL,
  `school` varchar(100) COLLATE utf8mb4_bin NOT NULL,
  `student_number` varchar(32) COLLATE utf8mb4_bin NOT NULL,
  `provider` varchar(64) COLLATE utf8mb4_bin NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_bin NOT NULL,
  `evidence_reference` varchar(200) COLLATE utf8mb4_bin DEFAULT NULL,
  `requested_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `checked_at` datetime DEFAULT NULL,
  `expires_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_campus_verification_request` (`request_id`),
  KEY `idx_campus_verification_user` (`base_user_id`,`id`),
  CONSTRAINT `fk_verification_base_user` FOREIGN KEY (`base_user_id`) REFERENCES `base_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ck_campus_verification_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'VERIFIED',_utf8mb4'REJECTED',_utf8mb4'INVALIDATED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='校园身份认证申请；非法律实名认证';
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '分类主键ID',
  `parent_id` int DEFAULT '0' COMMENT '父分类ID，0表示顶级分类',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '分类名称',
  `sort` int DEFAULT '0' COMMENT '排序权重，数值越大越靠前',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态：1启用，0禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_parent_id` (`parent_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='商品分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `category` VALUES (1,0,'电子产品',10,1,'2026-06-21 18:43:07','2026-06-22 21:16:29'),(2,1,'手机',20,1,'2026-06-21 18:43:22','2026-06-22 21:16:29'),(3,1,'相机',50,1,'2026-06-21 18:47:41','2026-06-21 18:55:09'),(4,0,'出版物',60,1,'2026-06-21 18:48:11','2026-06-22 21:16:38'),(5,2,'vivo',30,1,'2026-06-21 18:54:45','2026-06-22 21:16:29'),(6,2,'华为',40,1,'2026-06-21 18:54:52','2026-06-22 21:16:29'),(7,4,'书籍',80,1,'2026-06-21 18:56:31','2026-06-22 21:16:29'),(8,4,'唱片',70,1,'2026-06-21 18:56:42','2026-06-22 21:16:29');
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息主键ID',
  `from_user_id` int NOT NULL COMMENT '发送者base_user_id',
  `to_user_id` int NOT NULL COMMENT '接收者base_user_id',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '消息内容',
  `message_type` tinyint(1) NOT NULL DEFAULT '1' COMMENT '消息类型：1文本，2表情，3图片',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '消息状态：0未读，1已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_from_user_id` (`from_user_id`) USING BTREE,
  KEY `idx_to_user_id` (`to_user_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  KEY `idx_from_to` (`from_user_id`,`to_user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='聊天消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `chat_message` VALUES (1,2,3,'你好哦',1,1,'2026-06-22 17:13:47'),(2,3,2,'在吗',1,1,'2026-06-22 17:14:00'),(3,3,2,'💝',1,1,'2026-06-22 17:14:05'),(4,3,2,'💛',1,1,'2026-06-22 17:14:07'),(6,3,2,'🌟',1,1,'2026-06-22 17:16:55'),(7,2,3,'1',1,1,'2026-06-22 17:17:52'),(8,2,3,'2',1,1,'2026-06-22 17:17:53'),(9,2,3,'3',1,1,'2026-06-22 17:17:54'),(10,2,3,'4',1,1,'2026-06-22 17:17:54'),(11,5,2,'12121',1,1,'2026-06-22 17:20:18'),(12,5,2,'3213123',1,1,'2026-06-22 17:20:20'),(13,5,2,'⭐🤛🤬',1,1,'2026-06-22 17:22:35'),(14,5,2,'👐',1,1,'2026-06-22 17:22:44'),(15,2,5,'你好',1,1,'2026-06-22 17:25:17'),(16,5,2,'在吗？',1,1,'2026-06-22 17:33:36'),(17,2,5,'你好',1,1,'2026-06-22 17:35:17'),(18,2,3,'2323',1,1,'2026-06-22 17:40:39'),(19,2,5,'432432',1,1,'2026-06-22 17:40:42'),(20,2,5,'4432432',1,1,'2026-06-22 17:46:09'),(21,2,5,'432423',1,1,'2026-06-22 17:46:15'),(22,5,2,'🤲',1,1,'2026-06-22 17:46:24'),(23,5,2,'🤝🥵',1,1,'2026-06-22 17:46:35'),(24,5,2,'/files/abf2331e-745b-4665-aec2-8a11dd39d609.png',3,1,'2026-06-22 17:46:37'),(25,2,3,'你好',1,1,'2026-06-22 22:36:42'),(26,2,5,'🙏',1,0,'2026-06-23 17:42:00'),(27,2,3,'nihao',1,1,'2026-06-23 17:42:07'),(28,3,2,'你好',1,1,'2026-06-23 18:35:24'),(29,2,3,'🤜',1,1,'2026-06-23 18:38:09'),(30,2,3,'你好1',1,1,'2026-06-25 17:11:41'),(31,2,3,'/files/33a942b0-4764-4353-b789-9b3bbe88f27a.png',3,1,'2026-06-25 17:16:28'),(32,2,3,'👊',1,1,'2026-06-25 17:16:33'),(33,3,2,'嘻嘻',1,1,'2026-06-25 17:17:14'),(34,2,3,'🤪',1,0,'2026-11-10 23:15:32');
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `item_similarity` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_id1` int NOT NULL COMMENT '商品ID(较小值)',
  `product_id2` int NOT NULL COMMENT '商品ID(较大值)',
  `similarity` decimal(12,10) NOT NULL COMMENT '余弦相似度(0~1)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '计算时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_product_pair` (`product_id1`,`product_id2`) USING BTREE,
  KEY `idx_product_id_1` (`product_id1`) USING BTREE,
  KEY `idx_product_id_2` (`product_id2`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1433 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='商品相似度表 - 协同过滤推荐系统';
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `item_similarity` VALUES (1407,1,17,0.9718501057,'2026-11-10 23:08:55'),(1408,1,5,0.2356000256,'2026-11-10 23:08:55'),(1409,1,8,0.9633249400,'2026-11-10 23:08:55'),(1410,1,9,0.9718501057,'2026-11-10 23:08:55'),(1411,1,10,0.6971216583,'2026-11-10 23:08:55'),(1412,1,28,0.1665943758,'2026-11-10 23:08:55'),(1413,1,13,0.8537961758,'2026-11-10 23:08:55'),(1414,8,17,0.9912279007,'2026-11-10 23:08:55'),(1415,9,17,1.0000000000,'2026-11-10 23:08:55'),(1416,10,17,0.5773502692,'2026-11-10 23:08:55'),(1417,13,17,0.7071067812,'2026-11-10 23:08:55'),(1418,3,8,0.1321637201,'2026-11-10 23:08:55'),(1419,3,10,0.5773502692,'2026-11-10 23:08:55'),(1420,3,28,0.7071067812,'2026-11-10 23:08:55'),(1421,5,10,0.5773502692,'2026-11-10 23:08:55'),(1422,5,28,0.7071067812,'2026-11-10 23:08:55'),(1423,5,13,0.7071067812,'2026-11-10 23:08:55'),(1424,8,9,0.9912279007,'2026-11-10 23:08:55'),(1425,8,10,0.6485904547,'2026-11-10 23:08:55'),(1426,8,28,0.0934538627,'2026-11-10 23:08:55'),(1427,8,13,0.7009039703,'2026-11-10 23:08:55'),(1428,9,10,0.5773502692,'2026-11-10 23:08:55'),(1429,9,13,0.7071067812,'2026-11-10 23:08:55'),(1430,10,28,0.8164965809,'2026-11-10 23:08:55'),(1431,10,13,0.8164965809,'2026-11-10 23:08:55'),(1432,13,28,0.5000000000,'2026-11-10 23:08:55');
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '订单主键ID',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '订单编号（唯一）',
  `product_id` int NOT NULL COMMENT '商品ID（关联product.id）',
  `buyer_id` int NOT NULL COMMENT '买家用户ID（关联user.id）',
  `seller_id` int NOT NULL COMMENT '卖家用户ID（关联user.id）',
  `address_id` int DEFAULT NULL COMMENT '收货地址ID（关联address.id，邮寄时必填）',
  `quantity` int DEFAULT '1' COMMENT '购买数量',
  `stock_deducted_quantity` int unsigned DEFAULT NULL COMMENT '订单实际扣减库存数量，0未扣减，NULL历史待核对',
  `unit_price` decimal(10,2) NOT NULL COMMENT '成交单价（下单时价格快照）',
  `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
  `shipping_fee` decimal(10,2) DEFAULT '0.00' COMMENT '运费',
  `pay_amount` decimal(10,2) NOT NULL COMMENT '实付金额（总金额+运费）',
  `pay_method` tinyint(1) DEFAULT NULL COMMENT '支付方式：1微信，2支付宝，3线下交易',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `order_status` tinyint(1) DEFAULT '0' COMMENT '订单状态：0待付款，1待发货，2待收货，3已完成，4已取消，5退款/售后',
  `ship_time` datetime DEFAULT NULL COMMENT '发货时间',
  `receive_time` datetime DEFAULT NULL COMMENT '收货时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '取消原因',
  `buyer_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '买家备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_order_no` (`order_no`) USING BTREE,
  KEY `idx_buyer_id` (`buyer_id`) USING BTREE,
  KEY `idx_seller_id` (`seller_id`) USING BTREE,
  KEY `idx_product_id` (`product_id`) USING BTREE,
  KEY `idx_order_status` (`order_status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `order` VALUES (1,'ORD1782110548659FAC81D6F',8,2,3,2,1,0,3599.00,3599.00,6.00,3605.00,NULL,NULL,4,NULL,NULL,'2026-06-22 14:44:16',NULL,'测试','2026-06-22 14:42:29','2026-06-22 14:42:28'),(2,'ORD17821106338520F50E86D',17,2,3,2,1,0,45.00,45.00,6.00,51.00,2,'2026-06-22 14:44:27',4,'2026-06-22 14:46:30',NULL,'2026-06-22 14:53:12','卖家同意退款','操场6:00交易','2026-06-22 14:43:54','2026-06-22 14:43:53'),(3,'ORD178211085888132EB8ABD',35,2,3,2,1,0,79.00,79.00,6.00,85.00,1,'2026-06-22 14:47:42',4,'2026-06-22 14:47:48','2026-06-22 14:47:54','2026-06-22 14:53:01','卖家同意退款',NULL,'2026-06-22 14:47:39','2026-06-22 14:47:38'),(4,'ORD1782111889628499E72E1',17,2,3,2,1,NULL,45.00,45.00,0.00,45.00,3,'2026-06-22 15:04:53',3,'2026-06-22 15:05:00','2026-06-22 15:05:07',NULL,NULL,NULL,'2026-06-22 15:04:50','2026-06-22 15:04:49'),(5,'ORD1782112463509FD7CEB29',8,2,3,2,1,0,3599.00,3599.00,6.00,3605.00,1,'2026-06-22 15:14:28',4,'2026-06-22 15:14:48',NULL,'2026-06-22 15:15:13','管理员同意退款','测试','2026-06-22 15:14:24','2026-06-22 15:14:23'),(6,'ORD178213221118161698BCC',8,2,3,2,1,NULL,3599.00,3599.00,6.00,3605.00,3,'2026-06-22 20:43:37',3,'2026-06-22 20:43:54','2026-06-22 20:44:06',NULL,NULL,NULL,'2026-06-22 20:43:31','2026-06-22 20:43:31'),(7,'ORD1782132289294F26E3936',11,2,3,3,1,NULL,14999.00,14999.00,6.00,15005.00,1,'2026-06-22 20:44:56',5,'2026-06-22 20:45:11',NULL,NULL,NULL,NULL,'2026-06-22 20:44:49','2026-06-22 20:44:49'),(8,'ORD1782133459149B3E6163F',8,2,3,2,1,NULL,3599.00,3599.00,6.00,3605.00,1,'2026-06-22 21:04:22',3,'2026-06-22 21:04:28','2026-06-22 21:04:32',NULL,NULL,'测试','2026-06-22 21:04:19','2026-06-22 21:04:19'),(9,'ORD178213516316888DE2955',8,2,3,2,1,0,3599.00,3599.00,6.00,3605.00,1,'2026-06-22 21:32:46',4,'2026-06-22 21:33:31',NULL,'2026-06-22 21:33:47','卖家同意退款',NULL,'2026-06-22 21:32:43','2026-06-22 21:32:43'),(10,'ORD1782203576474517760FE',20,6,3,NULL,1,NULL,199.00,199.00,0.00,199.00,2,'2026-06-23 16:33:00',1,NULL,NULL,NULL,NULL,NULL,'2026-06-23 16:32:56','2026-06-23 16:32:56'),(11,'ORD1782284635793AC902CD5',32,2,3,NULL,1,NULL,3999.00,3999.00,0.00,3999.00,2,'2026-06-24 15:04:00',3,'2026-06-24 15:04:20','2026-06-24 15:04:28',NULL,NULL,'线下','2026-06-24 15:03:56','2026-06-24 15:03:55'),(12,'ORD1782378023295FC9771DB',1,2,3,NULL,1,NULL,1999.00,1999.00,0.00,1999.00,1,'2026-06-25 17:00:59',3,'2026-06-25 17:01:53','2026-06-25 17:02:15',NULL,NULL,NULL,'2026-06-25 17:00:23','2026-06-25 17:00:23'),(13,'ORD17943236342710F69E9CB',52,2,7,NULL,1,1,520.00,520.00,0.00,520.00,3,'2026-11-10 23:13:58',3,'2026-11-10 23:14:25','2026-11-10 23:14:46',NULL,NULL,NULL,'2026-11-10 23:13:54','2026-11-10 23:13:54');
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '商品主键ID',
  `user_id` int NOT NULL COMMENT '发布用户ID（关联user.id）',
  `category_id` int NOT NULL COMMENT '商品分类ID（关联category.id）',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '商品标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '商品描述',
  `price` decimal(10,2) NOT NULL COMMENT '商品价格',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价（用于显示折扣）',
  `condition` tinyint(1) DEFAULT '1' COMMENT '新旧程度：1全新，2几乎全新，3有使用痕迹，4老旧',
  `trade_type` tinyint(1) DEFAULT '1' COMMENT '交易方式：1自提，2邮寄，3两者皆可',
  `status` tinyint(1) DEFAULT '0' COMMENT '审核状态：0待审核，1审核通过，2审核拒绝，3已下架',
  `view_count` int DEFAULT '0' COMMENT '浏览量',
  `want_count` int DEFAULT '0' COMMENT '想要数（冗余字段，提高查询效率）',
  `favorite_count` int DEFAULT '0' COMMENT '收藏数（冗余字段）',
  `review_count` int NOT NULL DEFAULT '0' COMMENT '评价数（冗余字段）',
  `rating` decimal(2,1) NOT NULL DEFAULT '0.0' COMMENT '平均评分（冗余字段，保留一位小数）',
  `audit_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '审核备注（拒绝原因等）',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `auditor_id` int DEFAULT NULL COMMENT '审核管理员ID（关联admin.id）',
  `stock` int DEFAULT NULL COMMENT '库存',
  `version` int DEFAULT NULL COMMENT '版本号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_category_id` (`category_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  KEY `idx_price` (`price`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=53 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='商品信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `product` VALUES (1,3,6,'华为手机','华为手机',1999.00,3000.00,1,1,1,41,2,1,1,5.0,NULL,'2026-06-25 17:06:53',1,0,2,'2026-06-21 20:30:08','2026-11-10 23:15:26'),(2,3,5,'vivo X300','vivo',3999.00,5000.00,2,2,0,11,2,2,0,0.0,NULL,NULL,NULL,1,1,'2026-06-21 20:37:08','2026-06-25 16:59:44'),(3,3,2,'iPhone 15 Pro Max 256GB','深空黑色，成色极新，带原装配件',6999.00,8999.00,2,2,1,157,24,46,0,0.0,NULL,'2026-06-21 10:30:00',1,1,1,'2026-06-15 08:00:00','2026-06-25 16:59:44'),(4,3,2,'小米13 Ultra 拍照旗舰','徕卡光学镜头，12+256G，使用2个月',4599.00,5999.00,2,3,1,91,12,34,0,0.0,NULL,'2026-06-20 14:20:00',1,1,1,'2026-06-14 10:30:00','2026-06-25 16:59:44'),(5,3,2,'OPPO Find X6 Pro','绿色，16+512G，几乎全新',4999.00,6999.00,2,1,1,71,8,24,0,0.0,NULL,'2026-06-19 09:15:00',1,1,1,'2026-06-13 16:45:00','2026-06-23 16:43:47'),(6,3,2,'荣耀Magic5 Pro','青海湖电池，12+256G，黑色',4299.00,5699.00,3,2,1,46,5,12,0,0.0,NULL,'2026-06-18 11:00:00',1,1,1,'2026-06-12 07:20:00','2026-10-27 10:14:35'),(7,2,5,'vivo X Fold2 折叠屏','12+512G，红色，使用1个月',8999.00,11999.00,2,3,1,246,45,67,0,0.0,'待审核','2026-06-22 21:14:55',1,1,1,'2026-06-20 22:00:00','2026-11-09 15:08:08'),(8,3,5,'vivo S17 Pro','12+512G，山海青，拍照神器',3599.00,4399.00,1,2,1,102,16,29,2,5.0,NULL,'2026-06-22 21:34:21',1,1,3,'2026-06-11 09:00:00','2026-06-25 16:58:36'),(9,1,6,'华为Mate 60 Pro','昆仑玻璃，卫星通话，白色',6999.00,8999.00,1,3,1,574,90,124,0,0.0,NULL,'2026-06-17 08:00:00',1,1,1,'2026-06-10 20:00:00','2026-11-10 23:11:31'),(10,2,6,'华为P60 Pro','洛可可白，12+512G，拍照天花板',4999.00,6988.00,2,1,1,131,37,59,0,0.0,NULL,'2026-06-16 13:45:00',1,1,1,'2026-06-09 11:30:00','2026-06-23 18:49:03'),(11,3,3,'索尼A7M4 微单相机','全画幅，3300万像素，带24-70镜头',14999.00,18999.00,2,2,1,243,56,78,0,0.0,NULL,'2026-06-15 10:00:00',1,1,1,'2026-06-08 14:00:00','2026-06-23 18:55:36'),(12,1,3,'佳能EOS R6 Mark II','专业级微单，二手极品成色',12999.00,16999.00,3,3,1,145,23,45,0,0.0,NULL,'2026-06-14 09:30:00',1,1,1,'2026-06-07 18:00:00','2026-06-22 21:26:49'),(13,2,3,'富士X-T5 银色','复古造型，4020万像素，几乎全新',10999.00,13999.00,2,1,1,93,14,36,0,0.0,NULL,'2026-06-13 14:00:00',1,1,1,'2026-06-06 12:00:00','2026-06-23 16:40:37'),(14,3,1,'三星S24 Ultra 顶配','1TB版本，钛金属边框，全新未拆',12999.00,15999.00,1,2,1,346,67,89,0,0.0,NULL,'2026-06-12 11:00:00',1,1,1,'2026-06-05 09:00:00','2026-06-22 21:26:49'),(15,1,1,'小米14 Pro 钛金属版','16+1TB，仅拆封激活，几乎全新',5999.00,7999.00,2,3,1,56,9,15,0,0.0,NULL,'2026-06-11 16:00:00',1,1,1,'2026-06-04 08:30:00','2026-06-22 21:26:49'),(16,2,7,'三体全集（典藏版）','精装未拆封，含三部曲',199.00,299.00,1,2,1,68,15,28,0,0.0,NULL,'2026-06-10 10:00:00',1,1,1,'2026-06-03 10:00:00','2026-06-23 15:26:34'),(17,3,7,'人类群星闪耀时','茨威格经典作品，几乎全新',45.00,68.00,2,3,1,45,4,8,1,5.0,NULL,'2026-06-09 09:00:00',1,1,1,'2026-06-02 15:00:00','2026-06-22 22:22:12'),(18,1,7,'百年孤独 精装版','马尔克斯经典，未开封',89.00,128.00,1,1,1,34,7,12,0,0.0,NULL,'2026-06-08 14:00:00',1,1,1,'2026-06-01 11:00:00','2026-06-22 21:26:49'),(19,2,8,'周杰伦《最伟大的作品》黑胶','限量版，全新未拆',399.00,599.00,1,2,1,89,23,45,0,0.0,NULL,'2026-06-07 16:30:00',1,1,1,'2026-05-31 09:00:00','2026-06-22 21:26:49'),(20,3,8,'Taylor Swift《Midnights》CD','豪华版，3CD+写真，几乎全新',199.00,299.00,2,3,3,47,9,17,0,0.0,NULL,'2026-06-06 13:00:00',1,0,2,'2026-05-30 14:00:00','2026-06-23 16:32:56'),(21,1,8,'古典音乐合集 贝多芬交响曲','DG公司经典录音，9CD套装',259.00,399.00,3,1,1,12,2,5,0,0.0,NULL,'2026-06-05 11:00:00',1,1,1,'2026-05-29 16:00:00','2026-06-22 21:26:49'),(22,2,4,'摄影入门教程套装','含3本摄影书籍+1本后期处理',159.00,239.00,2,2,1,28,6,10,0,0.0,NULL,'2026-06-04 10:30:00',1,1,1,'2026-05-28 08:00:00','2026-06-22 21:26:49'),(23,3,4,'世界名著10册套装','精装版，含红与黑、战争与和平等',399.00,599.00,1,3,1,57,12,24,0,0.0,NULL,'2026-06-03 09:00:00',1,1,1,'2026-05-27 13:00:00','2026-06-22 21:26:49'),(24,1,2,'一加12 顶配版','16+1TB，绿色，全新',4999.00,6499.00,1,2,1,34,6,11,0,0.0,NULL,'2026-06-02 15:30:00',1,1,1,'2026-05-26 10:00:00','2026-06-22 21:26:49'),(25,2,5,'vivo X100 Pro','16+512G，蓝色，蔡司镜头',5299.00,6999.00,2,3,1,78,15,29,0,0.0,NULL,'2026-06-01 12:00:00',1,1,1,'2026-05-25 17:00:00','2026-06-22 21:26:49'),(26,3,6,'华为nova 12 Ultra','12+512G，绿色，拍照出色',3999.00,5499.00,2,1,1,47,8,17,0,0.0,NULL,'2026-05-31 10:00:00',1,1,1,'2026-05-24 09:00:00','2026-06-22 21:26:49'),(27,1,2,'Redmi K70 Pro','16+1TB，白色，电竞旗舰',3299.00,4599.00,1,2,1,67,14,23,0,0.0,NULL,'2026-05-30 14:00:00',1,1,1,'2026-05-23 11:00:00','2026-06-22 21:26:49'),(28,2,3,'尼康Z8 旗舰微单','4571万像素，全新未拆',27999.00,32999.00,1,3,1,166,36,58,0,0.0,'正在审核中','2026-06-22 21:14:57',1,1,1,'2026-06-20 18:00:00','2026-06-25 16:56:42'),(29,3,3,'适马 50mm F1.4 镜头','Art系列，全新',6999.00,8999.00,1,2,1,23,5,9,0,0.0,NULL,'2026-05-29 16:00:00',1,1,1,'2026-05-22 14:00:00','2026-06-22 21:26:49'),(30,1,1,'MacBook Pro 14寸 M3 Pro','18GB内存，512GB SSD，深空灰',14999.00,18999.00,2,3,1,234,45,67,0,0.0,NULL,'2026-05-28 09:00:00',1,1,1,'2026-05-21 08:00:00','2026-06-22 21:26:49'),(31,2,1,'iPad Pro 12.9寸 M2','256GB，WiFi版，几乎全新',7999.00,10999.00,2,2,1,89,18,34,0,0.0,NULL,'2026-05-27 13:30:00',1,1,1,'2026-05-20 15:00:00','2026-06-22 21:26:49'),(32,3,1,'AirPods Max 蓝色','旗舰降噪耳机，几乎全新',3999.00,4999.00,2,1,3,57,12,23,1,1.0,NULL,'2026-05-26 10:00:00',1,0,2,'2026-05-19 10:00:00','2026-06-24 15:04:59'),(33,1,6,'华为GT 3 Pro 手表','钛金属版，全新',2999.00,3999.00,1,2,1,34,7,14,0,0.0,NULL,'2026-05-25 15:00:00',1,1,1,'2026-05-18 16:00:00','2026-06-22 21:26:49'),(34,2,5,'vivo TWS 4 无线耳机','旗舰降噪，几乎全新',399.00,599.00,2,3,1,23,4,8,0,0.0,NULL,'2026-05-24 11:00:00',1,1,1,'2026-05-17 09:00:00','2026-06-22 21:26:49'),(35,3,7,'芯片简史：从晶体管到AI','热门科技类书籍，全新',79.00,109.00,1,2,1,37,7,13,0,0.0,NULL,'2026-05-23 14:00:00',1,1,1,'2026-05-16 11:00:00','2026-06-25 16:54:50'),(36,1,7,'摄影构图艺术','全彩印刷，几乎全新',89.00,129.00,2,1,1,15,3,6,0,0.0,NULL,'2026-05-22 09:30:00',1,1,1,'2026-05-15 08:00:00','2026-06-22 21:26:49'),(37,2,8,'林俊杰《重拾_快乐》CD','全新未拆，签名版',199.00,299.00,1,2,1,45,10,18,0,0.0,NULL,'2026-05-21 16:00:00',1,1,1,'2026-05-14 13:00:00','2026-06-22 21:26:49'),(38,3,8,'圣诞快乐 劳伦斯先生 原声','坂本龙一经典作品，黑胶版',299.00,499.00,2,3,1,35,5,11,0,0.0,NULL,'2026-05-20 10:30:00',1,1,1,'2026-05-13 10:00:00','2026-06-22 21:26:49'),(39,1,2,'魅族21 Pro 白色','12+512G，白色，全新',3999.00,5299.00,1,2,1,23,4,9,0,0.0,NULL,'2026-05-19 13:00:00',1,1,1,'2026-05-12 15:00:00','2026-06-22 21:26:49'),(40,2,5,'vivo Y100 千元机','8+256G，金色，几乎全新',1299.00,1699.00,2,3,1,56,8,15,0,0.0,NULL,'2026-05-18 11:00:00',1,1,1,'2026-05-11 09:00:00','2026-06-22 21:26:49'),(41,3,6,'华为畅享60X','7000mAh大电池，几乎全新',1499.00,2199.00,3,1,1,34,6,12,0,0.0,NULL,'2026-05-17 15:30:00',1,1,1,'2026-05-10 14:00:00','2026-06-22 21:26:49'),(42,1,3,'松下GH6 视频微单','专业视频机，M4/3系统',10999.00,13999.00,2,2,1,56,9,17,0,0.0,NULL,'2026-05-16 10:00:00',1,1,1,'2026-05-09 12:00:00','2026-06-22 21:26:49'),(43,2,3,'佳能RF 24-105mm F4L','红圈镜头，全新',9999.00,12999.00,1,3,1,45,8,14,0,0.0,NULL,'2026-05-15 09:00:00',1,1,1,'2026-05-08 16:00:00','2026-06-22 21:26:49'),(44,3,1,'戴尔XPS 16 笔记本电脑','Ultra 9处理器，32GB内存，1TB SSD',15999.00,19999.00,2,3,2,67,11,22,0,0.0,'商品描述与实物不符','2026-06-18 16:00:00',1,1,1,'2026-05-07 10:00:00','2026-06-22 21:26:49'),(45,3,7,'经济学原理 第七版','曼昆经典教材，几乎全新',99.00,149.00,3,2,1,12,2,4,0,0.0,NULL,'2026-05-14 14:00:00',1,1,1,'2026-05-06 08:00:00','2026-06-25 16:59:51'),(46,3,7,'Python从入门到实践 第三版','编程畅销书，全新',79.00,109.00,1,1,1,78,15,28,0,0.0,NULL,'2026-05-13 11:30:00',1,1,1,'2026-05-05 13:00:00','2026-06-25 16:59:51'),(47,3,2,'索尼Xperia 1 V','4K屏幕，专业影像，几乎全新',8999.00,10999.00,2,2,1,36,5,10,0,0.0,'价格异常，待核实','2026-06-22 21:14:59',1,1,1,'2026-06-19 20:00:00','2026-06-25 16:59:51'),(48,3,5,'vivo Pad 3 Pro 平板','13寸大屏，天玑9300，几乎全新',2999.00,3999.00,2,3,1,23,4,8,0,0.0,NULL,'2026-05-12 15:00:00',1,1,1,'2026-05-04 11:00:00','2026-06-25 16:59:51'),(49,3,8,'莫扎特弦乐四重奏 全集','8CD套装，经典录音，几乎全新',399.00,599.00,3,1,1,8,1,3,0,0.0,NULL,'2026-05-11 10:00:00',1,1,1,'2026-05-03 14:00:00','2026-06-22 21:26:49'),(50,3,1,'Kindle Oasis 3 电子书','32GB，几乎全新，带保护套',2199.00,2999.00,2,2,1,34,7,13,0,0.0,NULL,'2026-05-10 09:00:00',1,1,1,'2026-05-02 16:00:00','2026-06-25 16:59:51'),(51,3,4,'中国国家地理 2025年合订本','12期完整合集，几乎全新',299.00,399.00,2,3,1,23,3,7,0,0.0,NULL,'2026-05-09 13:00:00',1,1,1,'2026-05-01 09:00:00','2026-06-25 16:59:51'),(52,7,1,'明日香','明日香限定图片',520.00,5200.00,1,1,3,2,2,0,1,5.0,NULL,'2026-11-10 23:12:48',1,0,2,'2026-11-10 23:12:26','2026-11-10 23:14:54');
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_favorite` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '收藏主键ID',
  `user_id` int NOT NULL COMMENT '用户ID（关联user.id）',
  `product_id` int NOT NULL COMMENT '商品ID（关联product.id）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_product` (`user_id`,`product_id`) USING BTREE,
  KEY `idx_product_id` (`product_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='商品收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `product_favorite` VALUES (4,3,2,'2026-06-22 14:16:06'),(5,2,2,'2026-06-23 15:27:04'),(6,2,13,'2026-06-23 16:24:52'),(7,2,10,'2026-06-23 16:24:57'),(8,2,20,'2026-06-23 16:25:04'),(9,6,1,'2026-06-23 16:32:07'),(10,6,28,'2026-06-23 16:32:24'),(11,6,10,'2026-06-23 16:32:37'),(12,6,13,'2026-06-23 16:33:13'),(13,2,9,'2026-06-23 16:33:27'),(14,6,5,'2026-06-23 16:42:00'),(15,5,28,'2026-06-23 16:42:39'),(16,5,8,'2026-06-23 16:42:47'),(17,5,3,'2026-06-23 16:42:53'),(18,5,10,'2026-06-23 16:42:58');
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_image` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '图片主键ID',
  `product_id` int NOT NULL COMMENT '商品ID（关联product.id）',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '图片URL地址',
  `is_cover` tinyint(1) DEFAULT '0' COMMENT '是否为封面图：1是，0否',
  `sort` int DEFAULT '0' COMMENT '排序权重',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_product_id` (`product_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=168 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='商品图册表';
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `product_image` VALUES (14,3,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(15,3,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(16,3,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(17,4,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(18,4,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(19,4,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(20,5,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(21,5,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(22,5,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(23,5,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,3,'2026-06-21 21:39:06'),(24,6,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(25,6,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(26,6,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(31,8,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(32,8,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(33,8,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(34,9,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(35,9,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(36,9,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(37,9,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,3,'2026-06-21 21:39:06'),(41,11,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(42,11,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(43,11,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(44,12,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(45,12,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(46,12,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(47,12,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,3,'2026-06-21 21:39:06'),(51,14,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(52,14,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(53,15,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(54,15,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(55,16,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(56,16,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(57,17,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(58,17,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(59,17,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(60,18,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(61,18,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(62,18,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(63,19,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(64,19,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(65,20,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(66,20,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(67,20,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(68,21,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(69,21,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(70,21,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(71,22,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(72,22,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(73,22,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(74,23,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(75,23,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(76,23,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(77,23,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,3,'2026-06-21 21:39:06'),(78,24,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(79,24,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(80,24,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(81,25,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(82,25,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(83,25,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(84,26,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(85,26,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(86,26,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(87,26,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,3,'2026-06-21 21:39:06'),(88,27,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(89,27,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(94,29,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(95,29,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(96,29,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(97,30,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(98,30,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(99,30,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(100,31,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(101,31,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(102,31,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(103,32,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(104,32,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(105,33,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(106,33,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(107,34,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(108,34,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(111,36,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(112,36,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(113,37,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(114,37,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(115,37,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(116,38,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(117,38,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(118,38,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(119,39,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(120,39,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(121,39,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(122,40,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(123,40,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(124,40,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(125,41,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(126,41,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(127,42,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(128,42,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(129,42,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,2,'2026-06-21 21:39:06'),(130,42,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,3,'2026-06-21 21:39:06'),(131,43,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(132,43,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(133,44,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(134,44,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',0,1,'2026-06-21 21:39:06'),(135,45,'/files/d323b8fc-bd79-4699-ab1f-05c5c0644e76.png',1,0,'2026-06-21 21:39:06'),(136,45,'/files/8dce78e3-8cca-47ea-9819-479d353c1e20.png',0,1,'2026-06-21 21:39:06'),(137,45,'/files/8dce78e3-8cca-47ea-9819-479d353c1e20.png',0,2,'2026-06-21 21:39:06'),(138,46,'/files/8dce78e3-8cca-47ea-9819-479d353c1e20.png',1,0,'2026-06-21 21:39:06'),(139,46,'/files/8dce78e3-8cca-47ea-9819-479d353c1e20.png',0,1,'2026-06-21 21:39:06'),(140,46,'/files/8dce78e3-8cca-47ea-9819-479d353c1e20.png',0,2,'2026-06-21 21:39:06'),(141,47,'/files/8dce78e3-8cca-47ea-9819-479d353c1e20.png',1,0,'2026-06-21 21:39:06'),(142,47,'/files/8dce78e3-8cca-47ea-9819-479d353c1e20.png',0,1,'2026-06-21 21:39:06'),(145,49,'/files/8dce78e3-8cca-47ea-9819-479d353c1e20.png',1,0,'2026-06-21 21:39:06'),(146,49,'/files/8dce78e3-8cca-47ea-9819-479d353c1e20.png',0,1,'2026-06-21 21:39:06'),(147,49,'/files/8dce78e3-8cca-47ea-9819-479d353c1e20.png',0,2,'2026-06-21 21:39:06'),(148,50,'/files/8dce78e3-8cca-47ea-9819-479d353c1e20.png',1,0,'2026-06-21 21:39:06'),(149,50,'/files/8dce78e3-8cca-47ea-9819-479d353c1e20.png',0,1,'2026-06-21 21:39:06'),(150,50,'/files/8dce78e3-8cca-47ea-9819-479d353c1e20.png',0,2,'2026-06-21 21:39:06'),(154,35,'/files/53a9359d-2912-4305-b78c-8929fb4fe020.png',1,1,'2026-06-22 15:01:18'),(155,35,'/files/ab53bbf6-7068-45ef-a9ad-489d6bcde9ea.png',0,2,'2026-06-22 15:01:18'),(157,2,'/files/0334d6f8-c49a-45d7-a5da-5ff81a4ca717.png',1,1,'2026-06-22 21:59:06'),(158,7,'/files/5b769f27-553a-4b9b-97cd-451b901e1905.png',1,1,'2026-06-22 22:00:19'),(159,7,'/files/2cd8ea1d-1602-4519-9a95-0cd96098e59e.png',0,2,'2026-06-22 22:00:19'),(160,13,'/files/151ba013-9c84-410b-bb71-bc378a108ef6.png',1,1,'2026-06-22 22:20:01'),(161,28,'/files/82a1ec92-0add-47a0-b2e6-d86697d21a53.png',1,1,'2026-06-22 22:23:55'),(162,51,'/files/d368e78b-faa0-41b0-acae-c0254c09d771.png',1,1,'2026-06-22 22:26:21'),(163,48,'/files/dee2a10a-9651-4597-b0b3-d76f7010c61e.png',1,1,'2026-06-23 14:48:36'),(164,1,'/files/11b44fd7-2203-4964-8ccd-094fedb7957d.png',1,1,'2026-06-23 14:54:48'),(165,1,'/files/507fc7ea-bf2a-4644-a022-bbf91079f9c0.png',0,2,'2026-06-23 14:54:48'),(166,10,'/files/e668fd6e-fd2a-4736-a953-b0308cb09001.png',1,1,'2026-06-23 16:24:11'),(167,52,'/files/products/7/3601f76b-1fe1-4ebd-a709-683e20904f09.jpg',1,0,'2026-11-10 23:12:26');
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_review` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '评价主键ID',
  `order_id` int NOT NULL COMMENT '订单ID（关联order.id，一个订单只能评价一次）',
  `product_id` int NOT NULL COMMENT '商品ID（关联product.id，冗余便于查询）',
  `buyer_id` int NOT NULL COMMENT '买家用户ID（关联base_user.id）',
  `seller_id` int NOT NULL COMMENT '卖家用户ID（关联base_user.id，冗余便于卖家查看）',
  `rating` tinyint(1) NOT NULL DEFAULT '5' COMMENT '评分：1-5星',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '评价内容',
  `reply_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '卖家回复内容',
  `reply_time` datetime DEFAULT NULL COMMENT '卖家回复时间',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态：1显示，0隐藏（管理员隐藏违规评价）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_order_id` (`order_id`) USING BTREE COMMENT '一个订单只能评价一次',
  KEY `idx_product_id` (`product_id`) USING BTREE,
  KEY `idx_buyer_id` (`buyer_id`) USING BTREE,
  KEY `idx_seller_id` (`seller_id`) USING BTREE,
  KEY `idx_rating` (`rating`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='商品评价表';
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `product_review` VALUES (1,4,17,2,3,5,'卖家很好！有问必答，态度优秀！','感谢小伙伴的支持！爱你哦！','2026-06-22 15:46:10',1,'2026-06-22 15:40:51','2026-06-22 15:48:13'),(2,6,8,2,3,5,'好好好',NULL,NULL,1,'2026-06-22 20:44:23','2026-06-22 20:44:22'),(3,8,8,2,3,5,'非常好的体验！',NULL,NULL,1,'2026-06-22 21:06:42','2026-06-22 21:06:41'),(4,11,32,2,3,1,'***，很不好！',NULL,NULL,1,'2026-06-24 15:05:00','2026-06-24 15:04:59'),(5,12,1,2,3,5,'非常好','谢谢亲哦！','2026-06-25 17:10:07',1,'2026-06-25 17:03:37','2026-11-09 15:06:05'),(6,13,52,2,7,5,'非常美丽',NULL,NULL,1,'2026-11-10 23:14:54','2026-11-10 23:14:54');
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_view` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '商品浏览表主键ID',
  `user_id` int DEFAULT NULL COMMENT '用户ID，外键',
  `product_id` int DEFAULT NULL COMMENT '商品ID，外键',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=177 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `product_view` VALUES (1,2,2,'2026-06-22 14:18:56'),(2,2,2,'2026-06-22 14:19:02'),(3,2,2,'2026-06-22 14:40:33'),(4,2,2,'2026-06-22 14:40:40'),(5,2,8,'2026-06-22 14:42:09'),(6,2,17,'2026-06-22 14:42:53'),(7,2,17,'2026-06-22 14:42:57'),(8,2,35,'2026-06-22 14:47:28'),(9,2,11,'2026-06-22 14:53:30'),(10,2,11,'2026-06-22 14:53:45'),(11,2,8,'2026-06-22 14:53:54'),(12,2,35,'2026-06-22 14:54:08'),(13,2,38,'2026-06-22 14:54:28'),(14,3,10,'2026-06-22 15:01:45'),(15,3,23,'2026-06-22 15:02:00'),(16,2,26,'2026-06-22 15:02:29'),(17,2,11,'2026-06-22 15:02:38'),(18,2,26,'2026-06-22 15:02:44'),(19,2,17,'2026-06-22 15:02:53'),(20,2,8,'2026-06-22 15:14:12'),(21,2,17,'2026-06-22 15:41:14'),(22,2,17,'2026-06-22 15:44:05'),(23,2,17,'2026-06-22 15:44:07'),(24,2,17,'2026-06-22 15:44:10'),(25,2,17,'2026-06-22 15:44:13'),(26,2,17,'2026-06-22 15:44:32'),(27,2,17,'2026-06-22 15:44:47'),(28,2,17,'2026-06-22 15:44:48'),(29,2,17,'2026-06-22 15:46:18'),(30,2,2,'2026-06-22 15:46:27'),(31,2,10,'2026-06-22 15:46:32'),(32,2,17,'2026-06-22 15:48:09'),(33,2,17,'2026-06-22 15:48:16'),(34,2,17,'2026-06-22 15:51:07'),(35,2,17,'2026-06-22 15:53:05'),(36,2,17,'2026-06-22 15:54:15'),(37,2,17,'2026-06-22 15:54:20'),(38,2,17,'2026-06-22 15:55:31'),(39,2,17,'2026-06-22 15:55:34'),(40,2,17,'2026-06-22 15:55:36'),(41,2,8,'2026-06-22 17:09:29'),(42,2,2,'2026-06-22 17:09:40'),(43,2,5,'2026-06-22 17:09:45'),(44,2,14,'2026-06-22 17:12:15'),(45,2,8,'2026-06-22 17:13:39'),(46,5,8,'2026-06-22 17:19:44'),(47,5,8,'2026-06-22 17:19:53'),(48,5,11,'2026-06-22 17:20:01'),(49,5,11,'2026-06-22 17:20:11'),(50,5,2,'2026-06-22 17:20:12'),(51,2,8,'2026-06-22 17:36:16'),(52,5,8,'2026-06-22 17:40:05'),(53,5,2,'2026-06-22 17:40:27'),(54,2,11,'2026-06-22 17:40:34'),(55,2,8,'2026-06-22 20:43:24'),(56,2,11,'2026-06-22 20:44:42'),(57,2,8,'2026-06-22 21:04:03'),(58,2,8,'2026-06-22 21:06:46'),(59,2,28,'2026-06-22 21:27:59'),(60,2,28,'2026-06-22 21:29:17'),(61,2,8,'2026-06-22 21:29:31'),(62,2,8,'2026-06-22 21:31:03'),(63,2,1,'2026-06-22 21:49:28'),(64,2,7,'2026-06-22 21:59:16'),(65,2,2,'2026-06-22 22:10:43'),(66,2,1,'2026-06-22 22:12:16'),(67,2,51,'2026-06-22 22:17:46'),(68,2,17,'2026-06-22 22:22:13'),(69,2,28,'2026-06-22 22:23:07'),(70,2,1,'2026-06-22 22:30:17'),(71,2,51,'2026-06-22 22:33:03'),(72,2,51,'2026-06-22 22:36:53'),(73,2,2,'2026-06-22 22:47:30'),(74,2,47,'2026-06-23 14:46:50'),(75,2,1,'2026-06-23 14:50:41'),(76,2,1,'2026-06-23 14:51:11'),(77,2,1,'2026-06-23 14:53:53'),(78,2,1,'2026-06-23 14:53:56'),(79,2,1,'2026-06-23 14:54:50'),(80,2,1,'2026-06-23 14:59:46'),(81,2,1,'2026-06-23 15:01:31'),(82,2,1,'2026-06-23 15:02:07'),(83,2,8,'2026-06-23 15:02:13'),(84,2,8,'2026-06-23 15:06:13'),(85,2,8,'2026-06-23 15:06:18'),(86,2,8,'2026-06-23 15:09:11'),(87,2,8,'2026-06-23 15:15:06'),(88,2,8,'2026-06-23 15:15:48'),(89,2,28,'2026-06-23 15:15:53'),(90,2,1,'2026-06-23 15:16:02'),(91,2,1,'2026-06-23 15:18:44'),(92,2,1,'2026-06-23 15:22:05'),(93,2,1,'2026-06-23 15:22:07'),(94,2,1,'2026-06-23 15:22:09'),(95,2,1,'2026-06-23 15:22:14'),(96,2,1,'2026-06-23 15:22:21'),(97,2,1,'2026-06-23 15:22:34'),(98,2,1,'2026-06-23 15:23:22'),(99,2,7,'2026-06-23 15:24:17'),(100,2,7,'2026-06-23 15:24:20'),(101,2,7,'2026-06-23 15:24:24'),(102,2,7,'2026-06-23 15:25:13'),(103,2,28,'2026-06-23 15:26:18'),(104,2,7,'2026-06-23 15:26:25'),(105,2,13,'2026-06-23 15:26:27'),(106,2,4,'2026-06-23 15:26:32'),(107,2,16,'2026-06-23 15:26:35'),(108,2,4,'2026-06-23 15:26:38'),(109,2,10,'2026-06-23 15:26:41'),(110,2,2,'2026-06-23 15:26:48'),(111,2,1,'2026-06-23 15:41:36'),(112,2,11,'2026-06-23 15:51:55'),(113,2,9,'2026-06-23 15:52:19'),(114,2,9,'2026-06-23 15:55:53'),(115,2,9,'2026-06-23 15:56:01'),(116,2,9,'2026-06-23 15:59:39'),(117,2,13,'2026-06-23 16:24:50'),(118,2,10,'2026-06-23 16:24:56'),(119,2,20,'2026-06-23 16:25:03'),(120,6,1,'2026-06-23 16:32:05'),(121,6,28,'2026-06-23 16:32:20'),(122,6,10,'2026-06-23 16:32:35'),(123,6,20,'2026-06-23 16:32:45'),(124,6,13,'2026-06-23 16:33:12'),(125,2,9,'2026-06-23 16:33:24'),(126,6,1,'2026-06-23 16:35:41'),(127,6,1,'2026-06-23 16:35:51'),(128,6,1,'2026-06-23 16:36:43'),(129,6,1,'2026-06-23 16:37:53'),(130,6,1,'2026-06-23 16:39:36'),(131,6,7,'2026-06-23 16:39:41'),(132,6,7,'2026-06-23 16:40:18'),(133,6,47,'2026-06-23 16:40:22'),(134,6,8,'2026-06-23 16:40:27'),(135,6,1,'2026-06-23 16:40:31'),(136,6,13,'2026-06-23 16:40:38'),(137,6,5,'2026-06-23 16:41:57'),(138,5,1,'2026-06-23 16:42:17'),(139,5,5,'2026-06-23 16:42:23'),(140,5,28,'2026-06-23 16:42:38'),(141,5,8,'2026-06-23 16:42:46'),(142,5,3,'2026-06-23 16:42:51'),(143,5,10,'2026-06-23 16:42:57'),(144,6,1,'2026-06-23 16:43:23'),(145,6,7,'2026-06-23 16:43:26'),(146,6,10,'2026-06-23 16:43:31'),(147,2,1,'2026-06-23 16:43:42'),(148,2,5,'2026-06-23 16:43:47'),(149,3,7,'2026-06-23 17:31:43'),(150,3,1,'2026-06-23 17:37:06'),(151,3,1,'2026-06-23 17:54:27'),(152,2,1,'2026-06-23 18:00:56'),(153,2,10,'2026-06-23 18:49:04'),(154,2,11,'2026-06-23 18:55:37'),(155,2,1,'2026-06-24 14:53:57'),(156,2,1,'2026-06-24 14:54:15'),(157,2,28,'2026-06-24 15:03:16'),(158,2,32,'2026-06-24 15:03:37'),(159,2,28,'2026-06-25 16:53:31'),(160,2,35,'2026-06-25 16:54:51'),(161,2,7,'2026-06-25 16:56:05'),(162,2,28,'2026-06-25 16:56:43'),(163,2,8,'2026-06-25 16:57:02'),(164,2,8,'2026-06-25 16:58:36'),(165,2,51,'2026-06-25 16:58:53'),(166,2,1,'2026-06-25 16:59:57'),(167,3,1,'2026-06-25 17:07:11'),(168,3,1,'2026-06-25 17:10:11'),(169,2,6,'2026-10-27 10:14:36'),(170,2,7,'2026-11-09 15:08:08'),(171,2,1,'2026-11-10 00:19:26'),(172,7,9,'2026-11-10 23:11:24'),(173,7,9,'2026-11-10 23:11:32'),(174,7,52,'2026-11-10 23:13:21'),(175,2,52,'2026-11-10 23:13:45'),(176,2,1,'2026-11-10 23:15:26');
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_want` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '想要记录主键ID',
  `user_id` int NOT NULL COMMENT '用户ID（关联user.id）',
  `product_id` int NOT NULL COMMENT '商品ID（关联product.id）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点击想要的时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_product` (`user_id`,`product_id`) USING BTREE,
  KEY `idx_product_id` (`product_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='商品想要记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `product_want` VALUES (1,2,2,'2026-06-22 14:12:30'),(2,3,2,'2026-06-22 14:16:04'),(3,2,13,'2026-06-23 16:24:53'),(4,2,10,'2026-06-23 16:24:58'),(5,2,20,'2026-06-23 16:25:05'),(6,6,1,'2026-06-23 16:32:07'),(7,6,28,'2026-06-23 16:32:24'),(8,6,10,'2026-06-23 16:32:37'),(9,6,13,'2026-06-23 16:33:14'),(10,2,9,'2026-06-23 16:33:27'),(11,5,28,'2026-06-23 16:42:40'),(12,5,8,'2026-06-23 16:42:48'),(13,5,3,'2026-06-23 16:42:53'),(14,5,10,'2026-06-23 16:42:59'),(15,2,1,'2026-06-24 14:54:16'),(16,7,52,'2026-11-10 23:13:22'),(17,2,52,'2026-11-10 23:13:47');
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_image` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '评价图片主键ID',
  `review_id` int NOT NULL COMMENT '评价ID（关联product_review.id）',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '图片URL地址',
  `sort` int DEFAULT '0' COMMENT '排序权重',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_review_id` (`review_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='评价图片表';
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `review_image` VALUES (1,1,'/files/59a05b1c-3ea8-48bc-8948-5f302c60ed33.png',0,'2026-06-22 15:40:51'),(2,1,'/files/7efd464d-6109-42aa-93ee-1647e02b07e2.png',1,'2026-06-22 15:40:51'),(3,2,'/files/8e15e01b-581e-44fc-b80f-71c25995c46e.png',0,'2026-06-22 20:44:23'),(4,2,'/files/acda1a96-eec3-4900-ab7b-7f62269d6e44.png',1,'2026-06-22 20:44:23'),(5,3,'/files/34d7e57c-c736-4435-a419-9ad19189c590.png',0,'2026-06-22 21:06:42'),(6,3,'/files/5ff0a557-20d1-4fe3-b200-b3b8c42bdba5.png',1,'2026-06-22 21:06:42'),(7,3,'/files/93199581-0949-41db-94fe-fc12abeec0e2.png',2,'2026-06-22 21:06:42'),(8,5,'/files/fe90d383-6321-407b-a683-a336026ab966.png',0,'2026-06-25 17:03:37'),(9,5,'/files/bded9012-6973-4d2c-bcbc-45c3296601ea.png',1,'2026-06-25 17:03:37');
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '用户表主键ID',
  `base_user_id` int DEFAULT NULL COMMENT '基础用户信息主键ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `user` VALUES (1,2),(2,3),(5,5),(6,6),(7,7);
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_campus_profile` (
  `base_user_id` int unsigned NOT NULL,
  `school` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '用户自填学校，不代表已认证',
  `college` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '用户自填学院，不代表已认证',
  `student_number` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '用户自填学号，仅本人和管理员可读取',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`base_user_id`),
  CONSTRAINT `fk_campus_profile_base_user` FOREIGN KEY (`base_user_id`) REFERENCES `base_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='个人校园资料，非学校身份认证';
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `user_campus_profile` VALUES (7,NULL,NULL,NULL,'2026-11-10 23:11:14');
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

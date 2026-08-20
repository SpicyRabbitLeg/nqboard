DROP DATABASE IF EXISTS `nqboard_quanta`;

CREATE DATABASE  `nqboard_quanta` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

USE nqboard_quanta;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for stock_basic
-- ----------------------------
DROP TABLE IF EXISTS `stock_basic`;
CREATE TABLE `stock_basic` (
   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
   `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
   `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
   `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
   `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
   `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '删除状态（0未删除、1删除）',
   `order_num` int NULL DEFAULT NULL COMMENT '排序字段',
   `remark`    varchar(500) null comment '备注',
   `ts_code` varchar(32) NOT NULL COMMENT '股票代码 000001.SZ',
   `symbol` varchar(16) DEFAULT NULL COMMENT '股票代码',
   `name` varchar(64) DEFAULT NULL COMMENT '股票名称',
   `area` varchar(32) DEFAULT NULL COMMENT '地域',
   `industry` varchar(64) DEFAULT NULL COMMENT '行业',
   `cnspell` varchar(32) DEFAULT NULL COMMENT '拼音缩写',
   `market` varchar(16) DEFAULT NULL COMMENT '市场类型：主板/创业板/科创板',
   `list_date` varchar(16) DEFAULT NULL COMMENT '上市日期',
   `act_name` varchar(128) DEFAULT NULL COMMENT '实控人名称',
   `act_ent_type` varchar(64) DEFAULT NULL COMMENT '实控人企业性质',
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_ts_code` (`ts_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tushare股票基础信息';

-- ----------------------------
-- Records of lock_table
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;

DROP DATABASE IF EXISTS `nqboard_device`;

CREATE DATABASE  `nqboard_device` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

USE nqboard_device;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for gen_datasource_conf
-- ----------------------------
DROP TABLE IF EXISTS `gen_datasource_conf`;
CREATE TABLE `gen_datasource_conf`  (
  `id` bigint NOT NULL COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '别名',
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'jdbcurl',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '密码',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标记',
  `ds_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据库类型',
  `conf_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '配置类型',
  `ds_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据库名称',
  `instance` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '实例',
  `port` int NULL DEFAULT NULL COMMENT '端口',
  `host` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '主机',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据源表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of gen_datasource_conf
-- ----------------------------
INSERT INTO `gen_datasource_conf` VALUES (1941836681422323711, 'taso_iot', 'jdbc:TAOS-WS://127.0.0.1:6041/nqboard?user=root&password=taosdata', 'root', 'rTq5lMc4C4fFUnHkZgnPVQ==', '2025-07-06 20:28:37', '2026-03-26 15:02:07', '0', 'taos', '1', '', '', 6041, '');

-- ----------------------------
-- Table structure for iot_alarm_record
-- ----------------------------
DROP TABLE IF EXISTS `iot_alarm_record`;
CREATE TABLE `iot_alarm_record`  (
  `id` bigint NOT NULL COMMENT 'id',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '删除状态（0未删除、1删除）',
  `device_id` bigint NOT NULL COMMENT '设备id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `order_num` int NULL DEFAULT NULL COMMENT '排序字段',
  `message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '描述信息',
  `handle_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '处理状态（0未处理、1处理、2不需要处理）',
  `img_str` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'base64图片',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '告警记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iot_alarm_record
-- ----------------------------

-- ----------------------------
-- Table structure for iot_category
-- ----------------------------
DROP TABLE IF EXISTS `iot_category`;
CREATE TABLE `iot_category`  (
  `id` bigint NOT NULL COMMENT '业务id',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '删除状态（0未删除、1删除）',
  `order_num` int NULL DEFAULT NULL COMMENT '排序字段',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '产品名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '产品分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iot_category
-- ----------------------------
INSERT INTO `iot_category` VALUES (1901610483258023937, 'spicy', '2025-03-17 20:24:04', 'spicy', '2025-03-17 20:26:08', '1', 1, '插座开关', '插座开关');
INSERT INTO `iot_category` VALUES (1901611035547197442, 'spicy', '2025-03-17 20:26:16', 'spicy', '2025-03-30 16:08:48', '0', 2, '插电开关', '插电开关');
INSERT INTO `iot_category` VALUES (1901611377882095618, 'spicy', '2025-03-17 20:27:37', 'spicy', '2025-03-17 20:27:46', '1', 1, '批完', '批完');
INSERT INTO `iot_category` VALUES (1901611399818305537, 'spicy', '2025-03-17 20:27:43', 'spicy', '2025-03-17 20:27:46', '1', 1, '213', '我');
INSERT INTO `iot_category` VALUES (1901611454923071490, 'spicy', '2025-03-17 20:27:56', 'spicy', '2025-03-17 20:28:43', '1', 1, '22', '22');
INSERT INTO `iot_category` VALUES (1901611466583236610, 'spicy', '2025-03-17 20:27:59', 'spicy', '2025-03-17 20:28:43', '1', 1, '123123', '21312');
INSERT INTO `iot_category` VALUES (1901611479644299266, 'spicy', '2025-03-17 20:28:02', 'spicy', '2025-03-17 20:28:43', '1', 1, '123123', '23123');
INSERT INTO `iot_category` VALUES (1901611497822412802, 'spicy', '2025-03-17 20:28:06', 'spicy', '2025-03-17 20:28:43', '1', 1, '驱蚊器', '12312');
INSERT INTO `iot_category` VALUES (1901611507779690498, 'spicy', '2025-03-17 20:28:08', 'spicy', '2025-03-17 20:28:43', '1', 1, '驱蚊器', '驱蚊器');
INSERT INTO `iot_category` VALUES (1901611519620210690, 'spicy', '2025-03-17 20:28:11', 'spicy', '2025-03-17 20:28:43', '1', 1, '驱蚊器', '驱蚊器');
INSERT INTO `iot_category` VALUES (1901611536003162113, 'spicy', '2025-03-17 20:28:15', 'spicy', '2025-03-17 20:28:43', '1', 1, '驱蚊器', '驱蚊器');
INSERT INTO `iot_category` VALUES (1901611561080905729, 'spicy', '2025-03-17 20:28:21', 'spicy', '2025-03-17 20:28:43', '1', 1, '驱蚊器', '驱蚊器');
INSERT INTO `iot_category` VALUES (1901611572556521473, 'spicy', '2025-03-17 20:28:24', 'spicy', '2025-03-17 20:28:43', '1', 1, '驱蚊器', '驱蚊器');
INSERT INTO `iot_category` VALUES (1901611583587540994, 'spicy', '2025-03-17 20:28:26', 'spicy', '2025-03-17 20:28:47', '1', 1, '驱蚊器', '驱蚊器');
INSERT INTO `iot_category` VALUES (1901611603711811585, 'spicy', '2025-03-17 20:28:31', 'spicy', '2025-03-17 20:28:47', '1', 1, '驱蚊器', '驱蚊器');
INSERT INTO `iot_category` VALUES (1901960870523604994, 'spicy', '2025-03-18 19:36:23', 'spicy', '2025-03-30 16:08:45', '0', 1, '传感器', '传感器');
INSERT INTO `iot_category` VALUES (1901960897643974657, 'spicy', '2025-03-18 19:36:29', NULL, NULL, '0', 3, '照明', '照明');
INSERT INTO `iot_category` VALUES (2037081246098112514, 'admin', '2026-03-26 16:16:31', NULL, NULL, '0', 4, '摄像头', '摄像头');

-- ----------------------------
-- Table structure for iot_device
-- ----------------------------
DROP TABLE IF EXISTS `iot_device`;
CREATE TABLE `iot_device`  (
  `id` bigint NOT NULL COMMENT 'id',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '删除状态（0未删除、1删除）',
  `order_num` int NULL DEFAULT NULL COMMENT '排序字段',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '设备名称',
  `longitude` decimal(9, 6) NULL DEFAULT NULL COMMENT '设备经度',
  `latitude` decimal(10, 6) NULL DEFAULT NULL COMMENT '设备纬度',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '设备状态（0关闭、1开启、2禁用）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `product_id` bigint NOT NULL COMMENT '产品id',
  `cron` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '定时任务cron表达式',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '地理位置',
  `driver_value` varchar(600) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '驱动连接json',
  `identifier` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '设备标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '设备表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iot_device
-- ----------------------------
INSERT INTO `iot_device` VALUES (2037087953079619586, 'admin', '2026-03-26 16:43:10', 'admin', '2026-03-26 16:44:33', '0', 0, '设备1', NULL, NULL, '0', '', 2037081329992581122, '0 0/1 * * * ?', 1, '中国', '{\"serverUri\":\"rtsp://localhost:8554\"}', 'she_bei_1');

-- ----------------------------
-- Table structure for iot_model
-- ----------------------------
DROP TABLE IF EXISTS `iot_model`;
CREATE TABLE `iot_model`  (
  `id` bigint NOT NULL COMMENT 'id',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '删除状态（0未删除、1删除）',
  `order_num` int NULL DEFAULT NULL COMMENT '排序字段',
  `product_id` bigint NOT NULL COMMENT '产品id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型名称',
  `identifier` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标识',
  `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类别（0读写、1只读、2只写）',
  `data_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据类型（integer、decimal、string、bool、array、enum）',
  `base_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '基础值',
  `proportional_coefficient` int NULL DEFAULT NULL COMMENT '比例系数',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述信息',
  `specs` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '正常值范围{min:0,max:100}',
  `unit` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '单位',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `itx_order`(`order_num` ASC) USING BTREE COMMENT '排序'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '物模型' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iot_model
-- ----------------------------
INSERT INTO `iot_model` VALUES (2037082215229800450, 'admin', '2026-03-26 16:20:22', 'admin', '2026-03-26 16:20:22', '0', NULL, 2037081329992581122, '电压', 'dianya', '0', 'int', '', 0, '电压数据', '{max:100,min:2}', NULL);
INSERT INTO `iot_model` VALUES (2037083268344041473, 'admin', '2026-03-26 16:24:33', 'admin', '2026-03-26 16:24:33', '0', 1, 2037081329992581122, '电费', 'dianfei', '2', 'double', '', NULL, '电费', '', '°');

-- ----------------------------
-- Table structure for iot_model_template
-- ----------------------------
DROP TABLE IF EXISTS `iot_model_template`;
CREATE TABLE `iot_model_template`  (
  `id` bigint NOT NULL COMMENT 'id',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '删除状态（0未删除、1删除）',
  `order_num` int NULL DEFAULT NULL COMMENT '排序字段',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型名称',
  `identifier` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标识',
  `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型（0读写、1只读、2只写）',
  `data_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据类型（integer、decimal、string、bool、array）',
  `base_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '基础值',
  `proportional_coefficient` int NULL DEFAULT NULL COMMENT '比例系数',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述信息',
  `specs` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '正常值范围 {min:0,max:100}',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '单位',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '通用物模型表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iot_model_template
-- ----------------------------
INSERT INTO `iot_model_template` VALUES (1913501835184844801, 'spicy', '2025-04-19 15:56:03', 'spicy', '2025-11-08 16:54:49', '0', NULL, '电压', 'dianya', '0', 'int', '', 0, '电压数据', '{max:100,min:2}', NULL);
INSERT INTO `iot_model_template` VALUES (1915994751581155330, 'spicy', '2025-04-26 13:02:01', 'spicy', '2025-11-08 16:54:49', '0', 1, '水压', 'water_sw', '0', 'double', '100', NULL, '水压', '{\"min\":\"10\",\"max\":\"200\"}', NULL);
INSERT INTO `iot_model_template` VALUES (1915996109348663297, 'spicy', '2025-04-26 13:07:25', 'spicy', '2025-11-08 16:54:49', '0', 1, '电费', 'dianfei', '2', 'double', '', NULL, '电费', '', '°');
INSERT INTO `iot_model_template` VALUES (1956725588123230209, 'spicy', '2025-08-16 22:31:49', 'spicy', '2025-11-08 16:54:49', '0', 1, '压力', 'yali', '0', 'long', '', NULL, '22', '', '');
INSERT INTO `iot_model_template` VALUES (1961803373128785921, 'spicy', '2025-08-30 22:49:07', 'spicy', '2025-11-08 16:54:49', '0', 4, '测试字符串', 'test_string', '0', 'string', '', NULL, '测试字符串', '', '');
INSERT INTO `iot_model_template` VALUES (1961803483246043137, 'spicy', '2025-08-30 22:49:34', 'spicy', '2025-11-08 16:54:49', '0', 1, '测试int', 'test_int', '0', 'int', '98', 2, '测试int', '{\"min\":\"100\",\"max\":\"500\"}', '');
INSERT INTO `iot_model_template` VALUES (1961803579006197761, 'spicy', '2025-08-30 22:49:56', 'spicy', '2025-11-08 16:54:49', '0', 1, '测试double', 'test_double', '0', 'double', '', NULL, '测试double', '', '');
INSERT INTO `iot_model_template` VALUES (1961803671083753474, 'spicy', '2025-08-30 22:50:18', 'spicy', '2025-11-08 16:54:49', '0', 1, '测试特殊符号', 'test_(teshu)', '0', 'string', '', NULL, '测试特殊符号', '', '');

-- ----------------------------
-- Table structure for iot_point
-- ----------------------------
DROP TABLE IF EXISTS `iot_point`;
CREATE TABLE `iot_point`  (
  `id` bigint NOT NULL COMMENT 'id',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '删除状态（0未删除、1删除）',
  `order_num` int NULL DEFAULT NULL COMMENT '排序字段',
  `protocol_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '协议连接信息具体值json',
  `device_id` bigint NULL DEFAULT NULL COMMENT '设备id',
  `model_id` bigint NULL DEFAULT NULL COMMENT '物模型id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '设备端点' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iot_point
-- ----------------------------

-- ----------------------------
-- Table structure for iot_product
-- ----------------------------
DROP TABLE IF EXISTS `iot_product`;
CREATE TABLE `iot_product`  (
  `id` bigint NOT NULL COMMENT 'id',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '删除状态（0未删除、1删除）',
  `order_num` int NULL DEFAULT NULL COMMENT '排序字段',
  `category_id` bigint NOT NULL COMMENT '分类id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '产品名称',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `protocol` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '连接协议',
  `auth_switch` tinyint NULL DEFAULT 0 COMMENT '授权开关',
  `auth_mode` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '授权模式（0简单、1加密）',
  `img` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图标',
  `identifier` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标识',
  `camera_types` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '镜头类型',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `itx_name`(`name` ASC) USING BTREE COMMENT '名称'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '产品表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of iot_product
-- ----------------------------
INSERT INTO `iot_product` VALUES (2037081329992581122, 'admin', '2026-03-26 16:16:51', 'admin', '2026-03-26 16:26:17', '0', 1, 2037081246098112514, 'AI摄像头', 'ai摄像头', 'RTSP', 0, '', '/api/admin/sys-file/s3demo/a13e4c1355c3434d8758f28b5eafeda8.png', 'a_i_she_xiang_tou', '[\"fire\"]');

-- ----------------------------
-- Table structure for undo_log
-- ----------------------------
DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log`  (
  `branch_id` bigint NOT NULL COMMENT 'branch transaction id',
  `xid` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'global transaction id',
  `context` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'undo_log context,such as serialization',
  `rollback_info` longblob NOT NULL COMMENT 'rollback info',
  `log_status` int NOT NULL COMMENT '0:normal status,1:defense status',
  `log_created` datetime(6) NOT NULL COMMENT 'create datetime',
  `log_modified` datetime(6) NOT NULL COMMENT 'modify datetime',
  UNIQUE INDEX `ux_undo_log`(`xid` ASC, `branch_id` ASC) USING BTREE,
  INDEX `ix_log_created`(`log_created` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AT transaction mode undo table' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of undo_log
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;

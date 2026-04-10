DROP DATABASE IF EXISTS `nqboard`;

CREATE DATABASE  `nqboard` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE `nqboard`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for QRTZ_BLOB_TRIGGERS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_BLOB_TRIGGERS`;
CREATE TABLE `QRTZ_BLOB_TRIGGERS`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `BLOB_DATA` blob NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `QRTZ_BLOB_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of QRTZ_BLOB_TRIGGERS
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_CALENDARS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_CALENDARS`;
CREATE TABLE `QRTZ_CALENDARS`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `CALENDAR_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `CALENDAR` blob NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `CALENDAR_NAME`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of QRTZ_CALENDARS
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_CRON_TRIGGERS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_CRON_TRIGGERS`;
CREATE TABLE `QRTZ_CRON_TRIGGERS`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `CRON_EXPRESSION` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `TIME_ZONE_ID` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `QRTZ_CRON_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of QRTZ_CRON_TRIGGERS
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_FIRED_TRIGGERS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_FIRED_TRIGGERS`;
CREATE TABLE `QRTZ_FIRED_TRIGGERS`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `ENTRY_ID` varchar(95) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `INSTANCE_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `FIRED_TIME` bigint NOT NULL,
  `SCHED_TIME` bigint NOT NULL,
  `PRIORITY` int NOT NULL,
  `STATE` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `JOB_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `JOB_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `IS_NONCONCURRENT` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `REQUESTS_RECOVERY` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`, `ENTRY_ID`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of QRTZ_FIRED_TRIGGERS
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_JOB_DETAILS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_JOB_DETAILS`;
CREATE TABLE `QRTZ_JOB_DETAILS`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `JOB_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `JOB_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `DESCRIPTION` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `JOB_CLASS_NAME` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `IS_DURABLE` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `IS_NONCONCURRENT` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `IS_UPDATE_DATA` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `REQUESTS_RECOVERY` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `JOB_DATA` blob NULL,
  PRIMARY KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of QRTZ_JOB_DETAILS
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_LOCKS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_LOCKS`;
CREATE TABLE `QRTZ_LOCKS`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `LOCK_NAME` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `LOCK_NAME`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of QRTZ_LOCKS
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_PAUSED_TRIGGER_GRPS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_PAUSED_TRIGGER_GRPS`;
CREATE TABLE `QRTZ_PAUSED_TRIGGER_GRPS`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_GROUP`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of QRTZ_PAUSED_TRIGGER_GRPS
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_SCHEDULER_STATE
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_SCHEDULER_STATE`;
CREATE TABLE `QRTZ_SCHEDULER_STATE`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `INSTANCE_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `LAST_CHECKIN_TIME` bigint NOT NULL,
  `CHECKIN_INTERVAL` bigint NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `INSTANCE_NAME`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of QRTZ_SCHEDULER_STATE
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_SIMPLE_TRIGGERS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_SIMPLE_TRIGGERS`;
CREATE TABLE `QRTZ_SIMPLE_TRIGGERS`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `REPEAT_COUNT` bigint NOT NULL,
  `REPEAT_INTERVAL` bigint NOT NULL,
  `TIMES_TRIGGERED` bigint NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `QRTZ_SIMPLE_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of QRTZ_SIMPLE_TRIGGERS
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_SIMPROP_TRIGGERS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_SIMPROP_TRIGGERS`;
CREATE TABLE `QRTZ_SIMPROP_TRIGGERS`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `STR_PROP_1` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `STR_PROP_2` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `STR_PROP_3` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `INT_PROP_1` int NULL DEFAULT NULL,
  `INT_PROP_2` int NULL DEFAULT NULL,
  `LONG_PROP_1` bigint NULL DEFAULT NULL,
  `LONG_PROP_2` bigint NULL DEFAULT NULL,
  `DEC_PROP_1` decimal(13, 4) NULL DEFAULT NULL,
  `DEC_PROP_2` decimal(13, 4) NULL DEFAULT NULL,
  `BOOL_PROP_1` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `BOOL_PROP_2` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `QRTZ_SIMPROP_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of QRTZ_SIMPROP_TRIGGERS
-- ----------------------------

-- ----------------------------
-- Table structure for QRTZ_TRIGGERS
-- ----------------------------
DROP TABLE IF EXISTS `QRTZ_TRIGGERS`;
CREATE TABLE `QRTZ_TRIGGERS`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `JOB_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `JOB_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `DESCRIPTION` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `NEXT_FIRE_TIME` bigint NULL DEFAULT NULL,
  `PREV_FIRE_TIME` bigint NULL DEFAULT NULL,
  `PRIORITY` int NULL DEFAULT NULL,
  `TRIGGER_STATE` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `TRIGGER_TYPE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `START_TIME` bigint NOT NULL,
  `END_TIME` bigint NULL DEFAULT NULL,
  `CALENDAR_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `MISFIRE_INSTR` smallint NULL DEFAULT NULL,
  `JOB_DATA` blob NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  INDEX `SCHED_NAME`(`SCHED_NAME` ASC, `JOB_NAME` ASC, `JOB_GROUP` ASC) USING BTREE,
  CONSTRAINT `QRTZ_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) REFERENCES `QRTZ_JOB_DETAILS` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of QRTZ_TRIGGERS
-- ----------------------------

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
-- Table structure for gen_field_type
-- ----------------------------
DROP TABLE IF EXISTS `gen_field_type`;
CREATE TABLE `gen_field_type`  (
  `id` bigint NOT NULL COMMENT '主键',
  `column_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段类型',
  `attr_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '属性类型',
  `package_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '属性包名',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标记',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `column_type`(`column_type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字段类型管理' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of gen_field_type
-- ----------------------------
INSERT INTO `gen_field_type` VALUES (1, 'datetime', 'LocalDateTime', 'java.time.LocalDateTime', '2023-02-06 08:45:10', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (2, 'date', 'LocalDate', 'java.time.LocalDate', '2023-02-06 08:45:10', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (3, 'tinyint', 'Integer', NULL, '2023-02-06 08:45:11', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (4, 'smallint', 'Integer', NULL, '2023-02-06 08:45:11', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (5, 'mediumint', 'Integer', NULL, '2023-02-06 08:45:11', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (6, 'int', 'Integer', NULL, '2023-02-06 08:45:11', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (7, 'integer', 'Integer', NULL, '2023-02-06 08:45:11', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (8, 'bigint', 'Long', NULL, '2023-02-06 08:45:11', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (9, 'float', 'Float', NULL, '2023-02-06 08:45:11', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (10, 'double', 'Double', NULL, '2023-02-06 08:45:11', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (11, 'decimal', 'BigDecimal', 'java.math.BigDecimal', '2023-02-06 08:45:11', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (12, 'bit', 'Boolean', NULL, '2023-02-06 08:45:11', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (13, 'char', 'String', NULL, '2023-02-06 08:45:11', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (14, 'varchar', 'String', NULL, '2023-02-06 08:45:11', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (15, 'tinytext', 'String', NULL, '2023-02-06 08:45:11', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (16, 'text', 'String', NULL, '2023-02-06 08:45:11', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (17, 'mediumtext', 'String', NULL, '2023-02-06 08:45:11', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (18, 'longtext', 'String', NULL, '2023-02-06 08:45:11', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (19, 'timestamp', 'LocalDateTime', 'java.time.LocalDateTime', '2023-02-06 08:45:11', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (20, 'NUMBER', 'Integer', NULL, '2023-02-06 08:45:11', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (21, 'BINARY_INTEGER', 'Integer', NULL, '2023-02-06 08:45:12', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (22, 'BINARY_FLOAT', 'Float', NULL, '2023-02-06 08:45:12', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (23, 'BINARY_DOUBLE', 'Double', NULL, '2023-02-06 08:45:12', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (24, 'VARCHAR2', 'String', NULL, '2023-02-06 08:45:12', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (25, 'NVARCHAR', 'String', NULL, '2023-02-06 08:45:12', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (26, 'NVARCHAR2', 'String', NULL, '2023-02-06 08:45:12', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (27, 'CLOB', 'String', NULL, '2023-02-06 08:45:12', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (28, 'int8', 'Long', NULL, '2023-02-06 08:45:12', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (29, 'int4', 'Integer', NULL, '2023-02-06 08:45:12', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (30, 'int2', 'Integer', NULL, '2023-02-06 08:45:12', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (31, 'numeric', 'BigDecimal', 'java.math.BigDecimal', '2023-02-06 08:45:12', NULL, NULL, NULL, '0');
INSERT INTO `gen_field_type` VALUES (32, 'json', 'String', NULL, '2023-02-06 08:45:12', NULL, NULL, NULL, '0');

-- ----------------------------
-- Table structure for gen_group
-- ----------------------------
DROP TABLE IF EXISTS `gen_group`;
CREATE TABLE `gen_group`  (
  `id` bigint NOT NULL,
  `group_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分组名称',
  `group_desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分组描述',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改人',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标记',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '模板分组' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of gen_group
-- ----------------------------

-- ----------------------------
-- Table structure for gen_table
-- ----------------------------
DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table`  (
  `id` bigint NOT NULL,
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '表名',
  `class_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类名',
  `db_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据库类型',
  `table_comment` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '说明',
  `author` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '作者',
  `email` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `package_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '项目包名',
  `version` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '项目版本号',
  `i18n` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '是否生成带有i18n 0 不带有 1带有',
  `style` bigint NULL DEFAULT NULL COMMENT '代码风格',
  `child_table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '子表名称',
  `main_field` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '主表关联键',
  `child_field` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '子表关联键',
  `generator_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '生成方式  0：zip压缩包   1：自定义目录',
  `backend_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '后端生成路径',
  `frontend_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '前端生成路径',
  `module_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '模块名',
  `function_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '功能名',
  `form_layout` tinyint NULL DEFAULT NULL COMMENT '表单布局  1：一列   2：两列',
  `ds_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据源ID',
  `baseclass_id` bigint NULL DEFAULT NULL COMMENT '基类ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `table_name`(`table_name` ASC, `ds_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '代码生成表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of gen_table
-- ----------------------------

-- ----------------------------
-- Table structure for gen_table_column
-- ----------------------------
DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column`  (
  `id` bigint NOT NULL,
  `ds_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据源名称',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '表名称',
  `field_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段名称',
  `field_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段类型',
  `field_comment` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字段说明',
  `attr_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '属性名',
  `attr_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '属性类型',
  `package_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '属性包名',
  `sort` int NULL DEFAULT NULL COMMENT '排序',
  `auto_fill` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '自动填充  DEFAULT、INSERT、UPDATE、INSERT_UPDATE',
  `primary_pk` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '主键 0：否  1：是',
  `base_field` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '基类字段 0：否  1：是',
  `form_item` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '表单项 0：否  1：是',
  `form_required` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '表单必填 0：否  1：是',
  `form_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '表单类型',
  `form_validator` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '表单效验',
  `grid_item` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '列表项 0：否  1：是',
  `grid_sort` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '列表排序 0：否  1：是',
  `query_item` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '查询项 0：否  1：是',
  `query_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '查询方式',
  `query_form_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '查询表单类型',
  `field_dict` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典类型',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '代码生成表字段' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of gen_table_column
-- ----------------------------

-- ----------------------------
-- Table structure for gen_template
-- ----------------------------
DROP TABLE IF EXISTS `gen_template`;
CREATE TABLE `gen_template`  (
  `id` bigint NOT NULL COMMENT '主键',
  `template_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板名称',
  `generator_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板路径',
  `template_desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板描述',
  `template_code` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板代码',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标记',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '模板' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of gen_template
-- ----------------------------

-- ----------------------------
-- Table structure for gen_template_group
-- ----------------------------
DROP TABLE IF EXISTS `gen_template_group`;
CREATE TABLE `gen_template_group`  (
  `group_id` bigint NOT NULL COMMENT '分组id',
  `template_id` bigint NOT NULL COMMENT '模板id',
  PRIMARY KEY (`group_id`, `template_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '模板分组关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of gen_template_group
-- ----------------------------

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部门名称',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '父级部门ID',
  PRIMARY KEY (`dept_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '部门管理' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES (1, '总裁办', 1, 'admin', 'admin', '2023-04-03 13:04:47', '2023-04-03 13:07:49', '0', 0);
INSERT INTO `sys_dept` VALUES (2, '技术部', 2, 'admin', 'admin', '2023-04-03 13:04:47', '2023-04-03 13:04:47', '0', 1);
INSERT INTO `sys_dept` VALUES (3, '市场部', 3, 'admin', 'admin', '2023-04-03 13:04:47', '2023-04-03 13:04:47', '0', 1);
INSERT INTO `sys_dept` VALUES (4, '销售部', 4, 'admin', 'admin', '2023-04-03 13:04:47', '2023-04-03 13:04:47', '0', 1);
INSERT INTO `sys_dept` VALUES (5, '财务部', 5, 'admin', 'admin', '2023-04-03 13:04:47', '2023-04-03 13:04:47', '0', 1);
INSERT INTO `sys_dept` VALUES (6, '人事行政部', 6, 'admin', 'admin', '2023-04-03 13:04:47', '2023-04-03 13:53:36', '1', 1);
INSERT INTO `sys_dept` VALUES (7, '研发部', 7, 'admin', 'admin', '2023-04-03 13:04:47', '2023-04-03 13:04:47', '0', 2);
INSERT INTO `sys_dept` VALUES (8, 'UI设计部', 11, 'admin', 'admin', '2023-04-03 13:04:47', '2023-04-03 13:04:47', '0', 7);
INSERT INTO `sys_dept` VALUES (9, '产品部', 12, 'admin', 'admin', '2023-04-03 13:04:47', '2023-04-03 13:04:47', '0', 2);
INSERT INTO `sys_dept` VALUES (10, '渠道部', 13, 'admin', 'admin', '2023-04-03 13:04:47', '2023-04-03 13:04:47', '0', 3);
INSERT INTO `sys_dept` VALUES (11, '推广部', 14, 'admin', 'admin', '2023-04-03 13:04:47', '2023-04-03 13:04:47', '0', 3);
INSERT INTO `sys_dept` VALUES (12, '客服部', 15, 'admin', 'admin', '2023-04-03 13:04:47', '2023-04-03 13:04:47', '0', 4);
INSERT INTO `sys_dept` VALUES (13, '财务会计部', 16, 'admin', 'admin', '2023-04-03 13:04:47', '2023-04-03 13:04:47', '0', 5);
INSERT INTO `sys_dept` VALUES (14, '审计风控部', 17, 'admin', 'admin', '2023-04-03 13:04:47', '2023-04-03 14:06:57', '0', 5);

-- ----------------------------
-- Table structure for sys_dict
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict`  (
  `id` bigint NOT NULL COMMENT '编号',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典类型',
  `description` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remarks` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注信息',
  `system_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '系统标志',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `sys_dict_del_flag`(`del_flag` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict
-- ----------------------------
INSERT INTO `sys_dict` VALUES (1, 'log_type', '日志类型', ' ', ' ', '2019-03-19 11:06:44', '2019-03-19 11:06:44', '异常、正常', '1', '0');
INSERT INTO `sys_dict` VALUES (2, 'social_type', '社交登录', ' ', ' ', '2019-03-19 11:09:44', '2019-03-19 11:09:44', '微信、QQ', '1', '0');
INSERT INTO `sys_dict` VALUES (3, 'job_type', '定时任务类型', ' ', ' ', '2019-03-19 11:22:21', '2019-03-19 11:22:21', 'quartz', '1', '0');
INSERT INTO `sys_dict` VALUES (4, 'job_status', '定时任务状态', ' ', ' ', '2019-03-19 11:24:57', '2019-03-19 11:24:57', '发布状态、运行状态', '1', '0');
INSERT INTO `sys_dict` VALUES (5, 'job_execute_status', '定时任务执行状态', ' ', ' ', '2019-03-19 11:26:15', '2019-03-19 11:26:15', '正常、异常', '1', '0');
INSERT INTO `sys_dict` VALUES (6, 'misfire_policy', '定时任务错失执行策略', ' ', ' ', '2019-03-19 11:27:19', '2019-03-19 11:27:19', '周期', '1', '0');
INSERT INTO `sys_dict` VALUES (7, 'gender', '性别', ' ', ' ', '2019-03-27 13:44:06', '2019-03-27 13:44:06', '微信用户性别', '1', '0');
INSERT INTO `sys_dict` VALUES (8, 'subscribe', '订阅状态', ' ', ' ', '2019-03-27 13:48:33', '2019-03-27 13:48:33', '公众号订阅状态', '1', '0');
INSERT INTO `sys_dict` VALUES (9, 'response_type', '回复', ' ', ' ', '2019-03-28 21:29:21', '2019-03-28 21:29:21', '微信消息是否已回复', '1', '0');
INSERT INTO `sys_dict` VALUES (10, 'param_type', '参数配置', ' ', ' ', '2019-04-29 18:20:47', '2019-04-29 18:20:47', '检索、原文、报表、安全、文档、消息、其他', '1', '0');
INSERT INTO `sys_dict` VALUES (11, 'status_type', '租户状态', ' ', ' ', '2019-05-15 16:31:08', '2019-05-15 16:31:08', '租户状态', '1', '0');
INSERT INTO `sys_dict` VALUES (12, 'dict_type', '字典类型', ' ', ' ', '2019-05-16 14:16:20', '2019-05-16 14:20:16', '系统类不能修改', '1', '0');
INSERT INTO `sys_dict` VALUES (13, 'channel_type', '支付类型', ' ', ' ', '2019-05-16 14:16:20', '2019-05-16 14:20:16', '系统类不能修改', '1', '0');
INSERT INTO `sys_dict` VALUES (14, 'grant_types', '授权类型', ' ', ' ', '2019-08-13 07:34:10', '2019-08-13 07:34:10', NULL, '1', '0');
INSERT INTO `sys_dict` VALUES (15, 'style_type', '前端风格', ' ', ' ', '2020-02-07 03:49:28', '2020-02-07 03:50:40', '0-Avue 1-element', '1', '0');
INSERT INTO `sys_dict` VALUES (16, 'captcha_flag_types', '验证码开关', ' ', ' ', '2020-11-18 06:53:25', '2020-11-18 06:53:25', '是否校验验证码', '1', '0');
INSERT INTO `sys_dict` VALUES (17, 'enc_flag_types', '前端密码加密', ' ', ' ', '2020-11-18 06:54:44', '2020-11-18 06:54:44', '前端密码是否加密传输', '1', '0');
INSERT INTO `sys_dict` VALUES (18, 'lock_flag', '用户状态', 'admin', ' ', '2023-02-01 16:55:31', NULL, NULL, '1', '0');
INSERT INTO `sys_dict` VALUES (19, 'ds_config_type', '数据连接类型', 'admin', ' ', '2023-02-06 18:36:59', NULL, NULL, '1', '0');
INSERT INTO `sys_dict` VALUES (20, 'common_status', '通用状态', 'admin', ' ', '2023-02-09 11:02:08', NULL, NULL, '1', '0');
INSERT INTO `sys_dict` VALUES (21, 'app_social_type', 'app社交登录', 'admin', ' ', '2023-02-10 11:11:06', NULL, 'app社交登录', '1', '0');
INSERT INTO `sys_dict` VALUES (22, 'yes_no_type', '是否', 'admin', ' ', '2023-02-20 23:25:04', NULL, NULL, '1', '0');
INSERT INTO `sys_dict` VALUES (23, 'repType', '微信消息类型', 'admin', ' ', '2023-02-24 15:08:25', NULL, NULL, '0', '0');
INSERT INTO `sys_dict` VALUES (24, 'leave_status', '请假状态', 'admin', ' ', '2023-03-02 22:50:15', NULL, NULL, '0', '0');
INSERT INTO `sys_dict` VALUES (25, 'schedule_type', '日程类型', 'admin', ' ', '2023-03-06 14:49:18', NULL, NULL, '0', '0');
INSERT INTO `sys_dict` VALUES (26, 'schedule_status', '日程状态', 'admin', ' ', '2023-03-06 14:52:57', NULL, NULL, '0', '0');
INSERT INTO `sys_dict` VALUES (27, 'ds_type', '代码生成器支持的数据库类型', 'admin', ' ', '2023-03-12 09:57:59', NULL, NULL, '1', '0');
INSERT INTO `sys_dict` VALUES (1905889865141186561, 'data_type', '数据类型', 'spicy', ' ', '2025-03-29 15:48:48', NULL, '数据类型', '1', '0');
INSERT INTO `sys_dict` VALUES (1905891040976564226, 'read_write_type', '读写类别', 'spicy', ' ', '2025-03-29 15:53:29', NULL, '读写类别', '0', '0');
INSERT INTO `sys_dict` VALUES (1907990469833400321, 'protocol', '连接协议', 'spicy', ' ', '2025-04-04 10:55:51', NULL, '连接协议：MQTT、Modbus-TCP、OPC-ua.....', '0', '0');
INSERT INTO `sys_dict` VALUES (1908006267369037826, 'protocol_MQTT', 'mqtt协议', 'spicy', 'spicy', '2025-04-04 11:58:38', '2025-09-14 13:22:02', 'mqtt协议具体入参', '0', '1');
INSERT INTO `sys_dict` VALUES (1908006877564772353, 'protocol_MODBUS_TCP', 'modbus tcp协议', 'spicy', 'spicy', '2025-04-04 12:01:03', '2025-09-14 13:21:52', '协议具体入参', '0', '1');
INSERT INTO `sys_dict` VALUES (1908007297460740097, 'protocol_OPC_UA', 'opc ua 协议', 'spicy', 'spicy', '2025-04-04 12:02:43', '2025-09-14 13:21:55', 'opc ua 协议具体入参', '0', '1');
INSERT INTO `sys_dict` VALUES (1917876753079066626, 'device_status', '设备状态', 'spicy', ' ', '2025-05-01 17:40:25', NULL, '设备状态（0关闭、1开启、2禁用）', '0', '0');
INSERT INTO `sys_dict` VALUES (1919030963073974274, 'cron', 'corn表达式', 'spicy', ' ', '2025-05-04 22:06:50', NULL, 'corn表达式', '0', '0');
INSERT INTO `sys_dict` VALUES (1921092719615176706, 'function_code', '功能码', 'spicy', ' ', '2025-05-10 14:39:31', NULL, '功能码', '0', '0');
INSERT INTO `sys_dict` VALUES (1949119522925461505, 'device_online_status', '设备在线状态', 'spicy', 'spicy', '2025-07-26 22:48:02', '2025-07-26 23:33:57', '设备在线状态', '0', '1');
INSERT INTO `sys_dict` VALUES (1971955213640265730, 'listener_type', '监听类型', 'spicy', ' ', '2025-09-27 23:08:55', NULL, '监听类型', '0', '0');
INSERT INTO `sys_dict` VALUES (1971956236517765121, 'listener_value_type', '监听值类型', 'spicy', ' ', '2025-09-27 23:12:59', NULL, '监听值类型', '0', '0');
INSERT INTO `sys_dict` VALUES (1971957097356718082, 'listener_event_type', '监听事件类型', 'spicy', 'spicy', '2025-09-27 23:16:24', '2025-09-27 23:18:28', '监听事件类型', '0', '1');
INSERT INTO `sys_dict` VALUES (1971957456481415170, 'listener_event_task_type', '监听任务事件类型', 'spicy', 'spicy', '2025-09-27 23:17:49', NULL, '监听任务事件类型', '0', '0');
INSERT INTO `sys_dict` VALUES (1971957589461823490, 'listener_event_exec_type', '监听任务执行类型', 'spicy', 'spicy', '2025-09-27 23:18:21', NULL, '监听任务执行类型', '0', '0');
INSERT INTO `sys_dict` VALUES (1987103096611487746, 'flow_user_type', '流程用户类型', 'spicy', ' ', '2025-11-08 18:21:11', NULL, 'flow_user_type', '0', '0');
INSERT INTO `sys_dict` VALUES (2037080792068886530, 'ai_cammer_type', 'AI监控类型', 'admin', NULL, '2026-03-26 16:14:43', NULL, 'AI监控类型', '0', '0');

-- ----------------------------
-- Table structure for sys_dict_item
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_item`;
CREATE TABLE `sys_dict_item`  (
  `id` bigint NOT NULL COMMENT '编号',
  `dict_id` bigint NOT NULL COMMENT '字典ID',
  `item_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典项值',
  `label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典项名称',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典类型',
  `description` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字典项描述',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序（升序）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remarks` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注信息',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `sys_dict_value`(`item_value` ASC) USING BTREE,
  INDEX `sys_dict_label`(`label` ASC) USING BTREE,
  INDEX `sys_dict_item_del_flag`(`del_flag` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典项' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict_item
-- ----------------------------
INSERT INTO `sys_dict_item` VALUES (1, 1, '9', '异常', 'log_type', '日志异常', 1, ' ', ' ', '2019-03-19 11:08:59', '2019-03-25 12:49:13', '', '0');
INSERT INTO `sys_dict_item` VALUES (2, 1, '0', '正常', 'log_type', '日志正常', 0, ' ', ' ', '2019-03-19 11:09:17', '2019-03-25 12:49:18', '', '0');
INSERT INTO `sys_dict_item` VALUES (3, 2, 'WX', '微信', 'social_type', '微信登录', 0, ' ', ' ', '2019-03-19 11:10:02', '2019-03-25 12:49:36', '', '0');
INSERT INTO `sys_dict_item` VALUES (4, 2, 'QQ', 'QQ', 'social_type', 'QQ登录', 1, ' ', ' ', '2019-03-19 11:10:14', '2019-03-25 12:49:36', '', '0');
INSERT INTO `sys_dict_item` VALUES (5, 3, '1', 'java类', 'job_type', 'java类', 1, ' ', ' ', '2019-03-19 11:22:37', '2019-03-25 12:49:36', '', '0');
INSERT INTO `sys_dict_item` VALUES (6, 3, '2', 'spring bean', 'job_type', 'spring bean容器实例', 2, ' ', ' ', '2019-03-19 11:23:05', '2019-03-25 12:49:36', '', '0');
INSERT INTO `sys_dict_item` VALUES (7, 3, '9', '其他', 'job_type', '其他类型', 9, ' ', ' ', '2019-03-19 11:23:31', '2019-03-25 12:49:36', '', '0');
INSERT INTO `sys_dict_item` VALUES (8, 3, '3', 'Rest 调用', 'job_type', 'Rest 调用', 3, ' ', ' ', '2019-03-19 11:23:57', '2019-03-25 12:49:36', '', '0');
INSERT INTO `sys_dict_item` VALUES (9, 3, '4', 'jar', 'job_type', 'jar类型', 4, ' ', ' ', '2019-03-19 11:24:20', '2019-03-25 12:49:36', '', '0');
INSERT INTO `sys_dict_item` VALUES (10, 4, '1', '未发布', 'job_status', '未发布', 1, ' ', ' ', '2019-03-19 11:25:18', '2019-03-25 12:49:36', '', '0');
INSERT INTO `sys_dict_item` VALUES (11, 4, '2', '运行中', 'job_status', '运行中', 2, ' ', ' ', '2019-03-19 11:25:31', '2019-03-25 12:49:36', '', '0');
INSERT INTO `sys_dict_item` VALUES (12, 4, '3', '暂停', 'job_status', '暂停', 3, ' ', ' ', '2019-03-19 11:25:42', '2019-03-25 12:49:36', '', '0');
INSERT INTO `sys_dict_item` VALUES (13, 5, '0', '正常', 'job_execute_status', '正常', 0, ' ', ' ', '2019-03-19 11:26:27', '2019-03-25 12:49:36', '', '0');
INSERT INTO `sys_dict_item` VALUES (14, 5, '1', '异常', 'job_execute_status', '异常', 1, ' ', ' ', '2019-03-19 11:26:41', '2019-03-25 12:49:36', '', '0');
INSERT INTO `sys_dict_item` VALUES (15, 6, '1', '错失周期立即执行', 'misfire_policy', '错失周期立即执行', 1, ' ', ' ', '2019-03-19 11:27:45', '2019-03-25 12:49:36', '', '0');
INSERT INTO `sys_dict_item` VALUES (16, 6, '2', '错失周期执行一次', 'misfire_policy', '错失周期执行一次', 2, ' ', ' ', '2019-03-19 11:27:57', '2019-03-25 12:49:36', '', '0');
INSERT INTO `sys_dict_item` VALUES (17, 6, '3', '下周期执行', 'misfire_policy', '下周期执行', 3, ' ', ' ', '2019-03-19 11:28:08', '2019-03-25 12:49:36', '', '0');
INSERT INTO `sys_dict_item` VALUES (18, 7, '1', '男', 'gender', '微信-男', 0, ' ', ' ', '2019-03-27 13:45:13', '2019-03-27 13:45:13', '微信-男', '0');
INSERT INTO `sys_dict_item` VALUES (19, 7, '2', '女', 'gender', '女-微信', 1, ' ', ' ', '2019-03-27 13:45:34', '2019-03-27 13:45:34', '女-微信', '0');
INSERT INTO `sys_dict_item` VALUES (20, 7, '0', '未知', 'gender', 'x性别未知', 3, ' ', ' ', '2019-03-27 13:45:57', '2019-03-27 13:45:57', 'x性别未知', '0');
INSERT INTO `sys_dict_item` VALUES (21, 8, '0', '未关注', 'subscribe', '公众号-未关注', 0, ' ', ' ', '2019-03-27 13:49:07', '2019-03-27 13:49:07', '公众号-未关注', '0');
INSERT INTO `sys_dict_item` VALUES (22, 8, '1', '已关注', 'subscribe', '公众号-已关注', 1, ' ', ' ', '2019-03-27 13:49:26', '2019-03-27 13:49:26', '公众号-已关注', '0');
INSERT INTO `sys_dict_item` VALUES (23, 9, '0', '未回复', 'response_type', '微信消息-未回复', 0, ' ', ' ', '2019-03-28 21:29:47', '2019-03-28 21:29:47', '微信消息-未回复', '0');
INSERT INTO `sys_dict_item` VALUES (24, 9, '1', '已回复', 'response_type', '微信消息-已回复', 1, ' ', ' ', '2019-03-28 21:30:08', '2019-03-28 21:30:08', '微信消息-已回复', '0');
INSERT INTO `sys_dict_item` VALUES (25, 10, '1', '检索', 'param_type', '检索', 0, ' ', ' ', '2019-04-29 18:22:17', '2019-04-29 18:22:17', '检索', '0');
INSERT INTO `sys_dict_item` VALUES (26, 10, '2', '原文', 'param_type', '原文', 0, ' ', ' ', '2019-04-29 18:22:27', '2019-04-29 18:22:27', '原文', '0');
INSERT INTO `sys_dict_item` VALUES (27, 10, '3', '报表', 'param_type', '报表', 0, ' ', ' ', '2019-04-29 18:22:36', '2019-04-29 18:22:36', '报表', '0');
INSERT INTO `sys_dict_item` VALUES (28, 10, '4', '安全', 'param_type', '安全', 0, ' ', ' ', '2019-04-29 18:22:46', '2019-04-29 18:22:46', '安全', '0');
INSERT INTO `sys_dict_item` VALUES (29, 10, '5', '文档', 'param_type', '文档', 0, ' ', ' ', '2019-04-29 18:22:56', '2019-04-29 18:22:56', '文档', '0');
INSERT INTO `sys_dict_item` VALUES (30, 10, '6', '消息', 'param_type', '消息', 0, ' ', ' ', '2019-04-29 18:23:05', '2019-04-29 18:23:05', '消息', '0');
INSERT INTO `sys_dict_item` VALUES (31, 10, '9', '其他', 'param_type', '其他', 0, ' ', ' ', '2019-04-29 18:23:16', '2019-04-29 18:23:16', '其他', '0');
INSERT INTO `sys_dict_item` VALUES (32, 10, '0', '默认', 'param_type', '默认', 0, ' ', ' ', '2019-04-29 18:23:30', '2019-04-29 18:23:30', '默认', '0');
INSERT INTO `sys_dict_item` VALUES (33, 11, '0', '正常', 'status_type', '状态正常', 0, ' ', ' ', '2019-05-15 16:31:34', '2019-05-16 22:30:46', '状态正常', '0');
INSERT INTO `sys_dict_item` VALUES (34, 11, '9', '冻结', 'status_type', '状态冻结', 1, ' ', ' ', '2019-05-15 16:31:56', '2019-05-16 22:30:50', '状态冻结', '0');
INSERT INTO `sys_dict_item` VALUES (35, 12, '1', '系统类', 'dict_type', '系统类字典', 0, ' ', ' ', '2019-05-16 14:20:40', '2019-05-16 14:20:40', '不能修改删除', '0');
INSERT INTO `sys_dict_item` VALUES (36, 12, '0', '业务类', 'dict_type', '业务类字典', 0, ' ', ' ', '2019-05-16 14:20:59', '2019-05-16 14:20:59', '可以修改', '0');
INSERT INTO `sys_dict_item` VALUES (37, 2, 'GITEE', '码云', 'social_type', '码云', 2, ' ', ' ', '2019-06-28 09:59:12', '2019-06-28 09:59:12', '码云', '0');
INSERT INTO `sys_dict_item` VALUES (38, 2, 'OSC', '开源中国', 'social_type', '开源中国登录', 2, ' ', ' ', '2019-06-28 10:04:32', '2019-06-28 10:04:32', '', '0');
INSERT INTO `sys_dict_item` VALUES (39, 14, 'password', '密码模式', 'grant_types', '支持oauth密码模式', 0, ' ', ' ', '2019-08-13 07:35:28', '2019-08-13 07:35:28', NULL, '0');
INSERT INTO `sys_dict_item` VALUES (40, 14, 'authorization_code', '授权码模式', 'grant_types', 'oauth2 授权码模式', 1, ' ', ' ', '2019-08-13 07:36:07', '2019-08-13 07:36:07', NULL, '0');
INSERT INTO `sys_dict_item` VALUES (41, 14, 'client_credentials', '客户端模式', 'grant_types', 'oauth2 客户端模式', 2, ' ', ' ', '2019-08-13 07:36:30', '2019-08-13 07:36:30', NULL, '0');
INSERT INTO `sys_dict_item` VALUES (42, 14, 'refresh_token', '刷新模式', 'grant_types', 'oauth2 刷新token', 3, ' ', ' ', '2019-08-13 07:36:54', '2019-08-13 07:36:54', NULL, '0');
INSERT INTO `sys_dict_item` VALUES (43, 14, 'implicit', '简化模式', 'grant_types', 'oauth2 简化模式', 4, ' ', ' ', '2019-08-13 07:39:32', '2019-08-13 07:39:32', NULL, '0');
INSERT INTO `sys_dict_item` VALUES (44, 15, '0', 'Avue', 'style_type', 'Avue风格', 0, ' ', ' ', '2020-02-07 03:52:52', '2020-02-07 03:52:52', '', '0');
INSERT INTO `sys_dict_item` VALUES (45, 15, '1', 'element', 'style_type', 'element-ui', 1, ' ', ' ', '2020-02-07 03:53:12', '2020-02-07 03:53:12', '', '0');
INSERT INTO `sys_dict_item` VALUES (46, 16, '0', '关', 'captcha_flag_types', '不校验验证码', 0, ' ', ' ', '2020-11-18 06:53:58', '2020-11-18 06:53:58', '不校验验证码 -0', '0');
INSERT INTO `sys_dict_item` VALUES (47, 16, '1', '开', 'captcha_flag_types', '校验验证码', 1, ' ', ' ', '2020-11-18 06:54:15', '2020-11-18 06:54:15', '不校验验证码-1', '0');
INSERT INTO `sys_dict_item` VALUES (48, 17, '0', '否', 'enc_flag_types', '不加密', 0, ' ', ' ', '2020-11-18 06:55:31', '2020-11-18 06:55:31', '不加密-0', '0');
INSERT INTO `sys_dict_item` VALUES (49, 17, '1', '是', 'enc_flag_types', '加密', 1, ' ', ' ', '2020-11-18 06:55:51', '2020-11-18 06:55:51', '加密-1', '0');
INSERT INTO `sys_dict_item` VALUES (50, 13, 'MERGE_PAY', '聚合支付', 'channel_type', '聚合支付', 1, ' ', ' ', '2019-05-30 19:08:08', '2019-06-18 13:51:53', '聚合支付', '0');
INSERT INTO `sys_dict_item` VALUES (51, 2, 'CAS', 'CAS登录', 'social_type', 'CAS 单点登录系统', 3, ' ', ' ', '2022-02-18 13:56:25', '2022-02-18 13:56:28', NULL, '0');
INSERT INTO `sys_dict_item` VALUES (52, 2, 'DINGTALK', '钉钉', 'social_type', '钉钉', 3, ' ', ' ', '2022-02-18 13:56:25', '2022-02-18 13:56:28', NULL, '0');
INSERT INTO `sys_dict_item` VALUES (53, 2, 'WEIXIN_CP', '企业微信', 'social_type', '企业微信', 3, ' ', ' ', '2022-02-18 13:56:25', '2022-02-18 13:56:28', NULL, '0');
INSERT INTO `sys_dict_item` VALUES (54, 15, '2', 'APP', 'style_type', 'uview风格', 1, ' ', ' ', '2020-02-07 03:53:12', '2020-02-07 03:53:12', '', '0');
INSERT INTO `sys_dict_item` VALUES (55, 13, 'ALIPAY_WAP', '支付宝支付', 'channel_type', '支付宝支付', 1, ' ', ' ', '2019-05-30 19:08:08', '2019-06-18 13:51:53', '聚合支付', '0');
INSERT INTO `sys_dict_item` VALUES (56, 13, 'WEIXIN_MP', '微信支付', 'channel_type', '微信支付', 1, ' ', ' ', '2019-05-30 19:08:08', '2019-06-18 13:51:53', '聚合支付', '0');
INSERT INTO `sys_dict_item` VALUES (57, 14, 'mobile', 'mobile', 'grant_types', '移动端登录', 5, 'admin', ' ', '2023-01-29 17:21:42', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (58, 18, '0', '有效', 'lock_flag', '有效', 0, 'admin', ' ', '2023-02-01 16:56:00', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (59, 18, '9', '禁用', 'lock_flag', '禁用', 1, 'admin', ' ', '2023-02-01 16:56:09', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (60, 15, '4', 'vue3', 'style_type', 'element-plus', 4, 'admin', ' ', '2023-02-06 13:52:43', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (61, 19, '0', '主机', 'ds_config_type', '主机', 0, 'admin', ' ', '2023-02-06 18:37:23', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (62, 19, '1', 'JDBC', 'ds_config_type', 'jdbc', 2, 'admin', ' ', '2023-02-06 18:37:34', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (63, 20, 'false', '否', 'common_status', '否', 1, 'admin', ' ', '2023-02-09 11:02:39', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (64, 20, 'true', '是', 'common_status', '是', 2, 'admin', ' ', '2023-02-09 11:02:52', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (65, 21, 'MINI', '小程序', 'app_social_type', '小程序登录', 0, 'admin', ' ', '2023-02-10 11:11:41', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (66, 22, '0', '否', 'yes_no_type', '0', 0, 'admin', ' ', '2023-02-20 23:35:23', NULL, '0', '0');
INSERT INTO `sys_dict_item` VALUES (67, 22, '1', '是', 'yes_no_type', '1', 0, 'admin', ' ', '2023-02-20 23:35:37', NULL, '1', '0');
INSERT INTO `sys_dict_item` VALUES (69, 23, 'text', '文本', 'repType', '文本', 0, 'admin', ' ', '2023-02-24 15:08:45', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (70, 23, 'image', '图片', 'repType', '图片', 0, 'admin', ' ', '2023-02-24 15:08:56', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (71, 23, 'voice', '语音', 'repType', '语音', 0, 'admin', ' ', '2023-02-24 15:09:08', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (72, 23, 'video', '视频', 'repType', '视频', 0, 'admin', ' ', '2023-02-24 15:09:18', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (73, 23, 'shortvideo', '小视频', 'repType', '小视频', 0, 'admin', ' ', '2023-02-24 15:09:29', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (74, 23, 'location', '地理位置', 'repType', '地理位置', 0, 'admin', ' ', '2023-02-24 15:09:41', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (75, 23, 'link', '链接消息', 'repType', '链接消息', 0, 'admin', ' ', '2023-02-24 15:09:49', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (76, 23, 'event', '事件推送', 'repType', '事件推送', 0, 'admin', ' ', '2023-02-24 15:09:57', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (77, 24, '0', '未提交', 'leave_status', '未提交', 0, 'admin', ' ', '2023-03-02 22:50:45', NULL, '未提交', '0');
INSERT INTO `sys_dict_item` VALUES (78, 24, '1', '审批中', 'leave_status', '审批中', 0, 'admin', ' ', '2023-03-02 22:50:57', NULL, '审批中', '0');
INSERT INTO `sys_dict_item` VALUES (79, 24, '2', '完成', 'leave_status', '完成', 0, 'admin', ' ', '2023-03-02 22:51:06', NULL, '完成', '0');
INSERT INTO `sys_dict_item` VALUES (80, 24, '9', '驳回', 'leave_status', '驳回', 0, 'admin', ' ', '2023-03-02 22:51:20', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (81, 25, 'record', '日程记录', 'schedule_type', '日程记录', 0, 'admin', ' ', '2023-03-06 14:50:01', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (82, 25, 'plan', '计划', 'schedule_type', '计划类型', 0, 'admin', ' ', '2023-03-06 14:50:29', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (83, 26, '0', '计划中', 'schedule_status', '日程状态', 0, 'admin', ' ', '2023-03-06 14:53:18', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (84, 26, '1', '已开始', 'schedule_status', '已开始', 0, 'admin', ' ', '2023-03-06 14:53:33', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (85, 26, '3', '已结束', 'schedule_status', '已结束', 0, 'admin', ' ', '2023-03-06 14:53:41', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (86, 27, 'mysql', 'mysql', 'ds_type', 'mysql', 0, 'admin', ' ', '2023-03-12 09:58:11', NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (1905891101827526658, 1905891040976564226, '0', '读写', 'read_write_type', '读写', 0, 'spicy', ' ', '2025-03-29 15:53:43', NULL, '读写', '0');
INSERT INTO `sys_dict_item` VALUES (1905891136677998593, 1905891040976564226, '1', '只读', 'read_write_type', '只读', 0, 'spicy', ' ', '2025-03-29 15:53:51', NULL, '只读', '0');
INSERT INTO `sys_dict_item` VALUES (1905891170744135681, 1905891040976564226, '2', '只写', 'read_write_type', '只写', 0, 'spicy', ' ', '2025-03-29 15:54:00', NULL, '只写', '0');
INSERT INTO `sys_dict_item` VALUES (1907991092624629761, 1907990469833400321, 'MQTT', 'MQTT', 'protocol', 'MQTT协议', 3, 'spicy', 'spicy', '2025-04-04 10:58:20', '2025-09-14 13:22:18', 'MQTT协议', '0');
INSERT INTO `sys_dict_item` VALUES (1907991237864988673, 1907990469833400321, 'MODBUS_TCP', 'MODBUS_TCP', 'protocol', 'MODBUS-TCP协议', 1, 'spicy', 'spicy', '2025-04-04 10:58:55', '2025-09-14 13:22:15', 'MODBUS-TCP协议', '0');
INSERT INTO `sys_dict_item` VALUES (1907991324968099841, 1907990469833400321, 'OPC_UA', 'OPC_UA', 'protocol', 'OPC_UA协议', 2, 'spicy', 'spicy', '2025-04-04 10:59:15', '2025-09-14 13:22:21', 'OPC_UA协议', '0');
INSERT INTO `sys_dict_item` VALUES (1908006381475078146, 1908006267369037826, 'host', '连接地址', 'protocol_MQTT', 'host', 0, 'spicy', 'spicy', '2025-04-04 11:59:05', '2025-04-04 12:35:25', 'host地址如：tcp://127.0.0.1:1883', '1');
INSERT INTO `sys_dict_item` VALUES (1908006417290240002, 1908006267369037826, 'username', '用户名', 'protocol_MQTT', 'username', 0, 'spicy', 'spicy', '2025-04-04 11:59:14', '2025-05-01 14:06:57', '用户名称', '1');
INSERT INTO `sys_dict_item` VALUES (1908006486835994626, 1908006267369037826, 'password', '密码', 'protocol_MQTT', 'password', 0, 'spicy', 'spicy', '2025-04-04 11:59:30', '2025-05-01 14:07:03', 'password密码', '1');
INSERT INTO `sys_dict_item` VALUES (1908006940970065921, 1908006877564772353, 'ip', '连接地址', 'protocol_MODBUS_TCP', 'ip', 0, 'spicy', 'spicy', '2025-04-04 12:01:18', '2025-04-04 12:35:35', '如：127.0.0.1', '1');
INSERT INTO `sys_dict_item` VALUES (1908006989938565121, 1908006877564772353, 'port', '连接端口', 'protocol_MODBUS_TCP', 'port', 0, 'spicy', 'spicy', '2025-04-04 12:01:30', '2025-04-04 12:35:40', '如：6000', '1');
INSERT INTO `sys_dict_item` VALUES (1908007034989584385, 1908006877564772353, 'slaveId', '从站编号', 'protocol_MODBUS_TCP', 'slaveId', 0, 'spicy', 'spicy', '2025-04-04 12:01:41', '2025-04-04 12:36:20', '如：1', '1');
INSERT INTO `sys_dict_item` VALUES (1908007077851176962, 1908006877564772353, 'functionCode', '功能码', 'protocol_MODBUS_TCP', 'functionCode', 0, 'spicy', 'spicy', '2025-04-04 12:01:51', '2025-04-04 12:36:27', '如： 2', '1');
INSERT INTO `sys_dict_item` VALUES (1908007123359375362, 1908006877564772353, 'offset', '开始地址', 'protocol_MODBUS_TCP', 'offset', 0, 'spicy', 'spicy', '2025-04-04 12:02:02', '2025-04-04 12:36:39', '如：1', '1');
INSERT INTO `sys_dict_item` VALUES (1908007348648026113, 1908007297460740097, 'host', '连接地址', 'protocol_OPC_UA', '连接地址', 1, 'spicy', 'spicy', '2025-04-04 12:02:56', '2025-05-22 11:52:08', '如：127.0.0.1', '1');
INSERT INTO `sys_dict_item` VALUES (1908007391115354114, 1908007297460740097, 'port', '连接端口', 'protocol_OPC_UA', '连接端口', 2, 'spicy', 'spicy', '2025-04-04 12:03:06', '2025-05-22 11:52:15', '如：6000', '1');
INSERT INTO `sys_dict_item` VALUES (1908007437378527233, 1908007297460740097, 'namespace', '命名空间', 'protocol_OPC_UA', '命名空间', 4, 'spicy', 'spicy', '2025-04-04 12:03:17', '2025-05-22 11:52:32', '如：/xx/xx', '1');
INSERT INTO `sys_dict_item` VALUES (1917876833420959746, 1917876753079066626, '0', '关闭', 'device_status', '关闭', 0, 'spicy', ' ', '2025-05-01 17:40:44', NULL, '关闭', '0');
INSERT INTO `sys_dict_item` VALUES (1917876871706566657, 1917876753079066626, '1', '开启', 'device_status', '开启', 1, 'spicy', ' ', '2025-05-01 17:40:53', NULL, '开启', '0');
INSERT INTO `sys_dict_item` VALUES (1917876934864396290, 1917876753079066626, '2', '禁用', 'device_status', '禁用', 2, 'spicy', ' ', '2025-05-01 17:41:08', NULL, '禁用', '0');
INSERT INTO `sys_dict_item` VALUES (1918953692770340865, 1908006267369037826, 'namespace', '命名空间', 'protocol_MQTT', '命名空间', 3, 'spicy', ' ', '2025-05-04 16:59:47', NULL, '命名空间', '1');
INSERT INTO `sys_dict_item` VALUES (1918953726823895041, 1908006267369037826, 'tag', 'tag', 'protocol_MQTT', 'tag', 45, 'spicy', ' ', '2025-05-04 16:59:56', NULL, 'tag', '1');
INSERT INTO `sys_dict_item` VALUES (1919031062491561985, 1919030963073974274, '0/1 * * * * ?', '表示每1秒 执行任务', 'cron', '表示每1秒 执行任务', 1, 'spicy', 'spicy', '2025-05-04 22:07:14', '2025-05-04 22:08:11', '表示每1秒 执行任务', '0');
INSERT INTO `sys_dict_item` VALUES (1919031138056142850, 1919030963073974274, '0 0/1 * * * ?', '表示每1分钟 执行任务', 'cron', '表示每1分钟 执行任务', 3, 'spicy', 'spicy', '2025-05-04 22:07:32', '2025-05-04 22:12:28', '表示每1分钟 执行任务', '0');
INSERT INTO `sys_dict_item` VALUES (1919031232075661314, 1919030963073974274, '0/10 * * * * ?', '表示每10秒 执行任务', 'cron', '表示每10秒 执行任务', 2, 'spicy', 'spicy', '2025-05-04 22:07:54', '2025-05-04 22:08:08', '表示每10秒 执行任务', '0');
INSERT INTO `sys_dict_item` VALUES (1919032055807606785, 1919030963073974274, '0 0 12 * * ?', '每天中午12点触发', 'cron', '每天中午12点触发', 4, 'spicy', 'spicy', '2025-05-04 22:11:11', '2025-05-04 22:11:16', '每天中午12点触发', '0');
INSERT INTO `sys_dict_item` VALUES (1919032199215054849, 1919030963073974274, '0 15 10 L * ?', '每月最后一日的上午10:15触发', 'cron', '每月最后一日的上午10:15触发', 4, 'spicy', 'spicy', '2025-05-04 22:11:45', NULL, '每月最后一日的上午10:15触发', '0');
INSERT INTO `sys_dict_item` VALUES (1919032306501156865, 1919030963073974274, '0 0/30 * * * ?', '表示每30分钟 执行任务', 'cron', '表示每30分钟 执行任务', 3, 'spicy', 'spicy', '2025-05-04 22:12:10', '2025-05-04 22:12:24', '表示每30分钟 执行任务', '0');
INSERT INTO `sys_dict_item` VALUES (1921092837865189378, 1921092719615176706, '1', '线圈（Coils）', 'function_code', '线圈（Coils）', 0, 'spicy', ' ', '2025-05-10 14:39:59', NULL, '线圈是可读写的二进制值（0或1），通常用来表示开/关状态、启动/停止命令等', '0');
INSERT INTO `sys_dict_item` VALUES (1921092911508779009, 1921092719615176706, '2', '离散输入（Discrete Inputs）', 'function_code', '离散输入（Discrete Inputs）', 1, 'spicy', 'spicy', '2025-05-10 14:40:17', '2025-05-10 14:40:21', '离散输入是只读的二进制值，常用于表示传感器状态（如开关状态）。', '0');
INSERT INTO `sys_dict_item` VALUES (1921092984808435714, 1921092719615176706, '3', '输入寄存器（Input Registers）', 'function_code', '输入寄存器（Input Registers）', 2, 'spicy', 'spicy', '2025-05-10 14:40:34', NULL, '输入寄存器是只读的16位寄存器，通常用于存储模拟信号的值，如温度、压力或者其他测量值。', '0');
INSERT INTO `sys_dict_item` VALUES (1921093056275181570, 1921092719615176706, '4', '保持寄存器（Holding Registers）', 'function_code', '保持寄存器（Holding Registers）', 4, 'spicy', 'spicy', '2025-05-10 14:40:51', NULL, '保持寄存器是可读写的16位寄存器，可以用来存储和调整设备参数，或者作为与设备交换控制信息的缓冲区', '0');
INSERT INTO `sys_dict_item` VALUES (1923950684290486273, 1905889865141186561, 'string', '字符串', 'data_type', '字符串', 1, 'spicy', ' ', '2025-05-18 11:56:03', NULL, '字符串', '0');
INSERT INTO `sys_dict_item` VALUES (1923950752565366785, 1905889865141186561, 'short', '短整数', 'data_type', '短整数', 3, 'spicy', ' ', '2025-05-18 11:56:19', NULL, '短整数', '0');
INSERT INTO `sys_dict_item` VALUES (1923950784714706945, 1905889865141186561, 'int', '整数', 'data_type', '整数', 4, 'spicy', ' ', '2025-05-18 11:56:27', NULL, '整数', '0');
INSERT INTO `sys_dict_item` VALUES (1923950820756361218, 1905889865141186561, 'long', '长整数', 'data_type', '长整数', 5, 'spicy', ' ', '2025-05-18 11:56:36', NULL, '长整数', '0');
INSERT INTO `sys_dict_item` VALUES (1923950861592104962, 1905889865141186561, 'float', '浮点数', 'data_type', '浮点数', 7, 'spicy', ' ', '2025-05-18 11:56:45', NULL, '浮点数', '0');
INSERT INTO `sys_dict_item` VALUES (1923950895834402818, 1905889865141186561, 'double', '双精度浮点数', 'data_type', '双精度浮点数', 8, 'spicy', ' ', '2025-05-18 11:56:54', NULL, '双精度浮点数', '0');
INSERT INTO `sys_dict_item` VALUES (1923950938876350465, 1905889865141186561, 'bool', '布尔量', 'data_type', '布尔量', 10, 'spicy', ' ', '2025-05-18 11:57:04', NULL, '布尔量', '0');
INSERT INTO `sys_dict_item` VALUES (1925399437409054722, 1908007297460740097, 'tag', '标签', 'protocol_OPC_UA', '标签', 3, 'spicy', 'spicy', '2025-05-22 11:52:53', '2025-05-22 11:52:53', '如：2', '1');
INSERT INTO `sys_dict_item` VALUES (1941822970397724674, 27, 'taos', 'taos', 'ds_type', 'TDengine时序数据库', 2, 'spicy', 'spicy', '2025-07-06 19:34:08', NULL, 'taos', '0');
INSERT INTO `sys_dict_item` VALUES (1949119570560172034, 1949119522925461505, '1', '在线', 'device_online_status', '在线', 2, 'spicy', 'spicy', '2025-07-26 22:48:13', '2025-07-26 22:48:30', '在线', '1');
INSERT INTO `sys_dict_item` VALUES (1949119606979313665, 1949119522925461505, '0', '离线', 'device_online_status', '离线', 1, 'spicy', 'spicy', '2025-07-26 22:48:22', '2025-07-26 22:48:27', '离线', '1');
INSERT INTO `sys_dict_item` VALUES (1949131142007373826, 1917876753079066626, '3', '在线', 'device_status', '在线', 5, 'spicy', ' ', '2025-07-26 23:34:12', NULL, '在线', '0');
INSERT INTO `sys_dict_item` VALUES (1949131173959581697, 1917876753079066626, '4', '离线', 'device_status', '离线', 7, 'spicy', ' ', '2025-07-26 23:34:20', NULL, '离线', '0');
INSERT INTO `sys_dict_item` VALUES (1971955274608668674, 1971955213640265730, '1', '任务监听', 'listener_type', '任务监听', 1, 'spicy', ' ', '2025-09-27 23:09:09', NULL, '任务监听', '0');
INSERT INTO `sys_dict_item` VALUES (1971955306615402498, 1971955213640265730, '2', '执行监听', 'listener_type', '执行监听', 2, 'spicy', ' ', '2025-09-27 23:09:17', NULL, '执行监听', '0');
INSERT INTO `sys_dict_item` VALUES (1971956320072495106, 1971956236517765121, 'classListener', 'Java类', 'listener_value_type', 'Java类', 1, 'spicy', ' ', '2025-09-27 23:13:19', NULL, 'Java类', '0');
INSERT INTO `sys_dict_item` VALUES (1971956361105371138, 1971956236517765121, 'expressionListener', '表达式', 'listener_value_type', '表达式', 2, 'spicy', ' ', '2025-09-27 23:13:28', NULL, '表达式', '0');
INSERT INTO `sys_dict_item` VALUES (1971956410157756417, 1971956236517765121, 'delegateExpressionListener', '代理表达式', 'listener_value_type', '代理表达式', 3, 'spicy', ' ', '2025-09-27 23:13:40', NULL, '代理表达式', '0');
INSERT INTO `sys_dict_item` VALUES (1971957777865764865, 1971957456481415170, 'create', '创建', 'listener_event_task_type', '创建', 1, 'spicy', ' ', '2025-09-27 23:19:06', NULL, '创建', '0');
INSERT INTO `sys_dict_item` VALUES (1971957808798756865, 1971957456481415170, 'assignment', '赋值', 'listener_event_task_type', '赋值', 2, 'spicy', 'spicy', '2025-09-27 23:19:13', '2025-09-27 23:19:57', '赋值', '0');
INSERT INTO `sys_dict_item` VALUES (1971957851194781697, 1971957456481415170, 'complete', '完成', 'listener_event_task_type', '完成', 3, 'spicy', ' ', '2025-09-27 23:19:24', NULL, '完成', '0');
INSERT INTO `sys_dict_item` VALUES (1971957901195079682, 1971957456481415170, 'delete', '删除', 'listener_event_task_type', '删除', 4, 'spicy', 'spicy', '2025-09-27 23:19:36', '2025-09-27 23:19:39', '删除', '0');
INSERT INTO `sys_dict_item` VALUES (1971958061602041857, 1971957589461823490, 'start', '开始', 'listener_event_exec_type', '开始', 0, 'spicy', 'spicy', '2025-09-27 23:20:14', NULL, '开始', '0');
INSERT INTO `sys_dict_item` VALUES (1971958117331759106, 1971957589461823490, 'end', '结束', 'listener_event_exec_type', '结束', 1, 'spicy', 'spicy', '2025-09-27 23:20:27', NULL, '结束', '0');
INSERT INTO `sys_dict_item` VALUES (1971958183526264834, 1971957589461823490, 'take', '传递', 'listener_event_exec_type', '传递', 3, 'spicy', 'spicy', '2025-09-27 23:20:43', NULL, '传递', '0');
INSERT INTO `sys_dict_item` VALUES (1987103145642901506, 1987103096611487746, 'assignee', '指定人员', 'flow_user_type', '指定人员', 1, 'spicy', ' ', '2025-11-08 18:21:23', NULL, '指定人员', '0');
INSERT INTO `sys_dict_item` VALUES (1987103181416120322, 1987103096611487746, 'candidateUsers', '候选人员', 'flow_user_type', '候选人员', 2, 'spicy', ' ', '2025-11-08 18:21:32', NULL, '候选人员', '0');
INSERT INTO `sys_dict_item` VALUES (1987103212143591426, 1987103096611487746, 'candidateGroups', '候选角色', 'flow_user_type', '候选角色', 3, 'spicy', ' ', '2025-11-08 18:21:39', NULL, '候选角色', '0');
INSERT INTO `sys_dict_item` VALUES (2037080430779928577, 1907990469833400321, 'RTSP', 'RTSP', 'protocol', 'RTSP协议', 4, 'admin', NULL, '2026-03-26 16:13:16', NULL, 'RTSP协议', '0');
INSERT INTO `sys_dict_item` VALUES (2037080824989978625, 2037080792068886530, 'fire', '火灾', 'ai_cammer_type', '火灾', 1, 'admin', 'admin', '2026-03-26 16:14:50', '2026-03-26 16:15:50', '火灾', '0');
INSERT INTO `sys_dict_item` VALUES (2037080877645271041, 2037080792068886530, 'person_head', '安全帽', 'ai_cammer_type', '安全帽', 2, 'admin', 'admin', '2026-03-26 16:15:03', '2026-03-26 16:15:44', '安全帽', '0');
INSERT INTO `sys_dict_item` VALUES (2037081012127240194, 2037080792068886530, 'animals', '动物', 'ai_cammer_type', '动物', 3, 'admin', NULL, '2026-03-26 16:15:35', NULL, '动物', '0');

-- ----------------------------
-- Table structure for sys_file
-- ----------------------------
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file`  (
  `id` bigint NOT NULL COMMENT '编号',
  `file_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件名',
  `bucket_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件存储桶名称',
  `original` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '原始文件名',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件类型',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '上传时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '文件管理表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_file
-- ----------------------------
INSERT INTO `sys_file` VALUES (2037081308601618434, '0328da26aed64eec8d8e1127bc70532b.png', 's3demo', 'wechat_2025-08-20_145854_408.png', 'png', 59122, 'admin', 'admin', '2026-03-26 16:16:46', NULL, '0');
INSERT INTO `sys_file` VALUES (2037081462524186626, '246ee39e58d644d4af72fe3076bd5b86.png', 's3demo', 'Group 2085663045.png', 'png', 638374, 'admin', 'admin', '2026-03-26 16:17:22', NULL, '0');
INSERT INTO `sys_file` VALUES (2037081494317010945, 'db3ce1872816432987cea9fcdb6407c1.png', 's3demo', 'wechat_2025-08-20_145854_408.png', 'png', 59122, 'admin', 'admin', '2026-03-26 16:17:30', NULL, '0');
INSERT INTO `sys_file` VALUES (2037081686864924674, '55481f287bdf4b61855bd2098c29f252.png', 's3demo', 'Group 2085663045.png', 'png', 638374, 'admin', 'admin', '2026-03-26 16:18:16', NULL, '0');
INSERT INTO `sys_file` VALUES (2037081706678816769, 'b9e5e991e2514e1782df3af27ce8b5c1.png', 's3demo', 'wechat_2025-08-20_145854_408.png', 'png', 59122, 'admin', 'admin', '2026-03-26 16:18:21', NULL, '0');
INSERT INTO `sys_file` VALUES (2037083699518373889, 'a13e4c1355c3434d8758f28b5eafeda8.png', 's3demo', 'wechat_2025-08-20_145854_408.png', 'png', 59122, 'admin', 'admin', '2026-03-26 16:26:16', NULL, '0');

-- ----------------------------
-- Table structure for sys_job
-- ----------------------------
DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job`  (
  `job_id` bigint NOT NULL COMMENT '任务id',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务组名',
  `job_order` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '组内执行顺利，值越大执行优先级越高，最大值9，最小值1',
  `job_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1' COMMENT '1、java类;2、spring bean名称;3、rest调用;4、jar调用;9其他',
  `execute_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'job_type=3时，rest调用地址，仅支持rest get协议,需要增加String返回值，0成功，1失败;job_type=4时，jar路径;其它值为空',
  `class_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'job_type=1时，类完整路径;job_type=2时，spring bean名称;其它值为空',
  `method_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务方法',
  `method_params_value` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '参数值',
  `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'cron执行表达式',
  `misfire_policy` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '3' COMMENT '错失执行策略（1错失周期立即执行 2错失周期执行一次 3下周期执行）',
  `job_tenant_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '1、多租户任务;2、非多租户任务',
  `job_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态（1、未发布;2、运行中;3、暂停;4、删除;）',
  `job_execute_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态（0正常 1异常）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `start_time` timestamp NULL DEFAULT NULL COMMENT '初次执行时间',
  `previous_time` timestamp NULL DEFAULT NULL COMMENT '上次执行时间',
  `next_time` timestamp NULL DEFAULT NULL COMMENT '下次执行时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '备注信息',
  PRIMARY KEY (`job_id`) USING BTREE,
  UNIQUE INDEX `job_name_group_idx`(`job_name` ASC, `job_group` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '定时任务调度表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_job
-- ----------------------------

-- ----------------------------
-- Table structure for sys_job_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE `sys_job_log`  (
  `job_log_id` bigint NOT NULL COMMENT '任务日志ID',
  `job_id` bigint NOT NULL COMMENT '任务id',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务组名',
  `job_order` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '组内执行顺利，值越大执行优先级越高，最大值9，最小值1',
  `job_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1' COMMENT '1、java类;2、spring bean名称;3、rest调用;4、jar调用;9其他',
  `execute_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'job_type=3时，rest调用地址，仅支持post协议;job_type=4时，jar路径;其它值为空',
  `class_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'job_type=1时，类完整路径;job_type=2时，spring bean名称;其它值为空',
  `method_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '任务方法',
  `method_params_value` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '参数值',
  `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'cron执行表达式',
  `job_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '日志信息',
  `job_log_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
  `execute_time` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '执行时间',
  `exception_info` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '异常信息',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`job_log_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '定时任务执行日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_job_log
-- ----------------------------

-- ----------------------------
-- Table structure for sys_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log`  (
  `id` bigint NOT NULL COMMENT '编号',
  `log_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '日志类型',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '日志标题',
  `service_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '服务ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remote_addr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '远程地址',
  `user_agent` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户代理',
  `request_uri` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '请求URI',
  `method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '请求方法',
  `params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '请求参数',
  `time` bigint NULL DEFAULT NULL COMMENT '执行时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志',
  `exception` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '异常信息',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `sys_log_request_uri`(`request_uri` ASC) USING BTREE,
  INDEX `sys_log_type`(`log_type` ASC) USING BTREE,
  INDEX `sys_log_create_date`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_log
-- ----------------------------
INSERT INTO `sys_log` VALUES (2037075395056603138, '0', '登录成功', 'nqboard-auth', 'admin', NULL, '2026-03-26 15:53:16', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/oauth2/token', 'POST', 'randomStr=%5Bf32ccaea-09e5-4f23-bfdb-d93000071702%5D&code=%5B0%5D&grant_type=%5Bpassword%5D&scope=%5Bserver%5D&username=%5Badmin%5D', 1173, '0', NULL);
INSERT INTO `sys_log` VALUES (2037079443751145473, '0', '更新角色菜单', 'nqboard-upms-biz', 'admin', NULL, '2026-03-26 16:09:21', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/role/menu', 'PUT', '[{\"roleId\":1,\"menuIds\":\"1000,1100,1101,1102,1103,1104,1200,1201,1202,1203,1300,1301,1302,1303,1304,1305,1400,1401,1402,1403,1600,1601,1602,1603,1604,1605,2000,2001,2100,2101,2102,2200,2201,2202,2203,2906,2907,2210,2211,2212,2213,2800,2810,2820,2830,2840,2850,2860,2870,2871,2400,2401,2402,2403,2600,2601,4000,4002,4001,9000,9005,9007,2300,9006,9050,9065,9051,9052,9053,9054,9055,9056,9057,9059,9060,9061,9062,9063,9064,1901265970396176385,1743227371409,1743227371410,1743227371411,1743227371412,1743227371413,1743227371414,1913505265764814850,1913505381577936898,1913505476759277570,1913505537257918465,1913505609009876993,1913587779967668225,1742130402867,1742130402869,1742130402870,1742130402871,1742130402872,1743318978196,1743318978197,1743318978198,1743318978199,1743318978200,1743318978201,1913445900512940034,1746080528758,1746080528760,1746080528761,1746080528762,1746080528763,1989630040934592514,1746334604170,1746334604172,1746334604173,1746334604174,1918194239292833794,1956950686046371842,1965783565920636929,\"}]', 194, '0', NULL);
INSERT INTO `sys_log` VALUES (2037080315499483137, '0', '同步字典缓存', 'nqboard-upms-biz', 'admin', NULL, '2026-03-26 16:12:49', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/dict/sync', 'PUT', '[]', 3, '0', NULL);
INSERT INTO `sys_log` VALUES (2037080430847037441, '0', '新增字典项', 'nqboard-upms-biz', 'admin', NULL, '2026-03-26 16:13:17', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/dict/item', 'POST', '[{\"id\":2037080430779928577,\"dictId\":1907990469833400321,\"label\":\"RTSP\",\"dictType\":\"protocol\",\"description\":\"RTSP协议\",\"sortOrder\":4,\"createBy\":\"admin\",\"updateBy\":\"admin\",\"createTime\":\"2026-03-26 16:13:16\",\"updateTime\":\"2026-03-26 16:13:16\",\"remarks\":\"RTSP协议\",\"delFlag\":\"0\",\"value\":\"RTSP\"}]', 9, '0', NULL);
INSERT INTO `sys_log` VALUES (2037080498413080577, '0', '同步字典缓存', 'nqboard-upms-biz', 'admin', NULL, '2026-03-26 16:13:33', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/dict/sync', 'PUT', '[]', 2, '0', NULL);
INSERT INTO `sys_log` VALUES (2037080792131801090, '0', '添加字典', 'nqboard-upms-biz', 'admin', NULL, '2026-03-26 16:14:43', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/dict', 'POST', '[{\"id\":2037080792068886530,\"dictType\":\"ai_cammer_type\",\"description\":\"AI监控类型\",\"createTime\":\"2026-03-26 16:14:42\",\"updateTime\":\"2026-03-26 16:14:42\",\"systemFlag\":\"0\",\"remarks\":\"AI监控类型\",\"createBy\":\"admin\",\"updateBy\":\"admin\",\"delFlag\":\"0\"}]', 8, '0', NULL);
INSERT INTO `sys_log` VALUES (2037080825052893185, '0', '新增字典项', 'nqboard-upms-biz', 'admin', NULL, '2026-03-26 16:14:50', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/dict/item', 'POST', '[{\"id\":2037080824989978625,\"dictId\":2037080792068886530,\"label\":\"fire\",\"dictType\":\"ai_cammer_type\",\"description\":\"fire\",\"sortOrder\":1,\"createBy\":\"admin\",\"updateBy\":\"admin\",\"createTime\":\"2026-03-26 16:14:50\",\"updateTime\":\"2026-03-26 16:14:50\",\"remarks\":\"fire\",\"delFlag\":\"0\",\"value\":\"fire\"}]', 8, '0', NULL);
INSERT INTO `sys_log` VALUES (2037080877771100162, '0', '新增字典项', 'nqboard-upms-biz', 'admin', NULL, '2026-03-26 16:15:03', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/dict/item', 'POST', '[{\"id\":2037080877645271041,\"dictId\":2037080792068886530,\"label\":\"person_head\",\"dictType\":\"ai_cammer_type\",\"description\":\"person_head\",\"sortOrder\":2,\"createBy\":\"admin\",\"updateBy\":\"admin\",\"createTime\":\"2026-03-26 16:15:03\",\"updateTime\":\"2026-03-26 16:15:03\",\"remarks\":\"person_head\",\"delFlag\":\"0\",\"value\":\"person_head\"}]', 21, '0', NULL);
INSERT INTO `sys_log` VALUES (2037081012190154753, '0', '新增字典项', 'nqboard-upms-biz', 'admin', NULL, '2026-03-26 16:15:35', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/dict/item', 'POST', '[{\"id\":2037081012127240194,\"dictId\":2037080792068886530,\"label\":\"动物\",\"dictType\":\"ai_cammer_type\",\"description\":\"动物\",\"sortOrder\":3,\"createBy\":\"admin\",\"updateBy\":\"admin\",\"createTime\":\"2026-03-26 16:15:35\",\"updateTime\":\"2026-03-26 16:15:35\",\"remarks\":\"动物\",\"delFlag\":\"0\",\"value\":\"animals\"}]', 8, '0', NULL);
INSERT INTO `sys_log` VALUES (2037081048760291329, '0', '修改字典项', 'nqboard-upms-biz', 'admin', NULL, '2026-03-26 16:15:44', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/dict/item', 'PUT', '[{\"id\":2037080877645271041,\"dictId\":2037080792068886530,\"label\":\"安全帽\",\"dictType\":\"ai_cammer_type\",\"description\":\"安全帽\",\"sortOrder\":2,\"createBy\":\"admin\",\"updateBy\":\"admin\",\"createTime\":\"2026-03-26 16:15:03\",\"updateTime\":\"2026-03-26 16:15:43\",\"remarks\":\"安全帽\",\"delFlag\":\"0\",\"value\":\"person_head\"}]', 26, '0', NULL);
INSERT INTO `sys_log` VALUES (2037081074110664706, '0', '修改字典项', 'nqboard-upms-biz', 'admin', NULL, '2026-03-26 16:15:50', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/dict/item', 'PUT', '[{\"id\":2037080824989978625,\"dictId\":2037080792068886530,\"label\":\"火灾\",\"dictType\":\"ai_cammer_type\",\"description\":\"火灾\",\"sortOrder\":1,\"createBy\":\"admin\",\"updateBy\":\"admin\",\"createTime\":\"2026-03-26 16:14:50\",\"updateTime\":\"2026-03-26 16:15:49\",\"remarks\":\"火灾\",\"delFlag\":\"0\",\"value\":\"fire\"}]', 16, '0', NULL);
INSERT INTO `sys_log` VALUES (2037081076899876866, '0', '同步字典缓存', 'nqboard-upms-biz', 'admin', NULL, '2026-03-26 16:15:51', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/dict/sync', 'PUT', '[]', 1, '0', NULL);
INSERT INTO `sys_log` VALUES (2037081246987292673, '0', '新增产品分类表', 'nqboard-device-biz', 'admin', NULL, '2026-03-26 16:16:31', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/category', 'POST', '[{\"id\":2037081246098112514,\"createBy\":\"admin\",\"updateBy\":\"admin\",\"createTime\":\"2026-03-26 16:16:30\",\"updateTime\":\"2026-03-26 16:16:30\",\"delFlag\":\"0\",\"orderNum\":4,\"name\":\"摄像头\",\"remark\":\"摄像头\"}]', 17, '0', NULL);
INSERT INTO `sys_log` VALUES (2037081330953064449, '0', '新增产品表', 'nqboard-device-biz', 'admin', NULL, '2026-03-26 16:16:51', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/product', 'POST', '[{\"id\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"delFlag\":null,\"orderNum\":1,\"categoryId\":2037081246098112514,\"name\":\"AI摄像头\",\"identifier\":\"a_i_she_xiang_tou\",\"remark\":\"ai摄像头\",\"protocol\":\"RTSP\",\"authSwitch\":false,\"authMode\":\"\",\"img\":\"\",\"cameraTypes\":\"[\\\"fire\\\"]\"}]', 259, '0', NULL);
INSERT INTO `sys_log` VALUES (2037081468979220481, '0', '修改个人信息', 'nqboard-upms-biz', 'admin', NULL, '2026-03-26 16:17:24', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/user/edit', 'PUT', '[{\"userId\":1,\"username\":\"admin\",\"createBy\":null,\"updateBy\":null,\"createTime\":\"2018-04-20 07:15:18\",\"updateTime\":\"2023-07-07 14:55:40\",\"delFlag\":\"0\",\"lockFlag\":\"0\",\"avatar\":\"/api/admin/sys-file/s3demo/246ee39e58d644d4af72fe3076bd5b86.png\",\"deptId\":null,\"wxOpenid\":null,\"miniOpenid\":null,\"qqOpenid\":null,\"giteeLogin\":null,\"oscId\":null,\"nickname\":\"管理员\",\"name\":\"管理员\",\"email\":\"pig4cloud@qq.com\",\"role\":null,\"post\":null,\"newpassword1\":null}]', 14, '0', NULL);
INSERT INTO `sys_log` VALUES (2037081499702497282, '0', '修改产品表', 'nqboard-device-biz', 'admin', NULL, '2026-03-26 16:17:31', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/product', 'PUT', '[{\"id\":2037081329992581122,\"createBy\":\"admin\",\"createTime\":\"2026-03-26 16:16:51\",\"updateBy\":\"admin\",\"updateTime\":\"2026-03-26 16:17:31\",\"delFlag\":\"0\",\"orderNum\":1,\"categoryId\":2037081246098112514,\"name\":\"AI摄像头\",\"identifier\":null,\"remark\":\"ai摄像头\",\"protocol\":\"RTSP\",\"authSwitch\":false,\"authMode\":\"\",\"img\":\"\",\"cameraTypes\":\"[\\\"fire\\\"]\"}]', 14, '0', NULL);
INSERT INTO `sys_log` VALUES (2037081727184769025, '0', '修改产品表', 'nqboard-device-biz', 'admin', NULL, '2026-03-26 16:18:26', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/product', 'PUT', '[{\"id\":2037081329992581122,\"createBy\":\"admin\",\"createTime\":\"2026-03-26 16:16:51\",\"updateBy\":\"admin\",\"updateTime\":\"2026-03-26 16:18:25\",\"delFlag\":\"0\",\"orderNum\":1,\"categoryId\":2037081246098112514,\"name\":\"AI摄像头\",\"identifier\":null,\"remark\":\"ai摄像头\",\"protocol\":\"RTSP\",\"authSwitch\":false,\"authMode\":\"\",\"img\":\"\",\"cameraTypes\":\"[\\\"fire\\\"]\"}]', 14, '0', NULL);
INSERT INTO `sys_log` VALUES (2037082215376588802, '0', '通过通用物模型导入模型', 'nqboard-device-biz', 'admin', NULL, '2026-03-26 16:20:22', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/model/importModelByTemplate', 'POST', '[{\"templateIds\":[1913501835184844801],\"productId\":2037081329992581122}]', 69, '0', NULL);
INSERT INTO `sys_log` VALUES (2037083271414153218, '0', '通过通用物模型导入模型', 'nqboard-device-biz', 'admin', NULL, '2026-03-26 16:24:34', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/model/importModelByTemplate', 'POST', '[{\"templateIds\":[1915996109348663297],\"productId\":2037081329992581122}]', 168, '0', NULL);
INSERT INTO `sys_log` VALUES (2037083704476041218, '0', '修改产品表', 'nqboard-device-biz', 'admin', NULL, '2026-03-26 16:26:17', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/product', 'PUT', '[{\"id\":2037081329992581122,\"createBy\":\"admin\",\"createTime\":\"2026-03-26 16:16:51\",\"updateBy\":\"admin\",\"updateTime\":\"2026-03-26 16:26:16\",\"delFlag\":\"0\",\"orderNum\":1,\"categoryId\":2037081246098112514,\"name\":\"AI摄像头\",\"identifier\":null,\"remark\":\"ai摄像头\",\"protocol\":\"RTSP\",\"authSwitch\":false,\"authMode\":\"\",\"img\":\"/api/admin/sys-file/s3demo/a13e4c1355c3434d8758f28b5eafeda8.png\",\"cameraTypes\":\"[\\\"fire\\\"]\"}]', 14, '0', NULL);
INSERT INTO `sys_log` VALUES (2037087953222107138, '0', '新增设备表', 'nqboard-device-biz', 'admin', NULL, '2026-03-26 16:43:10', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/iotDevice', 'POST', '[{\"id\":null,\"createBy\":null,\"createTime\":null,\"updateBy\":null,\"updateTime\":null,\"orderNum\":0,\"delFlag\":null,\"name\":\"设备1\",\"identifier\":\"she_bei_1\",\"longitude\":null,\"latitude\":null,\"location\":\"中国\",\"status\":\"0\",\"remark\":\"\",\"productId\":2037081329992581122,\"driverValue\":null,\"cron\":\"\",\"userId\":1}]', 100, '0', NULL);
INSERT INTO `sys_log` VALUES (2037088159003049986, '0', '修改设备表', 'nqboard-device-biz', 'admin', NULL, '2026-03-26 16:43:59', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/iotDevice', 'PUT', '[{\"id\":2037087953079619586,\"createBy\":\"admin\",\"createTime\":\"2026-03-26 16:43:10\",\"updateBy\":\"admin\",\"updateTime\":\"2026-03-26 16:43:59\",\"orderNum\":0,\"delFlag\":\"0\",\"name\":null,\"identifier\":\"she_bei_1\",\"longitude\":null,\"latitude\":null,\"location\":\"中国\",\"status\":\"0\",\"remark\":\"\",\"productId\":null,\"driverValue\":null,\"cron\":\"0 0/1 * * * ?\",\"userId\":null}]', 10, '0', NULL);
INSERT INTO `sys_log` VALUES (2037088260429709313, '0', '修改设备表', 'nqboard-device-biz', 'admin', NULL, '2026-03-26 16:44:23', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/iotDevice', 'PUT', '[{\"id\":2037087953079619586,\"createBy\":null,\"createTime\":null,\"updateBy\":\"admin\",\"updateTime\":\"2026-03-26 16:44:23\",\"orderNum\":null,\"delFlag\":null,\"name\":null,\"identifier\":null,\"longitude\":null,\"latitude\":null,\"location\":null,\"status\":null,\"remark\":null,\"productId\":null,\"driverValue\":\"{\\\"serverUri\\\":\\\"rtsp://localhost:8554\\\"}\",\"cron\":null,\"userId\":null}]', 7, '0', NULL);
INSERT INTO `sys_log` VALUES (2037088296395866113, '0', '修改设备表', 'nqboard-device-biz', 'admin', NULL, '2026-03-26 16:44:32', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/iotDevice', 'PUT', '[{\"id\":2037087953079619586,\"createBy\":\"admin\",\"createTime\":\"2026-03-26 16:43:10\",\"updateBy\":\"admin\",\"updateTime\":\"2026-03-26 16:44:31\",\"orderNum\":0,\"delFlag\":\"0\",\"name\":null,\"identifier\":\"she_bei_1\",\"longitude\":null,\"latitude\":null,\"location\":\"中国\",\"status\":\"0\",\"remark\":\"\",\"productId\":null,\"driverValue\":\"{\\\"serverUri\\\":\\\"rtsp://localhost:8554\\\"}\",\"cron\":\"0 0/1 * * * ?\",\"userId\":null}]', 9, '0', NULL);
INSERT INTO `sys_log` VALUES (2037088300137185281, '0', '修改设备表', 'nqboard-device-biz', 'admin', NULL, '2026-03-26 16:44:33', NULL, '192.168.97.57', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36', '/iotDevice', 'PUT', '[{\"id\":2037087953079619586,\"createBy\":null,\"createTime\":null,\"updateBy\":\"admin\",\"updateTime\":\"2026-03-26 16:44:32\",\"orderNum\":null,\"delFlag\":null,\"name\":null,\"identifier\":null,\"longitude\":null,\"latitude\":null,\"location\":null,\"status\":null,\"remark\":null,\"productId\":null,\"driverValue\":\"{\\\"serverUri\\\":\\\"rtsp://localhost:8554\\\"}\",\"cron\":null,\"userId\":null}]', 9, '0', NULL);

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单名称',
  `en_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '英文名称',
  `permission` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '权限标识',
  `path` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '路由路径',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '父菜单ID',
  `icon` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单图标',
  `visible` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '是否可见，0隐藏，1显示',
  `sort_order` int NULL DEFAULT 1 COMMENT '排序值，越小越靠前',
  `keep_alive` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '是否缓存，0否，1是',
  `embedded` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否内嵌，0否，1是',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '菜单类型，0目录，1菜单，2按钮',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志，0未删除，1已删除',
  PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1000, '权限管理', 'authorization', NULL, '/admin', -1, 'iconfont icon-icon-', '1', 0, '0', '0', '0', '', '2018-09-28 08:29:53', 'admin', '2023-03-12 22:32:52', '0');
INSERT INTO `sys_menu` VALUES (1100, '用户管理', 'user', NULL, '/admin/user/index', 1000, 'ele-User', '1', 1, '0', '0', '0', '', '2017-11-02 22:24:37', 'admin', '2023-07-05 10:28:22', '0');
INSERT INTO `sys_menu` VALUES (1101, '用户新增', NULL, 'sys_user_add', NULL, 1100, NULL, '1', 1, '0', NULL, '1', ' ', '2017-11-08 09:52:09', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (1102, '用户修改', NULL, 'sys_user_edit', NULL, 1100, NULL, '1', 1, '0', NULL, '1', ' ', '2017-11-08 09:52:48', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (1103, '用户删除', NULL, 'sys_user_del', NULL, 1100, NULL, '1', 1, '0', NULL, '1', ' ', '2017-11-08 09:54:01', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (1104, '导入导出', NULL, 'sys_user_export', NULL, 1100, NULL, '1', 1, '0', NULL, '1', ' ', '2017-11-08 09:54:01', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (1200, '菜单管理', 'menu', NULL, '/admin/menu/index', 1000, 'iconfont icon-caidan', '1', 2, '0', '0', '0', '', '2017-11-08 09:57:27', 'admin', '2023-07-05 10:28:17', '0');
INSERT INTO `sys_menu` VALUES (1201, '菜单新增', NULL, 'sys_menu_add', NULL, 1200, NULL, '1', 1, '0', NULL, '1', ' ', '2017-11-08 10:15:53', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (1202, '菜单修改', NULL, 'sys_menu_edit', NULL, 1200, NULL, '1', 1, '0', NULL, '1', ' ', '2017-11-08 10:16:23', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (1203, '菜单删除', NULL, 'sys_menu_del', NULL, 1200, NULL, '1', 1, '0', NULL, '1', ' ', '2017-11-08 10:16:43', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (1300, '角色管理', 'role', NULL, '/admin/role/index', 1000, 'iconfont icon-gerenzhongxin', '1', 3, '0', NULL, '0', '', '2017-11-08 10:13:37', 'admin', '2023-07-05 10:28:13', '0');
INSERT INTO `sys_menu` VALUES (1301, '角色新增', NULL, 'sys_role_add', NULL, 1300, NULL, '1', 1, '0', NULL, '1', ' ', '2017-11-08 10:14:18', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (1302, '角色修改', NULL, 'sys_role_edit', NULL, 1300, NULL, '1', 1, '0', NULL, '1', ' ', '2017-11-08 10:14:41', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (1303, '角色删除', NULL, 'sys_role_del', NULL, 1300, NULL, '1', 1, '0', NULL, '1', ' ', '2017-11-08 10:14:59', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (1304, '分配权限', NULL, 'sys_role_perm', NULL, 1300, NULL, '1', 1, '0', NULL, '1', ' ', '2018-04-20 07:22:55', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (1305, '角色导入导出', NULL, 'sys_role_export', NULL, 1300, NULL, '1', 4, '0', NULL, '1', ' ', '2022-03-26 15:54:34', ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1400, '部门管理', 'dept', NULL, '/admin/dept/index', 1000, 'iconfont icon-zidingyibuju', '1', 4, '0', NULL, '0', '', '2018-01-20 13:17:19', 'admin', '2023-07-05 10:28:07', '0');
INSERT INTO `sys_menu` VALUES (1401, '部门新增', NULL, 'sys_dept_add', NULL, 1400, NULL, '1', 1, '0', NULL, '1', ' ', '2018-01-20 14:56:16', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (1402, '部门修改', NULL, 'sys_dept_edit', NULL, 1400, NULL, '1', 1, '0', NULL, '1', ' ', '2018-01-20 14:56:59', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (1403, '部门删除', NULL, 'sys_dept_del', NULL, 1400, NULL, '1', 1, '0', NULL, '1', ' ', '2018-01-20 14:57:28', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (1600, '岗位管理', 'post', NULL, '/admin/post/index', 1000, 'iconfont icon--chaifenhang', '1', 5, '1', '0', '0', '', '2022-03-26 13:04:14', 'admin', '2023-07-05 10:28:03', '0');
INSERT INTO `sys_menu` VALUES (1601, '岗位信息查看', NULL, 'sys_post_view', NULL, 1600, NULL, '1', 0, '0', NULL, '1', ' ', '2022-03-26 13:05:34', ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1602, '岗位信息新增', NULL, 'sys_post_add', NULL, 1600, NULL, '1', 1, '0', NULL, '1', ' ', '2022-03-26 13:06:00', ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1603, '岗位信息修改', NULL, 'sys_post_edit', NULL, 1600, NULL, '1', 2, '0', NULL, '1', ' ', '2022-03-26 13:06:31', ' ', '2022-03-26 13:06:38', '0');
INSERT INTO `sys_menu` VALUES (1604, '岗位信息删除', NULL, 'sys_post_del', NULL, 1600, NULL, '1', 3, '0', NULL, '1', ' ', '2022-03-26 13:06:31', ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1605, '岗位导入导出', NULL, 'sys_post_export', NULL, 1600, NULL, '1', 4, '0', NULL, '1', ' ', '2022-03-26 13:06:31', ' ', '2022-03-26 06:32:02', '0');
INSERT INTO `sys_menu` VALUES (2000, '系统管理', 'system', NULL, '/system', -1, 'iconfont icon-quanjushezhi_o', '1', 1, '0', NULL, '0', '', '2017-11-07 20:56:00', 'admin', '2023-07-05 10:27:58', '0');
INSERT INTO `sys_menu` VALUES (2001, '日志管理', 'log', NULL, '/admin/logs', 2000, 'ele-Cloudy', '1', 0, '0', '0', '0', 'admin', '2023-03-02 12:26:42', 'admin', '2023-07-05 10:27:53', '0');
INSERT INTO `sys_menu` VALUES (2100, '操作日志', 'operation', NULL, '/admin/log/index', 2001, 'iconfont icon-jinridaiban', '1', 2, '0', '0', '0', '', '2017-11-20 14:06:22', 'admin', '2023-07-05 10:27:49', '0');
INSERT INTO `sys_menu` VALUES (2101, '日志删除', NULL, 'sys_log_del', NULL, 2100, NULL, '1', 1, '0', NULL, '1', ' ', '2017-11-20 20:37:37', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (2102, '导入导出', NULL, 'sys_log_export', NULL, 2100, NULL, '1', 1, '0', NULL, '1', ' ', '2017-11-08 09:54:01', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (2200, '字典管理', 'dict', NULL, '/admin/dict/index', 2000, 'iconfont icon-zhongduancanshuchaxun', '1', 6, '0', NULL, '0', '', '2017-11-29 11:30:52', 'admin', '2023-07-05 10:27:37', '0');
INSERT INTO `sys_menu` VALUES (2201, '字典删除', NULL, 'sys_dict_del', NULL, 2200, NULL, '1', 1, '0', NULL, '1', ' ', '2017-11-29 11:30:11', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (2202, '字典新增', NULL, 'sys_dict_add', NULL, 2200, NULL, '1', 1, '0', NULL, '1', ' ', '2018-05-11 22:34:55', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (2203, '字典修改', NULL, 'sys_dict_edit', NULL, 2200, NULL, '1', 1, '0', NULL, '1', ' ', '2018-05-11 22:36:03', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (2210, '参数管理', 'parameter', NULL, '/admin/param/index', 2000, 'iconfont icon-wenducanshu-05', '1', 7, '1', NULL, '0', '', '2019-04-29 22:16:50', 'admin', '2023-02-16 15:24:51', '0');
INSERT INTO `sys_menu` VALUES (2211, '参数新增', NULL, 'sys_syspublicparam_add', NULL, 2210, NULL, '1', 1, '0', NULL, '1', ' ', '2019-04-29 22:17:36', ' ', '2020-03-24 08:57:11', '0');
INSERT INTO `sys_menu` VALUES (2212, '参数删除', NULL, 'sys_syspublicparam_del', NULL, 2210, NULL, '1', 1, '0', NULL, '1', ' ', '2019-04-29 22:17:55', ' ', '2020-03-24 08:57:12', '0');
INSERT INTO `sys_menu` VALUES (2213, '参数编辑', NULL, 'sys_syspublicparam_edit', NULL, 2210, NULL, '1', 1, '0', NULL, '1', ' ', '2019-04-29 22:18:14', ' ', '2020-03-24 08:57:13', '0');
INSERT INTO `sys_menu` VALUES (2300, '代码生成', 'code', NULL, '/gen/table/index', 9000, 'iconfont icon-zhongduancanshu', '1', 1, '0', '0', '0', '', '2018-01-20 13:17:19', 'admin', '2023-02-20 13:54:35', '0');
INSERT INTO `sys_menu` VALUES (2400, '终端管理', 'client', NULL, '/admin/client/index', 2000, 'iconfont icon-gongju', '1', 9, '1', NULL, '0', '', '2018-01-20 13:17:19', 'admin', '2023-02-16 15:25:28', '0');
INSERT INTO `sys_menu` VALUES (2401, '客户端新增', NULL, 'sys_client_add', NULL, 2400, '1', '1', 1, '0', NULL, '1', ' ', '2018-05-15 21:35:18', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (2402, '客户端修改', NULL, 'sys_client_edit', NULL, 2400, NULL, '1', 1, '0', NULL, '1', ' ', '2018-05-15 21:37:06', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (2403, '客户端删除', NULL, 'sys_client_del', NULL, 2400, NULL, '1', 1, '0', NULL, '1', ' ', '2018-05-15 21:39:16', ' ', '2021-05-25 03:12:55', '0');
INSERT INTO `sys_menu` VALUES (2600, '令牌管理', 'token', NULL, '/admin/token/index', 2000, 'ele-Key', '1', 11, '0', NULL, '0', '', '2018-09-04 05:58:41', 'admin', '2023-02-16 15:28:28', '0');
INSERT INTO `sys_menu` VALUES (2601, '令牌删除', NULL, 'sys_token_del', NULL, 2600, NULL, '1', 1, '0', NULL, '1', ' ', '2018-09-04 05:59:50', ' ', '2020-03-24 08:57:24', '0');
INSERT INTO `sys_menu` VALUES (2800, 'Quartz管理', 'quartz', NULL, '/daemon/job-manage/index', 2000, 'ele-AlarmClock', '1', 8, '0', NULL, '0', '', '2018-01-20 13:17:19', 'admin', '2023-02-16 15:25:06', '0');
INSERT INTO `sys_menu` VALUES (2810, '任务新增', NULL, 'job_sys_job_add', NULL, 2800, '1', '1', 0, '0', NULL, '1', ' ', '2018-05-15 21:35:18', ' ', '2020-03-24 08:57:26', '0');
INSERT INTO `sys_menu` VALUES (2820, '任务修改', NULL, 'job_sys_job_edit', NULL, 2800, '1', '1', 0, '0', NULL, '1', ' ', '2018-05-15 21:35:18', ' ', '2020-03-24 08:57:27', '0');
INSERT INTO `sys_menu` VALUES (2830, '任务删除', NULL, 'job_sys_job_del', NULL, 2800, '1', '1', 0, '0', NULL, '1', ' ', '2018-05-15 21:35:18', ' ', '2020-03-24 08:57:28', '0');
INSERT INTO `sys_menu` VALUES (2840, '任务暂停', NULL, 'job_sys_job_shutdown_job', NULL, 2800, '1', '1', 0, '0', NULL, '1', ' ', '2018-05-15 21:35:18', ' ', '2020-03-24 08:57:28', '0');
INSERT INTO `sys_menu` VALUES (2850, '任务开始', NULL, 'job_sys_job_start_job', NULL, 2800, '1', '1', 0, '0', NULL, '1', ' ', '2018-05-15 21:35:18', ' ', '2020-03-24 08:57:29', '0');
INSERT INTO `sys_menu` VALUES (2860, '任务刷新', NULL, 'job_sys_job_refresh_job', NULL, 2800, '1', '1', 0, '0', NULL, '1', ' ', '2018-05-15 21:35:18', ' ', '2020-03-24 08:57:30', '0');
INSERT INTO `sys_menu` VALUES (2870, '执行任务', NULL, 'job_sys_job_run_job', NULL, 2800, '1', '1', 0, '0', NULL, '1', ' ', '2019-08-08 15:35:18', ' ', '2020-03-24 08:57:31', '0');
INSERT INTO `sys_menu` VALUES (2871, '导出', NULL, 'job_sys_job_export', NULL, 2800, NULL, '1', 0, '0', '0', '1', 'admin', '2023-03-06 15:26:13', ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (2906, '文件管理', 'file', NULL, '/admin/file/index', 2000, 'ele-Files', '1', 6, '0', NULL, '0', '', '2019-06-25 12:44:46', 'admin', '2023-02-16 15:24:42', '0');
INSERT INTO `sys_menu` VALUES (2907, '删除文件', NULL, 'sys_file_del', NULL, 2906, NULL, '1', 1, '0', NULL, '1', ' ', '2019-06-25 13:41:41', ' ', '2020-03-24 08:58:42', '0');
INSERT INTO `sys_menu` VALUES (4000, '系统监控', 'monitor', NULL, '/daemon', -1, 'iconfont icon-shuju', '1', 3, '0', '0', '0', 'admin', '2023-02-06 20:20:47', 'admin', '2023-02-23 20:01:07', '0');
INSERT INTO `sys_menu` VALUES (4001, '文档扩展', 'doc', NULL, 'http://pig-gateway:9999/swagger-ui.html', 4000, 'iconfont icon-biaodan', '1', 2, '0', '1', '0', '', '2018-06-26 10:50:32', 'admin', '2023-02-23 20:01:29', '0');
INSERT INTO `sys_menu` VALUES (4002, '缓存监控', 'cache', NULL, '/ext/cache', 4000, 'iconfont icon-shuju', '1', 1, '0', '0', '0', 'admin', '2023-05-29 15:12:59', 'admin', '2023-06-06 11:58:41', '0');
INSERT INTO `sys_menu` VALUES (9000, '开发平台', 'develop', NULL, '/gen', -1, 'iconfont icon-shuxingtu', '1', 9, '0', '0', '0', '', '2019-08-12 09:35:16', 'admin', '2023-07-05 10:25:27', '0');
INSERT INTO `sys_menu` VALUES (9005, '数据源管理', 'datasource', NULL, '/gen/datasource/index', 9000, 'ele-Coin', '1', 0, '0', NULL, '0', '', '2019-08-12 09:42:11', 'admin', '2023-07-05 10:26:56', '0');
INSERT INTO `sys_menu` VALUES (9006, '表单设计', 'Form Design', NULL, '/gen/design/index', 9000, 'iconfont icon-AIshiyanshi', '0', 2, '0', '0', '0', '', '2019-08-16 10:08:56', 'admin', '2023-02-23 14:06:50', '0');
INSERT INTO `sys_menu` VALUES (9007, '生成页面', 'generation', NULL, '/gen/gener/index', 9000, 'iconfont icon-tongzhi4', '0', 0, '0', '0', '0', 'admin', '2023-02-20 09:58:23', 'admin', '2023-07-05 10:27:06', '0');
INSERT INTO `sys_menu` VALUES (9050, '元数据管理', 'metadata', NULL, '/gen/metadata', 9000, 'iconfont icon--chaifenhang', '1', 9, '0', '0', '0', '', '2018-07-27 01:13:21', 'admin', '2023-07-05 10:27:13', '0');
INSERT INTO `sys_menu` VALUES (9051, '模板管理', 'template', NULL, '/gen/template/index', 9050, 'iconfont icon--chaifenhang', '1', 5, '0', '0', '0', 'admin', '2023-02-21 11:22:54', 'admin', '2023-07-05 10:27:18', '0');
INSERT INTO `sys_menu` VALUES (9052, '查询', NULL, 'codegen_template_view', NULL, 9051, NULL, '0', 0, '0', '0', '1', 'admin', '2023-02-21 12:33:03', 'admin', '2023-02-21 13:50:54', '0');
INSERT INTO `sys_menu` VALUES (9053, '增加', NULL, 'codegen_template_add', NULL, 9051, NULL, '1', 0, '0', '0', '1', 'admin', '2023-02-21 13:34:10', 'admin', '2023-02-21 13:39:49', '0');
INSERT INTO `sys_menu` VALUES (9054, '新增', NULL, 'codegen_template_add', NULL, 9051, NULL, '0', 1, '0', '0', '1', 'admin', '2023-02-21 13:51:32', ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (9055, '导出', NULL, 'codegen_template_export', NULL, 9051, NULL, '0', 2, '0', '0', '1', 'admin', '2023-02-21 13:51:58', ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (9056, '删除', NULL, 'codegen_template_del', NULL, 9051, NULL, '0', 3, '0', '0', '1', 'admin', '2023-02-21 13:52:16', ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (9057, '编辑', NULL, 'codegen_template_edit', NULL, 9051, NULL, '0', 4, '0', '0', '1', 'admin', '2023-02-21 13:52:58', ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (9059, '模板分组', 'group', NULL, '/gen/group/index', 9050, 'iconfont icon-shuxingtu', '1', 6, '0', '0', '0', 'admin', '2023-02-21 15:06:50', 'admin', '2023-07-05 10:27:22', '0');
INSERT INTO `sys_menu` VALUES (9060, '查询', NULL, 'codegen_group_view', NULL, 9059, NULL, '0', 0, '0', '0', '1', 'admin', '2023-02-21 15:08:07', ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (9061, '新增', NULL, 'codegen_group_add', NULL, 9059, NULL, '0', 0, '0', '0', '1', 'admin', '2023-02-21 15:08:28', ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (9062, '修改', NULL, 'codegen_group_edit', NULL, 9059, NULL, '0', 0, '0', '0', '1', 'admin', '2023-02-21 15:08:43', ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (9063, '删除', NULL, 'codegen_group_del', NULL, 9059, NULL, '0', 0, '0', '0', '1', 'admin', '2023-02-21 15:09:02', ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (9064, '导出', NULL, 'codegen_group_export', NULL, 9059, NULL, '0', 0, '0', '0', '1', 'admin', '2023-02-21 15:09:22', ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (9065, '字段管理', 'field', NULL, '/gen/field-type/index', 9050, 'iconfont icon-fuwenben', '1', 0, '0', '0', '0', 'admin', '2023-02-23 20:05:09', 'admin', '2023-07-05 10:27:31', '0');
INSERT INTO `sys_menu` VALUES (1742130402867, '产品分类', 'category', '', '/device/category/index', 1901265970396176385, 'iconfont icon-fuzhiyemian', '1', 2, '0', NULL, '0', '', NULL, 'spicy', '2025-03-30 15:48:35', '0');
INSERT INTO `sys_menu` VALUES (1742130402869, '产品分类表新增', NULL, 'device_iotCategory_add', NULL, 1742130402867, '1', '1', 1, '0', NULL, '1', ' ', NULL, ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1742130402870, '产品分类表修改', NULL, 'device_iotCategory_edit', NULL, 1742130402867, '1', '1', 2, '0', NULL, '1', ' ', NULL, ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1742130402871, '产品分类表删除', NULL, 'device_iotCategory_del', NULL, 1742130402867, '1', '1', 3, '0', NULL, '1', ' ', NULL, ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1742130402872, '导入导出', NULL, 'device_iotCategory_export', NULL, 1742130402867, '1', '1', 3, '0', NULL, '1', ' ', NULL, ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1743227371409, '通用物模型', 'model_template', '', '/device/modelTemplate/index', 1901265970396176385, 'iconfont icon-siweidaotu', '1', 1, '0', NULL, '0', '', NULL, 'spicy', '2025-03-30 15:48:29', '0');
INSERT INTO `sys_menu` VALUES (1743227371410, '通用物模型表查看', NULL, 'device_ModelTemplate_view', NULL, 1743227371409, '1', '1', 0, '0', NULL, '1', ' ', NULL, ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1743227371411, '通用物模型表新增', NULL, 'device_ModelTemplate_add', NULL, 1743227371409, '1', '1', 1, '0', NULL, '1', ' ', NULL, ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1743227371412, '通用物模型表修改', NULL, 'device_ModelTemplate_edit', NULL, 1743227371409, '1', '1', 2, '0', NULL, '1', ' ', NULL, ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1743227371413, '通用物模型表删除', NULL, 'device_ModelTemplate_del', NULL, 1743227371409, '1', '1', 3, '0', NULL, '1', ' ', NULL, ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1743227371414, '导入导出', NULL, 'device_ModelTemplate_export', NULL, 1743227371409, '1', '1', 3, '0', NULL, '1', ' ', NULL, ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1743318978196, '产品管理', 'product', '', '/device/product/index', 1901265970396176385, 'iconfont icon-wenducanshu-05', '1', 3, '0', NULL, '0', '', NULL, 'spicy', '2025-03-30 15:48:38', '0');
INSERT INTO `sys_menu` VALUES (1743318978197, '产品表查看', NULL, 'device_product_view', NULL, 1743318978196, '1', '1', 0, '0', NULL, '1', ' ', NULL, ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1743318978198, '产品表新增', NULL, 'device_product_add', NULL, 1743318978196, '1', '1', 1, '0', NULL, '1', ' ', NULL, ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1743318978199, '产品表修改', NULL, 'device_product_edit', NULL, 1743318978196, '1', '1', 2, '0', NULL, '1', ' ', NULL, ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1743318978200, '产品表删除', NULL, 'device_product_del', NULL, 1743318978196, '1', '1', 3, '0', NULL, '1', ' ', NULL, ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1743318978201, '导入导出', NULL, 'device_product_export', NULL, 1743318978196, '1', '1', 3, '0', NULL, '1', ' ', NULL, ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1746080528758, '设备管理', NULL, '', '/device/device/index', 1901265970396176385, 'iconfont icon-putong', '1', 8, '0', NULL, '0', '', NULL, 'spicy', '2025-07-06 13:18:12', '0');
INSERT INTO `sys_menu` VALUES (1746080528760, '设备新增', NULL, 'device_iotDevice_add', NULL, 1746080528758, '1', '1', 1, '0', NULL, '1', '', NULL, 'spicy', '2025-05-01 17:25:54', '0');
INSERT INTO `sys_menu` VALUES (1746080528761, '设备修改', NULL, 'device_iotDevice_edit', NULL, 1746080528758, '1', '1', 2, '0', NULL, '1', '', NULL, 'spicy', '2025-05-01 17:25:58', '0');
INSERT INTO `sys_menu` VALUES (1746080528762, '设备删除', NULL, 'device_iotDevice_del', NULL, 1746080528758, '1', '1', 3, '0', NULL, '1', '', NULL, 'spicy', '2025-05-01 17:26:02', '0');
INSERT INTO `sys_menu` VALUES (1746080528763, '导入导出', NULL, 'device_iotDevice_export', NULL, 1746080528758, '1', '1', 3, '0', NULL, '1', ' ', NULL, ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1746334604170, '设备端点', NULL, '', '/device/point/index', 1901265970396176385, 'icon-bangzhushouji', '0', 8, '0', NULL, '0', '', NULL, 'spicy', '2025-05-04 13:21:34', '0');
INSERT INTO `sys_menu` VALUES (1746334604172, '设备端点新增', NULL, 'device_iotPoint_add', NULL, 1746334604170, '1', '1', 1, '0', NULL, '1', ' ', NULL, ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1746334604173, '设备端点修改', NULL, 'device_iotPoint_edit', NULL, 1746334604170, '1', '1', 2, '0', NULL, '1', ' ', NULL, ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1746334604174, '设备端点删除', NULL, 'device_iotPoint_del', NULL, 1746334604170, '1', '1', 3, '0', NULL, '1', ' ', NULL, ' ', NULL, '0');
INSERT INTO `sys_menu` VALUES (1901265970396176385, '设备管理', 'device', NULL, '/device', -1, 'iconfont icon--chaifenhang', '1', 99, '0', '0', '0', 'spicy', '2025-03-16 21:35:06', 'spicy', '2025-07-26 16:36:25', '0');
INSERT INTO `sys_menu` VALUES (1913445900512940034, '产品编辑', 'product edit', '', '/device/product/edit', 1901265970396176385, 'iconfont icon-wenducanshu-05', '0', 3, '0', NULL, '0', 'spicy', '2025-04-19 12:13:47', 'spicy', '2025-04-19 12:13:47', '0');
INSERT INTO `sys_menu` VALUES (1913505265764814850, '物模型', 'model', '', '/device/model/index', 1901265970396176385, 'iconfont icon-siweidaotu', '0', 1, '0', NULL, '0', 'spicy', '2025-04-19 16:09:41', 'spicy', '2025-04-19 16:11:07', '0');
INSERT INTO `sys_menu` VALUES (1913505381577936898, '物模型表查看', NULL, 'device_model_view', NULL, 1913505265764814850, '1', '1', 0, '0', NULL, '1', 'spicy', '2025-04-19 16:10:09', 'spicy', NULL, '0');
INSERT INTO `sys_menu` VALUES (1913505476759277570, '物模型表编辑', NULL, 'device_model_edit', NULL, 1913505265764814850, '1', '1', 0, '0', NULL, '1', 'spicy', '2025-04-19 16:10:32', 'spicy', '2025-04-19 16:10:52', '0');
INSERT INTO `sys_menu` VALUES (1913505537257918465, '物模型表添加', NULL, 'device_model_add', NULL, 1913505265764814850, '1', '1', 0, '0', NULL, '1', 'spicy', '2025-04-19 16:10:46', 'spicy', NULL, '0');
INSERT INTO `sys_menu` VALUES (1913505609009876993, '物模型表删除', NULL, 'device_model_del', NULL, 1913505265764814850, '1', '1', 0, '0', NULL, '1', 'spicy', '2025-04-19 16:11:03', 'spicy', NULL, '0');
INSERT INTO `sys_menu` VALUES (1913587779967668225, '物模型表导入', NULL, 'device_model_export', NULL, 1913505265764814850, '1', '1', 0, '0', NULL, '1', 'spicy', '2025-04-19 21:37:34', 'spicy', NULL, '0');
INSERT INTO `sys_menu` VALUES (1918194239292833794, '设备编辑', 'device edit', '', '/device/device/edit', 1901265970396176385, 'iconfont icon-diannao1', '0', 8, '0', '0', '0', 'spicy', '2025-05-02 14:42:00', 'spicy', '2025-05-02 14:42:17', '0');
INSERT INTO `sys_menu` VALUES (1956950686046371842, '设备数据', 'device source', '', '/device/device/source', 1901265970396176385, 'iconfont icon-diannao1', '0', 8, '0', '0', '0', 'spicy', '2025-08-17 13:26:17', 'spicy', '2025-08-17 13:26:17', '0');
INSERT INTO `sys_menu` VALUES (1965783565920636929, '设备写入', 'device write', '', '/device/device/write', 1901265970396176385, 'iconfont icon-dongtai', '1', 99, '0', '0', '0', 'spicy', '2025-09-10 22:24:59', 'spicy', '2025-09-10 22:25:19', '0');
INSERT INTO `sys_menu` VALUES (1989630040934592514, '设备写入', NULL, 'device_iotDevice_write', NULL, 1746080528758, '1', '1', 7, '0', NULL, '1', 'spicy', '2025-11-15 17:42:22', 'spicy', '2025-11-15 17:42:22', '0');
INSERT INTO `sys_menu`  VALUES (2040110571465031681, '设备告警', 'alarm record', '', '/device/alarmRecord/index', 1901265970396176385, 'iconfont icon-zaosheng', '1', 100, '0', '0', '0', 'admin', '2026-04-04 00:53:58', 'admin', '2026-04-04 00:53:58', '0');
INSERT INTO `sys_menu`  VALUES (2040116983687626753, '设备告警删除', '', 'device_iotAlarmRecord_del', '', 2040110571465031681, '1', '1', 3, '0', NULL, '1', 'admin', '2026-04-04 01:19:27', 'admin', NULL, '0');
INSERT INTO `sys_menu`  VALUES (2040116923151237121, '设备告警编辑', '', 'device_iotAlarmRecord_edit', '', 2040110571465031681, '1', '1', 2, '0', NULL, '1', 'admin', '2026-04-04 01:19:13', 'admin', NULL, '0');
INSERT INTO `sys_menu`  VALUES (2040116858659618818, '设备告警新增', '', 'device_iotAlarmRecord_add', '', 2040110571465031681, '1', '1', 1, '0', NULL, '1', 'admin', '2026-04-04 01:18:57', 'admin', NULL, '0');

-- ----------------------------
-- Table structure for sys_oauth_client_details
-- ----------------------------
DROP TABLE IF EXISTS `sys_oauth_client_details`;
CREATE TABLE `sys_oauth_client_details`  (
  `id` bigint NOT NULL COMMENT 'ID',
  `client_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '客户端ID',
  `resource_ids` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '资源ID集合',
  `client_secret` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '客户端秘钥',
  `scope` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '授权范围',
  `authorized_grant_types` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '授权类型',
  `web_server_redirect_uri` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '回调地址',
  `authorities` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '权限集合',
  `access_token_validity` int NULL DEFAULT NULL COMMENT '访问令牌有效期（秒）',
  `refresh_token_validity` int NULL DEFAULT NULL COMMENT '刷新令牌有效期（秒）',
  `additional_information` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '附加信息',
  `autoapprove` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '自动授权',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标记，0未删除，1已删除',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '终端信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_oauth_client_details
-- ----------------------------
INSERT INTO `sys_oauth_client_details` VALUES (1, 'app', NULL, 'app', 'server', 'password,refresh_token,authorization_code,client_credentials,mobile', 'http://localhost:4040/sso1/login,http://localhost:4041/sso1/login,http://localhost:8080/renren-admin/sys/oauth2-sso,http://localhost:8090/sys/oauth2-sso', NULL, 43200, 2592001, '{\"enc_flag\":\"1\",\"captcha_flag\":\"1\",\"online_quantity\":\"1\"}', 'true', '0', '', 'admin', NULL, '2023-02-09 13:54:54');
INSERT INTO `sys_oauth_client_details` VALUES (2, 'daemon', NULL, 'daemon', 'server', 'password,refresh_token', NULL, NULL, 43200, 2592001, '{\"enc_flag\":\"1\",\"captcha_flag\":\"1\"}', 'true', '0', ' ', ' ', NULL, NULL);
INSERT INTO `sys_oauth_client_details` VALUES (3, 'gen', NULL, 'gen', 'server', 'password,refresh_token', NULL, NULL, 43200, 2592001, '{\"enc_flag\":\"1\",\"captcha_flag\":\"1\"}', 'true', '0', ' ', ' ', NULL, NULL);
INSERT INTO `sys_oauth_client_details` VALUES (4, 'mp', NULL, 'mp', 'server', 'password,refresh_token', NULL, NULL, 43200, 2592001, '{\"enc_flag\":\"1\",\"captcha_flag\":\"1\"}', 'true', '0', ' ', ' ', NULL, NULL);
INSERT INTO `sys_oauth_client_details` VALUES (5, 'pig', NULL, 'pig', 'server', 'password,refresh_token,authorization_code,client_credentials,mobile', 'http://localhost:4040/sso1/login,http://localhost:4041/sso1/login,http://localhost:8080/renren-admin/sys/oauth2-sso,http://localhost:8090/sys/oauth2-sso', NULL, 43200, 2592001, '{\"enc_flag\":\"1\",\"captcha_flag\":\"1\",\"online_quantity\":\"1\"}', 'false', '0', '', 'admin', NULL, '2023-03-08 11:32:41');
INSERT INTO `sys_oauth_client_details` VALUES (6, 'test', NULL, 'test', 'server', 'password,refresh_token', NULL, NULL, 43200, 2592001, '{ \"enc_flag\":\"1\",\"captcha_flag\":\"0\"}', 'true', '0', ' ', ' ', NULL, NULL);
INSERT INTO `sys_oauth_client_details` VALUES (7, 'social', NULL, 'social', 'server', 'password,refresh_token,mobile', NULL, NULL, 43200, 2592001, '{ \"enc_flag\":\"0\",\"captcha_flag\":\"0\"}', 'true', '0', ' ', ' ', NULL, NULL);

-- ----------------------------
-- Table structure for sys_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post`  (
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  `post_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL COMMENT '岗位排序',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '岗位描述',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '是否删除  -1：已删除  0：正常',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`post_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '岗位信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_post
-- ----------------------------
INSERT INTO `sys_post` VALUES (1, 'CTO', 'CTO', 0, 'CTOOO', '0', '2022-03-26 13:48:17', '', '2023-03-08 16:03:35', 'admin');

-- ----------------------------
-- Table structure for sys_public_param
-- ----------------------------
DROP TABLE IF EXISTS `sys_public_param`;
CREATE TABLE `sys_public_param`  (
  `public_id` bigint NOT NULL COMMENT '编号',
  `public_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '名称',
  `public_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '键',
  `public_value` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '值',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态，0禁用，1启用',
  `validate_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '校验码',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `public_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '类型，0未知，1系统，2业务',
  `system_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '系统标识，0非系统，1系统',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标记，0未删除，1已删除',
  PRIMARY KEY (`public_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '公共参数配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_public_param
-- ----------------------------
INSERT INTO `sys_public_param` VALUES (1, '租户默认来源', 'TENANT_DEFAULT_ID', '1', '0', '', ' ', ' ', '2020-05-12 04:03:46', '2020-06-20 08:56:30', '2', '0', '1');
INSERT INTO `sys_public_param` VALUES (2, '租户默认部门名称', 'TENANT_DEFAULT_DEPTNAME', '租户默认部门', '0', '', ' ', ' ', '2020-05-12 03:36:32', NULL, '2', '1', '0');
INSERT INTO `sys_public_param` VALUES (3, '租户默认账户', 'TENANT_DEFAULT_USERNAME', 'admin', '0', '', ' ', ' ', '2020-05-12 04:05:04', NULL, '2', '1', '0');
INSERT INTO `sys_public_param` VALUES (4, '租户默认密码', 'TENANT_DEFAULT_PASSWORD', '123456', '0', '', ' ', ' ', '2020-05-12 04:05:24', NULL, '2', '1', '0');
INSERT INTO `sys_public_param` VALUES (5, '租户默认角色编码', 'TENANT_DEFAULT_ROLECODE', 'ROLE_ADMIN', '0', '', ' ', ' ', '2020-05-12 04:05:57', NULL, '2', '1', '0');
INSERT INTO `sys_public_param` VALUES (6, '租户默认角色名称', 'TENANT_DEFAULT_ROLENAME', '租户默认角色', '0', '', ' ', ' ', '2020-05-12 04:06:19', NULL, '2', '1', '0');
INSERT INTO `sys_public_param` VALUES (7, '表前缀', 'GEN_TABLE_PREFIX', 'tb_', '0', '', ' ', ' ', '2020-05-12 04:23:04', NULL, '9', '1', '0');
INSERT INTO `sys_public_param` VALUES (8, '接口文档不显示的字段', 'GEN_HIDDEN_COLUMNS', 'tenant_id', '0', '', ' ', ' ', '2020-05-12 04:25:19', NULL, '9', '1', '0');
INSERT INTO `sys_public_param` VALUES (9, '注册用户默认角色', 'USER_DEFAULT_ROLE', 'GENERAL_USER', '0', NULL, ' ', ' ', '2022-03-31 16:52:24', NULL, '2', '1', '0');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `role_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色名称',
  `role_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色编码',
  `role_desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色描述',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标记，0未删除，1已删除',
  PRIMARY KEY (`role_id`) USING BTREE,
  INDEX `role_idx1_role_code`(`role_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '管理员', 'ROLE_ADMIN', '管理员', '', 'admin', '2017-10-29 15:45:51', '2023-07-07 14:55:07', '0');
INSERT INTO `sys_role` VALUES (2, '普通用户', 'GENERAL_USER', '普通用户', '', 'admin', '2022-03-31 17:03:15', '2023-04-03 02:28:51', '0');

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色菜单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (1, 1000);
INSERT INTO `sys_role_menu` VALUES (1, 1100);
INSERT INTO `sys_role_menu` VALUES (1, 1101);
INSERT INTO `sys_role_menu` VALUES (1, 1102);
INSERT INTO `sys_role_menu` VALUES (1, 1103);
INSERT INTO `sys_role_menu` VALUES (1, 1104);
INSERT INTO `sys_role_menu` VALUES (1, 1200);
INSERT INTO `sys_role_menu` VALUES (1, 1201);
INSERT INTO `sys_role_menu` VALUES (1, 1202);
INSERT INTO `sys_role_menu` VALUES (1, 1203);
INSERT INTO `sys_role_menu` VALUES (1, 1300);
INSERT INTO `sys_role_menu` VALUES (1, 1301);
INSERT INTO `sys_role_menu` VALUES (1, 1302);
INSERT INTO `sys_role_menu` VALUES (1, 1303);
INSERT INTO `sys_role_menu` VALUES (1, 1304);
INSERT INTO `sys_role_menu` VALUES (1, 1305);
INSERT INTO `sys_role_menu` VALUES (1, 1400);
INSERT INTO `sys_role_menu` VALUES (1, 1401);
INSERT INTO `sys_role_menu` VALUES (1, 1402);
INSERT INTO `sys_role_menu` VALUES (1, 1403);
INSERT INTO `sys_role_menu` VALUES (1, 1600);
INSERT INTO `sys_role_menu` VALUES (1, 1601);
INSERT INTO `sys_role_menu` VALUES (1, 1602);
INSERT INTO `sys_role_menu` VALUES (1, 1603);
INSERT INTO `sys_role_menu` VALUES (1, 1604);
INSERT INTO `sys_role_menu` VALUES (1, 1605);
INSERT INTO `sys_role_menu` VALUES (1, 2000);
INSERT INTO `sys_role_menu` VALUES (1, 2001);
INSERT INTO `sys_role_menu` VALUES (1, 2100);
INSERT INTO `sys_role_menu` VALUES (1, 2101);
INSERT INTO `sys_role_menu` VALUES (1, 2102);
INSERT INTO `sys_role_menu` VALUES (1, 2200);
INSERT INTO `sys_role_menu` VALUES (1, 2201);
INSERT INTO `sys_role_menu` VALUES (1, 2202);
INSERT INTO `sys_role_menu` VALUES (1, 2203);
INSERT INTO `sys_role_menu` VALUES (1, 2210);
INSERT INTO `sys_role_menu` VALUES (1, 2211);
INSERT INTO `sys_role_menu` VALUES (1, 2212);
INSERT INTO `sys_role_menu` VALUES (1, 2213);
INSERT INTO `sys_role_menu` VALUES (1, 2300);
INSERT INTO `sys_role_menu` VALUES (1, 2400);
INSERT INTO `sys_role_menu` VALUES (1, 2401);
INSERT INTO `sys_role_menu` VALUES (1, 2402);
INSERT INTO `sys_role_menu` VALUES (1, 2403);
INSERT INTO `sys_role_menu` VALUES (1, 2600);
INSERT INTO `sys_role_menu` VALUES (1, 2601);
INSERT INTO `sys_role_menu` VALUES (1, 2800);
INSERT INTO `sys_role_menu` VALUES (1, 2810);
INSERT INTO `sys_role_menu` VALUES (1, 2820);
INSERT INTO `sys_role_menu` VALUES (1, 2830);
INSERT INTO `sys_role_menu` VALUES (1, 2840);
INSERT INTO `sys_role_menu` VALUES (1, 2850);
INSERT INTO `sys_role_menu` VALUES (1, 2860);
INSERT INTO `sys_role_menu` VALUES (1, 2870);
INSERT INTO `sys_role_menu` VALUES (1, 2871);
INSERT INTO `sys_role_menu` VALUES (1, 2906);
INSERT INTO `sys_role_menu` VALUES (1, 2907);
INSERT INTO `sys_role_menu` VALUES (1, 4000);
INSERT INTO `sys_role_menu` VALUES (1, 4001);
INSERT INTO `sys_role_menu` VALUES (1, 4002);
INSERT INTO `sys_role_menu` VALUES (1, 9000);
INSERT INTO `sys_role_menu` VALUES (1, 9005);
INSERT INTO `sys_role_menu` VALUES (1, 9006);
INSERT INTO `sys_role_menu` VALUES (1, 9007);
INSERT INTO `sys_role_menu` VALUES (1, 9050);
INSERT INTO `sys_role_menu` VALUES (1, 9051);
INSERT INTO `sys_role_menu` VALUES (1, 9052);
INSERT INTO `sys_role_menu` VALUES (1, 9053);
INSERT INTO `sys_role_menu` VALUES (1, 9054);
INSERT INTO `sys_role_menu` VALUES (1, 9055);
INSERT INTO `sys_role_menu` VALUES (1, 9056);
INSERT INTO `sys_role_menu` VALUES (1, 9057);
INSERT INTO `sys_role_menu` VALUES (1, 9059);
INSERT INTO `sys_role_menu` VALUES (1, 9060);
INSERT INTO `sys_role_menu` VALUES (1, 9061);
INSERT INTO `sys_role_menu` VALUES (1, 9062);
INSERT INTO `sys_role_menu` VALUES (1, 9063);
INSERT INTO `sys_role_menu` VALUES (1, 9064);
INSERT INTO `sys_role_menu` VALUES (1, 9065);
INSERT INTO `sys_role_menu` VALUES (1, 1742130402867);
INSERT INTO `sys_role_menu` VALUES (1, 1742130402869);
INSERT INTO `sys_role_menu` VALUES (1, 1742130402870);
INSERT INTO `sys_role_menu` VALUES (1, 1742130402871);
INSERT INTO `sys_role_menu` VALUES (1, 1742130402872);
INSERT INTO `sys_role_menu` VALUES (1, 1743227371409);
INSERT INTO `sys_role_menu` VALUES (1, 1743227371410);
INSERT INTO `sys_role_menu` VALUES (1, 1743227371411);
INSERT INTO `sys_role_menu` VALUES (1, 1743227371412);
INSERT INTO `sys_role_menu` VALUES (1, 1743227371413);
INSERT INTO `sys_role_menu` VALUES (1, 1743227371414);
INSERT INTO `sys_role_menu` VALUES (1, 1743318978196);
INSERT INTO `sys_role_menu` VALUES (1, 1743318978197);
INSERT INTO `sys_role_menu` VALUES (1, 1743318978198);
INSERT INTO `sys_role_menu` VALUES (1, 1743318978199);
INSERT INTO `sys_role_menu` VALUES (1, 1743318978200);
INSERT INTO `sys_role_menu` VALUES (1, 1743318978201);
INSERT INTO `sys_role_menu` VALUES (1, 1746080528758);
INSERT INTO `sys_role_menu` VALUES (1, 1746080528760);
INSERT INTO `sys_role_menu` VALUES (1, 1746080528761);
INSERT INTO `sys_role_menu` VALUES (1, 1746080528762);
INSERT INTO `sys_role_menu` VALUES (1, 1746080528763);
INSERT INTO `sys_role_menu` VALUES (1, 1746334604170);
INSERT INTO `sys_role_menu` VALUES (1, 1746334604172);
INSERT INTO `sys_role_menu` VALUES (1, 1746334604173);
INSERT INTO `sys_role_menu` VALUES (1, 1746334604174);
INSERT INTO `sys_role_menu` VALUES (1, 1901265970396176385);
INSERT INTO `sys_role_menu` VALUES (1, 1913445900512940034);
INSERT INTO `sys_role_menu` VALUES (1, 1913505265764814850);
INSERT INTO `sys_role_menu` VALUES (1, 1913505381577936898);
INSERT INTO `sys_role_menu` VALUES (1, 1913505476759277570);
INSERT INTO `sys_role_menu` VALUES (1, 1913505537257918465);
INSERT INTO `sys_role_menu` VALUES (1, 1913505609009876993);
INSERT INTO `sys_role_menu` VALUES (1, 1913587779967668225);
INSERT INTO `sys_role_menu` VALUES (1, 1918194239292833794);
INSERT INTO `sys_role_menu` VALUES (1, 1956950686046371842);
INSERT INTO `sys_role_menu` VALUES (1, 1965783565920636929);
INSERT INTO `sys_role_menu` VALUES (1, 1989630040934592514);
INSERT INTO `sys_role_menu` VALUES (2, 4000);
INSERT INTO `sys_role_menu` VALUES (2, 4001);
INSERT INTO `sys_role_menu` VALUES (2, 4002);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '密码',
  `salt` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '盐值',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '电话号码',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '姓名',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱地址',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '所属部门ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `lock_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '锁定标记，0未锁定，9已锁定',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标记，0未删除，1已删除',
  `wx_openid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '微信登录openId',
  `mini_openid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '小程序openId',
  `qq_openid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'QQ openId',
  `gitee_login` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '码云标识',
  `osc_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '开源中国标识',
  PRIMARY KEY (`user_id`) USING BTREE,
  INDEX `user_wx_openid`(`wx_openid` ASC) USING BTREE,
  INDEX `user_qq_openid`(`qq_openid` ASC) USING BTREE,
  INDEX `user_idx1_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$c/Ae0pRjJtMZg3BnvVpO.eIK6WYWVbKTzqgdy3afR7w.vd.xi3Mgy', '', '17034642999', '/api/admin/sys-file/s3demo/246ee39e58d644d4af72fe3076bd5b86.png', '管理员', '管理员', 'pig4cloud@qq.com', 4, ' ', 'admin', '2018-04-20 07:15:18', '2026-03-26 16:17:24', '0', '0', NULL, 'oBxPy5E-v82xWGsfzZVzkD3wEX64', NULL, 'log4j', NULL);

-- ----------------------------
-- Table structure for sys_user_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post`  (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`, `post_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户与岗位关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_post
-- ----------------------------
INSERT INTO `sys_user_post` VALUES (1, 1);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1);
INSERT INTO `sys_user_role` VALUES (1676492190299299842, 2);

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

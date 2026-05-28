DROP DATABASE IF EXISTS `nqboard_flw`;

CREATE DATABASE  `nqboard_flw` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

USE nqboard_flw;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for flw_expression
-- ----------------------------
DROP TABLE IF EXISTS `flw_expression`;
CREATE TABLE `flw_expression`
(
    `id`          bigint(20) NOT NULL COMMENT 'id',
    `order_num`   int(11) NULL DEFAULT NULL COMMENT '排序字段',
    `create_by`   varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
    `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
    `update_by`   varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '修改人',
    `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
    `del_flag`    char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
    `name`        varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '名称',
    `expression`  text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '表达式内容',
    `data_type`   varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '表达式类型',
    `status`      char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '状态（0成功、1失败）',
    `remark`      varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述信息',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX         `idx_order_num`(`order_num`) USING BTREE,
    INDEX         `idx_create_time`(`create_time`) USING BTREE,
    INDEX         `idx_del_flag`(`del_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '流程表达式' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of flw_expression
-- ----------------------------
INSERT INTO `flw_expression`
VALUES (1987100822665359361, 1, 'spicy', '2025-11-08 18:12:09', 'spicy', '2025-11-08 18:12:09', '0', '当前用户',
        '${current}', 'dynamic', '0', 'ces');


-- ----------------------------
-- Table structure for flw_listener
-- ----------------------------
DROP TABLE IF EXISTS `flw_listener`;
CREATE TABLE `flw_listener`
(
    `id`          bigint(20) NOT NULL,
    `create_by`   varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '创建人',
    `create_time` datetime                                                     NOT NULL COMMENT '创建时间',
    `update_by`   varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '修改人',
    `update_time` datetime                                                     NOT NULL COMMENT '修改时间',
    `order_num`   int(11) NULL DEFAULT NULL COMMENT '排序字段',
    `del_flag`    char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NOT NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
    `name`        varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '名称',
    `type`        varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '监听类型（1任务监听、2执行监听）',
    `event_type`  varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '事件类型 任务监听：（create、assignment、complete、delete） 执行监听（start、end、take）',
    `value_type`  varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '值类型（classListener：Java类、expressionListener：表达式、delegateExpressionListener代理表达式）',
    `value`       text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '执行内容',
    `status`      int(11) NULL DEFAULT NULL COMMENT '状态',
    `remark`      varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述信息',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX         `idx_del_flag`(`del_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '流程监听器' ROW_FORMAT = DYNAMIC;


-- ----------------------------
-- Table structure for undo_log
-- ----------------------------
DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log` (
    `branch_id` bigint NOT NULL COMMENT 'branch transaction id',
    `xid` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'global transaction id',
    `context` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'undo_log context,such as serialization',
    `rollback_info` longblob NOT NULL COMMENT 'rollback info',
    `log_status` int NOT NULL COMMENT '0:normal status,1:defense status',
    `log_created` datetime(6) NOT NULL COMMENT 'create datetime',
    `log_modified` datetime(6) NOT NULL COMMENT 'modify datetime',
    UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`) USING BTREE,
    KEY `ix_log_created` (`log_created`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='AT transaction mode undo table';
-- ----------------------------
-- nqboard_export 专家抽取管理库
-- ----------------------------

DROP DATABASE IF EXISTS `nqboard_export`;

CREATE DATABASE `nqboard_export` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE nqboard_export;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for expert_info
-- ----------------------------
DROP TABLE IF EXISTS `expert_info`;
CREATE TABLE `expert_info`  (
  `id` bigint NOT NULL COMMENT '专家id',
  `expert_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '专家名称',
  `subject_category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '学科门类',
  `first_discipline` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '一级学科',
  `second_discipline` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '二级学科',
  `research_direction` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '研究方向',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '专家表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

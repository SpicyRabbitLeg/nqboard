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
  `domain_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '领域编码（AI打标）',
  `domain_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '领域名称',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_domain`(`domain_code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '专家表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for extract_domain
-- ----------------------------
DROP TABLE IF EXISTS `extract_domain`;
CREATE TABLE `extract_domain`  (
  `id` bigint NOT NULL COMMENT '领域id',
  `domain_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '领域编码',
  `domain_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '领域名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '领域描述（AI判定依据）',
  `keywords` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '关键词（逗号分隔）',
  `adjacent_codes` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邻接领域编码JSON数组',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态（0启用、1停用）',
  `seq` int NULL DEFAULT NULL COMMENT '排序',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_domain_code`(`domain_code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '专家抽取领域表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for extract_record
-- ----------------------------
DROP TABLE IF EXISTS `extract_record`;
CREATE TABLE `extract_record`  (
  `id` bigint NOT NULL COMMENT '记录id',
  `query_text` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户自然语言查询',
  `parsed_domains` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '解析出的领域编码JSON数组',
  `parsed_keywords` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '解析出的关键词JSON数组',
  `selected_count` int NULL DEFAULT 0 COMMENT '已选数量',
  `candidate_count` int NULL DEFAULT 0 COMMENT '候选数量',
  `unmatched_count` int NULL DEFAULT 0 COMMENT '未匹配数量（抽取时点快照）',
  `cost_ms` int NULL DEFAULT NULL COMMENT '耗时（毫秒）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态（0成功、1失败）',
  `fail_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '失败原因',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '专家抽取记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for extract_record_detail
-- ----------------------------
DROP TABLE IF EXISTS `extract_record_detail`;
CREATE TABLE `extract_record_detail`  (
  `id` bigint NOT NULL COMMENT '明细id',
  `record_id` bigint NOT NULL COMMENT '抽取记录id',
  `expert_id` bigint NOT NULL COMMENT '专家id',
  `expert_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '专家名称（冗余）',
  `subject_category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '学科门类（冗余）',
  `first_discipline` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '一级学科（冗余）',
  `grade` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '档位（1已选、2候选）',
  `score` decimal(8, 6) NULL DEFAULT NULL COMMENT '匹配得分',
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '命中理由',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_record_grade`(`record_id`, `grade`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '专家抽取记录明细表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

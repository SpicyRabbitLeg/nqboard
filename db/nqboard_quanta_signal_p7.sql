-- =============================================================
-- P7 试运行观察体系
-- 1. stock_candidate_hit：候选池信号命中率追踪表
--    （信号日候选 -> 次日开盘入场口径的前向收益，T+1/T+3/T+5）
-- 2. sys_job：每周回测校准定时任务（周六上午，用最新数据重跑校准）
-- 3. sys_menu：命中率日报前端菜单
-- =============================================================

USE `nqboard_quanta`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 候选池信号命中率追踪（P7）
-- ----------------------------
DROP TABLE IF EXISTS `stock_candidate_hit`;
CREATE TABLE `stock_candidate_hit` (
   `id` bigint NOT NULL COMMENT '业务id（雪花ID）',
   `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
   `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
   `create_time` datetime DEFAULT NULL COMMENT '入库时间',
   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
   `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记,1:已删除,0:正常',
   `order_num` int DEFAULT 0 COMMENT '排序字段',
   `trade_date` varchar(16) NOT NULL COMMENT '信号日 YYYYMMDD',
   `ts_code` varchar(32) NOT NULL COMMENT 'TS股票代码',
   `name` varchar(64) DEFAULT NULL COMMENT '股票名称',
   `pattern` varchar(32) DEFAULT NULL COMMENT '命中模板',
   `screen_score` decimal(8,2) DEFAULT NULL COMMENT '规则打分',
   `llm_score` decimal(8,4) DEFAULT NULL COMMENT 'LLM加权分',
   `decision_mode` varchar(16) DEFAULT 'rules' COMMENT 'agent/rules_fallback（A/B对比维度）',
   `confidence` int DEFAULT NULL COMMENT '置信度',
   `entry_price` decimal(12,4) DEFAULT NULL COMMENT '入场价（信号日次一交易日开盘价）',
   `entry_skipped` varchar(32) DEFAULT NULL COMMENT '未入场原因 gap_up/gap_down/suspended',
   `fwd_1d` decimal(10,4) DEFAULT NULL COMMENT '入场日收盘收益（gross）',
   `fwd_3d` decimal(10,4) DEFAULT NULL COMMENT '入场后第3个交易日收盘收益（gross）',
   `fwd_5d` decimal(10,4) DEFAULT NULL COMMENT '入场后第5个交易日收盘收益（gross）',
   `best_ret` decimal(10,4) DEFAULT NULL COMMENT '持有期内最高收盘收益（gross）',
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_date_ts` (`trade_date`, `ts_code`),
   KEY `idx_trade_date` (`trade_date`),
   KEY `idx_decision_mode` (`decision_mode`),
   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='候选池信号命中率追踪';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================
-- 主库：Quartz 任务 + 前端菜单
-- =============================================================
USE `nqboard`;

-- 每周回测校准（周六上午跑一次最新数据回测，校准报告供人工review阈值）
INSERT INTO `sys_job` (`job_id`, `job_name`, `job_group`, `job_order`, `job_type`, `execute_path`, `class_name`, `method_name`, `method_params_value`, `cron_expression`, `misfire_policy`, `job_tenant_type`, `job_status`, `job_execute_status`, `create_by`, `create_time`, `remark`)
VALUES (2090600000000000002, '每周回测校准', 'DEFAULT', '5', '2', NULL, 'stockBacktestTaskService', 'runLatest', NULL, '0 0 10 ? * SAT', '3', '2', '2', '0', 'admin', NOW(), '每周六10:00重跑回测（最新数据校准入池线与模板阈值），任务结果在回测中心页面查看');

-- 命中率日报菜单
INSERT INTO `sys_menu` (`menu_id`, `name`, `en_name`, `permission`, `path`, `parent_id`, `icon`, `visible`, `sort_order`, `keep_alive`, `embedded`, `menu_type`, `create_by`, `create_time`, `del_flag`) VALUES (2090602000000000005, '命中率日报', 'hitRate', NULL, '/quanta/hitRate/index', 2090421625264017410, 'iconfont icon-baobiao', '1', 8, '0', '0', '0', 'admin', NOW(), '0');

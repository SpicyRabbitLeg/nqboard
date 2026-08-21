-- =============================================================
-- 短线候选池系统（NQBoard Quanta Signal）
-- 数据同步层：stock_money_flow / stock_industry_daily / stock_restricted_release
-- 流水线层：quant_pipeline_log
-- 业务层：stock_screen_result / stock_agent_analysis / stock_candidate /
--         stock_sim_position / stock_position_daily /
--         stock_backtest_task / stock_backtest_trade
-- =============================================================

USE `nqboard_quanta`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 个股主力资金流（东财 clist 资金流排名接口，日频快照）
-- ----------------------------
DROP TABLE IF EXISTS `stock_money_flow`;
CREATE TABLE `stock_money_flow` (
   `id` bigint NOT NULL COMMENT '业务id（雪花ID）',
   `ts_code` varchar(32) NOT NULL COMMENT 'TS股票代码 600519.SH',
   `name` varchar(64) DEFAULT NULL COMMENT '股票名称',
   `industry_name` varchar(64) DEFAULT NULL COMMENT '所属行业（东财口径，用于关联行业板块）',
   `trade_date` varchar(16) NOT NULL COMMENT '交易日期 YYYYMMDD',
   `close` decimal(12,4) DEFAULT NULL COMMENT '收盘价（快照价）',
   `pct_chg` decimal(10,4) DEFAULT NULL COMMENT '涨跌幅%',
   `main_net_inflow` decimal(20,4) DEFAULT NULL COMMENT '主力净流入额(元)',
   `main_net_pct` decimal(10,4) DEFAULT NULL COMMENT '主力净流入占比%',
   `super_large_net` decimal(20,4) DEFAULT NULL COMMENT '超大单净流入额(元)',
   `large_net` decimal(20,4) DEFAULT NULL COMMENT '大单净流入额(元)',
   `medium_net` decimal(20,4) DEFAULT NULL COMMENT '中单净流入额(元)',
   `small_net` decimal(20,4) DEFAULT NULL COMMENT '小单净流入额(元)',
   `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
   `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
   `create_time` datetime DEFAULT NULL COMMENT '入库时间',
   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
   `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记,1:已删除,0:正常',
   `order_num` int DEFAULT 0 COMMENT '排序字段',
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_ts_date` (`ts_code`, `trade_date`),
   KEY `idx_trade_date` (`trade_date`),
   KEY `idx_industry` (`industry_name`),
   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个股主力资金流（东财日频快照）';

-- ----------------------------
-- 行业板块日线（东财 push2his 板块K线，secid=90.BKxxxx）
-- ----------------------------
DROP TABLE IF EXISTS `stock_industry_daily`;
CREATE TABLE `stock_industry_daily` (
   `id` bigint NOT NULL COMMENT '业务id（雪花ID）',
   `board_code` varchar(16) NOT NULL COMMENT '板块代码 BK0475',
   `board_name` varchar(64) DEFAULT NULL COMMENT '板块名称（如：银行）',
   `trade_date` varchar(16) NOT NULL COMMENT '交易日期 YYYYMMDD',
   `open` decimal(16,4) DEFAULT NULL COMMENT '开盘价',
   `high` decimal(16,4) DEFAULT NULL COMMENT '最高价',
   `low` decimal(16,4) DEFAULT NULL COMMENT '最低价',
   `close` decimal(16,4) DEFAULT NULL COMMENT '收盘价',
   `volume` decimal(24,0) DEFAULT NULL COMMENT '成交量',
   `amount` decimal(24,4) DEFAULT NULL COMMENT '成交额(元)',
   `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
   `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
   `create_time` datetime DEFAULT NULL COMMENT '入库时间',
   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
   `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记,1:已删除,0:正常',
   `order_num` int DEFAULT 0 COMMENT '排序字段',
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_board_date` (`board_code`, `trade_date`),
   KEY `idx_board_name` (`board_name`),
   KEY `idx_trade_date` (`trade_date`),
   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行业板块日线K线（东财）';

-- ----------------------------
-- 限售解禁（tushare share_float）
-- ----------------------------
DROP TABLE IF EXISTS `stock_restricted_release`;
CREATE TABLE `stock_restricted_release` (
   `id` bigint NOT NULL COMMENT '业务id（雪花ID）',
   `ts_code` varchar(32) NOT NULL COMMENT 'TS股票代码',
   `ann_date` varchar(16) DEFAULT NULL COMMENT '公告日期 YYYYMMDD',
   `float_date` varchar(16) NOT NULL COMMENT '解禁日期 YYYYMMDD',
   `float_share` decimal(20,4) DEFAULT NULL COMMENT '解禁数量(万股)',
   `float_ratio` decimal(12,4) DEFAULT NULL COMMENT '解禁数量占总股本比例%',
   `holder_name` varchar(256) DEFAULT NULL COMMENT '解禁股东名称',
   `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
   `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
   `create_time` datetime DEFAULT NULL COMMENT '入库时间',
   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
   `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记,1:已删除,0:正常',
   `order_num` int DEFAULT 0 COMMENT '排序字段',
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_ts_float_holder` (`ts_code`, `float_date`, `holder_name`),
   KEY `idx_float_date` (`float_date`),
   KEY `idx_ts_code` (`ts_code`),
   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限售解禁（tushare share_float）';

-- ----------------------------
-- 盘后流水线执行日志
-- ----------------------------
DROP TABLE IF EXISTS `quant_pipeline_log`;
CREATE TABLE `quant_pipeline_log` (
   `id` bigint NOT NULL COMMENT '业务id（雪花ID）',
   `run_id` varchar(64) NOT NULL COMMENT '本次流水线运行id',
   `run_date` varchar(16) NOT NULL COMMENT '运行日期 YYYYMMDD',
   `step` varchar(32) NOT NULL COMMENT '步骤编码 index_daily/stock_daily/...',
   `step_name` varchar(64) DEFAULT NULL COMMENT '步骤名称',
   `status` varchar(16) NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/SUCCESS/FAILED/SKIPPED',
   `affected` int DEFAULT NULL COMMENT '影响行数（同步条数等）',
   `message` varchar(1000) DEFAULT NULL COMMENT '执行说明',
   `exception` varchar(2000) DEFAULT NULL COMMENT '异常信息（截断）',
   `begin_time` datetime DEFAULT NULL COMMENT '开始时间',
   `end_time` datetime DEFAULT NULL COMMENT '结束时间',
   `elapsed_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
   `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
   `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
   `create_time` datetime DEFAULT NULL COMMENT '入库时间',
   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
   `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记,1:已删除,0:正常',
   `order_num` int DEFAULT 0 COMMENT '排序字段',
   PRIMARY KEY (`id`),
   KEY `idx_run_id` (`run_id`),
   KEY `idx_run_date_step` (`run_date`, `step`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘后流水线执行日志';

-- ----------------------------
-- 每日筛选打分结果（Stage 0/0.5/1，P2 使用）
-- ----------------------------
DROP TABLE IF EXISTS `stock_screen_result`;
CREATE TABLE `stock_screen_result` (
   `id` bigint NOT NULL COMMENT '业务id（雪花ID）',
   `trade_date` varchar(16) NOT NULL COMMENT '信号日（打分依据日）YYYYMMDD',
   `ts_code` varchar(32) NOT NULL COMMENT 'TS股票代码',
   `name` varchar(64) DEFAULT NULL COMMENT '股票名称',
   `screen_score` decimal(8,2) DEFAULT NULL COMMENT '综合打分（模板分+上下文加分，满分100）',
   `pattern` varchar(32) DEFAULT NULL COMMENT '命中模板 breakout/pullback/trend_accel/oversold/none',
   `pattern_score` decimal(8,2) DEFAULT NULL COMMENT '模板基础分',
   `passed` char(1) NOT NULL DEFAULT '0' COMMENT '是否通过打分入池线 1:通过 0:未通过',
   `reject_reason` varchar(512) DEFAULT NULL COMMENT '硬门否决原因（多个分号分隔）',
   `metrics` json DEFAULT NULL COMMENT '特征向量（动量/量比/RSI/trend_strength等）',
   `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
   `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
   `create_time` datetime DEFAULT NULL COMMENT '入库时间',
   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
   `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记,1:已删除,0:正常',
   `order_num` int DEFAULT 0 COMMENT '排序字段',
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_date_ts` (`trade_date`, `ts_code`),
   KEY `idx_trade_date` (`trade_date`),
   KEY `idx_passed` (`passed`),
   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日筛选打分结果';

-- ----------------------------
-- LLM(Dify) 逐 Agent 分析结果（P5 使用）
-- ----------------------------
DROP TABLE IF EXISTS `stock_agent_analysis`;
CREATE TABLE `stock_agent_analysis` (
   `id` bigint NOT NULL COMMENT '业务id（雪花ID）',
   `trade_date` varchar(16) NOT NULL COMMENT '分析基准日 YYYYMMDD',
   `ts_code` varchar(32) NOT NULL COMMENT 'TS股票代码',
   `agent_key` varchar(32) NOT NULL COMMENT '分析师标识 technical/sector/money_flow/dragon_tiger/news/policy',
   `signal` varchar(16) DEFAULT NULL COMMENT 'bullish/bearish/neutral/n/a',
   `confidence` int DEFAULT NULL COMMENT '置信度 0-100',
   `reasoning` varchar(1000) DEFAULT NULL COMMENT '推理摘要',
   `decision_mode` varchar(16) DEFAULT 'agent' COMMENT 'agent=LLM分析 rules_fallback=规则降级',
   `model_name` varchar(64) DEFAULT NULL COMMENT '使用的模型',
   `latency_ms` int DEFAULT NULL COMMENT '分析耗时毫秒',
   `tool_calls` int DEFAULT NULL COMMENT '工具调用次数',
   `raw_output` text COMMENT 'Agent原始输出全文（调试用）',
   `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
   `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
   `create_time` datetime DEFAULT NULL COMMENT '入库时间',
   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
   `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记,1:已删除,0:正常',
   `order_num` int DEFAULT 0 COMMENT '排序字段',
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_date_ts_agent` (`trade_date`, `ts_code`, `agent_key`),
   KEY `idx_trade_date` (`trade_date`),
   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM(Dify)逐Agent分析结果';

-- ----------------------------
-- 候选股票池（P4 使用）
-- ----------------------------
DROP TABLE IF EXISTS `stock_candidate`;
CREATE TABLE `stock_candidate` (
   `id` bigint NOT NULL COMMENT '业务id（雪花ID）',
   `trade_date` varchar(16) NOT NULL COMMENT '入选日（信号日）YYYYMMDD',
   `ts_code` varchar(32) NOT NULL COMMENT 'TS股票代码',
   `name` varchar(64) DEFAULT NULL COMMENT '股票名称',
   `screen_score` decimal(8,2) DEFAULT NULL COMMENT '规则打分',
   `llm_score` decimal(8,4) DEFAULT NULL COMMENT 'LLM加权分',
   `confidence` int DEFAULT NULL COMMENT '综合置信度 0-100',
   `action` varchar(16) DEFAULT NULL COMMENT 'entry_ok/watch/avoid',
   `pattern` varchar(32) DEFAULT NULL COMMENT '命中模板',
   `reasons` json DEFAULT NULL COMMENT '看多理由标签列表',
   `agent_summary` json DEFAULT NULL COMMENT '各Agent结论摘要',
   `exit_plan` json DEFAULT NULL COMMENT '离场计划（止损价/止盈价/最大持有天数）',
   `market_ret_5d` decimal(10,6) DEFAULT NULL COMMENT '沪深300 5日收益',
   `decision_mode` varchar(16) DEFAULT 'rules' COMMENT 'agent/rules_fallback',
   `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/EXPIRED/REMOVED',
   `expire_date` varchar(16) DEFAULT NULL COMMENT '过期日（入选日+5交易日）',
   `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
   `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
   `create_time` datetime DEFAULT NULL COMMENT '入库时间',
   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
   `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记,1:已删除,0:正常',
   `order_num` int DEFAULT 0 COMMENT '排序字段',
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_date_ts` (`trade_date`, `ts_code`),
   KEY `idx_status` (`status`),
   KEY `idx_ts_code` (`ts_code`),
   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='候选股票池';

-- ----------------------------
-- 模拟持仓（P4 使用）
-- ----------------------------
DROP TABLE IF EXISTS `stock_sim_position`;
CREATE TABLE `stock_sim_position` (
   `id` bigint NOT NULL COMMENT '业务id（雪花ID）',
   `candidate_id` bigint DEFAULT NULL COMMENT '来源候选记录id',
   `ts_code` varchar(32) NOT NULL COMMENT 'TS股票代码',
   `buy_date` varchar(16) NOT NULL COMMENT '实际买入日（信号日次一交易日）',
   `buy_price` decimal(12,4) NOT NULL COMMENT '买入价（次日开盘价）',
   `qty` int NOT NULL COMMENT '买入数量（100股整手）',
   `cost` decimal(16,2) NOT NULL COMMENT '买入成本（含佣金）',
   `stop_price` decimal(12,4) DEFAULT NULL COMMENT '止损价（买入价*0.95）',
   `target_price` decimal(12,4) DEFAULT NULL COMMENT '止盈价（买入价*1.15）',
   `max_hold_days` int DEFAULT 5 COMMENT '最大持有交易日数',
   `status` varchar(16) NOT NULL DEFAULT 'PENDING_BUY' COMMENT 'PENDING_BUY/HOLDING/PENDING_SELL/EXITED',
   `exit_date` varchar(16) DEFAULT NULL COMMENT '卖出日',
   `exit_price` decimal(12,4) DEFAULT NULL COMMENT '卖出价',
   `exit_reason` varchar(32) DEFAULT NULL COMMENT 'stop_loss/take_profit/time_exit/gap_stop/weak_exit/manual/open_at_end',
   `proceeds` decimal(16,2) DEFAULT NULL COMMENT '卖出净得（扣佣金印花税）',
   `pnl` decimal(16,2) DEFAULT NULL COMMENT '盈亏额',
   `ret` decimal(10,4) DEFAULT NULL COMMENT '收益率',
   `held_days` int DEFAULT NULL COMMENT '持有交易日数',
   `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
   `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
   `create_time` datetime DEFAULT NULL COMMENT '入库时间',
   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
   `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记,1:已删除,0:正常',
   `order_num` int DEFAULT 0 COMMENT '排序字段',
   PRIMARY KEY (`id`),
   KEY `idx_ts_code` (`ts_code`),
   KEY `idx_status` (`status`),
   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模拟持仓';

-- ----------------------------
-- 模拟持仓逐日盯市（P4 使用）
-- ----------------------------
DROP TABLE IF EXISTS `stock_position_daily`;
CREATE TABLE `stock_position_daily` (
   `id` bigint NOT NULL COMMENT '业务id（雪花ID）',
   `position_id` bigint NOT NULL COMMENT '持仓id',
   `trade_date` varchar(16) NOT NULL COMMENT '交易日期 YYYYMMDD',
   `close` decimal(12,4) DEFAULT NULL COMMENT '当日收盘价',
   `day_pnl` decimal(16,2) DEFAULT NULL COMMENT '当日盈亏',
   `cum_pnl` decimal(16,2) DEFAULT NULL COMMENT '累计盈亏',
   `cum_ret` decimal(10,4) DEFAULT NULL COMMENT '累计收益率',
   `action` varchar(16) DEFAULT 'HOLD' COMMENT 'HOLD/SELL/PENDING_SELL',
   `action_reason` varchar(255) DEFAULT NULL COMMENT '动作说明（触发原因/剩余天数）',
   `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
   `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
   `create_time` datetime DEFAULT NULL COMMENT '入库时间',
   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
   `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记,1:已删除,0:正常',
   `order_num` int DEFAULT 0 COMMENT '排序字段',
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_position_date` (`position_id`, `trade_date`),
   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模拟持仓逐日盯市';

-- ----------------------------
-- 回测任务（P3 使用）
-- ----------------------------
DROP TABLE IF EXISTS `stock_backtest_task`;
CREATE TABLE `stock_backtest_task` (
   `id` bigint NOT NULL COMMENT '业务id（雪花ID）',
   `params` json DEFAULT NULL COMMENT '回测参数（universe/days/min_score/capital等）',
   `status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/DONE/FAILED',
   `progress` int DEFAULT 0 COMMENT '进度百分比 0-100',
   `stats` json DEFAULT NULL COMMENT '统计结果（胜率/盈亏比/回撤/离场分布）',
   `equity_curve` json DEFAULT NULL COMMENT '权益曲线 [{date,equity}]',
   `error_msg` varchar(1000) DEFAULT NULL COMMENT '失败原因',
   `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
   `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
   `create_time` datetime DEFAULT NULL COMMENT '入库时间',
   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
   `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记,1:已删除,0:正常',
   `order_num` int DEFAULT 0 COMMENT '排序字段',
   PRIMARY KEY (`id`),
   KEY `idx_status` (`status`),
   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回测任务';

-- ----------------------------
-- 回测成交明细（P3 使用）
-- ----------------------------
DROP TABLE IF EXISTS `stock_backtest_trade`;
CREATE TABLE `stock_backtest_trade` (
   `id` bigint NOT NULL COMMENT '业务id（雪花ID）',
   `task_id` bigint NOT NULL COMMENT '回测任务id',
   `ts_code` varchar(32) NOT NULL COMMENT 'TS股票代码',
   `entry_date` varchar(16) NOT NULL COMMENT '买入日',
   `entry_price` decimal(12,4) NOT NULL COMMENT '买入价（开盘价）',
   `exit_date` varchar(16) DEFAULT NULL COMMENT '卖出日',
   `exit_price` decimal(12,4) DEFAULT NULL COMMENT '卖出价（开盘价）',
   `qty` int DEFAULT NULL COMMENT '数量',
   `reason` varchar(32) DEFAULT NULL COMMENT '离场原因 stop_loss/take_profit/time_exit/gap_stop/weak_exit/open_at_end',
   `pnl` decimal(16,2) DEFAULT NULL COMMENT '盈亏额',
   `ret` decimal(10,4) DEFAULT NULL COMMENT '收益率',
   `held_days` int DEFAULT NULL COMMENT '持有交易日数',
   `signal_score` decimal(8,2) DEFAULT NULL COMMENT '信号日打分',
   `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
   `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
   `create_time` datetime DEFAULT NULL COMMENT '入库时间',
   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
   `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记,1:已删除,0:正常',
   `order_num` int DEFAULT 0 COMMENT '排序字段',
   PRIMARY KEY (`id`),
   KEY `idx_task_id` (`task_id`),
   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回测成交明细';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================
-- Quartz 定时任务注册（主库 nqboard 的 sys_job 表）
-- 单体模式（nqboard-boot）下 quartz 以 spring bean 方式反射调用
-- =============================================================
USE `nqboard`;

INSERT INTO `sys_job` (`job_id`, `job_name`, `job_group`, `job_order`, `job_type`, `execute_path`, `class_name`, `method_name`, `method_params_value`, `cron_expression`, `misfire_policy`, `job_tenant_type`, `job_status`, `job_execute_status`, `create_by`, `create_time`, `remark`)
VALUES (2090600000000000001, '盘后数据流水线', 'DEFAULT', '5', '2', NULL, 'quantPipelineService', 'runPipeline', NULL, '0 0 16 * * ?', '3', '2', '2', '0', 'admin', NOW(), '盘后全量数据同步流水线（指数/日线/龙虎榜/增减持/股东户数/资金流/板块/解禁），周一至周五16:00');

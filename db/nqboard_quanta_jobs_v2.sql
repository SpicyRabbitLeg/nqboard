-- =============================================================
-- 盘后任务调度矩阵 v2（数据同步独立错峰 + 信号流水线晚点执行）
--
-- 架构：数据同步 = 各自独立 Quartz 任务（bean: stockBasic，Feign 触发 quanta）
--       信号计算 = 盘后信号流水线（bean: quantPipelineService，只含 5 个信号步骤）
--
-- 注意：若你已在任务管理页面手工创建过同名/同功能任务，跳过对应 INSERT 即可。
-- 旧任务「盘后数据流水线」(id=2090600000000000001) 已被新架构替代，先删除。
-- =============================================================

USE `nqboard`;

-- 删除旧的一体化流水线任务（被错峰任务矩阵替代）
DELETE FROM `sys_job` WHERE `job_id` = 2090600000000000001;

-- ---------------- 数据同步任务（bean: stockBasic） ----------------
-- 指数日线：东财，收盘后即可
INSERT INTO `sys_job` (`job_id`, `job_name`, `job_group`, `job_order`, `job_type`, `class_name`, `method_name`, `cron_expression`, `misfire_policy`, `job_tenant_type`, `job_status`, `job_execute_status`, `create_by`, `create_time`, `remark`)
VALUES (2090603000000000001, '同步-指数日线', 'DEFAULT', '5', '2', 'stockBasic', 'syncIndexDaily', '0 10 15 ? * MON-FRI', '3', '2', '2', '0', 'admin', NOW(), '东财指数日线K线（工作日15:10）');

-- 股票日线：tushare，全量模式单股票一次请求，约17分钟
INSERT INTO `sys_job` (`job_id`, `job_name`, `job_group`, `job_order`, `job_type`, `class_name`, `method_name`, `cron_expression`, `misfire_policy`, `job_tenant_type`, `job_status`, `job_execute_status`, `create_by`, `create_time`, `remark`)
VALUES (2090603000000000002, '同步-股票日线', 'DEFAULT', '5', '2', 'stockBasic', 'syncDaily', '0 30 15 ? * MON-FRI', '3', '2', '2', '0', 'admin', NOW(), 'tushare日线全量（工作日15:30，约17分钟）');

-- 龙虎榜：tushare，16:30后数据完整
INSERT INTO `sys_job` (`job_id`, `job_name`, `job_group`, `job_order`, `job_type`, `class_name`, `method_name`, `cron_expression`, `misfire_policy`, `job_tenant_type`, `job_status`, `job_execute_status`, `create_by`, `create_time`, `remark`)
VALUES (2090603000000000003, '同步-龙虎榜', 'DEFAULT', '5', '2', 'stockBasic', 'syncTopList', '0 40 16 ? * MON-FRI', '3', '2', '2', '0', 'admin', NOW(), 'tushare龙虎榜每日明细（工作日16:40）');

-- 股东增减持：tushare
INSERT INTO `sys_job` (`job_id`, `job_name`, `job_group`, `job_order`, `job_type`, `class_name`, `method_name`, `cron_expression`, `misfire_policy`, `job_tenant_type`, `job_status`, `job_execute_status`, `create_by`, `create_time`, `remark`)
VALUES (2090603000000000004, '同步-股东增减持', 'DEFAULT', '5', '2', 'stockBasic', 'syncMotHolder', '0 0 17 ? * MON-FRI', '3', '2', '2', '0', 'admin', NOW(), 'tushare股东增减持（工作日17:00）');

-- 股东户数：tushare
INSERT INTO `sys_job` (`job_id`, `job_name`, `job_group`, `job_order`, `job_type`, `class_name`, `method_name`, `cron_expression`, `misfire_policy`, `job_tenant_type`, `job_status`, `job_execute_status`, `create_by`, `create_time`, `remark`)
VALUES (2090603000000000005, '同步-股东户数', 'DEFAULT', '5', '2', 'stockBasic', 'syncMotHolderCount', '0 10 17 ? * MON-FRI', '3', '2', '2', '0', 'admin', NOW(), 'tushare股东户数（工作日17:10）');

-- 主力资金流：东财当日快照
INSERT INTO `sys_job` (`job_id`, `job_name`, `job_group`, `job_order`, `job_type`, `class_name`, `method_name`, `cron_expression`, `misfire_policy`, `job_tenant_type`, `job_status`, `job_execute_status`, `create_by`, `create_time`, `remark`)
VALUES (2090603000000000006, '同步-主力资金流', 'DEFAULT', '5', '2', 'stockBasic', 'syncMoneyFlow', '0 30 17 ? * MON-FRI', '3', '2', '2', '0', 'admin', NOW(), '东财个股主力资金流快照（工作日17:30）');

-- 行业板块日线：东财，86板块约5分钟
INSERT INTO `sys_job` (`job_id`, `job_name`, `job_group`, `job_order`, `job_type`, `class_name`, `method_name`, `cron_expression`, `misfire_policy`, `job_tenant_type`, `job_status`, `job_execute_status`, `create_by`, `create_time`, `remark`)
VALUES (2090603000000000007, '同步-行业板块日线', 'DEFAULT', '5', '2', 'stockBasic', 'syncIndustryDaily', '0 40 17 ? * MON-FRI', '3', '2', '2', '0', 'admin', NOW(), '东财行业板块日线K线（工作日17:40，约5分钟）');

-- 限售解禁：tushare，全量回补90天约45秒（自愈漏跑）
INSERT INTO `sys_job` (`job_id`, `job_name`, `job_group`, `job_order`, `job_type`, `class_name`, `method_name`, `cron_expression`, `misfire_policy`, `job_tenant_type`, `job_status`, `job_execute_status`, `create_by`, `create_time`, `remark`)
VALUES (2090603000000000008, '同步-限售解禁', 'DEFAULT', '5', '2', 'stockBasic', 'syncRestrictedRelease', '0 0 18 ? * MON-FRI', '3', '2', '2', '0', 'admin', NOW(), 'tushare限售解禁（工作日18:00，回补90天）');

-- 股票基础信息：tushare，变动少，每周一次
INSERT INTO `sys_job` (`job_id`, `job_name`, `job_group`, `job_order`, `job_type`, `class_name`, `method_name`, `cron_expression`, `misfire_policy`, `job_tenant_type`, `job_status`, `job_execute_status`, `create_by`, `create_time`, `remark`)
VALUES (2090603000000000009, '同步-股票基础信息', 'DEFAULT', '5', '2', 'stockBasic', 'sync', '0 0 10 ? * SUN', '3', '2', '2', '0', 'admin', NOW(), 'tushare股票基础信息（每周日10:00）');

-- ---------------- 信号流水线（bean: quantPipelineService） ----------------
-- 盘后信号流水线：筛选 -> 候选池 -> LLM分析 -> 持仓跟踪 -> 命中率（数据全部就绪后执行）
-- 筛选步骤自带数据就绪检查（日线覆盖率>=95%），数据缺失时该步骤 FAILED 并给出明确原因
INSERT INTO `sys_job` (`job_id`, `job_name`, `job_group`, `job_order`, `job_type`, `class_name`, `method_name`, `cron_expression`, `misfire_policy`, `job_tenant_type`, `job_status`, `job_execute_status`, `create_by`, `create_time`, `remark`)
VALUES (2090603000000000010, '盘后信号流水线', 'DEFAULT', '5', '2', 'quantPipelineService', 'runPipeline', '0 30 19 ? * MON-FRI', '3', '2', '2', '0', 'admin', NOW(), '信号流水线：筛选打分->候选池->LLM分析->持仓跟踪->命中率（工作日19:30，需数据同步任务先完成）');

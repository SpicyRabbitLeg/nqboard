-- ----------------------------
-- nqboard-sniper 菜单 / 按钮权限 / 字典初始化脚本（T14）
-- 目标库：nqboard（平台主库，非 nqboard_sniper）
-- 号段（2026-10-04 实测 6000~6999 空闲）：菜单 6000~6599、字典 3001~3011、字典项 30010~30199
-- 路由约定：网关全局 StripPrefix=1，菜单 path 为前端完整路径 /sniper/**，
--           Controller @RequestMapping 不带 /sniper 前缀（对齐 device /category）
-- 配套 Nacos 网关路由（nqboard-gateway-dev.yml 手工添加，限流样式对齐 device 路由）：
--   - id: nqboard-sniper-biz
--     uri: lb://nqboard-sniper-biz
--     predicates:
--       - Path=/sniper/**
-- 执行前请核对号段；重复执行前先按 menu_id/dict_type 清理。
-- ----------------------------

USE nqboard;

-- ----------------------------
-- 一、菜单树（§1.4.1：6000 短线狙击目录 → 5 子目录 → 11 菜单页 + 2 隐藏路由 + 8 按钮）
-- ----------------------------

-- 6000 一级目录
INSERT INTO `sys_menu` VALUES (6000, '短线狙击', 'sniper', NULL, '/sniper', -1, 'ele-Aim', '1', 6, '0', '0', '0', 'sniper', NOW(), NULL, NULL, '0');

-- 6100 扫描信号子目录
INSERT INTO `sys_menu` VALUES (6100, '扫描信号', 'scan', NULL, '/sniper/scan', 6000, 'ele-Search', '1', 1, '0', '0', '0', 'sniper', NOW(), NULL, NULL, '0');
INSERT INTO `sys_menu` VALUES (6110, '扫描报告', 'scan-report', NULL, '/sniper/scan/report/index', 6100, 'ele-Document', '1', 1, '1', '0', '1', 'sniper', NOW(), NULL, NULL, '0');
INSERT INTO `sys_menu` VALUES (6114, '报告详情', 'scan-report-detail', NULL, '/sniper/scan/report/detail/:id', 6110, NULL, '0', 1, '0', '0', '1', 'sniper', NOW(), NULL, NULL, '0');
INSERT INTO `sys_menu` VALUES (6120, '信号明细', 'scan-signal', NULL, '/sniper/scan/signal/index', 6100, 'ele-Bell', '1', 2, '0', '0', '1', 'sniper', NOW(), NULL, NULL, '0');
INSERT INTO `sys_menu` VALUES (6130, '被拒明细', 'scan-rejected', NULL, '/sniper/scan/rejected/index', 6100, 'ele-Close', '1', 3, '0', '0', '1', 'sniper', NOW(), NULL, NULL, '0');
-- 扫描报告按钮
INSERT INTO `sys_menu` VALUES (6111, '手动扫描', NULL, 'sniper_scan_run', NULL, 6110, NULL, '1', 1, '0', NULL, '2', 'sniper', NOW(), NULL, NULL, '0');
INSERT INTO `sys_menu` VALUES (6112, '指定票复查', NULL, 'sniper_scan_recheck', NULL, 6110, NULL, '1', 2, '0', NULL, '2', 'sniper', NOW(), NULL, NULL, '0');
INSERT INTO `sys_menu` VALUES (6113, '报告导出', NULL, 'sniper_scan_export', NULL, 6110, NULL, '1', 3, '0', NULL, '2', 'sniper', NOW(), NULL, NULL, '0');

-- 6200 信号台账子目录
INSERT INTO `sys_menu` VALUES (6200, '信号台账', 'ledger', NULL, '/sniper/ledger', 6000, 'ele-Notebook', '1', 2, '0', '0', '0', 'sniper', NOW(), NULL, NULL, '0');
INSERT INTO `sys_menu` VALUES (6210, '仓位台账', 'ledger-position', NULL, '/sniper/ledger/position/index', 6200, 'ele-Wallet', '1', 1, '1', '0', '1', 'sniper', NOW(), NULL, NULL, '0');
INSERT INTO `sys_menu` VALUES (6211, '仓位详情', 'ledger-position-detail', NULL, '/sniper/ledger/position/detail/:id', 6210, NULL, '0', 1, '0', '0', '1', 'sniper', NOW(), NULL, NULL, '0');
-- 仓位台账按钮
INSERT INTO `sys_menu` VALUES (6212, '手动跟踪', NULL, 'sniper_ledger_track', NULL, 6210, NULL, '1', 1, '0', NULL, '2', 'sniper', NOW(), NULL, NULL, '0');
INSERT INTO `sys_menu` VALUES (6213, '台账导出', NULL, 'sniper_ledger_export', NULL, 6210, NULL, '1', 2, '0', NULL, '2', 'sniper', NOW(), NULL, NULL, '0');

-- 6300 复盘分析子目录
INSERT INTO `sys_menu` VALUES (6300, '复盘分析', 'review', NULL, '/sniper/review', 6000, 'ele-DataAnalysis', '1', 3, '0', '0', '0', 'sniper', NOW(), NULL, NULL, '0');
INSERT INTO `sys_menu` VALUES (6310, '复盘看板', 'review-board', NULL, '/sniper/review/index', 6300, 'ele-TrendCharts', '1', 1, '1', '0', '1', 'sniper', NOW(), NULL, NULL, '0');
-- 复盘看板按钮
INSERT INTO `sys_menu` VALUES (6311, '生成复盘', NULL, 'sniper_review_run', NULL, 6310, NULL, '1', 1, '0', NULL, '2', 'sniper', NOW(), NULL, NULL, '0');
INSERT INTO `sys_menu` VALUES (6312, '复盘导出', NULL, 'sniper_review_export', NULL, 6310, NULL, '1', 2, '0', NULL, '2', 'sniper', NOW(), NULL, NULL, '0');

-- 6400 数据资产子目录
INSERT INTO `sys_menu` VALUES (6400, '数据资产', 'data', NULL, '/sniper/data', 6000, 'ele-Coin', '1', 4, '0', '0', '0', 'sniper', NOW(), NULL, NULL, '0');
INSERT INTO `sys_menu` VALUES (6410, '日行情查询', 'data-price', NULL, '/sniper/data/price/index', 6400, 'ele-Histogram', '1', 1, '0', '0', '1', 'sniper', NOW(), NULL, NULL, '0');
INSERT INTO `sys_menu` VALUES (6420, '市场快照', 'data-snapshot', NULL, '/sniper/data/snapshot/index', 6400, 'ele-Monitor', '1', 2, '0', '0', '1', 'sniper', NOW(), NULL, NULL, '0');
INSERT INTO `sys_menu` VALUES (6430, '事件数据', 'data-event', NULL, '/sniper/data/event/index', 6400, 'ele-Message', '1', 3, '0', '0', '1', 'sniper', NOW(), NULL, NULL, '0');
INSERT INTO `sys_menu` VALUES (6440, '基础数据', 'data-basic', NULL, '/sniper/data/basic/index', 6400, 'ele-Grid', '1', 4, '0', '0', '1', 'sniper', NOW(), NULL, NULL, '0');
INSERT INTO `sys_menu` VALUES (6450, '数据刷新', 'data-refresh', NULL, '/sniper/data/refresh/index', 6400, 'ele-Refresh', '1', 5, '0', '0', '1', 'sniper', NOW(), NULL, NULL, '0');
-- 数据刷新按钮
INSERT INTO `sys_menu` VALUES (6451, '触发刷新', NULL, 'sniper_data_refresh', NULL, 6450, NULL, '1', 1, '0', NULL, '2', 'sniper', NOW(), NULL, NULL, '0');

-- 6500 运维监控子目录
INSERT INTO `sys_menu` VALUES (6500, '运维监控', 'ops', NULL, '/sniper/ops', 6000, 'ele-Operation', '1', 5, '0', '0', '0', 'sniper', NOW(), NULL, NULL, '0');
INSERT INTO `sys_menu` VALUES (6510, '任务运行记录', 'ops-dailyrun', NULL, '/sniper/ops/dailyrun/index', 6500, 'ele-Timer', '1', 1, '0', '0', '1', 'sniper', NOW(), NULL, NULL, '0');
INSERT INTO `sys_menu` VALUES (6520, 'LLM调用审计', 'ops-llm', NULL, '/sniper/ops/llm/index', 6500, 'ele-ChatDotRound', '1', 2, '0', '0', '1', 'sniper', NOW(), NULL, NULL, '0');
INSERT INTO `sys_menu` VALUES (6530, '外部请求审计', 'ops-request', NULL, '/sniper/ops/request/index', 6500, 'ele-Connection', '1', 3, '0', '0', '1', 'sniper', NOW(), NULL, NULL, '0');

-- ----------------------------
-- 二、字典（11 个；id 3001~3011）
-- ----------------------------

INSERT INTO `sys_dict` VALUES (3001, 'sniper_scan_action', '扫描信号动作', 'sniper', NULL, NOW(), NULL, 'entry_ok/watch/avoid（永不输出 sell）', '0', '0');
INSERT INTO `sys_dict` VALUES (3002, 'sniper_position_status', '仓位状态', 'sniper', NULL, NOW(), NULL, 'pending_entry/open/closed', '0', '0');
INSERT INTO `sys_dict` VALUES (3003, 'sniper_close_reason', '平仓原因', 'sniper', NULL, NOW(), NULL, 'stop_loss/horizon/gap_abort/never_filled', '0', '0');
INSERT INTO `sys_dict` VALUES (3004, 'sniper_run_phase', '任务阶段', 'sniper', NULL, NOW(), NULL, 'data_update/scan/track/review', '0', '0');
INSERT INTO `sys_dict` VALUES (3005, 'sniper_run_status', '任务状态', 'sniper', NULL, NOW(), NULL, 'running/done/failed', '0', '0');
INSERT INTO `sys_dict` VALUES (3006, 'sniper_reject_reason', '拒因', 'sniper', NULL, NOW(), NULL, '23 个 RejectReasonEnum.code()（与枚举同源、与 Python 版逐字节一致）', '0', '0');
INSERT INTO `sys_dict` VALUES (3007, 'sniper_llm_workflow', 'LLM工作流', 'sniper', NULL, NOW(), NULL, 'news_sentiment/policy_sentiment/flat_decision', '0', '0');
INSERT INTO `sys_dict` VALUES (3008, 'sniper_llm_status', 'LLM调用状态', 'sniper', NULL, NOW(), NULL, 'ok/parse_error/timeout/http_error', '0', '0');
INSERT INTO `sys_dict` VALUES (3009, 'sniper_sentiment', '新闻情绪', 'sniper', NULL, NOW(), NULL, 'positive/negative/neutral（入库时关键词预打标）', '0', '0');
INSERT INTO `sys_dict` VALUES (3010, 'sniper_adjust', '复权口径', 'sniper', NULL, NOW(), NULL, 'none/qfq（hfq 零消费不建行）', '0', '0');
INSERT INTO `sys_dict` VALUES (3011, 'sniper_universe', '股票池', 'sniper', NULL, NOW(), NULL, 'all/hs300/csi500/hs300_csi500', '0', '0');

-- 字典项（id 30010 起；sniper_reject_reason 的 23 项在 M3 拒因枚举落地后补插，保持与枚举同源）
INSERT INTO `sys_dict_item` VALUES (30010, 3001, 'entry_ok', '可试仓', 'sniper_scan_action', NULL, 0, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30011, 3001, 'watch', '观望', 'sniper_scan_action', NULL, 1, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30012, 3001, 'avoid', '回避', 'sniper_scan_action', NULL, 2, 'sniper', NULL, NOW(), NULL, NULL, '0');

INSERT INTO `sys_dict_item` VALUES (30020, 3002, 'pending_entry', '待入场', 'sniper_position_status', NULL, 0, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30021, 3002, 'open', '持仓中', 'sniper_position_status', NULL, 1, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30022, 3002, 'closed', '已平仓', 'sniper_position_status', NULL, 2, 'sniper', NULL, NOW(), NULL, NULL, '0');

INSERT INTO `sys_dict_item` VALUES (30030, 3003, 'stop_loss', '止损', 'sniper_close_reason', NULL, 0, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30031, 3003, 'horizon', '到期平仓', 'sniper_close_reason', NULL, 1, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30032, 3003, 'gap_abort', '低开放弃', 'sniper_close_reason', NULL, 2, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30033, 3003, 'never_filled', '未成交', 'sniper_close_reason', NULL, 3, 'sniper', NULL, NOW(), NULL, NULL, '0');

INSERT INTO `sys_dict_item` VALUES (30040, 3004, 'data_update', '数据日更', 'sniper_run_phase', NULL, 0, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30041, 3004, 'scan', '盘后扫描', 'sniper_run_phase', NULL, 1, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30042, 3004, 'track', '台账跟踪', 'sniper_run_phase', NULL, 2, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30043, 3004, 'review', '周复盘', 'sniper_run_phase', NULL, 3, 'sniper', NULL, NOW(), NULL, NULL, '0');

INSERT INTO `sys_dict_item` VALUES (30050, 3005, 'running', '运行中', 'sniper_run_status', NULL, 0, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30051, 3005, 'done', '成功', 'sniper_run_status', NULL, 1, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30052, 3005, 'failed', '失败', 'sniper_run_status', NULL, 2, 'sniper', NULL, NOW(), NULL, NULL, '0');

INSERT INTO `sys_dict_item` VALUES (30060, 3007, 'news_sentiment', '新闻情绪', 'sniper_llm_workflow', NULL, 0, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30061, 3007, 'policy_sentiment', '政策舆情', 'sniper_llm_workflow', NULL, 1, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30062, 3007, 'flat_decision', '空仓决策', 'sniper_llm_workflow', NULL, 2, 'sniper', NULL, NOW(), NULL, NULL, '0');

INSERT INTO `sys_dict_item` VALUES (30070, 3008, 'ok', '成功', 'sniper_llm_status', NULL, 0, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30071, 3008, 'parse_error', '输出非法', 'sniper_llm_status', NULL, 1, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30072, 3008, 'timeout', '超时', 'sniper_llm_status', NULL, 2, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30073, 3008, 'http_error', 'HTTP错误', 'sniper_llm_status', NULL, 3, 'sniper', NULL, NOW(), NULL, NULL, '0');

INSERT INTO `sys_dict_item` VALUES (30080, 3009, 'positive', '正面', 'sniper_sentiment', NULL, 0, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30081, 3009, 'negative', '负面', 'sniper_sentiment', NULL, 1, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30082, 3009, 'neutral', '中性', 'sniper_sentiment', NULL, 2, 'sniper', NULL, NOW(), NULL, NULL, '0');

INSERT INTO `sys_dict_item` VALUES (30090, 3010, 'none', '不复权', 'sniper_adjust', NULL, 0, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30091, 3010, 'qfq', '前复权', 'sniper_adjust', NULL, 1, 'sniper', NULL, NOW(), NULL, NULL, '0');

INSERT INTO `sys_dict_item` VALUES (30100, 3011, 'all', '全市场', 'sniper_universe', NULL, 0, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30101, 3011, 'hs300', '沪深300', 'sniper_universe', NULL, 1, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30102, 3011, 'csi500', '中证500', 'sniper_universe', NULL, 2, 'sniper', NULL, NOW(), NULL, NULL, '0');
INSERT INTO `sys_dict_item` VALUES (30103, 3011, 'hs300_csi500', '沪深300+中证500', 'sniper_universe', NULL, 3, 'sniper', NULL, NOW(), NULL, NULL, '0');

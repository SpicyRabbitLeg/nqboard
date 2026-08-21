-- =============================================================
-- 短线候选池系统 菜单注册（主库 nqboard 的 sys_menu 表）
-- 父菜单：量化管理（menu_id=2090421625264017410）
-- 页面：候选池 / 模拟持仓 / 回测中心 / 流水线监控
-- =============================================================

USE `nqboard`;

INSERT INTO `sys_menu` (`menu_id`, `name`, `en_name`, `permission`, `path`, `parent_id`, `icon`, `visible`, `sort_order`, `keep_alive`, `embedded`, `menu_type`, `create_by`, `create_time`, `del_flag`) VALUES (2090602000000000001, '候选股票池', 'stockCandidate', NULL, '/quanta/stockCandidate/index', 2090421625264017410, 'iconfont icon-shaixuan', '1', 4, '0', '0', '0', 'admin', NOW(), '0');
INSERT INTO `sys_menu` (`menu_id`, `name`, `en_name`, `permission`, `path`, `parent_id`, `icon`, `visible`, `sort_order`, `keep_alive`, `embedded`, `menu_type`, `create_by`, `create_time`, `del_flag`) VALUES (2090602000000000002, '模拟持仓', 'stockSimPosition', NULL, '/quanta/stockSimPosition/index', 2090421625264017410, 'iconfont icon-jiaoyi', '1', 5, '0', '0', '0', 'admin', NOW(), '0');
INSERT INTO `sys_menu` (`menu_id`, `name`, `en_name`, `permission`, `path`, `parent_id`, `icon`, `visible`, `sort_order`, `keep_alive`, `embedded`, `menu_type`, `create_by`, `create_time`, `del_flag`) VALUES (2090602000000000003, '回测中心', 'stockBacktestTask', NULL, '/quanta/stockBacktestTask/index', 2090421625264017410, 'iconfont icon-tubiaozhexiantu', '1', 6, '0', '0', '0', 'admin', NOW(), '0');
INSERT INTO `sys_menu` (`menu_id`, `name`, `en_name`, `permission`, `path`, `parent_id`, `icon`, `visible`, `sort_order`, `keep_alive`, `embedded`, `menu_type`, `create_by`, `create_time`, `del_flag`) VALUES (2090602000000000004, '流水线监控', 'quantPipeline', NULL, '/quanta/quantPipeline/index', 2090421625264017410, 'iconfont icon-shuju', '1', 7, '0', '0', '0', 'admin', NOW(), '0');

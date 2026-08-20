DROP DATABASE IF EXISTS `nqboard_quanta`;

CREATE DATABASE  `nqboard_quanta` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

USE nqboard_quanta;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for stock_basic
-- ----------------------------
DROP TABLE IF EXISTS `stock_basic`;
CREATE TABLE `stock_basic` (
   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
   `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
   `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
   `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
   `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
   `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '删除状态（0未删除、1删除）',
   `order_num` int NULL DEFAULT NULL COMMENT '排序字段',
   `remark`    varchar(500) null comment '备注',
   `ts_code` varchar(32) NOT NULL COMMENT '股票代码 000001.SZ',
   `symbol` varchar(16) DEFAULT NULL COMMENT '股票代码',
   `name` varchar(64) DEFAULT NULL COMMENT '股票名称',
   `area` varchar(32) DEFAULT NULL COMMENT '地域',
   `industry` varchar(64) DEFAULT NULL COMMENT '行业',
   `cnspell` varchar(32) DEFAULT NULL COMMENT '拼音缩写',
   `market` varchar(16) DEFAULT NULL COMMENT '市场类型：主板/创业板/科创板',
   `list_date` varchar(16) DEFAULT NULL COMMENT '上市日期',
   `act_name` varchar(128) DEFAULT NULL COMMENT '实控人名称',
   `act_ent_type` varchar(64) DEFAULT NULL COMMENT '实控人企业性质',
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_ts_code` (`ts_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tushare股票基础信息';

-- ----------------------------
-- Table structure for stock_daily
-- ----------------------------
DROP TABLE IF EXISTS `stock_daily`;
CREATE TABLE `stock_daily` (
   `id` bigint NOT NULL COMMENT '业务id（雪花ID）',
   `ts_code` varchar(32) DEFAULT NULL COMMENT '股票代码',
   `trade_date` varchar(16) DEFAULT NULL COMMENT '交易日期',
   `open` float DEFAULT NULL COMMENT '开盘价',
   `high` float DEFAULT NULL COMMENT '最高价',
   `low` float DEFAULT NULL COMMENT '最低价',
   `close` float DEFAULT NULL COMMENT '收盘价',
   `pre_close` float DEFAULT NULL COMMENT '昨收价【除权价】',
   `change` float DEFAULT NULL COMMENT '涨跌额',
   `pct_chg` float DEFAULT NULL COMMENT '涨跌幅（%）【基于除权后的昨收计算的涨跌幅：（今收-除权昨收）/除权昨收】',
   `vol` float DEFAULT NULL COMMENT '成交量 （手）',
   `amount` float DEFAULT NULL COMMENT '成交额 （千元）',
   `ah_vol` float DEFAULT NULL COMMENT '盘后成交量 （手）',
   `ah_amount` float DEFAULT NULL COMMENT '盘后成交额 （千元）',
   `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
   `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
   `create_time` datetime DEFAULT NULL COMMENT '创建时间',
   `update_time` datetime DEFAULT NULL COMMENT '修改时间',
   `del_flag` char(1) DEFAULT '0' COMMENT '删除标记,1:已删除,0:正常',
   `order_num` int DEFAULT NULL COMMENT '排序字段',
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_ts_trade` (`ts_code`, `trade_date`),
   KEY `idx_ts_code` (`ts_code`),
   KEY `idx_trade_date` (`trade_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tushare日线行情';

-- ----------------------------
-- Table structure for stock_cons_weight
-- ----------------------------
DROP TABLE IF EXISTS `stock_cons_weight`;
CREATE TABLE `stock_cons_weight` (
   `id` bigint NOT NULL COMMENT '业务id（雪花ID）',
   `ts_code` varchar(32) NOT NULL COMMENT '股票代码，如600519.SH',
   `index_code` varchar(32) NOT NULL COMMENT '指数代码，如000300',
   `index_name` varchar(128) NOT NULL COMMENT '指数名称，如沪深300',
   `weight` decimal(12,4) DEFAULT NULL COMMENT '指数权重(百分比)',
   `trade_date` date NOT NULL COMMENT '调样生效收盘日期',
   `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
   `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
   `create_time` datetime DEFAULT NULL COMMENT '创建时间',
   `update_time` datetime DEFAULT NULL COMMENT '修改时间',
   `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记,1:已删除,0:正常',
   `order_num` int DEFAULT 0 COMMENT '排序字段',
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_idx_cons_date` (`ts_code`, `index_code`, `trade_date`),
   KEY `idx_ts_code` (`ts_code`),
   KEY `idx_index_code` (`index_code`),
   KEY `idx_trade_date` (`trade_date`),
   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指数成分股及权重';

-- ----------------------------
-- Table structure for stock_mot_holder
-- ----------------------------
DROP TABLE IF EXISTS `stock_mot_holder`;
CREATE TABLE `stock_mot_holder` (
   `id` bigint NOT NULL COMMENT '业务id（雪花ID）',
   `ts_code` varchar(32) NOT NULL COMMENT 'TS代码',
   `ann_date` varchar(16) NOT NULL COMMENT '公告日期',
   `holder_name` varchar(256) NOT NULL COMMENT '股东名称',
   `holder_type` char(1) NOT NULL COMMENT '股东类型 G高管 P个人 C公司',
   `in_des` char(2) NOT NULL COMMENT 'IN增持 DE减持',
   `change_vol` decimal(20,4) DEFAULT NULL COMMENT '变动数量',
   `change_ratio` decimal(12,4) DEFAULT NULL COMMENT '占流通比例(%)',
   `after_share` decimal(20,4) DEFAULT NULL COMMENT '变动后持股',
   `after_ratio` decimal(12,4) DEFAULT NULL COMMENT '变动后占流通比例(%)',
   `avg_price` decimal(12,4) DEFAULT NULL COMMENT '平均价格',
   `total_share` decimal(20,4) DEFAULT NULL COMMENT '持股总数',
   `begin_date` varchar(16) DEFAULT NULL COMMENT '增减持开始日期',
   `close_date` varchar(16) DEFAULT NULL COMMENT '增减持结束日期',
   `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
   `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
   `create_time` datetime DEFAULT NULL COMMENT '入库时间',
   `update_time` datetime DEFAULT NULL COMMENT '修改时间',
   `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记,1:已删除,0:正常',
   `order_num` int DEFAULT 0 COMMENT '排序字段',
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_mot_holder_unique` (`ts_code`, `ann_date`, `holder_name`),
   KEY `idx_ts_code` (`ts_code`),
   KEY `idx_ann_date` (`ann_date`),
   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tushare 股东增减持表 mot_holder';

-- ----------------------------
-- Table structure for stock_mot_holder_count
-- ----------------------------
DROP TABLE IF EXISTS `stock_mot_holder_count`;
CREATE TABLE `stock_mot_holder_count` (
   `id` bigint NOT NULL COMMENT '业务id（雪花ID）',
   `ts_code` varchar(32) NOT NULL COMMENT 'TS股票代码',
   `ann_date` varchar(16) NOT NULL COMMENT '公告日期 YYYYMMDD',
   `end_date` varchar(16) NOT NULL COMMENT '统计截止日期 YYYYMMDD(报告期末)',
   `holder_num` int NOT NULL COMMENT '股东户数',
   `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
   `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
   `create_time` datetime DEFAULT NULL COMMENT '创建时间',
   `update_time` datetime DEFAULT NULL COMMENT '修改时间',
   `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记,1:已删除,0:正常',
   `order_num` int DEFAULT 0 COMMENT '排序字段',
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_mot_holder_cnt` (`ts_code`, `end_date`),
   KEY `idx_ts_code` (`ts_code`),
   KEY `idx_ann_date` (`ann_date`),
   KEY `idx_end_date` (`end_date`),
   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='股东户数表 tushare stk_holdernumber';

-- ----------------------------
-- Table structure for stock_top_list
-- ----------------------------
DROP TABLE IF EXISTS `stock_top_list`;
CREATE TABLE `stock_top_list` (
   `id` bigint NOT NULL COMMENT '业务id（雪花ID）',
   `trade_date` varchar(16) NOT NULL COMMENT '交易日期YYYYMMDD',
   `ts_code` varchar(32) NOT NULL COMMENT 'TS代码',
   `name` varchar(64) NOT NULL COMMENT '股票名称',
   `close` decimal(12,4) DEFAULT NULL COMMENT '收盘价',
   `pct_change` decimal(12,4) DEFAULT NULL COMMENT '涨跌幅%',
   `turnover_rate` decimal(12,4) DEFAULT NULL COMMENT '换手率%',
   `amount` decimal(24,4) DEFAULT NULL COMMENT '总成交额(元)',
   `l_sell` decimal(24,4) DEFAULT NULL COMMENT '龙虎榜卖出额',
   `l_buy` decimal(24,4) DEFAULT NULL COMMENT '龙虎榜买入额',
   `l_amount` decimal(24,4) DEFAULT NULL COMMENT '龙虎榜成交额',
   `net_amount` decimal(24,4) DEFAULT NULL COMMENT '龙虎榜净买入额',
   `net_rate` decimal(12,4) DEFAULT NULL COMMENT '龙虎榜净买额占比',
   `amount_rate` decimal(12,4) DEFAULT NULL COMMENT '龙虎榜成交额占比',
   `float_values` decimal(24,4) DEFAULT NULL COMMENT '当日流通市值',
   `reason` varchar(512) DEFAULT NULL COMMENT '上榜理由',
   `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
   `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
   `create_time` datetime DEFAULT NULL COMMENT '入库时间',
   `update_time` datetime DEFAULT NULL COMMENT '修改时间',
   `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记,1:已删除,0:正常',
   `order_num` int DEFAULT 0 COMMENT '排序字段',
   PRIMARY KEY (`id`),
   UNIQUE KEY `idx_trade_ts` (`trade_date`, `ts_code`),
   KEY `idx_ts_code` (`ts_code`),
   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tushare top_list龙虎榜每日明细';

-- ----------------------------
-- Table structure for stock_mot_ann_news
-- ----------------------------
DROP TABLE IF EXISTS `stock_mot_ann_news`;
CREATE TABLE `stock_mot_ann_news` (
   `id` bigint NOT NULL COMMENT '业务id（雪花ID）',
   `ts_code` varchar(32) NOT NULL COMMENT 'TS股票代码',
   `pub_date` varchar(16) NOT NULL COMMENT '发布日期 YYYYMMDD',
   `pub_datetime` varchar(32) DEFAULT NULL COMMENT '精确发布时间',
   `news_type` varchar(10) NOT NULL COMMENT 'ann交易所公告 media媒体新闻',
   `src` varchar(64) DEFAULT NULL COMMENT '来源：巨潮资讯/东方财富等',
   `title` text NOT NULL COMMENT '新闻/公告标题【核心字段】',
   `summary` text COMMENT '简短摘要，不要存全文',
   `url` varchar(512) DEFAULT NULL COMMENT '原文链接',
   `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
   `update_by` varchar(64) DEFAULT NULL COMMENT '修改人',
   `create_time` datetime DEFAULT NULL COMMENT '创建时间',
   `update_time` datetime DEFAULT NULL COMMENT '修改时间',
   `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记,1:已删除,0:正常',
   `order_num` int DEFAULT 0 COMMENT '排序字段',
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_mot_ann_news` (`ts_code`, `pub_date`, `news_type`, `url`),
   KEY `idx_ts_code` (`ts_code`),
   KEY `idx_pub_date` (`pub_date`),
   KEY `idx_news_type` (`news_type`),
   KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告&媒体新闻表';

-- ----------------------------
-- Records of lock_table
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;

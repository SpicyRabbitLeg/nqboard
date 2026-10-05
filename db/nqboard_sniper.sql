-- ----------------------------
-- nqboard_sniper 建库脚本（短线股票分析模块）
-- 规格来源：nqboard-sniper-biz/src/main/resources/doc/nqboard-migration-plan.md §3（oracle）
-- 房规对齐 db/nqboard_device.sql：反引号 / InnoDB / utf8mb4_general_ci / ROW_FORMAT=Dynamic / DROP+CREATE 头
-- 全部 21 张表统一 BaseEntity 六件套：id 雪花主键(ASSIGN_ID) + 审计五列；原复合自然键转 UNIQUE KEY 保 upsert 幂等
-- 注意：脚本为 seed 语义（同 device.sql），重复执行会 DROP DATABASE 清空重建，生产数据期禁跑
-- ----------------------------

DROP DATABASE IF EXISTS `nqboard_sniper`;

CREATE DATABASE `nqboard_sniper` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE nqboard_sniper;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sniper_trade_calendar
-- 交易日历（磁盘缓存等价物；预灌 1990~至今+400天，§4.3.1）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_trade_calendar`;
CREATE TABLE `sniper_trade_calendar`  (
  `id`          bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`   varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`   varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`    char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `cal_date`   date NOT NULL COMMENT '日历日期',
  `is_open`    tinyint NOT NULL COMMENT '1=交易日',
  `fetched_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拉取时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cal` (`cal_date`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='交易日历' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_stock_basic
-- 股票基础信息（Tushare stock_basic 批量 + 东财 f127 行业修正，§4.3.2）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_stock_basic`;
CREATE TABLE `sniper_stock_basic`  (
  `id`          bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`   varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`   varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`    char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `code`            char(6) NOT NULL COMMENT '6位代码',
  `exchange`        char(2) NOT NULL COMMENT 'SH/SZ/BJ',
  `ts_code`         varchar(12) NOT NULL COMMENT '600519.SH',
  `name`            varchar(40) NOT NULL COMMENT '证券简称',
  `list_date`       date NULL COMMENT '上市日期(次新过滤)',
  `industry`        varchar(30) NULL COMMENT '行业(东财f127口径优先,与板块名对齐)',
  `industry_source` varchar(10) NULL COMMENT 'em/tushare',
  `fetched_at`      datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拉取时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_basic` (`code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='股票基础信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_index_constituents
-- 指数成分（index_weight 每次取最新快照日，§4.3.3）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_index_constituents`;
CREATE TABLE `sniper_index_constituents`  (
  `id`          bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`   varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`   varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`    char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `index_code`    varchar(9) NOT NULL COMMENT '000300.SH/000905.SH',
  `snapshot_date` date NOT NULL COMMENT '成分快照日',
  `code`          char(6) NOT NULL COMMENT '成分股代码',
  `fetched_at`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拉取时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cons` (`index_code`, `snapshot_date`, `code`) USING BTREE,
  KEY `idx_cons_code` (`code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='指数成分快照' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_daily_price
-- 日行情（核心表，双复权口径；UK(code,trade_date,adjust) 即防重，§4.3.4）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_daily_price`;
CREATE TABLE `sniper_daily_price`  (
  `id`          bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`   varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`   varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`    char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `code`       char(6) NOT NULL COMMENT '6位代码',
  `trade_date` date NOT NULL COMMENT '交易日',
  `adjust`     enum('none','qfq') NOT NULL COMMENT '复权口径',
  `open`       decimal(16,4) NOT NULL COMMENT '开盘价',
  `high`       decimal(16,4) NOT NULL COMMENT '最高价',
  `low`        decimal(16,4) NOT NULL COMMENT '最低价',
  `close`      decimal(16,4) NOT NULL COMMENT '收盘价',
  `pre_close`  decimal(16,4) NULL COMMENT '除权调整后昨收(仅none行,来自tushare daily)',
  `volume`     decimal(18,2) NOT NULL COMMENT '成交量(统一为手;新浪股在入库前÷100,原样存)',
  `amount`     decimal(20,4) NULL COMMENT '成交额(tushare=千元,东财/腾讯=元,原样存)',
  `source`     varchar(20) NOT NULL COMMENT 'tushare/em/tencent/sina',
  `fetched_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拉取时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_price` (`code`, `trade_date`, `adjust`) USING BTREE,
  KEY `idx_price_date` (`trade_date`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='日行情' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_index_daily
-- 指数日行情（市场门 5 日涨幅、台账 000300/000905/399006 基准消费；独立于 sniper_daily_price——
-- 000905.SH 指数与 000905.SZ 个股 6 位代码冲突，同表 UK(code,trade_date,adjust) 会互相覆盖，2026-10-04 勘误）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_index_daily`;
CREATE TABLE `sniper_index_daily`  (
  `id`          bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`   varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`   varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`    char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `index_code` char(6) NOT NULL COMMENT '指数代码前6位(000300/000905/399006)',
  `trade_date` date NOT NULL COMMENT '交易日',
  `open`       decimal(16,4) NULL COMMENT '开盘价',
  `high`       decimal(16,4) NULL COMMENT '最高价',
  `low`        decimal(16,4) NULL COMMENT '最低价',
  `close`      decimal(16,4) NOT NULL COMMENT '收盘价',
  `volume`     decimal(18,2) NULL COMMENT '成交量(手,tushare index_daily原样)',
  `amount`     decimal(20,4) NULL COMMENT '成交额(千元,tushare index_daily原样)',
  `source`     varchar(20) NOT NULL DEFAULT 'tushare' COMMENT '来源',
  `fetched_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拉取时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_index_daily` (`index_code`, `trade_date`) USING BTREE,
  KEY `idx_index_daily_date` (`trade_date`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='指数日行情' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_adj_factor
-- 复权因子（qfq 重算 + 除权检测；adj_factor 支持 trade_date 全市场一次拉，§4.3.4）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_adj_factor`;
CREATE TABLE `sniper_adj_factor`  (
  `id`          bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`   varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`   varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`    char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `code`       char(6) NOT NULL,
  `trade_date` date NOT NULL,
  `factor`     decimal(20,10) NOT NULL COMMENT '复权因子',
  `fetched_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拉取时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_factor` (`code`, `trade_date`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='复权因子' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_market_snapshot
-- 全市场快照（Stage0 输入；每日收盘后一行/票，§4.3.5）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_market_snapshot`;
CREATE TABLE `sniper_market_snapshot`  (
  `id`          bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`   varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`   varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`    char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `trade_date`    date NOT NULL COMMENT '交易日',
  `code`          char(6) NOT NULL,
  `name`          varchar(40) NOT NULL,
  `price`         decimal(16,4) NULL COMMENT '最新价',
  `change_pct`    decimal(10,4) NULL COMMENT '涨跌幅%',
  `open`          decimal(16,4) NULL,
  `high`          decimal(16,4) NULL,
  `low`           decimal(16,4) NULL,
  `prev_close`    decimal(16,4) NULL COMMENT '昨收',
  `volume`        decimal(20,2) NULL COMMENT '成交量,手',
  `amount`        decimal(20,4) NULL COMMENT '成交额,元',
  `turnover_rate` decimal(10,4) NULL COMMENT '换手率%',
  `source`        varchar(20) NOT NULL DEFAULT 'em' COMMENT 'em/sina/tencent',
  `fetched_at`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拉取时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_snapshot` (`trade_date`, `code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='全市场快照' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_industry_board_daily
-- 行业板块日K（sector_rotation 输入，§4.3.6）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_industry_board_daily`;
CREATE TABLE `sniper_industry_board_daily`  (
  `id`          bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`   varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`   varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`    char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `board_name` varchar(30) NOT NULL COMMENT '板块名(东财口径)',
  `trade_date` date NOT NULL,
  `open`       decimal(16,4) NULL,
  `high`       decimal(16,4) NULL,
  `low`        decimal(16,4) NULL,
  `close`      decimal(16,4) NOT NULL,
  `volume`     decimal(20,2) NULL COMMENT '手',
  `amount`     decimal(20,4) NULL COMMENT '元',
  `source`     varchar(20) NOT NULL DEFAULT 'em' COMMENT 'em/ths/synthetic',
  `fetched_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拉取时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_board` (`board_name`, `trade_date`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='行业板块日K' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_financial_indicator
-- 财务指标（同花顺 indicator + tushare daily_basic 估值合并，两腿各存一行，§4.3.7）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_financial_indicator`;
CREATE TABLE `sniper_financial_indicator`  (
  `id`          bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`   varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`   varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`    char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `code`           char(6) NOT NULL,
  `report_period`  date NOT NULL COMMENT '报告期(腿1)或估值交易日(腿2)',
  `gross_margin`   decimal(10,4) NULL,
  `net_margin`     decimal(10,4) NULL,
  `roe`            decimal(10,4) NULL,
  `revenue_growth` decimal(10,4) NULL,
  `profit_growth`  decimal(10,4) NULL,
  `pe_ttm`         decimal(16,4) NULL,
  `pb`             decimal(16,4) NULL,
  `ps_ttm`         decimal(16,4) NULL,
  `market_cap`     decimal(20,4) NULL COMMENT '元(daily_basic total_mv 万元×1e4)',
  `source`         varchar(20) NOT NULL COMMENT 'ths/tushare',
  `fetched_at`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拉取时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_fin` (`code`, `report_period`, `source`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='财务指标(双腿)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_company_news
-- 个股新闻（sentiment/policy 输入，§4.3.8）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_company_news`;
CREATE TABLE `sniper_company_news`  (
  `id`          bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`   varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`   varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`    char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `code`               char(6) NOT NULL,
  `title`              varchar(200) NOT NULL,
  `source_name`        varchar(50) NULL COMMENT '文章来源',
  `url`                varchar(500) NULL,
  `body_digest`        varchar(2000) NULL COMMENT '正文前2000字',
  `published_at`       datetime NOT NULL COMMENT '发布时间',
  `sentiment`          enum('positive','negative','neutral') NULL COMMENT '关键词规则预打标(入库时)',
  `announcement_type`  varchar(20) NULL COMMENT '业绩预告/回购/减持/监管/政策',
  `fetch_date`         date NOT NULL COMMENT '拉取日(增量游标)',
  `fetched_at`         datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拉取时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_news` (`code`, `published_at`, `title`(100)),
  KEY `idx_news_code_date` (`code`, `published_at`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='个股新闻' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_insider_trade
-- 高管/股东增减持（sentiment insider 腿；tushare stk_holdertrade 优先，§4.3.9）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_insider_trade`;
CREATE TABLE `sniper_insider_trade`  (
  `id`          bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`   varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`   varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`    char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `code`         char(6) NOT NULL,
  `ann_date`     date NOT NULL COMMENT '公告日期(demat_date兜底)',
  `holder_name`  varchar(100) NULL,
  `holder_type`  varchar(20) NULL,
  `change_vol`   decimal(20,2) NOT NULL COMMENT '股; DE减持为负(与Python口径一致)',
  `avg_price`    decimal(16,4) NULL,
  `after_shares` decimal(20,2) NULL COMMENT '变动后持股(股)',
  `source`       varchar(20) NOT NULL COMMENT 'tushare/ths',
  `fetched_at`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拉取时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_insider` (`code`, `ann_date`, `holder_name`(50), `change_vol`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='股东增减持' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_fund_flow_daily
-- 主力资金流（main_force_flow 输入；tushare moneyflow 优先，万元→元后入库，§4.3.10）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_fund_flow_daily`;
CREATE TABLE `sniper_fund_flow_daily`  (
  `id`          bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`   varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`   varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`    char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `code`       char(6) NOT NULL,
  `trade_date` date NOT NULL,
  `main_net`   decimal(20,4) NULL COMMENT '主力净流入,元',
  `super_net`  decimal(20,4) NULL COMMENT '超大单净流入,元',
  `large_net`  decimal(20,4) NULL,
  `medium_net` decimal(20,4) NULL,
  `small_net`  decimal(20,4) NULL,
  `source`     varchar(20) NOT NULL COMMENT 'tushare/em',
  `fetched_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拉取时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_flow` (`code`, `trade_date`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='主力资金流' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_dragon_tiger
-- 龙虎榜（dragon_tiger 输入；tushare top_list 按日全市场 / em 区间兜底，§4.3.11）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_dragon_tiger`;
CREATE TABLE `sniper_dragon_tiger`  (
  `id`          bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`   varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`   varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`    char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `code`       char(6) NOT NULL,
  `trade_date` date NOT NULL,
  `reason`     varchar(100) NOT NULL COMMENT '上榜原因',
  `net_buy`    decimal(20,4) NULL COMMENT '净买额,元',
  `buy_amt`    decimal(20,4) NULL COMMENT '买入额,元',
  `sell_amt`   decimal(20,4) NULL COMMENT '卖出额,元',
  `change_pct` decimal(10,4) NULL COMMENT '当日涨跌幅%',
  `source`     varchar(20) NOT NULL DEFAULT 'tushare' COMMENT 'tushare/em',
  `fetched_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拉取时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_dragon` (`code`, `trade_date`, `reason`(50)) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='龙虎榜' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_restricted_release
-- 限售解禁（gate 解禁否决输入；tushare share_float 优先，§4.3.12）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_restricted_release`;
CREATE TABLE `sniper_restricted_release`  (
  `id`          bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`   varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`   varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`    char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `code`          char(6) NOT NULL,
  `plan_date`     date NOT NULL COMMENT '解禁时间',
  `shares`        decimal(20,2) NULL COMMENT '解禁数量,股',
  `market_value`  decimal(20,4) NULL COMMENT '实际解禁市值,元(em口径)',
  `float_ratio`   decimal(10,4) NULL COMMENT '占总股本比例,小数(tushare口径)',
  `float_mv_ratio` decimal(10,4) NULL COMMENT '占解禁前流通市值比例,小数(em真口径;tushare模式下=占总股本近似,gate消费)',
  `source`        varchar(20) NOT NULL COMMENT 'tushare/em',
  `fetched_at`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拉取时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_release` (`code`, `plan_date`, `shares`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='限售解禁' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_scan_report
-- 扫描报告头（每次运行一行；UK(as_of,universe) 保证幂等重跑覆盖，§3.3）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_scan_report`;
CREATE TABLE `sniper_scan_report`  (
  `id`                  bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`  varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`  varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`   char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `as_of`               date NOT NULL COMMENT '数据日期',
  `universe`            varchar(20) NOT NULL,
  `mode`                varchar(20) NOT NULL DEFAULT 'flat_short',
  `gate_version`        varchar(4) NOT NULL DEFAULT '3',
  `market_ret_5d`       decimal(10,6) NOT NULL COMMENT '沪深300五日涨幅',
  `market_gate_blocked` char(1) NOT NULL DEFAULT '0' COMMENT '0否1是',
  `prefetch_ok`         char(1) NOT NULL DEFAULT '1' COMMENT '0否1是',
  `stage0_count`        int NULL,
  `stage1_count`        int NULL,
  `stage2_count`        int NULL,
  `signal_count`        int NOT NULL DEFAULT 0,
  `meta`                json NULL COMMENT '预算/漏斗统计/prefetch_warnings',
  `report_json`         json NULL COMMENT '完整 to_dict() 快照(golden master 对拍用)',
  `report_md`           mediumtext NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_report` (`as_of`, `universe`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='扫描报告头' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_scan_signal
-- 信号明细（列对齐 BuySignal，src/scan/models.py；重跑时按 report_id 先删后插）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_scan_signal`;
CREATE TABLE `sniper_scan_signal`  (
  `id`               bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`  varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`  varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`   char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `report_id`        bigint NOT NULL COMMENT '报告id',
  `ticker`           varchar(9) NOT NULL COMMENT '600519.SH',
  `action`           enum('entry_ok','watch','avoid') NOT NULL,
  `confidence`       decimal(5,2) NOT NULL,
  `weighted_score`   decimal(10,6) NOT NULL,
  `quantity`         int NOT NULL DEFAULT 0,
  `trial_cash_used`  decimal(16,2) NOT NULL DEFAULT 0 COMMENT '占用资金,元',
  `reasoning`        varchar(200) NULL COMMENT '决策理由,截断200字(与BuySignal一致)',
  `screen_score`     decimal(10,4) NULL COMMENT 'Stage1快筛分',
  `reasons`          json NULL COMMENT '偏多因子标签数组',
  `risk_flags`       json NULL,
  `analyst_summary`  json NULL,
  `prefetch_warnings` json NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sig_report` (`report_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='扫描信号明细' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_scan_rejected
-- 被拒明细（复盘对账核心；重跑时按 report_id 先删后插）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_scan_rejected`;
CREATE TABLE `sniper_scan_rejected`  (
  `id`         bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`  varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`  varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`   char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `report_id`  bigint NOT NULL COMMENT '报告id',
  `ticker`     varchar(9) NOT NULL,
  `reasons`    json NOT NULL COMMENT '拒因 code() 字符串数组',
  `decision`   json NULL,
  `warnings`   json NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_rej_report` (`report_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='扫描被拒明细' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_ledger_position
-- 模拟台账仓位（tracking.py TrackedPosition 等价物；UK 幂等，§9）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_ledger_position`;
CREATE TABLE `sniper_ledger_position`  (
  `id`               bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`  varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`  varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`   char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `cohort_date`      date NOT NULL COMMENT '信号日, cohort_id=scan_<date>',
  `source_report`    varchar(200) NULL COMMENT '来源报告标识(ingested_reports 幂等审计)',
  `ticker`           varchar(9) NOT NULL,
  `status`           enum('pending_entry','open','closed') NOT NULL COMMENT 'gap_abort/never_filled是closed+close_reason,不占status',
  `entry_date`       date NULL,
  `entry_price`      decimal(16,4) NULL,
  `entry_cost`       decimal(16,2) NULL COMMENT '含费用,元',
  `signal_day_close` decimal(16,4) NULL COMMENT '信号日收盘(gap判定基准)',
  `quantity`         int NULL,
  `trial_cash_used`  decimal(16,2) NULL,
  `stop_price`       decimal(16,4) NULL COMMENT '=entry_price×(1-stop_pct)',
  `stop_from_date`   date NULL COMMENT '止损启用日(防对已存活仓位回溯止损)',
  `close_date`       date NULL,
  `close_price`      decimal(16,4) NULL,
  `close_reason`     enum('stop_loss','horizon','gap_abort','never_filled') NULL,
  `exit_proceeds`    decimal(16,2) NULL COMMENT '卖出净额(扣费)',
  `pnl`              decimal(16,2) NULL COMMENT '绝对盈亏,元',
  `pnl_pct`          decimal(10,6) NULL,
  `bench_ret_full`   decimal(10,6) NULL COMMENT '入场~平仓区间沪深300收益',
  `excess_ret`       decimal(10,6) NULL,
  `max_gain`         decimal(10,6) NULL,
  `max_drawdown`     decimal(10,6) NULL,
  `confidence`       decimal(5,2) NULL,
  `weighted_score`   decimal(10,6) NULL,
  `screen_score`     decimal(10,4) NULL,
  `reasons`          json NULL,
  `risk_flags`       json NULL,
  `analyst_summary`  json NULL,
  `market_ret_5d`    decimal(10,6) NULL COMMENT '信号日市场环境原始值;market_up/down由复盘按>=0派生',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_pos` (`cohort_date`, `ticker`),
  KEY `idx_pos_status` (`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='模拟台账仓位' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_ledger_mark
-- 台账每日 mark（update_marks 每日一行；全量重建幂等，§9）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_ledger_mark`;
CREATE TABLE `sniper_ledger_mark`  (
  `id`             bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`  varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`  varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`   char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `position_id`    bigint NOT NULL COMMENT '仓位id',
  `trade_date`     date NOT NULL,
  `close`          decimal(16,4) NOT NULL,
  `ret_from_entry` decimal(10,6) NULL,
  `ret_from_t0`    decimal(10,6) NULL COMMENT '相对信号日收盘',
  `bench_close`    decimal(16,4) NULL COMMENT '沪深300收盘',
  `bench_ret`      decimal(10,6) NULL,
  `excess_ret`     decimal(10,6) NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_mark` (`position_id`, `trade_date`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='台账每日mark' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_daily_run
-- 调度幂等与预算记账（Quartz Job 幂等键；实体继承 BaseEntity，§3.4）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_daily_run`;
CREATE TABLE `sniper_daily_run`  (
  `id`          bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`  varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`  varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`   char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `run_date`    date NOT NULL,
  `phase`       varchar(20) NOT NULL COMMENT 'scan/track/review/data_update',
  `status`      enum('running','done','failed') NOT NULL,
  `budget_used` int NOT NULL DEFAULT 0 COMMENT '东财主源日预算消耗',
  `detail`      json NULL COMMENT '缺数标记/统计',
  `started_at`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `finished_at` datetime NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_run` (`run_date`, `phase`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='每日运行记账' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_llm_call_log
-- LLM 调用审计（P-07 缺口：token/成本可离线评估；成功失败都落行，§3.4）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_llm_call_log`;
CREATE TABLE `sniper_llm_call_log`  (
  `id`           bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`  varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`  varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`   char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `called_at`    datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `workflow`     varchar(40) NOT NULL COMMENT 'news_sentiment/policy_sentiment/flat_decision',
  `tickers`      json NULL,
  `input_digest` varchar(64) NULL,
  `output_json`  json NULL,
  `status`       enum('ok','parse_error','timeout','http_error') NOT NULL,
  `latency_ms`   int NULL,
  `error`        varchar(500) NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_llm_time` (`called_at`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='LLM调用审计' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sniper_request_log
-- 外部请求审计（Python CN_REQUEST_LOG 等价物，对拍 fixtures 录制源；append-only，§3.4）
-- ----------------------------
DROP TABLE IF EXISTS `sniper_request_log`;
CREATE TABLE `sniper_request_log`  (
  `id`          bigint NOT NULL COMMENT '雪花id(ASSIGN_ID)',
  `create_by`   varchar(255) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`   varchar(255) NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag`    char(1) NULL DEFAULT '0' COMMENT '删除状态（0未删除、1删除）',
  `called_at`  datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `endpoint`   varchar(60) NOT NULL,
  `source`     varchar(20) NOT NULL,
  `params`     json NULL,
  `status`     varchar(10) NOT NULL,
  `n_rows`     int NULL,
  `elapsed_ms` int NULL,
  `error`      varchar(500) NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_req_time` (`called_at`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT='外部请求审计' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

# nqboard 迁移完整方案（Java + Spring Boot + MySQL + Dify）

> 基线：[`重构.md`](重构.md) 第 2~7 章（现状规格）+ 本仓库当前代码（2026-10-01 逐文件核对，commit 9ed0a90）。
> 核对范围：`src/scan/*`、`src/agents/*`（短线 7 分析师 + risk + PM）、`src/market/cn/*`、`src/prediction/{features,calibration,rule_ensemble}`、`src/tools/api.py`、`scripts/scan_buy_signals.py`、`scripts/track_signals.py`、`.env`、`每日操作指南.md`。差异记录见附录 A。
> 目标：将 A 股短线信号系统（盘后扫描 + 信号跟踪）完整迁移到 nqboard 项目，技术栈 **Java 17 + Spring Boot 3.5（Spring Cloud Alibaba 微服务）+ MySQL 8 + Dify**，落位 **`nqboard-sniper` 模块**（api/biz 双子模块，端口 6008，见 §1.1 与 [`重构.md`](重构.md) §9.2）。
> 原则：**行为不变优先**——阈值、公式、拒因字符串、fail-closed 语义一律照搬，不做"顺手优化"；确需变更的（见 §2 决策）单独列出。
> 明确不迁移：14 个名人/全画像 agent（fundamentals、valuation、growth 及 13 位投资人 agent，full profile 遗留）、legacy 交互 CLI、`app/` FastAPI Web UI、北向资金**分析师**（北向数据预检告警保留）、实验性回测/ML 管线（`src/backtesting/`、`src/backtester.py`，与扫描管线无 import 依赖；注意 `src/prediction/` 的 `features/technical.py`、`features/short_structure.py`、`rule_ensemble.py`、`calibration.py` 被 quick_screen/trend_predictor/gate 消费，**在迁移范围内**）、`--intraday` 盘中模式（Java 版仅支持盘后 T 日扫描；如需盘中另立项目）。
>
> **文档集导航（本目录自包含，开发只读本套文档即可）**：
> - `nqboard-migration-plan.md`（本档）——迁移规格基线（oracle）：DDL / 取数 / 漏斗 / Agent / Gate / 调度 / 配置 / 对拍全部以此为准。
> - [`重构.md`](重构.md)——Python 版现状规格：本档所有公式、阈值、拒因字符串的**第一出处**（§2~7 现状规格、§9.2 目标架构、§10 附录）；其中 `src/*` 等代码锚点指向 ai-hedge-fund 仓库，仅对拍溯源时查阅。
> - [`每日操作指南.md`](每日操作指南.md)——运营纪律与红线出处（§六：连续两周胜率 <40% 暂停实盘）。
> - `nqboard-sniper/`——按菜单组拆分的模块化实施文档（索引见其 `README.md`；nqboard 仓库副本目录名为 `短线算法构建文档/`，内容相同）。

---

## 目录

1. [总体架构（含 §1.4 前端菜单规划）](#1-总体架构)
2. [五个顶层决策（先读）](#2-五个顶层决策先读)
3. [MySQL 全量建表 DDL](#3-mysql-全量建表-ddl)
4. [数据接入层设计](#4-数据接入层设计)
5. [漏斗 Stage 0~3 实现规格](#5-漏斗-stage-03-实现规格)
6. [Dify 配置与调用契约](#6-dify-配置与调用契约)
7. [Agent 层实现规格](#7-agent-层实现规格)
8. [Gate（输出门禁）实现规格](#8-gate输出门禁实现规格)
9. [台账与复盘子系统](#9-台账与复盘子系统)
10. [调度与运行时](#10-调度与运行时)
11. [配置映射（.env → Nacos yml / SniperProperties）](#11-配置映射env--nacos-yml--sniperproperties)
12. [对拍与验收](#12-对拍与验收)
13. [分期实施计划](#13-分期实施计划)
14. [附录 A：与当前代码核对的差异记录](#附录-a与当前代码核对的差异记录)

---

## 1. 总体架构

### 1.1 模块划分（落位 nqboard-sniper）

nqboard 是 pig4cloud 系 Spring Cloud 微服务工程（`com.mx:nqboard`，仓库约定见其 `AGENTS.md`）。业务落位 **`nqboard-sniper`** 模块（骨架已就位：启动类 `SniperApplication`、端口 6008、Nacos 配置导入），按平台 api/biz 双层约定组织：

```
nqboard-sniper/
├── nqboard-sniper-api/            # 对外契约（entity/dto/vo/enums/constant/feign）
│   └── com.mx.nqboard.sniper.api
│       ├── enums/                 #   RejectReasonEnum(23拒因)、AdjustEnum、PositionStatusEnum...
│       ├── entity/                #   全部实体（继承 BaseEntity）
│       ├── dto/vo/                #   ScanRunDTO / ScanReportVO ...
│       └── feign/                 #   RemoteSniperService（登记 FeignClient.imports）
└── nqboard-sniper-biz/            # 业务实现（com.mx.nqboard.sniper）
    ├── controller/                #   ScanController / LedgerController / ReviewController
    ├── service/ + impl/           #   平台标准 CRUD 分层
    ├── mapper/                    #   BaseMapper（对拍/聚合允许 XML）
    ├── algorithm/                 #   ★纯算法域（零 Spring 依赖，可单测）
    │   ├── indicator/             #     唯一指标库：EMA/MACD/RSI/ADX/BOLL/量比/涨停判定
    │   ├── funnel/                #     UniverseFilter / QuickScreen / TechnicalFeatureCalculator
    │   ├── agent/                 #     7 分析师 + RiskManager + PortfolioManager（规则部分）
    │   └── gate/                  #     OutputGate 14 条规则 + OverheatVeto
    ├── data/                      # 数据接入：DataGateway（唯一 IO 边界）+ provider/（Tushare/东财/
    │                              #   腾讯/新浪/Composite 路由）+ ratelimit/（Sentinel+日预算）
    ├── llm/                       # DifyClient + 三个工作流入出参 record
    ├── pipeline/                  # ScanPipeline / LedgerService / ReviewService
    ├── config/                    # SniperProperties（@ConfigurationProperties，阈值收编）
    └── consumer/                  # （预留）RocketMQ 消费者
```

> Python 组件 → sniper 类的逐项映射表见 [`重构.md`](重构.md) §9.2。跨服务契约只放 `-api`；`-biz → -api` 单向依赖；日常 CRUD 一律 MyBatis-Plus LambdaQueryWrapper，返回体一律 `R<T>`。
> 包结构说明：`algorithm/ data/ llm/ pipeline/` 是相对平台模板包（controller/service/impl/mapper/consumer/config/utils）**有意的扩展**——algorithm 是零 Spring 依赖的纯算法域（bit-exact 对拍的前提）、data 是唯一 IO 边界、pipeline 是编排门面；业务表 CRUD 仍走平台标准 `XxxService extends IService<XxxEntity>` + `ServiceImpl` + `Mapper extends BaseMapper` 分层，扩展包内不得夹带 CRUD。

### 1.2 技术选型

| 关注点 | 选型 | 理由 |
|---|---|---|
| 运行时 | Java 17, Spring Boot 3.5.11（平台基线） | **无虚拟线程**——6 分析师并行用 CompletableFuture + 专用线程池 |
| 微服务 | Nacos 注册+配置中心、Spring Cloud Gateway（/sniper/** 路由） | 平台现成；配置/密钥走 Nacos + jasypt |
| ORM | MyBatis-Plus 3.5.16（common-mybatis） | 平台标准；BaseEntity 审计填充、LambdaQueryWrapper 禁手拼 SQL |
| 连接池 | HikariCP（默认，数据源配在 Nacos） | — |
| 缓存 | RedisUtils（common-core）+ 进程内 Caffeine 可选 | 行情热点/日历/行业映射；key 遵循 CacheConstants |
| 限流/熔断 | Sentinel（common-feign 自带）+ 自研日预算计数器 | 对齐 Python 版节流/熔断参数 |
| 重试 | 自实现指数退避（照抄 Python `_call`/`run_throttled_retry` 语义） | 限频错误退避 2n s、其他 1n s + rand |
| 调度 | **nqboard-visual-quartz 定时任务管理台** | 平台现成；4 个 Job（data_update/scan/track/review），幂等键写 sniper_daily_run |
| 序列化 | Jackson（JSON 列读写，common-mybatis TypeHandler） | — |
| LLM | Dify Workflow HTTP API | 见 §6 |
| 构建 | Maven（nqboard 仓库现有多模块，业务落 nqboard-sniper） | — |

### 1.3 端到端数据流

```
[定时 15:35 交易日] ScanPipeline.run()          # Python 现状：15:30 后手动跑，调度取 15:35
   │
   ├─ ① MarketGate: 沪深300 近5日涨幅 < 0 → 空报告落库，结束（零 LLM/零漏斗请求；与
   │     Python scripts/scan_buy_signals.py 一致：市场门在 Stage0 之前短路）
   ├─ ② Stage0 UniverseFilter: 快照 + 成分 + 上市日期 → 9 条规则 → 候选池(~800)
   ├─ ③ Stage1 QuickScreen: 逐票 60 日 qfq 行情(读库,缺则拉) → 技术特征 → 过热否决
   │      → 快筛打分 ≥35 → 排序取前 max_screen（代码默认 30，日常命令行传 50）
   ├─ ④ Stage2: 前 max_deep=8 只 → 数据预检(prefetch) → 6 分析师并行(CompletableFuture+线程池,Java17无虚拟线程)
   │      → trend_predictor 汇合 → risk_manager → portfolio_manager(Dify flat-decision)
   ├─ ⑤ Stage3 OutputGate: 解禁否决 + 14 条规则 → 排序 → 行业cap(1) → 每日cap(1)
   └─ ⑥ sniper_scan_report / sniper_scan_signal / sniper_scan_rejected 落库
        + 渲染 Markdown（保留 reports/scan_<date>.json/.md 双产物语义，供对拍与人工阅读）
[定时 扫描后] LedgerService.ingestAndTrack() → 台账入账/模拟成交/止损/平仓
```

> Python 现状没有独立的"数据更新任务"——行情在扫描过程中惰性拉取并用当日 JSON 磁盘缓存去重（`CN_DAILY_CACHE_ENABLED`，`data/cache/*.json`）。Java 版改为"先日更、后扫描"两段式（§4.4），属于**调度形态变化、数据语义不变**，对拍时以"同一天结束后库内数据一致"为准。

---

### 1.4 前端菜单规划（sys_menu）（2026-10-02）

> **模块化实施文档**：按本节 5 个菜单组拆分的实施规划（数据库设计/取数/字段映射/入库/页面定义）见 `nqboard-sniper/` 目录（nqboard 仓库副本目录名为 `短线算法构建文档/`，内容相同），索引与建设顺序见其 `README.md`；本节为菜单总规划，两者冲突时以本文档为准。
> 依据本方案功能面梳理，模块对外可见能力共五块：**扫描管线（报告/信号/被拒）、模拟台账、复盘分析、行情数据资产（§3.1/3.2 的 13 张表）、运维审计（§3.4 三张表）**。调度挂 nqboard-visual-quartz 平台管理台、阈值/密钥走 Nacos（§11），均不建业务菜单——菜单全部围绕"查结果 + 手动触发"设计。
> 平台 `sys_menu` 三级结构：menu_type `0`=目录、`1`=菜单、`2`=按钮（按钮挂 permission 权限标识，命名 `sniper_<域>_<动作>`，对齐平台 `sys_user_add` 风格）。现有库固定号段占用至 9065（1000/2000/3000/4000/9000 段及各自子段），**6000 段空闲**，本模块取 6000~6599；一级目录 icon 建议 `ele-Aim`，其余随平台图标库自选。

#### 1.4.1 菜单总览树

```text
短线狙击 /sniper（6000，目录，menu_type=0）
├─ 扫描信号（6100，子目录）
│   ├─ 扫描报告     /sniper/scan/report/index      （6110）
│   │   └─ 报告详情 /sniper/scan/report/detail/:id （6114，visible=0 隐藏路由）
│   ├─ 信号明细     /sniper/scan/signal/index      （6120）
│   └─ 被拒明细     /sniper/scan/rejected/index    （6130）
├─ 信号台账（6200，子目录）
│   └─ 仓位台账     /sniper/ledger/position/index  （6210）
│       └─ 仓位详情 /sniper/ledger/position/detail/:id（6211，visible=0 隐藏路由）
├─ 复盘分析（6300，子目录）
│   └─ 复盘看板     /sniper/review/index           （6310）
├─ 数据资产（6400，子目录）
│   ├─ 日行情查询   /sniper/data/price/index       （6410）
│   ├─ 市场快照     /sniper/data/snapshot/index    （6420）
│   ├─ 事件数据     /sniper/data/event/index       （6430）
│   ├─ 基础数据     /sniper/data/basic/index       （6440）
│   └─ 数据刷新     /sniper/data/refresh/index     （6450）
└─ 运维监控（6500，子目录）
    ├─ 任务运行记录 /sniper/ops/dailyrun/index     （6510）
    ├─ LLM调用审计  /sniper/ops/llm/index          （6520）
    └─ 外部请求审计 /sniper/ops/request/index      （6530）
```

> 嫌两级目录层级深，可把 11 个菜单平铺在 `/sniper` 下，结构与内容不变。前端组件目录：nqboard-ui `views/sniper/{scan,ledger,review,data,ops}/`，路由 path 与上表一致。

#### 1.4.2 各菜单明细

**扫描信号组（§3.3 扫描业务表 + §10 手动端点）**

| 菜单(ID) | 页面内容 | 数据来源 | 按钮（menu_type=2，permission） |
|---|---|---|---|
| 扫描报告(6110) | 列表：as_of/universe/漏斗计数(stage0/1/2_count)/signal_count/市场门拦截标记(market_gate_blocked)/prefetch_ok；详情(6114 隐藏路由)渲染 `report_md`（Markdown）+ `report_json` 原文 + meta 漏斗统计 | `sniper_scan_report` | 手动扫描 `sniper_scan_run`(6111，选 universe 四档)；指定票复查 `sniper_scan_recheck`(6112，tickers 入参，confirm_only 语义不入台账)；导出 `sniper_scan_export`(6113，`scan_<date>.json/.md`) |
| 信号明细(6120) | action 三态筛选（entry_ok/watch/avoid）；confidence/weighted_score/quantity/trial_cash_used/reasoning；reasons/risk_flags/analyst_summary/prefetch_warnings JSON 展开 | `sniper_scan_signal` | —（只读） |
| 被拒明细(6130) | 按拒因/按报告筛选；顶部按 23 个拒因分组统计（复盘对账核心入口）；decision/warnings JSON 查看 | `sniper_scan_rejected` | —（只读） |

**信号台账组（§9）**

| 菜单(ID) | 页面内容 | 数据来源 | 按钮 |
|---|---|---|---|
| 仓位台账(6210) | status 三态筛选（pending_entry/open/closed）；entry_*/stop_*/close_*、close_reason 四态（stop_loss/horizon/gap_abort/never_filled）、pnl/pnl_pct/excess_ret/max_gain/max_drawdown；详情(6211 隐藏路由)画每日 mark 收益曲线 vs 沪深300 | `sniper_ledger_position` + `sniper_ledger_mark` | 手动跟踪 `sniper_ledger_track`(6212，触发 ingest+fill+marks+close，即 15:40 track Job 手动版)；导出 `sniper_ledger_export`(6213，`ledger.md`) |

**复盘分析组（§9 review）**

| 菜单(ID) | 页面内容 | 数据来源 | 按钮 |
|---|---|---|---|
| 复盘看板(6310) | 汇总指标：胜率/平均净收益/平均超额/平均持有天数/profit_factor；四组分桶：置信度5桶 × 理由标签 × 快筛分4桶 × 市场环境（market_ret_5d≥0 派生 market_up/down）；页顶红线告警条（连续两周胜率<40% 暂停实盘，Java 新增自动告警输出，§9） | `sniper_ledger_position`/`sniper_ledger_mark` 聚合（never_filled/gap_abort 不计入统计） | 生成复盘 `sniper_review_run`(6311)；导出 `sniper_review_export`(6312，`review_<date>.md`) |

**数据资产组（§3.1/3.2 十三张表 + §4.4 日更 + §4.5 回填）**

| 菜单(ID) | 页面内容 | 数据来源 | 按钮 |
|---|---|---|---|
| 日行情查询(6410) | code+日期区间查询，adjust none/qfq 切换，source 列标注；**强制筛选条件**（code 或日期）防全表拉取 | `sniper_daily_price`、`sniper_adj_factor` | —（只读） |
| 市场快照(6420) | 按交易日查全市场快照（涨跌幅/成交额/换手率），Stage0 输入 | `sniper_market_snapshot` | —（只读） |
| 事件数据(6430) | 一个页面五个 Tab：个股新闻（含预打标 sentiment）/股东增减持/主力资金流/龙虎榜/限售解禁 | `sniper_company_news`/`sniper_insider_trade`/`sniper_fund_flow_daily`/`sniper_dragon_tiger`/`sniper_restricted_release` | —（只读） |
| 基础数据(6440) | 一个页面六个 Tab：交易日历/股票基础（东财行业口径）/指数成分/行业板块日K/财务指标（双腿）/复权因子 | `sniper_trade_calendar`/`sniper_stock_basic`/`sniper_index_constituents`/`sniper_industry_board_daily`/`sniper_financial_indicator`/`sniper_adj_factor` | —（只读） |
| 数据刷新(6450) | 各数据阶段最近成功时间与缺口展示；手动触发 §4.4 五阶段（快照/日历/行情增量+qfq 重算/事件数据）及 §4.5 历史回填 | `sniper_daily_run`(data_update) | 触发刷新 `sniper_data_refresh`(6451) |

**运维监控组（§3.4）**

| 菜单(ID) | 页面内容 | 数据来源 | 按钮 |
|---|---|---|---|
| 任务运行记录(6510) | 按 run_date/phase 查 4 类任务（data_update/scan/track/review）状态 running/done/failed、耗时、东财日预算消耗 budget_used、缺数标记 detail | `sniper_daily_run` | —（只读） |
| LLM调用审计(6520) | 3 个 Dify 工作流（news_sentiment/policy_sentiment/flat_decision）的调用状态/latency_ms/error/出入参 JSON | `sniper_llm_call_log` | —（只读） |
| 外部请求审计(6530) | 出站 HTTP 全量记录：endpoint/source/n_rows/elapsed_ms/error | `sniper_request_log` | —（只读） |

#### 1.4.3 配套字典（sys_dict）

前端筛选项直接引用字典，不写死枚举：

| 字典标识 | 说明 | 取值 |
|---|---|---|
| sniper_scan_action | 信号动作 | entry_ok/watch/avoid |
| sniper_position_status | 仓位状态 | pending_entry/open/closed |
| sniper_close_reason | 平仓原因 | stop_loss/horizon/gap_abort/never_filled |
| sniper_run_phase | 任务阶段 | data_update/scan/track/review |
| sniper_run_status | 任务状态 | running/done/failed |
| sniper_reject_reason | 拒因 | 23 个 `RejectReasonEnum.code()`（逐字节一致，与 D5 契约测试同源） |
| sniper_llm_workflow | LLM 工作流 | news_sentiment/policy_sentiment/flat_decision |
| sniper_llm_status | LLM 调用状态 | ok/parse_error/timeout/http_error |
| sniper_sentiment | 新闻情绪 | positive/negative/neutral |
| sniper_adjust | 复权口径 | none/qfq |
| sniper_universe | 股票池 | all/hs300/csi500/hs300_csi500 |

#### 1.4.4 后端接口分组与前端落位

网关路由 `/sniper/**`（6008）。§1.1 已定 `ScanController`/`LedgerController`/`ReviewController`；按本规划补 `DataController`（§3.1/3.2 数据查询 + 数据刷新触发）与 `OpsController`（daily_run/llm_log/request_log 查询）。页面查询走平台标准 MyBatis-Plus 分页 + `R<T>` 返回体；`report_md`/`report_json`/`meta` 等 JSON/长文本列原样透传，由前端渲染。

**接口路径三层约定（对齐 device 等现有模块）**：前端/网关经 `/sniper/**` 访问；平台网关 `NqBoardRequestGlobalFilter` 做全局 StripPrefix=1，因此 Controller 的 `@RequestMapping` **不带 `/sniper` 前缀**（对齐 device `@RequestMapping("/category")`）；路由在 Nacos `nqboard-gateway-dev.yml` 增加 `- id: nqboard-sniper-biz / uri: lb://nqboard-sniper-biz / Path=/sniper/**`（限流配置对齐 device 路由样式）。下文各模块文档接口表中的 `/sniper/**` 路径均指前端/网关路径。分页实现按平台延迟关联范式（先查 id 再查详情，`buildQueryWrapper` 单一来源）。

#### 1.4.5 明确不建菜单的部分

1. **调度管理**：4 个 Quartz Job（data_update/scan/track/review）挂 nqboard-visual-quartz 定时任务管理台（§1.2/§10），不在 sniper 重复建设。
2. **参数与密钥**：SniperProperties 全部阈值、Tushare token、Dify API key 走 Nacos + jasypt（§11），平台配置中心负责。
3. **Dify 工作流编排**：Dify 平台自身管理，nqboard 侧只消费调用结果（§6）。

#### 1.4.6 分期落地建议

菜单不阻塞后端开发，随期建齐：

| 期 | 菜单 |
|---|---|
| M1~M2（数据层对拍期） | 数据资产 5 页 + 外部请求审计（对拍期间需直观看数据质量） |
| M5 后 | LLM 调用审计 |
| M6（台账+复盘+调度上线） | 扫描信号 3 页、仓位台账、复盘看板、任务运行记录 + 全部操作按钮 |

**核心五页**（PM 每日盘后 + 每周复盘的完整闭环，优先保证）：扫描报告、仓位台账、复盘看板、被拒明细、任务运行记录。

---

## 2. 五个顶层决策（先读）

### D1 AkShare SDK 不可用 → 全部改直连 HTTP

Java 没有 akshare 库。**所有数据用 HTTP 直连获取**，三组端点：

| 源 | 端点 | 说明 |
|---|---|---|
| Tushare | `POST https://api.tushare.pro`，body `{"api_name":"daily","token":"...","params":{...},"fields":"..."}` | 主源；端点名与 Python SDK 完全一致（pro_bar 除外，见下） |
| 东方财富直连 | `push2his.eastmoney.com/api/qt/stock/kline/get`（K线）、`push2.eastmoney.com/api/qt/clist/get`（全市场列表=快照）、`push2.eastmoney.com/api/qt/stock/get`（个股信息 f57/f58/f84/f85/f116/f117/f127/f189/f43）、`push2ex.eastmoney.com`（涨停池）、`search-api-web.eastmoney.com/search/jsonp`（新闻兜底） | **`src/market/cn/web_fallback.py`（981 行）就是现成的可运行参考实现，Java 照抄 URL+参数+Header（浏览器 UA）即可** |
| 腾讯/新浪 | `web.ifzq.gtimg.cn/appstock/app/fqkline/get`、`qt.gtimg.cn/q=`（GBK 解码）、`quotes.sina.cn/.../getKLineData`、`hq.sinajs.cn/list=`（需 Referer 头） | 仅兜底 |

Tushare 端点可用性（2000 积分档实测）：`daily`、`pro_bar`、`index_daily`、`trade_cal`、`index_weight`、`stock_basic`、`daily_basic`、`moneyflow`、`top_list`、`share_float`、`stk_holdertrade`、`margin_detail`、`adj_factor` 可用；实时快照/新闻/板块K线/涨跌停池不可用（保持东财直连）。

> `pro_bar` 是聚合接口（= daily + adj_factor 本地计算复权），**没有同名 HTTP 端点**。Java 侧两种实现：① 逐票调 `daily`+`adj_factor` 后按公式 `hfq=价×factor`、`qfq=价×factor÷max(factor)` 本地计算（先抽样验证与 Python 版 pro_bar 输出 bit-exact）；② 直接用 `daily(trade_date=...)` 全市场批量 + `adj_factor(trade_date=...)` 全市场批量，本地合成（Stage1 提速关键，见 §4.5）。

### D2 MySQL 就是行情库（SQLite 方案停做）

Python 版的三层缓存（内存 → 当日磁盘 JSON → 网络，`src/data/cache.py` + `src/data/daily_cache.py`，`data/cache/*.json`）在 Java 版收敛为：**内存/Redis 热点（RedisUtils，Caffeine 可选）→ MySQL 表（持久）→ 外部 HTTP（缺口拉取）**。网关每个方法的读序：

```
Redis/Caffeine（当日内热点，TTL=当日收盘后失效） → MySQL 对应表 → 缺口查询 → HTTP 拉取 → 回写 MySQL → 返回
```

当日磁盘缓存的"同日不重拉"语义由 MySQL 天然满足；`data/cache/*.json` 不再需要。

**并行计划处置**：本仓库曾做过行情 SQLite 化实验——`data/market.db`（2026-09-26 产物，675k 行 `daily_bars`，无 `source` 列）**当前无任何代码引用**（src/scripts/tests 零命中），是孤儿产物；2026-10-01 起草的 SQLite 重构方案（`daily_prices` 表 PK(code,date,adjust)+source 列）**自本方案起停做**，直接按本方案建 MySQL 表（列设计与该方案兼容：同唯一键 UK(code,trade_date,adjust)、带 `source` 列）。`data/market.db` 保留只读作历史参考，不迁移不删除。

### D3 复权口径：只存 `none` 和 `qfq` 两套 + 复权因子表

- 短线管线全链路消费 **qfq**（`api.get_prices` 显式传 `CN_LIVE_PRICE_ADJUST=qfq`），`CN_PRICE_ADJUST=hfq` 仅是 provider 层的默认参数（`akshare_client.py:412` / `providers/akshare_provider.py:21`，adjust=None 时才生效），短线调用方从不触发——**迁移范围内 hfq 零消费，不建 hfq 行**（full-profile agent 是它的唯一潜在消费方，已在不迁移清单）。
- `sniper_daily_price` 以 `(code, trade_date, adjust)` 为主键，adjust ∈ {`none`,`qfq`}。
- **qfq 的除权陷阱**：除权除息日该票全部历史 qfq 价会平移。对策：每日增量后用 `adj_factor` 对比检测因子变化 → 变化票重拉全历史 qfq（见 §4.4）。`none` 行 insert-only 永不重拉。
- **涨跌停价是唯一的"必须不复权"路径**：Python 版两处——① 停牌检查显式请求 `adjust="none"`（`security_status.py:59`）；② 正式涨停/跌停价计算在 `trading_rules.calc_limit_prices`，用**交易所发布的前收盘价**（除权除息日用调整后 reference_close，不能用原始昨收）。Java 版 `SecurityStatus`/`calcLimitPrices` 必须基于 `none` 行 + 除权日 reference_close 实现。
- 注意区分：Stage1/Stage3 的 `limit_up_distance_pct`、`consecutive_limit_up` 特征是在 **qfq 序列**上按板块比例近似计算的（`features/technical.py`），只用于打分；可交易性判断（`tradeability.py`）才用上面那条不复权路径。两套并存是现状语义，Java 照搬。
- 单位陷阱：各源单位不一致（**权威单位矩阵见 §4.3.0**——Tushare daily vol=手/amount=千元、东财 K线/快照 vol=手/amount=元、新浪 vol=股/amount=元、Tushare moneyflow/daily_basic/margin 金额=万元、top_list=元）。行情表（sniper_daily_price/sniper_market_snapshot）**原样入库 + `source` 列标注**，消费侧按 source 换算（对拍期间禁止任何"顺手统一"）；事件/资金类表入库时即换算成 Python client 层对外单位（元），换算规则逐表写在 §4.3。

### D4 Dify 承担全部 LLM 节点，Java 侧统一 fail-closed

全系统 LLM 调用点只有 3 类（新闻情绪批量分类 / 政策舆情判断 / 空仓决策合成），全部做成 Dify Workflow。**失败语义在 Java 侧兜底**：Dify 调用失败/超时/JSON 非法 = `LlmCallException` → 对应 agent 标 `llm_error` → gate 拒票（或 PM 决策降级 watch）。绝不消费 Dify 的默认/占位输出。详见 §6.4。

> 与现状对齐：`.env` 默认 `CN_DECISION_MODE=rules`/`CN_SENTIMENT_MODE=rules`，但**每日实际运行命令带 `--model glm-5.3`**（[`每日操作指南.md`](每日操作指南.md) §二 标准命令），即决策与情绪都走 LLM、厂商为智谱。Dify 侧默认模型选 `glm-5.3` 就是延续现状。

### D5 拒因字符串是跨系统契约

台账/复盘/报告按拒因前缀解析（完整清单 §8.3，**23 个**）。Java 用枚举 `RejectReason`，每个枚举带 `code()` 返回与 Python 版**逐字节相同**的字符串；gate/ledger/复盘全部走枚举，落库时存 `code()`。拼写漂移 = 历史分桶统计静默断裂，CI 加契约测试锁死。

---

## 3. MySQL 全量建表 DDL（库 `nqboard_sniper` · 脚本 `db/nqboard_sniper.sql`）

> 遵循 nqboard 房规（对照 `db/nqboard_device.sql` 样式）：反引号标识符、表尾 `ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '…' ROW_FORMAT = Dynamic`（下文表尾写法一致）；列级 CHARACTER SET 子句省略（随表 utf8mb4_general_ci）。脚本 `db/nqboard_sniper.sql` 的文件头对齐 device.sql：`CREATE DATABASE nqboard_sniper DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;` + `USE nqboard_sniper;`，每表前 `DROP TABLE IF EXISTS`、表尾 `SET FOREIGN_KEY_CHECKS = 1;` 收尾。
> **统一 BaseEntity 范式（全表六件套；2026-10-02 复核拍板：不做豁免）**：所有表 DDL 一律以 `id bigint`（@TableId ASSIGN_ID 雪花）+ 审计五列 `create_by/create_time/update_by/update_time/del_flag`（@TableLogic，填充器自动写）开头，实体统一继承 `BaseEntity`（`com.mx.nqboard.common.mybatis.base`），对齐平台"实体统一继承 BaseEntity"铁律。
> - **业务表**（scan_* / ledger_* / daily_run / llm_call_log）：幂等靠 UNIQUE KEY；走 BaseMapper/IService 标准 CRUD，审计列由填充器自动写。
> - **行情/事件表**（calendar/basic/constituents/price/factor/snapshot/board/financial/news/insider/fund_flow/dragon/restricted/request_log）：append-only、无逻辑删除语义——`del_flag` 仅为满足平台范式保留（恒 '0'，查询不做逻辑删除过滤）；原复合自然键改"代理 id 主键 + UNIQUE KEY"，批量 `INSERT ... ON DUPLICATE KEY UPDATE` 命中 UK，幂等语义不变；不走 BaseMapper 单条 CRUD，批量路径的审计列由代码显式赋值（create_by='sniper'、create_time=now()、del_flag='0'，命中 UK 时 update_time=now()），不依赖填充器；保留 `fetched_at`（§4.3.0 通用规则）。
> 实体/服务命名对照：`sniper_scan_report` ↔ `ScanReportEntity`（放 sniper-api/entity）↔ `ScanReportMapper` / `ScanReportService(Impl)`（放 sniper-biz）。
> 类型：价格 `decimal(16,4)`、金额 `decimal(20,4)`、量 `decimal(18,2)`（保留源精度，消费侧再转 int，见 D3）。

### 3.1 行情与基础数据

```sql
-- 交易日历（磁盘缓存等价物；预灌 1990~至今+400天）
CREATE TABLE `sniper_trade_calendar` (
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

-- 股票基础信息（Tushare stock_basic 批量 + 东财 f127 行业修正，§4.3.2）
CREATE TABLE `sniper_stock_basic` (
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

-- 指数成分（index_weight 每次取最新快照日，§4.3.3）
CREATE TABLE `sniper_index_constituents` (
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

-- 日行情（核心表，双复权口径；UK(code,trade_date,adjust) 即防重，§4.3.4）
CREATE TABLE `sniper_daily_price` (
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

-- 复权因子（qfq 重算 + 除权检测；adj_factor 支持 trade_date 全市场一次拉）
CREATE TABLE `sniper_adj_factor` (
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

-- 全市场快照（Stage0 输入；每日收盘后一行/票，§4.3.5）
CREATE TABLE `sniper_market_snapshot` (
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

-- 行业板块日K（sector_rotation 输入，§4.3.6）
CREATE TABLE `sniper_industry_board_daily` (
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
```

### 3.2 基本面与事件数据

```sql
-- 财务指标（同花顺 indicator + tushare daily_basic 估值合并，两腿各存一行，§4.3.7）
CREATE TABLE `sniper_financial_indicator` (
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

-- 个股新闻（sentiment/policy 输入，§4.3.8）
CREATE TABLE `sniper_company_news` (
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

-- 高管/股东增减持（sentiment insider 腿；tushare stk_holdertrade 优先，§4.3.9）
CREATE TABLE `sniper_insider_trade` (
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

-- 主力资金流（main_force_flow 输入；tushare moneyflow 优先，万元→元后入库，§4.3.10）
CREATE TABLE `sniper_fund_flow_daily` (
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

-- 龙虎榜（dragon_tiger 输入；tushare top_list 按日全市场 / em 区间兜底，§4.3.11）
CREATE TABLE `sniper_dragon_tiger` (
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

-- 限售解禁（gate 解禁否决输入；tushare share_float 优先，§4.3.12）
CREATE TABLE `sniper_restricted_release` (
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
```

### 3.3 扫描业务表（实体继承 BaseEntity：id + 审计五列）

```sql
-- 扫描报告头（每次运行一行；UK(as_of,universe) 保证幂等重跑覆盖）
CREATE TABLE `sniper_scan_report` (
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

-- 信号明细（列对齐 BuySignal，src/scan/models.py）
CREATE TABLE `sniper_scan_signal` (
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

-- 被拒明细（复盘对账核心）
CREATE TABLE `sniper_scan_rejected` (
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

-- 模拟台账仓位（tracking.py TrackedPosition 等价物；UK 幂等）
CREATE TABLE `sniper_ledger_position` (
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

-- 台账每日 mark（update_marks 每日一行；全量重建幂等）
CREATE TABLE `sniper_ledger_mark` (
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
```

### 3.4 运维/审计表

```sql
-- 调度幂等与预算记账（Quartz Job 幂等键；实体继承 BaseEntity）
CREATE TABLE `sniper_daily_run` (
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

-- LLM 调用审计（对应 P-07 缺口：token/成本可离线评估；实体继承 BaseEntity）
CREATE TABLE `sniper_llm_call_log` (
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

-- 外部请求审计（Python CN_REQUEST_LOG 等价物，对拍 fixtures 录制源；append-only，审计列由 AOP 写入路径显式赋值）
CREATE TABLE `sniper_request_log` (
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
```


### 3.5 业务表写入方对照（pipeline 产物，非外部接口）

业务表的字段不来自外部接口，来自扫描/台账管线的计算结果——开发时按下表找"谁写哪列"（公式出处全部指向 [`重构.md`](重构.md) 第 2~6 章的现状规格）：

| 表 | 写入组件 | 触发时机 | 关键列来源 |
|---|---|---|---|
| `sniper_scan_report` | `ScanPipeline`（收尾 upsert，UK(as_of,universe) 重跑覆盖） | Quartz scan Job 15:35 / `POST /scan/run` | `as_of`=resolveAsOf(end_date)；`market_ret_5d`/`market_gate_blocked`=市场门（§5.1）；`stage0/1/2_count`=漏斗计数；`meta.budget`=日预算；`report_json`=`ScanReport.to_dict()`（重构.md §10.1）；`report_md`=Markdown 渲染器；`signal_count`=signals 数 |
| `sniper_scan_signal` | `ScanPipeline` | 同上 | 列 = `BuySignal` 字段 1:1（重构.md §10.1）：confidence/weighted_score/quantity/trial_cash_used/reasoning/reasons/risk_flags/analyst_summary/screen_score/prefetch_warnings |
| `sniper_scan_rejected` | `ScanPipeline` | 同上 | `reasons`=prefetch 拒因 + gate 拒因（§8.3 全部 23 个 code()）；`decision`=PM 决策对象；`warnings`=prefetch 告警 |
| `sniper_ledger_position` | `LedgerService.ingest/fill/close` | Quartz track Job 15:40 | ingest：confidence/weighted_score/quantity/reasons… ← 报告 entry_ok 信号；fill：entry_* ← T+1 开盘价+费用（gap≤−2% → gap_abort）；close：close_* ← 止损/T+5 到期（重构.md §6.1） |
| `sniper_ledger_mark` | `LedgerService.updateMarks`（全量重建幂等） | track Job | close/ret_from_entry/ret_from_t0 ← 行情；bench_*/excess_ret ← 沪深300（重构.md §6.1） |
| `sniper_daily_run` | Quartz Job 包装器（各任务首尾各写一次） | 4 个 Job | run_date/phase/status/budget_used/detail；UK(run_date,phase) 即幂等键 |
| `sniper_llm_call_log` | `DifyClient`（每次调用收尾，成功失败都写） | 每次 LLM 调用 | workflow/tickers/input_digest/output_json/status/latency_ms/error |
| `sniper_request_log` | HTTP 拦截器（AOP 切面统一写） | 全部出站请求（含兜底链） | endpoint/source/params/status/n_rows/elapsed_ms/error |

> **报告重跑的子表清理（2026-10-03 补）**：`sniper_scan_signal` / `sniper_scan_rejected` 只有 `report_id` 普通索引、无唯一键——`sniper_scan_report` 按 UK(as_of,universe) 重跑覆盖时，ScanPipeline 必须**先按 report_id 删除两张子表的旧行再插入**（同一事务内），否则信号/被拒明细随重跑次数翻倍。台账 ingest 以"最新报告"为准，不受子表重建影响（UK(cohort_date,ticker) 幂等）。

---

## 4. 数据接入层设计

### 4.1 DataGateway 接口（唯一 IO 边界）

Python 侧的 IO 边界是 `src/tools/api.py`（**注意路径带 `src/` 前缀**，20 个公开函数）。其中短线管线真正消费的子集 + 独立模块 `security_status.py` 的能力，映射为下面的接口；`search_line_items`/`get_market_cap`/`is_st_stock`/`get_northbound_holdings`/`get_margin_detail`/`get_margin_balance`/`get_limit_pool`/`get_auction_snapshot` 仅被不迁移的 agent 或无人消费，**不进接口**（北向预检告警用 `getNorthboundHoldings` 例外保留）。

```java
public interface DataGateway {
    // 行情
    List<Price> getPrices(String ticker, LocalDate start, LocalDate end, Adjust adjust); // 默认 QFQ
    List<Price> getIndexPrices(String indexCode, LocalDate start, LocalDate end);
    MarketSnapshotFrame getSpotSnapshot(LocalDate asOf);          // 全市场快照(sniper_market_snapshot)
    // 基础
    List<LocalDate> getTradingDays(LocalDate start, LocalDate end);
    LocalDate resolveAsOf(LocalDate raw);                          // 非交易日前移
    Set<String> getIndexConstituents(String indexCode);            // 最新快照日
    StockBasic getStockBasic(String code);                         // 含 list_date/industry
    Map<String, BigDecimal> getAdjFactors(String tradeDate);       // 全市场因子(除权检测/qfq合成,§4.4)
    // 财务与事件（全部显式 asOf，P-06 纪律）
    List<FinancialIndicator> getFinancialMetrics(String ticker, LocalDate asOf, int limit);
    List<CompanyNews> getCompanyNews(String ticker, LocalDate asOf, LocalDate start, int limit);
    List<InsiderTrade> getInsiderTrades(String ticker, LocalDate asOf, int limit);
    List<FundFlow> getMainFundFlow(String ticker, LocalDate asOf, int days);
    List<DragonTiger> getDragonTiger(String ticker, LocalDate asOf, int days);
    List<RestrictedRelease> getRestrictedRelease(String ticker, LocalDate asOf);
    String getStockIndustry(String ticker);
    List<BoardBar> getIndustryBoardHist(String boardName, LocalDate asOf);
    SecurityStatus getSecurityStatus(String ticker, LocalDate asOf); // 停牌+不复权涨跌停价(trading_rules.calc_limit_prices 语义)
    // 北向仅保留预检告警例外（北向分析师不迁移）
    List<NorthboundHolding> getNorthboundHoldings(String ticker, LocalDate asOf);
}
```

**纪律：每个方法显式收 `asOf`**，窗口计算一律锚定 asOf 回溯（Python 版个别位置用 `datetime.now()` 的前视偏差在 Java 版直接消灭；唯一例外是 Stage0 次新过滤以"快照日"为锚——快照本身就是当日盘中数据，与 Python `universe.py:294` 一致）。

### 4.2 Provider 路由与降级链

```
CompositeProvider（路由）
  行情: Tushare → 东财K线(push2his fqt=1) → 腾讯fqkline → 新浪(不复权,仅当请求none时可用,qfq请求降级到新浪必须打 dirty 标)
  快照: 东财 clist → 新浪行情中心直连(get_market_spot_sina) → 腾讯批量报价(需代码表,取 tushare stock_basic)
        —— 照搬 Python fetch_a_share_spot 的 4 级链(universe.py:100)；akshare-sina 中间层由
        "新浪直连"替代，行为等价。全部失败才允许 Stage0 报错退出。
  新闻: 东财个股新闻 → search-api-web jsonp
  资金流/龙虎榜/解禁/增减持/成分/日历: Tushare → 东财/新浪对应直连
```

QoS 参数（对齐 Python 版）：

| 机制 | 参数 | 实现建议 |
|---|---|---|
| Tushare 节流 | 350ms 全局间隔 | 自研令牌桶/计数器（Sentinel 规则亦可），全局锁间隔照 Python `_throttle` |
| Tushare 重试 | 3 次，限频错误退避 2n s、其他 1n s + rand | 自实现双判定（照 Python `_call`），不引 Spring Retry |
| 东财节流 | 500ms 下限 | 同上 |
| 东财熔断 | 7 类连接错误 → OPEN 300s | Sentinel 熔断规则（或自研状态机）+ `waitDurationInOpenState=300s` 语义，OPEN 期间直接路由腾讯/新浪 |
| 重试（东财/腾讯/新浪） | 最多 5 次，退避 2n+rand(0,1) s | 照 Python `run_throttled_retry` |
| 日预算 | 300 次/日，仅"东财主源"模式计数；Tushare 主源不消耗 | `AtomicInteger` + `sniper_daily_run.budget_used` 持久化；耗尽 → Stage1 跳票并记 `stage1_skipped_due_to_budget`（Python `rate_budget.py` 语义） |
| 请求审计 | 全部 HTTP 出站 | AOP 切面统一写 `sniper_request_log`（endpoint/source/params/n_rows/elapsed_ms）。Python 版 `CN_REQUEST_LOG` 是 opt-in 的 JSONL 文件（`src/data/request_log.py`），Java 侧默认全量，对拍录制直接查表 |

> **日预算的两处口径细节（2026-10-03 源码核对）**：① 全市场快照请求**无条件消耗 1 次预算**（`universe.py:113` 不判 tushare 主源模式——快照链永远东财开头，"仅东财主源计数"对快照不适用）；② 预算耗尽除 `stage1_skipped_due_to_budget` 外还记 `truncated_at_stage`，取值为 `"stage1"` 或 `"stage1_market_env"`（quick_screen.py:49,69），报告 meta 须带出。③ 行情反向兜底：非 Tushare 优先模式下 AkShare 行情为空会按 `CN_PRICE_FALLBACK`（默认 tushare）回退 Tushare（composite.py:105-134），Java 版 CompositeProvider 保留该双向兜底。④ `sniper_request_log` 字段与 Python 版差异：Python 记录无 `summary` 键（摘要字段平铺进顶层），Java 表结构本就平铺，无影响。

### 4.3 表级入库规格（查什么 → 字段怎么映射 → 怎么入库）

> 每张表一节：主源端点+参数 → 响应字段→表列映射（含单位与换算）→ 更新方式（回填/日更/按需）→ 兜底链。端点与字段映射均从 `tushare_client.py` / `web_fallback.py` / `akshare_client.py`（生效的末版函数）逐行核对，Java 实现时对照抄即可。

#### 4.3.0 总览与通用入库规则

| 表 | 主源 | 兜底链 | 更新方式 | 每日调用量(约) |
|---|---|---|---|---|
| sniper_trade_calendar | Tushare `trade_cal` | 无(星期近似仅日志告警) | 预灌全量 + 每日 1 调 | 1 |
| sniper_stock_basic | Tushare `stock_basic` | 东财 f127 行业修正 | 全量预灌 + 每周 1 调；f127 按需 | 1 + 按需 |
| sniper_index_constituents | Tushare `index_weight` | 中证官网成分接口 | 每次扫描前刷新 | 1~2 |
| sniper_daily_price / sniper_adj_factor | Tushare `daily`+`adj_factor`(按日全市场) | 东财K线→腾讯→新浪(§4.3.4) | 日更 2 调 + 缺口逐票 | 2 + 缺口 |
| sniper_market_snapshot | 东财 clist(~55 页) | 新浪行情中心→腾讯批量 | 每日 15:05 一次全市场 | ~55(东财,不计Tushare预算) |
| sniper_industry_board_daily | 东财板块K线(push2his secid=90.BK) | THS板块→合成等权指数 | deep 票按需(近120日) | ≤8 |
| sniper_financial_indicator | THS indicator + Tushare `daily_basic` | — | deep 票按需 | ≤16 |
| sniper_company_news | 东财个股新闻 | EM search jsonp | deep 票按需(fetch_date 增量) | ≤8 |
| sniper_insider_trade | Tushare `stk_holdertrade` | THS 股东变动 | deep 票按需(365日) | ≤8 |
| sniper_fund_flow_daily | Tushare `moneyflow` | 东财个股资金流 | deep 票按需(30日) | ≤8 |
| sniper_dragon_tiger | Tushare `top_list`(按日全市场) | 东财 LHB 区间 | 日更 1 调 | 1 |
| sniper_restricted_release | Tushare `share_float` | 东财解禁队列 | deep 票按需(−3y~+2y) | ≤8 |

**单位换算权威矩阵**（Provider 层换算依据，别凭记忆写）：

| 源/接口 | volume | 金额 |
|---|---|---|
| Tushare `daily` / `index_daily` | **手** | **千元** |
| Tushare `moneyflow` / `daily_basic.total_mv` / `margin_detail` | — | **万元**（Python 层 ×1e4→元） |
| Tushare `top_list` | — | **元**（实测口径，无需换算） |
| Tushare `stk_holdertrade.change_vol` / `share_float.float_share` | **股** | — |
| Tushare `share_float.float_ratio` | — | **百分点**（8.0=8%，÷100 存小数） |
| 东财 K线（push2his 第6列 / clist f5） | **手** | **元** |
| 东财 clist f6 / push2ex amount | — | 元 |
| 腾讯 fqkline / qt.gtimg 位置36 | **手** | 位置37=**万元**(×1e4→元) |
| 新浪 K线 / 行情中心 / hq.sinajs | **股**(÷100→手) | 元 |

通用规则：

1. **入库单位分层**：行情表（sniper_daily_price/sniper_market_snapshot）原样存+source 标注（量比等比值特征对单位不敏感，bit-exact 优先）；**事件/资金类表入库时即换算成 Python client 层的对外单位（元、股、小数比例）**——main_force_flow 的阈值 `max(均值×0.8, 1元)`、龙虎榜 `net_buy/1e8 亿` 格式化、解禁 `_normalize_ratio` 都按这些单位硬编码，入库不换算消费侧就得到处补。
2. **upsert 幂等**：全部 `INSERT ... ON DUPLICATE KEY UPDATE`，UNIQUE KEY 即防重（复合自然键一律建为 UK，`id` 仅为代理主键）；同一任务重复跑不产生重复行。
3. **日期格式**：DATE 列一律 `YYYY-MM-DD`；Tushare/东财的 `YYYYMMDD` 先转换再入库。
4. **失败语义**：主源失败→按兜底链降级→全链失败记 `sniper_daily_run.detail` 缺数标记；消费侧按各 agent 的"缺失→neutral 40 / fail-open"语义兜底（§7.3），**不允许因单表缺数抛异常终止扫描**（行情除外：无行情=Stage1 淘汰，这本身就是行为）。
5. **请求审计**：所有出站 HTTP（含兜底链）写 `sniper_request_log`；Tushare 侧同时受 350ms 节流+3 次重试约束（限频错误退避 2n s、其他 1n s+rand，`tushare_client._call` 语义照抄）。
6. **Tushare HTTP 响应信封（所有 `api_name` 通用）**：`POST api.tushare.pro`，body `{"api_name":"…","token":"…","params":{…},"fields":"列名1,列名2"}`；响应 `{"code":0,"msg":null,"data":{"fields":["列名1","列名2"],"items":[[行1…],[行2…]],"has_more":false}}`——**fields 是列名数组、items 是行数组且与 fields 序一一对应**，Java 侧按 fields 序号位解析行数组（不要假设列序固定，fields 不传时 Tushare 给默认全集）；`has_more=true` 时必须带 `offset` 参数翻页重调，漏翻页会静默丢数据。以下各表的"接口字段"均指 `data.fields` 中的列名。
7. **source 相关换算封装在数据层（2026-10-03 补）**：行情/快照表按 source 混存单位（如 `sniper_daily_price.amount` 千元/元混存），所有"按 source 换算/判源"的逻辑只允许出现在 provider/DataGateway/mapper 层，业务与算法代码不得直接判 source——否则"顺手统一"的口子一旦撕开，bit-exact 对拍就守不住。

#### 4.3.1 交易日历 `sniper_trade_calendar`

- 端点：`POST api.tushare.pro` api_name=`trade_cal`，params `{"exchange":"SSE","start_date":"19900101","end_date":"<今天+400天的YYYYMMDD>"}`。
- 映射：`cal_date`→cal_date、`is_open`(1/0)→is_open。全量约 9k 行，1 调拿完。
- 更新：回填 1 调；此后每日 1 调刷新尾部窗口。交易日判断 / resolveAsOf（非交易日前移）/ lookback_start 全部以此表为准；表为空时退化为"周一~周五近似"（`calendar.py` 同款兜底，仅日志告警）。

**字段映射（Tushare `trade_cal` → 表列）**：

| 接口字段（data.fields） | 表列 | 类型/换算 | 说明 |
|---|---|---|---|
| `cal_date` | `cal_date` | YYYYMMDD→DATE | 日历日期 |
| `is_open` | `is_open` | tinyint 原样 | 1=交易日 |
| `pretrade_date` | —（不入库） | — | 现链路未消费 |
| （请求参数 `exchange`="SSE"） | — | — | 请求侧字段 |

#### 4.3.2 股票基础 `sniper_stock_basic`（含行业两套口径）

- 端点：`stock_basic` params `{"exchange":"","list_status":"L"}`，fields `ts_code,symbol,name,area,industry,market,exchange,list_date`（全量 ~5400 行，1 调）。
- 映射：`ts_code`→code（"."前 6 位）+ exchange（后缀）+ ts_code 原样；name→name；`list_date`→list_date；`industry`→industry（industry_source='tushare'）。
- **行业两套口径（关键，别用错）**：tushare `stock_basic.industry` 是旧分类（"电气设备/酿酒行业"），**东财 f127** 才是与板块名对齐的现用口径（"电池/白酒Ⅱ/证券Ⅱ"）——sector_rotation 与 gate 行业 cap 消费的都是它。东财修正：`GET push2delay.eastmoney.com/api/qt/stock/get?fltt=2&invt=2&fields=f57,f58,f127&secid={m}.{code}`（m：代码 6/9/5 开头=1，否则=0；push2delay 优先、push2 兜底），f127 非空则覆盖 industry 列、industry_source='em'。tushare 口径保留作合成板块兜底（成员近似用 `web_fallback._TS_INDUSTRY_ALIASES` 别名表，Java 同步内置该表）。
- 更新：回填/每周全量 1 调；f127 在 deep 票分析+gate 行业 cap 前逐票查并回写。

**字段映射①（Tushare `stock_basic` → 表列；请求 params `{exchange:"",list_status:"L"}`）**：

| 接口字段 | 表列 | 类型/换算 | 说明 |
|---|---|---|---|
| `ts_code` | `code` + `exchange` + `ts_code` | "."前6位→code；后缀→exchange；原样→ts_code | 一字段拆三列 |
| `name` | `name` | 原样 | ST 过滤依据 |
| `industry` | `industry`（+ `industry_source`='tushare'） | 原样 | 旧分类，仅合成板块兜底用 |
| `list_date` | `list_date` | YYYYMMDD→DATE | 次新过滤 |
| `symbol` / `area` / `market` / `exchange`（接口列） / `list_status` | —（不入库） | — | 现链路未消费 |

**字段映射②（东财 f127 行业修正 → 覆盖列）**：

| 接口字段（data.*） | 表列 | 说明 |
|---|---|---|
| `f127` | `industry`（覆盖）+ `industry_source`='em' | 与板块名对齐的现用口径；空/"-"/"--" 不覆盖 |
| `f57` / `f58` | —（校验用） | 应与 code/名称一致 |

#### 4.3.3 指数成分 `sniper_index_constituents`

- 主源：`index_weight` params `{"index_code":"000300.SH"|"000905.SH"}`（39 开头用 .SZ）。返回跨多日快照，**只取 max(trade_date) 那天**；`con_code` 取"."前 6 位、校验 6 位数字。snapshot_date=该 max(trade_date)。
- 兜底：中证指数官网成分接口（akshare `index_stock_cons_csindex`/`index_stock_cons_weight_csindex` 底层；"成分券代码"列正则提取 6 位）。
- 更新：每次扫描前各拉 1 次（universe=hs300_csi500 共 2 调）；同 snapshot_date 幂等覆盖。

**字段映射（Tushare `index_weight` → 表列）**：

| 接口字段 | 表列 | 类型/换算 | 说明 |
|---|---|---|---|
| `index_code` | `index_code` | 请求参数回显 | 000300.SH / 000905.SH |
| `trade_date` | `snapshot_date` | YYYYMMDD→DATE | 一次调用返回多个快照日，**只取 max(trade_date) 的全部行** |
| `con_code` | `code` | "."前6位，正则校验 6 位数字 | |
| `weight` | —（不入库） | — | 现链路不用权重 |

兜底（中证官网，akshare `index_stock_cons_csindex` 底层）：列 `成分券代码` → `code`（正则提取 6 位）。

#### 4.3.4 日行情 `sniper_daily_price` / `sniper_adj_factor`（核心表）

**批量日更（15:10，共 2 调）**：

- `daily` params `{"trade_date":"<当日YYYYMMDD>"}` **一次拉全市场** → 每票一行 adjust='none'。映射：ts_code→code；trade_date→DATE；open/high/low/close/**pre_close** 原样（pre_close 是除权调整后昨收，tushare 独有，不复权序列推不出，必须从这里拿）；`vol`（手）→volume；`amount`（千元）→amount；source='tushare'。
- `adj_factor` params `{"trade_date":"<当日>"}` 全市场一次 → `sniper_adj_factor`。

**qfq 本地合成（15:12）**：`qfq_price = none_price × factor ÷ max_factor`（max_factor=该票**全历史** max(factor)，OHLC 四列同乘；volume/amount 不乘——复权只作用价格，与 Python `pro_bar` 语义一致）。当日行算好即 upsert；**除权检测**：当日 factor ≠ 该票前一交易日 factor → 该票全历史 qfq 重算覆盖（每天几十~200 只，可控）。

**指数行情**：`index_daily` params `{"ts_code":"000300.SH","start_date":..,"end_date":..}` → 同映射入 sniper_daily_price（code='000300'、adjust='none'）。消费方：市场门 5 日涨幅、台账沪深300基准。000905/399006 同理（`_INDEX_CODES` 三只）。

**逐票缺口兜底链**（读库缺数据时网关自动触发，顺序照 Python）：

1. Tushare 逐票：`daily(ts_code,start,end)` + `adj_factor(ts_code,start,end)` 本地合成（pro_bar 等价实现，公式同上）；
2. 东财K线：`GET push2his.eastmoney.com/api/qt/stock/kline/get?secid={m}.{code}&fields1=f1..f6&fields2=f51..f61&klt=101&fqt={0|1|2}&beg={YYYYMMDD}&end={YYYYMMDD}`——m：6/9/5 开头=1 否则=0；**fqt 0=不复权 1=qfq 2=hfq**；klines 是 CSV 行，**字段顺序：日期,开盘,收盘,最高,最低,成交量,成交额,振幅,涨跌幅,涨跌额,换手率（注意是 O-C-H-L，不是 O-H-L-C！）**；volume 手、amount 元；
3. 腾讯：`GET web.ifzq.gtimg.cn/appstock/app/fqkline/get?param={sh|sz}{code},day,{YYYY-MM-DD},{YYYY-MM-DD},640[,qfq|hfq]`——行格式 `[date,open,close,high,low,vol(手),...]`（同样是 O-C-H-L），**单次上限 640 根**，更长窗口分段拉；
4. 新浪：`GET quotes.sina.cn/cn/api/json_v2.php/CN_MarketDataService.getKLineData?symbol={sh|sz}{code}&scale=240&ma=no&datalen=1023`（Sina Referer 头）——**只有不复权**；volume 股（÷100→手 后入库）。qfq 请求不许降级到新浪（宁可缺数淘汰，也不让复权口径污染指标）。

兜底行 source='em'/'tencent'/'sina'，**原样存**（新浪股→手换算在入库前完成，与 Python web_fallback 一致）。

**历史回填**：按交易日循环 `daily(trade_date)`+`adj_factor(trade_date)`（全市场各 1 调，~250 调/年/口径），qfq 本地合成；完成后**抽样 ≥50 只与 Python `get_prices(adjust='qfq')` 逐字段比对 bit-exact**（判定口径见 §12 头注：按存储精度取整后相等），不一致的票退回逐票拉取模式（D1 验证门）。

**字段映射①（Tushare `daily`，params `{trade_date:"YYYYMMDD"}` 全市场一次 → adjust='none' 行）**：

| 接口字段 | 表列 | 类型/换算 | 说明 |
|---|---|---|---|
| `ts_code` | `code` | "."前6位 | |
| `trade_date` | `trade_date` | YYYYMMDD→DATE | |
| `open` / `high` / `low` / `close` | 同名列 | 元，原样 | |
| `pre_close` | `pre_close` | 元，原样 | **除权调整后昨收，只有这里有**；涨跌停计算依赖 |
| `vol` | `volume` | **手**，原样 | |
| `amount` | `amount` | **千元**，原样 | |
| `change` / `pct_chg` | —（不入库） | — | 可由 OHLC 推导 |

**字段映射②（Tushare `adj_factor`，params `{trade_date}` → `sniper_adj_factor`）**：`ts_code`→`code`、`trade_date`→`trade_date`（→DATE）、`adj_factor`→`factor`（原样 decimal(20,10)）。

**字段映射③（Tushare `index_daily`，params `{ts_code:"000300.SH",start_date,end_date}` → 指数 none 行）**：`ts_code`→`code`（前6位，如 000300）、`trade_date`→DATE、`open/high/low/close` 原样、`vol`→`volume`（手）、`amount`（千元）；`pre_close/change/pct_chg` 不入库。source='tushare'。

**字段映射④（东财 K线兜底，klines CSV 行按逗号分割 → 各 adjust 行）**：

| CSV 位置 | 表列 | 类型/换算 | 说明 |
|---|---|---|---|
| parts[0] | `trade_date` | YYYYMMDD→DATE | |
| parts[1] | `open` | 元 | **顺序是 O-C-H-L，别按 O-H-L-C 接** |
| parts[2] | `close` | 元 | |
| parts[3] | `high` | 元 | |
| parts[4] | `low` | 元 | |
| parts[5] | `volume` | **手**，原样 | |
| parts[6] | `amount` | 元，原样 | |
| parts[7]~parts[10]（振幅/涨跌幅/涨跌额/换手率） | —（不入库） | — | 未消费 |
| （接口无 pre_close） | `pre_close` | 用前一日 close 回填或留空 | 除权修正价只有 tushare daily 有 |

**字段映射⑤（腾讯 fqkline，行格式 `[date,open,close,high,low,vol,…]`）**：row[0]→`trade_date`（YYYY-MM-DD 原样）、row[1]→`open`、row[2]→`close`、row[3]→`high`、row[4]→`low`、row[5]→`volume`（**手**原样）；无 amount/pre_close。

**字段映射⑥（新浪 getKLineData，JSON `{day,open,high,low,close,volume}`）**：day→`trade_date`、open/high/low/close 原样（元）、volume→`volume`（**股 ÷100→手**）；**仅不复权**，qfq 请求不许走此源。

**qfq 行合成规则**：`sniper_daily_price(adjust='qfq')` 的 OHLC 四列 = none 行同列 × factor ÷ max_factor（该票全历史 max(factor)）；`volume`/`amount`/`pre_close` 与 none 行相同（复权只作用价格）。

#### 4.3.5 全市场快照 `sniper_market_snapshot`

- 主源：东财 clist 分页 `GET push2.eastmoney.com/api/qt/clist/get?pn={1..N}&pz=100&po=1&np=1&fltt=2&invt=2&fid=f3&fs=m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23,m:0+t:81+s:2048&fields=f12,f13,f14,f2,f3,f5,f6,f8,f15,f16,f17,f18`（akshare `stock_zh_a_spot_em` 底层等价；~55 页拉完全市场）。映射：f12→code、f14→name、f2→price、f3→change_pct(%)、f5→volume(手)、f6→amount(元)、f8→turnover_rate、f15/f16/f17→high/low/open、f18→prev_close、f13→交易所（结合代码段定板块）。
- 兜底1 新浪行情中心：`GET vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData?page={n}&num=80&sort=amount&asc=0&node=hs_a&symbol=&_s_r_a=page`（Sina Referer 头；~70 页）——`trade`=最新价、`settlement`=昨收、`changepercent`=涨跌幅、`amount`(元)、`volume`(**股**,÷100)、`open/high/low`。
- 兜底2 腾讯批量：`GET qt.gtimg.cn/q={sh|sz}{code},...`（**GBK 解码**，60 码/批，代码表来自 sniper_stock_basic）——payload 按 `~` 分割，位置：2=代码、1=名称、3=最新、4=昨收、5=今开、32=涨跌幅、33=最高、34=最低、36=总手(手)、37=成交额(**万元**×1e4→元)、38=换手率。
- 更新：每日 15:05 一次全量 upsert（UK trade_date+code）。**无法回填历史**（clist 只有当日），回填期 Stage0 一字板过滤失真的已知问题见 §4.5。三源字段对齐以 Python `quotes_to_spot_df` 的 schema（代码/名称/最新价/涨跌幅/成交额/成交量/今开/最高/最低）为准。

**字段映射①（东财 clist，fields 串，`fltt=2` 时数值已是浮点）**：

| 接口字段 | 表列 | 类型/换算 | 说明 |
|---|---|---|---|
| `f12` | `code` | 原样 | |
| `f13` | —（定交易所） | 0=深 1=沪 | 结合代码段定板块 |
| `f14` | `name` | 原样 | |
| `f2` | `price` | 元 | |
| `f3` | `change_pct` | % | |
| `f5` | `volume` | **手** | |
| `f6` | `amount` | 元 | |
| `f8` | `turnover_rate` | % | |
| `f15` / `f16` / `f17` | `high` / `low` / `open` | 元 | |
| `f18` | `prev_close` | 元 | |
| `f10`（量比）等其余 | —（不入库） | — | 未消费 |

**字段映射②（新浪行情中心 item）**：

| 接口字段 | 表列 | 类型/换算 |
|---|---|---|
| `code` | `code` | zfill(6) |
| `name` | `name` | 原样 |
| `trade` | `price` | 元 |
| `settlement` | `prev_close` | 元 |
| `changepercent` | `change_pct` | %（trade/settlement 都非空时优先现算） |
| `amount` | `amount` | 元 |
| `volume` | `volume` | **股 ÷100→手** |
| `open` / `high` / `low` | 同名列 | 元 |

**字段映射③（腾讯 qt.gtimg，GBK，payload 按 `~` 分割取位置索引）**：

| 位置 | 表列 | 类型/换算 |
|---|---|---|
| parts[2] | `code` | zfill(6) |
| parts[1] | `name` | 原样 |
| parts[3] | `price` | 元 |
| parts[4] | `prev_close` | 元 |
| parts[5] | `open` | 元 |
| parts[32] | `change_pct` | % |
| parts[33] / parts[34] | `high` / `low` | 元 |
| parts[36] | `volume` | **手** |
| parts[37] | `amount` | **万元 ×1e4→元** |
| parts[38] | `turnover_rate` | % |

三源对齐基准：Python `quotes_to_spot_df` schema（代码/名称/最新价/涨跌幅/成交额/成交量/今开/最高/最低）；`turnover_rate` 仅东财/腾讯有，新浪置空。

#### 4.3.6 行业板块日K `sniper_industry_board_daily`

- 板块代码表：`GET push2(delay).eastmoney.com/api/qt/clist/get?pn={1..6}&pz=100&po=1&np=1&fltt=2&invt=2&fid=f3&fs=m:90+t:2&fields=f12,f14` → ~496 个行业板块，f14=板块名、f12=BK 码。名字→BK码映射做长缓存（字典表或 Caffeine），板块名先精确后互为子串模糊匹配（`_get_board_codes_em` 语义）。
- 主源：`GET push2his.eastmoney.com/api/qt/stock/kline/get?secid=90.{bk}&fields1=f1..f6&fields2=f51..f61&klt=101&fqt=0&beg&end`——CSV 顺序同 §4.3.4（O-C-H-L），volume 手、amount 元 → board_name/trade_date/open/high/low/close/volume/amount。
- 兜底链（照 Python `get_industry_board_hist` 顺序）：东财直连 → 同花顺板块指数（板块名精确→子串匹配；列名 开盘价→开盘 等改名后对齐）→ **合成等权指数**（取板块市值前 12 名成员、各自 close 归一到首日=1 后逐日平均，剔除 200/900 开头 B 股；成员接口 `fs=b:{bk}&fid=f20` 不可用时用 tushare industry+别名表近似）。
- 更新：deep 票 sector_rotation 消费时按需拉近 120 日并回写（Python 现状；日更任务不必全量刷 90+ 板块）。

**字段映射（东财板块K线 CSV → 表列）**：与 §4.3.4 字段映射④ 同款——parts[0]→`trade_date`、parts[1]→`open`、parts[2]→`close`、parts[3]→`high`、parts[4]→`low`、parts[5]→`volume`（手）、parts[6]→`amount`（元）。板块名/BK码来自代码表（f14/f12，走缓存不入库）。

THS 兜底（列改名后入库）：`日期`→`trade_date`、`开盘价`→`open`、`最高价`→`high`、`最低价`→`low`、`收盘价`→`close`、`成交量`→`volume`、`成交额`→`amount`。合成等权指数兜底：仅产 `trade_date`+`close`，volume/amount 留空，source='synthetic'。

#### 4.3.7 财务指标 `sniper_financial_indicator`

- 腿1（利润率/成长，按报告期）：同花顺财务指标（akshare `stock_financial_analysis_indicator(symbol=code, start_year="2018")`；Java 直连同花顺或改用 Tushare `fina_indicator`，列名映射照 `financial_mapping.py`）→ gross_margin/net_margin/roe/revenue_growth/profit_growth，report_period=报告期，source='ths'。
- 腿2（估值，按交易日）：`daily_basic` params `{"ts_code":..,"start_date":..,"end_date":..}` → `pe_ttm`/`pb`/`ps_ttm` 原样；`total_mv`（**万元×1e4→元**）→market_cap；report_period=估值交易日，source='tushare'。
- 合并语义（Python `_merge_metrics`）：报告期字段取腿1 最新一期（PIT：**报告期+45 自然日后才可用**，`CN_PIT_REPORT_LAG_DAYS`，过滤放消费侧）；估值字段取腿2 锚定 asOf 的最近交易日。两腿各存一行，消费侧按需取——不要入库时强行合成单行。
- 更新：deep 票按需（Stage2/财务消费），当日重复查询由表幂等吸收。

**字段映射①（腿1 同花顺指标，列名照 `financial_mapping.py`，五个消费列）**：

| 接口列 | 表列 | 类型/换算 | 说明 |
|---|---|---|---|
| `日期` | `report_period` | →DATE | 报告期 |
| `销售毛利率(%)`（兜底 `毛利率`） | `gross_margin` | **%→小数 ÷100** | `_pct_to_ratio` 语义，库里存小数 |
| `销售净利率(%)`（兜底 `净利率`） | `net_margin` | ÷100 | |
| `净资产收益率(%)` | `roe` | ÷100 | |
| `主营业务收入增长率(%)`（兜底 `营业收入增长率(%)`） | `revenue_growth` | ÷100 | |
| `净利润增长率(%)` | `profit_growth` | ÷100 | |
| （其余 ~35 列） | —（不入库） | — | 短线消费面之外；`pe/pb/ps/market_cap` 由腿2 提供 |
| （固定） | `source`='ths' | | 腿1 各行估值列留空 |

**字段映射②（腿2 Tushare `daily_basic`）**：

| 接口字段 | 表列 | 类型/换算 | 说明 |
|---|---|---|---|
| `trade_date` | `report_period` | YYYYMMDD→DATE | 估值交易日 |
| `pe_ttm` / `pb` / `ps_ttm` | 同名列 | 原样 | |
| `total_mv` | `market_cap` | **万元 ×1e4→元** | |
| `close`/`turnover_rate`/`dv_ratio`/`total_share` 等其余 | —（不入库） | — | 未消费 |
| （固定） | `source`='tushare' | | 腿2 各行利润率/成长列留空 |

#### 4.3.8 个股新闻 `sniper_company_news`

- 主源：东财个股新闻（akshare `stock_news_em(symbol=code)` 底层；Java 直连同款 push2 新闻接口）。列映射：新闻标题→title、新闻内容→body_digest（≤2000 字）、发布时间→published_at、文章来源→source_name、新闻链接→url。
- 兜底：`GET search-api-web.eastmoney.com/search/jsonp?cb=jQuerycb&param={json}`，param JSON：`{"uid":"","keyword":"<6位代码>","type":["cmsArticleWebOld"],"client":"web","clientType":"web","clientVersion":"curr","param":{"cmsArticleWebOld":{"searchScope":"default","sort":"default","pageIndex":1,"pageSize":<limit>,"preTag":"<em>","postTag":"</em>"}}}`——**keyword 用代码不是名称**（与 Python 一致）；响应剥 jsonp 壳后取 `result.cmsArticleWebOld[]`，title/content 去 `<em>` 标签，`mediaName`→source_name、`date`→published_at、`url`、`content`→body_digest。
- 预打标（入库时照抄 Python 规则）：sentiment=`_sentiment_from_text(title+body)`（正/负关键词计数，pos>neg→positive、neg>pos→negative、否则 neutral；关键词表 `_POSITIVE_KW`/`_NEGATIVE_KW` 照搬）；announcement_type 五规则：业绩预告(业绩预告/业绩快报)、回购(回购)、减持(减持/清仓)、监管(立案/问询/处罚/监管)、政策(政策/国务院/央行/证监会)。
- 更新：deep 票 Stage2 预检时拉（limit 50~100），`fetch_date=当日` 作增量游标；`uk(code,published_at,title(100))` 幂等去重。0 条新闻仅告警不拒票（§5.4）。

**字段映射①（主源 EM 个股新闻接口，akshare `stock_news_em` 封装，底层 np-listapi getListInfo——Java 直连时以 akshare 源码为准抄 URL/参数）**：

| 接口列 | 表列 | 类型/换算 | 说明 |
|---|---|---|---|
| `新闻标题` | `title` | 原样 | |
| `新闻内容` | `body_digest` | ≤2000 字截断 | |
| `发布时间` | `published_at` | →datetime | |
| `文章来源` | `source_name` | 空默认"东方财富" | |
| `新闻链接` | `url` | 原样 | |
| `关键词`（请求回显） | —（不入库） | — | |
| （请求参数 symbol=6位代码） | `code` | | |
| （入库时计算） | `sentiment` / `announcement_type` / `id`（雪花）/ `fetch_date`=当日 | 规则打标 | §4.3.8 正文规则 |

**字段映射②（兜底 EM search jsonp，`result.cmsArticleWebOld[]`）**：`title`→`title`（去 `<em>` 标签）、`content`→`body_digest`、`date`（前10位）→`published_at`、`url`→`url`、`mediaName`→`source_name`（空默认"东方财富"）。

#### 4.3.9 高管/股东增减持 `sniper_insider_trade`

- 主源：`stk_holdertrade` params `{"ts_code":..,"start_date":"<asOf−365d>","end_date":"<asOf>"}`。映射：`ann_date`（空则 `demat_date` 兜底再退 end_date）→ann_date；holder_name；holder_type；`change_vol`（股）→change_vol，**`in_de`='DE' 时取负**（减持为负，与 Python/东财口径一致）；`avg_price`；`after_share`→after_shares。source='tushare'。
- 兜底：同花顺股东变动（akshare `stock_shareholder_change_ths(symbol=code)`；列 公告日期/变动股东/变动数量/交易均价/剩余股份总数；"变动数量"是含"万股"的文本，解析语义照 `_parse_share_change`）。
- 更新：deep 票按需回写；`uk(code,ann_date,holder_name(50),change_vol)` 幂等。

**字段映射①（Tushare `stk_holdertrade` → 表列）**：

| 接口字段 | 表列 | 类型/换算 | 说明 |
|---|---|---|---|
| `ts_code` | `code` | "."前6位 | |
| `ann_date` | `ann_date` | YYYYMMDD→DATE | **空则 `demat_date` 兜底，再退请求 end_date** |
| `holder_name` | `holder_name` | 原样 | |
| `holder_type` | `holder_type` | 原样 | |
| `in_de` | （决定符号） | ='DE' 时 change_vol **取负** | 减持为负口径 |
| `change_vol` | `change_vol` | **股**，按 in_de 定号 | |
| `avg_price` | `avg_price` | 元/股 | |
| `after_share` | `after_shares` | 股 | |
| `change_ratio`/`total_share`/`after_ratio` 等 | —（不入库） | — | 未消费 |

**字段映射②（兜底 THS `stock_shareholder_change_ths`）**：`公告日期`→`ann_date`、`变动股东`→`holder_name`、`变动数量`→`change_vol`（**文本含"万股"，按 `_parse_share_change` 解析成股**）、`交易均价`→`avg_price`、`剩余股份总数`→`after_shares`、`变动途径`→`holder_type`。

#### 4.3.10 主力资金流 `sniper_fund_flow_daily`

- 主源：`moneyflow` params `{"ts_code":..,"start_date":"<asOf−30d>","end_date":"<asOf>"}`。映射：trade_date；`net_mf_amount`（**万元×1e4→元**）→main_net；super_net=`buy_elg_amount − sell_elg_amount`（万元×1e4）；large/medium/small_net 可从 buy_lg/sell_lg、buy_md/sell_md、buy_sm/sell_sm 同法推导（Python 只消费 main+super，其余列可空）。
- 兜底：东财个股资金流（akshare `stock_individual_fund_flow(stock=code, market="sh"|"sz")`；列"主力净流入-净额"等，单位元）。
- 更新：deep 票按需（main_force_flow agent 消费最近 5 日）；UK(code,trade_date) 幂等。**main_net 必须为元**——阈值 `max(均值×0.8, 1.0元)` 与 reasoning 的 `net_3d/1e8 亿` 格式化都按元硬编码。

**字段映射①（Tushare `moneyflow`，金额单位均为万元）**：

| 接口字段 | 表列 | 类型/换算 | 说明 |
|---|---|---|---|
| `ts_code` | `code` | "."前6位 | |
| `trade_date` | `trade_date` | YYYYMMDD→DATE | |
| `net_mf_amount` | `main_net` | **×1e4→元** | agent 阈值按元硬编码 |
| `buy_elg_amount − sell_elg_amount` | `super_net` | ×1e4→元 | |
| `buy_lg_amount − sell_lg_amount` | `large_net` | ×1e4→元 | |
| `buy_md_amount − sell_md_amount` | `medium_net` | ×1e4→元 | |
| `buy_sm_amount − sell_sm_amount` | `small_net` | ×1e4→元 | |
| 各 `*_vol` 列 | —（不入库） | — | 未消费 |

**字段映射②（兜底 EM `stock_individual_fund_flow`，单位已是元）**：`日期`→`trade_date`、`主力净流入-净额`→`main_net`、`超大单净流入-净额`→`super_net`、`大单净流入-净额`→`large_net`、`中单净流入-净额`→`medium_net`、`小单净流入-净额`→`small_net`；各"净占比"列不入库。

#### 4.3.11 龙虎榜 `sniper_dragon_tiger`

- 主源：`top_list` params `{"trade_date":"<当日YYYYMMDD>"}` **全市场一次**（Python 按日缓存；Java 放日更任务 15:15 对当日拉 1 调，比逐票区间查询省 ~30 倍）。映射：ts_code→code；trade_date；`reason`→reason；`net_amount`（**元**，实测口径）→net_buy；`l_buy`→buy_amt；`l_sell`→sell_amt；`pct_change`→change_pct。
- 兜底：东财 LHB 明细区间（akshare `stock_lhb_detail_em(start_date,end_date)` 底层 datacenter-web 接口；列 代码/收盘价/涨跌幅/龙虎榜买入额/卖出额/净买额/净买额占总成交比/上榜原因/上榜后1日/2日/5日/10日）。
- 消费：dragon_tiger agent 查该票近 30 日记录（5 日上榜次数、5 日净买合计、原因关键词分）。UK(code,trade_date,reason) 幂等。

**字段映射①（Tushare `top_list`，params `{trade_date}` 全市场一次，金额单位为元）**：

| 接口字段 | 表列 | 类型/换算 | 说明 |
|---|---|---|---|
| `ts_code` | `code` | "."前6位 | |
| `trade_date` | `trade_date` | YYYYMMDD→DATE | |
| `reason` | `reason` | 原样 | 上榜原因（agent 关键词打分） |
| `net_amount` | `net_buy` | **元，原样** | 实测口径，无需换算 |
| `l_buy` | `buy_amt` | 元 | |
| `l_sell` | `sell_amt` | 元 | |
| `pct_change` | `change_pct` | % | |
| `name`/`close`/`turnover_rate`/`amount`/`l_amount`/`net_rate`/`amount_rate`/`float_values` | —（不入库） | — | agent 只消费上榜次数/净买/原因 |

**字段映射②（兜底 EM `stock_lhb_detail_em`，区间一次）**：`代码`→`code`、`上榜日`→`trade_date`、`上榜原因`→`reason`、`龙虎榜净买额`→`net_buy`、`龙虎榜买入额`→`buy_amt`、`龙虎榜卖出额`→`sell_amt`、`涨跌幅`→`change_pct`；收盘价/占比/上榜后N日列不入库。

#### 4.3.12 限售解禁 `sniper_restricted_release`

- 主源：`share_float` params `{"ts_code":..,"start_date":"<asOf−3y>","end_date":"<asOf+2y>"}`。映射：`float_date`→plan_date；`float_share`（股）→shares；`float_ratio`（**百分点值**，8.0=8%）÷100→float_ratio（小数），**同值近似写入 float_mv_ratio**——gate `_normalize_ratio`（>1.5 视为百分比再归一）消费的"占解禁前流通市值比例"列在 Tushare 模式下实为占总股本（更小、更保守，只会放过边缘 case 不会误杀），与东财真流通市值口径不同，靠 source 列区分。
- 兜底：东财解禁队列（akshare `stock_restricted_release_queue_em(symbol=code)`；列 解禁时间/限售股类型/解禁数量/实际解禁数量/实际解禁市值/**占解禁前流通市值比例**——真流通市值口径）。
- 更新：deep 票按需（gate 解禁否决消费）；查询失败 **fail-open**（不拦截信号，`gate.py` 语义）；`uk(code,plan_date,shares)` 幂等。

**字段映射①（Tushare `share_float` → 表列）**：

| 接口字段 | 表列 | 类型/换算 | 说明 |
|---|---|---|---|
| `ts_code` | `code` | "."前6位 | |
| `float_date` | `plan_date` | YYYYMMDD→DATE | 解禁时间 |
| `float_share` | `shares` | **股，原样** | |
| `float_ratio` | `float_ratio` **和** `float_mv_ratio` | **百分点→小数 ÷100**（8.0→0.08） | 两列同值：tushare 口径实为占总股本，gate 消费的 `float_mv_ratio` 拿到的是保守近似（更小，只会放过边缘 case 不会误杀），靠 source 区分 |
| `holder_name` / `share_type` / `ann_date` | —（不入库） | — | gate 只消费 date+ratio |
| （固定） | `source`='tushare' | | |

**字段映射②（兜底 EM `stock_restricted_release_queue_em`）**：`解禁时间`→`plan_date`、`解禁数量`→`shares`（股）、`实际解禁市值`→`market_value`（元）、`占解禁前流通市值比例`→`float_mv_ratio`（**真流通市值口径，百分数→÷100**）、`float_ratio` 留空；source='em'。

### 4.4 每日数据更新任务（收盘后，扫描之前）

| 时间(约) | 任务 | 逻辑 |
|---|---|---|
| 15:05 | 快照 | 东财 clist 全市场（失败走 §4.2 快照兜底链）→ `sniper_market_snapshot`（当日 upsert） |
| 15:08 | 交易日历 | `trade_cal` 增量 → `sniper_trade_calendar` |
| 15:10 | 日行情增量 | Tushare `daily(trade_date=当日)` **一次拉全市场** → `sniper_daily_price(adjust='none')`；同口径 `adj_factor(trade_date=当日)` → `sniper_adj_factor`（规格 §4.3.4） |
| 15:12 | qfq 增量+重算 | ① 当日 qfq：对当日有交易的票 `qfq=none×factor÷最新factor` 本地算并 upsert；② **除权检测**：当日 factor ≠ 昨日 factor 的票集合 → 逐票重拉全历史（`pro_bar` 等价实现）覆盖 qfq 行（每天几十~200 只，可控） |
| 15:15 | 事件数据 | 龙虎榜：`top_list(trade_date=当日)` 全市场 1 调（§4.3.11）；其余——新闻(fetch_date=今日,§4.3.8)、增减持(§4.3.9)、资金流(§4.3.10)、解禁(§4.3.12)、板块K线(§4.3.6)、财务(§4.3.7)——按需增量（deep 票才拉也行：Stage2 预检时缺啥拉啥并回写，即"拉取即入库"，表幂等保证重跑无副作用） |
| 15:35 | 扫描 | ScanPipeline |

### 4.5 历史回填（一次性）

1. `stock_basic` 全量 → `sniper_stock_basic`（§4.3.2）；`trade_cal` 全量 → 日历表（§4.3.1）。
2. 循环交易日逐日：`daily(trade_date)` + `adj_factor(trade_date)` → none 行 + 因子（全市场一次一调，约 250 调/年，极快）。
3. qfq 历史：优先用因子本地合成（`close×factor÷max_factor`），**抽样 ≥50 只与 Python 版 `get_prices(adjust='qfq')` 输出逐字段比对**（判定口径见 §12 头注：按存储精度取整后相等），完全一致才允许此模式（D1 公式验证）；不一致的票退回逐票拉取模式。
4. 指数行情/成分随用随补（`index_daily`/`index_weight`，§4.3.3）；快照历史：**无法回填**（clist 只有当日）——回填期 Stage0 的一字板过滤在历史日期上会失真，对拍时需知悉（Python 版有同样问题，见 P-06）。

---

## 5. 漏斗 Stage 0~3 实现规格

> 以下阈值全部进 `SniperProperties`（§11，命名与 §1.1/§11 一致），值为 Python 版 `.env` 当前值。

### 5.1 市场门（最先执行，先于 Stage0）

沪深300 最近 5 个交易日涨幅（6 根收盘K线，`market_env.fetch_market_ret_5d`：5 个区间需 6 根收盘）< `market.minRet=0.0` → 直接落库空报告（`market_gate_blocked=1`），跳过一切后续（零 LLM/零漏斗请求）。开关与阈值默认值：代码默认 `CN_SCAN_MARKET_GATE_ENABLED=false`、`CN_SCAN_MARKET_MIN_RET=-0.03`；`.env` 覆盖为 `true` / `0.0`（2026-09 台账复盘：五日下跌批次胜率仅 28.6%）——Java 版保留开关、默认取 true 与 0.0 并在配置里注明出处。

### 5.2 Stage 0 池过滤（9 条全过才保留）

| # | 规则 | 参数 |
|---|---|---|
| 1 | 代码可提取 6 位数字 | — |
| 2 | 在指数成分内 | universe=hs300∪csi500（支持 all/hs300/csi500/hs300_csi500 四档） |
| 3 | 剔除 ST | 名称 `^\*?ST`（忽略大小写） |
| 4 | 剔除科创/北交 | 688/689/4xx/8xx |
| 5 | 剔除次新 | `list_date` 距今 < 60 自然日（Tushare 缺失时 fail-open，Stage1 ≥30 根兜底） |
| 6 | 流动性 | 当日成交额 ≥ 1e8 元（成交额缺失时放行，fail-open） |
| 7 | 价格 > 0 | —（价格缺失时放行） |
| 8 | 一字涨停剔除 | change_pct ≥ 板块限值−0.5pp 且 \|开−高\|、\|高−低\|、\|低−收\| 均 < 价×0.001 |
| 9 | 近涨停剔除 | change_pct ≥ 板块限值−0.3pp；限值表 主板9.8/创业科创19.5/北交29.5 |

### 5.3 Stage 1 快筛（纯规则，~800 票）

1. 逐票取 60 交易日 qfq 行情（库优先）；**<30 根 → 淘汰**。
2. `TechnicalFeatureCalculator`（纯函数，落 `com.mx.nqboard.sniper.algorithm.indicator`，`features/technical.py` 1:1）：EMA5/10/20、MACD(12,26,9)、RSI6/14（**简化版：涨跌单侧滚动均值；单侧为零映射为极值——全涨→100、全跌→0；仅当涨跌都为 0（窗口内无波动）才返回 50**——`_rsi()` 注释明确这是防钝化关键）、momentum_5d、volume_ratio=mean5/mean20、20日突破（≥0.995×20日高）、volatility_20d（√244 年化）、ma_ratio、gap_open_pct、涨停族特征（板块动态限值±0.1%容差）、**trend_strength**（`0.30*ema+0.25*macd+0.20*rsi+0.15*mom+0.10*vol`，clamp[-1,1]；子分构造：ema_score=排列×spread、macd_norm=hist/close×50、rsi_score=(rsi6−50)/30、mom_score=sign×min(|ret5|×10,1)、vol_score=(vr−1)/1.5 且放量下跌取负）。
3. 过热否决（与 gate 共用同一 Bean，见 §8.2）→ 命中即淘汰。
4. 快筛打分表（逐条照抄 `quick_screen.py`，与 重构.md §3.2 一致；阈值常量在 `short_metrics.py`）：EMA排列+25/−5/+10、MACD金叉+10、动量×量比+20（1%~8% 且量比≥1.2）/+10（0.5%~1%）/+3（>8%，追高风险）/−5（<−8%，放量大跌）、量比+15（≥1.5）/+8（≥1.2）、RSI +10（25~40 接近超卖反弹）/+15（40~70 健康上升）/+8（≤25 深度超卖）/−3（≥75 超买）、突破+15（突破且量比≥1.5）/+5、趋势强度+15（>0.3）/+8（>0.1）/−8（<−0.2）、市场环境+10（market_ret_5d>−2%）/−5（<−4%）、连板惩罚−15（≥2 连板且今日仍封板 limit_up_dist<0.005））→ `<35` 淘汰。
5. 全候选按分降序取前 `max_screen`——**CLI 默认 30，每日命令行传 50**（无对应 env 变量）；Java 版做成配置项，默认 50 与日常运行对齐。

### 5.4 Stage 2 数据预检（进 LLM 前最后一道）

| 项 | 必需 | 失败 |
|---|---|---|
| 行情 ≥60 根（`max(30, lookback)`） | 是 | 拒因 `prefetch:prices` |
| 新闻接口可达（0条仅告警） | 是 | 拒因 `prefetch:news_fetch` |
| 增减持/北向/行业/板块 | 否 | warning 入报告 |

拒因兜底：预检失败但列不出缺失项时用 `prefetch:failed`（`run_flat_short.py:118`）。

### 5.5 Stage 3 输出门禁 → 见 §8。

---

## 6. Dify 配置与调用契约

### 6.1 平台前置

1. **Dify 版本** ≥0.10（云平台或自部署均可；自部署建议 `docker compose` 且挂公网出口）。
2. **模型供应商**：设置 → 模型供应商 → **智谱 AI**，填 `ZHIPU_API_KEY`；默认模型 `glm-5.3`（与每日运行 `--model glm-5.3` 一致，见 D4），参数统一 **temperature=0.1**（结构化任务要稳）。
3. 建 3 个应用，类型全部选 **工作流（Workflow）**（不是 Chatflow），各自获得 `app-xxx` API Key，配到 nqboard 的 yml。
4. 每个 LLM 节点：**开启 JSON 输出**（或后接"代码执行"节点做 `JSON.parse` + 字段校验，校验失败输出 `{"error":"parse_error"}`——**不要**在 Dify 里做静默重试，重试与 fail-closed 统一归 Java 管）。

### 6.2 W1 `news-sentiment-batch`（新闻情绪批量分类）

- **输入变量**：`ticker`(string)、`headlines`(string，编号文本行，**与 Python `_classify_news_batch_with_llm` 相同格式**：`"0. 标题一\n1. 标题二\n..."`，最多 5 条)。
- **LLM 节点 System prompt（照搬 Python 版原文，仅把变量换成 Dify 占位符）**：

```
你是 A 股新闻情绪分析师。请分析以下 {{headlines}} 条新闻标题对 {{ticker}} 未来 1-5 个交易日的短期情绪影响。

对每条新闻输出 index(与上面编号一致，从0开始)、sentiment(positive/negative/neutral)、confidence(0-100)。仅返回 JSON，格式: {"items": [{"index": 0, "sentiment": "...", "confidence": 80}, ...]}
```

- **输出 schema**：`{"items":[{"index":int≥0, "sentiment":"positive|negative|neutral", "confidence":0-100}]}`
- **Java 侧校验**：缺失条目回填 neutral（Python 行为：`signals=["neutral"]*n`，index 越界的 item 丢弃）；LLM 返回空/None → 整体判失败（走重试 → llm_error）。

### 6.3 W2 `policy-sentiment`（政策舆情判断）

- **输入变量**：`ticker`、`news`（已按 **14 个政策关键词**预筛，关键词表照抄 `akshare_client._POLICY_KW`：政策/国务院/央行/证监会/发改委/工信部/财政部/监管/规划/指导意见/十四五/补贴/降准/降息）。
- **LLM 节点 prompt（照搬）**：

```
你是 A 股政策舆情分析师。股票 {{ticker}} 近期政策/监管相关新闻如下：{{news}}。请判断对股价的短期影响（1-5日），输出 signal(bullish/bearish/neutral)、confidence(0-100)、reasoning(≤80字中文)。仅返回 JSON。
```

- **输出 schema**：`{"signal":"bullish|bearish|neutral","confidence":int,"reasoning":string}`
- **无/少政策新闻时 Java 不调用本工作流**（直接规则 neutral 40，省一次调用；与 `policy_sentiment.py:35` 一致）。

### 6.4 W3 `flat-decision`（空仓决策合成）

- **输入变量**：`context`(string，**JSON 对象，键=ticker**，每票结构与 Python `flat_decision_llm.py` 相同)：

```json
{
  "600519.SH": {
    "weighted_score": 0.4123,
    "score_reason": "…",
    "allowed_actions": {"buy": {"tradable": true, "block_reason": "", "quantity": 200}},
    "tradable": true,
    "block_reason": "",
    "rule_suggestion": {"action": "entry_ok", "quantity": 200, "confidence": 82, "reasoning": "…"}
  }
}
```

（注意：`rule_suggestion` 是**完整规则决策对象**，不是字符串枚举；`tradable`/`block_reason` 是独立顶层字段——Java 侧不要"简化"掉，LLM 依赖它们守合规。）
- **LLM 节点 System prompt（照搬，红线语义不可改）**：

```
你是 A 股空仓试仓分析师。空仓模式下只能输出 entry_ok(可试仓)、watch(观望)、avoid(回避)，禁止 sell。遵守 T+1、涨跌停等合规约束；若 tradable=false 则不可 entry_ok。参考 rule_suggestion 与 weighted_score，可微调但不得违反合规。理由≤80字。仅返回 JSON。
```

- Human 消息附带格式说明：`{"decisions": {"TICKER": {"action":"entry_ok|watch|avoid","quantity":int,"confidence":int,"reasoning":"...","tradable":bool,"block_reason":"","weighted_score":float}}}`。
- **Java 侧强制后处理**：`tradable=false` 的票无论 LLM 输出什么都改写 `watch`；`quantity` ≤ allowed quantity 且为整手（主板/创业板 100、科创 200，`round_lot`）；超界即改写（与 Python 版"合规 clamp"一致）。
- **失败降级**：规则版兜底但**全部 entry_ok 强制降级 watch**，reasoning 追加"LLM 合成不可用，规则兜底降级为 watch"，`block_reason=llm_unavailable`（`flat_decision_llm.py:100-113` 逐字语义）。

### 6.5 Java 调用契约（`DifyClient`）

```http
POST {dify.base-url}/v1/workflows/run
Authorization: Bearer app-xxxxxxxx
Content-Type: application/json

{"inputs": {"ticker":"600519.SH", "headlines":"0. ..."},
 "response_mode": "blocking",
 "user": "nqboard-scan"}
```

- 响应取 `data.outputs`（即工作流"结束"节点的输出变量）。
- **超时 90s/次（`llm.timeout=90`），重试 3 次（退避 2n+rand s）**；重试耗尽抛 `LlmCallException`。
- **失败语义表（fail-closed，全系统一致）**：

| 场景 | Java 处理 | 下游效果 |
|---|---|---|
| HTTP 非 200 / 超时 / 网络错误 | 重试3次 → `LlmCallException` | 该 agent payload `signal="n/a", confidence=0, reasoning.fallback="llm_error"` → gate 第7条拒票（`llm_unavailable:<agent>`） |
| 200 但 outputs 非法/字段缺失 | 同上（`parse_error`） | 同上 |
| W3 失败 | 规则版兜底但全部 entry_ok 强制降级 watch，`block_reason=llm_unavailable` | gate 全拒，报告可见 |
| Dify 输出与 pydantic 等价 schema 不符的"看似合理"默认值 | **一律不信** | 这是 fail-hard 存在的理由（Python 版 CN_LLM_FAIL_HARD=true） |

- 每次调用（成功/失败）写 `sniper_llm_call_log`。
- 调用量：8 只 deep 票 = 情绪 8 + 政策 ≤8 + flat 合成 1 = **≤17 次/扫描**。

---

## 7. Agent 层实现规格

### 7.1 上下文与编排

- `AnalysisContext`：`Map<agentId, Map<ticker, SignalPayload>> analystSignals` + `Map<ticker, RiskResult> riskResults` + metadata（model/mode/asOf/trialCash）。对应 LangGraph 的 `AgentState`，无消息传递。
- 编排（Java 17 无虚拟线程，CompletableFuture + 专用线程池）：

```java
// Java 17 无虚拟线程：CompletableFuture + 专用线程池
// 规模下限 = 分析师数(6)（单票内并行）；若跨票并行深度分析，按 6×max_deep=48 配置，
// 并在实现中说明线程安全边界（AnalysisContext 每 ticker 写自己的槽位，无共享可变状态）
var ex = Executors.newFixedThreadPool(12);
var futures = analysts.stream()
        .map(a -> CompletableFuture.runAsync(() -> a.analyze(ctx), ex))
        .toList();
CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join(); // 6 路并行
trendPredictor.analyze(ctx);   // 汇合：读前6家
riskManager.analyze(ctx);
portfolioManager.analyze(ctx); // flat: 规则合成 + 可选 Dify W3
```

（分析师集合 = `utils/analysts.py` SHORT_TERM_ANALYST_KEYS：technical/sentiment/main_force_flow/dragon_tiger/sector_rotation/policy_sentiment + trend_predictor；短画像用 main_force_flow 替代 northbound_flow——北向 2024-08 后停发。）

### 7.2 SignalPayload 统一结构

`{signal: bullish|bearish|neutral|n/a, confidence: 0-100, reasoning: {...}}`——`reasoning` 各 agent 自定义（gate 只深挖 `technical.reasoning.composite_metrics`）。

**两套特征字典并存（照搬现状，勿合并）**：
- **Stage1 用 features dict**（`features/technical.py`）：trend_strength、momentum_5d/20d、volatility_20d、rsi_6/rsi_14、ma_ratio、volume_ratio、limit_up_distance_pct、ema5/10/20、ema_align、ema_spread、macd_hist、macd_gold_cross、breakout_20d、breakout_up、vwap_dev、gap_open_pct、consecutive_limit_up、is_limit_up_today、limit_ratio。
- **Stage2/Gate 用 composite_metrics dict**（`agents/technicals.py`）：ema5/10/20、**adx**、rsi_6、**bollinger_z**、momentum_5d、volume_ratio、high_20、last_close、limit_up_distance_pct、consecutive_limit_up、macd_hist、trend_strength。
gate 的 `flat_trend_adx` 检查读的是 composite_metrics 里的 `adx`——Stage1 的 features 里没有它，Java 侧 FeatureCalculator 需提供两种导出视图。

### 7.3 七分析师规则速查（公式/阈值照抄 重构.md §4.2，已逐个对过源码）

| Agent | 数据(gateway 方法) | 规则要点 | LLM |
|---|---|---|---|
| technical | getPrices(60日 qfq) | 4子策略加权：趋势0.35(EMA排列+ADX14≥20) / 动量0.30(ret5 1~8%+量比>1.2；>8%弱多0.35) / 超买超卖0.20(RSI6<25且z<−1.5多；>75且z>1.5空) / 突破0.15(≥0.995×20日高+量比>1.5)；score=Σ(值×权重×conf)/Σ(权重×conf)，>0.2 bullish，conf=\|score\|×100；**输出 composite_metrics{ema5/10/20, adx, rsi_6, bollinger_z, momentum_5d, volume_ratio, high_20, last_close, limit_up_distance_pct, consecutive_limit_up, macd_hist, trend_strength}**（见 §7.2 两套字典） | 否 |
| sentiment | getInsiderTrades(1000) + getCompanyNews(100) | insider 0.3(减持=空) + news 0.7(LLM=W1 批量分类前5条；无LLM用关键词预打标)；失败→n/a（fail-closed：news 腿占 0.7，缺失不得静默合成） | W1 |
| main_force_flow | getMainFundFlow(5日) | threshold=max(\|均值\|×0.8, 1元)；连续流入≥2日且3日净额>threshold→bullish conf=min(85,50+min(连续,4)×8+min(net3d/均值,3)×5)；对称空；仅3日净额越阈→bullish/bearish 55；否则 neutral 48；缺失→neutral 40（不拒票）；超大单占比>0.3 同向 +5（cap 88） | 否 |
| dragon_tiger | getDragonTiger(30日) | 近5日上榜≥2且净买>0→bullish conf=min(85,60+次数×5+min(净买/1e8,5)×4)；近5日上榜≥1且净买>0且买/卖比>1.3→bullish conf=min(75,55+min(净买/1e8,3)×5)；净买<0→bearish conf=min(78,55+min(\|净买\|/1e8,5)×5)；上榜≥1但净买≈0→neutral 50；近30日上榜但近5日无→neutral 45；未上榜→neutral 40；原因关键词同向 +4（cap 88） | 否 |
| sector_rotation | getStockIndustry + getIndustryBoardHist | 板块3日涨幅>1%→bullish(<−1%空) conf=min(85,52+\|ret\|×400+同向+8)；**板块缺失→neutral 40，禁止用个股涨幅替代**（`sector_rotation.py:134`） | 否 |
| policy_sentiment | getCompanyNews(50)+14关键词预筛 | 有政策新闻+LLM模式→W2；失败→n/a；无/少政策新闻→neutral 40 | W2 |
| trend_predictor | 前6家信号 + getPrices/getFinancialMetrics/flow特征/短结构 | score=0.40×trend_strength+0.05×基本面+0.15×情绪+0.20×资金流+0.05×分析师均值+0.15×连板结构（`rule_ensemble.py` SHORT_WEIGHTS）；≥0.15→up（≤−0.15 down）；输出 prediction{direction,expected_return_pct,trend_strength,factor_contributions} | 否 |

### 7.4 risk_manager（非信号型）

60日收益率 → 年化波动（√244）→ vol_multiplier（<15%→1.25；15~30%→1−(v−0.15)×0.5；30~50%→0.75−(v−0.30)×0.5；>50%→0.5；夹[0.25,1.25]，基线仓位20%）× corr_multiplier（均值相关 ≥0.8→0.70 / ≥0.6→0.85 / ≥0.4→1.00 / ≥0.2→1.05 / <0.2→1.10）。输出 `remainingPositionLimit = min(上限−现持仓市值, 现金)`；**PM 建议股数 = limit ÷ price 向下取整手**（主板/创业板100，科创200，`round_lot`），费用=佣金 max(0.025%,5元)+卖出印花税0.05%（`trading_rules.calc_trade_fees`）。

### 7.5 portfolio_manager（flat 合成）

加权得分（`decision.py` SHORT_TERM_ANALYST_WEIGHTS：technical .30 / main_force .15 / dragon .10 / sentiment .10 / sector .15 / policy .10 / trend .10；n/a 整项剔除；`+0.15×trend_strength` 后 clamp ±1）：
- score ≤ **−0.20** → avoid（conf=min(90, 55+\|score\|×40)）
- score ≥ **0.35** 且 buy 可交易 → entry_ok（conf=min(95, 55+score×45)；score<0.5 时数量减半）
- ≥0.35 但不可买 → watch（block_reason=涨停/停牌/资金不足）
- 其余 → watch（conf=55）；**永不输出 sell**
- decision_mode=llm 时走 W3（§6.4），失败全部降 watch。

---

## 8. Gate（输出门禁）实现规格

### 8.1 主流程

```
解禁否决(逐票) → passesGate(14条,全评估不短路) → BuySignal 落库
→ 按(confidence, weighted_score)降序 → 行业cap(1,留最强,余 industry_cap) → 每日cap(1,余 daily_cap)
```

行业标签拿不到时 **fail-open 放行**（`gate.py:186`，数据故障不得拦信号），拒因写 `industry_cap:<industry>>1`。

### 8.2 过热否决 `overheatVeto(metrics)`（Stage1 与 gate 共用同一 Bean）

RSI6 ≥ 90 / momentum_5d > +4.5% / < −1% / trend_strength > 0.46 → 拒（依据 4.77 万样本挖掘 `reports/research/mining_2026-09-15.md`：这三个区间是最差 T+5 组）。gate 侧仅在 composite_metrics 非空时执行（缺失跳过，不拒）。

### 8.3 十四条规则与拒因（枚举 `RejectReason`，`code()` 必须逐字节一致）

| # | 检查 | 参数 | code() |
|---|---|---|---|
| 1 | 市场门二道 | ret_5d<0 | `market_env_weak:ret_5d=%s<%s` |
| 2 | 解禁否决 | 7天内≥5%（比值>1.5自动按百分比归一） | `restricted_release_within_7d:%.1f%%` |
| 3 | PM 必须 entry_ok | — | `action=%s` |
| 4 | 可交易 | — | `not_tradable` |
| 5 | 置信度 | ≥70（校准开关默认关；开启时拒因尾部追加 `" (calibrated)"`） | `confidence=%s<%s` |
| 6 | 加权得分 | **停用**（阈值0，保留代码路径） | `weighted_score=%s<%s` |
| 7 | LLM 硬失败 | 深度遍历7个payload（含 reasoning JSON 字符串）找 fallback/llm_mode=llm_error 或 signal=llm_error | `llm_unavailable:<agent>`（agent 名去 `_agent` 后缀，如 `llm_unavailable:sentiment`） |
| 8 | 趋势/技术双条件 | trend up（signal=bullish 或 direction=up）**或** \|trend_strength\|>0.3 | `trend_not_up_and_tech_weak` |
| 9 | 技术置信度 | ≥50 | `technical_confidence=%s<50` |
| 10 | 过热否决 | §8.2 | `overheated_rsi6=%s>=90` / `overheated_momentum_5d=%s>4.5%%` / `underwater_momentum_5d=%s` / `overextended_trend_strength=%s` |
| 11 | 非趋势须有真趋势 | trend非up时 ADX≥25 | `flat_trend_adx=%s<25` |
| 12 | 偏多分析师≥3 | 分组计数（价格组cap 2），n/a 不算 | `bullish_agents=%d<3` |
| 13 | 价格组≥1 | technical/trend/sector 最多计2 | `price_group_bullish=%d<1` |
| 14 | 资金情绪组≥1 | main_force/dragon/sentiment/policy | `fund_sentiment_bullish=%d<1` |

其余拒因（+6）：`no_decision` / `prefetch:prices` / `prefetch:news_fetch` / `prefetch:failed` / `industry_cap:%s>%s` / `daily_cap:%s`。14 条规则派生 17 个模板 + 其余 6 个 = 合计 **23 个**。

> **code() 动态部分与格式化精度（契约测试须按此拼接）**：`market_env_weak:ret_5d=%.4f<%.4f`；`restricted_release_within_{days}d:{ratio×100:.1f}%`（`days` 来自 `CN_SCAN_RESTRICTED_RELEASE_DAYS` 默认 7）；`confidence={conf}<{min}`（int 拼接，开启校准时尾部追加 `" (calibrated)"`）；`weighted_score={score:.2f}<{min:.2f}`（仅阈值>0 时评估）；`technical_confidence={conf:.0f}<{min:.0f}`；`flat_trend_adx={adx:.1f}<{min:.0f}`；`bullish_agents={n}<{min}` / `price_group_bullish={n}<1` / `fund_sentiment_bullish={n}<1`（int）；过热四串 `overheated_rsi6={rsi:.0f}>={max:.0f}`、`overheated_momentum_5d={mom×100:.1f}%>{max×100:.1f}%`、`underwater_momentum_5d={mom×100:.1f}%<{min×100:.1f}%`、`overextended_trend_strength={ts:.2f}>{max:.2f}`；`industry_cap:{industry}>{cap}`、`daily_cap:{cap}`（`cap` 来自 `CN_SCAN_MAX_SIGNALS_PER_DAY` 默认 1）。过热否决命中即返回**单个**字符串（按 RSI6→momentum→trend_strength 顺序取第一个命中）。

---

## 9. 台账与复盘子系统

对齐 `tracking.py` 语义，全部落 `sniper_ledger_position` / `sniper_ledger_mark`：

1. **ingest（幂等）**：读最新报告的 entry_ok → `unique(cohort_date,ticker)` 冲突即跳过；**已有活跃（pending/open）仓位的票不重复入场**（`active_tickers` 检查）。
2. **fill（T+1）**：信号日后首个交易日开盘价成交（含费用）；**开盘较信号日收盘低开 ≥2%（gap ≤ −0.02）→ gap_abort**（closed + close_reason=gap_abort，市场否定信号；仅 signal_day_close 可得时判断，tracking.py:276）；**超过 10 个交易日（第 11 天）无 bar → never_filled**（tracking.py:392-399 条件为 `>10`，非 ≥10）。
3. **update_marks**：每日记 close/ret_from_entry/ret_from_t0（相对信号日收盘）/沪深300基准/超额；维护 max_gain/max_drawdown（全量重建，幂等）。
4. **close**：T+2 起每日 low ≤ entry×(1−3%) → 止损成交价=min(当日开盘,止损价)；或 T+5（按**信号日**起数，停牌顺延）收盘平仓；**止损优先于到期**（`_resolve_exit` 先把止损扫描完整个日期序列，全程未触发才看 horizon——非"先到先得"，backfill 场景可观察到差异，tracking.py:401-428）。止损启用晚于建仓的存量仓位用 `stop_from_date` 防回溯。
5. **review**：只统计真实成交（**never_filled 与 gap_abort 都不计入**，`review.py:_closed_trades`）；胜率/平均净收益/平均超额/平均持有天数/profit_factor；分桶：置信度（5桶：0-30/30-50/50-70/70-85/85+）×理由标签×快筛分（4桶：<40/40-50/50-60/60+）×市场环境（由 `market_ret_5d≥0` 派生 market_up/down，**不是存储字段**）。
   **运营红线：连续两周胜率 <40% 暂停实盘**（`每日操作指南.md` §六，现行为人工纪律）——Java 版把它做成 ReviewService 的自动告警输出，属**新增能力**，不改变统计口径。

---

## 10. 调度与运行时

| 触发 | 任务 | 幂等保障 |
|---|---|---|
| 交易日 15:05~15:15 | §4.4 数据更新序列 | `sniper_daily_run(run_date,'data_update')` |
| 交易日 15:35（现状：15:30 后手动跑，耗时约 10-15 分钟） | ScanPipeline（可手动 POST /api/scan?universe=..&tickers=..） | `sniper_daily_run(run_date,'scan')` + report unique(as_of,universe) 重跑覆盖 |
| 交易日 15:40（扫描后） | LedgerService ingest+fill+marks+close | position unique(cohort,ticker) + mark 全量重建 |
| 每周五 16:00 | ReviewService + 胜率红线告警（现状：`track_signals.py --review` 每周手动） | — |

手动复查已持仓票 = Python 版 `--tickers`：POST /api/scan 带 tickers 参数，跳过漏斗直接深度分析（**不 ingest 台账**，`confirm_only` 语义）。

报告产物：落库同时导出 `scan_<date>.json` + `scan_<date>.md`（与 Python `reports/` 目录同构），JSON 供程序对拍，MD 供人工阅读；台账导出 `ledger.md` / `track_<date>.*` / `review_<date>.md` 同理。

---

## 11. 配置映射（.env → Nacos yml / SniperProperties）

```yaml
# Nacos: nqboard-sniper-biz-dev.yml（共享配置在 application-dev.yml；密钥 jasypt 加密）
sniper:
  data:
    tushare-token: ${TUSHARE_TOKEN}
    tushare-first: true            # CN_TUSHARE_FIRST(.env=true)/CN_DATA_PROVIDER(.env=tushare)——主源开关,决定日预算是否计数与降级链起点
    tushare-delay-ms: 350          # TUSHARE_REQUEST_DELAY_SECONDS(.env=0.35)
    tushare-max-retries: 3
    em-circuit-breaker-seconds: 300
    daily-budget: 300              # CN_SCAN_MAX_AKSHARE_REQUESTS(仅东财主源计数)
    request-log: true              # Java 默认全量;Python CN_REQUEST_LOG 为 opt-in JSONL
  strategy:
    profile: short
    account-mode: flat
    horizon: 5                     # CN_SHORT_HORIZON
    lookback: 60
    min-listing-days: 60
    min-turnover: 100000000
    min-screen-score: 35
    max-screen: 50                 # CLI 默认 30;每日命令行传 50——Java 取 50 并可配
    max-deep: 8
    trial-cash: 100000             # CN_TRIAL_CASH
    flat-entry-threshold: 0.35
    flat-avoid-threshold: -0.20
  gate:
    min-confidence: 70
    min-bullish-agents: 3
    min-weighted-score: 0          # 负IC已停用
    min-tech-confidence: 50
    flat-trend-min-adx: 25
    max-rsi6: 90
    max-momentum-5d: 0.045
    min-momentum-5d: -0.01
    max-trend-strength: 0.46
    market-gate-enabled: true      # 代码默认false!.env为true——以env为准
    market-min-ret: 0.0
    restricted-release-enabled: true   # CN_SCAN_RESTRICTED_RELEASE_GATE
    restricted-release-days: 7
    restricted-release-min-ratio: 0.05
    max-per-industry: 1
    max-signals-per-day: 1
    calibrated-confidence: false   # CN_SCAN_CALIBRATED_CONFIDENCE,默认关;开启只改拒因后缀
  llm:
    dify-base-url: ${DIFY_BASE_URL}
    news-sentiment-api-key: ${DIFY_KEY_NEWS}
    policy-sentiment-api-key: ${DIFY_KEY_POLICY}
    flat-decision-api-key: ${DIFY_KEY_FLAT}
    timeout-seconds: 90
    max-retries: 3
    fail-hard: true                # CN_LLM_FAIL_HARD,永远true,不提供开关
  track:
    horizon: 5
    stop-pct: 0.03
    max-entry-gap-down: 0.02       # 代码默认0.02(.env中该键被注释,生效值即代码默认)
  fees: { commission-rate: 0.00025, stamp-tax: 0.0005, min-commission: 5.0 }
  annual-trading-days: 244
```

---

## 12. 对拍与验收

> **"逐位相等 / bit-exact"统一判定口径（2026-10-02 拍板，消除"数据回填 bit-exact"与"指标级容差 1e-9"的表述张力）**：
> ① **数据级（入库数据）**按**存储精度取整后逐字段相等**判定——价格 decimal(16,4)、因子 decimal(20,10)、金额 decimal(20,4)；Python(float64) 与 Java(BigDecimal) 计算引擎在存储精度以下的舍入差（相对偏差 <1e-9）视为一致，不做浮点逐位比对；取整后仍不等的票按 §4.5 退回逐票拉取。
> ② **计算级（指标/漏斗/gate）**维持 BigDecimal 比较容差 1e-9（相对偏差）；拒因字符串不受容差约束、必须字节级相等。
> 两级口径互不冲突：前者管"入库数据是否一致"，后者管"计算过程是否一致"。

| 层级 | 方法 | 通过标准 |
|---|---|---|
| 数据级 | 录制 Python 版 `CN_REQUEST_LOG`（设为 JSONL 路径）3 个真实交易日 → Java provider 以 fixtures 回放（WireMock） | 同输入 → 相同 endpoint 序列与 n_rows |
| 指标级 | 构造已知 K 线（含除权跳空、一字板、连板、单侧涨跌）断言 EMA/MACD/RSI(单侧零→极值)/ADX/涨停特征数值 | 逐位相等（BigDecimal 比较容差 1e-9） |
| 漏斗级 | 同一份 MySQL 数据跑 Python quick_screen 与 Java QuickScreen | Stage0/1 产出票集与 screen_score 完全一致 |
| gate 级 | 构造 20+ 组 analyst_signals fixture 表驱动断言 passed+拒因集合 | 与 Python gate 输出逐条一致（含 code() 字节级） |
| 报告级 | golden master：同输入下 Java 输出 diff Python `reports/scan_<date>.json`（除时间戳/耗时） | 零差异 |
| LLM 级 | WireMock 模拟 Dify 正常/超时/非法 JSON 三态 | fail-closed 行为正确（n/a→拒票；W3失败→降watch） |
| 影子运行 | 上线前 2~4 周 Java 与 Python 双跑 | 信号一致率 100%（LLM 步骤允许语义等价差异,但拒票集合必须一致） |

---

## 13. 分期实施计划

| 期 | 内容 | 验收 |
|---|---|---|
| M1（~1.5周） | DDL 全量建表 + Tushare/东财 Provider（按 §4.3 逐表入库规格：端点/字段映射/单位换算/兜底链）+ 网关 + 回填脚本 + 日更任务 | 回填完成；§12 数据级对拍过 |
| M2（~1周） | 指标库（纯函数，含两套特征字典导出）+ TechnicalFeatureCalculator + Stage0/1 | 漏斗级对拍过；指标级单测绿 |
| M3（~1周） | Gate 14条 + RejectReason 枚举（23 拒因）+ ScanReport 落库/渲染 | gate 级表驱动测试绿 |
| M4（~1.5周） | 7 分析师（规则）+ risk + PM 规则版 + 编排 | 报告级 golden master（无 LLM 模式）零差异 |
| M5（~1周） | Dify 三工作流搭建 + DifyClient + 失败语义 + 审计表 | LLM 级三态测试绿；W1/W2/W3 输出与 Python 版同输入同构 |
| M6（~1周） | 台账 + 复盘（含红线告警） + 调度 + 手动端点 | 台账幂等验证；影子运行启动 |
| M7 | 影子运行 2~4 周 → 切换 | 一致率 100% 后 Python 版下线为 oracle 保留 |

> 待拍板项：① weighted_score 负 IC（−0.058）——**现状已停用**（`CN_SCAN_MIN_WEIGHTED_SCORE=0`，gate 代码路径保留），迁移照搬停用态即可；"分组共识"决策属可选优化，若做则 M4 时定并在影子运行对比两版差异清单。② 快照无法回填导致历史日期 Stage0 失真——影响对拍样例选择（选近期日期对拍）。③ **连板回溯窗口源码自身不一致**（2026-10-03 核对发现）：`features/technical.py` 的 `consecutive_limit_up` 用 5 日回溯（max_lookback=5），`features/short_structure.py` 用 6 日——Java 唯一指标库只能取其一，**S2 指标库动工前拍板**（建议取 5，与 Stage1 快筛/gate 过热否决的主链路一致；short_structure 仅 trend_predictor 消费，影响面小，偏差须记入对拍差异清单）。

---

## 附录 A：与当前代码核对的差异记录（2026-10-01）

相对上一版方案的修正（均已回填正文）：

1. **路径**：`tools/api.py` → `src/tools/api.py`（§4.1）；`security_status.py:60` → 实为 `security_status.py:59` 的停牌检查用 `adjust="none"`，正式涨跌停价在 `trading_rules.calc_limit_prices`（除权日用交易所 reference_close）（§D3）。
2. **hfq 口径**：`CN_PRICE_ADJUST=hfq` 并非"配置了但零消费"——它是 provider 层默认参数（`akshare_client.py:412`），只是短线调用方从不触发；措辞已收敛为"迁移范围内零消费"（§D3）。
3. **快照兜底链**：原文"东财 clist 无兜底"与现状不符——Python 有 东财→akshare新浪→新浪直连→腾讯批量 4 级链，Java 版照搬（§4.2）。
4. **DDL 修正**：sniper_scan_signal 补 `reasoning`/`prefetch_warnings`；sniper_scan_report 补 `mode`/`prefetch_ok`/`report_json`；sniper_ledger_position 按真实 `TrackedPosition` 重写——status 只有三态、gap_abort/never_filled 属 close_reason、`market_env` 枚举改为存 `market_ret_5d` 派生、补 signal_day_close/stop_from_date/exit_proceeds/pnl/bench_ret_full/trial_cash_used/weighted_score（§3.3）。
5. **W1 契约**：输入是编号文本行（非 JSON 数组），prompt 换为 Python 原文；缺失条目回填 neutral 而非整体 parse_error（§6.2）。
6. **W3 契约**：`rule_suggestion` 是完整决策对象，`tradable`/`block_reason` 为顶层字段（§6.4）。
7. **拒因总数** 21 → **23**（含 confidence 校准后缀说明）（§8.3）。
8. **max_screen**：代码默认 30、无 env 变量，50 来自每日命令行；Java 配置默认 50 并注明（§5.3、§11）。
9. **复盘红线**：代码中不存在，是人工纪律；标注为 Java 新增告警，统计口径写明 never_filled/gap_abort 不计入（§9）。
10. **market.db**：明确为无引用孤儿产物，SQLite 方案停做、由本方案 MySQL 取代（§D2）。
11. **不迁移清单**补回测/ML 管线（`src/backtesting/`、`src/backtester.py`）、`--intraday`、未消费的 api 面（margin/limit_pool/auction_snapshot 等）；北向仅保留预检告警。
12. **特征字典**：Stage1 features 与 Stage2/3 composite_metrics 是两套键集合（adx/bollinger_z 只在后者），Java 需双视图（§7.2）。
13. **落位修正（2026-10-01 晚）**：实地核实 nqboard 仓库后，§1 模块划分由虚构的 domain/data/llm/pipeline/app 五模块改为真实 **nqboard-sniper** 模块（api/biz 双子模块、端口 6008、com.mx.nqboard.sniper 包结构）；技术栈 Java 21→**17**（无虚拟线程，编排改 CompletableFuture+线程池）、MyBatis→**MyBatis-Plus 3.5.16**、@Scheduled→**nqboard-visual-quartz 管理台**、配置 application.yml→**Nacos+jasypt**、Resilience4j→**Sentinel+自研**、缓存→**RedisUtils**；DDL 表名前缀 t_→**sniper_**、业务表继承 BaseEntity/行情大表豁免；调度时间/流程/阈值/DDL 列不变。结构细节以 [`重构.md`](重构.md) §9.2 为准。
14. **DDL 全面 nqboard 化（2026-10-01 晚，第二轮）**：全文表名 `t_` → **`sniper_`**；§3 DDL 按房规重写（反引号/`ENGINE=InnoDB utf8mb4_general_ci`/`ROW_FORMAT=Dynamic`）：业务表改"雪花 id + BaseEntity 审计五列 + UK 幂等"，行情/事件表豁免审计列改带 `fetched_at`；§7 配置根键 `nqboard:` → `sniper:`（Nacos `nqboard-sniper-biz-dev.yml`）。DDL 可直接落 `db/nqboard_sniper.sql`。
15. **对照 Python 源码与 nqboard 开发手册的全面复核（2026-10-02，第三轮）**：① §8.3"其余拒因（+9）"为笔误实为 **+6**（14 条规则派生 17 个模板 + 6 = 23），并补拒因动态模板与 printf 精度契约注（`daily_cap:{cap}`、`restricted_release_within_{days}d` 等）；② §7.3 dragon_tiger 补全源码 6 分支（bullish 75 档、bearish 78 档、neutral 50/45 档）；③ 不迁移清单"v2/"更正为实际路径 `src/backtesting/` + `src/backtester.py`，并注明 `src/prediction/` 的 features/rule_ensemble/calibration 在迁移范围内（quick_screen/trend_predictor/gate 消费）；④ §5.3 RSI 语义精确化（全平才返回 50）+ 快筛"市场环境"补阈值 −2%/−4%；§5.2 规则 6/7 注明缺失放行；⑤ §5.1 补代码默认 `min_ret=-0.03`（.env 覆盖为 0.0）；⑥ §7.1 "编排（虚拟线程）"标题更正为 CompletableFuture+线程池，线程池规模说明改"下限 6、跨票并行 48"；⑦ §11 env 名修正 `TUSHARE_REQUEST_DELAY_SECONDS`，补 `CN_TUSHARE_FIRST`/`CN_DATA_PROVIDER` 映射，`max-entry-gap-down` 注明 .env 注释态生效值为代码默认；⑧ §1.4.4 与各模块文档补**接口路径三层约定**（平台网关全局 StripPrefix=1、Controller 不带 /sniper 前缀、Nacos 路由样式）；⑨ §1.1 显式声明 algorithm/data/llm/pipeline 为有意扩展包、CRUD 仍走标准 Service/ServiceImpl/Mapper；⑩ §4.1 DataGateway 接口签名补 `getNorthboundHoldings`（与 01 文档对齐，返回类型具体化）；⑪ 全部 DDL 表尾统一为 device.sql 原样式（等号带空格），§3 头注补 `db/nqboard_sniper.sql` 文件头（CREATE DATABASE/DROP TABLE IF EXISTS）约定；⑫ 分页补延迟关联范式、Controller 补 knife4j/@SysLog 注解要求（README 全局约定同步）。
16. **DDL 全表统一 BaseEntity 六件套（2026-10-02，第四轮）**：用户拍板取消行情/事件表的审计列豁免——13 张行情/事件表与 `sniper_request_log` 全部补 `id` 雪花主键 + `create_by/create_time/update_by/update_time/del_flag`，原复合自然键转 UNIQUE KEY（upsert 幂等语义不变，§4.3.0 规则 2 同步改为"UK 即防重"，D2 的 SQLite 兼容措辞同步改 UK 口径）；行情/事件表批量路径的审计列由代码显式赋值、`del_flag` 恒 '0' 仅作规范留位（查询不做逻辑删除过滤）；01/05 文档 DDL 与说明、README 骨架条目同步。
17. **文档集自包含化与精度口径统一（2026-10-02，第五轮）**：① `重构.md` / `每日操作指南.md` 随本档一并落位迁移目标仓库（`nqboard-sniper-biz/src/main/resources/doc/`），全部 `docs/重构.md`、`docs/每日操作指南.md` 悬空引用改为同目录相对链接，并新增"文档集导航"头注——开发只读该目录一套文档即可，无需回查 ai-hedge-fund 仓库；② §12 新增"逐位相等 / bit-exact 统一判定口径"头注：数据级按存储精度（价格 1e-4 / 因子 1e-10）取整后相等，计算级维持容差 1e-9（相对），拒因字节级——§4.3.4/§4.5 回填条款同步引用该口径；③ 模块文档：README 头注补配套文档链接与对拍口径指引，01 文档 §3.5 快照 clist URL 补全 `po/np/fltt/invt/fid` 参数（对齐 §4.3.5）、§4.4/§7 的 bit-exact 措辞对齐统一口径，04 文档红线出处改相对链接。
18. **五路源码逐条核对后的勘误回填（2026-10-03，第六轮）**：对 重构.md 第 2~7 章与本方案全部规格做了五路并行源码核对（数据层/过滤算法/Agent+LLM/Gate+台账/配置项，逐条对照 ai-hedge-fund 源码）。结论：公式/阈值/规则/端点链全部一致。回填的修订：① 台账两处行为口径——never_filled 触发为等待 **>10** 个交易日（tracking.py:392-399）、平仓为**止损全程优先**而非先到先得（tracking.py:401-428），§9 与 03 模块文档同步改；② §3.5 补报告重跑的子表（signal/rejected 无 UK）先删后插语义；③ §4.2 补日预算两处口径（快照无条件消耗、`truncated_at_stage` 取值）与行情反向兜底；④ §4.3.0 补规则 7（source 换算封装在数据层）；⑤ §13 补待拍板项③（连板回溯窗口 technical.py=5 vs short_structure.py=6）；⑥ 重构.md 侧修订：P-01 表 `get_margin_detail` 重复定义 4 次（非 5 次）、§5.1 `industry_cap` 拒因补 `>{cap}` 后缀、§2.4.7 北向 composite 强制 AkShare-only、§4.4.2 补一字涨停检查在 PM 决策路径不触发的警示、§6.1 台账两处同①、§7.2 `CN_SCAN_MIN_LISTING_DAYS` 标注"未设走默认"、P-04 补连板窗口不一致；⑦ 新增附录 B（边界语义补遗）收录核对发现的 30+ 条边界行为，供实现对拍时逐条遵守。

---

## 附录 B：边界语义补遗（2026-10-03 五路源码核对）

> 这些行为在 Python 源码中真实存在、但正文规格未覆盖或仅隐含。主流程不受影响，**但 golden master 对拍时 Java 侧若按"常识"而非本表实现，会在边缘样本上产生假差异**。按模块分组，实现时逐条遵守；凡标"源码自身不一致"的，以 §13 待拍板项决议为准。

### B.1 数据获取层

1. 全市场快照请求**无条件消耗 1 次日预算**（`universe.py:113`，不判 tushare 主源模式）。
2. 预算耗尽时除 `stage1_skipped_due_to_budget` 外还写 `truncated_at_stage`，取值 `"stage1"` / `"stage1_market_env"`（`quick_screen.py:49,69`）。
3. 行情反向兜底：非 Tushare 优先模式下 AkShare 行情为空按 `CN_PRICE_FALLBACK`（默认 tushare）回退（`composite.py:105-134`）。
4. 腾讯批量快照兜底依赖 Tushare `stock_basic` 代码表，**无 token 时该兜底级实际为空**（`universe.py:82-93`）。
5. Python 请求日志记录无 `summary` 键，摘要字段（n_rows/columns/date_min/date_max 等）平铺进顶层；`error`/`elapsed_ms` 仅非 None 时出现（`request_log.py:130-137`）。
6. 交易日历磁盘缓存（`data/cn_trade_calendar.json`）独立于 daily_cache 体系，无日期戳、永久有效直到手动删（`calendar.py:17`）。
7. 北向：`composite` 强制 AkShare-only（`composite.py:199-202`）；`tushare_client.get_northbound_holdings` 存在但不在链路中。

### B.2 过滤算法（Stage 0/1/特征）

8. Stage 0 各过滤对缺失值 **fail-open**：turnover/price/change_pct 为 None 时不剔除（`universe.py:328,333,349,359`）。
9. 次新过滤锚定 `date.today()` 而非 as_of（`universe.py:293-294`）——盘中扫描正确，历史日期回扫会误杀/误放（与 P-06 同源）。
10. 板块限值细节：创业板仅认 300 前缀，301 等落入 MAIN_SZ 用 9.8% 限值（`ticker.py:49`）；未知板块默认 9.8。
11. 快筛"市场环境"分：`market_ret_5d` 全批次只取一次，**取数失败/预算耗尽按 0.0 计**，此时 0 > −2% 照样得 +10（`quick_screen.py:46-51,195`）。
12. 快筛 RSI 区间 70<r<75 **不得分**（评分表无该档）。
13. 快筛"MACD 金叉"实现是 **hist 柱线由 ≤0 翻正**，不是 DIF 上穿 DEA（`technical.py:86`）——Java 实现勿按金叉常识写。
14. 特征 <10 根时返回值**非全 0**：rsi_6/rsi_14=50、ma_ratio=1.0、volume_ratio=1.0，并带 `_gaps=["insufficient_price_history"]`（`technical.py:28-41`）。
15. MACD 数据 <26 根时 ema26 退化为 ema12，dif≈0（`technical.py:80`）。
16. short_structure 四处边界：① 封板强度 ×0.15 **仅在当日封板时计入**，未封板该项为 0（`short_structure.py:108`）；② 高开子分实际为 `clip(gap_open_pct×15,-1,1)`，有 ×15 缩放（:105）；③ "换手"实为量比代理 `turnover_ratio_5d=当日量/5日均量`（:74-78）；④ "未封板但有连板记忆 0.2"分支**实际不可达**（连板计数从最新 K 线起数，今日未封板则 count=0，:187-204）。
17. **连板回溯窗口源码自身不一致**：technical.py=5 日 / short_structure.py=6 日 → §13 待拍板项③。
18. Stage 0 规则 9（近涨停剔除）代码注释称"仅作一字板检测字段缺失时的兜底"，但实际在 change_pct 非空时**无条件执行**（`universe.py:357-361`）——以行为为准。
19. Stage 1 单票异常被静默吞掉继续下一票（`quick_screen.py:209-211`）；`ScreenCandidate.name` 由 quick_screen 回填而非 score_ticker（:180,214）。

### B.3 Gate 与门禁

20. 规则 9（技术置信度）：tech_conf 为 None（无 technical payload）时**跳过不拒**（`gate.py:482`）。
21. 规则 11（flat-trend ADX）嵌在 `if metrics:` 内，无 composite_metrics 时不评估（`gate.py:486`）。
22. 规则 8 的技术强度判断**先查 `prediction.trend_strength`、再查 `composite_metrics.trend_strength`**，且该路径不解析字符串形式的 reasoning（要求 dict，`gate.py:553-567`）——与 `_technical_metrics`（会解析 JSON 字符串）行为不同，Java 版照此保留两条路径。
23. `analyst_summary` 的 key 去掉 `_agent` 后缀、reasoning 截断 2000 字符（`gate.py:409-412`）。
24. 偏多理由标签实际有**第 8 个"北向流入"**（`gate.py:371`，northbound_flow_agent）——short 链路不产生，但复盘分桶按 reasons 字符串动态统计，字典/前端勿写死 7 个白名单。
25. 校准开启时分桶样本 n<5 用内置先验 fallback（`calibration.py:28-34,67-71`）；拒因尾部追加 `" (calibrated)"`（§8.3 已收录）。
26. run_flat_short 路径下市场门第二道检查发生在 **prefetch 之后、agent 运行之前**（`run_flat_short.py:162-198`），CLI 入口的第一道在漏斗之前——两道共用 `market_env_blocks_entry`，Java 版 ScanPipeline 只需在 Stage0 前执行一次（入口短路已覆盖）。
27. `apply_output_gate` 还有 `min_weighted_score`、`release_blocks` 两个关键字参数（`gate.py:599-601`）；`release_blocks` 由主流程解禁检查预先算好传入，Java 版保留该结构避免重复查询。

### B.4 Agent 与 LLM

28. `SHORT_TERM_ANALYST_KEYS` 实际 **7 项**（含 `trend_predictor`，`utils/analysts.py:19-27`）——"六家并行"是 `main.py` 构图时把 trend_predictor 拆出来实现的；`AgentState.metadata` 还有 `trial_cash` 键（`main.py:89`）。
29. `decision_mode=llm` **只要传了 `--model` 即置位**，不要求命中 api_models.json；未命中时 provider 静默兜底 "OpenAI"（`scan_buy_signals.py:50-53`、`cli/input.py:361-363`）——Java 版决策：Dify 是唯一 LLM 路径，该兜底不迁移，但复盘 Python 行为时须知悉。
30. Zhipu 分支是 `ChatOpenAI(model=model_name, …)`，`glm-5.3` 来自 api_models.json 目录项而非硬编码（`llm/models.py:227-235`）。
31. LLM 调用量 `≤17 次/扫描`是**名义上限**（批量全部成功时）；sentiment 批量失败退化逐条重试，单票最多 6 次（1 批量 + 5 逐条，`sentiment.py:130-141`），Dify 侧限流/成本预算按此上限另留余量。
32. sentiment 批量 LLM 缺失条目回填 neutral、index 越界丢弃（§6.2 已收录）；policy LLM 返回 None（非异常）回落关键词规则、`LLMCallError` 才标 n/a（重构.md §4.2⑥ 已收录）——两条路径别混淆。
33. portfolio（非 flat）模式另有 rules 版阈值 BUY≥0.25/SELL≤−0.25、conf=min(95,50+|score|×50)（`decision.py:45-46,175-198`）——不在迁移范围，仅备查。

### B.5 台账与复盘

34. gap_abort 仅在 `signal_day_close` 可得时判断（`tracking.py:276`），缺信号日收盘时直接按开盘价成交。
35. 止损触发价：成交价 = `min(当日开盘, 止损价)`（跳空低开穿止损按开盘价成交，`tracking.py:417-418`）；`stop_from_date` 对止损启用日前入场的存量仓位设为启用当天，防回溯（:188-193,406-411）。
36. ingest 三重幂等：`ingested_reports` 来源记录 + 同 cohort 同票跳过 + **已有活跃（pending/open）仓位的票不重复入场**（`tracking.py:213-228`）。

> 勘误与核对过程记录：本次核对以五路并行方式逐条对照源码（2026-10-03），全部结论已回填本文与 重构.md；后续若再发现文档与源码偏差，按同样方式回填附录 A 条目并保持两仓库副本同步。

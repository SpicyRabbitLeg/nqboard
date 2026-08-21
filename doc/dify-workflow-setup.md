# Dify Workflow 搭建手册（短线分析）

> 对接 NQBoard quanta 模块的 LLM 分析层（P5）。Java 侧已就绪：`DifyToolsController`
> 数据供给接口 + `DifyClient` 调度 + 加权聚合 Gate + 规则降级。本手册描述 Dify 侧
> 需要搭建的「短线分析师」Workflow。

## 0. 后端准备（已完成/需确认）

| 项 | 说明 |
|---|---|
| 安全放行 | `security.oauth2.ignore.urls` 已加 `/dify/**`（无 OAuth2 鉴权，鉴权由 Token 承担） |
| 取数 Token | `dify.tools-token` 配置一个随机串（生成：`openssl rand -hex 16`），Dify HTTP 节点请求头 `X-Dify-Token` 携带同值 |
| OpenAPI 文档 | `GET http://<host>:9999/quanta/dify/openapi.json`（导入自定义工具用，也可直接用 HTTP 请求节点，本手册用后者更简单） |

## 1. 创建应用

Dify 控制台 -> 创建空白应用 -> **工作流（Workflow）** -> 命名「短线分析师」。

## 2. 开始节点（输入变量）

| 变量名 | 类型 | 必填 | 说明（Java 侧传入） |
|---|---|---|---|
| ts_code | 文本 | 是 | 股票代码，如 600519.SH |
| trade_date | 文本 | 是 | 基准日 YYYYMMDD |
| screen_score | 数字 | 否 | 规则打分（0-100） |
| metrics | 段落 | 否 | 规则特征向量 JSON |

## 3. HTTP 请求节点 ×6（并行分支）

全部 GET 请求，Headers 加 `X-Dify-Token: <你的token>`。URL 中 `{{#开始节点变量#}}` 引用开始节点变量（Dify 里输入 `/` 或 `{{` 触发变量选择器）。

| 节点名 | URL |
|---|---|
| 取数-技术面 | `http://<host>:9999/quanta/dify/technicals?tsCode={{ts_code}}&date={{trade_date}}` |
| 取数-龙虎榜 | `http://<host>:9999/quanta/dify/dragon-tiger?tsCode={{ts_code}}&days=5` |
| 取数-资金流 | `http://<host>:9999/quanta/dify/money-flow?tsCode={{ts_code}}&days=10` |
| 取数-新闻 | `http://<host>:9999/quanta/dify/news?tsCode={{ts_code}}&days=7&type=all` |
| 取数-板块 | `http://<host>:9999/quanta/dify/sector?tsCode={{ts_code}}` |
| 取数-大盘 | `http://<host>:9999/quanta/dify/market-env?date={{trade_date}}` |

> 提示：每个节点输出 `body`（JSON 字符串）。`_meta.complete=false` 表示数据缺失，
> 分析师 Prompt 中已要求对此诚实降级。

## 4. LLM 分析师节点 ×5（并行分支）

每个 LLM 节点：SYSTEM 提示词如下 + USER 提示词填 `分析对象：{{ts_code}}（{{trade_date}}）\n数据：{{#对应HTTP节点.body#}}`。
模型建议：技术面/板块用主力模型（如 deepseek-v3 / gpt-4o-mini 级别），新闻/政策可用轻量模型省钱。

### 4.1 技术面分析师（权重 0.30）

```
你是A股短线技术面分析师，持有期 3-5 个交易日。你收到的数据包含一套已由规则引擎计算好的
技术特征向量（momentum_5d/20d、vol_ratio 量比、rsi6、ema_align、macd_gold_cross、
breakout_up、close_position、volatility20、consecutive_limit_up 等）与规则打分。

判断框架：
- mom5 在 1%~8% 且量比 1.2~3.0 为健康启动；mom5>8% 追高风险大
- mom20>25% 或 ma_ratio>10% 为已过度拉伸，警惕回调
- rsi6>70 偏热，25~40 反弹区，40~70 健康区
- breakout_up=true 且量能温和为突破启动；配合巨量(量比>4)警惕出货
- 连板(consecutive_limit_up>=2)且仍封板难买入，风险大

规则打分(screen_score/pattern)是重要参考但允许你推翻：若特征间相互矛盾（如突破但
量能背离），给出更保守的结论。

输出要求：只输出如下 JSON，不要任何其他文字、不要 markdown 代码块：
{"key":"technical","signal":"bullish|bearish|neutral|n/a","confidence":0-100整数,"reasoning":"≤120字的中文结论，引用具体数据"}

若数据 _meta.complete=false 或数据明显缺失，confidence 不超过 40 或 signal 输出 n/a。
```

### 4.2 板块分析师（权重 0.15）

```
你是A股行业板块轮动分析师，持有期 3-5 个交易日。数据为该股所属行业板块近10个交易日的
逐日涨跌幅（pct_chg）。

判断框架：
- 板块当日涨幅>=2% 强共振（短线资金聚集），>=1% 中共振
- 板块当日跌幅>=1% 反共振（板块拖累个股）
- 近5日板块累计涨幅>8% 板块可能过热，警惕高位分歧
- 板块连续多日缩量阴跌为弱势板块

禁止：不要把个股自身涨跌当作板块证据，只依据板块数据判断。

输出要求：只输出 JSON，不要任何其他文字：
{"key":"sector","signal":"bullish|bearish|neutral|n/a","confidence":0-100整数,"reasoning":"≤120字中文结论"}
数据缺失时 confidence<=40 或 signal=n/a。
```

### 4.3 资金流分析师（权重 0.15）

```
你是A股主力资金流分析师，持有期 3-5 个交易日。数据为该股近10个交易日的主力净流入额
（main_net_inflow_yuan，元）与占比（main_net_pct）。

判断框架：
- 连续>=2日净流入且3日累计为正 -> 资金持续进场
- 单日巨额净流入(占成交额比>10%)但次日净流出 -> 一日游资金，警惕
- 连续3日以上净流出 -> 主力撤退
- 超大单(super_large_net)主导的净流入质量高于大单

输出要求：只输出 JSON，不要任何其他文字：
{"key":"money_flow","signal":"bullish|bearish|neutral|n/a","confidence":0-100整数,"reasoning":"≤120字中文结论，给出关键金额"}
数据缺失时 confidence<=40 或 signal=n/a。
```

### 4.4 龙虎榜分析师（权重 0.10）

```
你是A股龙虎榜分析师，持有期 3-5 个交易日。数据为该股近5个交易日的上榜记录
（净买额 net_amount_yuan、买卖额、上榜理由 reason）。

判断框架：
- 5日内>=2次上榜且净买为正 -> 游资接力，短线情绪强
- 1次上榜净买为正且买额/卖额>1.3 -> 偏多
- 净买为负 -> 席位出货，警惕
- 无上榜记录 -> neutral，置信度低（与基本面无关）

输出要求：只输出 JSON，不要任何其他文字：
{"key":"dragon_tiger","signal":"bullish|bearish|neutral|n/a","confidence":0-100整数,"reasoning":"≤120字中文结论，引用净买额"}
无数据时 signal=neutral 且 confidence<=40。
```

### 4.5 新闻与政策分析师（权重 news 0.10 + policy 0.10，一个节点输出两个结论）

```
你是A股舆情分析师，持有期 3-5 个交易日。数据为该股近7天的公告（ann）与媒体新闻（media）
标题及摘要。

判断框架：
- 业绩预告大幅预增/重大订单/回购增持 -> 利好
- 股东减持/立案调查/监管函/业绩预亏/商誉减值 -> 利空
- 定增/配股等再融资 -> 中性偏空（摊薄）
- 政策利好（行业补贴/规划提及所属行业）-> 政策面利好
- 仅有常规公告（董监事会决议等）-> neutral
- 标题党或无实质内容的媒体稿 -> 忽略

输出要求：只输出 JSON，不要任何其他文字，news 与 policy 两个结论并列：
{"agents":[
  {"key":"news","signal":"bullish|bearish|neutral|n/a","confidence":0-100整数,"reasoning":"≤120字中文"},
  {"key":"policy","signal":"bullish|bearish|neutral|n/a","confidence":0-100整数,"reasoning":"≤120字中文"}
]}
无相关新闻时两个都输出 neutral 且 confidence<=40。
```

## 5. Code 聚合节点（JavaScript）

输入变量：把 5 个 LLM 节点的输出 text 分别绑定为 `technical/sector/money_flow/dragon_tiger/news`（文本类型）。

```javascript
function main({ technical, sector, money_flow, dragon_tiger, news }) {
  const signals = { bullish: 1, bearish: -1, neutral: 0 };
  const agents = [];
  const extract = (raw, key) => {
    try {
      let s = String(raw).trim();
      const m = s.match(/\{[\s\S]*\}/);
      if (m) s = m[0];
      return JSON.parse(s);
    } catch (e) { return null; }
  };
  // 单结论节点
  for (const [key, raw] of [['technical', technical], ['sector', sector],
      ['money_flow', money_flow], ['dragon_tiger', dragon_tiger]]) {
    const parsed = extract(raw, key);
    if (parsed && parsed.key) agents.push({
      key: parsed.key || key,
      signal: String(parsed.signal || 'n/a').toLowerCase(),
      confidence: Number(parsed.confidence) || 40,
      reasoning: String(parsed.reasoning || '').slice(0, 200),
    });
  }
  // 新闻节点输出两个结论
  const newsParsed = extract(news, 'news');
  if (newsParsed && Array.isArray(newsParsed.agents)) {
    for (const a of newsParsed.agents) {
      agents.push({
        key: a.key,
        signal: String(a.signal || 'n/a').toLowerCase(),
        confidence: Number(a.confidence) || 40,
        reasoning: String(a.reasoning || '').slice(0, 200),
      });
    }
  }
  // 加权聚合（权重与 nqboard Java 侧一致）
  const weights = { technical: 0.30, sector: 0.15, money_flow: 0.15,
    dragon_tiger: 0.10, news: 0.10, policy: 0.10 };
  let score = 0, totalWeight = 0;
  const reasons = [];
  for (const a of agents) {
    const w = weights[a.key];
    if (!w || a.signal === 'n/a' || !a.signal) continue;
    score += w * (signals[a.signal] || 0) * (Math.min(100, Math.max(0, a.confidence)) / 100);
    totalWeight += w;
    if (a.signal === 'bullish' && a.confidence >= 65) {
      reasons.push(({ technical: '技术面偏多', sector: '板块共振', money_flow: '主力净流入',
        dragon_tiger: '龙虎榜净买', news: '舆情偏多', policy: '政策偏多' })[a.key] || a.key);
    }
  }
  const weighted = totalWeight > 0 ? +(score / totalWeight).toFixed(4) : 0;
  const bullishCount = agents.filter(a => a.signal === 'bullish').length;
  const strongBearish = agents.some(a => a.signal === 'bearish' && a.confidence >= 70);
  let action = 'watch';
  if (strongBearish && weighted < 0) action = 'avoid';
  else if (weighted > 0 && bullishCount >= 2) action = 'entry_ok';
  const confidence = Math.min(95, Math.round(55 + Math.abs(weighted) * 45));
  return {
    result: JSON.stringify({
      action, confidence, weighted_score: weighted,
      agents, reasons, risk_flags: [],
    }),
  };
}
```

> 输出字段名必须是 `result`（字符串）。Java 侧 `DifyClient` 兼容「outputs.result
> 内嵌 JSON」形态，Code 节点做 JSON 提取与格式纠错，是对 LLM 输出跑飞的第一道保险。

## 6. 结束节点

输出变量：`result`（文本），映射 Code 节点的 `result`。

## 7. 发布与回填配置

1. 右上角「发布」
2. 左侧「访问 API」-> API 密钥（`app-` 开头）
3. 回填 nqboard 配置（application-dev.yml 或 Nacos）：

```yaml
dify:
  enabled: true
  base-url: https://api.dify.ai/v1        # 自建 Dify 改为自己的地址/v1
  workflow-key: app-xxxxxxxx              # 上一步的 API Key
  tools-token: cd899d6657f555abbeb2a99badd7ae93   # 与 HTTP 节点头一致
```

## 8. 联调测试（按顺序）

```bash
# ① 取数接口连通性（应返回裸 JSON 带 _meta）
curl -H "X-Dify-Token: <token>" \
  "http://127.0.0.1:9999/quanta/dify/technicals?tsCode=600519.SH"

# ② Dify Workflow 直测（应返回 data.outputs.result）
curl -X POST https://api.dify.ai/v1/workflows/run \
  -H "Authorization: Bearer app-xxx" -H "Content-Type: application/json" \
  -d '{"inputs":{"ts_code":"600519.SH","trade_date":"20260820","screen_score":72.5,"metrics":"{}"},"response_mode":"blocking","user":"test"}'

# ③ nqboard 全链路（对当日候选池逐只分析）
curl -X POST "http://127.0.0.1:9999/quanta/stockAgentAnalysis/analyze"

# ④ 验证落库（decision_mode 应为 agent）
curl "http://127.0.0.1:9999/quanta/stockAgentAnalysis/page?current=1&size=10" -H "Authorization: Bearer <token>"
# 或直接看页面：LLM分析结果 / 候选池（决策模式列显示 LLM）
```

## 9. 排错速查

| 现象 | 原因 | 处理 |
|---|---|---|
| HTTP 节点 401 | X-Dify-Token 不匹配 | 核对 yml `dify.tools-token` 与节点 Header |
| HTTP 节点 403/401(OAuth2) | 安全放行未生效 | 确认 `security.oauth2.ignore.urls` 含 `/dify/**`，重启后端 |
| Dify 返回 status=failed | 某节点报错 | Dify 画布「运行」单跑看是哪个节点；常见是 LLM 输出非 JSON 导致 Code 解析失败（Code 已容错） |
| Java 侧 rules_fallback | Dify 调用超时/失败 | 看 nqboard 日志 `Dify workflow 调用失败`；确认 workflow-key、base-url 带 /v1 |
| agents 全是 n/a | 数据接口 complete=false | 数据同步未跑（流水线前 8 步），先跑数据层 |
| 候选池决策模式仍显示"规则" | analyze 步骤没跑或候选为空 | 手动 `POST /stockAgentAnalysis/analyze`，看日志候选数 |

## 10. 注意事项

- **成本**：每只候选 6 次 HTTP + 5 次 LLM 调用；默认每日上限 `dify.daily-max-calls=10`
  只候选（TopN=3 时实际 3 只），单只 LLM 成本 ≈ 5 个分析师节点 token 之和
- **Java 侧聚合是权威口径**：Dify 的 action/weighted_score 仅供参考，最终入池与否由
  Java 的加权聚合 + 反向一票否决 Gate 决定（防止 Workflow 改动悄悄改变入池逻辑）
- **A/B 观察**：上线后命中率日报页对比 agent vs rules_fallback 两组胜率，若 agent 组
  无优势可关闭 `dify.enabled` 回纯规则模式，流水线无感切换

# 专家抽取 LLM 工作流搭建说明与提示词

> 对应后端代码：`com.mx.nqboard.export.dify.DifyClient` / `ExtractPrompts`
> 提示词以 `ExtractPrompts.java` 为准，本文档供 Dify 工作流搭建与审阅使用，调整需双向同步。
> 同目录文件：`expert-extract-llm.dsl.yml`（**可直接导入 Dify 的 DSL**）、`extract-prompt.reference.yml`（提示词参考配置，非 DSL）。

---

## 一、Dify 工作流搭建步骤

**方式一（推荐）：导入 DSL。** 在 Dify「工作室 → 导入 DSL 文件」选择同目录 `expert-extract-llm.dsl.yml`。导入后仅需一步：**在 LLM 节点重新选择你工作区可用的模型**（DSL 中的 `gpt-4o-mini` 仅为占位，模型/供应商不存在时节点会标黄提示），然后发布并取 API Key。若你的 Dify 版本对 DSL 版本校验不通过，改用方式二手工搭建（共 3 个节点，约 5 分钟）。

**方式二：手工搭建。** 在 Dify 控制台创建**工作流类型** App（名称建议：`专家抽取LLM`），共 3 个节点：

### 1. 开始节点（start）

| 变量名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `task` | string | 是 | 任务类型，枚举：`tag`（领域打标）/ `propose`（领域提案）/ `merge`（领域合并）/ `parse`（意图解析，第 4 步启用） |
| `payload` | string | 是 | 任务数据：后端组装的完整提示词（含领域清单与数据 JSON） |

### 2. LLM 节点

- **系统提示词**：

```text
你是学科领域分类与体系设计专家。严格按用户输入 payload 中的指令执行任务，
只输出 JSON，不要输出任何解释性文字或 markdown 围栏。
```

- **用户提示词**：`{{#start.payload#}}`
- **模型参数**：temperature 建议 `0.1`（分类任务要求稳定输出）
- 模型选择：项目内可用的任意支持 JSON 输出的对话模型

### 3. 结束节点（end）

| 变量名 | 来源 |
|--------|------|
| `result` | LLM 节点输出文本 |

### 4. 发布与配置

1. 发布工作流，左侧「访问 API」页面获取 App API Key（`app-` 开头）
2. 在 Nacos `nqboard-export-biz-dev.yml` 配置：

```yaml
dify:
  base-url: http://<dify-host>/v1   # 需包含 /v1 前缀
  workflow-key: app-xxxxxxxx
  timeout-ms: 120000
  retry: 2
```

### 5. 调用契约（后端 → Dify）

```
POST {base-url}/workflows/run
Authorization: Bearer <workflow-key>
{
  "inputs": { "task": "tag", "payload": "<完整提示词>" },
  "response_mode": "blocking",
  "user": "nqboard"
}
响应取：data.status == "succeeded" && data.outputs.result
```

---

## 二、任务与提示词

### 1. tag —— 领域打标

- **调用方**：`POST /export/extract/domain/tagging`（异步分批，30 条组合/批）
- **payload 结构**：领域清单 JSON + 待分类条目 JSON（`index` 从 1 开始）
- **期望输出**：`[{"index":1,"domain_code":"D01"}]`

```text
你是学科领域分类专家。请将下面每一组【学科门类/一级学科/研究方向】归入给定领域清单中的一个领域。
规则：
1. 只能使用领域清单中已有的 domain_code，禁止发明新编码。
2. 注意区分表面相似但领域不同的方向（例如"植物保护"属于农业领域，与"动物保护/动物科学"无关）。
3. index 对应待分类条目的序号，逐条输出，不得遗漏。
【领域清单】
{domain_list_json}
【待分类条目】
{batch_json}
输出格式：[{"index":1,"domain_code":"D01"}]
严格只输出 JSON，不要输出任何解释性文字或 markdown 围栏。
```

### 2. propose —— 领域提案（AI 归纳阶段 A）

- **调用方**：`POST /export/extract/domain/generate` 阶段 A（50 个研究方向/批）
- **期望输出**：`[{"domain_name":"...","description":"...","keywords":"..."}]`

```text
你是学科领域体系设计专家。下面是一批研究方向（含学科上下文）。请归纳它们所属的研究领域。
要求：
1. 每个领域包含 domain_name（8-20字）、description（50字内，说明该领域覆盖哪些研究方向）、keywords（逗号分隔，5-10个）。
2. 领域粒度适中：不要过宽（如"理工科"），也不要按每个方向各设一个领域。
3. 注意区分表面相似但领域不同的方向（例如"植物保护"属农业，与"动物保护/动物科学"无关）。
【研究方向批次】
{batch_json}
输出格式：[{"domain_name":"...","description":"...","keywords":"..."}]
严格只输出 JSON，不要输出任何解释性文字或 markdown 围栏。
```

### 3. merge —— 领域合并（AI 归纳阶段 B）

- **调用方**：`POST /export/extract/domain/generate` 阶段 B（合并全部提案，产出邻接表）
- **期望输出**：`[{"domain_code":"D01","domain_name":"...","description":"...","keywords":"...","adjacent":["..."]}]`

```text
你是学科领域体系设计专家。下面是多批次归纳出的候选领域清单（存在重复与相近领域）。请合并去重，整理为 15-30 个领域。
要求：
1. 按顺序为每个领域编号 domain_code（D01、D02……）。
2. 每个领域输出 domain_name、description（50字内）、keywords（逗号分隔，5-10个）。
3. 输出 adjacent：与该领域语义相邻、抽取时可作为候选扩展的其他领域名称数组（典型 2-4 个，必须来自本清单内的领域名称）。
4. 注意区分表面相似但领域不同的方向，不得合并（例如"植物保护"与"动物保护"属于不同领域）。
【候选领域清单】
{proposals_json}
输出格式：[{"domain_code":"D01","domain_name":"...","description":"...","keywords":"...","adjacent":["..."]}]
严格只输出 JSON，不要输出任何解释性文字或 markdown 围栏。
```

### 4. parse —— 意图解析（第 4 步抽取主流程启用）

- **调用方**：`POST /export/extract/run` 第一步
- **期望输出**：`{"domains":["D01"],"keywords":["..."],"related_disciplines":["..."],"confidence":0.9}`

```text
你是专家抽取意图解析专家。给定用户自然语言查询与领域清单，请完成解析：
1. domains：查询命中的领域 domain_code 数组，只能从领域清单中选取，无法判定时输出空数组。
2. keywords：辅助检索关键词数组（含同义词、英文名，3-8 个），例如"大模型"应同时给出 LLM。
3. related_disciplines：与查询相关的一级学科数组（用于候选过滤，注意区分相似领域，如"动物保护"相关学科为动物学/畜牧学/兽医学/生态学，不含植物保护）。
4. confidence：解析置信度，0-1 之间的小数。
【领域清单】
{domain_list_json}
【用户查询】
{query}
输出格式：{"domains":[],"keywords":[],"related_disciplines":[],"confidence":0.0}
严格只输出 JSON，不要输出任何解释性文字或 markdown 围栏。
```

---

## 三、验证样例

配好 Dify 与 Nacos 后，可用如下最小 payload 验证工作流通畅（`task=tag`）：

```json
【领域清单】
[{"domain_code":"D01","domain_name":"人工智能与大模型","description":"机器学习、深度学习、大模型相关","keywords":"机器学习,深度学习,LLM"},
 {"domain_code":"D02","domain_name":"智慧农业与植物保护","description":"作物、植物病理、农药相关","keywords":"植物保护,病虫害,精准农业"}]
【待分类条目】
[{"index":1,"subject_category":"农学","first_discipline":"植物保护","research_direction":"植物病理学"},
 {"index":2,"subject_category":"工学","first_discipline":"计算机科学与技术","research_direction":"大模型与自然语言处理"}]
```

期望输出：`[{"index":1,"domain_code":"D02"},{"index":2,"domain_code":"D01"}]`

## 四、注意事项

1. 提示词变更需**双向同步**：`ExtractPrompts.java`（运行时生效）与本目录文档
2. 所有任务的输出均为纯 JSON，后端已做围栏清理与重试（`dify.retry` 次），LLM 偶发非法输出会整批跳过并记日志，不阻断任务
3. 领域清单变更（增删改/邻接调整）后，需在领域管理页重新触发打标

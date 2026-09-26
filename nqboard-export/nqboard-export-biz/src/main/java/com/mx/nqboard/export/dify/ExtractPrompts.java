package com.mx.nqboard.export.dify;

/**
 * <p>
 * 专家抽取 LLM 提示词模板（后端组装完整提示词，Dify 工作流仅透传给 LLM）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/09/25
 */
public final class ExtractPrompts {

	private ExtractPrompts() {
	}

	private static final String OUTPUT_RULE = "严格只输出 JSON，不要输出任何解释性文字或 markdown 围栏。";

	/**
	 * 领域打标：将研究方向组合归入领域清单
	 */
	public static String tag(String domainListJson, String batchJson) {
		return """
				你是学科领域分类专家。请将下面每一组【学科门类/一级学科/研究方向】归入给定领域清单中的一个领域。
				规则：
				1. 只能使用领域清单中已有的 domain_code，禁止发明新编码。
				2. 注意区分表面相似但领域不同的方向（例如"植物保护"属于农业领域，与"动物保护/动物科学"无关）。
				3. index 对应待分类条目的序号，逐条输出，不得遗漏。
				【领域清单】
				%s
				【待分类条目】
				%s
				输出格式：[{"index":1,"domain_code":"D01"}]
				%s""".formatted(domainListJson, batchJson, OUTPUT_RULE);
	}

	/**
	 * 领域提案：从一批研究方向归纳候选领域
	 */
	public static String propose(String batchJson) {
		return """
				你是学科领域体系设计专家。下面是一批研究方向（含学科上下文）。请归纳它们所属的研究领域。
				要求：
				1. 每个领域包含 domain_name（8-20字）、description（50字内，说明该领域覆盖哪些研究方向）、keywords（逗号分隔，5-10个）。
				2. 领域粒度适中：不要过宽（如"理工科"），也不要按每个方向各设一个领域。
				3. 注意区分表面相似但领域不同的方向（例如"植物保护"属农业，与"动物保护/动物科学"无关）。
				【研究方向批次】
				%s
				输出格式：[{"domain_name":"...","description":"...","keywords":"..."}]
				%s""".formatted(batchJson, OUTPUT_RULE);
	}

	/**
	 * 领域合并：将多批提案合并去重为最终领域清单
	 */
	public static String merge(String proposalsJson) {
		return """
				你是学科领域体系设计专家。下面是多批次归纳出的候选领域清单（存在重复与相近领域）。请合并去重，整理为 15-30 个领域。
				要求：
				1. 按顺序为每个领域编号 domain_code（D01、D02……）。
				2. 每个领域输出 domain_name、description（50字内）、keywords（逗号分隔，5-10个）。
				3. 输出 adjacent：与该领域语义相邻、抽取时可作为候选扩展的其他领域名称数组（典型 2-4 个，必须来自本清单内的领域名称）。
				4. 注意区分表面相似但领域不同的方向，不得合并（例如"植物保护"与"动物保护"属于不同领域）。
				【候选领域清单】
				%s
				输出格式：[{"domain_code":"D01","domain_name":"...","description":"...","keywords":"...","adjacent":["..."]}]
				%s""".formatted(proposalsJson, OUTPUT_RULE);
	}

	/**
	 * 意图解析：将用户自然语言查询解析为领域/关键词/相关学科
	 */
	public static String parse(String domainListJson, String query) {
		return """
				你是专家抽取意图解析专家。给定用户自然语言查询与领域清单，请完成解析：
				1. domains：查询命中的领域 domain_code 数组，只能从领域清单中选取，无法判定时输出空数组。
				2. keywords：辅助检索关键词数组（含同义词、英文名，3-8 个），例如"大模型"应同时给出 LLM。
				3. related_disciplines：与查询相关的一级学科数组（用于候选过滤，注意区分相似领域，如"动物保护"相关学科为动物学/畜牧学/兽医学/生态学，不含植物保护）。
				4. confidence：解析置信度，0-1 之间的小数。
				【领域清单】
				%s
				【用户查询】
				%s
				输出格式：{"domains":[],"keywords":[],"related_disciplines":[],"confidence":0.0}
				%s""".formatted(domainListJson, query, OUTPUT_RULE);
	}
}

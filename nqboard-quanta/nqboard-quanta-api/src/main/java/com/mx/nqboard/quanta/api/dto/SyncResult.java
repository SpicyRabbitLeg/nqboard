package com.mx.nqboard.quanta.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>
 * 数据同步结果（条数口径：处理总数/成功/失败）
 * </p>
 * <p>
 * 各同步服务方法的统一返回值，由 {@code @QuantSyncLog} 切面捕获并落
 * {@code quant_sync_log}；{@link #affected} 保持原 int 返回值语义（落库影响行数），
 * 供 Controller 转换为 Feign 的 {@code R<Integer>} 契约。
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据同步结果")
public class SyncResult {

	/**
	 * 落库影响行数（原 int 返回值语义）
	 */
	@Schema(description = "落库影响行数")
	private int affected;

	/**
	 * 成功条数（与 affected 同口径时一致）
	 */
	@Schema(description = "成功条数")
	private int successCount;

	/**
	 * 失败条数
	 */
	@Schema(description = "失败条数")
	private int failCount;

	/**
	 * 处理总数（如待同步股票数/交易日数）
	 */
	@Schema(description = "处理总数")
	private Integer totalCount;

	/**
	 * 同步区间说明（如 20260815~20260822）
	 */
	@Schema(description = "同步区间说明")
	private String syncRange;

	/**
	 * 执行说明
	 */
	@Schema(description = "执行说明")
	private String message;

	/**
	 * 无失败场景的快捷构造（成功条数=影响行数，总数默认同成功条数）
	 */
	public static SyncResult of(int affected) {
		return SyncResult.builder()
				.affected(affected)
				.successCount(affected)
				.totalCount(affected)
				.build();
	}

	/**
	 * 带成功/失败计数的构造
	 */
	public static SyncResult of(int successCount, int failCount, String syncRange) {
		return SyncResult.builder()
				.affected(successCount)
				.successCount(successCount)
				.failCount(failCount)
				.totalCount(successCount + failCount)
				.syncRange(syncRange)
				.build();
	}

}

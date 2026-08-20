package com.mx.nqboard.quanta.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * 股东户数表（tushare stk_holdernumber）
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("stock_mot_holder_count")
@Schema(description = "股东户数表")
@EqualsAndHashCode(callSuper = true)
public class StockMotHolderCountEntity extends Model<StockMotHolderCountEntity> {

    private static final long serialVersionUID = 1L;

	/**
	 * 业务id
	 */
	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "业务id")
	private Long id;

	/**
	 * 创建人
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建人")
	private String createBy;

	/**
	 * 修改人
	 */
	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改人")
	private String updateBy;

	/**
	 * 创建时间
	 */
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	/**
	 * 修改时间
	 */
	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改时间")
	private LocalDateTime updateTime;

	/**
	 * 0-正常，1-删除
	 */
	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记,1:已删除,0:正常")
	private String delFlag;

	/**
	 * 排序字段
	 */
	@Schema(description = "排序字段")
	private Integer orderNum;

	/**
	 * TS股票代码
	 */
	@Schema(description = "TS股票代码")
	private String tsCode;

	/**
	 * 公告日期 YYYYMMDD
	 */
	@Schema(description = "公告日期 YYYYMMDD")
	private String annDate;

	/**
	 * 统计截止日期 YYYYMMDD(报告期末)
	 */
	@Schema(description = "统计截止日期 YYYYMMDD(报告期末)")
	private String endDate;

	/**
	 * 股东户数
	 */
	@Schema(description = "股东户数")
	private Integer holderNum;
}

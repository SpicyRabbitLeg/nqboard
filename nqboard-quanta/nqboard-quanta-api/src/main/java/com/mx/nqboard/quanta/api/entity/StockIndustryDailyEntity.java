package com.mx.nqboard.quanta.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 行业板块日线K线（东财 push2his 板块K线，secid=90.BKxxxx）
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("stock_industry_daily")
@Schema(description = "行业板块日线K线")
@EqualsAndHashCode(callSuper = true)
public class StockIndustryDailyEntity extends Model<StockIndustryDailyEntity> {

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
	 * 板块代码 BK0475
	 */
	@Schema(description = "板块代码 BK0475")
	private String boardCode;

	/**
	 * 板块名称（如：银行）
	 */
	@Schema(description = "板块名称")
	private String boardName;

	/**
	 * 交易日期 YYYYMMDD
	 */
	@Schema(description = "交易日期 YYYYMMDD")
	private String tradeDate;

	/**
	 * 开盘价
	 */
	@Schema(description = "开盘价")
	private BigDecimal open;

	/**
	 * 最高价
	 */
	@Schema(description = "最高价")
	private BigDecimal high;

	/**
	 * 最低价
	 */
	@Schema(description = "最低价")
	private BigDecimal low;

	/**
	 * 收盘价
	 */
	@Schema(description = "收盘价")
	private BigDecimal close;

	/**
	 * 成交量
	 */
	@Schema(description = "成交量")
	private BigDecimal volume;

	/**
	 * 成交额(元)
	 */
	@Schema(description = "成交额(元)")
	private BigDecimal amount;
}

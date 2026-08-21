package com.mx.nqboard.quanta.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * 交易日历（tushare trade_cal）
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("trade_cal")
@Schema(description = "交易日历（tushare trade_cal）")
@EqualsAndHashCode(callSuper = true)
public class TradeCalEntity extends Model<TradeCalEntity> {

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
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;

	/**
	 * 交易所 SSE上交所 SZSE深交所
	 */
	@Schema(description = "交易所 SSE上交所 SZSE深交所")
	private String exchange;

	/**
	 * 日历日期 YYYYMMDD
	 */
	@Schema(description = "日历日期 YYYYMMDD")
	private String calDate;

	/**
	 * 是否交易 0休市 1开市
	 */
	@Schema(description = "是否交易 0休市 1开市")
	private String isOpen;

	/**
	 * 上一个交易日 YYYYMMDD
	 */
	@Schema(description = "上一个交易日 YYYYMMDD")
	private String pretradeDate;

}

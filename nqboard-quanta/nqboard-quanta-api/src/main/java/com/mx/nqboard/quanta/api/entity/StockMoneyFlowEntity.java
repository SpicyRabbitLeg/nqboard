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
 * 个股主力资金流（东财 clist 资金流排名接口日频快照）
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("stock_money_flow")
@Schema(description = "个股主力资金流")
@EqualsAndHashCode(callSuper = true)
public class StockMoneyFlowEntity extends Model<StockMoneyFlowEntity> {

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
	 * TS股票代码 600519.SH
	 */
	@Schema(description = "TS股票代码 600519.SH")
	private String tsCode;

	/**
	 * 股票名称
	 */
	@Schema(description = "股票名称")
	private String name;

	/**
	 * 所属行业（东财口径，用于关联行业板块）
	 */
	@Schema(description = "所属行业（东财口径）")
	private String industryName;

	/**
	 * 交易日期 YYYYMMDD
	 */
	@Schema(description = "交易日期 YYYYMMDD")
	private String tradeDate;

	/**
	 * 收盘价（快照价）
	 */
	@Schema(description = "收盘价（快照价）")
	private BigDecimal close;

	/**
	 * 涨跌幅%
	 */
	@Schema(description = "涨跌幅%")
	private BigDecimal pctChg;

	/**
	 * 主力净流入额(元)
	 */
	@Schema(description = "主力净流入额(元)")
	private BigDecimal mainNetInflow;

	/**
	 * 主力净流入占比%
	 */
	@Schema(description = "主力净流入占比%")
	private BigDecimal mainNetPct;

	/**
	 * 超大单净流入额(元)
	 */
	@Schema(description = "超大单净流入额(元)")
	private BigDecimal superLargeNet;

	/**
	 * 大单净流入额(元)
	 */
	@Schema(description = "大单净流入额(元)")
	private BigDecimal largeNet;

	/**
	 * 中单净流入额(元)
	 */
	@Schema(description = "中单净流入额(元)")
	private BigDecimal mediumNet;

	/**
	 * 小单净流入额(元)
	 */
	@Schema(description = "小单净流入额(元)")
	private BigDecimal smallNet;
}

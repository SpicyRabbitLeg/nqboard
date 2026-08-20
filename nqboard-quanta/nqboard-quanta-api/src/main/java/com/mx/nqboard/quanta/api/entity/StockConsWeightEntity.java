package com.mx.nqboard.quanta.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 指数成分股及权重（中证指数官网 closeweight 文件同步）
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("stock_cons_weight")
@Schema(description = "指数成分股及权重")
@EqualsAndHashCode(callSuper = true)
public class StockConsWeightEntity extends Model<StockConsWeightEntity> {

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
	 * 股票代码，如600519.SH
	 */
	@Schema(description = "股票代码，如600519.SH")
	private String tsCode;

	/**
	 * 指数代码，如000300
	 */
	@Schema(description = "指数代码，如000300")
	private String indexCode;

	/**
	 * 指数名称，如沪深300
	 */
	@Schema(description = "指数名称，如沪深300")
	private String indexName;

	/**
	 * 指数权重(百分比)
	 */
	@Schema(description = "指数权重(百分比)")
	private BigDecimal weight;

	/**
	 * 调样生效收盘日期
	 */
	@Schema(description = "调样生效收盘日期")
	private LocalDate tradeDate;
}

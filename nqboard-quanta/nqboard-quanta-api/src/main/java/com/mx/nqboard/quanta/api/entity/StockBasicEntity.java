package com.mx.nqboard.quanta.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * Tushare股票基础信息
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("stock_basic")
@Schema(description = "Tushare股票基础信息")
@EqualsAndHashCode(callSuper = true)
public class StockBasicEntity extends Model<StockBasicEntity> {

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
     * 股票代码 000001.SZ
     */
    @Schema(description = "股票代码 000001.SZ")
    private String tsCode;

    /**
     * 股票代码
     */
    @Schema(description = "股票代码")
    private String symbol;

    /**
     * 股票名称
     */
    @Schema(description = "股票名称")
    private String name;

    /**
     * 地域
     */
    @Schema(description = "地域")
    private String area;

    /**
     * 行业
     */
    @Schema(description = "行业")
    private String industry;

    /**
     * 拼音缩写
     */
    @Schema(description = "拼音缩写")
    private String cnspell;

    /**
     * 市场类型：主板/创业板/科创板
     */
    @Schema(description = "市场类型：主板/创业板/科创板")
    private String market;

    /**
     * 上市日期
     */
    @Schema(description = "上市日期")
    private String listDate;

    /**
     * 实控人名称
     */
    @Schema(description = "实控人名称")
    private String actName;

    /**
     * 实控人企业性质
     */
    @Schema(description = "实控人企业性质")
    private String actEntType;
}

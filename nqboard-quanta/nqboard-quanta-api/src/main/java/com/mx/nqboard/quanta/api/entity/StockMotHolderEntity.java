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
 * Tushare 股东增减持表 mot_holder（stk_holdertrade）
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("stock_mot_holder")
@Schema(description = "Tushare 股东增减持表")
@EqualsAndHashCode(callSuper = true)
public class StockMotHolderEntity extends Model<StockMotHolderEntity> {

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
	 * TS代码
	 */
	@Schema(description = "TS代码")
	private String tsCode;

	/**
	 * 公告日期
	 */
	@Schema(description = "公告日期")
	private String annDate;

	/**
	 * 股东名称
	 */
	@Schema(description = "股东名称")
	private String holderName;

	/**
	 * 股东类型 G高管 P个人 C公司
	 */
	@Schema(description = "股东类型 G高管 P个人 C公司")
	private String holderType;

	/**
	 * IN增持 DE减持（tushare 原字段名 in_de，落库列名 in_des）
	 */
	@Schema(description = "IN增持 DE减持")
	private String inDes;

	/**
	 * 变动数量
	 */
	@Schema(description = "变动数量")
	private BigDecimal changeVol;

	/**
	 * 占流通比例(%)
	 */
	@Schema(description = "占流通比例(%)")
	private BigDecimal changeRatio;

	/**
	 * 变动后持股
	 */
	@Schema(description = "变动后持股")
	private BigDecimal afterShare;

	/**
	 * 变动后占流通比例(%)
	 */
	@Schema(description = "变动后占流通比例(%)")
	private BigDecimal afterRatio;

	/**
	 * 平均价格
	 */
	@Schema(description = "平均价格")
	private BigDecimal avgPrice;

	/**
	 * 持股总数
	 */
	@Schema(description = "持股总数")
	private BigDecimal totalShare;

	/**
	 * 增减持开始日期
	 */
	@Schema(description = "增减持开始日期")
	private String beginDate;

	/**
	 * 增减持结束日期
	 */
	@Schema(description = "增减持结束日期")
	private String closeDate;
}

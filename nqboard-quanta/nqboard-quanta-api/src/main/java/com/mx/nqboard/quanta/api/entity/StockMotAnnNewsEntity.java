package com.mx.nqboard.quanta.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * 公告&媒体新闻表（东方财富/巨潮资讯，降级策略同步）
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Data
@TableName("stock_mot_ann_news")
@Schema(description = "公告&媒体新闻表")
@EqualsAndHashCode(callSuper = true)
public class StockMotAnnNewsEntity extends Model<StockMotAnnNewsEntity> {

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
	 * 发布日期 YYYYMMDD
	 */
	@Schema(description = "发布日期 YYYYMMDD")
	private String pubDate;

	/**
	 * 精确发布时间
	 */
	@Schema(description = "精确发布时间")
	private String pubDatetime;

	/**
	 * ann交易所公告 media媒体新闻
	 */
	@Schema(description = "ann交易所公告 media媒体新闻")
	private String newsType;

	/**
	 * 来源：巨潮资讯/东方财富等
	 */
	@Schema(description = "来源：巨潮资讯/东方财富等")
	private String src;

	/**
	 * 新闻/公告标题【核心字段】
	 */
	@Schema(description = "新闻/公告标题【核心字段】")
	private String title;

	/**
	 * 简短摘要，不要存全文
	 */
	@Schema(description = "简短摘要，不要存全文")
	private String summary;

	/**
	 * 原文链接
	 */
	@Schema(description = "原文链接")
	private String url;
}

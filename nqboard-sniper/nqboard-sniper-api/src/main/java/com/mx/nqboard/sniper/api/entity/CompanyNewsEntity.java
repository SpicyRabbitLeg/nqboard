package com.mx.nqboard.sniper.api.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mx.nqboard.common.mybatis.base.BaseEntity;
import com.mx.nqboard.sniper.api.enums.SentimentEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 个股新闻（sentiment/policy 输入；sentiment 为关键词规则预打标，入库时确定）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
@Data
@TableName("sniper_company_news")
@Schema(description = "个股新闻")
@EqualsAndHashCode(callSuper = true)
public class CompanyNewsEntity extends BaseEntity {

	private static final long serialVersionUID = 1L;

	/**
	 * 雪花id
	 */
	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "雪花id")
	private Long id;

	/**
	 * 删除状态（0未删除、1删除）
	 */
	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除状态（0未删除、1删除）")
	private String delFlag;

	/**
	 * 6位代码
	 */
	@Schema(description = "6位代码")
	private String code;

	/**
	 * 新闻标题
	 */
	@Schema(description = "新闻标题")
	private String title;

	/**
	 * 文章来源
	 */
	@Schema(description = "文章来源")
	private String sourceName;

	/**
	 * 新闻链接
	 */
	@Schema(description = "新闻链接")
	private String url;

	/**
	 * 正文前2000字
	 */
	@Schema(description = "正文前2000字")
	private String bodyDigest;

	/**
	 * 发布时间
	 */
	@Schema(description = "发布时间")
	private LocalDateTime publishedAt;

	/**
	 * 关键词规则预打标（入库时）
	 */
	@Schema(description = "关键词规则预打标（入库时）")
	private SentimentEnum sentiment;

	/**
	 * 公告类型（业绩预告/回购/减持/监管/政策）
	 */
	@Schema(description = "公告类型（业绩预告/回购/减持/监管/政策）")
	private String announcementType;

	/**
	 * 拉取日（增量游标）
	 */
	@Schema(description = "拉取日（增量游标）")
	private LocalDate fetchDate;

	/**
	 * 拉取时间
	 */
	@Schema(description = "拉取时间")
	private LocalDateTime fetchedAt;

}

package com.mx.nqboard.sniper.data.ingest;

import java.time.LocalDateTime;

import com.mx.nqboard.common.mybatis.base.BaseEntity;

/**
 * <p>
 * 行情/事件表批量入库的审计列显式赋值（主文档 §3 头注：append-only 表不走 BaseMapper 单条 CRUD，
 * 批量路径审计列由代码显式赋值、不依赖填充器；del_flag 恒 '0' 仅作规范留位）。
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
public final class IngestAudit {

	private IngestAudit() {
	}

	/**
	 * 入库前统一赋审计列：create_by/update_by='sniper'、时间=now。
	 * del_flag 不在此赋值——由 upsert SQL 省略该列、DB 默认 '0' 填充。
	 * <b>id 必须由调用方 {@code entity.setId(IngestAudit.newId())} 显式生成</b>：
	 * 自定义 upsert SQL 不走 MP 自带 insert，ASSIGN_ID 填充器不触发。
	 */
	public static <E extends BaseEntity> E fill(E entity) {
		LocalDateTime now = LocalDateTime.now();
		entity.setCreateBy("sniper");
		entity.setCreateTime(now);
		entity.setUpdateBy("sniper");
		entity.setUpdateTime(now);
		return entity;
	}

	/** 雪花 id 生成（MyBatis-Plus IdWorker，与 ASSIGN_ID 同一算法） */
	public static long newId() {
		return com.baomidou.mybatisplus.core.toolkit.IdWorker.getId();
	}

}

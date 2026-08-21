package com.mx.nqboard.quanta.screen;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mx.nqboard.quanta.api.entity.StockIndexDailyEntity;
import com.mx.nqboard.quanta.mapper.StockIndexDailyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>
 * 大盘环境计算器（沪深300 5日收益 + H8 大盘门）
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Component
@RequiredArgsConstructor
public class MarketRegimeCalculator {

	/**
	 * 大盘基准指数
	 */
	private static final String BENCHMARK_INDEX = "sh000300";

	private final StockIndexDailyMapper stockIndexDailyMapper;

	/**
	 * H8 大盘门：5日收益低于该值当日不产生新候选（yml quanta.screen.market-min-ret-5d）
	 */
	@Value("${quanta.screen.market-min-ret-5d:" + ScreenConstants.H8_MARKET_MIN_RET_5D + "}")
	private double marketMinRet5d;

	/**
	 * 计算大盘环境
	 * @param tradeDate 信号日 YYYYMMDD
	 * @return 5日收益（数据不足返回0），blocked=是否触发大盘门
	 */
	public MarketRegime calc(String tradeDate) {
		List<StockIndexDailyEntity> rows = stockIndexDailyMapper.selectList(
				Wrappers.<StockIndexDailyEntity>lambdaQuery()
						.eq(StockIndexDailyEntity::getIndexCode, BENCHMARK_INDEX)
						.le(StockIndexDailyEntity::getTradeDate, tradeDate)
						.orderByDesc(StockIndexDailyEntity::getTradeDate)
						.last("limit 6"));
		if (rows.size() < 2) {
			return new MarketRegime(0, false);
		}
		// 倒序查询：rows[0] 最新
		double oldest = rows.get(rows.size() - 1).getClose().doubleValue();
		double latest = rows.get(0).getClose().doubleValue();
		double ret5d = oldest > 0 ? latest / oldest - 1 : 0;
		boolean blocked = ret5d < marketMinRet5d;
		return new MarketRegime(ret5d, blocked);
	}

	/**
	 * 大盘环境：5日收益 + 大盘门是否阻断
	 */
	public record MarketRegime(double ret5d, boolean blocked) {

	}

}

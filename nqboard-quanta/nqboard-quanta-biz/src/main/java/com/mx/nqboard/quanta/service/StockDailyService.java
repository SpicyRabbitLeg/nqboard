package com.mx.nqboard.quanta.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.quanta.api.entity.StockDailyEntity;
import com.mx.nqboard.quanta.api.vo.StockDailyKlineVO;

import java.util.List;

/**
 * <p>
 * Tushare日线行情 服务类
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public interface StockDailyService extends IService<StockDailyEntity> {

	/**
	 * 查询K线数据：按股票代码返回最新 limit 根日线，按交易日期正序
	 * <p>
	 * 前端一次拉取全量持有，本地切片翻页 + 均线计算，避免翻页反复请求
	 * @param tsCode 股票代码
	 * @param limit 返回根数上限
	 * @return K线数据列表（正序）
	 */
	List<StockDailyKlineVO> kline(String tsCode, Integer limit);

	/**
	 * 从 tushare 同步股票日线行情（市场过滤与全量/增量均取 yml 配置）
	 *
	 * <p>
	 * 本方法可被 Quartz 定时任务反射调用（后端调度中心 springBean 方式）：
	 * <pre>
	 *   jobType      = 2   （spring bean）
	 *   className    = stockDailyService
	 *   methodName   = syncFromTushare
	 *   cronExpression = 0 30 18 * * ?   （示例：每天 18:30 收盘后增量同步）
	 * </pre>
	 * @return 同步成功的条数
	 */
	int syncFromTushare();

	/**
	 * 从 tushare 同步股票日线行情
	 * <p>
	 * 流程：读取 stock_basic 全量股票代码（按市场过滤）→ 逐股票调用 tushare daily 接口
	 * → 按唯一键 (ts_code, trade_date) 批量插入/更新
	 * @param market 市场类型：主板/创业板/科创板，为空取 yml 配置 tushare.daily.market
	 * @param full 是否全量同步：true=2026-01-01 至今天；false=仅增量获取今天；为空取 yml 配置 tushare.daily.full
	 * @return 同步成功的条数
	 */
	int syncFromTushare(String market, Boolean full);

}

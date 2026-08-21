package com.mx.nqboard.quanta.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mx.nqboard.quanta.api.entity.StockBacktestTaskEntity;
import com.mx.nqboard.quanta.api.entity.StockBacktestTradeEntity;
import com.mx.nqboard.quanta.backtest.BacktestEngine;
import com.mx.nqboard.quanta.backtest.BacktestParams;
import com.mx.nqboard.quanta.mapper.StockBacktestTaskMapper;
import com.mx.nqboard.quanta.service.StockBacktestTaskService;
import com.mx.nqboard.quanta.service.StockBacktestTradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>
 * 回测任务 服务实现类（任务编排 + 异步执行 + 结果落库）
 * </p>
 *
 * @author SpicyRabbitLeg
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockBacktestTaskServiceImpl extends ServiceImpl<StockBacktestTaskMapper, StockBacktestTaskEntity>
		implements StockBacktestTaskService {

	/**
	 * 批量落库每批条数
	 */
	private static final int BATCH_SIZE = 500;

	private final StockBacktestTaskMapper stockBacktestTaskMapper;

	private final StockBacktestTradeService stockBacktestTradeService;

	private final BacktestEngine backtestEngine;

	/**
	 * 回测并发保护：同一时刻仅允许一个回测执行（数据加载重，避免重复IO）
	 */
	private final AtomicBoolean running = new AtomicBoolean(false);

	/**
	 * 异步执行线程池（单线程）
	 */
	private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "quant-backtest");
		t.setDaemon(true);
		return t;
	});

	@Override
	public Long createTask(BacktestParams params) {
		if (params == null) {
			params = new BacktestParams();
		}
		StockBacktestTaskEntity task = new StockBacktestTaskEntity();
		task.setParams(JSON.toJSONString(params));
		task.setStatus("PENDING");
		task.setProgress(0);
		save(task);
		executor.submit(() -> execute(task.getId()));
		return task.getId();
	}

	@Override
	public Long runLatest() {
		// 优先重跑最近一个 FAILED 任务；否则创建默认参数新任务
		StockBacktestTaskEntity failed = getOne(Wrappers.<StockBacktestTaskEntity>lambdaQuery()
				.in(StockBacktestTaskEntity::getStatus, "PENDING", "FAILED")
				.orderByDesc(StockBacktestTaskEntity::getId)
				.last("limit 1"));
		if (failed != null) {
			executor.submit(() -> execute(failed.getId()));
			return failed.getId();
		}
		return createTask(new BacktestParams());
	}

	@Override
	public Long rerun(Long taskId) {
		StockBacktestTaskEntity task = getById(taskId);
		if (task == null) {
			throw new IllegalArgumentException("回测任务不存在: " + taskId);
		}
		// 清理旧成交明细
		stockBacktestTradeService.remove(Wrappers.<StockBacktestTradeEntity>lambdaQuery()
				.eq(StockBacktestTradeEntity::getTaskId, taskId));
		task.setStatus("PENDING");
		task.setProgress(0);
		task.setStats(null);
		task.setEquityCurve(null);
		task.setErrorMsg(null);
		updateById(task);
		executor.submit(() -> execute(taskId));
		return taskId;
	}

	/**
	 * 执行任务（引擎调用 + 进度回写 + 结果落库）
	 */
	private void execute(Long taskId) {
		if (!running.compareAndSet(false, true)) {
			log.warn("已有回测在执行，任务 {} 排队线程被拒绝，可稍后 rerun", taskId);
			return;
		}
		StockBacktestTaskEntity task = getById(taskId);
		if (task == null) {
			running.set(false);
			return;
		}
		try {
			task.setStatus("RUNNING");
			updateById(task);
			BacktestParams params = JSON.parseObject(task.getParams(), BacktestParams.class);
			BacktestEngine.BacktestReport report = backtestEngine.run(params, (percent, message) -> {
				StockBacktestTaskEntity patch = new StockBacktestTaskEntity();
				patch.setId(taskId);
				patch.setProgress(percent);
				updateById(patch);
			});
			// 成交明细落库
			List<StockBacktestTradeEntity> trades = new ArrayList<>();
			for (BacktestEngine.TradeRecord t : report.getTrades()) {
				StockBacktestTradeEntity e = new StockBacktestTradeEntity();
				e.setTaskId(taskId);
				e.setTsCode(t.getTsCode());
				e.setName(t.getName());
				e.setPattern(t.getPattern());
				e.setEntryDate(t.getEntryDate());
				e.setEntryPrice(BigDecimal.valueOf(t.getEntryPrice()).setScale(3, RoundingMode.HALF_UP));
				e.setExitDate(t.getExitDate());
				e.setExitPrice(BigDecimal.valueOf(t.getExitPrice()).setScale(3, RoundingMode.HALF_UP));
				e.setQty(t.getQty());
				e.setReason(t.getReason());
				e.setPnl(BigDecimal.valueOf(t.getPnl()).setScale(2, RoundingMode.HALF_UP));
				e.setRet(BigDecimal.valueOf(t.getRet()).setScale(4, RoundingMode.HALF_UP));
				e.setHeldDays(t.getHeldDays());
				e.setSignalScore(BigDecimal.valueOf(t.getSignalScore()).setScale(2, RoundingMode.HALF_UP));
				trades.add(e);
			}
			if (!trades.isEmpty()) {
				stockBacktestTradeService.saveBatch(trades, BATCH_SIZE);
			}
			// 统计与权益曲线
			task.setStats(JSON.toJSONString(report.getStats()));
			task.setEquityCurve(JSON.toJSONString(report.getEquityCurve()));
			task.setStatus("DONE");
			task.setProgress(100);
			updateById(task);
			log.info("回测任务 {} 完成: 成交 {} 笔", taskId, trades.size());
		}
		catch (Exception e) {
			log.error("回测任务 {} 执行失败", taskId, e);
			StockBacktestTaskEntity patch = new StockBacktestTaskEntity();
			patch.setId(taskId);
			patch.setStatus("FAILED");
			patch.setErrorMsg(StrUtil.maxLength(e.getMessage(), 1000));
			updateById(patch);
		}
		finally {
			running.set(false);
		}
	}

}

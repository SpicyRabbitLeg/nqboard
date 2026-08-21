package com.mx.nqboard.quanta.test;

import com.mx.nqboard.quanta.screen.ExitEngine;

/**
 * <p>
 * 离场引擎六规则校验（main 方法运行，回测与模拟盘共用，必须锁死行为）
 * </p>
 *
 * @author SpicyRabbitLeg
 */
public class ExitEngineTest {

	private static int failures = 0;

	public static void main(String[] args) {
		// ① 止损：收盘 ≤ 95
		check("止损触发", "stop_loss".equals(ExitEngine.evaluateClose(100, 2, 100, 94.5)));
		// ② 保本止损：第3日起，期间浮盈曾≥5%（peak=106），收盘回落到买入价下方
		check("保本止损触发", "breakeven_stop".equals(ExitEngine.evaluateClose(100, 3, 106, 100.0)));
		check("保本止损-低于买入价", "breakeven_stop".equals(ExitEngine.evaluateClose(100, 4, 110, 99.0)));
		// 保本未生效：第2日（未满3日）
		check("保本未到生效日持有", null == ExitEngine.evaluateClose(100, 2, 106, 100.0));
		// 保本未生效：浮盈未达5%
		check("保本浮盈不足持有", null == ExitEngine.evaluateClose(100, 3, 103, 100.5));
		// 止损优先于保本（94 同时满足两者条件）
		check("止损优先于保本", "stop_loss".equals(ExitEngine.evaluateClose(100, 3, 106, 94.0)));
		// ③ 止盈：收盘 ≥ 115
		check("止盈触发", "take_profit".equals(ExitEngine.evaluateClose(100, 1, 114, 115.5)));
		// ④ 弱势提前离场：第4日收盘 < 买入价
		check("弱势提前离场", "weak_exit".equals(ExitEngine.evaluateClose(100, 4, 101, 99.5)));
		check("第3日不触发弱势离场", null == ExitEngine.evaluateClose(100, 3, 101, 99.5));
		// ⑤ 时间止损：持有满5日
		check("时间止损", "time_exit".equals(ExitEngine.evaluateClose(100, 5, 103, 102.0)));
		// 正常持有
		check("正常持有", null == ExitEngine.evaluateClose(100, 2, 103, 102.0));
		// ⑥ 开盘急杀
		check("开盘急杀触发", ExitEngine.isGapStop(100, 94.9));
		check("开盘急杀未触发", !ExitEngine.isGapStop(100, 95.1));
		// H7 入场跳空
		check("跳空高开放弃", ExitEngine.entryGapBlocked(100, 105.1));
		check("跳空低开放弃", ExitEngine.entryGapBlocked(100, 96.9));
		check("正常开盘可买", !ExitEngine.entryGapBlocked(100, 103.0));
		check("跌停开盘放弃", ExitEngine.entryGapBlocked(100, 90.0));

		System.out.println();
		if (failures == 0) {
			System.out.println("===== 全部通过 =====");
		}
		else {
			System.out.println("===== 失败 " + failures + " 项 =====");
			System.exit(1);
		}
	}

	private static void check(String name, boolean ok) {
		System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
		if (!ok) {
			failures++;
		}
	}

}

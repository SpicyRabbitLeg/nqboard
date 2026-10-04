package com.mx.nqboard.sniper.service;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Supplier;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mx.nqboard.sniper.api.entity.DailyRunEntity;
import com.mx.nqboard.sniper.api.enums.RunPhaseEnum;

/**
 * <p>
 * 每日运行记账 服务类（含任务幂等包装模板 runGuarded）
 * </p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
public interface DailyRunService extends IService<DailyRunEntity> {

	/**
	 * 幂等包装：UK(run_date, phase) 查已有行——running 态跳过（防并发双跑），done/failed 幂等重跑；
	 * work 正常返回写 done+detail（含 budgetUsed 键则写预算列），异常写 failed 后原样上抛。
	 */
	Map<String, Object> runGuarded(LocalDate runDate, RunPhaseEnum phase, Supplier<Map<String, Object>> work);

}

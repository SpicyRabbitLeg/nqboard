package com.mx.nqboard.sniper.data.ingest;

import java.time.Duration;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mx.nqboard.sniper.api.entity.DailyRunEntity;
import com.mx.nqboard.sniper.api.enums.RunStatusEnum;
import com.mx.nqboard.sniper.service.DailyRunService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 应用启动兜底：把上次进程中断遗留的 running 态运行记录置 failed。
 * runGuarded 对 running 态是"跳过"，进程被杀后该行永远停在 running，
 * 同 (run_date, phase) 的重跑会被永久拒绝——启动时统一清理。
 * </p>
 * <p>阈值 12 小时：单实例部署下正常运行任务最长数十分钟（9 月回填实测 44 分钟），
 * 启动时仍 running 的记录必然是中断残留；12 小时同时规避 Quartz 集群多节点
 * 场景误标其他节点刚启动的长任务。</p>
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyRunStartupRecovery implements ApplicationRunner {

	/** running 记录超过该时长视为中断残留（单实例最长任务实测 44 分钟） */
	private static final Duration STALE_THRESHOLD = Duration.ofHours(12);

	private final DailyRunService dailyRunService;

	@Override
	public void run(ApplicationArguments args) {
		boolean updated = dailyRunService.update(Wrappers.<DailyRunEntity>lambdaUpdate()
			.eq(DailyRunEntity::getStatus, RunStatusEnum.RUNNING)
			.lt(DailyRunEntity::getStartedAt, LocalDateTime.now().minus(STALE_THRESHOLD))
			.set(DailyRunEntity::getStatus, RunStatusEnum.FAILED)
			.set(DailyRunEntity::getDetail, "{\"error\":\"interrupted by restart\"}")
			.set(DailyRunEntity::getFinishedAt, LocalDateTime.now()));
		if (updated) {
			log.info("startup recovery: stale RUNNING daily_run rows marked failed");
		}
	}

}

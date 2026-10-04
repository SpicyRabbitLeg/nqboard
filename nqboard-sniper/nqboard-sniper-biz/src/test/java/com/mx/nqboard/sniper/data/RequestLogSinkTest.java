package com.mx.nqboard.sniper.data;

import java.util.concurrent.atomic.AtomicInteger;

import com.mx.nqboard.sniper.api.entity.RequestLogEntity;
import com.mx.nqboard.sniper.service.RequestLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RequestLogSink 单测——审计列显式赋值、params JSON 包装、error 截断、失败不阻断。
 *
 * @author SpicyRabbitLeg
 * @date 2026/10/03
 */
class RequestLogSinkTest {

	@Test
	@DisplayName("落表字段与审计列显式赋值（append-only，不依赖填充器）")
	void writesEntityWithExplicitAuditColumns() {
		RequestLogService service = mock(RequestLogService.class);
		when(service.save(any(RequestLogEntity.class))).thenReturn(true);
		RequestLogSink sink = new RequestLogSink(service);

		sink.accept("daily", "tushare", "{\"trade_date\":\"20260930\"}", "ok", 5400, 1234L, null);

		ArgumentCaptor<RequestLogEntity> captor = ArgumentCaptor.forClass(RequestLogEntity.class);
		verify(service, times(1)).save(captor.capture());
		RequestLogEntity entity = captor.getValue();
		assertThat(entity.getEndpoint()).isEqualTo("daily");
		assertThat(entity.getSource()).isEqualTo("tushare");
		// params 为合法 JSON 原样存
		assertThat(entity.getParams()).isEqualTo("{\"trade_date\":\"20260930\"}");
		assertThat(entity.getStatus()).isEqualTo("ok");
		assertThat(entity.getNRows()).isEqualTo(5400);
		assertThat(entity.getElapsedMs()).isEqualTo(1234);
		assertThat(entity.getError()).isNull();
		// 审计列显式赋值
		assertThat(entity.getCreateBy()).isEqualTo("sniper");
		assertThat(entity.getUpdateBy()).isEqualTo("sniper");
		assertThat(entity.getDelFlag()).isEqualTo("0");
		assertThat(entity.getCreateTime()).isNotNull();
	}

	@Test
	@DisplayName("非 JSON params（完整 URL）包装为 {\"raw\": …} 存入 json 列")
	void wrapsNonJsonParams() {
		RequestLogService service = mock(RequestLogService.class);
		when(service.save(any(RequestLogEntity.class))).thenReturn(true);
		RequestLogSink sink = new RequestLogSink(service);

		sink.accept("kline/get", "em", "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=1.600519", "ok",
				2468, 88L, null);

		ArgumentCaptor<RequestLogEntity> captor = ArgumentCaptor.forClass(RequestLogEntity.class);
		verify(service).save(captor.capture());
		assertThat(captor.getValue().getParams()).contains("\"raw\"").contains("secid=1.600519");
	}

	@Test
	@DisplayName("error 超长截断到 500 字")
	void truncatesErrorToColumnWidth() {
		RequestLogService service = mock(RequestLogService.class);
		when(service.save(any(RequestLogEntity.class))).thenReturn(true);
		RequestLogSink sink = new RequestLogSink(service);

		sink.accept("daily", "tushare", "{}", "error", null, 10L, "x".repeat(800));

		ArgumentCaptor<RequestLogEntity> captor = ArgumentCaptor.forClass(RequestLogEntity.class);
		verify(service).save(captor.capture());
		assertThat(captor.getValue().getError()).hasSize(500);
		assertThat(captor.getValue().getNRows()).isNull();
	}

	@Test
	@DisplayName("审计写失败只吞异常（不阻断主链路）")
	void swallowsWriteFailure() {
		RequestLogService service = mock(RequestLogService.class);
		doThrow(new RuntimeException("db down")).when(service).save(any(RequestLogEntity.class));
		RequestLogSink sink = new RequestLogSink(service);

		// 不抛异常即通过
		sink.accept("daily", "tushare", "{}", "ok", 1, 1L, null);
	}

	@Test
	@DisplayName("高频写入全量记录（Java 默认全量 vs Python opt-in）")
	void recordsEveryCall() {
		RequestLogService service = mock(RequestLogService.class);
		when(service.save(any(RequestLogEntity.class))).thenReturn(true);
		RequestLogSink sink = new RequestLogSink(service);

		AtomicInteger calls = new AtomicInteger();
		for (int i = 0; i < 50; i++) {
			sink.accept("daily", "tushare", "{}", "ok", i, 1L, null);
			calls.incrementAndGet();
		}
		verify(service, times(calls.get())).save(any(RequestLogEntity.class));
	}

}

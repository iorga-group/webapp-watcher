package com.iorga.webappwatcher.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.iorga.webappwatcher.watcher.RetentionLogWritingWatcher;

public class PatternDurationParameterHandlerTest {

	@Test
	public void testPattern() {
		PatternDurationParameterHandler<?> handler = new PatternDurationParameterHandler<RetentionLogWritingWatcher>(RetentionLogWritingWatcher.class, "writingEventsCooldown");

		List<PatternDuration> patternDurations = handler.convertFromString(".*RequestLogFilter#.*:60,.*writeAllRequestsWatcher#.*:-1,.*:1800");
		assertThat(patternDurations).hasSize(3);
		assertThat(patternDurations.get(0).getPattern().pattern()).isEqualTo(".*RequestLogFilter#.*");
		assertThat(patternDurations.get(1).getPattern().pattern()).isEqualTo(".*writeAllRequestsWatcher#.*");

		patternDurations = handler.convertFromString(".*RequestDurationWatcher#.*:1800,.*CpuCriticalUsageWatcher#.*:1800,.*RequestLogFilter#.*:-1,.*WriteAllRequestsWatcher#.*:-1");
		assertThat(patternDurations).hasSize(4);
	}
}

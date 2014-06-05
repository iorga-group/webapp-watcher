package com.iorga.webappwatcher;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;

import com.iorga.webappwatcher.event.RetentionLogWritingEvent;
import com.iorga.webappwatcher.eventlog.RequestEventLog;
import com.iorga.webappwatcher.watcher.RequestDurationWatcher;
import com.iorga.webappwatcher.watcher.RetentionLogWritingWatcher;

public class RequestLogFilterTest {
	private RequestLogFilter requestLogFilter;
	private EventLogManager eventLogManagerSpied;
	private RetentionLogWritingWatcherTest retentionLogWritingWatcherSpied;

	public static class RetentionLogWritingWatcherTest extends RetentionLogWritingWatcher {
		@Override
		public void sendMailForEvent(RetentionLogWritingEvent event) {
			super.sendMailForEvent(event);
		}
	}

	@Before
	public void setupFilter() throws ServletException {
		// Spy EventLogManager
		eventLogManagerSpied = spy(new EventLogManager());
		EventLogManager.setInstance(eventLogManagerSpied);

		requestLogFilter = new RequestLogFilter() {
			@Override
			protected RetentionLogWritingWatcher createRetentionLogWritingWatcher() {
				final RetentionLogWritingWatcherTest retentionLogWritingWatcher = new RetentionLogWritingWatcherTest();
				retentionLogWritingWatcherSpied = spy(retentionLogWritingWatcher);
				return retentionLogWritingWatcherSpied;
			}
		};
		FilterConfig filterConfig = mock(FilterConfig.class);
		when(filterConfig.getInitParameterNames()).thenReturn(new Enumeration<Object>() {
			@Override
			public boolean hasMoreElements() {
				return false;
			}
			@Override
			public Object nextElement() {
				return null;
			}
		});
		requestLogFilter.init(filterConfig);
	}

	@Test
	public void testChangeMailSettingsAndLogLongRequest() throws InterruptedException, IOException {
		requestLogFilter.setParameter("mailSmtpHost", "mailhost");
		requestLogFilter.setParameter("mailFrom", "from@mailhost.tld");
		requestLogFilter.setParameter("mailTo", "to@mailhost.tld");

		testLongDurationRequest();
	}

	@SuppressWarnings("unchecked")
	private void testLongDurationRequest() throws InterruptedException, IOException {
		final EventLogManager eventLogManager = EventLogManager.getInstance();
		RequestEventLog eventLog = eventLogManager.addEventLog(RequestEventLog.class);
		eventLog.setRequestURI("/test.xhtml");
		eventLog.setAfterProcessedDate(new Date(new Date().getTime()+40*1000));
		eventLogManager.fire(eventLog);

		Thread.sleep(300);

		verify(eventLogManagerSpied).writeRetentionLog(eq(RequestDurationWatcher.class), (String)any(), (Map<String, Object>)any());
		verify(retentionLogWritingWatcherSpied).sendMailForEvent((RetentionLogWritingEvent) any());
	}



	@Test
	public void test() throws InterruptedException, IOException {
		requestLogFilter.setParameter("writingEventsCooldown", ".*RequestLogFilter#.*:60,.*writeAllRequestsWatcher#.*:-1,.*:1800");

		testLongDurationRequest();
	}
}

---
layout: docs
title: Configuration
permalink: configuration/
lang: en
---

## Where to define your configurations

You can configure the default behaviors of WAW by setting values to specific configuration parameters either in the `web.xml` in `<init-param>`s like this :

```xml
	<filter>
		<filter-name>Log Filter</filter-name>
		<filter-class>com.iorga.webappwatcher.RequestLogFilter</filter-class>
		<init-param>
			<param-name>requestNameIncludes</param-name>
			<param-value>.*\.xhtml</param-value>
		</init-param>
	</filter>

```

Or you can add the file `WEB-INF/classes/webappwatcher.properties` with classic `key=value` declarations like this :

```
# A comment
requestNameIncludes=.*\.xhtml
```

## Main configuration parameters

### `requestNameIncludes`
List of regexp (separated by commas) to include requests (based on their `requestURI`).

Default value : `.*\\.xhtml`

### `requestNameExcludes`
List of regexp (separated by commas) to exclude requests (based on their `requestURI`).

`requestNameIncludes` is tested first and if the request matched, `requestNameExcludes` is tested.

Default value : _empty_

### <a name="cmdRequestName"></a>`cmdRequestName`
Base path alowing one to access to the filter GUI & "WebService".

Default value : `RequestLogFilterCmd`

### <a name="waitForEventLogToCompleteMillis"></a>`waitForEventLogToCompleteMillis`
The log writing thread will wait for that amount of time (in millis) for a request to be completed. If it's still not completed, at the end of that time, will log the request if `completed` field set to `false`.

Default value : `300000` _(5mn)_

### `logPath`
Path to write WAW log files, containing base log file (without the extension).

Default value : `webappwatcherlog`

### <a name="eventLogRetentionMillis"></a>`eventLogRetentionMillis`
The requests are stored in memory that amount of time before being logged or withdrawn (depending of crossed CPU threshold for example).

Default value : `300000` _(5mn)_

### `maxLogFileSizeMo`
Maximum length of a log file in MB. If the current log cross that limit, a new log file is created

Default value : `100`

### `cpuComputationDeltaMillis`
Time to wait in millis before creating a new "SystemEventLog" log (containing stacktraces, CPU, memory usage).

Default value : `300`

### `threadNameIncludes`
List of regexp (separated by commas) to include stacktraces based on their Thread's name.

Default value : `http.*`

### `threadNameExcludes`
List of regexp (separated by commas) to exclude stacktraces based on their Thread's name. `threadNameIncludes` is evaluated first, and if the Thread's name matched, `threadNameExcludes` is tested.

Default value : _empty_

### <a name="eventLogWatchers"></a>`eventLogWatchers`
List of "Watcher" class names separated by commas which trigger log writing.

Possible values :

Watcher class name | Watcher description
------------------ | -------------------
`com.iorga.webappwatcher.watcher.CpuCriticalUsageWatcher` | Observes CPU pics, and logs the event log queue if the configured threshold is crossed.
`com.iorga.webappwatcher.watcher.WriteAllRequestsWatcher` | Observes the events which would be deleted (if no watcher asked to write the event log queue, [`eventLogRetentionMillis`](#eventLogRetentionMillis) is reached) and asks to write only `RequestEventLog` events. Necessary to keep reasonnable log file size (by filtering out "big" log events like `SystemEventLog`s).
`com.iorga.webappwatcher.watcher.RequestDurationWatcher` | Observes too long requests (or lasting more than [`waitForEventLogToCompleteMillis`](#waitForEventLogToCompleteMillis)) and asks to log all events if the threashold is reached.
`com.iorga.webappwatcher.watcher.RetentionLogWritingWatcher` | Observes log writings and send alert mails if configured.

Default value :

```
com.iorga.webappwatcher.watcher.CpuCriticalUsageWatcher,com.iorga.webappwatcher.watcher.WriteAllRequestsWatcher,com.iorga.webappwatcher.watcher.RequestDurationWatcher,com.iorga.webappwatcher.watcher.RetentionLogWritingWatcher
```


## Configuration parameters [specific](#eventLogWatchers) to `com.iorga.webappwatcher.watcher.CpuCriticalUsageWatcher`

### `criticalCpuUsage`
If that CPU threshold (in %, float) is crossed, the event log queue in memory is flushed to disk.

Default value : 1.5 CPU by default, which means `75` if you have 2 CPUs (100 / nb_CPU * 1.5)

### `deadLockThreadsSearchDeltaMillis`
Time to wait in millis before triggering a new DeadLockThread detection.

Default value : `300000` _(5mn)_


## Configuration parameters [specific](#eventLogWatchers) to `com.iorga.webappwatcher.watcher.RequestDurationWatcher`

### `requestDurationLimits`
List of regexp (separated by commas) mapping request to a timeout (in millis).

If the request matching the regexp last more than the mapped timeout, a full event log queue flush is triggered.

Example : `.*/login\\.xhtml:60000` will flush to disk all the events if a request matching `.*/login\\.xhtml` last more than 60s (60000ms).

Default value : `.*\\.xhtml:30000`


## Configuration parameters [specific](#eventLogWatchers) to `com.iorga.webappwatcher.watcher.RetentionLogWritingWatcher`

### `writingEventsCooldown`
List of regexp (separated by commas) mapping watcher name and event's name to a cooldown time (in seconds).

If a mail is sent to warn about a specific event, another couldn't be sent for the same event while the mapped cooldown time has not passed.

The format is : `<WatcherNameRegexp>#<EventNameRegexp>:<CooldownInSeconds>`. Example : `.*CpuCriticalUsageWatcher#criticalCpuUsage:1800` will send "criticalCpuUsage" events mails only every 30 minutes minimum.

A cooldown of `-1` forbid to send mail for mapped events.

Default value : `.*RequestLogFilter#.*:-1,.*WriteAllRequestsWatcher#.*:-1,.*:1800`

### `mailSmtpHost`
SMTP host for alert mails.

Default value : _empty_

### `mailSmtpPort`
SMTP host's port for alert mails.

Default value : `25`

### `mailSmtpAuth`
If an authentication is required for the SMTP server (boolean : yes/true/no/false/_empty_ is accepted).

Default value : _empty_

### `mailSmtpUsername`
Username for the SMTP server authentication.

Default value : _empty_

### `mailSmtpPassword`
Password for the SMTP server authentication.

Default value : _empty_

### `mailSmtpSecurityType`
SMTP authentication type : `SSL` or `TLS`.

Default value : _empty_

### `mailFrom`
Source email address for alert mails.

Default value : _empty_

### `mailTo`
Destination email address(es) for alert mails.

Default value : _empty_


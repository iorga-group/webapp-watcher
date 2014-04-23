---
layout: full-page
title: Overview
---

Webapp Watcher analysis is split into two phases.

## Logging
The first one is the logging.

Webapp Watcher Filter listens and logs every HTTP requests that come to the application.

In parallel, it registers the JVM stacktraces of each targeted threads and logs them too.

![WAW Filter Schema]({{ site.baseurl }}/img/waw/waw-filter-schema.png "WAW Filter Schema")

## Analysing
The second phase is the analysis itself.

WAW-Analyzer is a software you run outside of WAW Filter.

You load the logs created by WAW Filter and then you can display metrics (for exemple median response time), and for a given long request, display it stacktrace time tree and find potential performance issues directly in your code.

![WAW Analyzer Schema]({{ site.baseurl }}/img/waw-analyzer/waw-analyzer-schema.png "WAW Analyzer Schema")

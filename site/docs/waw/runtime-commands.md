---
layout: docs
title: Runtime commands
permalink: runtime-commands/
lang: en
---

The Log Filter can be commanded remotly.

Here are the commands :

Command name | Action
------------ | ------
`stopAll` | Stops the events logging system (all its services)
`startAll` | Starts the events logging system (all its services)
`writeRetentionLog` | Asks for an immediate full event log writing. (Those will be flushed to disks in the log file).
`closeRetentionLog` | Closes the current log file. Another one will be created at the next log writing.
`downloadEventLog` | Downloads the current log file.
`printParameters` | Displays the current parameter values.
`printInfos` | Displays different informations (length of log file on the disk).
`changeParameters` | Modifies parameter values (all at the same time, with an HTTP POST, every parameter in the form is described in the [dedicated page](../configuration/)).
`printHtmlCommands` | Displays all those commands with a button to execute each one.

Example of a call : [http://localhost:8080/**yourapp**/RequestLogFilterCmd/**printParameters**](http://localhost:8080/yourapp/RequestLogFilterCmd/printParameters)


---
layout: docs
title: Usage
permalink: usage/
lang: en
---

## Process to find performance issues in your webapp
Go to `Analyze` > `Requests times and stacks` and [follow the documentation of that feature.](#analyze-requests-times-and-stacks).

## Analyze
Features available in `Analyze` menu.

Mainly used to display requests, filtering them, access to more details, and explore the code which was involved on those requests in order to find potential performance issues.

### Requests by specific query
Here you can type in a JS script which will return `true` or `false` if you want to keep the request in the results or not.

You can use the following variables in that script :

 * `headers`: a [`Set<String>`](http://docs.oracle.com/javase/7/docs/api/java/util/Set.html) of the request headers
 * `parameters`: a [`Map<String, Set<String>>`](http://docs.oracle.com/javase/7/docs/api/java/util/Map.html) of the request parameters
 * `request`: the [`RequestEventLog`](https://github.com/iorga-group/webapp-watcher/blob/master/webappwatcher/src/main/java/com/iorga/webappwatcher/eventlog/RequestEventLog.java)
 * `requestContainer`: the [`RequestsTimesAndStacks.RequestContainer`](https://github.com/iorga-group/webapp-watcher/blob/master/waw-analyzer/src/main/java/com/iorga/webappwatcher/analyzer/model/session/RequestsTimesAndStacks.java)

Here is a sample script :

```js
var setContains = function(set, containsFn) {
	if (!set) return false;
	var it = set.iterator();
	while (it.hasNext()) {
		if (containsFn(it.next())) return true;
	}
}
var match = setContains(parameters.get('myform:mybutton'), function(value) {return value.indexOf('SEARCH') > -1;});
request.method == 'POST' && request.requestURI.indexOf('home.xhtml') > -1 && match
```

Or more simple :

```js
'myuser'.equals(request.getPrincipal())
```

On a request line you can display the details of the request.

You can also download the result as an .csv or .xlsx file.

### <a name="analyze-requests-times-and-stacks"></a>Requests times and stacks
Here the requests are grouped by "URL".

Order results by descending mean or median time (by clicking on the column name you want).

![Requests times and stacks]({{ site.baseurl }}/img/waw-analyzer/analyze-requests-times-and-stacks.png "Requests times and stacks")

You can display the code stacks involved in the requests by group (click on a request group > `Stacks grouped`) or for a specific request (click on a request group > `Stacks per request`, then select your request, and display details or stacks for that request.

To find potential performance issues in the code, click on the worst line and display the stacks.

![Requests grouped stacks]({{ site.baseurl }}/img/waw-analyzer/requests-grouped-stacks.png "Requests grouped stacks")

Here you can travel through the code. Open every line which have the max number of stacks (at the right, which corresponds to the time passed in that code).

With that technic, you can go to the root of the performance issue.

### All requests list
The list of all requests.

Can be slow to display depending on the data you have in your logs.

Once displayed, you can filter the requests for a given principal (user name), and ask for `only actions` which will filter out "static" requests (like .jpg or .css requests).

On a request line you can display the details of the request.

## Statistics

### Time slice list
List of time slices which are used for graphs and daily statistics.

For each time slice, here are the information:

 * `startDate`: Start date
 * `endDate`: End date
 * `distinctUsers`: Number of distinct users count on that time slice. Based on the principal (user name) of the request.
 * `numberOfRequests`: Total number of requests count
 * `durationsFor1click*`: Represents the times between a request arrival on the server and its reply. Concerns only the "actions" of the users, and the ["type of webapp" configuration](../configuration/#type-of-webapp) determines what is a "user action" and what is not. Requests for static resources are not considered "user action" for example.
   * `durationsFor1clickSum`: Sum of those times in the time slice
   * `durationsFor1clickMean`: Mean of those times in the time slice
   * `durationsFor1clickMedian`: Median of those times in the time slice
   * `durationsFor1click90c`: 90th percentile of those times in the time slice
   * `durationsFor1clickMin`: 90th percentile of those times in the time slice
   * `durationsFor1clickMax`: 90th percentile of those times in the time slice

### Daily statistics
Group the time slice list previously defined by day.

Gives for each day, the `min`, `max`, `mean` and `median` of each figures of the time slices.

Also displays the number of distinct users for that day.

## Graphs

### Request graph
Displays the graph of all loaded event log files.

![Requests time statistics]({{ site.baseurl }}/img/waw-analyzer/requests-graph.jpg "Requests time statistics")

You can zoom in/out by scrolling, and move left/right by left/right drag & drop.

By clicking on the `Change displayed series`, you can display those series (each point corresponds to a time slice):

 * `x - y ms`: Number of requests (actions) which last between x and y milliseconds
 * `Memory (Mean)`: Mean memory usage
 * `CPU (Mean)`: Mean CPU usage
 * `Users`: Max number of active users
 * `Median`: Median of "durations for 1 click"

### Request graph per day
Same as previous graph, but displayed by day.

You can cut the displayed graphs by setting starting and ending time.

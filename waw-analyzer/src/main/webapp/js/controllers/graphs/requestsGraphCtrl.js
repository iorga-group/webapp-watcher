function RequestsGraphCtrl($scope, $http, irajMessageService, irajBreadcrumbsService, flotUtilsService) {
	/// Action methods ///
	/////////////////////
	
	/// Initialization ///
	/////////////////////
	irajBreadcrumbsService.setLastLabel('Requests graph');
	
	$http.get('api/graphs/requestsGraph/compute').success(function(data, status, headers, config) {
		function dateDoubleValueListToData(list) {
			var data = [];
			for (var i = 0 ; i < list.length ; i++) {
				data.push([list[i].date,list[i].doubleValue]);
			}
			return data;
		}
		// get the data
		var cpuUsageMeans = data.cpuUsageMeans,
			memoryUsageMeans = data.memoryUsageMeans,
			durationsFor1clickDispersionSeries = data.durationsFor1clickDispersionSeries,
			nbUsersMax = data.nbUsersMax,
			durationsFor1clickMedians = data.durationsFor1clickMedians;
		
		var series = [];
		// adding durations
		for (var i = 0 ; i < durationsFor1clickDispersionSeries.length ; i++) {
			var dispersionSerie = durationsFor1clickDispersionSeries[i];
			series.push({
				data: dateDoubleValueListToData(dispersionSerie.data),
				label: dispersionSerie.min+' - '+dispersionSerie.max,
				stack:true,
				lines:{show:true,fill:true}
			});
		}
		series.push({ data: dateDoubleValueListToData(memoryUsageMeans), label: "Memory (Mean)", yaxis: 3, color: '#0E660E'});
		series.push({ data: dateDoubleValueListToData(cpuUsageMeans), label: "CPU (Mean)", yaxis: 2, color: '#193C80'});
		series.push({ data: dateDoubleValueListToData(nbUsersMax), label: "Users", yaxis: 4, color: '#888888'});
		series.push({ data: dateDoubleValueListToData(durationsFor1clickMedians), label: "Median", yaxis: 5, color: '#000000'});
		
		var plot = $.plot($("#placeholder"), series,
	   		{
				xaxes: [{ mode: "time", timezone: "browser"}],
				yaxes: [
				        {axisLabel: 'nb of actions', zoomRange: false, panRange: false},	// durationsFor1clickSeries
				        {zoomRange: false, panRange: false, position: 'right', tickFormatter: flotUtilsService.cpuUsageFormatter},	// CPU
				        {zoomRange: false, panRange: false, position: 'right', tickFormatter: flotUtilsService.memoryFormatter},		// Memory
				        {axisLabel: 'nb of users', zoomRange: false, panRange: false},		// Users
				        {axisLabel: 'milliseconds', zoomRange: false, panRange: false}		// Median
				        ],
				crosshair: { mode : "x" },
				grid: { hoverable: true, clickable: true, autoHighlight: false },
				legend: { position: 'nw' },
		        zoom: {
		            interactive: true
		        },
		        pan: {
		            interactive: true
		        }
			}
		);
		
		flotUtilsService.addUpdateLegendsOnPlotHoverFunction(plot);
	});
}
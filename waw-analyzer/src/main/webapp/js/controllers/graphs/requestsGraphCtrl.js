function RequestsGraphCtrl($scope, $http, irajMessageService, irajBreadcrumbsService, flotUtilsService, graphUtilsService) {
	/// Action methods ///
	/////////////////////
	$scope.redraw = function() {
		var series = [];
		for (var i = 0 ; i < $scope.series.length ; i++) {
			if ($scope.series[i].displayed) {
				series.push($scope.series[i]);
			}
		}
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
	}
	
	/// Initialization ///
	/////////////////////
	irajBreadcrumbsService.setLastLabel('Requests graph');
	
	$http.get('api/graphs/requestsGraph/compute').success(function(data, status, headers, config) {
		$scope.series = graphUtilsService.createSeriesFromRequestsGraphComputeData(data);
		
		$scope.redraw();
	});
}
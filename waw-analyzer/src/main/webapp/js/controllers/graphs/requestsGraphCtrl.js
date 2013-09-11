/*
 * Copyright (C) 2013 Iorga Group
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/].
 */
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
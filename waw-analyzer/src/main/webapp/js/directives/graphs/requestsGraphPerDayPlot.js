'use strict';

angular.module('requestsGraphPerDayPlot', [])
	.directive('requestsGraphPerDayPlot', function($parse, flotUtilsService, $timeout) {
		return {
			link : function(scope, element, attrs) {
				var serieTypes = scope.serieTypes,
					currentSeriePerDay = $parse(attrs.requestsGraphPerDayPlot)(scope),
					xaxis = scope.xaxis;

				function computeSeriesToDisplay() {
					var series = [];
					for (var i = 0 ; i < serieTypes.length ; i++) {
						var serieType = serieTypes[i];
						// adding the displayed serie
						var serie = currentSeriePerDay.seriesByLabel[serieType.label];
						if (serieType.displayed) {
							series.push(serie);
						}
					}
					return series;
				}
				
				// method to redraw
				function redraw() {
					// create the plot
					var series = computeSeriesToDisplay(),
						yaxes = scope.yaxes;
					
					var plot = $.plot(element, series,
				   		{
							xaxes: [{ mode: "time", timezone: "browser",
								min: currentSeriePerDay.firstMoment.hours(xaxis.min.hours).minutes(xaxis.min.minutes).seconds(xaxis.min.seconds).valueOf(),
								max: currentSeriePerDay.firstMoment.hours(xaxis.max.hours).minutes(xaxis.max.minutes).seconds(xaxis.max.seconds).valueOf()}],
							yaxes: [
							        {min: yaxes[1].min, max: yaxes[1].max, axisLabel: 'nb of actions'},	// durationsFor1clickSeries
							        {min: yaxes[2].min, max: yaxes[2].max, position: 'right', tickFormatter: flotUtilsService.cpuUsageFormatter},	// CPU
							        {min: yaxes[3].min, max: yaxes[3].max, position: 'right', tickFormatter: flotUtilsService.memoryFormatter},	// Memory
							        {min: yaxes[4].min, max: yaxes[4].max, axisLabel: 'nb of users'},		// Users
							        {min: yaxes[5].min, max: yaxes[5].max, axisLabel: 'milliseconds'}		// Median
							        ],
							crosshair: { mode : "x" },
							grid: { hoverable: true, clickable: true, autoHighlight: false },
							legend: { position: 'nw' }
						}
					);
					
					flotUtilsService.addUpdateLegendsOnPlotHoverFunction(plot);
					
					/* doesn't work : the legends are buggy 
					// now redraw it
					var plot = element.data("plot");
					plot.setData(computeSeriesToDisplay());
					plot.setupGrid(); // recompute the labels
					plot.draw();
					*/
				}
				
				// watch for "displayed" modification on serieTypes
				scope.$watch(function() {
					// will return the dumber of displayed series
					var nb = 0;
					for (var i = 0 ; i < serieTypes.length ; i++) {
						var serieType = serieTypes[i];
						if (serieType.displayed) {
							nb++
						}
					}
					return nb;
				}, function(newValue, oldValue) {
					if (newValue != oldValue) {
						redraw();
					}
				});
				// watch for min & max time change
				var previousTimeoutPromise;
				scope.$watch(function() {
					return xaxis.min.hours+':'+xaxis.min.minutes+':'+xaxis.min.seconds+'-'+xaxis.max.hours+':'+xaxis.max.minutes+':'+xaxis.max.seconds;
				}, function() {
					if (previousTimeoutPromise) $timeout.cancel(previousTimeoutPromise);
					previousTimeoutPromise = $timeout(redraw, 1000);
				});
				
				redraw();
			}
		};
	})
;

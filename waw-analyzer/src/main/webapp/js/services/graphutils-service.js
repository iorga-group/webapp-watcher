'use strict';

angular.module('graphutils-service', [])
	.factory('graphUtilsService', function() {
		var graphUtilsService = {};
		
		graphUtilsService.createSeriesFromRequestsGraphComputeData = function(data) {
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
					displayed: true,
					data: dateDoubleValueListToData(dispersionSerie.data),
					label: dispersionSerie.min+' - '+dispersionSerie.max+' ms',
					stack:true,
					lines:{show:true,fill:true}
				});
			}
			series.push({ data: dateDoubleValueListToData(memoryUsageMeans), label: "Memory (Mean)", yaxis: 3, color: '#0E660E'});
			series.push({ data: dateDoubleValueListToData(cpuUsageMeans), label: "CPU (Mean)", yaxis: 2, color: '#193C80'});
			series.push({ data: dateDoubleValueListToData(nbUsersMax), label: "Users", yaxis: 4, color: '#888888'});
			series.push({ data: dateDoubleValueListToData(durationsFor1clickMedians), label: "Median", yaxis: 5, color: '#000000'});
			
			return series;
		}
		
		return graphUtilsService;
	})
;

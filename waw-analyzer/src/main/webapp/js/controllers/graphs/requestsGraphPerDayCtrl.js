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
function RequestsGraphPerDayCtrl($scope, $http, irajMessageService, irajBreadcrumbsService, flotUtilsService, graphUtilsService) {
	/// Action methods ///
	/////////////////////
	
	/// Initialization ///
	/////////////////////
	irajBreadcrumbsService.setLastLabel('Requests graph per day');
	
	$http.get('api/graphs/requestsGraph/compute').success(function(data, status, headers, config) {
		var series = graphUtilsService.createSeriesFromRequestsGraphComputeData(data),
			serieTypes = [];
		
		// now compute the series per day
		var seriesPerDayByDateName = {},
			seriesPerDay = [],
			currentSeriePerDay,
			yaxes = {}; // will store the min & max of all series
			xaxis = {min: {hours: 23, minutes: 59, seconds: 59}, max: {hours: 0, minutes: 0, seconds: 0}};
		
		for (var i = 0 ; i < series.length ; i++) {
			var serie = series[i],
				data = serie.data;
			// adding type
			serieTypes.push({
				label: serie.label,
				displayed: serie.displayed,
				color: serie.color
			});
			// handling data
			for (var j = 0 ; j < data.length ; j++) {
				var currentData = data[j];
				var currentDateTime = moment(currentData[0]);
				var dateName = currentDateTime.format('dddd LL');
				var currentSeriePerDay = seriesPerDayByDateName[dateName];
				if (!currentSeriePerDay) {
					// new seriePerDay
					currentSeriePerDay = {
						dateName: dateName,
						firstMoment: currentDateTime,
						id: Object.keys(seriesPerDayByDateName).length,
						seriesByLabel: {},
						durationsFor1clickSum: {}
					};
					seriesPerDayByDateName[dateName] = currentSeriePerDay;
					seriesPerDay.push(currentSeriePerDay);
				}
				var currentSerie = currentSeriePerDay.seriesByLabel[serie.label];
				if (!currentSerie) {
					// create current serie
					currentSerie = {
						data: [],
						label: serie.label,
						stack: serie.stack,
						lines: serie.lines,
						yaxis: serie.yaxis,
						color: serie.color
					};
					currentSeriePerDay.seriesByLabel[serie.label] = currentSerie;
				}
				// add the current data to the current serie
				currentSerie.data.push(currentData);
				// compute the min & max value
				var yaxisIndex = serie.yaxis || 1,
					yaxis = yaxes[yaxisIndex],
					value = currentData[1];
				if (!yaxis) {
					// create the yaxis object
					yaxis = {min: Number.MAX_VALUE, max: Number.MIN_VALUE};
					yaxes[yaxisIndex] = yaxis;
				}
				if (yaxisIndex == 1) {
					// it's a durationsFor1click serie's data, we must add that value to the durationsFor1clickSum for that date
					var sum = currentSeriePerDay.durationsFor1clickSum[j] || 0;
					sum += value;
					currentSeriePerDay.durationsFor1clickSum[j] = sum;
					value = sum;
				}
				if (value < yaxis.min) {
					yaxis.min = value;
				}
				if (value > yaxis.max) {
					yaxis.max = value;
				}
				// compute the min & max time
				var hours = currentDateTime.hours(),
					minutes = currentDateTime.minutes(),
					seconds = currentDateTime.seconds();
				if (hours < xaxis.min.hours || (hours == xaxis.min.hours && minutes < xaxis.min.minutes) || (hours == xaxis.min.hours && minutes == xaxis.min.minutes && seconds < xaxis.min.seconds)) {
					// new min
					xaxis.min.hours = hours;
					xaxis.min.minutes = minutes;
					xaxis.min.seconds = seconds;
				}
				if (hours > xaxis.max.hours || (hours == xaxis.max.hours && minutes > xaxis.max.minutes) || (hours == xaxis.max.hours && minutes == xaxis.max.minutes && seconds > xaxis.max.seconds)) {
					// new max
					xaxis.max.hours = hours;
					xaxis.max.minutes = minutes;
					xaxis.max.seconds = seconds;
				}
			}
		}
		
		$scope.serieTypes = serieTypes;
		$scope.seriesPerDay = seriesPerDay;
		$scope.yaxes = yaxes;
		$scope.xaxis = xaxis;
	});
}
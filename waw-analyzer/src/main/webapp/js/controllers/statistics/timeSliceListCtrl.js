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
function TimeSliceListCtrl($scope, $http, irajMessageService, irajBreadcrumbsService) {
	/// Action methods ///
	/////////////////////
	
	/// Initialization ///
	/////////////////////
	irajBreadcrumbsService.setLastLabel('Time slice list');
	
	$scope.statisticTypes = ['startDate', 'endDate', 'distinctUsers', 'numberOfRequests', 'durationsFor1clickSum', 'durationsFor1clickMean', 'durationsFor1clickMedian', 'durationsFor1click90c', 'durationsFor1clickMin', 'durationsFor1clickMax'];
	
	$http.get('api/statistics/timeSliceList/compute').success(function(data, status, headers, config) {
		var timeSlices = [];
		// init dates
		for (var i = 0 ; i < data.length ; i++) {
			var timeSlice = data[i];
			if (timeSlice.numberOfRequests > 0) { // filtering out 0 requests timeSlices
				// format date
				timeSlice.startDate = moment(timeSlice.startDate).format('L LT');
				timeSlice.endDate = moment(timeSlice.endDate).format('L LT');
				
				timeSlices.push(timeSlice);
			}
		}
		$scope.timeSlices = timeSlices;
	});
}
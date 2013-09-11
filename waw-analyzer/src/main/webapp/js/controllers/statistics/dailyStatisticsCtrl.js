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
function DailyStatisticsCtrl($scope, $http, irajMessageService, irajBreadcrumbsService) {
	/// Action methods ///
	/////////////////////
	
	/// Initialization ///
	/////////////////////
	irajBreadcrumbsService.setLastLabel('Daily Statistics');
	
	$scope.statisticRowTypes = ['min', 'max', 'mean', 'median'];
	
	$http.get('api/statistics/dailyStatistics/compute').success(function(data, status, headers, config) {
		var dayStatistics = data;
		// add a field indicating the date name of each day
		for (var i = 0 ; i < dayStatistics.length ; i++) {
			dayStatistics[i].dateName = moment(dayStatistics[i].startDate).format('dddd LL');
		}
		
		$scope.dayStatistics = dayStatistics;
	});
}
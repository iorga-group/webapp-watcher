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
function router($routeProvider) {
	$routeProvider
		.when('/', {controller:HomeCtrl, templateUrl: 'templates/views/home.html'})
		.when('/session/uploadedFiles', {controller:UploadedFilesCtrl, templateUrl: 'templates/views/session/uploadedFiles.html'})
		.when('/session/configurations', {controller:ConfigurationsCtrl, templateUrl: 'templates/views/session/configurations.html'})
		.when('/analyze/requestsTimesAndStacks', {controller:RequestsTimesAndStacksCtrl, templateUrl: 'templates/views/analyze/requestsTimesAndStacks.html'})
		.when('/analyze/groupedStacks/:requestId', {controller:GroupedStacksCtrl, templateUrl: 'templates/views/analyze/groupedStacks.html'})
		.when('/analyze/groupedStacks/:requestId/:requestIndex', {controller:GroupedStacksCtrl, templateUrl: 'templates/views/analyze/groupedStacks.html'})
		.when('/analyze/perRequestStacksList/:requestId', {controller:PerRequestStacksListCtrl, templateUrl: 'templates/views/analyze/perRequestStacksList.html'})
		.when('/analyze/allRequestList', {controller:AllRequestListCtrl, templateUrl: 'templates/views/analyze/allRequestList.html'})
		.when('/statistics/daily', {controller:DailyStatisticsCtrl, templateUrl: 'templates/views/statistics/dailyStatistics.html'})
		.when('/statistics/timeSliceList', {controller:TimeSliceListCtrl, templateUrl: 'templates/views/statistics/timeSliceList.html'})
		.when('/graphs/requestsGraph', {controller:RequestsGraphCtrl, templateUrl: 'templates/views/graphs/requestsGraph.html'})
		.when('/graphs/requestsGraphPerDay', {controller:RequestsGraphPerDayCtrl, templateUrl: 'templates/views/graphs/requestsGraphPerDay.html'})
};
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
function RequestsTimesAndStacksCtrl($http, $scope, irajTableService, irajBreadcrumbsService) {
	/// Action methods ///
	/////////////////////
	$scope.goToGroupedStacks = function(request) {
		irajBreadcrumbsService.changePathAndPush($scope, '/analyze/groupedStacks/'+request.id);
	}
	
	$scope.goToPerRequestStacksList = function(request) {
		irajBreadcrumbsService.changePathAndPush($scope, '/analyze/perRequestStacksList/'+request.id);
	}
	
	/// Initialization ///
	/////////////////////
	irajBreadcrumbsService.setLastLabel('Requests times and stacks');
	
	irajTableService.initTable('tableParams', 'requests', 'orderedRequests', $scope);
	
	if (irajBreadcrumbsService.shouldLoadFromLastScope()) {
		var lastScope = irajBreadcrumbsService.getLast().scope;
		$scope.tableParams = lastScope.tableParams;
		$scope.requests = lastScope.requests;
	} else {
		$http.get('api/analyze/requestsTimesAndStacks/compute', {irajClearAllMessages: true})
			.success(function(data, status, headers, config) {
				$scope.requests = data;
			})
		;
	}
}

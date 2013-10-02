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
function RequestsByQuery($http, $scope, irajTableService, irajBreadcrumbsService, $routeParams, requestUtilsService) {
	/// Action methods ///
	/////////////////////
	
	$scope.displayDetails = function(request) {
		$http.get('api/analyze/allRequestList/requestDetails/'+request.index).success(function(requestDetails) {
			requestUtilsService.showRequestDetailsModal(requestDetails, $scope);
		});
	}
	
	$scope.executeQuery = function() {
		// bug corrected from http://stackoverflow.com/a/11443066/535203
		$http.post('api/analyze/requestsByQuery/compute', $.param({query: $scope.query}), {irajClearAllMessages: true, headers: {'Content-Type': 'application/x-www-form-urlencoded'}})
			.success(function(requests) {
				// format them
				requestUtilsService.formatRequestList(requests);
				// set the new requests to the scope
				$scope.requests = requests;
			});
	}
	
	/// Initialization ///
	/////////////////////
	irajBreadcrumbsService.setLastLabel('Requests by query');
	
	irajTableService.initTable('tableParams', 'requests', 'orderedRequests', $scope);
	
	if (irajBreadcrumbsService.shouldLoadFromLastScope()) {
		var lastScope = irajBreadcrumbsService.getLast().scope;
		$scope.tableParams = lastScope.tableParams;
		$scope.requests = lastScope.requests;
	}
}

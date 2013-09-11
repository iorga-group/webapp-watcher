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
function AllRequestListCtrl($http, $scope, irajTableService, irajBreadcrumbsService, $routeParams, requestUtilsService) {
	/// Action methods ///
	/////////////////////
	/*
	$scope.goToGroupedStacks = function(request) {
		irajBreadcrumbsService.changePathAndPush($scope, '/analyze/groupedStacks/'+$scope.requestId+'/'+request.index);
	}
	*/
	
	$scope.displayDetails = function(request) {
		$http.get('api/analyze/allRequestList/requestDetails/'+request.index).success(function(requestDetails) {
			requestUtilsService.showRequestDetailsModal(requestDetails, $scope);
		});
	}
	
	$scope.doFilter = function() {
		var requests = [],
			allRequests = $scope.allRequests,
			allRequestsLength = allRequests.length,
			filter = $scope.filter,
			actionFilter = requestUtilsService.jsf21AndRichFaces4RequestActionFilter;
		for (var i = 0 ; i < allRequestsLength ; i++) {
			var request = allRequests[i],
				included = true;
			if (included && filter.onlyActions) {
				included = included && actionFilter(request);
			}
			if (included && filter.principal) {
				included = included && request.principal === filter.principal;
			}
			if (included) {
				// filter passed, add it to the final list
				requests.push(request);
			}
		}
		// set the new requests to the scope
		$scope.requests = requests;
	}
	
	/// Initialization ///
	/////////////////////
	irajBreadcrumbsService.setLastLabel('All requests list');
	
	irajTableService.initTable('tableParams', 'requests', 'orderedRequests', $scope);
	
	if (irajBreadcrumbsService.shouldLoadFromLastScope()) {
		var lastScope = irajBreadcrumbsService.getLast().scope;
		$scope.tableParams = lastScope.tableParams;
		$scope.requests = $scope.allRequests = lastScope.requests;
	} else {
		// Load the requests
		var requestId = $routeParams.requestId;
		$http.get('api/analyze/allRequestList/compute', {irajClearAllMessages: true})
			.success(function(requests) {
				var requestsInfos = requestUtilsService.formatRequestList(requests),
					filter = {},
					minMoment = moment(requestsInfos.minDateTime),
					maxMoment = moment(requestsInfos.maxDateTime);
				
				filter.min = {
					
				}
				
				$scope.principals = requestsInfos.principals;
				$scope.filter = filter;
				$scope.requests = $scope.allRequests = requests;
			});
	}
}

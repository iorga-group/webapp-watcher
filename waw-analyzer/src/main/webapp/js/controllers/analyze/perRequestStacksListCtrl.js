function PerRequestStacksListCtrl($http, $scope, irajTableService, irajBreadcrumbsService, $routeParams, requestUtilsService) {
	/// Action methods ///
	/////////////////////
	$scope.goToGroupedStacks = function(request) {
		irajBreadcrumbsService.changePathAndPush($scope, '/analyze/groupedStacks/'+$scope.requestId+'/'+request.index);
	}
	
	$scope.displayDetails = function(request) {
		$http.get('api/analyze/perRequestStacksList/requestDetails/'+$scope.requestId+'/'+request.index).success(function(requestDetails) {
			requestUtilsService.showRequestDetailsModal(requestDetails, $scope);
		});
	}
	
	/// Initialization ///
	/////////////////////
	irajBreadcrumbsService.setLastLabel('Per request stacks list');
	
	irajTableService.initTable('tableParams', 'slowRequests', 'orderedRequests', $scope);
	
	if (irajBreadcrumbsService.shouldLoadFromLastScope()) {
		var lastScope = irajBreadcrumbsService.getLast().scope;
		$scope.tableParams = lastScope.tableParams;
		$scope.requestUrl = lastScope.requestUrl;
		$scope.requestId = lastScope.requestId;
		$scope.slowRequests = lastScope.slowRequests;
	} else {
		// Load the requests
		var requestId = $routeParams.requestId;
		$http.get('api/analyze/perRequestStacksList/compute/'+requestId, {irajClearAllMessages: true})
			.success(function(data) {
				$scope.requestUrl = data.url;
				$scope.requestId = data.id;
				var slowRequests = data.slowRequests;
				
				requestUtilsService.formatRequestList(slowRequests);
				
				$scope.slowRequests = slowRequests;
			});
	}
}

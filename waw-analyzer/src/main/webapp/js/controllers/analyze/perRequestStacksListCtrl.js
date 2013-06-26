function PerRequestStacksListCtrl($http, $scope, irajTableService, irajBreadcrumbsService, $routeParams) {
	/// Action methods ///
	/////////////////////
	
	/// Initialization ///
	/////////////////////
	irajBreadcrumbsService.setLastLabel('Per request stacks list');
	
	irajTableService.initTable('tableParams', 'slowRequests', 'orderedSlowRequests', $scope);
	
	if (irajBreadcrumbsService.shouldLoadFromLastScope()) {
		var lastScope = irajBreadcrumbsService.getLast().scope;
		$scope.tableParams = lastScope.tableParams;
		$scope.requests = lastScope.requests;
	} else {
		// Load the requests
		var requestId = $routeParams.requestId;
		$http.get('api/analyze/perRequestStacksList/compute/'+requestId, {irajClearAllMessages: true})
			.success(function(data) {
				$scope.url = data.url;
				$scope.slowRequests = data.slowRequests;
			});
	}
}

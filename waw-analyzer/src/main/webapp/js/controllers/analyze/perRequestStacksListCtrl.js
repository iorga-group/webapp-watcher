function PerRequestStacksListCtrl($http, $scope, irajTableService, irajBreadcrumbsService, $routeParams) {
	/// Action methods ///
	/////////////////////
	$scope.goToGroupedStacks = function(request) {
		irajBreadcrumbsService.changePathAndPush($scope, '/analyze/groupedStacks/'+$scope.requestId+'/'+request.index);
	}
	
	/// Initialization ///
	/////////////////////
	irajBreadcrumbsService.setLastLabel('Per request stacks list');
	
	irajTableService.initTable('tableParams', 'slowRequests', 'orderedSlowRequests', $scope);
	
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
				// Add the index
				for (var i = 0 ; i < data.slowRequests.length ; i++) {
					slowRequests[i].index = i;
				}
				$scope.slowRequests = slowRequests;
			});
	}
}
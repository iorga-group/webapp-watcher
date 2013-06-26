function RequestsTimesAndStacksCtrl($http, $scope, irajTableService, irajBreadcrumbsService) {
	/// Action methods ///
	/////////////////////
	$scope.compute = function () {
		$http.get('api/analyze/requestsTimesAndStacks/compute/3000', {irajClearAllMessages: true})
			.success(function(data, status, headers, config) {
				$scope.requests = data;
			})
		;
	}
	
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
	}
}

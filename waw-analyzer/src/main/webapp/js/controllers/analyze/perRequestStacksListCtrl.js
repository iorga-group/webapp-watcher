function PerRequestStacksListCtrl($http, $scope, irajTableService, irajBreadcrumbsService, $routeParams) {
	/// Action methods ///
	/////////////////////
	$scope.goToGroupedStacks = function(request) {
		irajBreadcrumbsService.changePathAndPush($scope, '/analyze/groupedStacks/'+$scope.requestId+'/'+request.index);
	}
	
	$scope.displayDetails = function(request) {
		$http.get('api/analyze/perRequestStacksList/requestDetails/'+$scope.requestId+'/'+request.index).success(function(data) {
			var requestDetails = data,
				dateFormat = 'ddd, L HH:mm:ss';
			requestDetails.startDateDisplay = moment(requestDetails.startDate).format(dateFormat);
			requestDetails.endDateDisplay = moment(requestDetails.endDate).format(dateFormat);
			$scope.requestDetails = requestDetails;
			$('#requestDetailsModal').modal('show');
		});
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
					var slowRequest = slowRequests[i];
					slowRequest.index = i;
					slowRequest.startDateDisplay = moment(slowRequest.startDate).format('llll');
					slowRequest.endDateDisplay = moment(slowRequest.endDate).format('llll');
				}
				$scope.slowRequests = slowRequests;
			});
	}
}

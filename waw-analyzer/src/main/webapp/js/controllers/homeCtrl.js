function HomeCtrl($scope, $http, irajBreadcrumbsService) {
	
	/// Initialization ///
	/////////////////////
	$http.get('api/home/versions').success(function(data) {
		$scope.versions = data;
	});
	irajBreadcrumbsService.setLastLabel('WAW - Analyzer');
}
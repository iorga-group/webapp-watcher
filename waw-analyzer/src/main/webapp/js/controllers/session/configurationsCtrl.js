function ConfigurationsCtrl($scope, $http, irajMessageService, irajBreadcrumbsService) {
	/// Action methods ///
	/////////////////////
	$scope.save = function() {
		$http.post('api/session/configurations/save', $scope.configurations, {irajMessagesIdPrefix: 'configurations', irajClearAllMessages: true})
		.success(function(data, status, headers, config) {
			irajMessageService.displayMessage({message: "Configuration saved", type: 'success'}, 'configurations');
		});
	}
	
	/// Initialization ///
	/////////////////////
	irajBreadcrumbsService.setLastLabel('Session configurations');
	
	$http.get('api/session/configurations/load').success(function(data, status, headers, config) {
		$scope.configurations = data;
	});
}
function DailyStatisticsCtrl($scope, $http, irajMessageService, irajBreadcrumbsService) {
	/// Action methods ///
	/////////////////////
	
	/// Initialization ///
	/////////////////////
	irajBreadcrumbsService.setLastLabel('Daily Statistics');
	
	$scope.statisticRowTypes = ['min', 'max', 'mean', 'median'];
	
	$http.get('api/statistics/dailyStatistics/compute').success(function(data, status, headers, config) {
		var dayStatistics = data;
		// add a field indicating the date name of each day
		for (var i = 0 ; i < dayStatistics.length ; i++) {
			dayStatistics[i].dateName = moment(dayStatistics[i].startDate).format('dddd LL');
		}
		
		$scope.dayStatistics = dayStatistics;
	});
}
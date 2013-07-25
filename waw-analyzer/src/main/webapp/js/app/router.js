function router($routeProvider) {
	$routeProvider
		.when('/', {controller:HomeCtrl, templateUrl: 'templates/views/home.html'})
		.when('/session/uploadedFiles', {controller:UploadedFilesCtrl, templateUrl: 'templates/views/session/uploadedFiles.html'})
		.when('/session/configurations', {controller:ConfigurationsCtrl, templateUrl: 'templates/views/session/configurations.html'})
		.when('/analyze/requestsTimesAndStacks', {controller:RequestsTimesAndStacksCtrl, templateUrl: 'templates/views/analyze/requestsTimesAndStacks.html'})
		.when('/analyze/groupedStacks/:requestId', {controller:GroupedStacksCtrl, templateUrl: 'templates/views/analyze/groupedStacks.html'})
		.when('/analyze/groupedStacks/:requestId/:requestIndex', {controller:GroupedStacksCtrl, templateUrl: 'templates/views/analyze/groupedStacks.html'})
		.when('/analyze/perRequestStacksList/:requestId', {controller:PerRequestStacksListCtrl, templateUrl: 'templates/views/analyze/perRequestStacksList.html'})
		.when('/statistics/daily', {controller:DailyStatisticsCtrl, templateUrl: 'templates/views/statistics/dailyStatistics.html'})
		.when('/statistics/timeSliceList', {controller:TimeSliceListCtrl, templateUrl: 'templates/views/statistics/timeSliceList.html'})
		.when('/graphs/requestsGraph', {controller:RequestsGraphCtrl, templateUrl: 'templates/views/graphs/requestsGraph.html'})
		.when('/graphs/requestsGraphPerDay', {controller:RequestsGraphPerDayCtrl, templateUrl: 'templates/views/graphs/requestsGraphPerDay.html'})
};
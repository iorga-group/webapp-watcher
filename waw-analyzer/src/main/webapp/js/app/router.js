function router($routeProvider) {
	$routeProvider
		.when('/', {controller:HomeCtrl, templateUrl: 'templates/views/home.html'})
		.when('/session/uploadedFiles', {controller:UploadedFilesCtrl, templateUrl: 'templates/views/session/uploadedFiles.html'})
		.when('/analyze/requestsTimesAndStacks', {controller:RequestsTimesAndStacksCtrl, templateUrl: 'templates/views/analyze/requestsTimesAndStacks.html'})
		.when('/analyze/groupedStacks/:requestId', {controller:GroupedStacksCtrl, templateUrl: 'templates/views/analyze/groupedStacks.html'})
		.when('/analyze/perRequestStacksList/:requestId', {controller:PerRequestStacksListCtrl, templateUrl: 'templates/views/analyze/perRequestStacksList.html'})
};
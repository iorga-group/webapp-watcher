function router($routeProvider) {
	$routeProvider
		.when('/', {controller:HomeCtrl, templateUrl: 'templates/views/home.html'})
		.when('/session/uploadedFiles', {controller:UploadedFilesCtrl, templateUrl: 'templates/views/session/uploadedFiles.html'})
		.when('/analyze/requestsTimesAndStacks', {controller:RequestsTimesAndStacksCtrl, templateUrl: 'templates/views/analyze/requestsTimesAndStacks.html'})
};
function TimeSliceListCtrl($scope, $http, irajMessageService, irajBreadcrumbsService) {
	/// Action methods ///
	/////////////////////
	
	/// Initialization ///
	/////////////////////
	irajBreadcrumbsService.setLastLabel('Time slice list');
	
	$scope.statisticTypes = ['startDate', 'endDate', 'distinctUsers', 'numberOfRequests', 'durationsFor1clickSum', 'durationsFor1clickMean', 'durationsFor1clickMedian', 'durationsFor1click90c', 'durationsFor1clickMin', 'durationsFor1clickMax'];
	
	$http.get('api/statistics/timeSliceList/compute').success(function(data, status, headers, config) {
		var timeSlices = data;
		// init dates
		for (var i = 0 ; i < timeSlices.length ; i++) {
			timeSlices[i].startDate = moment(timeSlices[i].startDate).format('L LT');
			timeSlices[i].endDate = moment(timeSlices[i].endDate).format('L LT');
		}
		$scope.timeSlices = timeSlices;
	});
}
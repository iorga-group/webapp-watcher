function TimeSliceListCtrl($scope, $http, irajMessageService, irajBreadcrumbsService) {
	/// Action methods ///
	/////////////////////
	
	/// Initialization ///
	/////////////////////
	irajBreadcrumbsService.setLastLabel('Time slice list');
	
	$scope.statisticTypes = ['startDate', 'endDate', 'distinctUsers', 'numberOfRequests', 'durationsFor1clickSum', 'durationsFor1clickMean', 'durationsFor1clickMedian', 'durationsFor1click90c', 'durationsFor1clickMin', 'durationsFor1clickMax'];
	
	$http.get('api/statistics/timeSliceList/compute').success(function(data, status, headers, config) {
		var timeSlices = [];
		// init dates
		for (var i = 0 ; i < data.length ; i++) {
			var timeSlice = data[i];
			if (timeSlice.numberOfRequests > 0) { // filtering out 0 requests timeSlices
				// format date
				timeSlice.startDate = moment(timeSlice.startDate).format('L LT');
				timeSlice.endDate = moment(timeSlice.endDate).format('L LT');
				
				timeSlices.push(timeSlice);
			}
		}
		$scope.timeSlices = timeSlices;
	});
}
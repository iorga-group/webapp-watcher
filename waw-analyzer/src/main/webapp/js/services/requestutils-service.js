'use strict';

angular.module('requestutils-service', [])
	.factory('requestUtilsService', function() {
		var requestUtilsService = {};
		
		requestUtilsService.showRequestDetailsModal = function(requestDetails, $scope) {
			var dateFormat = 'ddd, L HH:mm:ss';
			requestDetails.startDateDisplay = moment(requestDetails.startDate).format(dateFormat);
			requestDetails.endDateDisplay = moment(requestDetails.endDate).format(dateFormat);
			$scope.requestDetails = requestDetails;
			$('#requestDetailsModal').modal('show');
		};
		
		requestUtilsService.formatRequestList = function(requestList) {
			var principals = {},
				minTime = Number.MAX_VALUE,
				maxTime = 0;
			
			// Add the index
			for (var i = 0 ; i < requestList.length ; i++) {
				var request = requestList[i];
				request.index = i;
				// remember the min & max time
				if (request.startDate < minTime) minTime = request.startDate;
				if (request.startDate > maxTime) maxTime = request.startDate;
				// and format the times
				request.startDateDisplay = moment(request.startDate).format('llll');
				request.endDateDisplay = moment(request.endDate).format('llll');
				// store the principal
				principals[request.principal] = true;
			}
			// convert principals to array
			var principalsArray = [],
				principal;
			for (principal in principals) {
				principalsArray.push(principal);
			}
			return {
				principals: principalsArray,
				minDateTime: minTime,
				maxDateTime: maxTime
			};
		}
		
		requestUtilsService.jsf21AndRichFaces4RequestActionFilter = function(request) {
			var contains = function(str, searchStr) {
					return str && str.indexOf(searchStr) != -1;
				},
				startsWith = function(str, searchStr) {
					// http://stackoverflow.com/questions/646628/javascript-startswith - http://jsperf.com/js-startwith-prototype
					return str.slice(-searchStr.length) === searchStr;
				},
				ajaxSources = request.url.match(/POST:.*ajax\.source=(^&*)/),
				ajaxSource = ajaxSources && ajaxSources.length > 0 ? ajaxSources[0] : '';

			return !contains(request.url, '/javax.faces.resource/') && !contains(request.url, '/rfRes/')
				&& (startsWith(request.url, 'GET:') || !contains(ajaxSource, 'poller'));
		}
		
		return requestUtilsService;
	})
;
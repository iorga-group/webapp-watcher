/*
 * Copyright (C) 2013 Iorga Group
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/].
 */
function RequestsByQuery($http, $scope, irajTableService, irajBreadcrumbsService, $routeParams, requestUtilsService) {
	function b64toBlob(b64Data, contentType, sliceSize) {
		contentType = contentType || '';
		sliceSize = sliceSize || 1024;

		function charCodeFromCharacter(c) {
			return c.charCodeAt(0);
		}

		var byteCharacters = atob(b64Data);
		var byteArrays = [];

		for (var offset = 0; offset < byteCharacters.length; offset += sliceSize) {
			var slice = byteCharacters.slice(offset, offset + sliceSize);
			var byteNumbers = Array.prototype.map.call(slice, charCodeFromCharacter);
			var byteArray = new Uint8Array(byteNumbers);

			byteArrays.push(byteArray);
		}

		var blob = new Blob(byteArrays, {type: contentType});
		return blob;
	}
	
	/// Action methods ///
	/////////////////////
	
	$scope.displayDetails = function(request) {
		$http.get('api/analyze/allRequestList/requestDetails/'+request.index).success(function(requestDetails) {
			requestUtilsService.showRequestDetailsModal(requestDetails, $scope);
		});
	}
	
	$scope.executeQuery = function() {
		// bug corrected from http://stackoverflow.com/a/11443066/535203
		$http.post('api/analyze/requestsByQuery/compute', $.param({query: $scope.query}), {irajClearAllMessages: true, headers: {'Content-Type': 'application/x-www-form-urlencoded'}})
			.success(function(requests) {
				// format them
				requestUtilsService.formatRequestList(requests);
				// set the new requests to the scope
				$scope.requests = requests;
			});
	}
	
	$scope.downloadResultsAsCSV = function() {
		var csvStr = 'Principal;URL;Duration;Nb Stacks;Start Date;End Date\n',
			requests = $scope.requests;
		for (var i = 0 ; i < requests.length ; i++) {
			var request = requests[i];
			//csvStr += request.principal + ';' + request.url + ';' + request.duration + ';' + request.nbStacks + ';' + moment(request.startDate).toISOString() + ';' + moment(request.endDate).toISOString() + '\n';
			csvStr += request.principal + ';' + request.url + ';' + request.duration + ';' + request.nbStacks + ';' + request.startDate + ';' + request.endDate + '\n';
		}
		var blob = new Blob([csvStr], {type: "text/csv;charset=utf-8"});
		saveAs(blob, "results.csv");
	}
	
	$scope.downloadResultsAsXLSX = function() {
		function autoWidth(value) {
			return {value: value, autoWidth:true};
		}
		function formatDate(value) {
			return {value: moment(request.startDate).toDate(), formatCode: 'yyyy-dd-mm hh:mm:ss.000'};
		}
		var rows = [[autoWidth('Principal'), autoWidth('URL'), 'Duration', 'Nb Stacks', autoWidth('Start Date'), autoWidth('End Date')]],
			requests = $scope.requests;
		for (var i = 0 ; i < requests.length ; i++) {
			var request = requests[i];
			rows.push([request.principal, request.url, request.duration, request.nbStacks, formatDate(request.startDate), formatDate(request.endDate)]);
		}
		var sheet = xlsx({worksheets:[{data: rows}]});
		var blob = b64toBlob(sheet.base64, 'data:application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
		saveAs(blob, "results.xlsx");
	}
	
	/// Initialization ///
	/////////////////////
	irajBreadcrumbsService.setLastLabel('Requests by query');
	
	irajTableService.initTable('tableParams', 'requests', 'orderedRequests', $scope);
	
	if (irajBreadcrumbsService.shouldLoadFromLastScope()) {
		var lastScope = irajBreadcrumbsService.getLast().scope;
		$scope.tableParams = lastScope.tableParams;
		$scope.requests = lastScope.requests;
	}
}

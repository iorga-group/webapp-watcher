'use strict';

angular.module('iraj-table-service', ['ngTable'])
	.factory('irajTableService', function(ngTableParams, $filter, $parse) {
		var irajTableService = {};
		
		irajTableService.sortTable = function(tableParamsName, dataExpression, orderedDataExpression, scope) {
			var data = scope.$eval(dataExpression);
			var tableParams = scope.$eval(tableParamsName);
			// see http://esvit.github.io/ng-table/#!/demo3
			// use build-in angular filter
			var orderedData = tableParams.sorting ? 
				$filter('orderBy')(data, tableParams.orderBy()) :
				data;
	 
			// slice array data on pages
			var list;
			if (orderedData) {
				list = orderedData.slice(
					(tableParams.page - 1) * tableParams.count,
					tableParams.page * tableParams.count
				);
			}
			
			$parse(orderedDataExpression).assign(scope, list);
//			$scope.$eval(orderedDataExpression+'=') = list;
		}
		
		irajTableService.initTable = function(tableParamsName, dataExpression, orderedDataExpression, scope, displayedRowsCount) {
			scope.$watch(tableParamsName, function() {
				irajTableService.sortTable(tableParamsName, dataExpression, orderedDataExpression, scope);
			});
			scope.$watch(dataExpression, function(data) {
				if (data) {
					var tableParams = scope.$eval(tableParamsName);
					tableParams.total = data.length;
					tableParams.page = 1;
					irajTableService.sortTable(tableParamsName, dataExpression, orderedDataExpression, scope);
				}
			});
			$parse(tableParamsName).assign(scope, new ngTableParams({count: displayedRowsCount ? displayedRowsCount : 25}));
		}
		
		return irajTableService;
	})
;
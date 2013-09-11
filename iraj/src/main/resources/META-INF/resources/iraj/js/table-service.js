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
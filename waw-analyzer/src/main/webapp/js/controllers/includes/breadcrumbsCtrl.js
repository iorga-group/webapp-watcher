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
function BreadcrumbsCtrl($scope, $location, irajBreadcrumbsService) {	
	$scope.breadcrumbs = irajBreadcrumbsService.getAll();
	
	$scope.goToUrl = function goToUrl(path, index){
		irajBreadcrumbsService.switchTo(index);
		$location.path(path);
	};
	
	$scope.$on('iraj:breadcrumbs-refresh', function() {
		$scope.breadcrumbs = irajBreadcrumbsService.getAll();
    });
}
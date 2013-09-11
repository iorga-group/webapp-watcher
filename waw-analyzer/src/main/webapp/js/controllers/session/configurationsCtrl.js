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
function ConfigurationsCtrl($scope, $http, irajMessageService, irajBreadcrumbsService) {
	/// Action methods ///
	/////////////////////
	$scope.save = function() {
		$http.post('api/session/configurations/save', $scope.configurations, {irajMessagesIdPrefix: 'configurations', irajClearAllMessages: true})
		.success(function(data, status, headers, config) {
			irajMessageService.displayMessage({message: "Configuration saved", type: 'success'}, 'configurations');
		});
	}
	
	/// Initialization ///
	/////////////////////
	irajBreadcrumbsService.setLastLabel('Session configurations');
	
	$http.get('api/session/configurations/load').success(function(data, status, headers, config) {
		$scope.configurations = data;
	});
}
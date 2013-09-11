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

angular.module('iraj-breadcrumbs-service', [])
	.factory('irajBreadcrumbsService', function($rootScope, $location) {
		var listBreadCrumb = [];
		var irajBreadcrumbsService = {};
	
		irajBreadcrumbsService.getAll = function() {
			return listBreadCrumb;
		};
	
		irajBreadcrumbsService.push = function(scope, path, label) {
			var taille = listBreadCrumb.length;
			// we fill the scope in the last element of the breadcrumbs
			this.getLast().scope = scope;
			listBreadCrumb.push({index : taille, label : label, path: path});
			$rootScope.$broadcast('iraj:breadcrumbs-refresh');
		};
		
		irajBreadcrumbsService.changePathAndPush = function(scope, path, label) {
			$location.path(path);
			irajBreadcrumbsService.push(scope, $location.path());
		}
	
		irajBreadcrumbsService.init = function(path, label) {
			listBreadCrumb = [];
			listBreadCrumb.push({index : 0, label : label, path: path});
			$rootScope.$broadcast('iraj:breadcrumbs-refresh');
		};
	
		irajBreadcrumbsService.switchTo = function(index) {
			var newListBreadCrumb = [];
			for (var i=0; i<=index; i++) {
				newListBreadCrumb.push(listBreadCrumb[i]);
			}
			listBreadCrumb = newListBreadCrumb;
			$rootScope.$broadcast('iraj:breadcrumbs-refresh');
		};
	
		irajBreadcrumbsService.replace = function(path, label) {
			var taille = listBreadCrumb.length;
			listBreadCrumb[taille-1] = {index : taille, label : label, path: path};
			$rootScope.$broadcast('iraj:breadcrumbs-refresh');
		};
	
		irajBreadcrumbsService.getFirst = function() {
			return listBreadCrumb[0] || {};
		};
	
		irajBreadcrumbsService.getLast = function() {
			return listBreadCrumb[listBreadCrumb.length-1] || null;
		};
		
		irajBreadcrumbsService.getLastIndex = function() {
			return listBreadCrumb.length-1;
		};

		irajBreadcrumbsService.shouldLoadFromLastScope = function() {
			if (irajBreadcrumbsService.getLast()){
				return irajBreadcrumbsService.getLast().scope;
			} else {
				return false;
			}
		};
		
		irajBreadcrumbsService.setLastLabel = function(label) {
			if (irajBreadcrumbsService.getLast()){
				irajBreadcrumbsService.getLast().label = label;
			}else{
				this.init($location.path(), label);
			}
		}

		return irajBreadcrumbsService;
	})
;
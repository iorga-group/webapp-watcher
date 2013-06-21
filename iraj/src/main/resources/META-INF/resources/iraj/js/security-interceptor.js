'use strict';
// based on https://github.com/witoldsz/angular-http-auth/blob/master/src/http-auth-interceptor.js

angular.module('iraj-security-interceptor', ['iraj-authentication-service'])
	.provider('irajSecurityInterceptor', function() {
		var apiPrefix = 'api/';
		this.setApiPrefix = function(apiPrefixParam) {
			apiPrefix = apiPrefixParam;
		}

		this.$get = function() {
			return {
				apiPrefix: apiPrefix
			};
		}
	})
	.config(function ($httpProvider) {
		$httpProvider.interceptors.push(function($q, $rootScope, irajSecurityInterceptor, irajAuthenticationService) {
			return {
				'request': function(config) {
					if (config.url.indexOf(irajSecurityInterceptor.apiPrefix) == 0) {
						if (!irajAuthenticationService.authenticated && !config.authenticating) {
							var deferred = $q.defer();
							irajAuthenticationService.appendQuery(config, deferred);
							$rootScope.$broadcast('iraj:auth-loginRequired');
							return deferred.promise;
						} else {
							// this is an api request, let's add the Authorization header
							irajAuthenticationService.addAuthorizationHeader(config);
						}
					}
					return config;
				}
			}
		});
	})
;

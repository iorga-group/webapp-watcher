'use strict';
// based on https://github.com/witoldsz/angular-http-auth/blob/master/src/http-auth-interceptor.js

angular.module('iraj-authentication-service', [])
	.provider('irajAuthenticationService', function() {
		var getTimeApiUrl = 'api/security/getTime';
		this.setGetTimeApiUrl = function (getTimeApiUrlParam) {
			getTimeApiUrl = getTimeApiUrlParam;
		};
		
		function AuthenticationService($injector, $rootScope) {
			var queryBuffer = [],
				$http,
				authenticationService = this;

			this.login = null;
			this.digestedPassword = null;
			this.authenticated = false;
			this.timeShifting = 0;

			this.tryLogin = function(login, digestedPassword) {
				authenticationService.login = login;
				authenticationService.digestedPassword = digestedPassword;
				$http = $http || $injector.get('$http');	// Lazy inject
				$http.get(getTimeApiUrl, {authenticating: true})
					.success(function(data, status, headers, config) {
						// data : server time
						authenticationService.timeShifting = new Date().getTime() - data;
						authenticationService.authenticated = true;
						$rootScope.$broadcast('iraj:auth-loginSucced', data, status, headers, config);
						authenticationService.retryAllQueries();
					})
					.error(function(data, status, headers, config) {
						$rootScope = $rootScope || $injector.get('$rootScope');	// Lazy inject
						$rootScope.$broadcast('iraj:auth-loginFailed', data, status, headers, config);
					});
			};
			this.appendQuery = function(config, deferred) {
				queryBuffer.push({
					config: config,
					deferred: deferred
				});
			};
			this.retryAllQueries = function() {
				for (var i = 0; i < queryBuffer.length; ++i) {
					authenticationService.retryHttpRequest(queryBuffer[i].config, queryBuffer[i].deferred);
				}
			};
			this.retryHttpRequest = function(config, deferred) {
				authenticationService.addAuthorizationHeader(config);
				return deferred.resolve(config);
			};
			this.addAuthorizationHeader = function(config) {
				// Adding the date header considering the time shifting
				config.headers['X-IRAJ-Date'] = new Date(new Date().getTime() - authenticationService.timeShifting).toUTCString();
				
				securityUtils.addAuthorizationHeader(authenticationService.login, authenticationService.digestedPassword, {
					method: config.method,
					body: config.transformRequest[0](config.data),
					headers: config.headers,
					resource: config.url.substring(3, config.url.length)
				});
			};
		};

		this.$get = function ($injector, $rootScope) {
			return new AuthenticationService($injector, $rootScope);
		};
	})
	.run(function($rootScope, irajAuthenticationService) {
		$rootScope.$on('iraj:auth-tryLogin', function(event, login, digestedPassword) {
			irajAuthenticationService.tryLogin(login, digestedPassword);
		});
	})
;

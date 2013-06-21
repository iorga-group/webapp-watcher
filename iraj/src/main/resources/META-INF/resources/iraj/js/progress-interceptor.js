'use strict';

angular.module('iraj-progress-interceptor', [])
	.provider('irajProgressInterceptor', function() {
		var defaultMessage;
		var callback = function(nbRequests, message, config) {
			var irajGlobalProgressEl = jQuery('[irajGlobalProgress]');
			if (irajGlobalProgressEl.length == 0) {
				jQuery(document.body).append('<div irajGlobalProgress><img irajGlobalProgressImg src="iraj/img/progress.gif" /><span irajGlobalProgressNbRequests class="badge"></span><span irajGlobalProgressMessage /></div>');
				irajGlobalProgressEl = jQuery('[irajGlobalProgress]');
			}
			var irajGlobalProgressMessageEl = irajGlobalProgressEl.find('[irajGlobalProgressMessage]');
			if (nbRequests == 0) {
				irajGlobalProgressEl.hide();
				irajGlobalProgressMessageEl.empty();
			} else {
				var irajGlobalProgressNbRequestsEl = irajGlobalProgressEl.find('[irajGlobalProgressNbRequests]');
				if (nbRequests > 1)	{
					irajGlobalProgressNbRequestsEl.text(nbRequests);
					irajGlobalProgressNbRequestsEl.show();
				} else {
					irajGlobalProgressNbRequestsEl.hide();
				}
				// first apply default message if any
				if (!message) {
					if (defaultMessage) {
						message = defaultMessage;
					} else if (config) {
						// if there is no message, defaultMessage but config is defined, take the url
						message = config.url;
					}
				}
				if (message) {
					irajGlobalProgressMessageEl.text(message);
				}
				irajGlobalProgressEl.show();
			}
		};
		
		this.setCallback = function(callbackParam) {
			callback = callbackParam;
		}
		
		this.setDefaultMessage = function(defaultMessageParam) {
			defaultMessage = defaultMessageParam;
		}
	
		this.$get = function() {
			return {
				callback: callback
			};
		}
	})
	.config(function ($httpProvider) {
		var nbRequests = 0;
		$httpProvider.interceptors.push(function($q, $rootScope, irajProgressInterceptor) {
			return {
				'request': function(config) {
					nbRequests++;
					irajProgressInterceptor.callback(nbRequests, config.irajProgressMessage, config);
					return config;
				},
				'response': function(response) {
					nbRequests--;
					irajProgressInterceptor.callback(nbRequests);
					return response;
				},
				'responseError': function(rejection) {
					nbRequests--;
					irajProgressInterceptor.callback(nbRequests);
					return $q.reject(rejection);
				}
			}
		});
	})
;

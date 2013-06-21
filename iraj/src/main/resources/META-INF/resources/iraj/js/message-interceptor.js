'use strict';

angular.module('iraj-message-interceptor', ['iraj-message-service'])
	.factory('irajMessageInterceptor', function(irajMessageService) {
		return {
			applyFieldMessages: function(response) {
				var irajFieldMessages = response.data.irajFieldMessages;
				if (irajFieldMessages) {
					// there are some field messages to display
					for (var i = 0 ; i < irajFieldMessages.length ; i++) {
						var irajFieldMessage = irajFieldMessages[i];
						var id = irajFieldMessage.id;
						if (!id) {
							// the final id has not been sent, let's recompute it
							id = response.config.irajMessagesIdPrefix;
							for (var j = 0 ; j < irajFieldMessage.propertyPath.length ; j++) {
								if (id) {
									id += '-';
								}
								id += irajFieldMessage.propertyPath[j];
							}
						}
						irajMessageService.displayFieldMessage({
							message: irajFieldMessage.message,
							type: irajFieldMessage.type,
							id: id
						}, response.config.irajMessagesIdPrefix);
					}
				}
				var irajMessages = response.data.irajMessages;
				if (irajMessages) {
					// there are some form messages to display
					irajMessageService.displayMessages(irajMessages, response.config.irajMessagesIdPrefix);
				}
				return irajFieldMessages || irajMessages;
			}
		}
	})
	.config(function ($httpProvider) {
		$httpProvider.interceptors.push(function($q, irajMessageService, irajMessageInterceptor) {
			return {
				'response': function(response) {
					irajMessageInterceptor.applyFieldMessages(response);
					return response;
				},
				'responseError': function(rejection) {
					if (!irajMessageInterceptor.applyFieldMessages(rejection)) {
						// no message found in the request, it's a more global problem, let's display it
						irajMessageService.displayMessage({message: rejection.status+' : '+rejection.data, type: 'error'});
					}
					return $q.reject(rejection);
				},
				'request': function(config) {
					if (config.irajClearFieldMessages) {
						irajMessageService.clearFieldMessages(config.irajMessagesIdPrefix);
					}
					if (config.irajClearAllMessages) {
						irajMessageService.clearAllMessages(config.irajMessagesIdPrefix);
					}
					return config;
				}
			}
		})
	})
;

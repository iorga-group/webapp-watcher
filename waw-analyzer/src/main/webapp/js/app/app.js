var module = angular.module('waw-analyzer', [
		'$strap.directives',
		'iraj-message-interceptor',
		'iraj-message-service',
		'iraj-breadcrumbs-service',
		'iraj-progress-interceptor',
		'blueimp.fileupload',
		'ngTable'])
	.config(router);

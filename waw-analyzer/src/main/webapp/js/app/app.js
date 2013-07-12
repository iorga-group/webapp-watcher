var module = angular.module('waw-analyzer', [
		'$strap.directives',
		'iraj-message-interceptor',
		'iraj-message-service',
		'iraj-breadcrumbs-service',
		'iraj-progress-interceptor',
		'iraj-table-service',
		'blueimp.fileupload',
		'flotutils-service'])
	.config(router)
;

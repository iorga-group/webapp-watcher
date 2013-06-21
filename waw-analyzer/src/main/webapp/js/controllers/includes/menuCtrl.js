function MenuCtrl($scope, $location, irajBreadcrumbsService) {
    $scope.loadPath = function(path, label) {
    	$location.path(path);
    	irajBreadcrumbsService.init(path, label);
    }
}
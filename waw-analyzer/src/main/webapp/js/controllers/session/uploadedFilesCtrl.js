function UploadedFilesCtrl() {}

function UploadedFilesFormCtrl($scope, $http) {
	var url = 'api/session/uploadedFiles/';
    $scope.loadingFiles = true;
    $scope.options = {
        url: url
    };
    $scope.loadingFiles = false;
    $http.get(url)
        .then(
            function (response) {
                $scope.loadingFiles = false;
                $scope.queue = response.data.files || [];
            },
            function () {
                $scope.loadingFiles = false;
            }
        );
}

function UploadedFilesDestroyCtrl($scope, $http) {
    var file = $scope.file, state;
	if (file.delete_url) {
	    file.$state = function () {
	        return state;
	    };
	    file.$destroy = function () {
	        state = 'pending';
	        return $http({
	            url: file.delete_url,
	            method: file.delete_type
	        }).then(
	            function () {
	                state = 'resolved';
	                $scope.clear(file);
	            },
	            function () {
	                state = 'rejected';
	            }
	        );
	    };
	} else if (!file.$cancel) {
	    file.$cancel = function () {
	        $scope.clear(file);
	    };
	}
}
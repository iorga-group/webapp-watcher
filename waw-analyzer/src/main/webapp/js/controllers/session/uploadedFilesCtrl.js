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
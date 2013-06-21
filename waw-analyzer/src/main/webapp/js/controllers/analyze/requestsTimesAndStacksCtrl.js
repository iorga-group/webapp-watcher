function RequestsTimesAndStacksCtrl($http, $scope, irajTableService) {
	/// Action methods ///
	/////////////////////
	$scope.compute = function () {
		$http.get('api/analyze/requestsTimesAndStacks/compute/3000', {irajClearAllMessages: true})
			.success(function(data, status, headers, config) {
				$scope.requests = data;
			})
		;
	}
	
	$scope.computeGroupedStacks = function(request) {
		if (!request.groupedStacks) {
			// load the groupedStacks
			$http.get('api/analyze/requestsTimesAndStacks/computeGroupedStacks/'+request.id, {irajClearAllMessages: true})
				.success(function(data, status, headers, config) {
					var nodes = data;
					// we will "flatten" the nodes in order to display all children in <p/>s if there is only 1 child each time
					var targetNodes = [];
					var id = 0;
					var recursiveFlatten = function (nodes, targetNodes) {
						var i;
						for (i = 0 ; i < nodes.length ; i++) {
							// for each node, will create a targetNode element
							var node = nodes[i];
							var targetNode = {
								id: request.id+'-'+(id++),
								stackTraceElement: {element: node.stackTraceElement, nb: node.nb},
								stackTraceElements: [],
								children: []
							};
							targetNodes.push(targetNode);
							while (node.children && node.children.length == 1) {
								node = node.children[0];
								targetNode.stackTraceElements.push({element: node.stackTraceElement, nb: node.nb});
							}
							if (node.children) {
								// multiple children, let's add it to children
								recursiveFlatten(node.children, targetNode.children);
							}
						}
					}
					recursiveFlatten(nodes, targetNodes);
					request.groupedStacks = targetNodes;
				})
			;
		}
	}
	
	$scope.displayChildren = function(node) {
		node.displayedChildren = node.children;
	}
	
	/// Initialization ///
	/////////////////////
	irajTableService.initTable('tableParams', 'requests', 'orderedRequests', $scope);
}

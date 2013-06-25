function GroupedStacksCtrl(irajBreadcrumbsService, $routeParams, $http, $scope) {
	/// Action methods ///
	/////////////////////
	$scope.switchChildrenDisplayed = function(node) {
		node.childrenDisplayed = !node.childrenDisplayed;
	}

	/// Initialization ///
	/////////////////////
	irajBreadcrumbsService.setLastLabel('Grouped stacks');
	
	// load the grouped stacks
	var requestId = $routeParams.requestId;

	// load the groupedStacks
	$http.get('api/analyze/requestsTimesAndStacks/computeGroupedStacks/'+requestId, {irajClearAllMessages: true})
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
						id: requestId+'-'+(id++),
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
			$scope.groupedStacks = targetNodes;
			$scope.rootNode = targetNodes[0];
		})
	;
}
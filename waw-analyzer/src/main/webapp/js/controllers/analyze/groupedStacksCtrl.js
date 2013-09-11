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
	var requestIndex = $routeParams.requestIndex;

	var flattenNodes = function (nodes) {
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
	}
	
	if (!requestIndex) {
		// load the groupedStacks for a requestId
		$http.get('api/analyze/groupedStacks/compute/'+requestId, {irajClearAllMessages: true})
			.success(function(data, status, headers, config) {
				flattenNodes(data);
			})
		;
	} else {
		// load the groupedStacks for a requestId and its requestIndex
		$http.get('api/analyze/groupedStacks/computeDetails/'+requestId+'/'+requestIndex, {irajClearAllMessages: true})
			.success(function(data, status, headers, config) {
				flattenNodes(data);
			})
		;
	}
}
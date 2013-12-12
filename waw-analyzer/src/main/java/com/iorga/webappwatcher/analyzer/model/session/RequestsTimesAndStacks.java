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
package com.iorga.webappwatcher.analyzer.model.session;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

import com.google.common.base.Supplier;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.iorga.webappwatcher.analyzer.model.session.UploadedFiles.FileMetadataReader;
import com.iorga.webappwatcher.analyzer.model.session.UploadedFiles.FilesChanged;
import com.iorga.webappwatcher.analyzer.util.RequestActionKeyComputer;
import com.iorga.webappwatcher.eventlog.EventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog.Header;
import com.iorga.webappwatcher.eventlog.RequestEventLog.Parameter;
import com.iorga.webappwatcher.eventlog.SystemEventLog;
import com.iorga.webappwatcher.eventlog.SystemEventLog.Thread;

@SessionScoped
public class RequestsTimesAndStacks implements Serializable {
	private static final long serialVersionUID = 1L;

	/// Dependencies ///
	@Inject
	private UploadedFiles uploadedFiles;
	@Inject
	private Configurations configurations;

	/// Parameters ///
	private int minMillisToLog;

	/// Variables structure ///
	private boolean computed = false;
	private int nextId = 0;
	private final Map<String, RequestTimes> requestsByUrl = Maps.newHashMap();
	private final Map<String, RequestTimes> requestsById = Maps.newHashMap();
	private final ListMultimap<RequestEventLog, SystemEventLog> slowRequestSystemLogs = newListMultimap();
	private final Map<String, TreeNode<StackStatElement>> groupedStacksRootsById = Maps.newHashMap();
	private final Map<RequestEventLog, TreeNode<StackStatElement>> groupedStacksRootsByRequestEventLog = Maps.newHashMap();
	private final List<RequestContainer> allRequests = Lists.newArrayList();

	public class RequestContainer implements Serializable {
		private static final long serialVersionUID = 1L;

		private final int requestIndex;
		private final RequestEventLog requestEventLog;
		private final String url;

		public RequestContainer(final int requestIndex, final RequestEventLog requestEventLog, final String url) {
			this.requestIndex = requestIndex;
			this.requestEventLog = requestEventLog;
			this.url = url;
		}

		public int getRequestIndex() {
			return requestIndex;
		}
		public RequestEventLog getRequestEventLog() {
			return requestEventLog;
		}
		public String getUrl() {
			return url;
		}
		public List<SystemEventLog> getSystemEventLogList() {
			return slowRequestSystemLogs.get(requestEventLog);
		}
	}

	public static class RequestTimes implements Serializable {
		private static final long serialVersionUID = 1L;

		private final String id;
		private final String url;
		private final DescriptiveStatistics statistics = new DescriptiveStatistics();
		private final List<RequestEventLog> slowRequests = Lists.newArrayList();

		public RequestTimes(final String url, final int id) {
			this.url = url;
			this.id = ""+id;
		}

		public String getId() {
			return id;
		}
		public String getUrl() {
			return url;
		}
		public DescriptiveStatistics getStatistics() {
			return statistics;
		}
		public List<RequestEventLog> getSlowRequests() {
			return slowRequests;
		}
	}

	public static class StackStatElement  implements Serializable {
		private static final long serialVersionUID = 1L;

		private final StackTraceElement stackTraceElement;
		private int nb = 1;

		public StackStatElement(final StackTraceElement stackTraceElement) {
			this.stackTraceElement = stackTraceElement;
		}

		public StackTraceElement getStackTraceElement() {
			return stackTraceElement;
		}

		public int getNb() {
			return nb;
		}
	}

	public static class TreeNode<T>  implements Serializable {
		private static final long serialVersionUID = 1L;

		private final TreeNode<T> parent;
		private final List<TreeNode<T>> children;
		private final T data;

		public TreeNode(final T data, final TreeNode<T> parent) {
			this.parent = parent;
			this.data = data;
			this.children = Lists.newLinkedList();

			// add this to parent
			if (parent != null) {
				parent.children.add(this);
			}
		}

		public TreeNode<T> getParent() {
			return parent;
		}
		public List<TreeNode<T>> getChildren() {
			return children;
		}
		public T getData() {
			return data;
		}
	}

	public static class GenericListSupplier<V> implements Supplier<List<V>>, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public List<V> get() {
			return Lists.newArrayList();
		}
	}


	/// Actions ///
	//////////////
	public List<RequestContainer> computeRequestContainers() throws ClassNotFoundException, IOException {
		compute();
		return allRequests;
	}

	public synchronized void compute() throws IOException, ClassNotFoundException {
		if (this.minMillisToLog != configurations.getMinMillisToLog() || !computed) {
			this.minMillisToLog = configurations.getMinMillisToLog();

			final List<RequestContainer> currentAllRequests = Lists.newLinkedList(); // create first a linked list, and then convert it to array list in order to access nth element in O(1)
			final RequestActionKeyComputer requestActionKeyComputer = configurations.getRequestActionKeyComputer();

			uploadedFiles.readFiles(new FileMetadataReader() {
				final List<RequestEventLog> currentSlowRequests = Lists.newLinkedList();

				@Override
				protected void handleEventLog(final EventLog eventLog) throws IOException {
					if (eventLog instanceof RequestEventLog) {
						final RequestEventLog request = (RequestEventLog) eventLog;

						// Compute the request key
						final String requestKey = requestActionKeyComputer.computeRequestKey(request);

						// log it by principal
						currentAllRequests.add(new RequestContainer(currentAllRequests.size(), request, requestKey));

						final Long durationMillis = request.getDurationMillis();
						if (durationMillis != null) {

							// now add the requestKey and its duration
							RequestTimes requestTimes = requestsByUrl.get(requestKey);
							if (requestTimes == null) {
								// create a new request times
								requestTimes = new RequestTimes(requestKey, nextId++);
								requestsByUrl.put(requestKey, requestTimes);
								requestsById.put(requestTimes.id, requestTimes);
							}
							requestTimes.statistics.addValue(durationMillis);
							// if the duration is more than the defined minimum, log it
							if (durationMillis >= minMillisToLog) {
								requestTimes.slowRequests.add(request);
								currentSlowRequests.add(request);
							}
						} else {
							System.out.println("Ignoring "+request);
						}
					} else if (eventLog instanceof SystemEventLog) {
						final SystemEventLog system = (SystemEventLog) eventLog;
						// check all the current slow requests in order to add them that systemEventLog or remove them from the current slow requests
						// list if the end date has passed
						for (final Iterator<RequestEventLog> iterator = currentSlowRequests.iterator(); iterator.hasNext();) {
							final RequestEventLog request = iterator.next();
							if (request.getAfterProcessedDate().getTime() > system.getDate().getTime()) {
								// the system log is included into that request duration, let's add it
								slowRequestSystemLogs.put(request, system);
							} else {
								// the systme log comes after the end of that request, that request has now passed, let's remove it from the list
								iterator.remove();
							}
						}
					}
				}
			});
			this.allRequests.addAll(currentAllRequests); // convert it to array list in order to access nth element in O(1)
			this.computed = true;
		}
	}

	public synchronized List<TreeNode<StackStatElement>> computeGroupedStacksForRequestId(final String requestId) {
		TreeNode<StackStatElement> groupedStacksRoot = groupedStacksRootsById.get(requestId);
		if (groupedStacksRoot == null) {
			groupedStacksRoot = new TreeNode<StackStatElement>(null, null);
			groupedStacksRootsById.put(requestId, groupedStacksRoot);
			// retrieve all the requests for that requestKey
			final List<RequestEventLog> requests = requestsById.get(requestId).slowRequests;
			for (final RequestEventLog request : requests) {
				computeGroupedStacksForRequest(request, groupedStacksRoot);
			}
		}
		return groupedStacksRoot.getChildren();
	}

	/**
	 * Filters all the requests with the query. Here are the variables available to the query :<ul>
	 * <li>headers: a <code>Set&lt;String&gt;</code> of the request headers</li>
	 * <li>parameters: a <code>Map&lt;String, Set&lt;String&gt;&gt;</code> of the request parameters</li>
	 * <li>request: the <code>RequestEventLog</code></li>
	 * <li>requestContainer: the <code>RequestContainer</code></li>
	 * </ul>
	 * @param query
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public List<RequestContainer> computeRequestContainersByQuery(final String query) throws ClassNotFoundException, IOException {
		final List<RequestContainer> requestContainers = computeRequestContainers();

		final List<RequestContainer> filteredRequestContainers = Lists.newLinkedList();

		final Context context = ContextFactory.getGlobal().enterContext();
		context.setOptimizationLevel(9);

		final ScriptableObject scope = context.initStandardObjects();
		final Script compiledQuery = context.compileString(query, "query.js", 1, null);

		for (final RequestContainer requestContainer : requestContainers) {
			//TODO : create headers & parameters only if necessary (found in the query)
			final Map<String, String> headers = Maps.newHashMap();
			final RequestEventLog request = requestContainer.requestEventLog;
			for (final Header header : request.getHeaders()) {
				headers.put(header.getName(), header.getValue());
			}
			final Map<String, Set<String>> parameters = Maps.newHashMap();
			for (final Parameter parameter : request.getParameters()) {
				final String[] values = parameter.getValues();
				parameters.put(parameter.getName(), values != null ? Sets.newHashSet(values) : Sets.<String>newHashSet());
			}
			// define variables in the scope
			scope.put("headers", scope, headers);
			scope.put("parameters", scope, parameters);
			scope.put("request", scope, request);
			scope.put("requestContainer", scope, requestContainer);
			// test if that request matches
			if (Boolean.TRUE.equals(compiledQuery.exec(context, scope))) {
				filteredRequestContainers.add(requestContainer);
			}
		}
		return filteredRequestContainers;
	}

	public synchronized List<TreeNode<StackStatElement>> computeGroupedStacksForRequestIdAndRequestIndex(final String requestId, final int requestIndex) {
		final RequestEventLog requestEventLog = getRequestEventLogForRequestIdAndRequestIndex(requestId, requestIndex);
		TreeNode<StackStatElement> groupedStacksRoot = groupedStacksRootsByRequestEventLog.get(requestEventLog);
		if (groupedStacksRoot == null) {
			groupedStacksRoot = new TreeNode<StackStatElement>(null, null);
			groupedStacksRootsByRequestEventLog.put(requestEventLog, groupedStacksRoot);
			computeGroupedStacksForRequest(requestEventLog, groupedStacksRoot);
		}
		return groupedStacksRoot.getChildren();
	}

	public RequestEventLog getRequestEventLogForRequestIdAndRequestIndex(final String requestId, final int requestIndex) {
		return requestsById.get(requestId).getSlowRequests().get(requestIndex);
	}

	public List<RequestTimes> createSortedRequestByDescendantMeanList() {
		return Ordering.from(new Comparator<RequestTimes>() {
			@Override
			public int compare(final RequestTimes o1, final RequestTimes o2) {
				return new Double(o1.statistics.getMean()).compareTo(new Double(o2.statistics.getMean()));
			}
		}).reverse().sortedCopy(requestsByUrl.values());
	}

	public RequestTimes getRequestTimesForId(final String requestId) {
		return requestsById.get(requestId);
	}

	public int getNbStacksForRequestEventLog(final RequestEventLog requestEventLog) {
		return slowRequestSystemLogs.get(requestEventLog).size();
	}

	/// Events ///
	/////////////
	public void onUploadedFilesChanged(@Observes @FilesChanged final UploadedFiles uploadedFiles) {
		resetComputation();
	}

	/// Utils ///
	////////////
	private static <K, V> ListMultimap<K, V> newListMultimap() {
		return Multimaps.newListMultimap(Maps.<K, Collection<V>>newHashMap(), new GenericListSupplier<V>());
	}

	private void computeGroupedStacksForRequest(final RequestEventLog request, final TreeNode<StackStatElement> groupedStacksRoot) {
		// retrieve all system logs for that request if any
		final List<SystemEventLog> systems = slowRequestSystemLogs.get(request);
		// for all that system events, will retrieve the thread of the request
		for (final SystemEventLog system : systems) {
			for (final Thread thread : system.getBlockedOrRunningThreads()) {
				if (thread.getId() == request.getThreadId()) {
					// This is the thread of the request, let's add the stack to the StackStatElement children
					final StackTraceElement[] stackTraces = thread.getStackTrace();
					recurseAddStackElement(groupedStacksRoot, stackTraces, stackTraces.length);
					break; // found
				}
			}
		}
	}

	private void recurseAddStackElement(final TreeNode<StackStatElement> parent, final StackTraceElement[] stackTrace, int stackIndex) {
		if (stackIndex > 0) {
			stackIndex--;
			final List<TreeNode<StackStatElement>> children = parent.getChildren();
			// first retrieve the element to add
			final StackTraceElement element = stackTrace[stackIndex];
			boolean wasAdded = false;
			for (final TreeNode<StackStatElement> node : children) {
				// check if it's the same stack
				final StackStatElement treeElement = node.getData();
				if (treeElement.stackTraceElement.equals(element)) {
					// it's the same element, let's add it
					treeElement.nb++;
					wasAdded = true;
					// and recurse add the next element
					recurseAddStackElement(node, stackTrace, stackIndex);
					break;	// found
				}
			}
			if (!wasAdded) {
				// not found, let's create a new node for it
				final TreeNode<StackStatElement> node = new TreeNode<StackStatElement>(new StackStatElement(element), parent);
				// and recurse add the next element
				recurseAddStackElement(node, stackTrace, stackIndex);
			}
		}
	}

	private void resetComputation() {
		minMillisToLog = 0;

		computed = false;
		nextId = 0;
		requestsByUrl.clear();
		requestsById.clear();
		slowRequestSystemLogs.clear();
		groupedStacksRootsById.clear();
		groupedStacksRootsByRequestEventLog.clear();
		allRequests.clear();
	}
}

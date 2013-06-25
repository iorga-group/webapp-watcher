package com.iorga.webappwatcher.analyzer.model.session;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.google.common.base.Supplier;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.iorga.webappwatcher.analyzer.model.session.UploadedFiles.FileMetadataReader;
import com.iorga.webappwatcher.eventlog.EventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog.Parameter;
import com.iorga.webappwatcher.eventlog.SystemEventLog;
import com.iorga.webappwatcher.eventlog.SystemEventLog.Thread;

@SessionScoped
public class RequestsTimesAndStacks implements Serializable {
	private static final long serialVersionUID = 1L;

	@Inject
	private UploadedFiles uploadedFiles;

	/// Parameters ///
	private int minMillisToLog;

	/// Variables structure ///
	private boolean computed = false;
	private int nextId = 0;
//	private final Map<String, DescriptiveStatistics> requests = Maps.newHashMap();
	private final Map<String, RequestTimes> requestsByUrl = Maps.newHashMap();
	private final Map<String, RequestTimes> requestsById = Maps.newHashMap();
	private final ListMultimap<String, RequestEventLog> slowRequestsById = newListMultimap();
	private final ListMultimap<RequestEventLog, SystemEventLog> slowRequestSystemLogs = newListMultimap();
	private final Map<String, TreeNode<StackStatElement>> groupedStacksRoots = Maps.newHashMap();
//	private List<Entry<String, DescriptiveStatistics>> sortedRequestlist;

	public static class RequestTimes implements Serializable {
		private static final long serialVersionUID = 1L;

		private final String id;
		private final String url;
		private final DescriptiveStatistics statistics = new DescriptiveStatistics();

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


	public synchronized void compute(final int minMillisToLog) throws IOException, ClassNotFoundException {
		if (this.minMillisToLog != minMillisToLog || !computed) {

			uploadedFiles.readFiles(new FileMetadataReader() {
				final List<RequestEventLog> currentSlowRequests = Lists.newLinkedList();

				@Override
				protected void handleEventLog(final EventLog eventLog) throws IOException {
					if (eventLog instanceof RequestEventLog) {
						final RequestEventLog request = (RequestEventLog) eventLog;

						final Long durationMillis = request.getDurationMillis();
						if (durationMillis != null) {
							// Compute the request key
							final StringBuilder requestKeyBuilder = new StringBuilder();
							requestKeyBuilder.append(request.getMethod()).append(":").append(request.getRequestURI());
							// Put the parameters in a Map in order to check values easily
							final Map<String, String[]> parameters = Maps.newHashMap();
							for (final Parameter parameter : request.getParameters()) {
								parameters.put(parameter.getName(), parameter.getValues());
							}
							// Now check if it's an AJAX request, retrieve the source /!\ Specific JSF
							if (parameters.containsKey("AJAX:EVENTS_COUNT")) {
								// It's an ajax request, let's add the source to the key
								requestKeyBuilder.append("?ajax.source=").append(parameters.get("javax.faces.source")[0]);
							}

							final String requestKey = requestKeyBuilder.toString();
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
								slowRequestsById.put(requestTimes.id, request);
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

			this.minMillisToLog = minMillisToLog;
			this.computed = true;
		}
	}

	public synchronized List<TreeNode<StackStatElement>> computeGroupedStacksForRequestId(final String requestId) {
		TreeNode<StackStatElement> groupedStacksRoot = groupedStacksRoots.get(requestId);
		if (groupedStacksRoot == null) {
			groupedStacksRoot = new TreeNode<StackStatElement>(null, null);
			groupedStacksRoots.put(requestId, groupedStacksRoot);
			// retrieve all the requests for that requestKey
			final List<RequestEventLog> requests = slowRequestsById.get(requestId);
			for (final RequestEventLog request : requests) {
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
		}
		return groupedStacksRoot.getChildren();
	}

	public List<RequestTimes> createSortedRequestByDescendantMeanList() {
		return Ordering.from(new Comparator<RequestTimes>() {
			@Override
			public int compare(final RequestTimes o1, final RequestTimes o2) {
				return (int) (o1.statistics.getMean() - o2.statistics.getMean());
			}
		}).reverse().sortedCopy(requestsByUrl.values());
	}


	/// Utils ///
	////////////
	private static <K, V> ListMultimap<K, V> newListMultimap() {
		return Multimaps.newListMultimap(Maps.<K, Collection<V>>newHashMap(), new Supplier<List<V>>() {
			@Override
			public List<V> get() {
				return Lists.newArrayList();
			}
		});
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
}

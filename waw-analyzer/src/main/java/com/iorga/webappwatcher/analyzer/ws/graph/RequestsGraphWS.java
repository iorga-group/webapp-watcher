package com.iorga.webappwatcher.analyzer.ws.graph;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.iorga.webappwatcher.analyzer.model.session.RequestsGraph;
import com.iorga.webappwatcher.analyzer.model.session.RequestsGraph.Graph;
import com.iorga.webappwatcher.analyzer.model.session.RequestsGraph.GraphMode;

@Path("/graphs/requestsGraph")
public class RequestsGraphWS {
	@Inject
	private RequestsGraph requestsGraph;

	@GET
	@Path("/compute")
	public Graph compute() throws ClassNotFoundException, IOException {
		return requestsGraph.compute(GraphMode.STATIC, 6);
	}
}

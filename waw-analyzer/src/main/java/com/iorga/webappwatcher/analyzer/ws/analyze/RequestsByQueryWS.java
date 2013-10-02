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
package com.iorga.webappwatcher.analyzer.ws.analyze;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.StreamingOutput;

import org.jboss.resteasy.annotations.GZIP;

import com.iorga.iraj.json.JsonWriter;
import com.iorga.webappwatcher.analyzer.model.session.RequestsTimesAndStacks;
import com.iorga.webappwatcher.analyzer.ws.template.RequestContainerTemplate;

@Path("/analyze/requestsByQuery")
public class RequestsByQueryWS {
	@Inject
	private RequestsTimesAndStacks requestsTimesAndStacks;
	@Inject
	private JsonWriter jsonWriter;

	@POST
	@GZIP
	@Path("/compute")
	public StreamingOutput compute(@FormParam("query") final String query) throws ClassNotFoundException, IOException {
		return jsonWriter.writeIterableWithTemplate(RequestContainerTemplate.class, requestsTimesAndStacks.computeRequestContainersByQuery(query));
	}
}

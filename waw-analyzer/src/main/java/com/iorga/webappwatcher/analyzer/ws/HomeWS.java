package com.iorga.webappwatcher.analyzer.ws;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/home")
public class HomeWS {
	@GET
	@Path("/versions")
	public Map<Object, Object> getVersions() throws IOException {
		final Properties properties = new Properties();
		properties.load(getClass().getResourceAsStream("/maven.properties"));
		return properties;
	}
}

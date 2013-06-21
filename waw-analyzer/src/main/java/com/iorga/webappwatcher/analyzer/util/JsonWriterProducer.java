package com.iorga.webappwatcher.analyzer.util;

import javax.enterprise.context.ApplicationScoped;

import com.iorga.iraj.json.JsonWriter;

@ApplicationScoped
public class JsonWriterProducer {
	@ApplicationScoped
	public JsonWriter createJsonWriter() {
		return new JsonWriter();
	}
}

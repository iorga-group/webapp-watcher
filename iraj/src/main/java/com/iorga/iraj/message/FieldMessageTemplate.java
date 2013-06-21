package com.iorga.iraj.message;

import com.iorga.iraj.annotation.ContextParam;


@SuppressWarnings("unused")
@ContextParam(FieldMessage.class)
public class FieldMessageTemplate extends MessageTemplate {
	private String id;
	private Iterable<String> propertyPath;
}

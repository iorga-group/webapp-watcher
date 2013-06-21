package com.iorga.iraj.jaxrs;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.iorga.iraj.annotation.ContextParam;
import com.iorga.iraj.annotation.IgnoreProperty;
import com.iorga.iraj.annotation.TargetType;

@SuppressWarnings("unused")
@ContextParam(Throwable.class)
public class ThrowableTemplate {
	@IgnoreProperty
	private static Logger log = LoggerFactory.getLogger(ThrowableTemplate.class);

	@TargetType(value = Iterable.class, parameterizedArguments = MessageTemplate.class)
	public static Iterable<Throwable> getIrajMessages(final Throwable throwable) {
		return Lists.newArrayList(throwable);
	}

	@ContextParam(Throwable.class)
	public static class MessageTemplate {
		private static final String type = "error";

		public static String getMessage(final Throwable throwable) {
			final String uuid = UUID.randomUUID().toString();
			log.warn("Throwable #"+uuid+" catched by GlobalExceptionMapper", throwable);
			return "Problème non attendu #" + uuid; //TODO système de traduction
		}
	}
}

package com.iorga.iraj.validation;

import java.util.Iterator;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path.Node;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.iorga.iraj.annotation.ContextParam;
import com.iorga.iraj.annotation.JsonProperty;
import com.iorga.iraj.util.JaxRsUtils;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

	@ContextParam(ConstraintViolationException.class)
	public static class ConstraintViolationExceptionTemplate {
		@JsonProperty("irajFieldMessages")
		Set<ConstraintViolationTemplate> constraintViolations;

		@ContextParam(ConstraintViolation.class)
		public static class ConstraintViolationTemplate {
			String message;
			static final String type = "error";

			public static Iterable<String> getPropertyPath(final ConstraintViolation<?> violation) {
				return new Iterable<String>() {
					@Override
					public Iterator<String> iterator() {
						final Iterator<Node> iterator = violation.getPropertyPath().iterator();
						iterator.next(); // bypass the first node path which is a reference to the method which has been called
						return new Iterator<String>() {
							@Override
							public boolean hasNext() {
								return iterator.hasNext();
							}
							@Override
							public String next() {
								return iterator.next().getName();
							}
							@Override
							public void remove() {
								iterator.remove();
							}
						};
					}
				};
			}
		}
	}
	@Override
	public Response toResponse(final ConstraintViolationException exception) {
		return JaxRsUtils.throwableToIrajResponse(ConstraintViolationExceptionTemplate.class, exception);
	}

}

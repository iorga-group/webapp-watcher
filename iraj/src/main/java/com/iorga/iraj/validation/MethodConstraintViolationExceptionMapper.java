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
package com.iorga.iraj.validation;

import java.util.Iterator;
import java.util.Set;

import javax.validation.Path.Node;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.hibernate.validator.method.MethodConstraintViolation;
import org.hibernate.validator.method.MethodConstraintViolationException;

import com.iorga.iraj.annotation.ContextParam;
import com.iorga.iraj.annotation.JsonProperty;
import com.iorga.iraj.util.JaxRsUtils;

@Provider
public class MethodConstraintViolationExceptionMapper implements ExceptionMapper<MethodConstraintViolationException> {

	@ContextParam(MethodConstraintViolationException.class)
	public static class MethodConstraintViolationExceptionTemplate {
		@JsonProperty("irajFieldMessages")
		Set<MethodConstraintViolationTemplate> constraintViolations;

		@ContextParam(MethodConstraintViolation.class)
		public static class MethodConstraintViolationTemplate {
			String message;
			static final String type = "error";

			public static Iterable<String> getPropertyPath(final MethodConstraintViolation<?> violation) {
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
	public Response toResponse(final MethodConstraintViolationException exception) {
		return JaxRsUtils.throwableToIrajResponse(MethodConstraintViolationExceptionTemplate.class, exception);
	}
}

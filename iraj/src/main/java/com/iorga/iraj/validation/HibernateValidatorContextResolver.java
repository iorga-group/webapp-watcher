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

import javax.validation.Configuration;
import javax.validation.Validation;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.validation.ValidatorAdapter;

@Provider
public class HibernateValidatorContextResolver implements ContextResolver<ValidatorAdapter> {
	private static final HibernateValidatorAdaptor adapter;

	static {
		final Configuration<?> configuration = Validation.byDefaultProvider().configure();
		configuration.messageInterpolator(new HttpRequestLocaleMessageInterpolator(configuration.getDefaultMessageInterpolator()));
		adapter = new HibernateValidatorAdaptor(configuration.buildValidatorFactory().getValidator());
	}

	@Override
	public ValidatorAdapter getContext(final Class<?> type) {
		return adapter;
	}

}

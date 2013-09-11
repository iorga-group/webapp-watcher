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

import java.util.List;
import java.util.Locale;

import javax.validation.MessageInterpolator;

import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

public class HttpRequestLocaleMessageInterpolator implements MessageInterpolator {
	private final MessageInterpolator delegate;


	public HttpRequestLocaleMessageInterpolator(final MessageInterpolator delegate) {
		this.delegate = delegate;
	}


	@Override
	public String interpolate(final String messageTemplate, final Context context) {
		final List<Locale> acceptableLanguages = ResteasyProviderFactory.getContextData(HttpRequest.class).getHttpHeaders().getAcceptableLanguages();
		if (acceptableLanguages.size() > 0) {
			return delegate.interpolate(messageTemplate, context, acceptableLanguages.get(0));
		} else {
			return delegate.interpolate(messageTemplate, context);
		}
	}

	@Override
	public String interpolate(final String messageTemplate, final Context context, final Locale locale) {
		return delegate.interpolate(messageTemplate, context, locale);
	}
}

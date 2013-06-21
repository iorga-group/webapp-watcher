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

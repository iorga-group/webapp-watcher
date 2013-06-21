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

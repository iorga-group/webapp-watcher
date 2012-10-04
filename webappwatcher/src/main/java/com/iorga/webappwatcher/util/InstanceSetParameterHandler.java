package com.iorga.webappwatcher.util;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;


public class InstanceSetParameterHandler<T, I> extends BasicParameterHandler<T, Set<I>> {

	private final static Logger log = LoggerFactory.getLogger(InstanceSetParameterHandler.class);

	private final Map<Class<?>, Object> parameterscontext;

	public InstanceSetParameterHandler(final Class<T> ownerClass, final String fieldName, final Map<Class<?>, Object> parameterscontext) {
		super(ownerClass, fieldName);
		this.parameterscontext = parameterscontext;
	}



	@SuppressWarnings("unchecked")
	@Override
	protected Set<I> convertFromString(final String value) {
		final Set<I> instances = Sets.newHashSet();
		final String[] includes = value.split(",");
		for (final String className : includes) {
			if (StringUtils.isNotBlank(className)) {
				try {
					instances.add((I) parameterscontext.get(Class.forName(className)));
				} catch (final ClassNotFoundException e) {
					log.warn("Can't get instance of "+className, e);
				}
			}
		}
		return instances;
	}

	@Override
	protected String convertToString(final Set<I> value) {
		if (value == null) {
			return "";
		}
		final StringBuilder stringBuilder = new StringBuilder();
		boolean first = true;
		for (final I instance : value) {
			if (!first) {
				stringBuilder.append(',');
			} else {
				first = false;
			}
			stringBuilder.append(instance.getClass().getName());
		}
		return stringBuilder.toString();
	}

	@Override
	public boolean isFieldSetIfBlank() {
		return true;
	}
}

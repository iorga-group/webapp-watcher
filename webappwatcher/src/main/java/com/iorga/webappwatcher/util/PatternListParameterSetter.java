package com.iorga.webappwatcher.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public final class PatternListParameterSetter<T> extends BasicParameterSetter<T, List<Pattern>> {
	public PatternListParameterSetter(final Class<T> ownerClass, final String fieldName) {
		super(ownerClass, fieldName);
	}

	@Override
	public List<Pattern> convertFromString(final String value) {
		final List<Pattern> patterns = new ArrayList<Pattern>();
		final String[] includes = value.split(",");
		for (final String include : includes) {
			if (StringUtils.isNotBlank(include)) {
				patterns.add(Pattern.compile(include));
			}
		}
		return patterns;
	}

	@Override
	public boolean isFieldSetIfBlank() {
		return true;
	}
}
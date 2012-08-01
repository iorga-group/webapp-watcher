package com.iorga.webappwatcher.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public final class PatternListParameterHandler<T> extends BasicParameterHandler<T, List<Pattern>> {
	public PatternListParameterHandler(final Class<T> ownerClass, final String fieldName) {
		super(ownerClass, fieldName);
	}

	@Override
	protected List<Pattern> convertFromString(final String value) {
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
	protected String convertToString(final List<Pattern> value) {
		if (value == null) {
			return "";
		}
		final StringBuilder stringBuilder = new StringBuilder();
		boolean first = true;
		for (final Pattern pattern : value) {
			if (!first) {
				stringBuilder.append(',');
			} else {
				first = false;
			}
			stringBuilder.append(pattern.pattern());
		}
		return stringBuilder.toString();
	}

	@Override
	public boolean isFieldSetIfBlank() {
		return true;
	}
}
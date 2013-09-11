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
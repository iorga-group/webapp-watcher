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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PatternDurationParameterHandler<T> extends BasicParameterHandler<T, List<PatternDuration>> {
	private static final Pattern patternDurationPattern = Pattern.compile("(.+?):(\\d+),?");

	public PatternDurationParameterHandler(final Class<T> ownerClass, final String fieldName) {
		super(ownerClass, fieldName);
	}

	@Override
	protected List<PatternDuration> convertFromString(final String value) {
		final List<PatternDuration> patternDurations = new ArrayList<PatternDuration>();
		final Matcher matcher = patternDurationPattern.matcher(value);
		while (matcher.find()) {
			final PatternDuration patternDuration = new PatternDuration(Pattern.compile(matcher.group(1)), new Integer(matcher.group(2)));
			patternDurations.add(patternDuration);
		}
		return patternDurations;
	}

	@Override
	protected String convertToString(final List<PatternDuration> value) {
		if (value == null) {
			return "";
		}
		final StringBuilder stringBuilder = new StringBuilder();
		boolean first = true;
		for (final PatternDuration patternDuration : value) {
			if (!first) {
				stringBuilder.append(',');
			} else {
				first = false;
			}
			stringBuilder.append(patternDuration.getPattern().pattern()).append(":").append(patternDuration.getDuration());
		}
		return stringBuilder.toString();
	}

	@Override
	public boolean isFieldSetIfBlank() {
		return true;
	}
}
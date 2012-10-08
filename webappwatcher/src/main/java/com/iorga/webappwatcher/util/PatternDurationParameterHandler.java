package com.iorga.webappwatcher.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PatternDurationParameterHandler<T> extends BasicParameterHandler<T, List<PatternDuration>> {
	private static final Pattern patternDurationPattern = Pattern.compile("(.+):(\\d+),??");

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
package com.iorga.webappwatcher.util;

import java.util.regex.Pattern;

public class PatternDuration {
	private Pattern pattern;
	private Integer duration;


	public PatternDuration() {
	}

	public PatternDuration(final Pattern pattern, final Integer integer) {
		this.pattern = pattern;
		this.duration = integer;
	}


	public Pattern getPattern() {
		return pattern;
	}

	public void setPattern(final Pattern pattern) {
		this.pattern = pattern;
	}

	public Integer getDuration() {
		return duration;
	}

	public void setDuration(final Integer duration) {
		this.duration = duration;
	}
}

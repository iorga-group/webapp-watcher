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

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

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class PatternUtils {

	public static boolean matches(final String input, final List<Pattern> includes, final List<Pattern> excludes) {
		boolean matches = false;
		if (includes != null) {
			for (final Iterator<Pattern> iterator = includes.iterator(); iterator.hasNext() && !matches;) {
				final Pattern include = iterator.next();
				matches |= include.matcher(input).matches();
			}
		}
		if (excludes != null) {
			for (final Iterator<Pattern> iterator = excludes.iterator(); iterator.hasNext() && matches;) {
				final Pattern exclude = iterator.next();
				matches &= !exclude.matcher(input).matches();
			}
		}
		return matches;
	}
}

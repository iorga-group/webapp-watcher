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

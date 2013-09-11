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
package com.iorga.iraj.util;

import org.junit.Assert;
import org.junit.Test;

public class QueryDSLUtilsTest {
	@Test
	public void parseOrderSpecifier() {
		Assert.assertEquals(QUser.user.firstName.asc(), QueryDSLUtils.parseOrderSpecifier("firstName", "ASC", QUser.user));
		Assert.assertEquals(QUser.user.profile.label.desc(), QueryDSLUtils.parseOrderSpecifier("profile.label", "DESC", QUser.user));
		Assert.assertEquals(QUser.user.lastName.desc(), QueryDSLUtils.parseOrderSpecifier("lastName", "desc", QUser.user));
		Assert.assertEquals(QUser.user.profile.code.asc(), QueryDSLUtils.parseOrderSpecifier("profile.code", "asc", QUser.user));
	}
}

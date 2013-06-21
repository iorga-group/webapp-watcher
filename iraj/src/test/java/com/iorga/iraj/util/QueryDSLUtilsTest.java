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

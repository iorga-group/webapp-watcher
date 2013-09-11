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
package com.iorga.webappwatcher.analyzer.ws.session;

import java.lang.reflect.InvocationTargetException;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.apache.commons.beanutils.PropertyUtils;

import com.iorga.iraj.util.BeanManagerUtils;
import com.iorga.webappwatcher.analyzer.model.session.Configurations;

@Path("/session/configurations")
public class ConfigurationsWS {
	@Inject
	private BeanManager beanManager;

	@GET
	@Path("/load")
	public Configurations load() {
		return getConfigurations();
	}

	@POST
	@Path("/save")
	public void save(final Configurations configurations) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		PropertyUtils.copyProperties(getConfigurations(), configurations);
	}

	private Configurations getConfigurations() {
		return BeanManagerUtils.getOrCreateInstance(beanManager, Configurations.class); // Using that to get the real instance of Configurations and not a Weld Proxy
	}
}

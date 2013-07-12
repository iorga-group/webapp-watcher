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

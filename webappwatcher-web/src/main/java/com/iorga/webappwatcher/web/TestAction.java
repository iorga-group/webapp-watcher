package com.iorga.webappwatcher.web;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;

@ViewScoped
@ManagedBean
public class TestAction implements Serializable {
	private static final long serialVersionUID = 1L;

	private final String label = "OK";

	@PostConstruct
	public void delayRequest() throws InterruptedException {
		final Map<String, String> requestParameterMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		final String duration = requestParameterMap.get("duration");
		if (StringUtils.isNotBlank(duration)) {
			final long durationMillis = Long.parseLong(duration) * 1000L;
			final String useCpu = requestParameterMap.get("useCpu");
			if (StringUtils.isNotBlank(useCpu) && Boolean.parseBoolean(useCpu)) {
				final Date startDate = new Date();
				while (new Date().getTime() - startDate.getTime() < durationMillis);
			} else {
				Thread.sleep(durationMillis);
			}
		}
	}


	public String getLabel() {
		return label;
	}

}

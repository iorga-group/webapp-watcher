package com.iorga.webappwatcher.web;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.lang.StringUtils;

@WebFilter(filterName = "Test Filter")
public class TestFilter implements Filter {

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {}

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
		final String principal = request.getParameter("principal");
		if (StringUtils.isNotBlank(principal)) {
			chain.doFilter(new HttpServletRequestWrapper((HttpServletRequest) request) {
				@Override
				public Principal getUserPrincipal() {
					return new Principal() {
						@Override
						public String getName() {
							return principal;
						}
					};
				}
			}, response);
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {}

}

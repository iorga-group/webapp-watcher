package com.iorga.iraj.security;

public class SimpleSecurityContext implements SecurityContext {
	private final String secretAccessKey;

	public SimpleSecurityContext(final String secretAccessKey) {
		this.secretAccessKey = secretAccessKey;
	}

	@Override
	public String getSecretAccessKey() {
		return secretAccessKey;
	}
}

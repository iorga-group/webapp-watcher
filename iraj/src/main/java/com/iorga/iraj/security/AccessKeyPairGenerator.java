package com.iorga.iraj.security;


public class AccessKeyPairGenerator {

	public static void main(final String[] args) {
		System.out.println(SecurityUtils.generateAccessKeyId()+" / "+SecurityUtils.generateSecretAccessKey());
	}

}

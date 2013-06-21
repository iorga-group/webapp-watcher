package com.iorga.iraj.json;

import java.lang.reflect.Type;

public class OnlyReturnContextCaller implements ContextCaller {
	private final Type returnType;

	public OnlyReturnContextCaller(final Type returnType) {
		this.returnType = returnType;
	}

	@Override
	public Object callContext(final Object context) {
		return context;
	}

	@Override
	public Type getReturnType() {
		return returnType;
	}

}

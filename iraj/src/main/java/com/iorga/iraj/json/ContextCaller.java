package com.iorga.iraj.json;

import java.lang.reflect.Type;

public interface ContextCaller {
	public Object callContext(Object context);
	public Type getReturnType();
}

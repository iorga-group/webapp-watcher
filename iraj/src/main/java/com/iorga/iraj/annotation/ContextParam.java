package com.iorga.iraj.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ContextParam {
	String name() default "";
	Class<?> value();
	Class<?>[] parameterizedArguments() default {};
}

package com.iorga.iraj.validation;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import org.hibernate.validator.method.MethodConstraintViolation;
import org.hibernate.validator.method.MethodConstraintViolationException;
import org.hibernate.validator.method.MethodValidator;
import org.jboss.resteasy.spi.validation.DoNotValidateRequest;
import org.jboss.resteasy.spi.validation.ValidateRequest;
import org.jboss.resteasy.spi.validation.ValidatorAdapter;
import org.jboss.resteasy.util.FindAnnotation;

// copied from https://github.com/resteasy/Resteasy/blob/2.3.6/providers/resteasy-hibernatevalidator-provider/src/main/java/org/jboss/resteasy/plugins/validation/hibernate/HibernateValidatorAdapter.java
public class HibernateValidatorAdaptor implements ValidatorAdapter {
	private final Validator validator;
	private final MethodValidator methodValidator;

	HibernateValidatorAdaptor(final Validator validator) {
		if( validator == null )
			throw new IllegalArgumentException("Validator cannot be null");

		this.validator = validator;
		this.methodValidator = validator.unwrap(MethodValidator.class);
	}

	@Override
	public void applyValidation(final Object resource, final Method invokedMethod, final Object[] args) {
		final ValidateRequest resourceValidateRequest = FindAnnotation.findAnnotation(invokedMethod.getDeclaringClass().getAnnotations(), ValidateRequest.class);

		if( resourceValidateRequest != null ) {
			final Set<ConstraintViolation<?>> constraintViolations = new HashSet<ConstraintViolation<?>>( validator.validate(resource, resourceValidateRequest.groups()) );

			if( constraintViolations.size() > 0 )
				throw new ConstraintViolationException(constraintViolations);
		}

		final ValidateRequest methodValidateRequest = FindAnnotation.findAnnotation(invokedMethod.getAnnotations(), ValidateRequest.class);
		final DoNotValidateRequest doNotValidateRequest = FindAnnotation.findAnnotation(invokedMethod.getAnnotations(), DoNotValidateRequest.class);

		if( (resourceValidateRequest != null || methodValidateRequest != null) && doNotValidateRequest == null ) {
			final Set<Class<?>> set = new HashSet<Class<?>>();
			if( resourceValidateRequest != null ) {
				for (final Class<?> group : resourceValidateRequest.groups()) {
					set.add(group);
				}
			}

			if( methodValidateRequest != null ) {
				for (final Class<?> group : methodValidateRequest.groups()) {
					set.add(group);
				}
			}

			final Set<MethodConstraintViolation<?>> constraintViolations = new HashSet<MethodConstraintViolation<?>>(methodValidator.validateAllParameters(resource, invokedMethod, args, set.toArray(new Class<?>[set.size()])));

			if(constraintViolations.size() > 0)
				throw new MethodConstraintViolationException(constraintViolations);
		}
	}

}

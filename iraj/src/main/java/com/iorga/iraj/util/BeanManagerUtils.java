package com.iorga.iraj.util;

import java.lang.annotation.Annotation;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

public class BeanManagerUtils {

	@SuppressWarnings("unchecked")
	public static <T> Bean<T> resolveBean(final BeanManager beanManager, final Class<T> type, final Annotation... qualifiers) {
		return (Bean<T>) beanManager.resolve(beanManager.getBeans(type, qualifiers));
	}

	public static <T> T getExistingInstance(final BeanManager beanManager, final Class<T> type, final Annotation... qualifiers) {
		final Bean<T> bean = resolveBean(beanManager, type, qualifiers);
		if (bean != null) {
			return beanManager.getContext(bean.getScope()).get(bean);
		} else {
			return null;
		}
	}

	public static <T> T getExistingInstance(final BeanManager beanManager, final Class<T> type, final Class<? extends Annotation> scope, final Annotation... qualifiers) {
		return getExistingInstance(beanManager, resolveBean(beanManager, type, qualifiers), scope);
	}

	public static <T> T getExistingInstance(final BeanManager beanManager, final Bean<T> bean, final Class<? extends Annotation> scope) {
		return beanManager.getContext(scope).get(bean);
	}

	public static <T> T getOrCreateInstance(final BeanManager beanManager, final Class<T> type, final Class<? extends Annotation> scope, final Annotation... qualifiers) {
		final Bean<T> bean = resolveBean(beanManager, type, qualifiers);
		return getOrCreateInstance(beanManager, bean, scope);
	}

	public static <T> T getOrCreateInstance(final BeanManager beanManager, final Bean<T> bean, final Class<? extends Annotation> scope) {
		if (bean != null) {
			return beanManager.getContext(scope).get(bean, beanManager.createCreationalContext(bean));
		} else {
			return null;
		}
	}

	public static <T> T getOrCreateInstance(final BeanManager beanManager, final Bean<T> bean) {
		if (bean != null) {
			return beanManager.getContext(bean.getScope()).get(bean, beanManager.createCreationalContext(bean));
		} else {
			return null;
		}
	}

	public static <T> T getOrCreateInstance(final BeanManager beanManager, final Class<T> type, final Annotation... qualifiers) {
		final Bean<T> bean = resolveBean(beanManager, type, qualifiers);
		if (bean != null) {
			return getOrCreateInstance(beanManager, bean, bean.getScope());
		} else {
			return null;
		}
	}
}

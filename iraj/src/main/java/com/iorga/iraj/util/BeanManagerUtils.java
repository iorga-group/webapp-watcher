/*
 * Copyright (C) 2013 Iorga Group
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/].
 */
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

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
package com.iorga.iraj.transaction;

import javax.enterprise.context.ContextException;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iorga.iraj.annotation.Transactional;

@Interceptor
@Transactional
public class TransactionalInterceptor {
	private static final Logger log = LoggerFactory.getLogger(TransactionalInterceptor.class);

	@Inject
	Instance<EntityManager> entityManagerInstance;

	@AroundInvoke
	public Object interceptTransactional(final InvocationContext invocationContext) throws Exception {
		EntityManager entityManager = null;
		EntityTransaction transaction = null;
		try {
			entityManager = entityManagerInstance.get();
			transaction = entityManager.getTransaction();
		} catch (final ContextException e) {
			log.warn("Problem while loading the entityManager, bypassing "+TransactionalInterceptor.class.getName(), e);
		}
		if (transaction != null) {
			boolean transactionBegun = false;
			if (!transaction.isActive() && !ManualTransactionalInterceptor.isCurrentThreadInManualTransaction()) {
				transaction.begin();
				transactionBegun = true;
			}
			try {
				final Object result = invocationContext.proceed();
				if (transactionBegun) {
					transaction.commit();
				}
				return result;
			} catch (final Throwable throwable) {
				if (transactionBegun) {
					if (transaction.isActive()) {
						transaction.rollback();
					}
				}
				if (throwable instanceof Error) {
					throw (Error) throwable;
				} else {
					throw (Exception) throwable;
				}
			}
		} else {
			return invocationContext.proceed();
		}
	}
}

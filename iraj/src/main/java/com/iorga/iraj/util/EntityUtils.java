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

import javax.persistence.EntityManager;
import javax.persistence.metamodel.Type;

import org.hibernate.proxy.HibernateProxy;

public class EntityUtils {

	@SuppressWarnings("unchecked")
	public static <I> I getIdForInstance(Object entityInstance, final EntityManager entityManager) {
		if (entityInstance instanceof HibernateProxy) {
			// Resolve real instances of that proxy first before trying to use the PersistenceUnitUtil to resolve the identifier
			// http://stackoverflow.com/questions/3787716/introspection-table-name-of-an-object-managed-by-hibernate-javassistlazyiniti
			// TODO : peut-être récupérer directement getIdentifier sur le LazyInitializer plutôt que d'appeler le PersistenceUnitUtil
			entityInstance = ((HibernateProxy)entityInstance).getHibernateLazyInitializer().getImplementation();
		}
		return (I) entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entityInstance);
    }

	public static Type<?> getIdType(final Class<?> entityClass, final EntityManager entityManager) {
		return entityManager.getEntityManagerFactory().getMetamodel().entity(entityClass).getIdType();
	}

	public static String getEntityName(final Class<?> entityClass, final EntityManager entityManager) {
		return entityManager.getEntityManagerFactory().getMetamodel().entity(entityClass).getName();
	}

}

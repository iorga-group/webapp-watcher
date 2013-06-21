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

package com.iorga.iraj.service;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.service.spi.ServiceException;

import com.google.common.reflect.TypeToken;
import com.iorga.iraj.util.EntityUtils;

public abstract class JPAEntityService<E, I> extends JPAService implements EntityService<E, I> {

	private TypeToken<E> entityTypeToken;
	private String entityName;

	protected JPAEntityService() {}

	@Override
	public void create(final E entity) {
		getEntityManager().persist(entity);
	}

	@Override
	public E find(final I id) {
		return getEntityManager().find(getEntityTypeToken(), id);
	}

	public E getReference(final I id) {
		return getEntityManager().getReference(getEntityTypeToken(), id);
	}

	public void refresh(final E entity) {
		getEntityManager().refresh(entity);
	}

	public List<E> findAll() {
		final EntityManager entityManager = getEntityManager();
		final Class<E> entityClass = getEntityTypeToken();
		final CriteriaQuery<E> query = entityManager.getCriteriaBuilder().createQuery(entityClass);
		query.from(entityClass);
		return entityManager.createQuery(query).getResultList();
	}
	
	public List<E> findAllWithOrderBy(String column) {
		final EntityManager entityManager = getEntityManager();
		final Class<E> entityClass = getEntityTypeToken();
		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<E> query = cb.createQuery(entityClass);
		Root<E> b = query.from(entityClass);
		query.orderBy(cb.asc(b.get(column)));
		return entityManager.createQuery(query).getResultList();
	}

	public void deleteAll() {
		getEntityManager().createQuery("delete from "+getEntityName()).executeUpdate();
	}

	@Override
	public E update(final E entity) {
		return getEntityManager().merge(entity);
	}

	@Override
	public void delete(final E entity) {
		getEntityManager().remove(entity);
	}

	@SuppressWarnings("unchecked")
	public Class<E> getEntityTypeToken() {
		if (entityTypeToken == null) {
			entityTypeToken = new TypeToken<E>(getClass()) {private static final long serialVersionUID = 1L;};
		}
		return (Class<E>) entityTypeToken.getRawType();
	}

	@Override
	public I getIdForInstance(final E instance) {
		return EntityUtils.getIdForInstance(instance, getEntityManager());
    }

	protected String getEntityName() {
		if (entityName == null) {
			entityName = EntityUtils.getEntityName(getEntityTypeToken(), getEntityManager());
		}
		return entityName;
	}

	public E loadIfRequired(E entity) throws ServiceException {
		if (!getEntityManager().contains(entity)) {
			entity = find(getIdForInstance(entity));
		}
		return entity;
	}
}

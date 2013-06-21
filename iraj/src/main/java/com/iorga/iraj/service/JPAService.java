package com.iorga.iraj.service;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import com.iorga.iraj.annotation.Transactional;

@Transactional
@ApplicationScoped
public abstract class JPAService {
	@Inject
	private Instance<EntityManager> entityManagerInstance;

	protected EntityManager getEntityManager() {
		return entityManagerInstance.get();
	}
}

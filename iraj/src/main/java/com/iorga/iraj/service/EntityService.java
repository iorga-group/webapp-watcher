package com.iorga.iraj.service;

public interface EntityService<E, I> {

	public abstract void delete(final E entity);

	public abstract E update(final E entity);

	public abstract E find(final I id);

	public abstract void create(final E entity);

	public abstract I getIdForInstance(final E instance);

}
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
package com.iorga.iraj.message;

public class FieldMessage extends Message {
	private String id;
	private Iterable<String> propertyPath;


	public FieldMessage(final String message, final MessageType type, final String id, final Iterable<String> propertyPath) {
		super(message, type);
		this.id = id;
		this.propertyPath = propertyPath;
	}


	/// Getters & Setters ///
	////////////////////////
	public String getId() {
		return id;
	}
	public void setId(final String id) {
		this.id = id;
	}
	public Iterable<String> getPropertyPath() {
		return propertyPath;
	}
	public void setPropertyPath(final Iterable<String> propertyPath) {
		this.propertyPath = propertyPath;
	}
}

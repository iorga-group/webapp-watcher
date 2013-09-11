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

import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Lists;

public class MessagesBuilder {
	private final List<FieldMessage> fieldMessages = new LinkedList<FieldMessage>();
	private final List<Message> messages = new LinkedList<Message>();


	public Messages build() {
		return new Messages(Lists.newArrayList(fieldMessages), Lists.newArrayList(messages));
	}

	public boolean isEmpty() {
		return fieldMessages.isEmpty() && messages.isEmpty();
	}


	public MessagesBuilder appendMessage(final String message, final MessageType type) {
		messages.add(new Message(message, type));
		return this;
	}

	public MessagesBuilder appendWarning(final String message) {
		appendMessage(message, MessageType.WARNING);
		return this;
	}

	public MessagesBuilder appendError(final String message) {
		appendMessage(message, MessageType.ERROR);
		return this;
	}

	public MessagesBuilder appendInfo(final String message) {
		appendMessage(message, MessageType.INFO);
		return this;
	}

	public MessagesBuilder appendSuccess(final String message) {
		appendMessage(message, MessageType.SUCCESS);
		return this;
	}


	public MessagesBuilder appendFieldMessage(final String message, final MessageType type, final String... propertyPath) {
		final FieldMessage fieldMessage;
		if (propertyPath != null) {
			fieldMessage = new FieldMessage(message, type, null, Lists.newArrayList(propertyPath));
		} else {
			fieldMessage = new FieldMessage(message, type, null, null);
		}
		fieldMessages.add(fieldMessage);
		return this;
	}

	public MessagesBuilder appendFieldWarning(final String message, final String... propertyPath) {
		appendFieldMessage(message, MessageType.WARNING, propertyPath);
		return this;
	}

	public MessagesBuilder appendFieldError(final String message, final String... propertyPath) {
		appendFieldMessage(message, MessageType.ERROR, propertyPath);
		return this;
	}

	public MessagesBuilder appendFieldInfo(final String message, final String... propertyPath) {
		appendFieldMessage(message, MessageType.INFO, propertyPath);
		return this;
	}

	public MessagesBuilder appendFieldSuccess(final String message, final String... propertyPath) {
		appendFieldMessage(message, MessageType.SUCCESS, propertyPath);
		return this;
	}
}

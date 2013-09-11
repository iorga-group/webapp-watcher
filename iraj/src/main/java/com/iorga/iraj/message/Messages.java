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

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import com.iorga.iraj.json.JsonWriter;

public class Messages {
	private static final JsonWriter JSON_WRITER = new JsonWriter();

	private final List<FieldMessage> fieldMessages;
	private final List<Message> messages;


	public Messages(final List<FieldMessage> fieldMessages, final List<Message> messages) {
		this.fieldMessages = fieldMessages;
		this.messages = messages;
	}

	public String getFirstMessage() {
		if (messages.size() > 0) {
			return messages.get(0).getMessage();
		} else if (fieldMessages.size() > 0) {
			return fieldMessages.get(0).getMessage();
		} else {
			return null;
		}
	}

	public boolean isEmpty() {
		return fieldMessages.isEmpty() && messages.isEmpty();
	}

	public void writeToOutputStream(final OutputStream outputStream) throws WebApplicationException, IOException {
		JSON_WRITER.writeWithTemplate(MessagesTemplate.class, this).write(outputStream);
	}


	/// Getters & Setters ///
	////////////////////////
	public List<FieldMessage> getFieldMessages() {
		return fieldMessages;
	}
	public List<Message> getMessages() {
		return messages;
	}

}

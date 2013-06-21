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

package com.iorga.iraj.message;

public class Message {
	private String message;
	private MessageType type;


	public Message(final String message, final MessageType type) {
		this.message = message;
		this.type = type;
	}


	/// Getters & Setters ///
	////////////////////////
	public String getMessage() {
		return message;
	}
	public void setMessage(final String message) {
		this.message = message;
	}
	public MessageType getType() {
		return type;
	}
	public void setType(final MessageType type) {
		this.type = type;
	}
}

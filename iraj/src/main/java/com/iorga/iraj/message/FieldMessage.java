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

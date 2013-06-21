package com.iorga.iraj.message;

public enum MessageType {
	WARNING, ERROR, INFO, SUCCESS;

	public String jsonValue() {
		return toString().toLowerCase();
	}
}

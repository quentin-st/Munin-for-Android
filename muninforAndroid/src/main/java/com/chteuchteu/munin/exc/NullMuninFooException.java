package com.chteuchteu.munin.exc;

@SuppressWarnings("serial")
public class NullMuninFooException extends Exception {
	public NullMuninFooException(String message) {
		super(message);
	}

	public NullMuninFooException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
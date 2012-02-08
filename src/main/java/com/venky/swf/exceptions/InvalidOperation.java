package com.venky.swf.exceptions;

public class InvalidOperation extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6440331015155813184L;
	

	public InvalidOperation() {
	}

	public InvalidOperation(String message) {
		super(message);
	}

	public InvalidOperation(Throwable cause) {
		super(cause);
	}

	public InvalidOperation(String message, Throwable cause) {
		super(message, cause);
	}


}

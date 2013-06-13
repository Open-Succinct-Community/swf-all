package com.venky.swf.exceptions;

public class IncompleteDataException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 809101847413646997L;

	public IncompleteDataException() {
		super();
	}

	public IncompleteDataException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public IncompleteDataException(String message, Throwable cause) {
		super(message, cause);
	}

	public IncompleteDataException(String message) {
		super(message);
	}

	public IncompleteDataException(Throwable cause) {
		super(cause);
	}

}

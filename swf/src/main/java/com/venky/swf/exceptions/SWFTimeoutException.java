package com.venky.swf.exceptions;

public class SWFTimeoutException extends RuntimeException{

	public SWFTimeoutException() {
		super();
	}

	public SWFTimeoutException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public SWFTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}

	public SWFTimeoutException(String message) {
		super(message);
	}

	public SWFTimeoutException(Throwable cause) {
		super(cause);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 352157071459390976L;
	
}

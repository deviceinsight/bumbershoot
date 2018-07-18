package com.deviceinsight.bumbershoot.exception;

@SuppressWarnings("serial")
public class ChartInvalidException extends Exception {

	public ChartInvalidException() {
	}

	public ChartInvalidException(String message, Throwable cause) {
		super(message, cause);
	}

	public ChartInvalidException(String message) {
		super(message);
	}

	public ChartInvalidException(Throwable cause) {
		super(cause);
	}


}

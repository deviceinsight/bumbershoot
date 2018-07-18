package com.deviceinsight.bumbershoot.exception;

@SuppressWarnings("serial")
public class ChartDownloadException extends Exception {

	public ChartDownloadException(Throwable e) {
		super(e);
	}

	public ChartDownloadException() {
		super();
	}

	public ChartDownloadException(String message, Throwable cause) {
		super(message, cause);
	}

	public ChartDownloadException(String message) {
		super(message);
	}

}

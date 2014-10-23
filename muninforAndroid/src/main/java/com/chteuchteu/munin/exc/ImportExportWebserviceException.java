package com.chteuchteu.munin.exc;

@SuppressWarnings("serial")
public class ImportExportWebserviceException extends Exception {
	public ImportExportWebserviceException(String message) {
		super(message);
	}
	
	public ImportExportWebserviceException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
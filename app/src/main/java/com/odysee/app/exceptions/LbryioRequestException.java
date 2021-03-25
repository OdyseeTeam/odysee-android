package com.odysee.app.exceptions;

public class LbryioRequestException extends Exception {
    public LbryioRequestException() {
        super();
    }
    public LbryioRequestException(String message) {
        super(message);
    }
    public LbryioRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}

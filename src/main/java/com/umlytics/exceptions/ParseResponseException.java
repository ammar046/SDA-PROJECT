package com.umlytics.exceptions;

public class ParseResponseException extends RuntimeException {
    public ParseResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.umlytics.exceptions;

public class SpeechServiceException extends RuntimeException {
    public SpeechServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

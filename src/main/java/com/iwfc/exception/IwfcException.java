package com.iwfc.exception;

// Base class for all custom exceptions so the UI can catch them in one place
public abstract class IwfcException extends RuntimeException {

    protected IwfcException(String message) {
        super(message);
    }
}

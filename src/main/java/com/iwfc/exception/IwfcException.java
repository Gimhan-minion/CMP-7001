package com.iwfc.exception;

public abstract class IwfcException extends RuntimeException {

    protected IwfcException(String message) {
        super(message);
    }
}

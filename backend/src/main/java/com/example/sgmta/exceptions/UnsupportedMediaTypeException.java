package com.example.sgmta.exceptions;

public class UnsupportedMediaTypeException extends RuntimeException {
    public UnsupportedMediaTypeException(String message) {
        super(message);
    }

    public UnsupportedMediaTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}

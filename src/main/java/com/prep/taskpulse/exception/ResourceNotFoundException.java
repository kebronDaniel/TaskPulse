package com.prep.taskpulse.exception;

// this enforces specificity (needed to be extended)
public abstract class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

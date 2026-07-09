package com.prep.taskpulse.exception;

import java.util.UUID;

public class UserNotFoundException extends ResourceNotFoundException{
    public UserNotFoundException(String message) {
        super(message);
    }
    public UserNotFoundException(UUID id) {
        super("User not found : " + id);
    }

}

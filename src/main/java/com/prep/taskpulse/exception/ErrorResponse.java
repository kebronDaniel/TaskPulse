package com.prep.taskpulse.exception;

import java.time.Instant;

public record ErrorResponse (
        Instant timestamp,
        int status,
        String message
){
}

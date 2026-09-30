package com.example.ticketsystem.exception;

public class UnauthorizedException extends RuntimeException {    
    
    public UnauthorizedException(String message) {
        super(message);
    }
}

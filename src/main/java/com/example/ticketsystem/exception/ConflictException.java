package com.example.ticketsystem.exception;

public class ConflictException extends RuntimeException {    
    
    public ConflictException(String message) {
        super(message);
    }
}

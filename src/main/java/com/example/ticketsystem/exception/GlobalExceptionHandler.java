package com.example.ticketsystem.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.ticketsystem.dto.response.MessageResponse;

@RestControllerAdvice 
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UnauthorizedException.class) 
    public ResponseEntity<MessageResponse> handleUnauthorized(UnauthorizedException e) {
        MessageResponse body = new MessageResponse(e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(BadRequestException.class) 
    public ResponseEntity<MessageResponse> handleBadRequest(BadRequestException e) {
        MessageResponse body = new MessageResponse(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConflictException.class) 
    public ResponseEntity<MessageResponse> handleConflict(ConflictException e) {
        MessageResponse body = new MessageResponse(e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(ForbiddenException.class) 
    public ResponseEntity<MessageResponse> handleForbidden(ForbiddenException e) {
        MessageResponse body = new MessageResponse(e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<MessageResponse> handleNotFound(NotFoundException e) {
        MessageResponse body = new MessageResponse(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}

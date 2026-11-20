package com.example.ticketsystem.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.example.ticketsystem.dto.response.MessageResponse;

@RestControllerAdvice 
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
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

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = HttpStatus.valueOf(status.value()).getReasonPhrase();
        return ResponseEntity.status(status).headers(headers).body(new MessageResponse(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("Internal server error"));
    }
}

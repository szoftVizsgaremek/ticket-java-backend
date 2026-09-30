package com.example.ticketsystem.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class SessionStore {

    private final Map<String, Long> sessions = new ConcurrentHashMap<>();
    
    public String create(Long userId) {
        UUID token = UUID.randomUUID();
        String tokenString = token.toString();
        sessions.put(tokenString, userId);
        return tokenString;
    }

    public Long resolve(String token) {
        return sessions.get(token);
    } 

    public void invalidate(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}

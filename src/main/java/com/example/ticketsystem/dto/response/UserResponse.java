package com.example.ticketsystem.dto.response;

import java.time.LocalDate;

public record UserResponse(
    Long id,
    String name,
    String username, 
    LocalDate birthDate, 
    String role, 
    String jobTitle,
    boolean active,
    String email) {}

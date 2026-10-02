package com.example.ticketsystem.dto.request;

import java.time.LocalDate;

public record UpdateUserRequest(
    String name, 
    String email, 
    String password, 
    String role, 
    String jobTitle, 
    LocalDate birthDate) {}

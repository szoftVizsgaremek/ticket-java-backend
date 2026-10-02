package com.example.ticketsystem.dto.request;

import java.time.LocalDate;

public record CreateUserRequest(
    String name, 
    String username, 
    String email, 
    String password, 
    String role, 
    String jobTitle, 
    LocalDate birthDate) {}
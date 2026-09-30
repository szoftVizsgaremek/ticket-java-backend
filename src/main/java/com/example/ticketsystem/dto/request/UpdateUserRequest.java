package com.example.ticketsystem.dto.request;

import java.time.LocalDate;

public record UpdateUserRequest(String name, String username, String email, String password, String role, LocalDate birthDate) {}

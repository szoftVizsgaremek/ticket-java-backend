package com.example.ticketsystem.dto.response;

import java.time.LocalDate;

public record UserResponse(Long id, String name, String username, String email, String role, LocalDate birthDate) {}

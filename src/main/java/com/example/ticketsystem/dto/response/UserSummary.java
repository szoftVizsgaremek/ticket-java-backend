package com.example.ticketsystem.dto.response;

public record UserSummary(
    Long id,
    String name,
    String username, 
    String jobTitle
) {}

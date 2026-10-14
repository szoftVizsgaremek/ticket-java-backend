package com.example.ticketsystem.dto.request;

public record CreateTicketRequest(
    String name,
    String description,
    String type,
    String priority,
    Long assigneeId
) {}
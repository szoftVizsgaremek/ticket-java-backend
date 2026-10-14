package com.example.ticketsystem.dto.request;

public record UpdateTicketRequest(
    String status,
    String priority,
    Long assigneeId,
    String type
) {}

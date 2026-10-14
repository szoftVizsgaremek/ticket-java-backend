package com.example.ticketsystem.dto.response;

import java.time.Instant;

public record TicketResponse(
    Long id,
    String name,
    String description,
    String status,
    String priority,
    String type, 
    UserSummary uploader,
    UserSummary assignee,
    Instant createdAt,
    Instant updatedAt
) {}

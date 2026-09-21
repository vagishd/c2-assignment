package com.support.ticketai.dto;

import com.support.ticketai.domain.Priority;
import com.support.ticketai.domain.TicketStatus;

import java.time.Instant;

public record TicketSummary(
        Long id,
        String ticketRef,
        String title,
        TicketStatus status,
        Priority priority,
        String assignee,
        String category,
        Instant createdAt,
        Instant updatedAt
) {
}

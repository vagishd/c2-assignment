package com.support.ticketai.dto;

import com.support.ticketai.domain.Priority;
import com.support.ticketai.domain.TicketStatus;

import java.time.Instant;
import java.util.List;

public record TicketResponse(
        Long id,
        String ticketRef,
        String title,
        String description,
        TicketStatus status,
        Priority priority,
        String assignee,
        String category,
        String resolutionNotes,
        List<CommentResponse> comments,
        Instant createdAt,
        Instant updatedAt
) {
}

package com.support.ticketai.dto;

import com.support.ticketai.domain.Priority;
import jakarta.validation.constraints.Size;

/**
 * Partial update. Any null field is left unchanged. Status is NOT changed here — use the
 * transitions endpoint (see api-contract.md).
 */
public record UpdateTicketRequest(
        @Size(max = 150, message = "Title must be at most 150 characters")
        String title,

        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description,

        Priority priority,

        @Size(max = 100, message = "Assignee must be at most 100 characters")
        String assignee
) {
}

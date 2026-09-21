package com.support.ticketai.dto;

import com.support.ticketai.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransitionRequest(
        @NotNull(message = "targetStatus is required")
        TicketStatus targetStatus,

        @Size(max = 5000, message = "Resolution notes must be at most 5000 characters")
        String resolutionNotes
) {
}

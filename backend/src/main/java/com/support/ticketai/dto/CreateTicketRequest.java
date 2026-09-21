package com.support.ticketai.dto;

import com.support.ticketai.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must be at most 150 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description,

        @NotNull(message = "Priority is required")
        Priority priority,

        @Size(max = 100, message = "Assignee must be at most 100 characters")
        String assignee,

        @Size(max = 60, message = "Category must be at most 60 characters")
        String category
) {
}

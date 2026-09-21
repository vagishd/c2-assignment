package com.support.ticketai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "Author is required")
        @Size(max = 100, message = "Author must be at most 100 characters")
        String author,

        @NotBlank(message = "Comment body is required")
        @Size(max = 3000, message = "Comment body must be at most 3000 characters")
        String body
) {
}

package com.support.ticketai.dto;

import java.time.Instant;

public record CommentResponse(
        Long id,
        String author,
        String body,
        Instant createdAt
) {
}

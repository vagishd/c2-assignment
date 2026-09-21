package com.support.ticketai.dto;

/** A cited ticket used to produce an assistant answer. */
public record TicketSource(
        String ticketId,
        String title,
        String status
) {
}

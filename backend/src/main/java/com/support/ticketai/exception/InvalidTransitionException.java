package com.support.ticketai.exception;

import com.support.ticketai.domain.TicketStatus;

public class InvalidTransitionException extends BusinessException {

    public InvalidTransitionException(TicketStatus from, TicketStatus to) {
        super("INVALID_TRANSITION", "Cannot transition ticket from " + from + " to " + to);
    }
}

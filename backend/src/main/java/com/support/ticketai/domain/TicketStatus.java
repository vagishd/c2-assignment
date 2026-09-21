package com.support.ticketai.domain;

import java.util.Map;
import java.util.Set;

/**
 * Ticket lifecycle states and the single source of truth for allowed transitions.
 *
 * <p>Allowed:
 * <pre>
 *   OPEN        -> IN_PROGRESS, CANCELLED
 *   IN_PROGRESS -> RESOLVED, CANCELLED
 *   RESOLVED    -> CLOSED
 *   CLOSED      -> (terminal)
 *   CANCELLED   -> (terminal)
 * </pre>
 * Any transition not listed here is invalid and must be rejected by the service layer.
 */
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED;

    // Defined once so the rule has a single home (see state-machine.md).
    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED = Map.of(
            OPEN, Set.of(IN_PROGRESS, CANCELLED),
            IN_PROGRESS, Set.of(RESOLVED, CANCELLED),
            RESOLVED, Set.of(CLOSED),
            CLOSED, Set.of(),
            CANCELLED, Set.of()
    );

    /**
     * @return true if this status may legally transition to {@code target}.
     */
    public boolean canTransitionTo(TicketStatus target) {
        return target != null && ALLOWED.get(this).contains(target);
    }

    /**
     * @return the set of statuses this status may legally transition to (empty for terminal states).
     */
    public Set<TicketStatus> allowedNextStates() {
        return ALLOWED.get(this);
    }
}

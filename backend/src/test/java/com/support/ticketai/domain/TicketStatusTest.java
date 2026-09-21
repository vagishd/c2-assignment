package com.support.ticketai.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static com.support.ticketai.domain.TicketStatus.CANCELLED;
import static com.support.ticketai.domain.TicketStatus.CLOSED;
import static com.support.ticketai.domain.TicketStatus.IN_PROGRESS;
import static com.support.ticketai.domain.TicketStatus.OPEN;
import static com.support.ticketai.domain.TicketStatus.RESOLVED;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exhaustive unit test of the ticket state machine (see state-machine.md, test-strategy.md).
 * Every allowed transition must pass; every other transition must be rejected.
 */
class TicketStatusTest {

    // The single source of truth for what SHOULD be allowed, kept independent of the enum's own map.
    private static final Map<TicketStatus, Set<TicketStatus>> EXPECTED_ALLOWED = Map.of(
            OPEN, Set.of(IN_PROGRESS, CANCELLED),
            IN_PROGRESS, Set.of(RESOLVED, CANCELLED),
            RESOLVED, Set.of(CLOSED),
            CLOSED, Set.of(),
            CANCELLED, Set.of()
    );

    @Test
    void canTransitionTo_coversEveryStatePair_matchesSpec() {
        for (TicketStatus from : TicketStatus.values()) {
            for (TicketStatus to : TicketStatus.values()) {
                boolean expected = EXPECTED_ALLOWED.get(from).contains(to);
                assertThat(from.canTransitionTo(to))
                        .as("%s -> %s should be %s", from, to, expected ? "allowed" : "rejected")
                        .isEqualTo(expected);
            }
        }
    }

    @Test
    void canTransitionTo_validPaths_areAllowed() {
        assertThat(OPEN.canTransitionTo(IN_PROGRESS)).isTrue();
        assertThat(OPEN.canTransitionTo(CANCELLED)).isTrue();
        assertThat(IN_PROGRESS.canTransitionTo(RESOLVED)).isTrue();
        assertThat(IN_PROGRESS.canTransitionTo(CANCELLED)).isTrue();
        assertThat(RESOLVED.canTransitionTo(CLOSED)).isTrue();
    }

    @Test
    void canTransitionTo_explicitlyRejectedCases_areRejected() {
        assertThat(CLOSED.canTransitionTo(OPEN)).isFalse();
        assertThat(RESOLVED.canTransitionTo(OPEN)).isFalse();
        assertThat(CANCELLED.canTransitionTo(OPEN)).isFalse();
        assertThat(OPEN.canTransitionTo(RESOLVED)).isFalse();
        assertThat(OPEN.canTransitionTo(CLOSED)).isFalse();
    }

    @Test
    void terminalStates_haveNoAllowedNextStates() {
        assertThat(CLOSED.allowedNextStates()).isEmpty();
        assertThat(CANCELLED.allowedNextStates()).isEmpty();
    }

    @Test
    void canTransitionTo_null_isRejected() {
        assertThat(OPEN.canTransitionTo(null)).isFalse();
    }
}

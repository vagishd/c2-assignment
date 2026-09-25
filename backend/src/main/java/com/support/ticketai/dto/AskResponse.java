package com.support.ticketai.dto;

import java.util.List;

/**
 * Assistant answer. {@code grounded} is true only when the answer was produced from retrieved
 * ticket context; when false, {@code sources} is empty and {@code answer} is the honest no-match
 * message (see rag-api-contract.md).
 */
public record AskResponse(
        String answer,
        List<TicketSource> sources,
        boolean grounded
) {
    /** The exact sentence the assistant must use when it cannot answer from ticket context. */
    public static final String NO_MATCH_MESSAGE =
            "No relevant tickets were found to answer this question.";

    public static AskResponse noMatch() {
        return new AskResponse(NO_MATCH_MESSAGE, List.of(), false);
    }
}

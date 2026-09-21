package com.support.ticketai.rag;

import com.support.ticketai.domain.Comment;
import com.support.ticketai.domain.Ticket;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds a single, self-describing knowledge {@link Document} from a ticket (see rag-ingestion.md).
 * The {@code [TKT-xxxx]} prefix and metadata header make each chunk citable and filterable.
 */
@Component
public class KnowledgeDocumentFactory {

    public static final String META_TICKET_ID = "ticketId";
    public static final String META_STATUS = "status";
    public static final String META_PRIORITY = "priority";
    public static final String META_ASSIGNEE = "assignee";
    public static final String META_CATEGORY = "category";
    public static final String META_TITLE = "title";

    public Document build(Ticket ticket) {
        String text = buildText(ticket);
        Map<String, Object> metadata = buildMetadata(ticket);
        return new Document(text, metadata);
    }

    private String buildText(Ticket ticket) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(ticket.getTicketRef()).append("] ").append(ticket.getTitle()).append("\n");
        sb.append("Status: ").append(ticket.getStatus())
                .append(" | Priority: ").append(ticket.getPriority())
                .append(" | Assignee: ").append(valueOrDash(ticket.getAssignee()))
                .append(" | Category: ").append(valueOrDash(ticket.getCategory()))
                .append("\n\n");

        sb.append("Description:\n").append(ticket.getDescription()).append("\n");

        if (!ticket.getComments().isEmpty()) {
            sb.append("\nComments:\n");
            for (Comment comment : ticket.getComments()) {
                sb.append("- ").append(comment.getAuthor()).append(": ").append(comment.getBody()).append("\n");
            }
        }

        if (ticket.getResolutionNotes() != null && !ticket.getResolutionNotes().isBlank()) {
            sb.append("\nResolution:\n").append(ticket.getResolutionNotes()).append("\n");
        }

        return sb.toString();
    }

    private Map<String, Object> buildMetadata(Ticket ticket) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(META_TICKET_ID, ticket.getTicketRef());
        metadata.put(META_TITLE, ticket.getTitle());
        metadata.put(META_STATUS, ticket.getStatus().name());
        metadata.put(META_PRIORITY, ticket.getPriority().name());
        metadata.put(META_ASSIGNEE, valueOrDash(ticket.getAssignee()));
        metadata.put(META_CATEGORY, valueOrDash(ticket.getCategory()));
        return metadata;
    }

    private String valueOrDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }
}

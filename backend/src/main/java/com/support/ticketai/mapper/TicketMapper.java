package com.support.ticketai.mapper;

import com.support.ticketai.domain.Comment;
import com.support.ticketai.domain.Ticket;
import com.support.ticketai.dto.CommentResponse;
import com.support.ticketai.dto.TicketResponse;
import com.support.ticketai.dto.TicketSummary;
import org.springframework.stereotype.Component;

import java.util.List;

/** Maps domain entities to response DTOs. Entities are never exposed at the API boundary. */
@Component
public class TicketMapper {

    public TicketResponse toResponse(Ticket ticket) {
        List<CommentResponse> comments = ticket.getComments().stream()
                .map(this::toCommentResponse)
                .toList();

        return new TicketResponse(
                ticket.getId(),
                ticket.getTicketRef(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getCategory(),
                ticket.getResolutionNotes(),
                comments,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    public TicketSummary toSummary(Ticket ticket) {
        return new TicketSummary(
                ticket.getId(),
                ticket.getTicketRef(),
                ticket.getTitle(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getCategory(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    public CommentResponse toCommentResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuthor(),
                comment.getBody(),
                comment.getCreatedAt()
        );
    }
}

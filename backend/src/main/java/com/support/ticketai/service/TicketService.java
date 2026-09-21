package com.support.ticketai.service;

import com.support.ticketai.domain.Comment;
import com.support.ticketai.domain.Ticket;
import com.support.ticketai.domain.TicketStatus;
import com.support.ticketai.dto.CreateCommentRequest;
import com.support.ticketai.dto.CreateTicketRequest;
import com.support.ticketai.dto.TransitionRequest;
import com.support.ticketai.dto.UpdateTicketRequest;
import com.support.ticketai.exception.InvalidTransitionException;
import com.support.ticketai.exception.TicketNotFoundException;
import com.support.ticketai.rag.TicketIngestionService;
import com.support.ticketai.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketIngestionService ingestionService;

    public Ticket create(CreateTicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setPriority(request.priority());
        ticket.setAssignee(request.assignee());
        ticket.setCategory(request.category());
        ticket.setStatus(TicketStatus.OPEN);

        // ticketRef is assigned in Ticket#assignTicketRef (@PrePersist) from the generated id.
        Ticket saved = ticketRepository.save(ticket);

        log.info("Created ticket {} ({})", saved.getTicketRef(), saved.getId());
        ingestionService.ingest(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public Ticket findById(Long id) {
        // Fetch with comments so the response maps correctly after the session closes (open-in-view=false).
        return ticketRepository.findWithCommentsById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Ticket> search(String keyword, TicketStatus status, Pageable pageable) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return ticketRepository.search(normalizedKeyword, status, pageable);
    }

    public Ticket update(Long id, UpdateTicketRequest request) {
        Ticket ticket = findById(id);
        if (request.title() != null) {
            ticket.setTitle(request.title());
        }
        if (request.description() != null) {
            ticket.setDescription(request.description());
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }
        if (request.assignee() != null) {
            ticket.setAssignee(request.assignee());
        }

        log.info("Updated ticket {}", ticket.getTicketRef());
        // Re-ingest so the knowledge base does not go stale (FR-19).
        ingestionService.ingest(ticket);
        return ticket;
    }

    public Comment addComment(Long id, CreateCommentRequest request) {
        Ticket ticket = findById(id);
        Comment comment = new Comment(request.author(), request.body());
        ticket.addComment(comment);

        log.info("Added comment to ticket {}", ticket.getTicketRef());
        ingestionService.ingest(ticket);
        // The comment now has an id because Ticket cascades the persist within this transaction.
        return comment;
    }

    public Ticket transition(Long id, TransitionRequest request) {
        Ticket ticket = findById(id);
        TicketStatus current = ticket.getStatus();
        TicketStatus target = request.targetStatus();

        if (!current.canTransitionTo(target)) {
            throw new InvalidTransitionException(current, target);
        }

        ticket.setStatus(target);
        if (request.resolutionNotes() != null
                && (target == TicketStatus.RESOLVED || target == TicketStatus.CLOSED)) {
            ticket.setResolutionNotes(request.resolutionNotes());
        }

        log.info("Transitioned ticket {} from {} to {}", ticket.getTicketRef(), current, target);
        // Status/resolution changed — refresh embeddings (FR-19).
        ingestionService.ingest(ticket);
        return ticket;
    }
}

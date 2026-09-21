package com.support.ticketai.controller;

import com.support.ticketai.domain.TicketStatus;
import com.support.ticketai.dto.CommentResponse;
import com.support.ticketai.dto.CreateCommentRequest;
import com.support.ticketai.dto.CreateTicketRequest;
import com.support.ticketai.dto.PagedResponse;
import com.support.ticketai.dto.TicketResponse;
import com.support.ticketai.dto.TicketSummary;
import com.support.ticketai.dto.TransitionRequest;
import com.support.ticketai.dto.UpdateTicketRequest;
import com.support.ticketai.mapper.TicketMapper;
import com.support.ticketai.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Ticket CRUD, comments, search, filter, and state transitions")
public class TicketController {

    private final TicketService ticketService;
    private final TicketMapper ticketMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a ticket")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket created"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest request) {
        return ticketMapper.toResponse(ticketService.create(request));
    }

    @GetMapping
    @Operation(summary = "List tickets (paged, optional keyword search and status filter)")
    public PagedResponse<TicketSummary> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TicketSummary> summaries = ticketService.search(q, status, pageable)
                .map(ticketMapper::toSummary);
        return PagedResponse.from(summaries);
    }

    @GetMapping("/{id}")
    @Operation(summary = "View ticket details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket found"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public TicketResponse get(@PathVariable Long id) {
        return ticketMapper.toResponse(ticketService.findById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update ticket fields (title, description, priority, assignee)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTicketRequest request) {
        return ticketMapper.toResponse(ticketService.update(id, request));
    }

    @PostMapping("/{id}/transitions")
    @Operation(summary = "Change ticket status (enforces the state machine)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transition applied"),
            @ApiResponse(responseCode = "404", description = "Ticket not found"),
            @ApiResponse(responseCode = "409", description = "Invalid state transition")
    })
    public TicketResponse transition(@PathVariable Long id, @Valid @RequestBody TransitionRequest request) {
        return ticketMapper.toResponse(ticketService.transition(id, request));
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a comment to a ticket")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comment added"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public CommentResponse addComment(@PathVariable Long id, @Valid @RequestBody CreateCommentRequest request) {
        return ticketMapper.toCommentResponse(ticketService.addComment(id, request));
    }
}

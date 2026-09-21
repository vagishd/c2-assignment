package com.support.ticketai.rag;

import com.support.ticketai.domain.Ticket;

/**
 * Keeps the vector store in sync with ticket data (see rag-ingestion.md).
 * TicketService depends on this abstraction so re-ingestion happens on every change
 * without coupling the ticket domain to the concrete RAG implementation.
 */
public interface TicketIngestionService {

    /** (Re)ingest a single ticket. Idempotent: replaces any existing documents for that ticket. */
    void ingest(Ticket ticket);

    /** Remove a ticket's documents from the vector store. */
    void remove(String ticketRef);
}

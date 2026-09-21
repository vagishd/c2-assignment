package com.support.ticketai.rag;

import com.support.ticketai.domain.Ticket;
import com.support.ticketai.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * On startup, (re)ingest every existing ticket so the in-memory vector store matches the database
 * (the store is not persisted; the H2 database is). See rag-ingestion.md.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class StartupIngestionRunner implements ApplicationRunner {

    private final TicketRepository ticketRepository;
    private final TicketIngestionService ingestionService;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        List<Ticket> tickets = ticketRepository.findAll();
        if (tickets.isEmpty()) {
            log.info("No tickets to ingest on startup");
            return;
        }
        tickets.forEach(ticket -> {
            // Touch lazy comments inside the transaction so the knowledge document is complete.
            ticket.getComments().size();
            ingestionService.ingest(ticket);
        });
        log.info("Startup ingestion complete: {} ticket(s) embedded", tickets.size());
    }
}

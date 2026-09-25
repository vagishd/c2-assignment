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
        int embedded = 0;
        for (Ticket ticket : tickets) {
            // Touch lazy comments inside the transaction so the knowledge document is complete.
            ticket.getComments().size();
            try {
                ingestionService.ingest(ticket);
                embedded++;
            } catch (Exception ex) {
                // The embedding model may be offline (e.g. Ollama not running). CRUD and the state
                // machine must still work, so we log and continue rather than failing startup.
                log.warn("Skipped startup ingestion for {} — embedding model unavailable ({})",
                        ticket.getTicketRef(), ex.getMessage());
            }
        }
        if (embedded == tickets.size()) {
            log.info("Startup ingestion complete: {} ticket(s) embedded", embedded);
        } else {
            log.warn("Startup ingestion partial: {}/{} ticket(s) embedded. The assistant will have "
                    + "limited context until the embedding model is available and tickets are re-ingested.",
                    embedded, tickets.size());
        }
    }
}

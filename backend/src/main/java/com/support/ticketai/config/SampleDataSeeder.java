package com.support.ticketai.config;

import com.support.ticketai.domain.Comment;
import com.support.ticketai.domain.Priority;
import com.support.ticketai.domain.Ticket;
import com.support.ticketai.domain.TicketStatus;
import com.support.ticketai.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a small set of realistic tickets on first run so the RAG assistant has data to retrieve.
 * Runs before startup ingestion (@Order). Idempotent: does nothing if tickets already exist, so
 * data survives restart without being duplicated.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SampleDataSeeder implements CommandLineRunner {

    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (ticketRepository.count() > 0) {
            log.info("Sample data already present; skipping seed");
            return;
        }

        seed("Payment failed at checkout",
                "Customers report repeated card declines on the final payment step during checkout.",
                Priority.HIGH, "alice", "PAYMENT", TicketStatus.RESOLVED,
                "Expired payment-gateway TLS certificate caused declines. Renewed the certificate and payments recovered.",
                new String[]{"alice:Reproduced on staging with a test card.",
                        "bob:Gateway logs show TLS handshake failures around the incident window."});

        seed("Duplicate charge on subscription renewal",
                "A customer was charged twice for a monthly subscription renewal.",
                Priority.URGENT, "carol", "PAYMENT", TicketStatus.RESOLVED,
                "A retry after a timeout created a second charge. Added an idempotency key on renewals and refunded the duplicate.",
                new String[]{"carol:Confirmed two identical charges 3 seconds apart."});

        seed("Shipment tracking number not updating",
                "The tracking status stays on 'label created' and never progresses for some orders.",
                Priority.MEDIUM, "dave", "SHIPPING", TicketStatus.RESOLVED,
                "Carrier webhook events were being dropped due to a misconfigured endpoint. Fixed the endpoint and backfilled events.",
                new String[]{"dave:Multiple orders stuck on the same status.",
                        "erin:Carrier dashboard shows the parcels actually moved."});

        seed("Tracking page shows wrong delivery date",
                "The estimated delivery date on the tracking page is off by several days.",
                Priority.LOW, "erin", "SHIPPING", TicketStatus.IN_PROGRESS,
                null,
                new String[]{"erin:Looks like a timezone conversion issue in the ETA calculation."});

        seed("Login fails with valid credentials",
                "Users intermittently cannot log in even with the correct password.",
                Priority.HIGH, "frank", "AUTH", TicketStatus.OPEN,
                null,
                new String[]{"frank:Happens more during peak hours; possibly session store pressure."});

        log.info("Seeded {} sample tickets", ticketRepository.count());
    }

    private void seed(String title, String description, Priority priority, String assignee,
                      String category, TicketStatus status, String resolutionNotes, String[] comments) {
        Ticket ticket = new Ticket();
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setPriority(priority);
        ticket.setAssignee(assignee);
        ticket.setCategory(category);
        ticket.setStatus(status);
        ticket.setResolutionNotes(resolutionNotes);

        for (String comment : comments) {
            String[] parts = comment.split(":", 2);
            ticket.addComment(new Comment(parts[0], parts[1]));
        }
        // ticketRef is assigned in Ticket#assignTicketRef (@PrePersist).
        ticketRepository.save(ticket);
    }
}

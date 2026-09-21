package com.support.ticketai.repository;

import com.support.ticketai.domain.Ticket;
import com.support.ticketai.domain.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketRef(String ticketRef);

    /** Fetches a ticket with its comments so the response can be mapped outside the session. */
    @EntityGraph(attributePaths = "comments")
    Optional<Ticket> findWithCommentsById(Long id);

    /**
     * Keyword search over title, description, and comment bodies, optionally filtered by status.
     * A null {@code status} means "any status"; a null/blank {@code keyword} means "no keyword filter".
     * DISTINCT because the comment join can multiply rows.
     */
    @Query("""
            SELECT DISTINCT t FROM Ticket t
            LEFT JOIN t.comments c
            WHERE (:status IS NULL OR t.status = :status)
              AND (:keyword IS NULL
                   OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(c.body) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Ticket> search(@Param("keyword") String keyword,
                        @Param("status") TicketStatus status,
                        Pageable pageable);
}

package com.support.ticketai.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_tickets_status", columnList = "status"),
        @Index(name = "idx_tickets_category", columnList = "category"),
        @Index(name = "uq_tickets_ref", columnList = "ticket_ref", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "comments")
@EqualsAndHashCode(of = "id")
public class Ticket {

    // Human-facing reference number, drawn from a dedicated sequence so it is independent of the id
    // and available before the first insert (avoids a NOT NULL violation on ticket_ref).
    private static final long TICKET_REF_BASE = 1000L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ticket_seq")
    @SequenceGenerator(name = "ticket_seq", sequenceName = "ticket_seq", allocationSize = 1)
    private Long id;

    @Column(name = "ticket_ref", nullable = false, unique = true, updatable = false)
    private String ticketRef;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    @Column(length = 100)
    private String assignee;

    @Column(length = 60)
    private String category;

    @Column(name = "resolution_notes", length = 5000)
    private String resolutionNotes;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<Comment> comments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void addComment(Comment comment) {
        comment.setTicket(this);
        this.comments.add(comment);
    }

    /**
     * Assigns the human-facing reference before insert. With sequence-based id generation the id is
     * already populated here, so ticket_ref is never null at insert time.
     */
    @PrePersist
    void assignTicketRef() {
        if (this.ticketRef == null && this.id != null) {
            this.ticketRef = "TKT-" + (TICKET_REF_BASE + this.id);
        }
    }
}

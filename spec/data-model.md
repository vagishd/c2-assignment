# Data Model

## Entities

### Ticket
| Field | Type | Notes |
|---|---|---|
| id | Long | PK, identity |
| ticketRef | String | Human reference, e.g. `TKT-1001`; unique; generated |
| title | String | required, 1..150 |
| description | String (TEXT) | required, 1..5000 |
| status | TicketStatus (enum, STRING) | default OPEN |
| priority | Priority (enum, STRING) | required |
| assignee | String | optional, <=100 |
| category | String | optional, <=60 (e.g. PAYMENT, SHIPPING) |
| resolutionNotes | String (TEXT) | optional; set when RESOLVED/CLOSED |
| comments | List<Comment> | one-to-many, cascade, lazy |
| createdAt | Instant | @CreationTimestamp |
| updatedAt | Instant | @UpdateTimestamp |

Indexes: `status`, `category`, unique on `ticketRef`.

### Comment
| Field | Type | Notes |
|---|---|---|
| id | Long | PK, identity |
| ticket | Ticket | many-to-one, FK, lazy |
| author | String | required, <=100 |
| body | String (TEXT) | required, 1..3000 |
| createdAt | Instant | @CreationTimestamp |

## Enums

### TicketStatus
`OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED`

### Priority
`LOW, MEDIUM, HIGH, URGENT`

## Relationships
- Ticket 1 ── * Comment (mappedBy `ticket`, cascade ALL, orphanRemoval true).

## Persistence notes
- Enums stored as STRING (never ORDINAL) so reordering is safe (per java-standard).
- H2 file mode; schema via JPA `ddl-auto: update` for the exercise, seeded with sample tickets on
  first run so the RAG feature has data to retrieve.

## ticketRef generation
- `TKT-` + a sequence starting at 1001. Stable, human-usable, and used as the citation id the
  assistant returns (e.g. "TKT-1001").

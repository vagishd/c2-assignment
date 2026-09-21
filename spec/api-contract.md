# API Contract

Base path: `/api`. All timestamps ISO-8601 UTC. Error shape is consistent (see bottom).

## Tickets

### Create — `POST /api/tickets` → 201
Request:
```json
{
  "title": "Payment failed at checkout",
  "description": "Card declined repeatedly on the payment step.",
  "priority": "HIGH",
  "assignee": "alice",
  "category": "PAYMENT"
}
```
Response 201: full `TicketResponse` (see below). Validation errors → 400.

### List — `GET /api/tickets` → 200
Query params:
- `q` (optional) keyword search over title/description/comments
- `status` (optional) one of the TicketStatus values
- `page` (default 0), `size` (default 20)

Response 200:
```json
{
  "content": [ { "...TicketSummary..." } ],
  "page": 0, "size": 20, "totalElements": 42, "totalPages": 3
}
```
`TicketSummary`: id, ticketRef, title, status, priority, assignee, category, createdAt, updatedAt.

### View — `GET /api/tickets/{id}` → 200
Response: full `TicketResponse`. Unknown id → 404 `TICKET_NOT_FOUND`.

### Update fields — `PATCH /api/tickets/{id}` → 200
Request (all fields optional; only provided fields change):
```json
{ "title": "...", "description": "...", "priority": "MEDIUM", "assignee": "bob" }
```
Note: status is NOT changed here — use the transitions endpoint. Validation errors → 400.

### Change status — `POST /api/tickets/{id}/transitions` → 200
Request:
```json
{ "targetStatus": "IN_PROGRESS", "resolutionNotes": "optional, used for RESOLVED/CLOSED" }
```
- Valid transition → 200 with updated `TicketResponse`.
- Invalid transition → 409 `INVALID_TRANSITION`, state unchanged.
- Unknown status value → 400.

### Add comment — `POST /api/tickets/{id}/comments` → 201
Request:
```json
{ "author": "alice", "body": "Reproduced on staging." }
```
Response 201: `CommentResponse` { id, author, body, createdAt }. Ticket not found → 404.

## TicketResponse (full)
```json
{
  "id": 1,
  "ticketRef": "TKT-1001",
  "title": "Payment failed at checkout",
  "description": "...",
  "status": "OPEN",
  "priority": "HIGH",
  "assignee": "alice",
  "category": "PAYMENT",
  "resolutionNotes": null,
  "comments": [ { "id": 5, "author": "alice", "body": "...", "createdAt": "..." } ],
  "createdAt": "2026-09-20T10:00:00Z",
  "updatedAt": "2026-09-20T10:05:00Z"
}
```

## AI assistant
See rag-api-contract.md for `POST /api/ai/ask`.

## Error response (all errors)
```json
{
  "code": "INVALID_TRANSITION",
  "message": "Cannot transition ticket from CLOSED to OPEN",
  "timestamp": "2026-09-20T10:15:30Z"
}
```
Codes: `VALIDATION_FAILED` (400), `TICKET_NOT_FOUND` (404), `INVALID_TRANSITION` (409),
`INTERNAL_ERROR` (500).

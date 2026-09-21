# Requirements — AI-Powered Support Ticket Management System

## 1. Purpose

A support ticket management system with an AI-native question-answering feature over ticket
history. The AI feature is designed in from the start, not bolted on. The assistant answers
questions grounded strictly in real ticket data, cites the tickets it used, and honestly says
when nothing relevant exists.

## 2. Scope

In scope:
- Full ticket lifecycle CRUD, comments, search, filter.
- Enforced ticket state machine.
- RAG pipeline over ticket content with a single retrieve-then-generate flow.
- REST API + React frontend.

Out of scope:
- Autonomous/agentic behavior. The assistant answers one question with one grounded response.
  It does not create tickets, send notifications, or chain into other tools.
- Authentication/authorization (not required by the exercise; noted as a future concern).

## 3. Technology

- Java 21, Spring Boot 3.x, Spring AI 1.0.x (GA).
- H2 (file mode) for persistence so data survives restart.
- In-memory vector store (Spring AI `SimpleVectorStore`).
- Embedding + chat model via Ollama (local, default) or OpenAI (cloud, optional) — see architecture.md.
- React (Vite) frontend.

## 4. Functional requirements

### 4.1 Ticket management
- FR-1 Create a ticket (title, description, priority; optional assignee, category).
- FR-2 List tickets (paged).
- FR-3 View ticket details including comments and history.
- FR-4 Update title, description, priority, assignee.
- FR-5 Add comments to a ticket.
- FR-6 Search tickets by keyword (title/description/comments).
- FR-7 Filter tickets by status.
- FR-8 Persist all data in a database that survives restart.
- FR-9 Validate input at the backend; reject invalid input with meaningful errors.
- FR-10 UI displays meaningful errors returned by the backend.

### 4.2 State machine (enforced by backend)
- FR-11 Allowed transitions only:
  - OPEN → IN_PROGRESS → RESOLVED → CLOSED
  - OPEN → CANCELLED
  - IN_PROGRESS → CANCELLED
- FR-12 Any other transition is rejected (e.g. CLOSED→OPEN, RESOLVED→OPEN, CANCELLED→OPEN).
- FR-13 Invalid transitions return a 409 with a clear error; state is unchanged.

### 4.3 AI assistant (RAG)
- FR-14 `POST /api/ai/ask` accepts a natural-language question and returns a grounded answer.
- FR-15 The answer is grounded strictly in retrieved ticket data — no general-knowledge fallback
  for support-specific questions.
- FR-16 The response cites the specific ticket ID(s) used to produce it.
- FR-17 When no relevant tickets are found, the response explicitly says so and does not fabricate.
- FR-18 Ticket content is converted into knowledge documents, chunked, embedded, and stored in the
  vector store, with metadata: ticketId, status, priority, assignee, category.
- FR-19 Re-ingest / refresh embeddings when a ticket is created, updated, or its status changes —
  the knowledge base must not go stale.
- FR-20 Retrieval parameters (top-K, similarity threshold) are configurable, not hardcoded.

## 5. Non-functional requirements
- NFR-1 No secrets committed; model keys via environment variables.
- NFR-2 App runs locally with a single backend start + single frontend start.
- NFR-3 Chunking strategy and embedding-model choice documented and justified in architecture.md.
- NFR-4 State-machine integration tests pass.
- NFR-5 At least one real AI mistake (code or RAG answer) caught and documented during development
  (see evaluation-strategy.md, "AI mistakes log").

## 6. Example assistant questions (in scope)
- "Have we seen payment failures before?"
- "What was the resolution for ticket TKT-1001?"
- "What are the common causes of shipment tracking issues?"
- "Show me similar resolved tickets."
- "Which high-priority tickets are related to payment?"

## 7. Acceptance criteria

The solution is complete when:
- [ ] Ticket can be created from the UI.
- [ ] Tickets can be listed.
- [ ] Ticket details can be viewed.
- [ ] Ticket fields can be updated.
- [ ] Assignee can be changed.
- [ ] Comments can be added.
- [ ] Search works.
- [ ] Status filter works.
- [ ] Valid status transitions work.
- [ ] Invalid status transitions are rejected by the backend.
- [ ] Data survives application restart.
- [ ] Backend validation works.
- [ ] UI shows meaningful errors.
- [ ] State-machine integration tests pass.
- [ ] Ticket data is converted into embeddings and stored in a vector store.
- [ ] `POST /api/ai/ask` returns a grounded, ticket-sourced answer for in-scope questions.
- [ ] The response cites the specific ticket ID(s) used to generate it.
- [ ] Out-of-scope / no-match questions return an honest "no relevant tickets found" response.
- [ ] Chunking strategy and embedding-model choice are documented and justified in architecture.md.
- [ ] Re-ingestion happens when a ticket is updated.
- [ ] Retrieval parameters (top-K, similarity threshold) are configurable.
- [ ] No secrets are committed.
- [ ] At least one meaningful AI mistake was caught and documented.

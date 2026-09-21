# Architecture

## 1. Overview

```
┌─────────────┐        REST/JSON         ┌──────────────────────────────────────┐
│  React (Vite)│  ───────────────────▶   │  Spring Boot 3 (Java 21)              │
│  frontend    │  ◀───────────────────   │                                        │
└─────────────┘                          │  Controller → Service → Repository     │
                                         │        │            │                  │
                                         │        │            ▼                  │
                                         │        │        H2 (file mode)          │
                                         │        │                                │
                                         │        ▼                                │
                                         │   RAG layer (Spring AI)                 │
                                         │   ├─ IngestionService                   │
                                         │   ├─ SimpleVectorStore (in-memory)      │
                                         │   ├─ EmbeddingModel (Ollama/OpenAI)     │
                                         │   └─ ChatClient (Ollama/OpenAI)         │
                                         └──────────────────────────────────────┘
```

- Spring AI is the backbone of the AI feature: `VectorStore`, `EmbeddingModel`, `ChatClient`,
  and the `Document`/`TextSplitter` abstractions are all used directly.

## 2. Backend layering (per java-standard steering)

```
controller/   REST endpoints only
service/      business logic: TicketService, TransitionService, and RAG services
repository/   Spring Data JPA
domain/       JPA entities + enums + state machine
dto/          request/response objects
mapper/       entity ↔ DTO
exception/    business exceptions + GlobalExceptionHandler
config/       Spring AI beans, vector store, properties
```

## 3. Persistence

- H2 in **file mode** (`jdbc:h2:file:./data/ticketsdb`) so data survives restart (FR-8, NFR-2).
- Entities: `Ticket`, `Comment` (see data-model.md).

## 4. AI / RAG design

### 4.1 Flow
`Ticket → KnowledgeDocument → chunk → embed → SimpleVectorStore` for ingestion, and
`question → embed → similarity search (top-K, threshold) → context → ChatClient → grounded answer + sources`
for querying. Full detail in rag-ingestion.md and rag-api-contract.md.

### 4.2 Chunking strategy — decision & justification

**Decision: one knowledge document per ticket, with token-bounded splitting using Spring AI's
`TokenTextSplitter` only when a ticket exceeds the size budget.**

Reasoning:
- Ticket content is naturally bounded and already structured (title, description, comments,
  resolution). A single ticket is usually small enough to be one coherent chunk, which keeps a
  retrieved chunk fully self-describing and makes citation exact (one chunk ↔ one ticketId).
- Fixed-size character splitting was rejected as the default because it can cut a description or
  resolution mid-sentence and split a single ticket across chunks that then cite the same ticket
  redundantly.
- Pure semantic splitting was rejected as overkill for short ticket text — the extra cost buys
  little when tickets are already small, discrete units.
- For the rare long ticket (many comments), `TokenTextSplitter` bounds chunk size while keeping
  each chunk tagged with the same ticket metadata, so citation still resolves to the ticket.

Documented alternatives considered: paragraph-based, fixed-size-with-overlap, semantic splitting.
Chosen: per-ticket document + token-bounded split for oversized tickets. Restated in rag-ingestion.md.

### 4.3 Embedding model choice — decision & tradeoff

**Decision: default to a local embedding model via Ollama (`nomic-embed-text`), with OpenAI
(`text-embedding-3-small`) as a configurable alternative.**

| Factor | Local (Ollama, default) | Cloud (OpenAI, optional) |
|---|---|---|
| Cost | No per-call cost | Per-token cost |
| Latency | Depends on local hardware; no network hop | Fast API, but network round-trip |
| Quality | Good for short support text | Generally higher on nuanced text |
| Data boundary | Ticket data never leaves the machine | Ticket data sent to a third party |
| Ops | Must run Ollama locally | Needs an API key/secret |

Reasoning: support tickets can contain customer/PII-adjacent content, and the exercise must run
without committing secrets. A local default keeps data on-device, has zero marginal cost, and needs
no key. OpenAI is left as a one-property switch for teams that want higher quality and accept the
cost/data tradeoff. The embedding model and chat model are both selected via `application.yml`
profiles; nothing is hardcoded.

### 4.4 Retrieval defaults (configurable — FR-20)
- `rag.top-k` (default 4)
- `rag.similarity-threshold` (default 0.5)
- Both are bound via `@ConfigurationProperties` and used by the retrieval service; never hardcoded.

### 4.5 Grounding & guardrails
- The system prompt constrains the model to answer only from the provided ticket context and to
  return an explicit no-match message when the context is empty.
- The static prompt (instructions + guardrails) is a constant so it is stable/cacheable across
  requests; only the dynamic context + question change.
- If similarity search returns nothing above the threshold, the service short-circuits and returns
  the honest no-match response **without calling the LLM** — this is the strongest anti-hallucination
  guarantee.

## 5. Configuration & secrets
- Model provider, model names, base URLs, top-K, and threshold live in `application.yml`.
- OpenAI key (if used) comes from `OPENAI_API_KEY` env var. No secrets committed (NFR-1).

## 6. Frontend
- React + Vite SPA. Screens: ticket list (with search + status filter), ticket detail (fields,
  comments, transitions), create ticket, and an Ask panel for the assistant. See ui-flow.md.
- Errors from the backend's consistent error shape are surfaced to the user (FR-10).

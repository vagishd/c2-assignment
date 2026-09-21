# RAG Ingestion

## Goal
Turn ticket data into searchable knowledge in the vector store, and keep it fresh (FR-18, FR-19).

## Flow
```
Ticket (description, comments, resolution notes)
        │
        ▼
Build KnowledgeDocument text
        │
        ▼
Chunk (per-ticket; token-bounded split only if oversized)
        │
        ▼
Generate embeddings (EmbeddingModel: Ollama default / OpenAI optional)
        │
        ▼
Store in SimpleVectorStore with metadata
```

## Knowledge document content
For each ticket, assemble a single text block:
```
[TKT-1001] Payment failed at checkout
Status: RESOLVED | Priority: HIGH | Assignee: alice | Category: PAYMENT

Description:
<description>

Comments:
- alice: <comment body>
- bob: <comment body>

Resolution:
<resolutionNotes>
```
The `[TKT-xxxx]` prefix and the metadata header make each chunk self-describing and citable.

## Metadata (attached to every Document)
- `ticketId` (the ticketRef, e.g. `TKT-1001`)
- `status`
- `priority`
- `assignee`
- `category`

Metadata enables citation (return `ticketId`) and future filtered retrieval (e.g. only HIGH
priority, only PAYMENT).

## Chunking strategy (see architecture.md §4.2)
- Default: one `Document` per ticket — tickets are small, discrete units; one chunk ↔ one ticket
  keeps citation exact.
- Oversized tickets (many long comments) are split with Spring AI `TokenTextSplitter`; every
  resulting chunk keeps the same ticket metadata so citation still resolves to the ticket.
- Rejected alternatives: fixed-size char splitting (cuts sentences, splits one ticket redundantly),
  pure semantic splitting (unnecessary cost for short text).

## Freshness / re-ingestion (FR-19)
Re-ingest the affected ticket on:
- create
- field update (title/description/priority/assignee)
- comment added
- status transition (incl. resolution notes)

Re-ingestion is idempotent: existing documents for that `ticketId` are removed from the store, then
the freshly built document(s) are added. This prevents duplicates and stale content.

On application startup, all existing tickets are (re)ingested so the store matches the database.

## Configuration
- Embedding model + provider selected via `application.yml` (see architecture.md §4.3). Never hardcoded.

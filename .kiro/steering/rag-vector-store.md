---
name: rag-vector-store
description: >
  Generic RAG and vector-store guidelines for any project that does retrieval-augmented
  generation. Apply when designing or changing ingestion, chunking, embeddings, vector
  storage, retrieval, or grounded generation. Triggers on "RAG", "embeddings", "vector store",
  "chunking", "similarity search", "retrieval", "ground the answer".
alwaysApply: false
---

# RAG / Vector Store Guidelines

These are defaults and conventions. Every project restates its concrete choices (and the
reasoning) in its own architecture spec — this file defines how to reason about them.

## Pipeline shape

```
Source data → Knowledge document → Chunk → Embed → Vector store
                                                        │
User question → Embed → Similarity search (top-K, threshold) → Context → LLM → Grounded answer + sources
```

Keep it a single retrieve-then-generate flow unless the project explicitly needs agentic behavior.

## Chunking conventions

- Match chunk boundaries to the natural structure of the source (records, sections, paragraphs) rather than blindly splitting by character count.
- Prefer structure- or semantics-aware splitting for prose; use bounded fixed-size chunks with overlap only when structure is absent.
- Keep chunks small enough to be specific but large enough to stand alone as context. Include a modest overlap so meaning is not cut mid-thought.
- Attach metadata to every chunk (source id, type, status, and any fields needed to filter or cite). Metadata is what makes citation and filtered retrieval possible.
- Document the chosen strategy and why alternatives were rejected in the architecture spec.

## Embedding model choice

Decide deliberately and record the tradeoff:

| Option | Typical strengths | Typical costs |
|---|---|---|
| Local (self-hosted) | No per-call cost, data stays local, predictable | Ops burden, hardware limits, may lag on quality |
| Cloud API | Strong quality, no infra to run | Per-call cost, network latency, data leaves the boundary |

Pick based on data sensitivity, budget, latency targets, and quality needs. State the decision and its justification in the architecture spec. Keep the embedding model configurable.

## Retrieval tuning defaults

- Top-K and the similarity threshold MUST be configurable, never hardcoded.
- Start conservative (a small top-K, a threshold that filters weak matches) and tune against an evaluation set.
- Apply the threshold so that genuinely irrelevant matches are dropped — this is what enables honest "no relevant results" behavior.
- Prefer metadata filtering + vector search together when the query implies a constraint.

## Freshness

- Re-embed when source content changes. Do not let the vector store drift out of sync with the system of record.
- Make ingestion idempotent so re-running it does not create duplicates.

## Grounding & guardrails

- The model answers only from retrieved context, never from general knowledge, for domain-specific questions.
- Always return the sources used to produce an answer.
- When retrieval yields nothing above the threshold, return an explicit "no relevant information found" response instead of fabricating a plausible answer.
- Keep the static parts of the system prompt (instructions, guardrails) stable so they can be cached and reused across requests.

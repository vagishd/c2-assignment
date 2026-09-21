# RAG API Contract — Assistant

## Endpoint
`POST /api/ai/ask` → 200

### Request
```json
{ "question": "What caused previous payment failures?" }
```
- `question`: required, non-blank, <= 1000 chars. Blank → 400 `VALIDATION_FAILED`.

### Response — answer found
```json
{
  "answer": "Payment failures were caused by an expired payment-gateway certificate; see the resolution on TKT-1001.",
  "sources": [
    { "ticketId": "TKT-1001", "title": "Payment failed at checkout", "status": "RESOLVED" }
  ],
  "grounded": true
}
```

### Response — no relevant tickets (honest abstention)
```json
{
  "answer": "No relevant tickets were found to answer this question.",
  "sources": [],
  "grounded": false
}
```

## Behavior & guarantees
1. Embed the question, run similarity search with configurable `top-K` and `similarity-threshold`.
2. If no chunk scores above the threshold → return the no-match response immediately. The LLM is
   NOT called. `grounded=false`, `sources=[]`. (FR-17)
3. If context is found → call the ChatClient with a system prompt that constrains it to answer only
   from the provided ticket context and to cite ticket IDs. (FR-15, FR-16)
4. `sources` always lists the ticket(s) whose chunks were used as context — this is the citation. (FR-16)
5. The assistant never uses general knowledge for support-specific questions. (FR-15)

## Grounding prompt (static, cacheable)
The system prompt is a constant (only the context + question vary), so it is stable across requests:
- Answer only using the provided ticket context.
- Cite the ticket IDs used.
- If the context does not contain the answer, say no relevant tickets were found — do not guess.

## Configuration (FR-20)
```yaml
rag:
  top-k: 4
  similarity-threshold: 0.5
```
Both bound via @ConfigurationProperties; changeable without code edits.

## Scope
Single retrieve→generate call. No tool-calling, no follow-up actions, no ticket mutations from the
assistant. (Out of scope per requirements.)

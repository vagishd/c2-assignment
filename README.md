# AI-Powered Support Ticket Management System

A support ticket system with an AI-native, grounded question-answering feature over ticket history.
Built with Java 21, Spring Boot 3, Spring AI, H2, an in-memory vector store, and a React frontend.

The design was done spec-first (see `spec/`) with reusable AI-assistant guidelines in `.kiro/steering/`.

## What it does

- Create, list, view, update tickets; change assignee; add comments.
- Search by keyword; filter by status.
- Enforced ticket state machine (`OPEN → IN_PROGRESS → RESOLVED → CLOSED`, plus `→ CANCELLED`).
- `POST /api/ai/ask`: a natural-language question answered strictly from real ticket data, citing the
  ticket IDs used, and returning an honest "no relevant tickets found" when nothing matches.

## Layout

```
.kiro/steering/   Reusable, generic AI-assistant guidelines (Java, testing, API, RAG, review commands)
spec/             Specifications (requirements, architecture, data model, API, state machine, RAG, tests)
backend/          Spring Boot 3 + Spring AI (Gradle)
frontend/         React + Vite
```

## Prerequisites

- JDK 21
- Node 18+
- (For live AI answers) [Ollama](https://ollama.com) running locally with the models pulled:
  ```
  ollama pull llama3.1
  ollama pull nomic-embed-text
  ```
  The ticket CRUD and state machine work without Ollama; only `/api/ai/ask` needs a model.

## Run the backend

```
cd backend
./gradlew bootRun
```
- API at http://localhost:8080, Swagger UI at http://localhost:8080/swagger-ui.html
- Data persists in `backend/data/` (H2 file mode) and survives restart.
- Sample tickets are seeded on first run so the assistant has data to retrieve.

### Using OpenAI instead of Ollama

```
export OPENAI_API_KEY=sk-...
cd backend
./gradlew bootRun --args='--spring.profiles.active=openai'
```
No secrets are committed; the key is read from the environment.

## Run the frontend

```
cd frontend
npm install
npm run dev
```
Open http://localhost:5173 (the dev server proxies `/api` to the backend on port 8080).

## Test

```
cd backend
./gradlew test
```
Includes the exhaustive state-machine unit test, the state-machine integration tests, and the RAG
grounding tests (which mock the model — no live calls).

## Configuration

Retrieval is tunable in `backend/src/main/resources/application.yml` (not hardcoded):

```yaml
rag:
  top-k: 4
  similarity-threshold: 0.5
  provider: ollama   # or openai
```

## AI design notes

- Chunking, embedding-model choice, and retrieval defaults are justified in `spec/architecture.md`.
- Grounding guardrails and the no-match short-circuit are in `spec/rag-api-contract.md`.
- Real AI mistakes caught during development are logged in `spec/evaluation-strategy.md`.
```

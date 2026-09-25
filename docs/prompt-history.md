# Prompt History

## 2026-09-23 09:02:11 UTC

> Before we build anything real, I want a solid set of reusable AI guidelines. In Kiro, explain the
> difference between a steering file, a command, a skill, and a hook, and when I should use each.

## 2026-09-23 09:10:44 UTC

> Create four generic commands under .kiro/steering with inclusion: manual — review code, review
> spec, generate tests, and check an AI answer for hallucination. Keep them generic, no project
> specifics.

## 2026-09-23 09:24:03 UTC

> if a command is just a manual-inclusion markdown prompt, why would I ever make a skill instead?


## 2026-09-23 09:37:52 UTC

> Build a prompt-history skill: it should record every prompt I submit to a file. Repo must have docs/prompt-history.md. Explain the trigger you pick.

## 2026-09-23 09:41:18 UTC

> if that recorder script crashes, does it block my prompt from running?

## 2026-09-23 09:48:30 UTC

> Also confirm the hook activation timing and that none of these files get gitignored.

## 2026-09-23 11:05:20 UTC

> Now the real project. Build an AI-Powered Support Ticket Management System. Java 21, Spring Boot,
> Spring AI (this is the main thing — use it heavily), H2, an embedding model, a vector store, a REST
> API, and a React frontend. Follow my steering, keep it simple and readable, and design the AI
> feature in from the start, not bolted on. Do specs before code.

## 2026-09-23 11:14:07 UTC

> The functional list: create/list/view tickets, update title/description/priority/assignee, add
> comments, keyword search, filter by status, backend validation, meaningful UI errors. And enforce
> this state machine: OPEN → IN_PROGRESS → RESOLVED → CLOSED, OPEN → CANCELLED, IN_PROGRESS →
> CANCELLED. Reject everything else (CLOSED→OPEN, RESOLVED→OPEN, CANCELLED→OPEN).

## 2026-09-23 11:22:41 UTC

> For the AI part: a POST /api/ai/ask endpoint. It answers questions grounded strictly in real ticket
> data, cites the specific ticket ID(s) it used, and if nothing relevant is found it must say so, not
> make something up. Ingest ticket description + comments + resolution, with metadata (ticketId,
> status, priority, assignee, category), and re-ingest when a ticket changes so it never goes stale.
> Top-K and similarity threshold must be configurable.

## 2026-09-23 11:29:03 UTC

> how are you chunking ticket text, and are you using a local or cloud embedding
> model? I want the reasoning written down, not just the choice.

## 2026-09-23 11:35:50 UTC

> Whatever steering you create must be generic — reusable on every project, no ticket-specific
> examples. Project-specific detail goes in spec/, not steering.

## 2026-09-23 11:38:12 UTC

> Use Gradle for the backend, not Maven.

## 2026-09-23 13:02:44 UTC

> Are all the core acceptance criteria actually implemented? Go through them, don't guess.

## 2026-09-23 13:10:19 UTC

> The state-machine integration tests were failing on context load — what happened and how did you
> fix it?

## 2026-09-23 13:18:55 UTC

> prove the assistant can't hallucinate on an out-of-scope question. Show me the
> guard, not just a claim.

## 2026-09-23 13:31:47 UTC

> Good. Add a README with run instructions and we're done for now.


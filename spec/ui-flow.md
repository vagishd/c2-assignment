# UI Flow

React (Vite) single-page app. Talks to the backend REST API. Surfaces the backend's consistent
error shape to the user (FR-10).

## Screens

### 1. Ticket list (home)
- Table/list of tickets: ticketRef, title, status badge, priority, assignee.
- Search box → `GET /api/tickets?q=...`.
- Status filter dropdown → `GET /api/tickets?status=...`.
- Pagination controls.
- "New ticket" button → create form.
- Row click → ticket detail.

### 2. Create ticket
- Form: title, description, priority (select), assignee (optional), category (optional).
- Submit → `POST /api/tickets`.
- Validation errors from backend (400) shown inline / as a message.

### 3. Ticket detail
- Shows all fields, status, resolution notes, timestamps.
- Comments list + add-comment form → `POST /api/tickets/{id}/comments`.
- Edit fields (title/description/priority/assignee) → `PATCH /api/tickets/{id}`.
- Status transition control: only offers valid next states; submit → `POST /api/tickets/{id}/transitions`.
  Invalid transition (409) shows the backend message.

### 4. Ask assistant
- A panel/page with a question input → `POST /api/ai/ask`.
- Shows the grounded answer.
- Shows cited sources (ticketRef + title) as links to the ticket detail.
- When `grounded=false`, clearly shows the honest "no relevant tickets found" message and no sources.

## Error handling
- A shared API client maps non-2xx responses to the error shape `{ code, message }` and surfaces
  `message` to the user (toast/inline). Network failures show a generic retry message.

## User journeys
- Create → see it in list → open → move OPEN→IN_PROGRESS→RESOLVED→CLOSED.
- Try an invalid transition → see rejection message.
- Search "payment" → filter to relevant tickets.
- Ask "Have we seen payment failures before?" → grounded answer citing TKT-xxxx.
- Ask something unrelated → honest no-match response.

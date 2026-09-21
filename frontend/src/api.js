// Thin API client. Maps any non-2xx response to the backend's { code, message } error shape
// so the UI can surface a meaningful message (see ui-flow.md).

async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(path, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    });
  } catch {
    throw { code: 'NETWORK_ERROR', message: 'Could not reach the server. Is the backend running?' };
  }

  if (response.status === 204) {
    return null;
  }

  let body = null;
  const text = await response.text();
  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = null;
    }
  }

  if (!response.ok) {
    throw {
      code: body?.code || 'ERROR',
      message: body?.message || `Request failed (${response.status})`,
    };
  }
  return body;
}

export const api = {
  listTickets: ({ q, status, page = 0, size = 20 } = {}) => {
    const params = new URLSearchParams();
    if (q) params.set('q', q);
    if (status) params.set('status', status);
    params.set('page', page);
    params.set('size', size);
    return request(`/api/tickets?${params.toString()}`);
  },
  getTicket: (id) => request(`/api/tickets/${id}`),
  createTicket: (payload) => request('/api/tickets', { method: 'POST', body: JSON.stringify(payload) }),
  updateTicket: (id, payload) => request(`/api/tickets/${id}`, { method: 'PATCH', body: JSON.stringify(payload) }),
  transition: (id, payload) => request(`/api/tickets/${id}/transitions`, { method: 'POST', body: JSON.stringify(payload) }),
  addComment: (id, payload) => request(`/api/tickets/${id}/comments`, { method: 'POST', body: JSON.stringify(payload) }),
  ask: (question) => request('/api/ai/ask', { method: 'POST', body: JSON.stringify({ question }) }),
};

// Allowed next states per current status — mirrors the backend state machine so the UI only
// offers valid transitions (backend still enforces authoritatively).
export const ALLOWED_TRANSITIONS = {
  OPEN: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['RESOLVED', 'CANCELLED'],
  RESOLVED: ['CLOSED'],
  CLOSED: [],
  CANCELLED: [],
};

export const STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED'];
export const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api, ALLOWED_TRANSITIONS, PRIORITIES } from '../api.js';
import ErrorBanner from '../components/ErrorBanner.jsx';
import StatusBadge from '../components/StatusBadge.jsx';

export default function TicketDetail() {
  const { id } = useParams();
  const [ticket, setTicket] = useState(null);
  const [error, setError] = useState(null);

  // Edit fields
  const [edit, setEdit] = useState({ title: '', description: '', priority: 'MEDIUM', assignee: '' });
  // Transition
  const [targetStatus, setTargetStatus] = useState('');
  const [resolutionNotes, setResolutionNotes] = useState('');
  // Comment
  const [comment, setComment] = useState({ author: '', body: '' });

  async function load() {
    setError(null);
    try {
      const t = await api.getTicket(id);
      setTicket(t);
      setEdit({
        title: t.title, description: t.description, priority: t.priority, assignee: t.assignee || '',
      });
    } catch (err) {
      setError(err);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function saveFields(e) {
    e.preventDefault();
    setError(null);
    try {
      const updated = await api.updateTicket(id, {
        title: edit.title,
        description: edit.description,
        priority: edit.priority,
        assignee: edit.assignee || null,
      });
      setTicket(updated);
    } catch (err) {
      setError(err);
    }
  }

  async function applyTransition(e) {
    e.preventDefault();
    setError(null);
    try {
      const updated = await api.transition(id, { targetStatus, resolutionNotes: resolutionNotes || null });
      setTicket(updated);
      setTargetStatus('');
      setResolutionNotes('');
    } catch (err) {
      setError(err);
    }
  }

  async function addComment(e) {
    e.preventDefault();
    setError(null);
    try {
      await api.addComment(id, comment);
      setComment({ author: '', body: '' });
      await load();
    } catch (err) {
      setError(err);
    }
  }

  if (!ticket) {
    return (
      <div>
        <ErrorBanner error={error} />
        {!error && <p className="muted">Loading...</p>}
        <Link to="/">Back to tickets</Link>
      </div>
    );
  }

  const nextStates = ALLOWED_TRANSITIONS[ticket.status] || [];

  return (
    <div>
      <Link to="/">← Back to tickets</Link>
      <h2>{ticket.ticketRef} — {ticket.title} <StatusBadge status={ticket.status} /></h2>
      <ErrorBanner error={error} />

      <div className="card">
        <h3>Edit fields</h3>
        <form onSubmit={saveFields}>
          <label>Title</label>
          <input style={{ width: '100%' }} value={edit.title}
                 onChange={(e) => setEdit({ ...edit, title: e.target.value })} />
          <label>Description</label>
          <textarea value={edit.description}
                    onChange={(e) => setEdit({ ...edit, description: e.target.value })} />
          <div className="row">
            <div>
              <label>Priority</label>
              <select value={edit.priority} onChange={(e) => setEdit({ ...edit, priority: e.target.value })}>
                {PRIORITIES.map((p) => <option key={p} value={p}>{p}</option>)}
              </select>
            </div>
            <div className="grow">
              <label>Assignee</label>
              <input style={{ width: '100%' }} value={edit.assignee}
                     onChange={(e) => setEdit({ ...edit, assignee: e.target.value })} />
            </div>
          </div>
          <div style={{ marginTop: 12 }}>
            <button className="primary" type="submit">Save changes</button>
          </div>
        </form>
      </div>

      <div className="card">
        <h3>Change status</h3>
        {nextStates.length === 0 ? (
          <p className="muted">This ticket is in a terminal state ({ticket.status}); no further transitions.</p>
        ) : (
          <form className="row" onSubmit={applyTransition}>
            <select value={targetStatus} onChange={(e) => setTargetStatus(e.target.value)} required>
              <option value="">Select next status...</option>
              {nextStates.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
            <input className="grow" placeholder="Resolution notes (for RESOLVED/CLOSED)"
                   value={resolutionNotes} onChange={(e) => setResolutionNotes(e.target.value)} />
            <button className="primary" type="submit" disabled={!targetStatus}>Apply</button>
          </form>
        )}
        {ticket.resolutionNotes && (
          <p className="muted">Resolution: {ticket.resolutionNotes}</p>
        )}
      </div>

      <div className="card">
        <h3>Comments</h3>
        {ticket.comments.length === 0 ? (
          <p className="muted">No comments yet.</p>
        ) : (
          <ul>
            {ticket.comments.map((c) => (
              <li key={c.id}><strong>{c.author}</strong>: {c.body}</li>
            ))}
          </ul>
        )}
        <form onSubmit={addComment} style={{ marginTop: 12 }}>
          <div className="row">
            <input placeholder="Author" value={comment.author}
                   onChange={(e) => setComment({ ...comment, author: e.target.value })} />
            <input className="grow" placeholder="Add a comment..." value={comment.body}
                   onChange={(e) => setComment({ ...comment, body: e.target.value })} />
            <button className="primary" type="submit">Comment</button>
          </div>
        </form>
      </div>
    </div>
  );
}

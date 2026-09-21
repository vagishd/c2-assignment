import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, PRIORITIES } from '../api.js';
import ErrorBanner from '../components/ErrorBanner.jsx';

export default function TicketCreate() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    title: '', description: '', priority: 'MEDIUM', assignee: '', category: '',
  });
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  function set(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function onSubmit(e) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const created = await api.createTicket({
        title: form.title,
        description: form.description,
        priority: form.priority,
        assignee: form.assignee || null,
        category: form.category || null,
      });
      navigate(`/tickets/${created.id}`);
    } catch (err) {
      setError(err);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div>
      <h2>New ticket</h2>
      <ErrorBanner error={error} />
      <form className="card" onSubmit={onSubmit}>
        <label>Title</label>
        <input className="grow" style={{ width: '100%' }} value={form.title}
               onChange={(e) => set('title', e.target.value)} />

        <label>Description</label>
        <textarea value={form.description} onChange={(e) => set('description', e.target.value)} />

        <div className="row">
          <div>
            <label>Priority</label>
            <select value={form.priority} onChange={(e) => set('priority', e.target.value)}>
              {PRIORITIES.map((p) => <option key={p} value={p}>{p}</option>)}
            </select>
          </div>
          <div className="grow">
            <label>Assignee (optional)</label>
            <input style={{ width: '100%' }} value={form.assignee}
                   onChange={(e) => set('assignee', e.target.value)} />
          </div>
          <div className="grow">
            <label>Category (optional)</label>
            <input style={{ width: '100%' }} value={form.category}
                   onChange={(e) => set('category', e.target.value)} />
          </div>
        </div>

        <div style={{ marginTop: 16 }}>
          <button className="primary" type="submit" disabled={saving}>
            {saving ? 'Creating...' : 'Create ticket'}
          </button>
        </div>
      </form>
    </div>
  );
}

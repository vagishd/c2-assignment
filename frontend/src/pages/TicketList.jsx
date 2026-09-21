import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, STATUSES } from '../api.js';
import ErrorBanner from '../components/ErrorBanner.jsx';
import StatusBadge from '../components/StatusBadge.jsx';

export default function TicketList() {
  const navigate = useNavigate();
  const [tickets, setTickets] = useState([]);
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const page = await api.listTickets({ q, status });
      setTickets(page.content);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function onSearch(e) {
    e.preventDefault();
    load();
  }

  return (
    <div>
      <h2>Tickets</h2>
      <ErrorBanner error={error} />

      <form className="card row" onSubmit={onSearch}>
        <input
          className="grow"
          placeholder="Search by keyword..."
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
        <button className="primary" type="submit">Search</button>
      </form>

      <div className="card">
        {loading ? (
          <p className="muted">Loading...</p>
        ) : tickets.length === 0 ? (
          <p className="muted">No tickets found.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Ref</th><th>Title</th><th>Status</th><th>Priority</th><th>Assignee</th>
              </tr>
            </thead>
            <tbody>
              {tickets.map((t) => (
                <tr key={t.id} className="clickable" onClick={() => navigate(`/tickets/${t.id}`)}>
                  <td>{t.ticketRef}</td>
                  <td>{t.title}</td>
                  <td><StatusBadge status={t.status} /></td>
                  <td>{t.priority}</td>
                  <td>{t.assignee || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import ErrorBanner from '../components/ErrorBanner.jsx';

const EXAMPLES = [
  'Have we seen payment failures before?',
  'What are the common causes of shipment tracking issues?',
  'Show me similar resolved tickets.',
  'Which high-priority tickets are related to payment?',
];

export default function Ask() {
  const [question, setQuestion] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  async function onAsk(e) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const res = await api.ask(question);
      setResult(res);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <h2>Ask the assistant</h2>
      <p className="muted">
        Answers are grounded strictly in ticket history and cite the tickets used. If nothing
        relevant is found, the assistant says so rather than guessing.
      </p>
      <ErrorBanner error={error} />

      <form className="card" onSubmit={onAsk}>
        <textarea placeholder="Ask about the tickets..." value={question}
                  onChange={(e) => setQuestion(e.target.value)} />
        <div className="row" style={{ marginTop: 12 }}>
          <button className="primary" type="submit" disabled={loading || !question.trim()}>
            {loading ? 'Thinking...' : 'Ask'}
          </button>
        </div>
        <div style={{ marginTop: 12 }}>
          <span className="muted">Try: </span>
          {EXAMPLES.map((ex) => (
            <button type="button" key={ex} className="link" style={{ marginRight: 12 }}
                    onClick={() => setQuestion(ex)}>
              {ex}
            </button>
          ))}
        </div>
      </form>

      {result && (
        <div className="card">
          <p className="answer">{result.answer}</p>
          {result.grounded && result.sources.length > 0 && (
            <div className="sources">
              <strong>Sources:</strong>
              <ul>
                {result.sources.map((s) => (
                  <li key={s.ticketId}>
                    {s.ticketId} — {s.title} <span className="muted">({s.status})</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
          {!result.grounded && (
            <p className="muted">No ticket sources — this is an honest no-match response.</p>
          )}
        </div>
      )}

      <p style={{ marginTop: 16 }}><Link to="/">← Back to tickets</Link></p>
    </div>
  );
}

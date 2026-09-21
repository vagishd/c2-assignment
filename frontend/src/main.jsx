import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import TicketList from './pages/TicketList.jsx';
import TicketCreate from './pages/TicketCreate.jsx';
import TicketDetail from './pages/TicketDetail.jsx';
import Ask from './pages/Ask.jsx';
import './styles.css';

function App() {
  return (
    <BrowserRouter>
      <header className="app">
        <strong>Support Tickets</strong>
        <nav>
          <Link to="/">Tickets</Link>
          <Link to="/new">New</Link>
          <Link to="/ask">Ask AI</Link>
        </nav>
      </header>
      <div className="container">
        <Routes>
          <Route path="/" element={<TicketList />} />
          <Route path="/new" element={<TicketCreate />} />
          <Route path="/tickets/:id" element={<TicketDetail />} />
          <Route path="/ask" element={<Ask />} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);

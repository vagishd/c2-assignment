// Shows the backend's meaningful error message (code + message). Renders nothing when there's no error.
export default function ErrorBanner({ error }) {
  if (!error) return null;
  return (
    <div className="error">
      <strong>{error.code}</strong>: {error.message}
    </div>
  );
}

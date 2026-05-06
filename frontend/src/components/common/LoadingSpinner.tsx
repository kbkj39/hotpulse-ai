export function LoadingSpinner() {
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '32px', gap: '8px' }}>
      <div
        style={{
          width: '20px',
          height: '20px',
          border: '1px solid #2E2E2E',
          borderTopColor: '#fff',
          borderRadius: '50%',
          animation: 'spin 0.8s linear infinite',
        }}
      />
      <style>{`@keyframes spin { to { transform: rotate(360deg) } }`}</style>
      <span style={{ fontFamily: "'Fira Code', 'Noto Sans SC', monospace", fontSize: '12px', color: '#555', letterSpacing: '0.05em' }}>LOADING</span>
    </div>
  )
}

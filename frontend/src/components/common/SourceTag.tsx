interface SourceTagProps {
  source: string
}

export function SourceTag({ source }: SourceTagProps) {
  return (
    <span
      style={{
        display: 'inline-block',
        border: '1px solid #2E2E2E',
        padding: '1px 6px',
        fontSize: '12px',
        fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
        color: '#888',
        letterSpacing: '0.03em',
        textTransform: 'uppercase',
      }}
    >
      {source}
    </span>
  )
}

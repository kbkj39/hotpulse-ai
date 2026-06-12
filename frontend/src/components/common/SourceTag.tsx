interface SourceTagProps {
  source: string
}

export function SourceTag({ source }: SourceTagProps) {
  return (
    <span
      style={{
        display: 'inline-block',
        border: '1px solid #303030',
        background: '#050505',
        padding: '2px 7px',
        fontSize: '11px',
        fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
        color: '#A0A0A0',
        letterSpacing: '0',
        borderRadius: '4px',
      }}
    >
      {source}
    </span>
  )
}

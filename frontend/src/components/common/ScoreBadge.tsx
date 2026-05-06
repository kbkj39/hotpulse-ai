interface ScoreBadgeProps {
  label: string
  value: number
}

export function ScoreBadge({ label, value }: ScoreBadgeProps) {
  const pct = (value * 100).toFixed(0)
  const opacity = value >= 0.7 ? 1 : value >= 0.4 ? 0.6 : 0.35

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '4px',
        border: '1px solid #1F1F1F',
        padding: '1px 6px',
        fontSize: '12px',
        fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
        color: `rgba(255,255,255,${opacity})`,
        letterSpacing: '0.03em',
      }}
    >
      {label}<span style={{ color: '#555' }}>/</span>{pct}
    </span>
  )
}

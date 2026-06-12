import type { AgentStepEvent } from '@/types/agent'

const STATUS_DOT: Record<string, { symbol: string; color: string }> = {
  PENDING:  { symbol: '○', color: '#333' },
  RUNNING:  { symbol: '◉', color: '#888' },
  DONE:     { symbol: '●', color: '#fff' },
  FAILED:   { symbol: '✕', color: '#555' },
}

interface AgentProgressPanelProps {
  steps: AgentStepEvent[]
}

export function AgentProgressPanel({ steps }: AgentProgressPanelProps) {
  if (steps.length === 0) return null

  return (
    <div
      style={{
        borderTop: '1px solid #151515',
        padding: '10px 14px',
        maxHeight: '140px',
        overflowY: 'auto',
        background: '#030303',
      }}
    >
      <p
        style={{
          fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
          fontSize: '11px',
          color: '#666',
          letterSpacing: '0.04em',
          marginBottom: '8px',
          margin: '0 0 8px 0',
        }}
      >
        执行进度
      </p>
      {steps.map((step, i) => {
        const dot = STATUS_DOT[step.status] ?? { symbol: '·', color: '#333' }
        return (
          <div
            key={i}
            style={{
              display: 'flex',
              alignItems: 'flex-start',
              gap: '8px',
              fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
              fontSize: '11px',
              marginBottom: '4px',
              lineHeight: 1.5,
            }}
          >
            <span style={{ color: dot.color, flexShrink: 0, marginTop: '1px' }}>{dot.symbol}</span>
            <span style={{ color: '#555', flexShrink: 0 }}>[{step.agentName}]</span>
            <span style={{ flex: 1, color: '#777' }}>{step.message}</span>
            <span style={{ color: '#2E2E2E', fontSize: '11px', whiteSpace: 'nowrap' }}>
              {step.timestamp ? new Date(step.timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : ''}
            </span>
          </div>
        )
      })}
    </div>
  )
}

import type { Message } from '@/types/chat'

interface MessageBubbleProps {
  message: Message
}

export function MessageBubble({ message }: MessageBubbleProps) {
  const isUser = message.role === 'user'

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: isUser ? 'flex-end' : 'flex-start',
        marginBottom: '14px',
      }}
    >
      <div
        style={{
          maxWidth: '76%',
          padding: '11px 14px',
          border: `1px solid ${isUser ? '#2A2A2A' : '#1F1F1F'}`,
          background: isUser ? '#111' : '#050505',
          color: isUser ? '#fff' : '#D0D0D0',
          fontFamily: "'Fira Sans', 'Noto Sans SC', sans-serif",
          fontSize: '14px',
          lineHeight: 1.7,
          borderRadius: '8px',
        }}
      >
        {!isUser && (
          <div
            style={{
              fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
              fontSize: '10px',
              color: '#666',
              letterSpacing: '0.04em',
              marginBottom: '6px',
            }}
          >
            助手
          </div>
        )}
        <p style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{message.content}</p>
        {!isUser && message.sourcesJson && (() => {
          try {
            const sources: Array<{url?: string; snippet?: string}> = JSON.parse(message.sourcesJson)
            if (!sources.length) return null
            return (
              <div style={{ marginTop: '10px', paddingTop: '10px', borderTop: '1px solid #1F1F1F' }}>
                <span style={{ fontFamily: "'Fira Code', 'Noto Sans SC', monospace", fontSize: '11px', color: '#444', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                  来源
                </span>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '3px', marginTop: '4px' }}>
                  {sources.map((src, i) => (
                    <a
                      key={i}
                      href={src.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      style={{
                        fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
                        fontSize: '12px',
                        color: '#555',
                        textDecoration: 'none',
                        transition: 'color 150ms ease',
                      }}
                      onMouseEnter={(e) => (e.currentTarget.style.color = '#fff')}
                      onMouseLeave={(e) => (e.currentTarget.style.color = '#555')}
                    >
                      [{i + 1}] {src.snippet?.slice(0, 50)}…
                    </a>
                  ))}
                </div>
              </div>
            )
          } catch { return null }
        })()}
      </div>
    </div>
  )
}

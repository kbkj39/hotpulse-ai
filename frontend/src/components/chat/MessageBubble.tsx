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
        marginBottom: '12px',
      }}
    >
      <div
        style={{
          maxWidth: '72%',
          padding: '10px 14px',
          border: `1px solid ${isUser ? '#2E2E2E' : '#1F1F1F'}`,
          background: isUser ? '#111' : 'transparent',
          color: isUser ? '#fff' : '#ccc',
          fontFamily: "'Fira Sans', 'Noto Sans SC', sans-serif",
          fontSize: '14px',
          lineHeight: 1.7,
        }}
      >
        {!isUser && (
          <div
            style={{
              fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
              fontSize: '10px',
              color: '#444',
              letterSpacing: '0.1em',
              textTransform: 'uppercase',
              marginBottom: '6px',
            }}
          >
            ASSISTANT
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
                  SOURCES
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

import { useEffect, useRef } from 'react'
import type { Message } from '@/types/chat'
import { MessageBubble } from './MessageBubble'

interface ChatWindowProps {
  messages: Message[]
}

export function ChatWindow({ messages }: ChatWindowProps) {
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  return (
    <div
      style={{
        flex: 1,
        overflowY: 'auto',
        padding: '20px 22px',
        background: '#020202',
      }}
    >
      {messages.length === 0 && (
        <div
          style={{
            maxWidth: '520px',
            margin: '72px auto 0',
            textAlign: 'center',
          }}
        >
          <div
            style={{
              fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
              fontSize: '13px',
              color: '#888',
              marginBottom: '10px',
            }}
          >
            开始一次分析
          </div>
          <p
            style={{
              margin: 0,
              fontFamily: "'Fira Sans', 'Noto Sans SC', sans-serif",
              fontSize: '14px',
              color: '#555',
              lineHeight: 1.7,
            }}
          >
            你可以直接提问，也可以先从右侧选择一个热点，再围绕该新闻追问背景、可信度或影响。
          </p>
        </div>
      )}
      {messages.map((msg, i) => (
        <MessageBubble key={i} message={msg} />
      ))}
      <div ref={bottomRef} />
    </div>
  )
}

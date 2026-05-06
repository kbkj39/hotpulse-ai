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
        padding: '16px',
        border: '2px solid #1F1F1F',
        borderBottom: '2px solid #1F1F1F',
        background: '#000',
      }}
    >
      {messages.length === 0 && (
        <p
          style={{
            textAlign: 'center',
            marginTop: '64px',
            fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
            fontSize: '13px',
            color: '#2E2E2E',
            letterSpacing: '0.08em',
            textTransform: 'uppercase',
          }}
        >
          输入问题开始对话
        </p>
      )}
      {messages.map((msg, i) => (
        <MessageBubble key={i} message={msg} />
      ))}
      <div ref={bottomRef} />
    </div>
  )
}

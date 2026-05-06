import { useState, useRef } from 'react'

interface ChatInputProps {
  onSend: (text: string) => void
  disabled?: boolean
}

export function ChatInput({ onSend, disabled }: ChatInputProps) {
  const [text, setText] = useState('')
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  const handleSend = () => {
    const trimmed = text.trim()
    if (!trimmed || disabled) return
    onSend(trimmed)
    setText('')
    textareaRef.current?.focus()
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const canSend = !disabled && text.trim().length > 0

  return (
    <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
      <textarea
        ref={textareaRef}
        value={text}
        onChange={(e) => setText(e.target.value)}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        rows={2}
        placeholder="输入问题，按 Enter 发送"
        style={{
          flex: 1,
          border: '2px solid #1F1F1F',
          background: disabled ? '#0A0A0A' : '#000',
          color: '#fff',
          fontFamily: "'Fira Sans', 'Noto Sans SC', sans-serif",
          fontSize: '14px',
          padding: '8px 12px',
          resize: 'none',
          outline: 'none',
          transition: 'border-color 150ms ease',
          opacity: disabled ? 0.5 : 1,
        }}
        onFocus={(e) => (e.currentTarget.style.borderColor = '#2E2E2E')}
        onBlur={(e) => (e.currentTarget.style.borderColor = '#1F1F1F')}
      />
      <button
        onClick={handleSend}
        disabled={!canSend}
        style={{
          padding: '8px 20px',
          border: `2px solid ${canSend ? '#fff' : '#1F1F1F'}`,
          background: canSend ? '#fff' : 'transparent',
          color: canSend ? '#000' : '#333',
          fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
          fontSize: '13px',
          letterSpacing: '0.08em',
          textTransform: 'uppercase',
          cursor: canSend ? 'pointer' : 'not-allowed',
          transition: 'all 150ms ease',
          alignSelf: 'stretch',
        }}
      >
        SEND
      </button>
    </div>
  )
}

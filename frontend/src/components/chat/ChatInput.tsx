import { useState, useRef, type ReactNode, type KeyboardEvent } from 'react'

interface ChatInputProps {
  onSend: (text: string) => void
  disabled?: boolean
  placeholder?: string
  pinnedHotspotBar?: ReactNode
}

export function ChatInput({ onSend, disabled, placeholder, pinnedHotspotBar }: ChatInputProps) {
  const [text, setText] = useState('')
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  const handleSend = () => {
    const trimmed = text.trim()
    if (!trimmed || disabled) return
    onSend(trimmed)
    setText('')
    textareaRef.current?.focus()
  }

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const canSend = !disabled && text.trim().length > 0

  return (
    <div style={{ padding: '12px 14px 14px', borderTop: '1px solid #1F1F1F', background: '#030303' }}>
      {pinnedHotspotBar}
      <div style={{ display: 'flex', gap: '10px', marginTop: pinnedHotspotBar ? '10px' : 0 }}>
      <textarea
        ref={textareaRef}
        value={text}
        onChange={(e) => setText(e.target.value)}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        rows={2}
        placeholder={placeholder ?? '输入问题，Enter 发送，Shift + Enter 换行'}
        style={{
          flex: 1,
          border: '1px solid #242424',
          background: disabled ? '#0A0A0A' : '#000',
          color: '#fff',
          fontFamily: "'Fira Sans', 'Noto Sans SC', sans-serif",
          fontSize: '14px',
          padding: '10px 12px',
          resize: 'none',
          outline: 'none',
          borderRadius: '8px',
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
          padding: '8px 18px',
          border: `1px solid ${canSend ? '#E6E6E6' : '#1F1F1F'}`,
          background: canSend ? '#E6E6E6' : 'transparent',
          color: canSend ? '#050505' : '#444',
          fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
          fontSize: '12px',
          letterSpacing: '0',
          borderRadius: '8px',
          cursor: canSend ? 'pointer' : 'not-allowed',
          transition: 'all 150ms ease',
          alignSelf: 'stretch',
        }}
      >
        发送
      </button>
      </div>
    </div>
  )
}

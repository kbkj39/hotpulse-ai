import { useEffect, useCallback } from 'react'
import { api } from '@/services/api'
import { useChatStore } from '@/store/chatStore'
import type { Conversation } from '@/types/chat'

interface ConversationHistoryPanelProps {
  onLoadConversation: (conversationId: string) => void
}

function formatTime(iso: string | undefined) {
  if (!iso) return ''
  const d = new Date(iso)
  const now = new Date()
  const diffMs = now.getTime() - d.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  const diffH = Math.floor(diffMin / 60)
  if (diffH < 24) return `${diffH}小时前`
  const diffD = Math.floor(diffH / 24)
  if (diffD < 7) return `${diffD}天前`
  return d.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
}

export function ConversationHistoryPanel({ onLoadConversation }: ConversationHistoryPanelProps) {
  const { conversations, conversationId, setConversations, startNewConversation } = useChatStore()

  const loadConversations = useCallback(async () => {
    try {
      const result = await api.getConversations(1, 30)
      setConversations(result.items)
    } catch (err) {
      console.error('Failed to load conversations:', err)
    }
  }, [setConversations])

  useEffect(() => {
    loadConversations()
  }, [loadConversations, conversationId])

  const handleSelect = async (conv: Conversation) => {
    if (String(conv.id) === conversationId) return
    try {
      const messages = await api.getConversationMessages(conv.id)
      onLoadConversation(String(conv.id))
      useChatStore.setState({ messages, conversationId: String(conv.id) })
    } catch (err) {
      console.error('Failed to load conversation messages:', err)
    }
  }

  return (
    <div
      style={{
        width: '220px',
        minWidth: '220px',
        borderRight: '2px solid #1F1F1F',
        display: 'flex',
        flexDirection: 'column',
        background: '#000',
        overflow: 'hidden',
      }}
    >
      {/* Header */}
      <div
        style={{
          padding: '0 12px 12px',
          borderBottom: '1px solid #1F1F1F',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <span
          style={{
            fontFamily: "'Fira Code', monospace",
            fontSize: '11px',
            letterSpacing: '0.1em',
            color: '#555',
            textTransform: 'uppercase',
          }}
        >
          历史对话
        </span>
        <button
          onClick={startNewConversation}
          title="新对话"
          style={{
            background: 'none',
            border: '1px solid #2E2E2E',
            color: '#888',
            fontFamily: "'Fira Code', monospace",
            fontSize: '11px',
            padding: '2px 8px',
            cursor: 'pointer',
            letterSpacing: '0.05em',
          }}
        >
          + 新建
        </button>
      </div>

      {/* List */}
      <div style={{ flex: 1, overflowY: 'auto' }}>
        {conversations.length === 0 && (
          <p
            style={{
              padding: '20px 12px',
              fontFamily: "'Fira Code', monospace",
              fontSize: '11px',
              color: '#2E2E2E',
              textAlign: 'center',
            }}
          >
            暂无历史对话
          </p>
        )}
        {conversations.map((conv) => {
          const isActive = String(conv.id) === conversationId
          return (
            <button
              key={conv.id}
              onClick={() => handleSelect(conv)}
              style={{
                display: 'block',
                width: '100%',
                textAlign: 'left',
                padding: '10px 12px',
                background: isActive ? '#111' : 'none',
                border: 'none',
                borderLeft: isActive ? '2px solid #39FF14' : '2px solid transparent',
                cursor: 'pointer',
                borderBottom: '1px solid #111',
              }}
            >
              <div
                style={{
                  fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
                  fontSize: '12px',
                  color: isActive ? '#fff' : '#888',
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  maxWidth: '180px',
                }}
              >
                {conv.title || '（无标题）'}
              </div>
              <div
                style={{
                  fontFamily: "'Fira Code', monospace",
                  fontSize: '10px',
                  color: '#333',
                  marginTop: '3px',
                  letterSpacing: '0.04em',
                }}
              >
                {formatTime(conv.createdAt)}
              </div>
            </button>
          )
        })}
      </div>
    </div>
  )
}

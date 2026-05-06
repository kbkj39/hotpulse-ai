import { useEffect } from 'react'
import { useChat } from '@/hooks/useChat'
import { useAgentExecution } from '@/hooks/useAgentExecution'
import { useAgentExecutionStore } from '@/store/agentExecutionStore'
import { ChatWindow } from '@/components/chat/ChatWindow'
import { ChatInput } from '@/components/chat/ChatInput'
import { AgentProgressPanel } from '@/components/chat/AgentProgressPanel'
import { ConversationHistoryPanel } from '@/components/chat/ConversationHistoryPanel'
import { useHotspots } from '@/hooks/useHotspots'
import { useSocket } from '@/hooks/useSocket'
import { HotspotFilterBar } from '@/components/hotspot/HotspotFilter'
import { HotspotList } from '@/components/hotspot/HotspotList'

export function ChatPage() {
  const { messages, sendMessage, addAssistantMessage } = useChat()
  const { steps, finalAnswer, isError, isRunning } = useAgentExecution(null)
  const resetExecution = useAgentExecutionStore((s) => s.reset)
  const { hotspots, filter, total, setFilter, loading } = useHotspots()
  useSocket()

  useEffect(() => {
    if (finalAnswer) {
      addAssistantMessage(finalAnswer)
    }
  }, [finalAnswer, addAssistantMessage])

  useEffect(() => {
    if (isError) {
      addAssistantMessage('抱歉，连接服务器失败，请检查后端是否正常运行后重试。')
    }
  }, [isError, addAssistantMessage])

  const handleSend = async (text: string) => {
    await sendMessage(text)
  }

  const handleLoadConversation = (conversationId: string) => {
    // 切换会话后重置执行状态
    resetExecution()
    console.debug('Switched to conversation', conversationId)
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 96px)' }}>
      {/* Page title */}
      <div
        style={{
          display: 'flex',
          alignItems: 'baseline',
          marginBottom: '16px',
          paddingBottom: '16px',
          borderBottom: '2px solid #1F1F1F',
        }}
      >
        <h1
          style={{
            fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
            fontWeight: 600,
            fontSize: '20px',
            letterSpacing: '-0.02em',
            color: '#fff',
            margin: 0,
          }}
        >
          智能对话
        </h1>
        {isRunning && (
          <span
            style={{
              marginLeft: '12px',
              fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
              fontSize: '11px',
              color: '#555',
              letterSpacing: '0.1em',
              textTransform: 'uppercase',
            }}
          >
            PROCESSING…
          </span>
        )}
      </div>

      {/* Body: history sidebar + chat area */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        <ConversationHistoryPanel onLoadConversation={handleLoadConversation} />

        {/* Chat column */}
        <div style={{ display: 'flex', flexDirection: 'column', flex: 1, overflow: 'hidden' }}>
          <ChatWindow messages={messages} />
          <AgentProgressPanel steps={steps} />
          <ChatInput onSend={handleSend} disabled={isRunning} />
        </div>

        {/* Right hotspots sidebar */}
        <div style={{ width: '360px', minWidth: '300px', borderLeft: '2px solid #1F1F1F', background: '#000', overflow: 'hidden' }}>
          <div style={{ padding: '12px', borderBottom: '1px solid #1F1F1F' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
              <h3 style={{ fontFamily: "'Fira Code', monospace", fontSize: '12px', color: '#888', margin: 0 }}>热点</h3>
              <span style={{ fontFamily: "'Fira Code', monospace", fontSize: '11px', color: '#333' }}>{total} ITEMS</span>
            </div>
            <HotspotFilterBar filter={filter} onChange={setFilter} />
          </div>
          <div style={{ height: '100%', overflowY: 'auto' }}>
            {loading ? <div style={{ padding: '24px' }}>加载中…</div> : <HotspotList hotspots={hotspots} />}
          </div>
        </div>
      </div>
    </div>
  )
}

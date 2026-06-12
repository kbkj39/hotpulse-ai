import { useEffect } from 'react'
import { useChat } from '@/hooks/useChat'
import { useAgentExecution } from '@/hooks/useAgentExecution'
import { useAgentExecutionStore } from '@/store/agentExecutionStore'
import { ChatWindow } from '@/components/chat/ChatWindow'
import { ChatInput } from '@/components/chat/ChatInput'
import { AgentProgressPanel } from '@/components/chat/AgentProgressPanel'
import { ConversationHistoryPanel } from '@/components/chat/ConversationHistoryPanel'
import { PinnedHotspotBar } from '@/components/chat/PinnedHotspotBar'
import { useHotspots } from '@/hooks/useHotspots'
import { useSocket } from '@/hooks/useSocket'
import { useChatStore } from '@/store/chatStore'
import type { Hotspot } from '@/types/hotspot'
import { HotspotFilterBar } from '@/components/hotspot/HotspotFilter'
import { HotspotList } from '@/components/hotspot/HotspotList'

export function ChatPage() {
  const { messages, pinnedHotspot, sendMessage, addAssistantMessage } = useChat()
  const { steps, finalAnswer, isError, isRunning } = useAgentExecution(null)
  const resetExecution = useAgentExecutionStore((s) => s.reset)
  const setPinnedHotspot = useChatStore((s) => s.setPinnedHotspot)
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
    resetExecution()
    setPinnedHotspot(null)
    console.debug('Switched to conversation', conversationId)
  }

  const handleHotspotSelect = (hotspot: Hotspot) => {
    setPinnedHotspot(pinnedHotspot?.id === hotspot.id ? null : hotspot)
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 96px)', minHeight: 0 }}>
      <div
        style={{
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          gap: '20px',
          marginBottom: '18px',
          paddingBottom: '16px',
          borderBottom: '2px solid #1F1F1F',
          flexWrap: 'wrap',
        }}
      >
        <div>
          <h1
            style={{
              fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
              fontWeight: 600,
              fontSize: '22px',
              letterSpacing: '0',
              color: '#fff',
              margin: 0,
            }}
          >
            智能对话
          </h1>
          <div
            style={{
              marginTop: '6px',
              fontFamily: "'Fira Sans', 'Noto Sans SC', sans-serif",
              fontSize: '13px',
              color: '#777',
            }}
          >
            直接提问，或从右侧选择一个热点作为上下文继续分析。
          </div>
        </div>
        {isRunning && (
          <span
            style={{
              marginTop: '4px',
              fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
              fontSize: '11px',
              color: '#9CE6A3',
              letterSpacing: '0.04em',
              border: '1px solid #263A2A',
              background: '#071107',
              borderRadius: '5px',
              padding: '5px 9px',
            }}
          >
            正在处理…
          </span>
        )}
      </div>

      <div style={{ display: 'flex', flex: 1, minHeight: 0, overflow: 'hidden', border: '1px solid #1F1F1F', borderRadius: '8px' }}>
        <ConversationHistoryPanel onLoadConversation={handleLoadConversation} />

        <div style={{ display: 'flex', flexDirection: 'column', flex: 1, minWidth: 0, overflow: 'hidden', background: '#020202' }}>
          <ChatWindow messages={messages} />
          <AgentProgressPanel steps={steps} />
          <ChatInput
            onSend={handleSend}
            disabled={isRunning}
            placeholder={pinnedHotspot ? '针对当前热点提问…' : undefined}
            pinnedHotspotBar={
              pinnedHotspot ? (
                <PinnedHotspotBar
                  hotspot={pinnedHotspot}
                  onClear={() => setPinnedHotspot(null)}
                />
              ) : undefined
            }
          />
        </div>

        <aside style={{ width: '340px', minWidth: '300px', borderLeft: '1px solid #1F1F1F', background: '#000', overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <div style={{ padding: '14px', borderBottom: '1px solid #1F1F1F' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
              <h3 style={{ fontFamily: "'Fira Code', 'Noto Sans SC', monospace", fontSize: '13px', color: '#E6E6E6', margin: 0 }}>相关热点</h3>
              <span style={{ fontFamily: "'Fira Code', 'Noto Sans SC', monospace", fontSize: '11px', color: '#666' }}>{total} 条</span>
            </div>
            <HotspotFilterBar filter={filter} onChange={setFilter} />
          </div>
          <div style={{ flex: 1, minHeight: 0, overflowY: 'auto', padding: '0 12px 12px' }}>
            {loading ? (
              <div style={{ padding: '24px', color: '#666', fontFamily: "'Fira Code', 'Noto Sans SC', monospace", fontSize: '12px' }}>加载中…</div>
            ) : (
              <HotspotList
                hotspots={hotspots}
                selectable
                compact
                selectedHotspotId={pinnedHotspot?.id ?? null}
                onSelectHotspot={handleHotspotSelect}
              />
            )}
          </div>
        </aside>
      </div>
    </div>
  )
}

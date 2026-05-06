import { useCallback } from 'react'
import { api } from '@/services/api'
import { useChatStore } from '@/store/chatStore'
import { useAgentExecutionStore } from '@/store/agentExecutionStore'
import type { Message } from '@/types/chat'

export function useChat() {
  const { messages, conversationId, addMessage, setConversationId } = useChatStore()
  const startExecution = useAgentExecutionStore((s) => s.start)

  const sendMessage = useCallback(
    async (query: string, onExecutionId?: (id: string) => void) => {
      const userMessage: Message = { role: 'user', content: query }
      addMessage(userMessage)

      try {
        const result = await api.agentQuery(query, conversationId ?? undefined)
        if (!conversationId && result.conversationId) {
          setConversationId(result.conversationId)
        }
        // 在 store 层启动 SSE，订阅生命周期独立于任何页面组件
        startExecution(result.executionId)
        onExecutionId?.(result.executionId)
      } catch (err) {
        console.error('Failed to send message:', err)
        addMessage({ role: 'assistant', content: '抱歉，请求失败，请稍后重试。' })
      }
    },
    [conversationId, addMessage, setConversationId, startExecution]
  )

  const addAssistantMessage = useCallback(
    (content: string) => {
      addMessage({ role: 'assistant', content })
    },
    [addMessage]
  )

  return { messages, conversationId, sendMessage, addAssistantMessage }
}

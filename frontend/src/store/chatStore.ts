import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Message, Conversation } from '@/types/chat'

interface ChatState {
  messages: Message[]
  conversationId: string | null
  conversations: Conversation[]
  setConversationId: (id: string) => void
  addMessage: (message: Message) => void
  setMessages: (messages: Message[]) => void
  clearMessages: () => void
  setConversations: (conversations: Conversation[]) => void
  startNewConversation: () => void
}

export const useChatStore = create<ChatState>()(
  persist(
    (set) => ({
      messages: [],
      conversationId: null,
      conversations: [],
      setConversationId: (id) => set({ conversationId: id }),
      addMessage: (message) =>
        set((state) => ({ messages: [...state.messages, message] })),
      setMessages: (messages) => set({ messages }),
      clearMessages: () => set({ messages: [], conversationId: null }),
      setConversations: (conversations) => set({ conversations }),
      startNewConversation: () => set({ messages: [], conversationId: null }),
    }),
    {
      name: 'hotpulse-chat',
    }
  )
)

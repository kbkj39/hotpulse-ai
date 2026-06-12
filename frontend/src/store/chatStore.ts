import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Message, Conversation } from '@/types/chat'
import type { Hotspot } from '@/types/hotspot'

interface ChatState {
  messages: Message[]
  conversationId: string | null
  conversations: Conversation[]
  pinnedHotspot: Hotspot | null
  setConversationId: (id: string) => void
  addMessage: (message: Message) => void
  setMessages: (messages: Message[]) => void
  clearMessages: () => void
  setConversations: (conversations: Conversation[]) => void
  setPinnedHotspot: (hotspot: Hotspot | null) => void
  startNewConversation: () => void
}

export const useChatStore = create<ChatState>()(
  persist(
    (set) => ({
      messages: [],
      conversationId: null,
      conversations: [],
      pinnedHotspot: null,
      setConversationId: (id) => set({ conversationId: id }),
      addMessage: (message) =>
        set((state) => ({ messages: [...state.messages, message] })),
      setMessages: (messages) => set({ messages }),
      clearMessages: () => set({ messages: [], conversationId: null, pinnedHotspot: null }),
      setConversations: (conversations) => set({ conversations }),
      setPinnedHotspot: (hotspot) => set({ pinnedHotspot: hotspot }),
      startNewConversation: () => set({ messages: [], conversationId: null, pinnedHotspot: null }),
    }),
    {
      name: 'hotpulse-chat',
    }
  )
)

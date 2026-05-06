export interface Message {
  id?: number
  role: 'user' | 'assistant'
  content: string
  sourcesJson?: string
  createdAt?: string
}

export interface Conversation {
  id: number
  title: string
  createdAt: string
}

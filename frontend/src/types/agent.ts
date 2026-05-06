export interface AgentStepEvent {
  agentName: string
  status: 'RUNNING' | 'DONE' | 'FAILED'
  message: string
  timestamp: string
  answer?: string
  hotspots?: unknown[]
}

export interface TaskPlan {
  topics: string[]
  sources: string[]
  keywords: string[]
  priority: string
  timeRange: string
}

export type AgentStatus = 'PENDING' | 'RUNNING' | 'DONE' | 'FAILED'

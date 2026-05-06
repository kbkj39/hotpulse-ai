import type { AgentStepEvent } from '@/types/agent'

export const agentSse = {
  subscribe: (
    executionId: string,
    onStep: (event: AgentStepEvent) => void,
    onError?: (err: Event) => void,
    onComplete?: () => void
  ): EventSource => {
    const base = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
    const eventSource = new EventSource(`${base}/agent/stream/${executionId}`)

    eventSource.onmessage = (e) => {
      try {
        const event: AgentStepEvent = JSON.parse(e.data)
        onStep(event)
        // System 的 DONE 或 FAILED 均为终止信号，关闭流
        if (event.agentName === 'System' && (event.status === 'DONE' || event.status === 'FAILED')) {
          eventSource.close()
          onComplete?.()
        }
      } catch (err) {
        console.warn('Failed to parse SSE event:', e.data, err)
      }
    }

    eventSource.onerror = (err) => {
      onError?.(err)
      eventSource.close()
    }

    return eventSource
  },
}

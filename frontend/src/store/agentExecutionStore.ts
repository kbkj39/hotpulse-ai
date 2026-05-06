import { create } from 'zustand'
import { agentSse } from '@/services/agentSse'
import type { AgentStepEvent } from '@/types/agent'
import { useHotspotStore } from '@/store/hotspotStore'

interface AgentExecutionState {
  executionId: string | null
  steps: AgentStepEvent[]
  finalAnswer: string | null
  isError: boolean
  isRunning: boolean
  _eventSource: EventSource | null

  start: (executionId: string) => void
  stop: () => void
  reset: () => void
}

export const useAgentExecutionStore = create<AgentExecutionState>((set, get) => ({
  executionId: null,
  steps: [],
  finalAnswer: null,
  isError: false,
  isRunning: false,
  _eventSource: null,

  start: (executionId: string) => {
    // 先关闭已有的连接
    get()._eventSource?.close()

    set({ executionId, steps: [], finalAnswer: null, isError: false, isRunning: true, _eventSource: null })

    const es = agentSse.subscribe(
      executionId,
      (event) => {
        set((state) => ({ steps: [...state.steps, event] }))

        if (event.agentName === 'System') {
          if (event.answer) set({ finalAnswer: event.answer })

          if (event.hotspots && Array.isArray(event.hotspots) && event.hotspots.length > 0) {
            const addHotspot = useHotspotStore.getState().addHotspot
            for (let i = event.hotspots.length - 1; i >= 0; i--) {
              try { addHotspot(event.hotspots[i] as any) } catch { /* ignore */ }
            }
          }

          if (event.status === 'DONE' || event.status === 'FAILED') {
            set({ isRunning: false, _eventSource: null })
          }
        }
      },
      () => {
        set({ isError: true, isRunning: false, _eventSource: null })
      },
      () => {
        set({ isRunning: false, _eventSource: null })
      },
    )

    set({ _eventSource: es })
  },

  stop: () => {
    get()._eventSource?.close()
    set({ isRunning: false, _eventSource: null })
  },

  reset: () => {
    get()._eventSource?.close()
    set({ executionId: null, steps: [], finalAnswer: null, isError: false, isRunning: false, _eventSource: null })
  },
}))

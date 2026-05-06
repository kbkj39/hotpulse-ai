import { useEffect } from 'react'
import { useAgentExecutionStore } from '@/store/agentExecutionStore'

export function useAgentExecution(executionId: string | null) {
  const storeExecutionId = useAgentExecutionStore((s) => s.executionId)
  const start = useAgentExecutionStore((s) => s.start)
  const steps = useAgentExecutionStore((s) => s.steps)
  const finalAnswer = useAgentExecutionStore((s) => s.finalAnswer)
  const isError = useAgentExecutionStore((s) => s.isError)
  const isRunning = useAgentExecutionStore((s) => s.isRunning)

  useEffect(() => {
    if (executionId && executionId !== storeExecutionId) {
      start(executionId)
    }
  }, [executionId, storeExecutionId, start])

  return { steps, finalAnswer, isError, isRunning }
}

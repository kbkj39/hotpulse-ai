import { useAgentExecutionStore } from '@/store/agentExecutionStore'

export function useAgentExecution(_executionId: string | null) {
  const steps = useAgentExecutionStore((s) => s.steps)
  const finalAnswer = useAgentExecutionStore((s) => s.finalAnswer)
  const isError = useAgentExecutionStore((s) => s.isError)
  const isRunning = useAgentExecutionStore((s) => s.isRunning)

  return { steps, finalAnswer, isError, isRunning }
}

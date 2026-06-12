import { useState, useEffect, useCallback, useRef } from 'react'
import { isAxiosError } from 'axios'
import { api } from '@/services/api'

export type DailyReportStatus = 'PENDING' | 'GENERATING' | 'READY' | 'EMPTY' | 'FAILED'

export interface DailyReport {
  reportDate: string
  content: string
  hotspotCount: number | null
  generatedAt: string | null
  status: DailyReportStatus
  errorMessage: string | null
}

const POLLING_STATUSES: DailyReportStatus[] = ['PENDING', 'GENERATING']
const POLL_INTERVAL_MS = 3000

function isPollingStatus(status: DailyReportStatus | undefined): boolean {
  return status != null && POLLING_STATUSES.includes(status)
}

export function useReport(date?: string) {
  const [report, setReport] = useState<DailyReport | null>(null)
  const [loading, setLoading] = useState(false)
  const [regenerating, setRegenerating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const reportRef = useRef<DailyReport | null>(null)

  reportRef.current = report

  const fetchReport = useCallback(
    async (silent = false) => {
      if (!silent) {
        setLoading(true)
      }
      setError(null)
      try {
        const data = date ? await api.getDailyReport(date) : await api.getLatestDailyReport()
        setReport(data as DailyReport)
      } catch (err) {
        console.error('Failed to fetch report:', err)
        setReport(null)
        if (isAxiosError(err) && err.response?.status === 404) {
          setError(date ? `暂无 ${date} 的日报` : '暂无日报数据')
        } else {
          setError('加载日报失败')
        }
      } finally {
        if (!silent) {
          setLoading(false)
        }
      }
    },
    [date]
  )

  const regenerate = useCallback(
    async (targetDate: string) => {
      setRegenerating(true)
      setError(null)
      try {
        const data = await api.regenerateDailyReport(targetDate)
        setReport(data as DailyReport)
      } catch (err) {
        console.error('Failed to regenerate report:', err)
        if (isAxiosError(err)) {
          const message =
            (err.response?.data as { message?: string } | undefined)?.message ??
            err.message
          setError(message || '重跑失败')
        } else {
          setError('重跑失败')
        }
      } finally {
        setRegenerating(false)
      }
    },
    []
  )

  useEffect(() => {
    fetchReport()
  }, [fetchReport])

  useEffect(() => {
    if (!isPollingStatus(report?.status)) {
      return
    }

    const timer = window.setInterval(() => {
      if (isPollingStatus(reportRef.current?.status)) {
        fetchReport(true)
      }
    }, POLL_INTERVAL_MS)

    return () => window.clearInterval(timer)
  }, [report?.status, fetchReport])

  return {
    report,
    loading,
    regenerating,
    error,
    refetch: () => fetchReport(),
    regenerate,
  }
}

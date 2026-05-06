import { useState, useEffect, useCallback } from 'react'
import { api } from '@/services/api'

export interface DailyReport {
  reportDate: string
  content: string
  hotspotCount: number
  generatedAt: string
}

export function useReport(date?: string) {
  const [report, setReport] = useState<DailyReport | null>(null)
  const [loading, setLoading] = useState(false)

  const fetchReport = useCallback(async () => {
    setLoading(true)
    try {
      const data = date ? await api.getDailyReport(date) : await api.getLatestDailyReport()
      setReport(data as DailyReport)
    } catch (err) {
      console.error('Failed to fetch report:', err)
      setReport(null)
    } finally {
      setLoading(false)
    }
  }, [date])

  useEffect(() => {
    fetchReport()
  }, [fetchReport])

  return { report, loading, refetch: fetchReport }
}

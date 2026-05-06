import { useCallback, useEffect, useState } from 'react'
import axios from 'axios'
import { api } from '@/services/api'
import type { MonitorKeyword } from '@/types/monitorKeyword'

export function useMonitorKeywords() {
  const [keywords, setKeywords] = useState<MonitorKeyword[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const readError = (err: unknown, fallback: string) => {
    if (axios.isAxiosError(err)) {
      const message = err.response?.data?.message
      if (typeof message === 'string' && message.trim()) {
        return message
      }
      if (typeof err.message === 'string' && err.message.trim()) {
        return err.message
      }
    }
    if (err instanceof Error && err.message.trim()) {
      return err.message
    }
    return fallback
  }

  const fetchKeywords = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setKeywords(await api.getMonitorKeywords())
    } catch (err) {
      console.error('Failed to fetch monitor keywords:', err)
      setError(readError(err, 'Failed to load monitor keywords'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchKeywords()
  }, [fetchKeywords])

  const createKeyword = useCallback(async (
    keyword: string,
    triggerNow = true,
    crawlIntervalHours?: number | null,
  ): Promise<{ keywordId: number; executionId: string | null }> => {
    setError(null)
    try {
      const result = await api.createMonitorKeyword({ keyword, enabled: true, triggerNow, crawlIntervalHours })
      setKeywords((prev) => [...prev, result.keyword])
      return { keywordId: result.keyword.id, executionId: result.executionId ? String(result.executionId) : null }
    } catch (err) {
      const message = readError(err, 'Failed to add monitor keyword')
      setError(message)
      throw new Error(message)
    }
  }, [])

  const toggleKeyword = useCallback(async (item: MonitorKeyword) => {
    setError(null)
    try {
      const updated = await api.updateMonitorKeyword(item.id, { enabled: !item.enabled })
      setKeywords((prev) => prev.map((entry) => (entry.id === item.id ? updated : entry)))
    } catch (err) {
      const message = readError(err, 'Failed to update monitor keyword')
      setError(message)
      throw new Error(message)
    }
  }, [])

  const deleteKeyword = useCallback(async (id: number) => {
    setError(null)
    try {
      await api.deleteMonitorKeyword(id)
      setKeywords((prev) => prev.filter((entry) => entry.id !== id))
    } catch (err) {
      const message = readError(err, 'Failed to delete monitor keyword')
      setError(message)
      throw new Error(message)
    }
  }, [])

  const triggerKeyword = useCallback(async (id: number): Promise<string | null> => {
    setError(null)
    try {
      const result = await api.triggerMonitorKeyword(id)
      return result.executionId ? String(result.executionId) : null
    } catch (err) {
      const message = readError(err, 'Failed to trigger keyword crawl')
      setError(message)
      throw new Error(message)
    }
  }, [])

  const triggerAllKeywords = useCallback(async (): Promise<{ keywordId: number; executionId: string | null }[]> => {
    setError(null)
    try {
      const results = await api.triggerAllMonitorKeywords()
      return results.map((r) => ({ keywordId: r.keywordId, executionId: r.executionId ? String(r.executionId) : null }))
    } catch (err) {
      const message = readError(err, 'Failed to trigger all keywords')
      setError(message)
      throw new Error(message)
    }
  }, [])

  const updateInterval = useCallback(async (id: number, hours: number | null): Promise<void> => {
    setError(null)
    try {
      const updated = await api.updateMonitorKeyword(id, { crawlIntervalHours: hours })
      setKeywords((prev) => prev.map((entry) => (entry.id === id ? updated : entry)))
    } catch (err) {
      const message = readError(err, 'Failed to update interval')
      setError(message)
      throw new Error(message)
    }
  }, [])

  return { keywords, loading, error, createKeyword, toggleKeyword, deleteKeyword, triggerKeyword, triggerAllKeywords, updateInterval, refetch: fetchKeywords }
}

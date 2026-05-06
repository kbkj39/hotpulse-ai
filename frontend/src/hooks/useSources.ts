import { useState, useEffect, useCallback } from 'react'
import { api } from '@/services/api'

export interface Source {
  id?: number
  name: string
  type: 'RSS' | 'HTML' | 'API'
  baseUrl: string
  reputationScore: number
  enabled: boolean
}

export function useSources() {
  const [sources, setSources] = useState<Source[]>([])
  const [loading, setLoading] = useState(false)

  const fetchSources = useCallback(async () => {
    setLoading(true)
    try {
      const data = await api.getSources()
      setSources(data as Source[])
    } catch (err) {
      console.error('Failed to fetch sources:', err)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchSources()
  }, [fetchSources])

  const createSource = useCallback(async (source: Source) => {
    await api.upsertSource(source)
    await fetchSources()
  }, [fetchSources])

  const editSource = useCallback(async (id: number, source: Source) => {
    await api.updateSource(id, source)
    await fetchSources()
  }, [fetchSources])

  const setSourceEnabled = useCallback(async (id: number, enabled: boolean) => {
    await api.setSourceEnabled(id, enabled)
    await fetchSources()
  }, [fetchSources])

  const deleteSource = useCallback(async (id: number) => {
    await api.deleteSource(id)
    await fetchSources()
  }, [fetchSources])

  return { sources, loading, createSource, editSource, setSourceEnabled, deleteSource, refetch: fetchSources }
}

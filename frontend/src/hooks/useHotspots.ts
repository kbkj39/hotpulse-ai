import { useEffect, useCallback, useState } from 'react'
import { api } from '@/services/api'
import { useHotspotStore } from '@/store/hotspotStore'

export function useHotspots() {
  const { hotspots, filter, total, setHotspots, setFilter } = useHotspotStore()
  const [loading, setLoading] = useState(false)

  const fetchHotspots = useCallback(async () => {
    setLoading(true)
    try {
      const result = await api.getHotspots(filter)
      setHotspots(result.items, result.total)
    } catch (err) {
      console.error('Failed to fetch hotspots:', err)
    } finally {
      setLoading(false)
    }
  }, [filter, setHotspots])

  useEffect(() => {
    fetchHotspots()
  }, [fetchHotspots])

  return { hotspots, filter, total, setFilter, refetch: fetchHotspots, loading }
}

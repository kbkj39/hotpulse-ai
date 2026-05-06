import { useEffect } from 'react'
import { socketService } from '@/services/socket'
import { useHotspotStore } from '@/store/hotspotStore'

export function useSocket() {
  const addHotspot = useHotspotStore((s) => s.addHotspot)

  useEffect(() => {
    const unsubscribe = socketService.onNewHotspot(addHotspot)
    return () => {
      unsubscribe()
    }
  }, [addHotspot])
}

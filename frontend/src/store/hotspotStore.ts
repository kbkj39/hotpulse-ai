import { create } from 'zustand'
import type { Hotspot, HotspotFilter } from '@/types/hotspot'

interface HotspotState {
  hotspots: Hotspot[]
  filter: HotspotFilter
  total: number
  setHotspots: (hotspots: Hotspot[], total: number) => void
  addHotspot: (hotspot: Hotspot) => void
  setFilter: (filter: Partial<HotspotFilter>) => void
}

export const useHotspotStore = create<HotspotState>((set) => ({
  hotspots: [],
  total: 0,
  filter: {
    sort: 'hot',
    page: 1,
    limit: 20,
  },
  setHotspots: (hotspots, total) => set({ hotspots, total }),
  addHotspot: (hotspot) =>
    set((state) => ({ hotspots: [hotspot, ...state.hotspots] })),
  setFilter: (newFilter) =>
    set((state) => ({ filter: { ...state.filter, ...newFilter } })),
}))

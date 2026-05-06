import type { Hotspot } from '@/types/hotspot'
import { HotspotCard } from './HotspotCard'

interface HotspotListProps {
  hotspots: Hotspot[]
}

export function HotspotList({ hotspots }: HotspotListProps) {
  if (hotspots.length === 0) {
    return (
      <p
        style={{
          textAlign: 'center',
          padding: '64px 0',
          fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
          fontSize: '12px',
          color: '#333',
          letterSpacing: '0.1em',
          textTransform: 'uppercase',
        }}
      >
        — 暂无热点数据 —
      </p>
    )
  }

  return (
    <div style={{ borderBottom: '1px solid #1F1F1F' }}>
      {hotspots.map((h) => (
        <HotspotCard key={h.id} hotspot={h} />
      ))}
    </div>
  )
}

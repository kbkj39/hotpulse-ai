import type { Hotspot } from '@/types/hotspot'

interface PinnedHotspotBarProps {
  hotspot: Hotspot
  onClear: () => void
}

const MONO = "'Fira Code', 'Noto Sans SC', monospace"

function getDisplayTitle(hotspot: Hotspot): string {
  if (hotspot.title && !/^https?:\/\//.test(hotspot.title)) {
    return hotspot.title.length > 48 ? hotspot.title.slice(0, 48) + '…' : hotspot.title
  }
  if (hotspot.summary) {
    return hotspot.summary.length > 48 ? hotspot.summary.slice(0, 48) + '…' : hotspot.summary
  }
  return hotspot.url || '未命名热点'
}

export function PinnedHotspotBar({ hotspot, onClear }: PinnedHotspotBarProps) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
        marginTop: '8px',
        padding: '8px 12px',
        border: '1px solid #2A2A2A',
        background: '#0A0A0A',
      }}
    >
      <span
        style={{
          fontFamily: MONO,
          fontSize: '10px',
          color: '#9CE6A3',
          letterSpacing: '0.08em',
          textTransform: 'uppercase',
          flexShrink: 0,
        }}
      >
        讨论中
      </span>
      <span
        style={{
          flex: 1,
          fontFamily: MONO,
          fontSize: '12px',
          color: '#aaa',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
        }}
        title={getDisplayTitle(hotspot)}
      >
        {getDisplayTitle(hotspot)}
        {hotspot.source ? ` · ${hotspot.source}` : ''}
      </span>
      <button
        type="button"
        onClick={onClear}
        style={{
          border: '1px solid #1F1F1F',
          background: 'transparent',
          color: '#666',
          fontFamily: MONO,
          fontSize: '10px',
          letterSpacing: '0.06em',
          textTransform: 'uppercase',
          padding: '3px 8px',
          cursor: 'pointer',
          flexShrink: 0,
        }}
      >
        清除
      </button>
    </div>
  )
}

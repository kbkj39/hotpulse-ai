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
        padding: '8px 12px',
        border: '1px solid #263A2A',
        background: '#071107',
        borderRadius: '8px',
      }}
    >
      <span
        style={{
          fontFamily: MONO,
          fontSize: '10px',
          color: '#9CE6A3',
          letterSpacing: '0.04em',
          flexShrink: 0,
        }}
      >
        当前热点
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
          color: '#888',
          fontFamily: MONO,
          fontSize: '10px',
          padding: '3px 8px',
          borderRadius: '5px',
          cursor: 'pointer',
          flexShrink: 0,
        }}
      >
        清除
      </button>
    </div>
  )
}

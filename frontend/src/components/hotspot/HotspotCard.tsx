import { useState, type MouseEvent } from 'react'
import type { Hotspot } from '@/types/hotspot'
import { ScoreBadge } from '@/components/common/ScoreBadge'
import { SourceTag } from '@/components/common/SourceTag'

interface HotspotCardProps {
  hotspot: Hotspot
  selectable?: boolean
  selected?: boolean
  onSelect?: (hotspot: Hotspot) => void
}

export function HotspotCard({ hotspot, selectable, selected, onSelect }: HotspotCardProps) {
  const isUrl = (text?: string) => {
    if (!text) return false
    try {
      const u = new URL(text)
      return !!u.protocol && !!u.hostname
    } catch (e) {
      return /https?:\/\//.test(text)
    }
  }

  const getDisplayTitle = () => {
    if (!hotspot.title) return hotspot.url || ''
    if (!isUrl(hotspot.title)) return hotspot.title
    if (hotspot.summary) {
      return hotspot.summary.length > 120 ? hotspot.summary.slice(0, 120) + '…' : hotspot.summary
    }
    try {
      const u = new URL(hotspot.url || hotspot.title)
      return u.hostname
    } catch (e) {
      return hotspot.url || hotspot.title
    }
  }
  return (
    <div
      role={selectable ? 'button' : undefined}
      tabIndex={selectable ? 0 : undefined}
      onClick={selectable ? () => onSelect?.(hotspot) : undefined}
      onKeyDown={
        selectable
          ? (e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault()
                onSelect?.(hotspot)
              }
            }
          : undefined
      }
      style={{
        borderTop: '1px solid #1F1F1F',
        padding: '16px 0',
        transition: 'background 150ms ease, border-color 150ms ease',
        cursor: selectable ? 'pointer' : 'default',
        position: 'relative',
        background: selected ? '#0D120D' : 'transparent',
        outline: selected ? '1px solid #9CE6A3' : 'none',
        outlineOffset: '-1px',
      }}
      onMouseEnter={(e) => {
        if (!selected) e.currentTarget.style.background = '#0A0A0A'
      }}
      onMouseLeave={(e) => {
        if (!selected) e.currentTarget.style.background = 'transparent'
      }}
    >
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '16px' }}>
        <div style={{ flex: 1 }}>
          <div
            style={{
              fontFamily: "'Fira Sans', 'Noto Sans SC', sans-serif",
              fontWeight: 600,
              fontSize: '17px',
              color: '#fff',
              lineHeight: 1.3,
              cursor: 'default',
            }}
          >
            {getDisplayTitle()}
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexShrink: 0 }}>
          <span
            style={{
              fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
              fontSize: '12px',
              color: '#444',
              whiteSpace: 'nowrap',
            }}
          >
            {hotspot.publishedAt ? new Date(hotspot.publishedAt).toLocaleDateString('zh-CN') : ''}
          </span>

          {/* link button with hover tooltip */}
          <LinkButton
            url={hotspot.url}
            onNavigate={(e) => e.stopPropagation()}
          />
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '8px', flexWrap: 'wrap' }}>
        <SourceTag source={hotspot.source} />
        {hotspot.tags?.map((tag) => (
          <span
            key={tag}
            style={{
              border: '1px solid #1F1F1F',
              padding: '1px 6px',
              fontSize: '11px',
              fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
              color: '#555',
              letterSpacing: '0.02em',
            }}
          >
            #{tag}
          </span>
        ))}
      </div>

      {hotspot.summary && (
        <p
          style={{
            fontFamily: "'Fira Sans', 'Noto Sans SC', sans-serif",
            fontSize: '13px',
            color: '#666',
            marginTop: '8px',
            lineHeight: 1.6,
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical',
            overflow: 'hidden',
          }}
        >
          {hotspot.summary}
        </p>
      )}

      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '10px', flexWrap: 'wrap' }}>
        <ScoreBadge label="真实" value={hotspot.truthScore} />
        <ScoreBadge label="相关" value={hotspot.relevanceScore} />
        <ScoreBadge label="重要" value={hotspot.importanceScore} />
        <span
          style={{
            marginLeft: 'auto',
            fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
            fontSize: '12px',
            color: '#444',
            letterSpacing: '0.05em',
          }}
        >
          HOT {(hotspot.hotScore * 100).toFixed(0)}
        </span>
      </div>
    </div>
  )
}

function LinkButton({ url, onNavigate }: { url?: string; onNavigate?: (e: MouseEvent) => void }) {
  const [hover, setHover] = useState(false)
  if (!url) return null
  return (
    <div style={{ position: 'relative', display: 'inline-block' }}>
      <button
        aria-label="打开原文"
        title="打开原文"
        onClick={(e) => {
          onNavigate?.(e)
          window.open(url, '_blank')
        }}
        onMouseEnter={() => setHover(true)}
        onMouseLeave={() => setHover(false)}
        style={{
          width: 28,
          height: 28,
          borderRadius: 6,
          border: '1px solid #1F1F1F',
          background: 'transparent',
          color: '#9CE6A3',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          cursor: 'pointer',
        }}
      >
        ↗
      </button>
      {hover && (
        <div
          style={{
            position: 'absolute',
            right: 36,
            top: -6,
            background: '#111',
            border: '1px solid #222',
            padding: '6px 8px',
            fontSize: 11,
            color: '#ccc',
            maxWidth: 320,
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            zIndex: 40,
            borderRadius: 4,
          }}
        >
          {url}
        </div>
      )}
    </div>
  )
}

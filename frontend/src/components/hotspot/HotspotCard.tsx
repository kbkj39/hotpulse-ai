import { useState, type MouseEvent } from 'react'
import type { Hotspot } from '@/types/hotspot'
import { ScoreBadge } from '@/components/common/ScoreBadge'
import { SourceTag } from '@/components/common/SourceTag'

interface HotspotCardProps {
  hotspot: Hotspot
  selectable?: boolean
  selected?: boolean
  compact?: boolean
  onSelect?: (hotspot: Hotspot) => void
}

export function HotspotCard({ hotspot, selectable, selected, compact, onSelect }: HotspotCardProps) {
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
        padding: compact ? '13px 0' : '18px 0',
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
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: compact ? '10px' : '18px' }}>
        <div style={{ flex: 1 }}>
          <div
            style={{
              fontFamily: "'Fira Sans', 'Noto Sans SC', sans-serif",
              fontWeight: 600,
              fontSize: compact ? '14px' : '18px',
              color: '#fff',
              lineHeight: 1.35,
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
              color: '#666',
              whiteSpace: 'nowrap',
            }}
          >
            {hotspot.publishedAt ? new Date(hotspot.publishedAt).toLocaleDateString('zh-CN') : ''}
          </span>

          {/* link button with hover tooltip */}
          {!compact && (
            <LinkButton
              url={hotspot.url}
              onNavigate={(e) => e.stopPropagation()}
            />
          )}
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '10px', flexWrap: 'wrap' }}>
        <SourceTag source={hotspot.source} />
        {hotspot.monitorKeyword && (
          <span
            title="监控关键词"
            style={{
              border: '1px solid #3A3A3A',
              background: '#080808',
              padding: '2px 8px',
              fontSize: '12px',
              fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
              color: '#E6E6E6',
              letterSpacing: '0.02em',
              borderRadius: '4px',
            }}
          >
            ▶ {hotspot.monitorKeyword}
          </span>
        )}
        {!compact && (hotspot.tags && hotspot.tags.length > 0 ? hotspot.tags.map((tag) => (
          <span
            key={tag}
            style={{
              border: '1px solid #304736',
              background: '#101810',
              padding: '2px 7px',
              fontSize: '11px',
              fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
              color: '#9CE6A3',
              letterSpacing: '0.02em',
              borderRadius: '4px',
            }}
          >
            #{tag}
          </span>
        )) : (
          <span
            style={{
              border: '1px dashed #2A2A2A',
              padding: '2px 7px',
              fontSize: '11px',
              fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
              color: '#555',
              letterSpacing: '0.02em',
              borderRadius: '4px',
            }}
          >
            NO TAGS
          </span>
        ))}
      </div>

      {!compact && hotspot.summary && (
        <p
          style={{
            fontFamily: "'Fira Sans', 'Noto Sans SC', sans-serif",
            fontSize: '14px',
            color: '#8A8A8A',
            marginTop: '10px',
            marginBottom: 0,
            lineHeight: 1.7,
            display: '-webkit-box',
            WebkitLineClamp: 3,
            WebkitBoxOrient: 'vertical',
            overflow: 'hidden',
          }}
        >
          {hotspot.summary}
        </p>
      )}

      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: compact ? '9px' : '12px', flexWrap: 'wrap' }}>
        {!compact && <ScoreBadge label="真实" value={hotspot.truthScore} />}
        {!compact && <ScoreBadge label="相关" value={hotspot.relevanceScore} />}
        {!compact && <ScoreBadge label="重要" value={hotspot.importanceScore} />}
        {compact && hotspot.url && (
          <button
            aria-label="打开原文"
            title="打开原文"
            onClick={(e) => {
              e.stopPropagation()
              window.open(hotspot.url, '_blank')
            }}
            style={{
              border: '1px solid #242424',
              background: 'transparent',
              color: '#888',
              fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
              fontSize: '11px',
              padding: '2px 7px',
              borderRadius: '4px',
              cursor: 'pointer',
            }}
          >
            原文
          </button>
        )}
        <span
          style={{
            marginLeft: 'auto',
            fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
            fontSize: '12px',
            color: '#9CE6A3',
            letterSpacing: '0.05em',
          }}
        >
          热度 {(hotspot.hotScore * 100).toFixed(0)}
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

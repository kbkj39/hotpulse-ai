import type { HotspotFilter } from '@/types/hotspot'

interface HotspotFilterProps {
  filter: HotspotFilter
  onChange: (filter: Partial<HotspotFilter>) => void
}

const SORT_OPTIONS: { value: HotspotFilter['sort']; label: string }[] = [
  { value: 'hot', label: '热度' },
  { value: 'importance', label: '重要' },
  { value: 'relevance', label: '相关' },
  { value: 'time', label: '最新' },
]

export function HotspotFilterBar({ filter, onChange }: HotspotFilterProps) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        flexWrap: 'wrap',
        marginBottom: '8px',
        padding: '10px 0 12px',
        borderBottom: '1px solid #1F1F1F',
        borderTop: '1px solid #151515',
      }}
    >
      <span
        style={{
          fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
          fontSize: '12px',
          color: '#444',
          letterSpacing: '0.08em',
          textTransform: 'uppercase',
          marginRight: '4px',
        }}
      >
        排序
      </span>
      {SORT_OPTIONS.map((opt) => {
        const isActive = filter.sort === opt.value
        return (
          <button
            key={opt.value}
            type="button"
            onClick={() => onChange({ sort: opt.value, page: 1 })}
            style={{
              padding: '3px 10px',
              border: `1px solid ${isActive ? '#E6E6E6' : '#1F1F1F'}`,
              background: isActive ? '#E6E6E6' : 'transparent',
              color: isActive ? '#050505' : '#777',
              fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
              fontSize: '12px',
              letterSpacing: '0',
              borderRadius: '5px',
              cursor: 'pointer',
              transition: 'all 150ms ease',
            }}
          >
            {opt.label}
          </button>
        )
      })}

      <input
        type="text"
        placeholder="标签"
        value={filter.tag ?? ''}
        onChange={(e) => onChange({ tag: e.target.value || undefined, page: 1 })}
        style={{
          marginLeft: '8px',
          padding: '3px 10px',
          border: '1px solid #1F1F1F',
          background: 'transparent',
          color: '#888',
          fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
          fontSize: '12px',
          letterSpacing: '0',
          outline: 'none',
          width: '120px',
          borderRadius: '5px',
          transition: 'border-color 150ms ease',
        }}
        onFocus={(e) => (e.currentTarget.style.borderColor = '#fff')}
        onBlur={(e) => (e.currentTarget.style.borderColor = '#1F1F1F')}
      />

      <input
        type="text"
        placeholder="全文关键词"
        value={filter.keyword ?? ''}
        onChange={(e) => onChange({ keyword: e.target.value || undefined, page: 1 })}
        style={{
          padding: '3px 10px',
          border: '1px solid #1F1F1F',
          background: 'transparent',
          color: '#888',
          fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
          fontSize: '12px',
          letterSpacing: '0',
          outline: 'none',
          width: '150px',
          borderRadius: '5px',
          transition: 'border-color 150ms ease',
        }}
        onFocus={(e) => (e.currentTarget.style.borderColor = '#fff')}
        onBlur={(e) => (e.currentTarget.style.borderColor = '#1F1F1F')}
      />
    </div>
  )
}

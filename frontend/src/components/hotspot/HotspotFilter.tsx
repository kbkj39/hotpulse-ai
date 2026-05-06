import type { HotspotFilter } from '@/types/hotspot'

interface HotspotFilterProps {
  filter: HotspotFilter
  onChange: (filter: Partial<HotspotFilter>) => void
}

const SORT_OPTIONS: { value: HotspotFilter['sort']; label: string }[] = [
  { value: 'hot', label: 'HOT' },
  { value: 'importance', label: 'IMP' },
  { value: 'relevance', label: 'REL' },
  { value: 'time', label: 'TIME' },
]

export function HotspotFilterBar({ filter, onChange }: HotspotFilterProps) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        flexWrap: 'wrap',
        marginBottom: '4px',
        paddingBottom: '12px',
        borderBottom: '1px solid #1F1F1F',
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
        SORT
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
              border: `1px solid ${isActive ? '#fff' : '#1F1F1F'}`,
              background: isActive ? '#fff' : 'transparent',
              color: isActive ? '#000' : '#555',
              fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
              fontSize: '12px',
              letterSpacing: '0.08em',
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
        placeholder="# TAG"
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
          letterSpacing: '0.05em',
          outline: 'none',
          width: '120px',
          transition: 'border-color 150ms ease',
        }}
        onFocus={(e) => (e.currentTarget.style.borderColor = '#fff')}
        onBlur={(e) => (e.currentTarget.style.borderColor = '#1F1F1F')}
      />

      <input
        type="text"
        placeholder="KEYWORD"
        value={filter.keyword ?? ''}
        onChange={(e) => onChange({ keyword: e.target.value || undefined, page: 1 })}
        style={{
          padding: '3px 10px',
          border: '1px solid #1F1F1F',
          background: 'transparent',
          color: '#888',
          fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
          fontSize: '12px',
          letterSpacing: '0.05em',
          outline: 'none',
          width: '140px',
          transition: 'border-color 150ms ease',
        }}
        onFocus={(e) => (e.currentTarget.style.borderColor = '#fff')}
        onBlur={(e) => (e.currentTarget.style.borderColor = '#1F1F1F')}
      />
    </div>
  )
}

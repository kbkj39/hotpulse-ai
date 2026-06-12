import { useMemo, useState } from 'react'
import type { CSSProperties, FormEvent } from 'react'
import { useSources } from '@/hooks/useSources'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import type { Source } from '@/hooks/useSources'

const EMPTY: Source = { name: '', type: 'RSS', baseUrl: '', reputationScore: 0.5, enabled: true }
const MONO = "'Fira Code', 'Noto Sans SC', monospace"
const SANS = "'Fira Sans', 'Noto Sans SC', sans-serif"
const TYPES: Array<'ALL' | Source['type']> = ['ALL', 'RSS', 'HTML', 'API']
const STATUSES: Array<'ALL' | 'ON' | 'OFF'> = ['ALL', 'ON', 'OFF']

const inputStyle: CSSProperties = {
  border: '1px solid #1F1F1F',
  background: 'transparent',
  color: '#fff',
  fontFamily: SANS,
  fontSize: '13px',
  padding: '8px 10px',
  outline: 'none',
  width: '100%',
  borderRadius: '5px',
  transition: 'border-color 150ms ease',
}

const labelStyle: CSSProperties = {
  fontFamily: MONO,
  fontSize: '11px',
  color: '#555',
  letterSpacing: '0.06em',
  textTransform: 'uppercase',
}

function fieldLabel(text: string) {
  return <div style={labelStyle}>{text}</div>
}

function focusBorder(e: React.FocusEvent<HTMLInputElement>) {
  e.currentTarget.style.borderColor = '#E6E6E6'
}

function blurBorder(e: React.FocusEvent<HTMLInputElement>) {
  e.currentTarget.style.borderColor = '#1F1F1F'
}

function SegmentButton({
  active,
  children,
  onClick,
}: {
  active: boolean
  children: React.ReactNode
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      style={{
        border: `1px solid ${active ? '#E6E6E6' : '#1F1F1F'}`,
        background: active ? '#E6E6E6' : 'transparent',
        color: active ? '#050505' : '#777',
        fontFamily: MONO,
        fontSize: '11px',
        padding: '6px 10px',
        borderRadius: '5px',
        cursor: 'pointer',
        minWidth: '52px',
      }}
    >
      {children}
    </button>
  )
}

export function SettingsPage() {
  const { sources, loading, createSource, editSource, setSourceEnabled, deleteSource } = useSources()
  const [form, setForm] = useState<Source>(EMPTY)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [query, setQuery] = useState('')
  const [typeFilter, setTypeFilter] = useState<'ALL' | Source['type']>('ALL')
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ON' | 'OFF'>('ALL')
  const [submitting, setSubmitting] = useState(false)
  const [busySourceId, setBusySourceId] = useState<number | null>(null)

  const enabledCount = sources.filter((source) => source.enabled).length
  const disabledCount = sources.length - enabledCount

  const filteredSources = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()
    return sources.filter((source) => {
      const matchesType = typeFilter === 'ALL' || source.type === typeFilter
      const matchesStatus =
        statusFilter === 'ALL' ||
        (statusFilter === 'ON' && source.enabled) ||
        (statusFilter === 'OFF' && !source.enabled)
      const matchesQuery =
        !normalizedQuery ||
        source.name.toLowerCase().includes(normalizedQuery) ||
        source.baseUrl.toLowerCase().includes(normalizedQuery) ||
        source.type.toLowerCase().includes(normalizedQuery)
      return matchesType && matchesStatus && matchesQuery
    })
  }, [query, sources, statusFilter, typeFilter])

  const resetForm = () => {
    setEditingId(null)
    setForm(EMPTY)
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setSubmitting(true)
    try {
      if (editingId != null) {
        await editSource(editingId, form)
      } else {
        await createSource(form)
      }
      resetForm()
    } finally {
      setSubmitting(false)
    }
  }

  const handleEdit = (source: Source) => {
    setEditingId(source.id ?? null)
    setForm({ ...source })
  }

  const handleToggle = async (source: Source) => {
    if (!source.id) return
    setBusySourceId(source.id)
    try {
      await setSourceEnabled(source.id, !source.enabled)
    } finally {
      setBusySourceId(null)
    }
  }

  const handleDelete = async (source: Source) => {
    if (!source.id || !confirm('确定要删除此信息源吗？')) return
    setBusySourceId(source.id)
    try {
      await deleteSource(source.id)
      if (editingId === source.id) resetForm()
    } finally {
      setBusySourceId(null)
    }
  }

  return (
    <div style={{ height: 'calc(100vh - 96px)', minHeight: 0, overflow: 'hidden' }}>
      <div
        style={{
          display: 'flex',
          gap: '18px',
          height: '100%',
          minHeight: 0,
          overflow: 'hidden',
          flexWrap: 'wrap',
        }}
      >
        <aside
          style={{
            flex: '0 0 360px',
            maxWidth: '100%',
            height: '100%',
            minHeight: 0,
            border: '1px solid #202020',
            borderRadius: '8px',
            background: '#050505',
            padding: '16px',
            overflow: 'hidden',
          }}
        >
          <form onSubmit={handleSubmit} style={{ display: 'flex', height: '100%', flexDirection: 'column', gap: '14px' }}>
            <div style={{ borderBottom: '1px solid #151515', paddingBottom: '12px' }}>
              <div style={{ fontFamily: MONO, color: '#fff', fontSize: '16px', fontWeight: 600 }}>
                {editingId != null ? '编辑信息源' : '新增信息源'}
              </div>
              <div style={{ marginTop: '6px', fontFamily: SANS, color: '#666', fontSize: '12px' }}>
                {editingId != null ? `ID ${editingId}` : `${sources.length} 个信息源`}
              </div>
            </div>

            <label style={{ display: 'grid', gap: '6px' }}>
              {fieldLabel('名称')}
              <input
                required
                placeholder="例如 36氪"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                style={inputStyle}
                onFocus={focusBorder}
                onBlur={blurBorder}
              />
            </label>

            <div style={{ display: 'grid', gap: '8px' }}>
              {fieldLabel('类型')}
              <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                {(['RSS', 'HTML', 'API'] as Source['type'][]).map((type) => (
                  <SegmentButton
                    key={type}
                    active={form.type === type}
                    onClick={() => setForm({ ...form, type })}
                  >
                    {type}
                  </SegmentButton>
                ))}
              </div>
            </div>

            <label style={{ display: 'grid', gap: '6px' }}>
              {fieldLabel('URL')}
              <input
                required
                placeholder="https://example.com/feed"
                value={form.baseUrl}
                onChange={(e) => setForm({ ...form, baseUrl: e.target.value })}
                style={inputStyle}
                onFocus={focusBorder}
                onBlur={blurBorder}
              />
            </label>

            <div style={{ display: 'grid', gap: '8px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: '12px' }}>
                {fieldLabel('信誉分')}
                <span style={{ fontFamily: MONO, fontSize: '11px', color: '#888' }}>
                  {form.reputationScore.toFixed(2)}
                </span>
              </div>
              <input
                type="range"
                min={0}
                max={1}
                step={0.05}
                value={form.reputationScore}
                onChange={(e) => setForm({ ...form, reputationScore: Number(e.target.value) })}
                style={{ width: '100%', accentColor: '#E6E6E6' }}
              />
            </div>

            <button
              type="button"
              onClick={() => setForm({ ...form, enabled: !form.enabled })}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: '12px',
                border: '1px solid #1F1F1F',
                background: form.enabled ? '#081108' : 'transparent',
                color: form.enabled ? '#9CE6A3' : '#666',
                fontFamily: MONO,
                fontSize: '12px',
                padding: '8px 10px',
                borderRadius: '5px',
                cursor: 'pointer',
              }}
            >
              <span>状态</span>
              <span>{form.enabled ? '启用' : '停用'}</span>
            </button>

            <div style={{ display: 'flex', gap: '8px', marginTop: 'auto', paddingTop: '10px' }}>
              <button
                type="submit"
                disabled={submitting}
                style={{
                  flex: '1 1 auto',
                  border: '1px solid #E6E6E6',
                  background: '#E6E6E6',
                  color: '#050505',
                  fontFamily: MONO,
                  fontSize: '12px',
                  padding: '9px 14px',
                  borderRadius: '5px',
                  cursor: submitting ? 'wait' : 'pointer',
                  opacity: submitting ? 0.7 : 1,
                }}
              >
                {submitting ? '保存中...' : editingId != null ? '保存' : '添加'}
              </button>
              {editingId != null && (
                <button
                  type="button"
                  onClick={resetForm}
                  disabled={submitting}
                  style={{
                    border: '1px solid #1F1F1F',
                    background: 'transparent',
                    color: '#777',
                    fontFamily: MONO,
                    fontSize: '12px',
                    padding: '9px 14px',
                    borderRadius: '5px',
                    cursor: submitting ? 'wait' : 'pointer',
                  }}
                >
                  取消
                </button>
              )}
            </div>
          </form>
        </aside>

        <section
          style={{
            display: 'flex',
            flex: '1 1 720px',
            minWidth: 0,
            height: '100%',
            minHeight: 0,
            flexDirection: 'column',
            border: '1px solid #202020',
            borderRadius: '8px',
            background: '#050505',
            overflow: 'hidden',
          }}
        >
          <div style={{ padding: '14px 16px', borderBottom: '1px solid #151515' }}>
            <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: '16px', flexWrap: 'wrap' }}>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: '10px', flexWrap: 'wrap' }}>
                <h1 style={{ margin: 0, fontFamily: MONO, fontSize: '16px', fontWeight: 600, color: '#fff' }}>
                  信息源
                </h1>
                <span style={{ fontFamily: SANS, color: '#666', fontSize: '12px' }}>
                  {enabledCount} 启用 / {disabledCount} 停用
                </span>
              </div>
              <span style={{ fontFamily: MONO, fontSize: '12px', color: '#888' }}>
                {filteredSources.length} / {sources.length}
              </span>
            </div>

            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', marginTop: '14px' }}>
              <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="搜索名称或 URL"
                style={{ ...inputStyle, width: 'min(100%, 260px)' }}
                onFocus={focusBorder}
                onBlur={blurBorder}
              />
              <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                {TYPES.map((type) => (
                  <SegmentButton key={type} active={typeFilter === type} onClick={() => setTypeFilter(type)}>
                    {type === 'ALL' ? '全部' : type}
                  </SegmentButton>
                ))}
              </div>
              <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                {STATUSES.map((status) => (
                  <SegmentButton key={status} active={statusFilter === status} onClick={() => setStatusFilter(status)}>
                    {status === 'ALL' ? '不限' : status}
                  </SegmentButton>
                ))}
              </div>
            </div>
          </div>

          <div style={{ flex: 1, minHeight: 0, overflowY: 'auto' }}>
            {loading ? (
              <LoadingSpinner />
            ) : filteredSources.length === 0 ? (
              <div style={{ padding: '48px 16px', textAlign: 'center', fontFamily: MONO, fontSize: '12px', color: '#444' }}>
                暂无匹配信息源
              </div>
            ) : (
              <table style={{ width: '100%', borderCollapse: 'collapse', fontFamily: MONO, fontSize: '12px' }}>
                <thead style={{ position: 'sticky', top: 0, background: '#050505', zIndex: 1 }}>
                  <tr style={{ borderBottom: '1px solid #1F1F1F' }}>
                    {['名称', '类型', 'URL', '信誉', '状态', '操作'].map((heading) => (
                      <th
                        key={heading}
                        style={{
                          textAlign: 'left',
                          padding: '9px 10px',
                          color: '#555',
                          fontWeight: 400,
                          letterSpacing: '0.06em',
                          textTransform: 'uppercase',
                          whiteSpace: 'nowrap',
                        }}
                      >
                        {heading}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {filteredSources.map((source) => {
                    const busy = busySourceId === source.id
                    return (
                      <tr
                        key={source.id}
                        style={{
                          borderBottom: '1px solid #111',
                          background: editingId === source.id ? '#080C08' : 'transparent',
                        }}
                      >
                        <td style={{ padding: '11px 10px', color: '#fff', minWidth: '120px' }}>{source.name}</td>
                        <td style={{ padding: '11px 10px', color: '#888' }}>{source.type}</td>
                        <td style={{ padding: '11px 10px', maxWidth: '360px' }}>
                          <a
                            href={source.baseUrl}
                            target="_blank"
                            rel="noreferrer"
                            title={source.baseUrl}
                            style={{
                              display: 'block',
                              color: '#777',
                              textDecoration: 'none',
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              whiteSpace: 'nowrap',
                            }}
                          >
                            {source.baseUrl}
                          </a>
                        </td>
                        <td style={{ padding: '11px 10px', color: '#888', whiteSpace: 'nowrap' }}>
                          {source.reputationScore.toFixed(2)}
                        </td>
                        <td style={{ padding: '11px 10px' }}>
                          <button
                            type="button"
                            disabled={busy}
                            onClick={() => void handleToggle(source)}
                            style={{
                              border: '1px solid #1F1F1F',
                              background: source.enabled ? '#081108' : 'transparent',
                              color: source.enabled ? '#9CE6A3' : '#666',
                              fontFamily: MONO,
                              fontSize: '11px',
                              padding: '4px 8px',
                              borderRadius: '5px',
                              cursor: busy ? 'wait' : 'pointer',
                              minWidth: '48px',
                            }}
                          >
                            {source.enabled ? 'ON' : 'OFF'}
                          </button>
                        </td>
                        <td style={{ padding: '11px 10px' }}>
                          <div style={{ display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
                            <button
                              type="button"
                              onClick={() => handleEdit(source)}
                              style={{
                                border: 'none',
                                background: 'transparent',
                                color: '#888',
                                fontFamily: MONO,
                                fontSize: '11px',
                                padding: 0,
                                cursor: 'pointer',
                              }}
                            >
                              编辑
                            </button>
                            <button
                              type="button"
                              disabled={busy}
                              onClick={() => void handleDelete(source)}
                              style={{
                                border: 'none',
                                background: 'transparent',
                                color: '#B45A5A',
                                fontFamily: MONO,
                                fontSize: '11px',
                                padding: 0,
                                cursor: busy ? 'wait' : 'pointer',
                              }}
                            >
                              删除
                            </button>
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            )}
          </div>
        </section>
      </div>
    </div>
  )
}

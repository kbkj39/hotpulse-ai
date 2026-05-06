import { useState } from 'react'
import { useSources } from '@/hooks/useSources'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import type { Source } from '@/hooks/useSources'

const EMPTY: Source = { name: '', type: 'RSS', baseUrl: '', reputationScore: 0.5, enabled: true }

const inputStyle: React.CSSProperties = {
  border: '1px solid #1F1F1F',
  background: 'transparent',
  color: '#fff',
  fontFamily: "'Fira Sans', 'Noto Sans SC', sans-serif",
  fontSize: '13px',
  padding: '6px 10px',
  outline: 'none',
  width: '100%',
  transition: 'border-color 150ms ease',
}

export function SettingsPage() {
  const { sources, loading, createSource, editSource, setSourceEnabled, deleteSource } = useSources()
  const [form, setForm] = useState<Source>(EMPTY)
  const [editingId, setEditingId] = useState<number | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (editingId != null) {
      await editSource(editingId, form)
      setEditingId(null)
    } else {
      await createSource(form)
    }
    setForm(EMPTY)
  }

  const focusBorder = (e: React.FocusEvent<HTMLInputElement | HTMLSelectElement>) =>
    (e.currentTarget.style.borderColor = '#2E2E2E')
  const blurBorder = (e: React.FocusEvent<HTMLInputElement | HTMLSelectElement>) =>
    (e.currentTarget.style.borderColor = '#1F1F1F')

  return (
    <div style={{ maxWidth: '720px' }}>
      <h1
        style={{
          fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
          fontWeight: 600,
          fontSize: '18px',
          letterSpacing: '-0.02em',
          color: '#fff',
          margin: '0 0 24px 0',
          paddingBottom: '16px',
          borderBottom: '1px solid #1F1F1F',
        }}
      >
        信息源管理
      </h1>

      <form
        onSubmit={handleSubmit}
        style={{
          border: '1px solid #1F1F1F',
          padding: '20px',
          marginBottom: '24px',
          display: 'flex',
          flexDirection: 'column',
          gap: '10px',
        }}
      >
        <p
          style={{
            fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
            fontSize: '11px',
            color: '#444',
            letterSpacing: '0.08em',
            textTransform: 'uppercase',
            margin: '0 0 4px 0',
          }}
        >
          {editingId != null ? 'EDIT SOURCE' : 'NEW SOURCE'}
        </p>
        <input
          required
          placeholder="名称"
          value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })}
          style={inputStyle}
          onFocus={focusBorder}
          onBlur={blurBorder}
        />
        <select
          value={form.type}
          onChange={(e) => setForm({ ...form, type: e.target.value as Source['type'] })}
          style={{
            ...inputStyle,
            cursor: 'pointer',
          }}
          onFocus={focusBorder}
          onBlur={blurBorder}
        >
          <option value="RSS" style={{ background: '#111' }}>RSS</option>
          <option value="HTML" style={{ background: '#111' }}>HTML</option>
          <option value="API" style={{ background: '#111' }}>API</option>
        </select>
        <input
          required
          placeholder="URL"
          value={form.baseUrl}
          onChange={(e) => setForm({ ...form, baseUrl: e.target.value })}
          style={inputStyle}
          onFocus={focusBorder}
          onBlur={blurBorder}
        />
        <label
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
            fontSize: '12px',
            color: '#555',
            cursor: 'pointer',
            letterSpacing: '0.05em',
          }}
        >
          <input
            type="checkbox"
            checked={form.enabled}
            onChange={(e) => setForm({ ...form, enabled: e.target.checked })}
            style={{ accentColor: '#fff', cursor: 'pointer' }}
          />
          ENABLED
        </label>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            type="submit"
            style={{
              border: '1px solid #fff',
              background: '#fff',
              color: '#000',
              fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
              fontSize: '11px',
              letterSpacing: '0.08em',
              textTransform: 'uppercase',
              padding: '6px 16px',
              cursor: 'pointer',
              transition: 'all 150ms ease',
            }}
            onMouseEnter={(e) => { e.currentTarget.style.background = '#ddd' }}
            onMouseLeave={(e) => { e.currentTarget.style.background = '#fff' }}
          >
            {editingId != null ? 'SAVE' : 'ADD'}
          </button>
          {editingId != null && (
            <button
              type="button"
              onClick={() => { setEditingId(null); setForm(EMPTY) }}
              style={{
                border: '1px solid #1F1F1F',
                background: 'transparent',
                color: '#555',
                fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
                fontSize: '11px',
                letterSpacing: '0.08em',
                textTransform: 'uppercase',
                padding: '6px 16px',
                cursor: 'pointer',
                transition: 'all 150ms ease',
              }}
              onMouseEnter={(e) => { e.currentTarget.style.borderColor = '#fff'; e.currentTarget.style.color = '#fff' }}
              onMouseLeave={(e) => { e.currentTarget.style.borderColor = '#1F1F1F'; e.currentTarget.style.color = '#555' }}
            >
              CANCEL
            </button>
          )}
        </div>
      </form>

      {loading ? (
        <LoadingSpinner />
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse', fontFamily: "'Fira Code', 'Noto Sans SC', monospace", fontSize: '12px' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid #1F1F1F' }}>
              {['名称', '类型', 'URL', '状态', '操作'].map((h) => (
                <th
                  key={h}
                  style={{
                    textAlign: 'left',
                    padding: '8px 10px',
                    color: '#444',
                    fontWeight: 400,
                    letterSpacing: '0.08em',
                    textTransform: 'uppercase',
                    fontSize: '11px',
                  }}
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {sources.map((s) => (
              <tr
                key={s.id}
                style={{ borderBottom: '1px solid #111', transition: 'background 150ms ease' }}
                onMouseEnter={(e) => (e.currentTarget.style.background = '#0A0A0A')}
                onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
              >
                <td style={{ padding: '10px', color: '#fff' }}>{s.name}</td>
                <td style={{ padding: '10px', color: '#555' }}>{s.type}</td>
                <td style={{ padding: '10px', color: '#444', maxWidth: '240px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{s.baseUrl}</td>
                <td style={{ padding: '10px' }}>
                  <span style={{ color: s.enabled ? '#fff' : '#333', letterSpacing: '0.05em' }}>
                    {s.enabled ? 'ON' : 'OFF'}
                  </span>
                </td>
                <td style={{ padding: '10px' }}>
                  <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                    <button
                      onClick={() => { setEditingId(s.id!); setForm(s) }}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: '#444',
                      fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
                      fontSize: '11px',
                      letterSpacing: '0.05em',
                      textTransform: 'uppercase',
                      cursor: 'pointer',
                      padding: '0',
                      transition: 'color 150ms ease',
                    }}
                    onMouseEnter={(e) => (e.currentTarget.style.color = '#fff')}
                    onMouseLeave={(e) => (e.currentTarget.style.color = '#444')}
                  >
                    EDIT
                  </button>
                    <button
                      onClick={async () => { await setSourceEnabled(s.id!, !s.enabled) }}
                      style={{
                        background: 'none',
                        border: 'none',
                        color: s.enabled ? '#fff' : '#888',
                        fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
                        fontSize: '11px',
                        letterSpacing: '0.05em',
                        textTransform: 'uppercase',
                        cursor: 'pointer',
                        padding: '0',
                        transition: 'color 150ms ease',
                      }}
                      onMouseEnter={(e) => (e.currentTarget.style.color = '#fff')}
                      onMouseLeave={(e) => (e.currentTarget.style.color = s.enabled ? '#fff' : '#888')}
                    >
                      {s.enabled ? 'DISABLE' : 'ENABLE'}
                    </button>
                    <button
                      onClick={async () => { if (confirm('确定要删除此信息源吗？')) await deleteSource(s.id!) }}
                      style={{
                        background: 'none',
                        border: 'none',
                        color: '#c55',
                        fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
                        fontSize: '11px',
                        letterSpacing: '0.05em',
                        textTransform: 'uppercase',
                        cursor: 'pointer',
                        padding: '0',
                        transition: 'color 150ms ease',
                      }}
                      onMouseEnter={(e) => (e.currentTarget.style.color = '#f55')}
                      onMouseLeave={(e) => (e.currentTarget.style.color = '#c55')}
                    >
                      DELETE
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}

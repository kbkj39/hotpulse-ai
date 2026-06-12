import { useState, type CSSProperties, type MouseEvent } from 'react'
import { useReport, type DailyReportStatus } from '@/hooks/useReport'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'

const STATUS_LABEL: Record<DailyReportStatus, string> = {
  PENDING: '等待生成',
  GENERATING: '生成中',
  READY: '已就绪',
  EMPTY: '无热点',
  FAILED: '失败',
}

const STATUS_COLOR: Record<DailyReportStatus, string> = {
  PENDING: '#888',
  GENERATING: '#60a5fa',
  READY: '#4ade80',
  EMPTY: '#666',
  FAILED: '#f87171',
}

const buttonStyle: CSSProperties = {
  border: '1px solid #1F1F1F',
  background: 'transparent',
  color: '#555',
  fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
  fontSize: '11px',
  letterSpacing: '0.08em',
  textTransform: 'uppercase',
  padding: '5px 12px',
  borderRadius: '5px',
  cursor: 'pointer',
  transition: 'all 150ms ease',
}

function hoverButton(e: MouseEvent<HTMLButtonElement>, active: boolean) {
  if (active) return
  e.currentTarget.style.borderColor = '#fff'
  e.currentTarget.style.color = '#fff'
}

function unhoverButton(e: MouseEvent<HTMLButtonElement>, active: boolean) {
  if (active) return
  e.currentTarget.style.borderColor = '#1F1F1F'
  e.currentTarget.style.color = '#555'
}

export function DailyReportPage() {
  const [date, setDate] = useState<string>('')
  const { report, loading, regenerating, error, refetch, regenerate } = useReport(date || undefined)

  const targetDate = date || report?.reportDate
  const isBusy = loading || regenerating
  const canRegenerate =
    !!targetDate && report?.status !== 'GENERATING' && !regenerating

  const handleRegenerate = () => {
    if (!targetDate) return
    regenerate(targetDate)
  }

  return (
    <div style={{ maxWidth: '720px' }}>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '16px',
          marginBottom: '24px',
          paddingBottom: '16px',
          borderBottom: '2px solid #1F1F1F',
          flexWrap: 'wrap',
        }}
      >
        <h1
          style={{
            fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
            fontWeight: 600,
            fontSize: '20px',
            letterSpacing: '-0.02em',
            color: '#fff',
            margin: 0,
          }}
        >
          每日日报
        </h1>
        <input
          type="date"
          value={date}
          onChange={(e) => setDate(e.target.value)}
          style={{
            border: '1px solid #1F1F1F',
            background: 'transparent',
            color: '#888',
            fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
            fontSize: '13px',
            padding: '4px 8px',
            outline: 'none',
            cursor: 'pointer',
          }}
          onFocus={(e) => (e.currentTarget.style.borderColor = '#2E2E2E')}
          onBlur={(e) => (e.currentTarget.style.borderColor = '#1F1F1F')}
        />
        <button
          onClick={refetch}
          disabled={isBusy}
          style={{ ...buttonStyle, opacity: isBusy ? 0.5 : 1, cursor: isBusy ? 'not-allowed' : 'pointer' }}
          onMouseEnter={(e) => hoverButton(e, isBusy)}
          onMouseLeave={(e) => unhoverButton(e, isBusy)}
        >
          刷新
        </button>
        <button
          onClick={handleRegenerate}
          disabled={!canRegenerate}
          style={{
            ...buttonStyle,
            opacity: !canRegenerate ? 0.5 : 1,
            cursor: !canRegenerate ? 'not-allowed' : 'pointer',
          }}
          onMouseEnter={(e) => hoverButton(e, !canRegenerate)}
          onMouseLeave={(e) => unhoverButton(e, !canRegenerate)}
        >
          {regenerating ? '生成中…' : '生成日报'}
        </button>
      </div>

      {error && (
        <p
          style={{
            marginBottom: '16px',
            fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
            fontSize: '12px',
            color: '#f87171',
          }}
        >
          {error}
        </p>
      )}

      {loading && !report && <LoadingSpinner />}

      {!loading && !report && !error && (
        <p
          style={{
            textAlign: 'center',
            padding: '64px 0',
            fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
            fontSize: '12px',
            color: '#2E2E2E',
            letterSpacing: '0.1em',
            textTransform: 'uppercase',
          }}
        >
          — 暂无日报数据 —
        </p>
      )}

      {report && (
        <div
          style={{
            border: '1px solid #1F1F1F',
            padding: '24px',
          }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginBottom: '20px',
              paddingBottom: '16px',
              borderBottom: '1px solid #111',
              flexWrap: 'wrap',
              gap: '12px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <span
                style={{
                  fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
                  fontSize: '13px',
                  color: '#fff',
                  letterSpacing: '0.03em',
                }}
              >
                {report.reportDate}
              </span>
              <span
                style={{
                  fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
                  fontSize: '10px',
                  letterSpacing: '0.06em',
                  textTransform: 'uppercase',
                  color: STATUS_COLOR[report.status],
                  border: `1px solid ${STATUS_COLOR[report.status]}`,
                  padding: '2px 8px',
                }}
              >
                {STATUS_LABEL[report.status]}
              </span>
            </div>
            <span
              style={{
                fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
                fontSize: '11px',
                color: '#333',
                letterSpacing: '0.05em',
              }}
            >
              {report.hotspotCount != null ? `${report.hotspotCount} 条热点` : '—'}
              {report.generatedAt
                ? ` · ${new Date(report.generatedAt).toLocaleString('zh-CN')}`
                : ''}
            </span>
          </div>

          {report.status === 'GENERATING' && (
            <div style={{ marginBottom: '16px' }}>
              <LoadingSpinner />
              <p
                style={{
                  marginTop: '12px',
                  fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
                  fontSize: '11px',
                  color: '#555',
                  textAlign: 'center',
                }}
              >
                LLM 正在生成日报，请稍候…
              </p>
            </div>
          )}

          {report.status === 'FAILED' && report.errorMessage && (
            <p
              style={{
                marginBottom: '16px',
                fontFamily: "'Fira Sans', 'Noto Sans SC', sans-serif",
                fontSize: '13px',
                color: '#f87171',
                lineHeight: 1.6,
              }}
            >
              {report.errorMessage}
            </p>
          )}

          {report.status === 'EMPTY' && (
            <p
              style={{
                marginBottom: '16px',
                fontFamily: "'Fira Sans', 'Noto Sans SC', sans-serif",
                fontSize: '13px',
                color: '#666',
              }}
            >
              该日期没有热点数据，无法生成日报。
            </p>
          )}

          {report.content && (
            <div
              style={{
                fontFamily: "'Fira Sans', 'Noto Sans SC', sans-serif",
                fontSize: '13px',
                color: '#888',
                lineHeight: 1.8,
                whiteSpace: 'pre-wrap',
              }}
            >
              {report.content}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

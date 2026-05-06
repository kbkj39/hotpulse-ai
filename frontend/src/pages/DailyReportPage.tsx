import { useState } from 'react'
import { useReport } from '@/hooks/useReport'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'

export function DailyReportPage() {
  const [date, setDate] = useState<string>('')
  const { report, loading, refetch } = useReport(date || undefined)

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
          style={{
            border: '1px solid #1F1F1F',
            background: 'transparent',
            color: '#555',
            fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
            fontSize: '11px',
            letterSpacing: '0.08em',
            textTransform: 'uppercase',
            padding: '4px 12px',
            cursor: 'pointer',
            transition: 'all 150ms ease',
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.borderColor = '#fff'
            e.currentTarget.style.color = '#fff'
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.borderColor = '#1F1F1F'
            e.currentTarget.style.color = '#555'
          }}
        >
          REFRESH
        </button>
      </div>

      {loading && <LoadingSpinner />}

      {!loading && !report && (
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

      {!loading && report && (
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
            }}
          >
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
                fontSize: '11px',
                color: '#333',
                letterSpacing: '0.05em',
              }}
            >
              {report.hotspotCount} ITEMS · {new Date(report.generatedAt).toLocaleString('zh-CN')}
            </span>
          </div>
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
        </div>
      )}
    </div>
  )
}

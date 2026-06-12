import { useMemo, useState, type CSSProperties } from 'react'
import { useReport, type DailyReportStatus } from '@/hooks/useReport'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'

const MONO = "'Fira Code', 'Noto Sans SC', monospace"
const SANS = "'Fira Sans', 'Noto Sans SC', sans-serif"

const STATUS_LABEL: Record<DailyReportStatus, string> = {
  PENDING: '等待生成',
  GENERATING: '生成中',
  READY: '已就绪',
  EMPTY: '无热点',
  FAILED: '失败',
}

const STATUS_COLOR: Record<DailyReportStatus, string> = {
  PENDING: '#888',
  GENERATING: '#8EA7FF',
  READY: '#9CE6A3',
  EMPTY: '#666',
  FAILED: '#FF8A8A',
}

const buttonStyle: CSSProperties = {
  border: '1px solid #1F1F1F',
  background: 'transparent',
  color: '#777',
  fontFamily: MONO,
  fontSize: '12px',
  padding: '8px 12px',
  borderRadius: '5px',
  cursor: 'pointer',
}

function formatTime(value?: string | null) {
  return value ? new Date(value).toLocaleString('zh-CN') : '尚未生成'
}

function StatusBadge({ status }: { status: DailyReportStatus }) {
  const color = STATUS_COLOR[status]
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        border: `1px solid ${color}`,
        color,
        fontFamily: MONO,
        fontSize: '11px',
        padding: '4px 8px',
        borderRadius: '5px',
      }}
    >
      {STATUS_LABEL[status]}
    </span>
  )
}

function StatLine({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', gap: '12px', borderTop: '1px solid #151515', paddingTop: '10px' }}>
      <span style={{ fontFamily: MONO, color: '#555', fontSize: '11px' }}>{label}</span>
      <span style={{ fontFamily: MONO, color: '#aaa', fontSize: '11px', textAlign: 'right' }}>{value}</span>
    </div>
  )
}

export function DailyReportPage() {
  const [date, setDate] = useState<string>('')
  const { report, loading, regenerating, error, refetch, regenerate } = useReport(date || undefined)

  const targetDate = date || report?.reportDate
  const isBusy = loading || regenerating
  const canRegenerate = !!targetDate && report?.status !== 'GENERATING' && !regenerating

  const contentMeta = useMemo(() => {
    const content = report?.content?.trim() ?? ''
    if (!content) return { lines: 0, chars: 0 }
    return {
      lines: content.split(/\r?\n/).filter(Boolean).length,
      chars: content.length,
    }
  }, [report?.content])

  const handleRegenerate = () => {
    if (targetDate) regenerate(targetDate)
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
            display: 'flex',
            flex: '0 0 340px',
            maxWidth: '100%',
            height: '100%',
            minHeight: 0,
            flexDirection: 'column',
            gap: '16px',
            overflow: 'hidden',
          }}
        >
          <section
            style={{
              flex: '0 0 auto',
              border: '1px solid #202020',
              borderRadius: '8px',
              background: '#050505',
              padding: '16px',
            }}
          >
            <div style={{ borderBottom: '1px solid #151515', paddingBottom: '14px' }}>
              <h1 style={{ margin: 0, fontFamily: MONO, fontSize: '17px', fontWeight: 600, color: '#fff' }}>
                每日日报
              </h1>
              <div style={{ marginTop: '6px', fontFamily: SANS, fontSize: '12px', color: '#666' }}>
                按日期查看、刷新或手动生成热点日报。
              </div>
            </div>

            <div style={{ display: 'grid', gap: '12px', paddingTop: '14px' }}>
              <label style={{ display: 'grid', gap: '6px' }}>
                <span style={{ fontFamily: MONO, fontSize: '11px', color: '#555' }}>日期</span>
                <input
                  type="date"
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                  style={{
                    border: '1px solid #1F1F1F',
                    background: 'transparent',
                    color: '#aaa',
                    fontFamily: MONO,
                    fontSize: '13px',
                    padding: '8px 10px',
                    borderRadius: '5px',
                    outline: 'none',
                    cursor: 'pointer',
                  }}
                />
              </label>

              {date && (
                <button
                  type="button"
                  onClick={() => setDate('')}
                  style={{
                    border: 'none',
                    background: 'transparent',
                    color: '#777',
                    fontFamily: MONO,
                    fontSize: '12px',
                    padding: 0,
                    cursor: 'pointer',
                    justifySelf: 'start',
                  }}
                >
                  查看最新日报
                </button>
              )}

              <button
                type="button"
                onClick={refetch}
                disabled={isBusy}
                style={{
                  ...buttonStyle,
                  opacity: isBusy ? 0.5 : 1,
                  cursor: isBusy ? 'not-allowed' : 'pointer',
                }}
              >
                刷新
              </button>

              <button
                type="button"
                onClick={handleRegenerate}
                disabled={!canRegenerate}
                style={{
                  border: '1px solid #E6E6E6',
                  background: '#E6E6E6',
                  color: '#050505',
                  fontFamily: MONO,
                  fontSize: '12px',
                  padding: '9px 12px',
                  borderRadius: '5px',
                  opacity: !canRegenerate ? 0.45 : 1,
                  cursor: !canRegenerate ? 'not-allowed' : 'pointer',
                }}
              >
                {regenerating ? '生成中...' : '生成日报'}
              </button>
            </div>
          </section>

          <section
            style={{
              flex: '0 0 auto',
              border: '1px solid #202020',
              borderRadius: '8px',
              background: '#050505',
              padding: '16px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px' }}>
              <span style={{ fontFamily: MONO, color: '#555', fontSize: '11px' }}>状态</span>
              {report ? <StatusBadge status={report.status} /> : <span style={{ fontFamily: MONO, color: '#444', fontSize: '11px' }}>无数据</span>}
            </div>
            <div style={{ display: 'grid', gap: '10px', marginTop: '12px' }}>
              <StatLine label="报告日期" value={targetDate ?? '未选择'} />
              <StatLine label="热点数量" value={report?.hotspotCount != null ? `${report.hotspotCount} 条` : '-'} />
              <StatLine label="生成时间" value={formatTime(report?.generatedAt)} />
              <StatLine label="内容规模" value={report?.content ? `${contentMeta.lines} 行 / ${contentMeta.chars} 字` : '-'} />
            </div>
          </section>

          {error && (
            <div
              style={{
                marginTop: '16px',
                border: '1px solid #3A1C1C',
                borderRadius: '6px',
                padding: '10px',
                fontFamily: SANS,
                fontSize: '12px',
                color: '#FF8A8A',
                lineHeight: 1.6,
              }}
            >
              {error}
            </div>
          )}
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
          <div
            style={{
              display: 'flex',
              alignItems: 'baseline',
              justifyContent: 'space-between',
              gap: '16px',
              flexWrap: 'wrap',
              padding: '14px 16px',
              borderBottom: '1px solid #151515',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'baseline', gap: '10px', flexWrap: 'wrap' }}>
              <h2 style={{ margin: 0, fontFamily: MONO, fontSize: '16px', fontWeight: 600, color: '#fff' }}>
                报告内容
              </h2>
              <span style={{ fontFamily: SANS, fontSize: '12px', color: '#666' }}>
                {targetDate ?? '未选择日期'}
              </span>
            </div>
            {report && <StatusBadge status={report.status} />}
          </div>

          <div style={{ flex: 1, minHeight: 0, overflowY: 'auto', padding: '18px' }}>
            {loading && !report ? (
              <LoadingSpinner />
            ) : !report && !error ? (
              <div style={{ padding: '72px 0', textAlign: 'center', fontFamily: MONO, fontSize: '12px', color: '#333' }}>
                暂无日报数据
              </div>
            ) : report?.status === 'GENERATING' ? (
              <div style={{ padding: '72px 0', textAlign: 'center' }}>
                <LoadingSpinner />
                <div style={{ marginTop: '12px', fontFamily: MONO, fontSize: '12px', color: '#666' }}>
                  LLM 正在生成日报，请稍候...
                </div>
              </div>
            ) : report?.status === 'FAILED' ? (
              <div style={{ fontFamily: SANS, fontSize: '13px', color: '#FF8A8A', lineHeight: 1.7 }}>
                {report.errorMessage || '日报生成失败'}
              </div>
            ) : report?.status === 'EMPTY' ? (
              <div style={{ padding: '72px 0', textAlign: 'center', fontFamily: MONO, fontSize: '12px', color: '#555' }}>
                该日期没有热点数据，无法生成日报。
              </div>
            ) : report?.content ? (
              <article
                style={{
                  maxWidth: '900px',
                  fontFamily: SANS,
                  fontSize: '14px',
                  color: '#aaa',
                  lineHeight: 1.85,
                  whiteSpace: 'pre-wrap',
                }}
              >
                {report.content}
              </article>
            ) : (
              <div style={{ padding: '72px 0', textAlign: 'center', fontFamily: MONO, fontSize: '12px', color: '#333' }}>
                暂无内容
              </div>
            )}
          </div>
        </section>
      </div>
    </div>
  )
}

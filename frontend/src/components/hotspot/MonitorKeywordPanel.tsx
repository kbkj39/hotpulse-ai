import { useState, useEffect, useRef } from 'react'
import type { FormEvent } from 'react'
import type { MonitorKeyword } from '@/types/monitorKeyword'
import { useAgentExecution } from '@/hooks/useAgentExecution'

interface MonitorKeywordPanelProps {
  keywords: MonitorKeyword[]
  activeKeyword?: string
  loading?: boolean
  error?: string | null
  onApply: (keyword?: string, executionId?: string | null) => void
  onCreate: (keyword: string, triggerNow: boolean, crawlIntervalHours?: number | null) => Promise<{ keywordId: number; executionId: string | null }>
  onToggle: (item: MonitorKeyword) => Promise<void>
  onDelete: (id: number) => Promise<void>
  onTrigger: (id: number) => Promise<string | null>
  onTriggerAll: () => Promise<{ keywordId: number; executionId: string | null }[]>
  onUpdateInterval: (id: number, hours: number | null) => Promise<void>
  /** 抓取任务完成后的回调（用于刷新热点列表）*/
  onCrawlDone?: () => void
}

const MONO = "'Fira Code', 'Noto Sans SC', monospace"
const SANS = "'Fira Sans', 'Noto Sans SC', sans-serif"

const STATUS_DOT: Record<string, { symbol: string; color: string }> = {
  PENDING: { symbol: '○', color: '#333' },
  RUNNING: { symbol: '◉', color: '#888' },
  DONE:    { symbol: '●', color: '#9CE6A3' },
  FAILED:  { symbol: '✕', color: '#FF8A8A' },
}

// ─────────────────────────────────────────────────────────────────────────────
// 单个关键词卡片（带可折叠进度面板）
// ─────────────────────────────────────────────────────────────────────────────
interface KeywordCardProps {
  item: MonitorKeyword
  selected: boolean
  externalExecutionId: string | null
  onApply: (kw?: string, executionId?: string | null) => void
  onToggle: (item: MonitorKeyword) => Promise<void>
  onDelete: (id: number) => Promise<void>
  onTrigger: (id: number) => Promise<string | null>
  onUpdateInterval: (id: number, hours: number | null) => Promise<void>
  onCrawlDone?: () => void
}

function KeywordCard({
  item,
  selected,
  externalExecutionId,
  onApply,
  onToggle,
  onDelete,
  onTrigger,
  onUpdateInterval,
  onCrawlDone,
}: KeywordCardProps) {
  const [expanded, setExpanded] = useState(false)
  const [executionId, setExecutionId] = useState<string | null>(null)
  const [localInterval, setLocalInterval] = useState(
    item.crawlIntervalHours ? String(item.crawlIntervalHours) : '',
  )
  const [triggering, setTriggering] = useState(false)
  const [saving, setSaving] = useState(false)
  const doneFiredRef = useRef(false)

  const { steps, isError: sseError } = useAgentExecution(executionId)
  const isDone = steps.some(
    (s) => s.agentName === 'System' && (s.status === 'DONE' || s.status === 'FAILED'),
  )
  const isRunning = !!executionId && !isDone && !sseError

  // 外部 executionId 改变时（来自全局触发）同步本地并展开
  useEffect(() => {
    if (externalExecutionId && externalExecutionId !== executionId) {
      setExecutionId(externalExecutionId)
      setExpanded(true)
      doneFiredRef.current = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [externalExecutionId])

  // interval prop 随父组件更新
  useEffect(() => {
    setLocalInterval(item.crawlIntervalHours ? String(item.crawlIntervalHours) : '')
  }, [item.crawlIntervalHours])

  useEffect(() => {
    if (isDone && !doneFiredRef.current) {
      doneFiredRef.current = true
      onCrawlDone?.()
    }
  }, [isDone, onCrawlDone])

  const handleTrigger = async () => {
    setTriggering(true)
    doneFiredRef.current = false
    try {
      const id = await onTrigger(item.id)
      if (id) {
        setExecutionId(id)
        setExpanded(true)
        onApply(item.keyword, id)
      }
    } catch (_) {
      /* error surfaced by parent hook */
    } finally {
      setTriggering(false)
    }
  }

  const handleSaveInterval = async () => {
    setSaving(true)
    try {
      const hours = localInterval === '' ? null : parseInt(localInterval, 10) || null
      await onUpdateInterval(item.id, hours)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div style={{ borderTop: '1px solid #151515', padding: '10px 0 8px' }}>
      {/* ── 主控制行 ── */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' }}>
        {/* 展开/折叠箭头 */}
        <button
          type="button"
          onClick={() => setExpanded((v) => !v)}
          title={expanded ? '收起进度' : '展开进度'}
          style={{
            border: 'none',
            background: 'transparent',
            color: isRunning ? '#9CE6A3' : '#3A3A3A',
            fontFamily: MONO,
            fontSize: '10px',
            padding: '0 4px 0 0',
            cursor: 'pointer',
            lineHeight: 1,
            flexShrink: 0,
            transition: 'color 0.2s',
          }}
        >
          {expanded ? '▼' : '▶'}
        </button>

        {/* 关键词名 */}
        <button
          type="button"
          onClick={() => onApply(selected ? undefined : item.keyword)}
          style={{
            border: 'none',
            background: 'transparent',
            color: item.enabled ? (selected ? '#fff' : '#ccc') : '#444',
            fontFamily: MONO,
            fontSize: '14px',
            padding: '0',
            cursor: 'pointer',
            textDecoration: selected ? 'underline' : 'none',
            textUnderlineOffset: '3px',
          }}
        >
          {item.keyword}
        </button>

        {/* ON/OFF */}
        <button
          type="button"
          onClick={() => void onToggle(item)}
          title={item.enabled ? '点击禁用' : '点击启用'}
          style={{
            border: '1px solid #1F1F1F',
            background: item.enabled ? '#101810' : 'transparent',
            color: item.enabled ? '#9CE6A3' : '#666',
            fontFamily: MONO,
            fontSize: '10px',
            padding: '2px 6px',
            borderRadius: '4px',
            cursor: 'pointer',
          }}
        >
          {item.enabled ? '启用' : '停用'}
        </button>

        {/* DEL */}
        <button
          type="button"
          onClick={() => void onDelete(item.id)}
          title="删除关键词"
          style={{
            border: '1px solid #1F1F1F',
            background: 'transparent',
            color: '#777',
            fontFamily: MONO,
            fontSize: '10px',
            padding: '2px 6px',
            borderRadius: '4px',
            cursor: 'pointer',
          }}
        >
          删除
        </button>

        <span style={{ flex: 1 }} />

        {/* 运行指示符 */}
        {isRunning && (
          <span style={{ color: '#9CE6A3', fontFamily: MONO, fontSize: '11px' }}>◉</span>
        )}

        {/* 立即搜索 */}
        <button
          type="button"
          onClick={handleTrigger}
          disabled={triggering}
          style={{
            border: '1px solid #2A2A2A',
            background: triggering ? 'transparent' : '#081108',
            color: triggering ? '#555' : '#9CE6A3',
            fontFamily: MONO,
            fontSize: '11px',
            padding: '3px 10px',
            borderRadius: '5px',
            cursor: triggering ? 'wait' : 'pointer',
            whiteSpace: 'nowrap',
          }}
        >
          {triggering ? '搜索中…' : '立即搜索'}
        </button>

        {/* 定时搜索间隔 */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '4px',
            fontFamily: MONO,
            fontSize: '11px',
            color: '#555',
          }}
        >
          <span>间隔</span>
          <input
            type="number"
            min={1}
            max={168}
            value={localInterval}
            onChange={(e) => setLocalInterval(e.target.value)}
            placeholder="无"
            style={{
              width: '46px',
              border: '1px solid #1F1F1F',
              background: 'transparent',
              color: '#aaa',
              fontFamily: MONO,
              fontSize: '11px',
              padding: '2px 4px',
              outline: 'none',
              textAlign: 'center',
            }}
          />
          <span>小时</span>
          <button
            type="button"
            onClick={handleSaveInterval}
            disabled={saving}
            style={{
              border: '1px solid #1F1F1F',
              background: 'transparent',
              color: saving ? '#444' : '#666',
              fontFamily: MONO,
              fontSize: '10px',
              padding: '2px 6px',
              borderRadius: '4px',
              cursor: saving ? 'wait' : 'pointer',
            }}
          >
            {saving ? '…' : '保存'}
          </button>
        </div>
      </div>

      {/* ── 可折叠进度面板 ── */}
      {expanded && (
        <div
          style={{
            marginTop: '6px',
            marginLeft: '16px',
            borderLeft: '1px solid #1A1A1A',
            paddingLeft: '10px',
          }}
        >
          {!executionId && (
            <div style={{ fontFamily: MONO, fontSize: '11px', color: '#2E2E2E' }}>
              尚未触发，点击「立即搜索」开始
            </div>
          )}
          {executionId && sseError && !isDone && (
            <div style={{ fontFamily: MONO, fontSize: '11px', color: '#FF8A8A' }}>
              连接中断，抓取仍在后台运行
            </div>
          )}
          {executionId && steps.length === 0 && !sseError && (
            <div style={{ fontFamily: MONO, fontSize: '11px', color: '#444' }}>
              等待任务启动…
            </div>
          )}
          <div style={{ maxHeight: '140px', overflowY: 'auto' }}>
            {steps.map((step, i) => {
              const dot = STATUS_DOT[step.status] ?? { symbol: '·', color: '#333' }
              return (
                <div
                  key={i}
                  style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: '6px',
                    fontFamily: MONO,
                    fontSize: '11px',
                    marginBottom: '2px',
                    lineHeight: 1.5,
                  }}
                >
                  <span style={{ color: dot.color, flexShrink: 0 }}>{dot.symbol}</span>
                  <span style={{ color: '#444', flexShrink: 0 }}>[{step.agentName}]</span>
                  <span style={{ flex: 1, color: '#555' }}>{step.message}</span>
                  <span style={{ color: '#2E2E2E', fontSize: '10px', whiteSpace: 'nowrap' }}>
                    {step.timestamp
                      ? new Date(step.timestamp).toLocaleTimeString('zh-CN', {
                          hour: '2-digit',
                          minute: '2-digit',
                          second: '2-digit',
                        })
                      : ''}
                  </span>
                </div>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// 主面板
// ─────────────────────────────────────────────────────────────────────────────
export function MonitorKeywordPanel({
  keywords,
  activeKeyword,
  loading,
  error,
  onApply,
  onCreate,
  onToggle,
  onDelete,
  onTrigger,
  onTriggerAll,
  onUpdateInterval,
  onCrawlDone,
}: MonitorKeywordPanelProps) {
  const [value, setValue] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [localError, setLocalError] = useState<string | null>(null)

  // 全局定时设置
  const [globalInterval, setGlobalInterval] = useState('')
  const [savingGlobal, setSavingGlobal] = useState(false)
  const [triggeringAll, setTriggeringAll] = useState(false)

  // keywordId -> executionId（全局触发后分发至各卡片）
  const [keywordExecIdMap, setKeywordExecIdMap] = useState<Record<number, string | null>>({})

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    const normalized = value.trim()
    if (!normalized) return
    setSubmitting(true)
    setLocalError(null)
    try {
      const { keywordId, executionId } = await onCreate(normalized, true, null)
      setValue('')
      onApply(normalized, executionId)
      if (executionId) {
        setKeywordExecIdMap((prev) => ({ ...prev, [keywordId]: executionId }))
      }
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : '添加关键词失败')
    } finally {
      setSubmitting(false)
    }
  }

  const handleTriggerAll = async () => {
    setTriggeringAll(true)
    setLocalError(null)
    try {
      const results = await onTriggerAll()
      const map: Record<number, string | null> = {}
      for (const r of results) map[r.keywordId] = r.executionId
      setKeywordExecIdMap((prev) => ({ ...prev, ...map }))
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : '触发全部关键词失败')
    } finally {
      setTriggeringAll(false)
    }
  }

  const handleSaveGlobalInterval = async () => {
    if (!keywords.length) return
    setSavingGlobal(true)
    setLocalError(null)
    const hours = globalInterval === '' ? null : parseInt(globalInterval, 10) || null
    try {
      await Promise.all(keywords.map((kw) => onUpdateInterval(kw.id, hours)))
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : '更新定时间隔失败')
    } finally {
      setSavingGlobal(false)
    }
  }

  return (
    <section
      style={{
        display: 'flex',
        flexDirection: 'column',
        flex: '1 1 auto',
        minHeight: 0,
        padding: '16px 18px',
        marginBottom: 0,
        border: '1px solid #1F1F1F',
        borderRadius: '8px',
        background: '#030303',
      }}
    >
      {/* 标题行 */}
      <div
        style={{
          display: 'flex',
          alignItems: 'baseline',
          justifyContent: 'space-between',
          gap: '16px',
          marginBottom: '12px',
          flexWrap: 'wrap',
        }}
      >
        <div>
          <div
            style={{
              fontFamily: MONO,
              fontSize: '12px',
              color: '#666',
              letterSpacing: '0.08em',
              textTransform: 'uppercase',
              marginBottom: '4px',
            }}
          >
            监控关键词
          </div>
          <div style={{ fontFamily: SANS, fontSize: '13px', color: '#888' }}>
            保存关注主题，手动或定时抓取新闻，并把趋势图和热点列表切到对应范围。
          </div>
        </div>
        {activeKeyword && (
          <button
            type="button"
            onClick={() => onApply(undefined)}
            style={{
              border: '1px solid #1F1F1F',
              background: 'transparent',
              color: '#777',
              fontFamily: MONO,
              fontSize: '11px',
              padding: '6px 10px',
              cursor: 'pointer',
            }}
          >
            清除筛选
          </button>
        )}
      </div>

      {/* 添加关键词 */}
      <form onSubmit={handleSubmit} style={{ marginBottom: '14px' }}>
        <div style={{ display: 'flex', gap: '8px' }}>
          <input
            type="text"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            placeholder="输入监控关键词"
            style={{
              flex: '1 1 200px',
              minWidth: '160px',
              border: '1px solid #1F1F1F',
              background: 'transparent',
              color: '#fff',
              fontFamily: SANS,
              fontSize: '14px',
              padding: '8px 10px',
              outline: 'none',
            }}
          />
          <button
            type="submit"
            disabled={submitting}
            style={{
              border: '1px solid #E6E6E6',
              background: '#E6E6E6',
              color: '#050505',
              fontFamily: MONO,
              fontSize: '11px',
              padding: '8px 14px',
              borderRadius: '5px',
              cursor: submitting ? 'wait' : 'pointer',
              opacity: submitting ? 0.7 : 1,
              whiteSpace: 'nowrap',
            }}
          >
            {submitting ? '添加中…' : '添加'}
          </button>
        </div>
      </form>

      {/* 全局操作行 */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          flexWrap: 'wrap',
          marginBottom: '14px',
          paddingBottom: '12px',
          borderBottom: '1px solid #151515',
        }}
      >
        <button
          type="button"
          onClick={handleTriggerAll}
          disabled={triggeringAll || keywords.filter((k) => k.enabled).length === 0}
          style={{
            border: '1px solid #2A2A2A',
            background: triggeringAll ? 'transparent' : '#081108',
            color: triggeringAll ? '#555' : '#9CE6A3',
            fontFamily: MONO,
            fontSize: '11px',
            padding: '5px 12px',
            borderRadius: '5px',
            cursor: triggeringAll ? 'wait' : 'pointer',
            whiteSpace: 'nowrap',
          }}
        >
          {triggeringAll ? '搜索中…' : '搜索全部'}
        </button>

        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '5px',
            fontFamily: MONO,
            fontSize: '11px',
            color: '#555',
          }}
        >
          <span>全部关键词间隔</span>
          <input
            type="number"
            min={1}
            max={168}
            value={globalInterval}
            onChange={(e) => setGlobalInterval(e.target.value)}
            placeholder="无"
            style={{
              width: '52px',
              border: '1px solid #1F1F1F',
              background: 'transparent',
              color: '#aaa',
              fontFamily: MONO,
              fontSize: '11px',
              padding: '3px 5px',
              outline: 'none',
              textAlign: 'center',
            }}
          />
          <span>小时</span>
          <button
            type="button"
            onClick={handleSaveGlobalInterval}
            disabled={savingGlobal || keywords.length === 0}
            style={{
              border: '1px solid #1F1F1F',
              background: 'transparent',
              color: savingGlobal ? '#444' : '#666',
              fontFamily: MONO,
              fontSize: '10px',
              padding: '3px 8px',
              borderRadius: '4px',
              cursor: savingGlobal ? 'wait' : 'pointer',
            }}
          >
            {savingGlobal ? '…' : '保存'}
          </button>
        </div>
      </div>

      {/* 错误提示 */}
      {(localError || error) && (
        <div style={{ marginBottom: '10px', fontFamily: MONO, fontSize: '12px', color: '#FF8A8A' }}>
          {localError || error}
        </div>
      )}

      {/* 关键词卡片列表 */}
      <div style={{ flex: '1 1 auto', minHeight: '96px', overflowY: 'auto', paddingRight: '2px' }}>
        {loading && (
          <div style={{ fontFamily: MONO, fontSize: '12px', color: '#444' }}>加载中…</div>
        )}
        {!loading && keywords.length === 0 && (
          <div style={{ fontFamily: MONO, fontSize: '12px', color: '#333' }}>
            暂无监控关键词，请在上方添加
          </div>
        )}
        {keywords.map((item) => (
          <KeywordCard
            key={item.id}
            item={item}
            selected={activeKeyword === item.keyword}
            externalExecutionId={keywordExecIdMap[item.id] ?? null}
            onApply={onApply}
            onToggle={onToggle}
            onDelete={onDelete}
            onTrigger={onTrigger}
            onUpdateInterval={onUpdateInterval}
            onCrawlDone={onCrawlDone}
          />
        ))}
      </div>
    </section>
  )
}

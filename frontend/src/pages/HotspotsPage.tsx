import { useHotspots } from '@/hooks/useHotspots'
import { useMonitorKeywords } from '@/hooks/useMonitorKeywords'
import { useSocket } from '@/hooks/useSocket'
import { HotspotFilterBar } from '@/components/hotspot/HotspotFilter'
import { HotspotList } from '@/components/hotspot/HotspotList'
import { MonitorKeywordPanel } from '@/components/hotspot/MonitorKeywordPanel'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'
import { HotspotTrendChart } from '@/components/hotspot/HotspotTrendChart'
import { useState } from 'react'

export function HotspotsPage() {
  const { hotspots, filter, total, setFilter, loading, refetch } = useHotspots()
  const { keywords, loading: keywordsLoading, error: keywordsError, createKeyword, toggleKeyword, deleteKeyword, triggerKeyword, triggerAllKeywords, updateInterval } = useMonitorKeywords()
  useSocket()
  const [trendInterval, setTrendInterval] = useState<'hour' | 'day'>('hour')
  const activeMonitorKeyword = keywords.some((item) => item.keyword === filter.keyword)
    ? filter.keyword
    : undefined
  const freeTextKeyword = activeMonitorKeyword ? undefined : filter.keyword
  const trendScopeText = activeMonitorKeyword
    ? `当前主题：${activeMonitorKeyword}`
    : filter.tag
      ? `当前标签：${filter.tag}`
      : freeTextKeyword
        ? `全文检索：${freeTextKeyword}`
        : '全部热点'

  return (
    <div>
      <div
        style={{
          display: 'flex',
          alignItems: 'baseline',
          justifyContent: 'space-between',
          gap: '20px',
          marginBottom: '22px',
          paddingBottom: '16px',
          borderBottom: '2px solid #1F1F1F',
          flexWrap: 'wrap',
        }}
      >
        <div>
          <h1
            style={{
              fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
              fontWeight: 600,
              fontSize: '22px',
              letterSpacing: '0',
              color: '#fff',
              margin: 0,
            }}
          >
            热点雷达
          </h1>
          <div
            style={{
              marginTop: '6px',
              fontFamily: "'Fira Sans', 'Noto Sans SC', sans-serif",
              fontSize: '13px',
              color: '#777',
            }}
          >
            监控关键词、跟踪主题热度，并筛出值得继续追踪的新闻线索。
          </div>
        </div>
        <span
          style={{
            fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
            fontSize: '12px',
            color: '#888',
            letterSpacing: '0.08em',
            textTransform: 'uppercase',
          }}
        >
          {total} 条热点
        </span>
      </div>
      <MonitorKeywordPanel
        keywords={keywords}
        activeKeyword={filter.keyword}
        loading={keywordsLoading}
        error={keywordsError}
        onApply={(keyword) => setFilter({ keyword, page: 1 })}
        onCreate={createKeyword}
        onToggle={toggleKeyword}
        onDelete={deleteKeyword}
        onTrigger={triggerKeyword}
        onTriggerAll={triggerAllKeywords}
        onUpdateInterval={updateInterval}
        onCrawlDone={refetch}
      />
      <section
        style={{
          marginTop: '28px',
          marginBottom: '28px',
          padding: '18px 20px 22px',
          border: '1px solid #202020',
          borderRadius: '8px',
          background: '#050505',
        }}
      >
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            gap: '16px',
            marginBottom: '10px',
            flexWrap: 'wrap',
          }}
        >
          <div>
            <h2
              style={{
                fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
                fontWeight: 600,
                fontSize: '17px',
                letterSpacing: '0',
                color: '#fff',
                margin: 0,
              }}
            >
              趋势观察
            </h2>
            <div
              style={{
                marginTop: '6px',
                fontFamily: "'Fira Sans', 'Noto Sans SC', sans-serif",
                fontSize: '13px',
                color: '#777',
              }}
            >
              {trendScopeText}
            </div>
          </div>
          <div
            role="tablist"
            aria-label="趋势时间粒度"
            style={{
              display: 'inline-flex',
              border: '1px solid #242424',
              borderRadius: '6px',
              overflow: 'hidden',
              flexShrink: 0,
            }}
          >
            {[
              ['hour', '按小时'],
              ['day', '按天'],
            ].map(([value, label]) => {
              const active = trendInterval === value
              return (
                <button
                  key={value}
                  type="button"
                  onClick={() => setTrendInterval(value as 'hour' | 'day')}
                  style={{
                    padding: '6px 14px',
                    background: active ? '#E6E6E6' : 'transparent',
                    color: active ? '#050505' : '#888',
                    border: 'none',
                    borderRight: value === 'hour' ? '1px solid #242424' : 'none',
                    cursor: 'pointer',
                    fontSize: '12px',
                    fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
                  }}
                >
                  {label}
                </button>
              )
            })}
          </div>
        </div>
        <HotspotTrendChart
          interval={trendInterval}
          monitorKeyword={activeMonitorKeyword}
          tag={filter.tag}
          keyword={freeTextKeyword}
        />
      </section>
      <HotspotFilterBar filter={filter} onChange={setFilter} />
      {loading ? <LoadingSpinner /> : <HotspotList hotspots={hotspots} />}
    </div>
  )
}

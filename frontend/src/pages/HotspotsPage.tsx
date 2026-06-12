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
  const activeMonitorKeyword = filter.monitorKeyword
  const freeTextKeyword = activeMonitorKeyword ? undefined : filter.keyword
  const trendScopeText = activeMonitorKeyword
    ? `当前主题：${activeMonitorKeyword}`
    : filter.tag
      ? `当前标签：${filter.tag}`
      : freeTextKeyword
        ? `全文检索：${freeTextKeyword}`
        : '全部热点'

  return (
    <div style={{ height: 'calc(100vh - 96px)', minHeight: 0, overflow: 'hidden' }}>
      <div
        style={{
          display: 'flex',
          alignItems: 'flex-start',
          gap: '18px',
          flexWrap: 'wrap',
          height: '100%',
          minHeight: 0,
          overflow: 'hidden',
        }}
      >
        <aside
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '16px',
            flex: '0 0 460px',
            maxWidth: '100%',
            height: '100%',
            minHeight: 0,
            overflow: 'hidden',
          }}
        >
          <MonitorKeywordPanel
            keywords={keywords}
            activeKeyword={filter.keyword}
            loading={keywordsLoading}
            error={keywordsError}
            onApply={(keyword, executionId) => setFilter({ keyword, monitorKeyword: keyword, executionId: executionId ?? undefined, page: 1 })}
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
              flex: '0 0 auto',
              padding: '16px',
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
                gap: '12px',
                marginBottom: '10px',
                flexWrap: 'wrap',
              }}
            >
              <div>
                <h2
                  style={{
                    fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
                    fontWeight: 600,
                    fontSize: '15px',
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
                    fontSize: '12px',
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
                  ['hour', '小时'],
                  ['day', '天'],
                ].map(([value, label]) => {
                  const active = trendInterval === value
                  return (
                    <button
                      key={value}
                      type="button"
                      onClick={() => setTrendInterval(value as 'hour' | 'day')}
                      style={{
                        padding: '5px 10px',
                        background: active ? '#E6E6E6' : 'transparent',
                        color: active ? '#050505' : '#888',
                        border: 'none',
                        borderRight: value === 'hour' ? '1px solid #242424' : 'none',
                        cursor: 'pointer',
                        fontSize: '11px',
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
              compact
            />
          </section>
        </aside>

        <section
          style={{
            display: 'flex',
            flex: '1 1 620px',
            minWidth: 0,
            height: '100%',
            minHeight: 0,
            flexDirection: 'column',
          }}
        >
          <div
            style={{
              marginBottom: '14px',
              padding: '14px 16px',
              border: '1px solid #202020',
              borderRadius: '8px',
              background: '#050505',
            }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'baseline',
                justifyContent: 'space-between',
                gap: '16px',
                marginBottom: '12px',
                paddingBottom: '12px',
                borderBottom: '1px solid #151515',
              }}
            >
              <div>
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'baseline',
                    gap: '10px',
                    flexWrap: 'wrap',
                  }}
                >
                  <h2
                    style={{
                      fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
                      fontSize: '16px',
                      fontWeight: 600,
                      color: '#fff',
                      margin: 0,
                    }}
                  >
                    热点列表
                  </h2>
                  <span
                    style={{
                      fontFamily: "'Fira Sans', 'Noto Sans SC', sans-serif",
                      fontSize: '12px',
                      color: '#666',
                    }}
                  >
                    {trendScopeText}
                  </span>
                </div>
              </div>
              <span
                style={{
                  fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
                  fontSize: '12px',
                  color: '#888',
                }}
              >
                {total} 条
              </span>
            </div>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                flexWrap: 'wrap',
              }}
            >
              <HotspotFilterBar
                filter={filter}
                onChange={(nextFilter) =>
                  setFilter({
                    ...nextFilter,
                    monitorKeyword: nextFilter.keyword !== undefined ? undefined : filter.monitorKeyword,
                    executionId: nextFilter.keyword !== undefined ? undefined : filter.executionId,
                  })
                }
                compact
              />
            </div>
          </div>
          <div style={{ flex: 1, minHeight: 0, overflowY: 'auto', paddingRight: '2px' }}>
            {loading ? <LoadingSpinner /> : <HotspotList hotspots={hotspots} />}
          </div>
        </section>
      </div>
    </div>
  )
}

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

  return (
    <div>
      <div
        style={{
          display: 'flex',
          alignItems: 'baseline',
          justifyContent: 'space-between',
          marginBottom: '20px',
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
          热点雷达
        </h1>
        <span
          style={{
            fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
            fontSize: '12px',
            color: '#333',
            letterSpacing: '0.08em',
            textTransform: 'uppercase',
          }}
        >
          {total} ITEMS
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
      <div style={{ marginTop: '32px', marginBottom: '32px', padding: '20px', border: '1px solid #333', borderRadius: '8px', background: '#0a0a0a' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h2
            style={{
              fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
              fontWeight: 600,
              fontSize: '18px',
              letterSpacing: '-0.02em',
              color: '#fff',
              margin: 0,
            }}
          >
            趋势分析
          </h2>
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              onClick={() => setTrendInterval('hour')}
              style={{
                padding: '6px 16px',
                background: trendInterval === 'hour' ? '#8884d8' : '#1a1a1a',
                color: '#fff',
                border: '1px solid #333',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '13px',
                fontFamily: "'Fira Code', monospace",
              }}
            >
              按小时
            </button>
            <button
              onClick={() => setTrendInterval('day')}
              style={{
                padding: '6px 16px',
                background: trendInterval === 'day' ? '#8884d8' : '#1a1a1a',
                color: '#fff',
                border: '1px solid #333',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '13px',
                fontFamily: "'Fira Code', monospace",
              }}
            >
              按天
            </button>
          </div>
        </div>
        <HotspotTrendChart interval={trendInterval} />
      </div>
      <HotspotFilterBar filter={filter} onChange={setFilter} />
      {loading ? <LoadingSpinner /> : <HotspotList hotspots={hotspots} />}
    </div>
  )
}

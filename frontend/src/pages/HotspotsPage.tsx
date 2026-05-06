import { useHotspots } from '@/hooks/useHotspots'
import { useMonitorKeywords } from '@/hooks/useMonitorKeywords'
import { useSocket } from '@/hooks/useSocket'
import { HotspotFilterBar } from '@/components/hotspot/HotspotFilter'
import { HotspotList } from '@/components/hotspot/HotspotList'
import { MonitorKeywordPanel } from '@/components/hotspot/MonitorKeywordPanel'
import { LoadingSpinner } from '@/components/common/LoadingSpinner'

export function HotspotsPage() {
  const { hotspots, filter, total, setFilter, loading, refetch } = useHotspots()
  const { keywords, loading: keywordsLoading, error: keywordsError, createKeyword, toggleKeyword, deleteKeyword, triggerKeyword, triggerAllKeywords, updateInterval } = useMonitorKeywords()
  useSocket()

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
      <HotspotFilterBar filter={filter} onChange={setFilter} />
      {loading ? <LoadingSpinner /> : <HotspotList hotspots={hotspots} />}
    </div>
  )
}

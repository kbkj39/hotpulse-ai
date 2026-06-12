import { useState, useEffect } from 'react'
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts'
import { api } from '@/services/api'

interface TrendPoint {
  timestamp: string
  count: number
  avgHotScore: number
  avgImportanceScore: number
}

interface HotspotTrendChartProps {
  interval?: 'hour' | 'day'
  monitorKeyword?: string
  tag?: string
  keyword?: string
  compact?: boolean
}

export function HotspotTrendChart({
  interval = 'hour',
  monitorKeyword,
  tag,
  keyword,
  compact,
}: HotspotTrendChartProps) {
  const [data, setData] = useState<TrendPoint[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)

    api
      .getHotspotTrends(interval, { monitorKeyword, tag, keyword })
      .then((result) => {
        if (!cancelled) {
          setData(result)
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err.message || '趋势数据加载失败')
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [interval, monitorKeyword, tag, keyword])

  const scopeLabel =
    monitorKeyword ? `监控关键词：${monitorKeyword}` :
    tag ? `标签：${tag}` :
    keyword ? `关键词：${keyword}` :
    '全部热点'

  if (loading) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#888' }}>
        正在加载趋势…
      </div>
    )
  }

  if (error) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#f44' }}>
        {error}
      </div>
    )
  }

  if (data.length === 0) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#888' }}>
        暂无趋势数据
      </div>
    )
  }

  const formattedData = data.map((point) => ({
    ...point,
    time: new Date(point.timestamp).toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: interval === 'hour' ? '2-digit' : undefined,
      minute: interval === 'hour' ? '2-digit' : undefined,
    }),
    avgHotScore: Number(point.avgHotScore?.toFixed(2)) || 0,
    avgImportanceScore: Number(point.avgImportanceScore?.toFixed(2)) || 0,
  }))

  return (
    <div style={{ width: '100%', height: compact ? '240px' : '320px', marginTop: compact ? '12px' : '20px' }}>
      <div
        style={{
          marginBottom: '8px',
          display: 'flex',
          justifyContent: compact ? 'flex-start' : 'space-between',
          gap: '12px',
          flexWrap: 'wrap',
          fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
          fontSize: '11px',
          color: '#666',
          letterSpacing: '0.04em',
        }}
      >
        <span>{scopeLabel}</span>
        {!compact && <span>分数线为每个时间桶 Top 5 热点均值</span>}
      </div>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart
          data={formattedData}
          margin={compact ? { top: 5, right: 18, left: 0, bottom: 5 } : { top: 5, right: 30, left: 20, bottom: 5 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="#202020" />
          <XAxis
            dataKey="time"
            stroke="#888"
            style={{ fontSize: '12px' }}
          />
          <YAxis
            yAxisId="left"
            stroke="#888"
            style={{ fontSize: '12px' }}
            label={compact ? undefined : { value: '分数', angle: -90, position: 'insideLeft', fill: '#777' }}
          />
          <YAxis
            yAxisId="right"
            orientation="right"
            stroke="#888"
            style={{ fontSize: '12px' }}
            label={compact ? undefined : { value: '数量', angle: 90, position: 'insideRight', fill: '#777' }}
          />
          <Tooltip
            contentStyle={{
              backgroundColor: '#111',
              border: '1px solid #333',
              borderRadius: '4px',
              color: '#fff',
            }}
          />
          {!compact && <Legend />}
          <Line
            yAxisId="left"
            type="monotone"
            dataKey="avgHotScore"
            stroke="#8884d8"
            strokeWidth={2}
            name="Top 热度"
            dot={{ r: 4 }}
          />
          <Line
            yAxisId="left"
            type="monotone"
            dataKey="avgImportanceScore"
            stroke="#82ca9d"
            strokeWidth={2}
            name="Top 重要性"
            dot={{ r: 4 }}
          />
          <Line
            yAxisId="right"
            type="monotone"
            dataKey="count"
            stroke="#ffc658"
            strokeWidth={3}
            name="热点数量"
            dot={{ r: 4 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}

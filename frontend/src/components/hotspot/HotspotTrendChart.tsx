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
}

export function HotspotTrendChart({ interval = 'hour' }: HotspotTrendChartProps) {
  const [data, setData] = useState<TrendPoint[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)

    api
      .getHotspotTrends(interval)
      .then((result) => {
        if (!cancelled) {
          setData(result)
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err.message || 'Failed to fetch trends')
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
  }, [interval])

  if (loading) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#888' }}>
        Loading trends...
      </div>
    )
  }

  if (error) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#f44' }}>
        Error: {error}
      </div>
    )
  }

  if (data.length === 0) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#888' }}>
        No trend data available
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
    <div style={{ width: '100%', height: '300px', marginTop: '20px' }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart
          data={formattedData}
          margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="#333" />
          <XAxis
            dataKey="time"
            stroke="#888"
            style={{ fontSize: '12px' }}
          />
          <YAxis
            yAxisId="left"
            stroke="#888"
            style={{ fontSize: '12px' }}
            label={{ value: 'Score', angle: -90, position: 'insideLeft', fill: '#888' }}
          />
          <YAxis
            yAxisId="right"
            orientation="right"
            stroke="#888"
            style={{ fontSize: '12px' }}
            label={{ value: 'Count', angle: 90, position: 'insideRight', fill: '#888' }}
          />
          <Tooltip
            contentStyle={{
              backgroundColor: '#1a1a1a',
              border: '1px solid #333',
              borderRadius: '4px',
              color: '#fff',
            }}
          />
          <Legend />
          <Line
            yAxisId="left"
            type="monotone"
            dataKey="avgHotScore"
            stroke="#8884d8"
            strokeWidth={2}
            name="Avg Hot Score"
            dot={{ r: 4 }}
          />
          <Line
            yAxisId="left"
            type="monotone"
            dataKey="avgImportanceScore"
            stroke="#82ca9d"
            strokeWidth={2}
            name="Avg Importance"
            dot={{ r: 4 }}
          />
          <Line
            yAxisId="right"
            type="monotone"
            dataKey="count"
            stroke="#ffc658"
            strokeWidth={2}
            name="Hotspot Count"
            dot={{ r: 4 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}

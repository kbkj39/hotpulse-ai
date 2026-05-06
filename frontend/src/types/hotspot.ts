export interface Hotspot {
  id: number
  title: string
  summary: string
  source: string
  publishedAt: string
  hotScore: number
  truthScore: number
  relevanceScore: number
  importanceScore: number
  analysisEvidence: string
  tags: string[]
  url: string
  fullText?: string
  executionId?: string
}

export interface HotspotFilter {
  sort: 'hot' | 'importance' | 'relevance' | 'time'
  tag?: string
  keyword?: string
  page: number
  limit: number
}

export interface ScoreDetail {
  label: string
  value: number
}

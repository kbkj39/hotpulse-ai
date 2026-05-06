import axios from 'axios'
import type { Hotspot, HotspotFilter } from '@/types/hotspot'
import type { MonitorKeyword } from '@/types/monitorKeyword'
import type { Message, Conversation } from '@/types/chat'

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  timeout: 30000,
})

instance.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API error:', error.response?.data || error.message)
    return Promise.reject(error)
  }
)

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const api = {
  getHotspots: (filter: Partial<HotspotFilter>) =>
    instance
      .get<ApiResult<{ total: number; items: Hotspot[] }>>('/hotspots', {
        params: filter,
      })
      .then((r) => r.data.data),

  getHotspotDetail: (id: number) =>
    instance
      .get<ApiResult<Hotspot>>(`/hotspots/${id}`)
      .then((r) => r.data.data),

  agentQuery: (query: string, conversationId?: string) =>
    instance
      .post<ApiResult<{ executionId: string; conversationId: string; status: string }>>('/agent/query', {
        query,
        conversationId,
      })
      .then((r) => r.data.data),

  getSources: () =>
    instance.get<ApiResult<unknown[]>>('/sources').then((r) => r.data.data),

  getMonitorKeywords: () =>
    instance.get<ApiResult<MonitorKeyword[]>>('/monitor-keywords').then((r) => r.data.data),

  createMonitorKeyword: (data: { keyword: string; enabled?: boolean; triggerNow?: boolean; crawlIntervalHours?: number | null }) =>
    instance
      .post<ApiResult<{ keyword: MonitorKeyword; executionId?: string | null }>>('/monitor-keywords', data)
      .then((r) => r.data.data),

  updateMonitorKeyword: (id: number, keyword: Partial<MonitorKeyword>) =>
    instance.put<ApiResult<MonitorKeyword>>(`/monitor-keywords/${id}`, keyword).then((r) => r.data.data),

  deleteMonitorKeyword: (id: number) =>
    instance.delete<ApiResult<{ deleted: boolean }>>(`/monitor-keywords/${id}`).then((r) => r.data.data),

  triggerMonitorKeyword: (id: number) =>
    instance
      .post<ApiResult<{ executionId: string | null }>>(`/monitor-keywords/${id}/trigger`)
      .then((r) => r.data.data),

  triggerAllMonitorKeywords: () =>
    instance
      .post<ApiResult<{ keywordId: number; executionId: string | null }[]>>('/monitor-keywords/trigger-all')
      .then((r) => r.data.data),

  upsertSource: (source: unknown) =>
    instance.post<ApiResult<unknown>>('/sources', source).then((r) => r.data.data),

  updateSource: (id: number, source: unknown) =>
    instance.put<ApiResult<unknown>>(`/sources/${id}`, source).then((r) => r.data.data),

  setSourceEnabled: (id: number, enabled: boolean) =>
    instance.patch<ApiResult<unknown>>(`/sources/${id}/enabled`, { enabled }).then((r) => r.data.data),

  deleteSource: (id: number) =>
    instance.delete<ApiResult<{ deleted: boolean }>>(`/sources/${id}`).then((r) => r.data.data),

  getDailyReport: (date: string) =>
    instance
      .get<ApiResult<unknown>>('/reports/daily', { params: { date } })
      .then((r) => r.data.data),

  getLatestDailyReport: () =>
    instance
      .get<ApiResult<unknown>>('/reports/daily/latest')
      .then((r) => r.data.data),

  getConversations: (page = 1, limit = 30) =>
    instance
      .get<ApiResult<{ total: number; items: Conversation[] }>>('/agent/conversations', {
        params: { page, limit },
      })
      .then((r) => r.data.data),

  getConversationMessages: (conversationId: number) =>
    instance
      .get<ApiResult<Message[]>>(`/agent/conversations/${conversationId}/messages`)
      .then((r) => r.data.data),
}

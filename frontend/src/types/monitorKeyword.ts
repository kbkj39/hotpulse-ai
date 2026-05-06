export interface MonitorKeyword {
  id: number
  keyword: string
  enabled: boolean
  /** 定时爬取间隔（小时），null 或 0 表示不定时 */
  crawlIntervalHours?: number | null
  /** 上次触发爬取的时间 */
  lastCrawledAt?: string | null
  createdAt: string
}

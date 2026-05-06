import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Layout } from '@/components/common/Layout'
import { HotspotsPage } from '@/pages/HotspotsPage'
import { ChatPage } from '@/pages/ChatPage'
import { SettingsPage } from '@/pages/SettingsPage'
import { DailyReportPage } from '@/pages/DailyReportPage'

export default function App() {
  return (
    <BrowserRouter basename="/hotpulse">
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Navigate to="/hotspots" replace />} />
          <Route path="hotspots" element={<HotspotsPage />} />
          <Route path="chat" element={<ChatPage />} />
          <Route path="settings" element={<SettingsPage />} />
          <Route path="report" element={<DailyReportPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

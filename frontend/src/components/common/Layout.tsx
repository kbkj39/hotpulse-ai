import { NavLink, Outlet } from 'react-router-dom'

const navLinks = [
  { to: '/hotspots', label: '热点雷达' },
  { to: '/chat', label: '智能对话' },
  { to: '/settings', label: '信息源' },
  { to: '/report', label: '日报' },
]

export function Layout() {
  return (
    <div className="min-h-screen" style={{ background: '#000', color: '#fff', fontFamily: "'Fira Sans', 'Noto Sans SC', sans-serif" }}>
      <nav
        style={{
          borderBottom: '2px solid #1F1F1F',
          padding: '0 24px',
          height: '52px',
          display: 'flex',
          alignItems: 'center',
          gap: '32px',
          background: '#000',
          position: 'sticky',
          top: 0,
          zIndex: 50,
        }}
      >
        <span
          style={{
            fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
            fontWeight: 600,
            fontSize: '17px',
            letterSpacing: '-0.02em',
            color: '#fff',
            marginRight: '8px',
          }}
        >
          HOTPULSE<span style={{ color: '#555', fontWeight: 400 }}>.AI</span>
        </span>
        {navLinks.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            style={({ isActive }) => ({
              fontFamily: "'Fira Code', 'Noto Sans SC', monospace",
              fontSize: '13px',
              fontWeight: isActive ? 500 : 400,
              color: isActive ? '#fff' : '#555',
              textDecoration: 'none',
              letterSpacing: '0.05em',
              textTransform: 'uppercase',
              borderBottom: isActive ? '2px solid #fff' : '2px solid transparent',
              paddingBottom: '2px',
              transition: 'color 150ms ease',
            })}
          >
            {link.label}
          </NavLink>
        ))}
      </nav>
      <main style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
        <Outlet />
      </main>
    </div>
  )
}

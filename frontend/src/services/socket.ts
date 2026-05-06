import { io, Socket } from 'socket.io-client'
import type { Hotspot } from '@/types/hotspot'

let socket: Socket | null = null

export const socketService = {
  connect: () => {
    if (!socket) {
      socket = io('/ws/hotspots', {
        path: `${import.meta.env.BASE_URL}socket.io`,
        transports: ['polling', 'websocket'],
        upgrade: true,
      })
      socket.on('connect', () => console.log('Socket.io connected'))
      socket.on('disconnect', () => console.log('Socket.io disconnected'))
      socket.on('connect_error', (err) => console.warn('Socket.io error:', err))
    }
    return socket
  },

  onNewHotspot: (callback: (hotspot: Hotspot) => void) => {
    const s = socketService.connect()
    s.on('newHotspot', callback)
    return () => s.off('newHotspot', callback)
  },

  disconnect: () => {
    socket?.disconnect()
    socket = null
  },
}

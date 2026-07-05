'use client'
import { useWebSocket } from '@/hooks/useWebSocket'

// useWebSocket() sets up ws.connect() plus every real-time listener
// (messages, typing, presence, incoming calls). It was fully built but
// never actually called anywhere in the app, so the STOMP client never
// connected — every ws.publish() (sendMessage, typingStarted, etc.) was
// silently no-op-ing because CipherLinkWS.connected was always false.
export function WebSocketProvider({ children }: { children: React.ReactNode }) {
  useWebSocket()
  return <>{children}</>
}

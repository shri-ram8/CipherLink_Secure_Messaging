'use client'
import { useWebSocket } from '@/hooks/useWebSocket'

export function Providers({ children }: { children: React.ReactNode }) {
  // Mounted once at the root so the STOMP connection is live across
  // every authenticated route (chat, calls, stories, activity, etc.),
  // not just the /chat/* subtree.
  useWebSocket()
  return <>{children}</>
}

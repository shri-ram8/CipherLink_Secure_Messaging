'use client'
import { motion } from 'framer-motion'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import {
  MessageCircle,
  CircleDot,
  Phone,
  User,
  Bell,
} from 'lucide-react'
import { useChatStore } from '@/store/chat'

const NAV_ITEMS = [
  { href: '/chat', icon: MessageCircle, label: 'Chats' },
  { href: '/stories', icon: CircleDot, label: 'Stories' },
  { href: '/calls', icon: Phone, label: 'Calls' },
  { href: '/activity', icon: Bell, label: 'Activity' },
  { href: '/profile', icon: User, label: 'Profile' },
]

export function BottomNav() {
  const pathname = usePathname()
  const conversations = useChatStore((s) => s.conversations)
  const totalUnread = conversations.reduce((sum, c) => sum + c.unreadCount, 0)

  return (
    <nav
      style={{
        position: 'fixed',
        bottom: 0,
        left: 0,
        right: 0,
        height: 'calc(64px + env(safe-area-inset-bottom))',
        paddingBottom: 'env(safe-area-inset-bottom)',
        background: 'rgba(10, 10, 15, 0.92)',
        backdropFilter: 'blur(24px) saturate(180%)',
        WebkitBackdropFilter: 'blur(24px) saturate(180%)',
        borderTop: '1px solid rgba(139, 92, 246, 0.12)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-around',
        paddingInline: 8,
        zIndex: 100,
      }}
    >
      {NAV_ITEMS.map(({ href, icon: Icon, label }) => {
        const isActive = pathname.startsWith(href)
        const showBadge = href === '/chat' && totalUnread > 0

        return (
          <Link
            key={href}
            href={href}
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 4,
              padding: '8px 12px',
              borderRadius: 12,
              position: 'relative',
              textDecoration: 'none',
              minWidth: 52,
            }}
          >
            <div style={{ position: 'relative' }}>
              <motion.div
                animate={{
                  scale: isActive ? 1.1 : 1,
                  color: isActive ? 'var(--accent-bright)' : 'var(--text-tertiary)',
                }}
                transition={{ type: 'spring', stiffness: 400, damping: 25 }}
                style={{ color: isActive ? 'var(--accent-bright)' : 'var(--text-tertiary)' }}
              >
                <Icon size={22} strokeWidth={isActive ? 2.2 : 1.8} />
              </motion.div>

              {showBadge && (
                <motion.span
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  style={{
                    position: 'absolute',
                    top: -4,
                    right: -6,
                    background: 'var(--accent)',
                    color: 'white',
                    fontSize: 10,
                    fontWeight: 700,
                    minWidth: 16,
                    height: 16,
                    borderRadius: 8,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: '0 4px',
                    border: '2px solid var(--bg-primary)',
                  }}
                >
                  {totalUnread > 99 ? '99+' : totalUnread}
                </motion.span>
              )}
            </div>

            {isActive && (
              <motion.span
                layoutId="navLabel"
                style={{
                  fontSize: 10,
                  fontWeight: 600,
                  color: 'var(--accent-bright)',
                  letterSpacing: '0.03em',
                }}
              >
                {label}
              </motion.span>
            )}

            {isActive && (
              <motion.div
                layoutId="navIndicator"
                style={{
                  position: 'absolute',
                  top: 0,
                  left: '50%',
                  transform: 'translateX(-50%)',
                  width: 20,
                  height: 2,
                  background: 'var(--accent)',
                  borderRadius: 1,
                  boxShadow: '0 0 8px var(--accent)',
                }}
                transition={{ type: 'spring', stiffness: 500, damping: 35 }}
              />
            )}
          </Link>
        )
      })}
    </nav>
  )
}

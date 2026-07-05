'use client'
import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { Avatar } from '@/components/ui/avatar-cl'
import { BottomNav } from '@/components/layout/BottomNav'
import {
  Eye, Heart, MessageCircle, Phone, AtSign, Bell
} from 'lucide-react'
import { formatDistanceToNow, parseISO } from 'date-fns'

// Activity types from backend notifications / events
type ActivityType =
  | 'STORY_VIEWED'
  | 'STORY_REACTED'
  | 'STORY_COMMENTED'
  | 'MESSAGE_REACTED'
  | 'MENTIONED_IN_GROUP'
  | 'MISSED_CALL'

interface ActivityItem {
  id: string
  type: ActivityType
  actorName: string
  actorAvatar?: string
  actorId: string
  payload?: string
  createdAt: string
  read: boolean
}

// Mock data — replace with real API when backend adds /api/v1/activity
const MOCK_ACTIVITY: ActivityItem[] = [
  { id: '1', type: 'STORY_VIEWED', actorName: 'Priya S.', actorId: 'u1', createdAt: new Date(Date.now() - 5 * 60000).toISOString(), read: false },
  { id: '2', type: 'STORY_REACTED', actorName: 'Arjun M.', actorId: 'u2', payload: '❤️', createdAt: new Date(Date.now() - 15 * 60000).toISOString(), read: false },
  { id: '3', type: 'MESSAGE_REACTED', actorName: 'Sneha R.', actorId: 'u3', payload: '😂', createdAt: new Date(Date.now() - 40 * 60000).toISOString(), read: true },
  { id: '4', type: 'MISSED_CALL', actorName: 'Rahul K.', actorId: 'u4', createdAt: new Date(Date.now() - 2 * 3600000).toISOString(), read: true },
  { id: '5', type: 'STORY_COMMENTED', actorName: 'Aisha B.', actorId: 'u5', payload: 'This is so cool! 🔥', createdAt: new Date(Date.now() - 5 * 3600000).toISOString(), read: true },
  { id: '6', type: 'MENTIONED_IN_GROUP', actorName: 'Dev Team', actorId: 'g1', payload: '@you check this out', createdAt: new Date(Date.now() - 86400000).toISOString(), read: true },
]

const ACTIVITY_META: Record<ActivityType, { icon: React.ReactNode; color: string; bg: string; text: (a: ActivityItem) => string }> = {
  STORY_VIEWED: {
    icon: <Eye size={16} />,
    color: '#8b5cf6',
    bg: 'rgba(139,92,246,0.12)',
    text: () => 'viewed your story',
  },
  STORY_REACTED: {
    icon: <Heart size={16} />,
    color: '#ef4444',
    bg: 'rgba(239,68,68,0.12)',
    text: (a) => `reacted ${a.payload} to your story`,
  },
  STORY_COMMENTED: {
    icon: <MessageCircle size={16} />,
    color: '#3b82f6',
    bg: 'rgba(59,130,246,0.12)',
    text: (a) => `replied to your story: "${a.payload}"`,
  },
  MESSAGE_REACTED: {
    icon: <Heart size={16} />,
    color: '#f59e0b',
    bg: 'rgba(245,158,11,0.12)',
    text: (a) => `reacted ${a.payload} to your message`,
  },
  MENTIONED_IN_GROUP: {
    icon: <AtSign size={16} />,
    color: '#10b981',
    bg: 'rgba(16,185,129,0.12)',
    text: (a) => `mentioned you in ${a.actorName}`,
  },
  MISSED_CALL: {
    icon: <Phone size={16} />,
    color: '#ef4444',
    bg: 'rgba(239,68,68,0.12)',
    text: () => 'called you — missed',
  },
}

export default function ActivityPage() {
  const [items, setItems] = useState<ActivityItem[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // Simulate API call — replace with real endpoint
    setTimeout(() => {
      setItems(MOCK_ACTIVITY)
      setLoading(false)
    }, 600)
  }, [])

  const unread = items.filter((i) => !i.read)
  const read = items.filter((i) => i.read)

  return (
    <div
      style={{
        height: '100dvh',
        background: 'var(--bg-primary)',
        display: 'flex',
        flexDirection: 'column',
        paddingTop: 'env(safe-area-inset-top)',
      }}
    >
      {/* Header */}
      <div style={{ padding: '20px 20px 16px', flexShrink: 0 }}>
        <h1
          style={{
            fontSize: 28,
            fontWeight: 800,
            letterSpacing: '-0.03em',
            background: 'linear-gradient(135deg, #f1f0ff, #a78bfa)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            backgroundClip: 'text',
          }}
        >
          Activity
        </h1>
      </div>

      <div style={{ flex: 1, overflowY: 'auto', paddingBottom: 80 }}>
        {loading ? (
          <ActivitySkeletons />
        ) : items.length === 0 ? (
          <EmptyState />
        ) : (
          <>
            {unread.length > 0 && (
              <Section title="New">
                {unread.map((item, i) => (
                  <ActivityRow key={item.id} item={item} index={i} />
                ))}
              </Section>
            )}
            {read.length > 0 && (
              <Section title="Earlier">
                {read.map((item, i) => (
                  <ActivityRow key={item.id} item={item} index={i} />
                ))}
              </Section>
            )}
          </>
        )}
      </div>

      <BottomNav />
    </div>
  )
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div style={{ marginBottom: 8 }}>
      <div style={{ padding: '8px 20px 6px' }}>
        <p style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-tertiary)', letterSpacing: '0.06em', textTransform: 'uppercase' }}>
          {title}
        </p>
      </div>
      {children}
    </div>
  )
}

function ActivityRow({ item, index }: { item: ActivityItem; index: number }) {
  const meta = ACTIVITY_META[item.type]

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.04, type: 'spring', stiffness: 400, damping: 30 }}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 14,
        padding: '12px 20px',
        background: !item.read ? 'rgba(139,92,246,0.04)' : 'transparent',
        position: 'relative',
      }}
    >
      {/* Unread dot */}
      {!item.read && (
        <div
          style={{
            position: 'absolute',
            left: 8,
            top: '50%',
            transform: 'translateY(-50%)',
            width: 6,
            height: 6,
            borderRadius: '50%',
            background: 'var(--accent)',
          }}
        />
      )}

      <div style={{ position: 'relative', flexShrink: 0 }}>
        <Avatar src={item.actorAvatar} name={item.actorName} size="md" />
        <div
          style={{
            position: 'absolute',
            bottom: -2,
            right: -2,
            width: 22,
            height: 22,
            borderRadius: '50%',
            background: meta.bg,
            border: '2px solid var(--bg-primary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: meta.color,
          }}
        >
          {meta.icon}
        </div>
      </div>

      <div style={{ flex: 1, minWidth: 0 }}>
        <p style={{ fontSize: 14, color: 'var(--text-primary)', lineHeight: 1.4 }}>
          <span style={{ fontWeight: 700 }}>{item.actorName}</span>{' '}
          <span style={{ color: 'var(--text-secondary)' }}>{meta.text(item)}</span>
        </p>
        <p style={{ fontSize: 12, color: 'var(--text-tertiary)', marginTop: 3 }}>
          {formatDistanceToNow(parseISO(item.createdAt), { addSuffix: true })}
        </p>
      </div>
    </motion.div>
  )
}

function ActivitySkeletons() {
  return (
    <div>
      {[...Array(7)].map((_, i) => (
        <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '12px 20px' }}>
          <div style={{ width: 44, height: 44, borderRadius: '50%', background: 'var(--surface-2)', animation: 'pulse 1.5s infinite', flexShrink: 0 }} />
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 8 }}>
            <div style={{ height: 13, width: '75%', background: 'var(--surface-2)', borderRadius: 6, animation: 'pulse 1.5s infinite' }} />
            <div style={{ height: 11, width: '35%', background: 'var(--surface-2)', borderRadius: 5, animation: 'pulse 1.5s infinite' }} />
          </div>
        </div>
      ))}
    </div>
  )
}

function EmptyState() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '50vh', gap: 12 }}>
      <div style={{ width: 64, height: 64, background: 'var(--surface-2)', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Bell size={26} color="var(--text-tertiary)" />
      </div>
      <p style={{ fontSize: 16, fontWeight: 500, color: 'var(--text-secondary)' }}>No activity yet</p>
      <p style={{ fontSize: 14, color: 'var(--text-tertiary)', textAlign: 'center', maxWidth: 260 }}>
        Reactions, story views, and mentions will show up here
      </p>
    </div>
  )
}

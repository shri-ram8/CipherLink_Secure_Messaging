'use client'
import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { callApi } from '@/lib/api'
import { useAuthStore } from '@/store/auth'
import { useChatStore } from '@/store/chat'
import { useCallStore } from '@/store/calls'
import { ws } from '@/lib/websocket'
import { Avatar } from '@/components/ui/avatar-cl'
import { BottomNav } from '@/components/layout/BottomNav'
import { formatCallDuration } from '@/lib/utils'
import { Phone, Video, PhoneMissed, PhoneIncoming, PhoneOutgoing, PhoneCall } from 'lucide-react'
import type { CallRecord } from '@/types'
import { toast } from '@/components/ui/toaster'

export default function CallsPage() {
  const { userId } = useAuthStore()
  const { conversations } = useChatStore()
  const { setOutgoing } = useCallStore()
  const [history, setHistory] = useState<CallRecord[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadHistory()
  }, [])

  async function loadHistory() {
    try {
      const data = await callApi.getHistory() as CallRecord[]
      setHistory(data || [])
    } finally {
      setLoading(false)
    }
  }

  async function callBack(record: CallRecord) {
    try {
      const res = await callApi.initiateCall(
        record.isIncoming ? record.callerId : record.receiverId,
        record.callType
      ) as { callId: string; roomId: string }
      setOutgoing(
        record.isIncoming ? record.callerId : record.receiverId,
        record.otherUserName,
        record.otherUserAvatarUrl,
        record.callType,
        res.callId
      )
      ws.initiateCall({
        receiverId: record.isIncoming ? record.callerId : record.receiverId,
        callType: record.callType,
        roomId: res.roomId,
      })
    } catch {
      toast('Failed to start call', 'error')
    }
  }

  const grouped = groupCallsByDate(history)

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
          Calls
        </h1>
      </div>

      <div style={{ flex: 1, overflowY: 'auto', paddingBottom: 80 }}>
        {loading ? (
          <CallSkeletons />
        ) : history.length === 0 ? (
          <EmptyState />
        ) : (
          Object.entries(grouped).map(([date, records]) => (
            <div key={date}>
              <div style={{ padding: '12px 20px 6px' }}>
                <p style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-tertiary)', letterSpacing: '0.06em', textTransform: 'uppercase' }}>
                  {date}
                </p>
              </div>
              {records.map((record, i) => (
                <CallRow key={record.callId} record={record} index={i} onCallBack={() => callBack(record)} />
              ))}
            </div>
          ))
        )}
      </div>

      <BottomNav />
    </div>
  )
}

function CallRow({ record, index, onCallBack }: { record: CallRecord; index: number; onCallBack: () => void }) {
  const isMissed = record.status === 'MISSED'

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.04 }}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 14,
        padding: '12px 20px',
      }}
    >
      <Avatar src={record.otherUserAvatarUrl} name={record.otherUserName} size="md" />

      <div style={{ flex: 1, minWidth: 0 }}>
        <p style={{ fontSize: 15, fontWeight: 600, color: isMissed ? '#ef4444' : 'var(--text-primary)', letterSpacing: '-0.01em' }}>
          {record.otherUserName}
        </p>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 3 }}>
          <CallDirectionIcon record={record} />
          <span style={{ fontSize: 13, color: 'var(--text-tertiary)' }}>
            {record.callType === 'VIDEO' ? 'Video' : 'Voice'} ·{' '}
            {record.durationSeconds ? formatCallDuration(record.durationSeconds) : record.status.toLowerCase()}
          </span>
        </div>
      </div>

      <motion.button
        whileHover={{ scale: 1.1 }}
        whileTap={{ scale: 0.9 }}
        onClick={onCallBack}
        style={{
          width: 40,
          height: 40,
          background: 'var(--accent-ultra-dim)',
          border: '1px solid var(--border-subtle)',
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          cursor: 'pointer',
          color: 'var(--accent-bright)',
        }}
      >
        {record.callType === 'VIDEO' ? <Video size={17} /> : <Phone size={17} />}
      </motion.button>
    </motion.div>
  )
}

function CallDirectionIcon({ record }: { record: CallRecord }) {
  const size = 13
  if (record.status === 'MISSED') return <PhoneMissed size={size} color="#ef4444" />
  if (record.isIncoming) return <PhoneIncoming size={size} color="#10b981" />
  return <PhoneOutgoing size={size} color="var(--accent-bright)" />
}

function groupCallsByDate(records: CallRecord[]): Record<string, CallRecord[]> {
  const groups: Record<string, CallRecord[]> = {}
  const today = new Date().toDateString()
  const yesterday = new Date(Date.now() - 86400000).toDateString()

  records.forEach((r) => {
    const d = new Date(r.startedAt)
    let key = d.toDateString()
    if (key === today) key = 'Today'
    else if (key === yesterday) key = 'Yesterday'
    else key = d.toLocaleDateString('en-US', { month: 'long', day: 'numeric' })

    if (!groups[key]) groups[key] = []
    groups[key].push(r)
  })

  return groups
}

function CallSkeletons() {
  return (
    <div>
      {[...Array(6)].map((_, i) => (
        <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '12px 20px' }}>
          <div style={{ width: 44, height: 44, borderRadius: '50%', background: 'var(--surface-2)', animation: 'pulse 1.5s infinite', flexShrink: 0 }} />
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 8 }}>
            <div style={{ height: 14, width: '40%', background: 'var(--surface-2)', borderRadius: 7, animation: 'pulse 1.5s infinite' }} />
            <div style={{ height: 11, width: '55%', background: 'var(--surface-2)', borderRadius: 5, animation: 'pulse 1.5s infinite' }} />
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
        <Phone size={26} color="var(--text-tertiary)" />
      </div>
      <p style={{ fontSize: 16, fontWeight: 500, color: 'var(--text-secondary)' }}>No calls yet</p>
      <p style={{ fontSize: 14, color: 'var(--text-tertiary)' }}>Your call history will appear here</p>
    </div>
  )
}

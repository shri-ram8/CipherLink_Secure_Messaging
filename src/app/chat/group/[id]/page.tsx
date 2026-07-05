'use client'
import { useEffect, useRef, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useParams, useRouter } from 'next/navigation'
import { groupApi } from '@/lib/api'
import { useChatStore } from '@/store/chat'
import { useAuthStore } from '@/store/auth'
import { ws } from '@/lib/websocket'
import { Avatar } from '@/components/ui/avatar-cl'
import { formatMessageTime, EMOJIS, vibrate } from '@/lib/utils'
import {
  ArrowLeft, Send, Paperclip, Mic, MoreVertical,
  Users, SmilePlus, Reply, Trash2, Copy, X
} from 'lucide-react'
import type { Group, GroupMessage } from '@/types'
import { toast } from '@/components/ui/toaster'

export default function GroupChatPage() {
  const { id } = useParams() as { id: string }
  const router = useRouter()
  const { userId } = useAuthStore()
  const { groups, groupMessages, setGroupMessages, addGroupMessage } = useChatStore()

  const [text, setText] = useState('')
  const [loading, setLoading] = useState(true)
  const [group, setGroup] = useState<Group | null>(null)
  const [contextMsg, setContextMsg] = useState<GroupMessage | null>(null)
  const [showEmojis, setShowEmojis] = useState(false)
  const [replyTo, setReplyTo] = useState<GroupMessage | null>(null)

  const bottomRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const typingTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)

  const msgs = groupMessages[id] || []

  useEffect(() => {
    loadGroup()
    loadMessages()
  }, [id])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [msgs.length])

  async function loadGroup() {
    const myGroups = groups
    const found = myGroups.find((g) => g.id === id)
    if (found) {
      setGroup(found)
      return
    }
    try {
      const data = await groupApi.getMembers(id) as { group: Group }
      setGroup(data?.group || null)
    } catch {}
  }

  async function loadMessages() {
    try {
      const data = await groupApi.getMessages(id) as { content: GroupMessage[] }
      setGroupMessages(id, data?.content || [])
    } finally {
      setLoading(false)
    }
  }

  function handleSend() {
    const payload = text.trim()
    if (!payload) return
    ws.sendGroupMessage({
      groupId: id,
      encryptedPayload: payload,
      replyToMessageId: replyTo?.id,
    })
    setText('')
    setReplyTo(null)
    vibrate(10)
  }

  function handleLongPress(msg: GroupMessage) {
    vibrate([5, 10])
    setContextMsg(msg)
  }

  async function handleCopy() {
    if (!contextMsg) return
    await navigator.clipboard.writeText(contextMsg.encryptedPayload || '')
    toast('Copied', 'success')
    setContextMsg(null)
  }

  const isMine = (msg: GroupMessage) => msg.senderUserId === userId

  return (
    <div
      style={{
        height: '100dvh',
        display: 'flex',
        flexDirection: 'column',
        background: 'var(--bg-primary)',
        paddingTop: 'env(safe-area-inset-top)',
      }}
    >
      {/* Header */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          padding: '12px 16px',
          background: 'rgba(10,10,15,0.92)',
          backdropFilter: 'blur(20px)',
          borderBottom: '1px solid var(--border-subtle)',
          flexShrink: 0,
        }}
      >
        <button
          onClick={() => router.back()}
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)', padding: 4 }}
        >
          <ArrowLeft size={22} />
        </button>

        <Avatar src={group?.groupPictureUrl} name={group?.name} size="md" />

        <div style={{ flex: 1, minWidth: 0 }}>
          <p style={{ fontSize: 16, fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.01em' }}>
            {group?.name || '…'}
          </p>
          <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginTop: 1 }}>
            <Users size={11} color="var(--text-tertiary)" />
            <p style={{ fontSize: 12, color: 'var(--text-tertiary)' }}>
              {group?.memberCount || 0} members
            </p>
          </div>
        </div>

        <button
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)', padding: 4 }}
          onClick={() => router.push(`/chat/group/${id}/info`)}
        >
          <MoreVertical size={20} />
        </button>
      </div>

      {/* Messages */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '12px 0' }}>
        {loading ? (
          <GroupMessageSkeletons />
        ) : msgs.length === 0 ? (
          <EmptyGroupChat groupName={group?.name} />
        ) : (
          msgs.map((msg, i) => {
            const mine = isMine(msg)
            const prevMsg = msgs[i - 1]
            const showSender = !mine && (!prevMsg || prevMsg.senderUserId !== msg.senderUserId)

            return (
              <motion.div
                key={msg.id}
                initial={{ opacity: 0, y: 6 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ type: 'spring', stiffness: 400, damping: 30 }}
                onContextMenu={(e) => { e.preventDefault(); handleLongPress(msg) }}
                onTouchStart={() => {
                  const t = setTimeout(() => handleLongPress(msg), 500)
                  const clear = () => clearTimeout(t)
                  document.addEventListener('touchend', clear, { once: true })
                  document.addEventListener('touchmove', clear, { once: true })
                }}
                style={{
                  display: 'flex',
                  justifyContent: mine ? 'flex-end' : 'flex-start',
                  padding: `${showSender ? 8 : 2}px 12px`,
                  gap: 8,
                  alignItems: 'flex-end',
                }}
              >
                {!mine && showSender && (
                  <Avatar name={msg.senderName} size="xs" />
                )}
                {!mine && !showSender && <div style={{ width: 28 }} />}

                <div style={{ maxWidth: '76%' }}>
                  {showSender && !mine && (
                    <p style={{ fontSize: 12, fontWeight: 600, color: 'var(--accent-bright)', marginBottom: 4, paddingLeft: 4 }}>
                      {msg.senderName}
                    </p>
                  )}
                  <div
                    className={mine ? 'bubble-mine' : 'bubble-other'}
                    style={{ padding: '10px 14px' }}
                  >
                    <p style={{ fontSize: 15, lineHeight: 1.5, color: mine ? 'white' : 'var(--text-primary)', wordBreak: 'break-word' }}>
                      {msg.encryptedPayload}
                    </p>
                    <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 4 }}>
                      <span style={{ fontSize: 11, color: mine ? 'rgba(255,255,255,0.55)' : 'var(--text-tertiary)' }}>
                        {formatMessageTime(msg.sentAt)}
                      </span>
                    </div>
                  </div>
                </div>
              </motion.div>
            )
          })
        )}
        <div ref={bottomRef} />
      </div>

      {/* Reply preview */}
      <AnimatePresence>
        {replyTo && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            style={{
              background: 'var(--surface-2)',
              borderTop: '1px solid var(--border-subtle)',
              padding: '10px 16px',
              display: 'flex',
              alignItems: 'center',
              gap: 10,
            }}
          >
            <div style={{ flex: 1 }}>
              <p style={{ fontSize: 12, color: 'var(--accent-bright)', fontWeight: 600 }}>
                Replying to {replyTo.senderName}
              </p>
              <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {replyTo.encryptedPayload?.slice(0, 60)}
              </p>
            </div>
            <button
              onClick={() => setReplyTo(null)}
              style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-tertiary)' }}
            >
              <X size={18} />
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Input */}
      <div
        style={{
          padding: '10px 12px',
          paddingBottom: 'calc(env(safe-area-inset-bottom) + 10px)',
          background: 'rgba(10,10,15,0.92)',
          backdropFilter: 'blur(20px)',
          borderTop: '1px solid var(--border-subtle)',
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          flexShrink: 0,
        }}
      >
        <button style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-tertiary)', padding: 6 }}>
          <Paperclip size={21} />
        </button>

        <div
          style={{
            flex: 1,
            background: 'var(--surface-2)',
            border: '1px solid var(--border-subtle)',
            borderRadius: 22,
            display: 'flex',
            alignItems: 'center',
            padding: '8px 14px',
          }}
        >
          <input
            ref={inputRef}
            value={text}
            onChange={(e) => setText(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
            placeholder="Message"
            style={{
              flex: 1,
              background: 'none',
              border: 'none',
              outline: 'none',
              fontSize: 15,
              color: 'var(--text-primary)',
            }}
          />
        </div>

        <motion.button
          whileTap={{ scale: 0.9 }}
          onClick={handleSend}
          style={{
            width: 42,
            height: 42,
            background: text.trim() ? 'linear-gradient(135deg, #7c3aed, #8b5cf6)' : 'var(--surface-3)',
            border: 'none',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            transition: 'background 0.2s',
            flexShrink: 0,
          }}
        >
          {text.trim() ? <Send size={18} color="white" /> : <Mic size={18} color="var(--text-tertiary)" />}
        </motion.button>
      </div>

      {/* Context menu */}
      <AnimatePresence>
        {contextMsg && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => { setContextMsg(null); setShowEmojis(false) }}
            style={{
              position: 'fixed',
              inset: 0,
              background: 'rgba(0,0,0,0.6)',
              backdropFilter: 'blur(4px)',
              zIndex: 200,
              display: 'flex',
              alignItems: 'flex-end',
              justifyContent: 'center',
              padding: '0 16px 32px',
            }}
          >
            <motion.div
              initial={{ y: 40, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              exit={{ y: 40, opacity: 0 }}
              onClick={(e) => e.stopPropagation()}
              style={{
                background: 'var(--surface-2)',
                border: '1px solid var(--border-default)',
                borderRadius: 20,
                padding: 8,
                width: '100%',
                maxWidth: 380,
                boxShadow: '0 -8px 40px rgba(0,0,0,0.5)',
              }}
            >
              {[
                {
                  icon: <Reply size={18} />,
                  label: 'Reply',
                  action: () => { setReplyTo(contextMsg); setContextMsg(null); inputRef.current?.focus() },
                },
                {
                  icon: <Copy size={18} />,
                  label: 'Copy',
                  action: handleCopy,
                },
              ].map(({ icon, label, action }) => (
                <button
                  key={label}
                  onClick={action}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 14,
                    width: '100%',
                    padding: '13px 16px',
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    borderRadius: 12,
                    color: 'var(--text-primary)',
                    fontSize: 15,
                    fontWeight: 500,
                  }}
                >
                  {icon}
                  {label}
                </button>
              ))}
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

function GroupMessageSkeletons() {
  return (
    <div style={{ padding: '12px 16px', display: 'flex', flexDirection: 'column', gap: 14 }}>
      {[...Array(5)].map((_, i) => (
        <div key={i} style={{ display: 'flex', justifyContent: i % 3 === 0 ? 'flex-end' : 'flex-start', gap: 8, alignItems: 'flex-end' }}>
          {i % 3 !== 0 && <div style={{ width: 28, height: 28, borderRadius: '50%', background: 'var(--surface-2)', animation: 'pulse 1.5s infinite' }} />}
          <div style={{ height: 44, width: `${120 + Math.random() * 100}px`, background: 'var(--surface-2)', borderRadius: 14, animation: 'pulse 1.5s infinite' }} />
        </div>
      ))}
    </div>
  )
}

function EmptyGroupChat({ groupName }: { groupName?: string }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '60%', gap: 12, padding: 24 }}>
      <div style={{ width: 56, height: 56, background: 'var(--surface-2)', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Users size={24} color="var(--text-tertiary)" />
      </div>
      <p style={{ fontSize: 16, fontWeight: 600, color: 'var(--text-secondary)' }}>
        Welcome to {groupName || 'the group'}!
      </p>
      <p style={{ fontSize: 14, color: 'var(--text-tertiary)', textAlign: 'center' }}>
        Be the first to say something
      </p>
    </div>
  )
}

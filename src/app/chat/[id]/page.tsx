'use client'
import { useEffect, useRef, useState } from 'react'
import { motion, AnimatePresence, useMotionValue, useTransform } from 'framer-motion'
import { useParams, useRouter } from 'next/navigation'
import { messageApi, callApi } from '@/lib/api'
import { useChatStore } from '@/store/chat'
import { useAuthStore } from '@/store/auth'
import { useCallStore } from '@/store/calls'
import { ws } from '@/lib/websocket'
import { Avatar } from '@/components/ui/avatar-cl'
import { formatMessageTime, formatLastSeen, EMOJIS, vibrate } from '@/lib/utils'
import {
  ArrowLeft, Phone, Video, Search,
  Send, Paperclip, Mic,
  Reply, Edit2, Trash2, Copy,
  Forward, Pin, SmilePlus, X, MapPin,
  Clock, Check, CheckCheck
} from 'lucide-react'
import type { Message, Conversation } from '@/types'
import { toast } from '@/components/ui/toaster'
import { CallOverlay } from '@/components/calls/CallOverlay'

type ContextAction = 'reply' | 'react' | 'edit' | 'forward' | 'pin' | 'copy' | 'delete'

export default function ChatPage() {
  const params = useParams()
  const id = params.id as string
  const router = useRouter()
  const { userId } = useAuthStore()
  const { conversations, messages, typingUsers, setMessages, addMessage, reconcileMessage, setActiveConversation, clearUnread } = useChatStore()
  const { setOutgoing } = useCallStore()

  const [text, setText] = useState('')
  const [loading, setLoading] = useState(true)
  const [replyTo, setReplyTo] = useState<Message | null>(null)
  const [editingMsg, setEditingMsg] = useState<Message | null>(null)
  const [contextMsg, setContextMsg] = useState<Message | null>(null)
  const [showEmojis, setShowEmojis] = useState(false)

  const bottomRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const typingTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)

  const conv = conversations.find((c) => c.id === id)
  const msgs = messages[id] || []
  const isTyping = conv ? typingUsers[conv.otherUserId] : false

  // Subscribe to real-time message events for THIS conversation
  useEffect(() => {
    const unsubs = [
      ws.on('MessageSent', (data) => {
        const msg = data.data as Message
        const clientTempId = (data as { clientTempId?: string }).clientTempId
        if (!msg || msg.conversationId !== id) return
        if (clientTempId) reconcileMessage(clientTempId, msg)
        else addMessage(msg)
      }),
      ws.on('ReceiveMessage', (data) => {
        const msg = data.data as Message
        if (msg && msg.conversationId === id) addMessage(msg)
      }),
      ws.on('MessageEdited', (data) => {
        const { messageId, newPayload } = data as { messageId: string; newPayload: string }
        if (messageId) useChatStore.getState().updateMessage(messageId, { encryptedPayload: newPayload, isEdited: true })
      }),
      ws.on('MessageDeleted', (data) => {
        const { messageId, conversationId } = data as { messageId: string; conversationId: string }
        if (messageId && conversationId === id) useChatStore.getState().removeMessage(messageId, conversationId)
      }),
      ws.on('MessageSeen', (data) => {
        const { messageId } = data as { messageId: string }
        if (messageId) useChatStore.getState().updateMessage(messageId, { status: 'SEEN' })
      }),
    ]
    return () => unsubs.forEach((u) => u())
  }, [id])

  useEffect(() => {
    setActiveConversation(id)
    clearUnread(id)
    loadMessages()
    return () => setActiveConversation(null)
  }, [id])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [msgs.length])

  async function loadMessages() {
    try {
      const data = await messageApi.getMessages(id, 1, 50) as { items: Message[], content?: Message[] }
      setMessages(id, data?.items || data?.content || [])
      messageApi.markSeen(id)
    } finally {
      setLoading(false)
    }
  }

  function handleTyping() {
    ws.typingStarted(id)
    clearTimeout(typingTimer.current)
    typingTimer.current = setTimeout(() => ws.typingStopped(id), 2000)
  }

  function handleSend() {
    const payload = text.trim()
    if (!payload && !editingMsg) return

    if (editingMsg) {
      ws.editMessage(editingMsg.id, payload)
      setEditingMsg(null)
      setText('')
      return
    }

    // Show the message immediately, like WhatsApp does — don't wait for
    // the server round-trip. A temp id lets us swap this for the real,
    // server-confirmed message once the "MessageSent" ack arrives.
    const clientTempId = `temp-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
    addMessage({
      id: clientTempId,
      conversationId: id,
      senderDeviceId: '',
      encryptedPayload: payload,
      isMine: true,
      status: 'SENDING',
      sentAt: new Date().toISOString(),
      isDeleted: false,
      isEdited: false,
      isPinned: false,
      replyToMessageId: replyTo?.id,
      replyPreview: replyTo?.encryptedPayload?.slice(0, 80),
    })

    ws.sendMessage({
      conversationId: id,
      encryptedPayload: payload,
      replyToMessageId: replyTo?.id,
      replyPreview: replyTo?.encryptedPayload?.slice(0, 80),
      clientTempId,
    })

    setText('')
    setReplyTo(null)
    vibrate(10)
  }

  function handleLongPress(msg: Message) {
    vibrate([5, 10])
    setContextMsg(msg)
  }

  async function handleContextAction(action: ContextAction, msg: Message) {
    setContextMsg(null)
    setShowEmojis(false)

    switch (action) {
      case 'reply':
        setReplyTo(msg)
        inputRef.current?.focus()
        break
      case 'edit':
        setEditingMsg(msg)
        setText(msg.encryptedPayload || '')
        inputRef.current?.focus()
        break
      case 'delete':
        ws.deleteMessage(msg.id)
        break
      case 'copy':
        await navigator.clipboard.writeText(msg.encryptedPayload || '')
        toast('Copied', 'success')
        break
      case 'pin':
        await messageApi.pinMessage(msg.id, !msg.isPinned)
        toast(msg.isPinned ? 'Unpinned' : 'Pinned', 'success')
        break
      case 'react':
        setShowEmojis(true)
        break
    }
  }

  function handleReact(emoji: string) {
    if (!contextMsg) return
    ws.react(contextMsg.id, emoji)
    setContextMsg(null)
    setShowEmojis(false)
  }

  async function startCall(type: 'VOICE' | 'VIDEO') {
    if (!conv) return
    try {
      const res = await callApi.initiateCall(conv.otherUserId, type) as { callId: string; roomId: string }
      // Just set state - CallOverlay's useEffect handles WebRTC setup + ws.initiateCall with SDP offer
      setOutgoing(conv.otherUserId, conv.otherUserName, conv.otherUserAvatarUrl, type, res.callId)
    } catch {
      toast('Failed to start call', 'error')
    }
  }

  const groupedMsgs = groupMessages(msgs)
  const lastMsg = msgs[msgs.length - 1]
  const showSeenLabel = !!lastMsg && lastMsg.isMine && lastMsg.status === 'SEEN'

  return (
    <div style={{ height: '100dvh', display: 'flex', flexDirection: 'column', background: 'var(--bg-primary)', paddingTop: 'env(safe-area-inset-top)' }}>
      <CallOverlay />

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px 16px', background: 'rgba(10,10,15,0.92)', backdropFilter: 'blur(20px)', borderBottom: '1px solid var(--border-subtle)', flexShrink: 0 }}>
        <button onClick={() => router.back()} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)', display: 'flex', padding: 4 }}>
          <ArrowLeft size={22} />
        </button>
        <Avatar src={conv?.otherUserAvatarUrl} name={conv?.otherUserName} size="md" isOnline={conv?.isOnline} />
        <div style={{ flex: 1, minWidth: 0 }}>
          <p style={{ fontSize: 16, fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.01em' }}>
            {conv?.otherUserName || conv?.otherUserPhone || '…'}
          </p>
          <p style={{ fontSize: 12, color: isTyping ? 'var(--accent-bright)' : 'var(--text-tertiary)', marginTop: 1 }}>
            {isTyping ? 'typing…' : conv?.isOnline ? 'Online' : formatLastSeen(conv?.lastSeen)}
          </p>
        </div>
        <div style={{ display: 'flex', gap: 4 }}>
          <HeaderBtn icon={<Phone size={19} />} onClick={() => startCall('VOICE')} />
          <HeaderBtn icon={<Video size={19} />} onClick={() => startCall('VIDEO')} />
          <HeaderBtn icon={<Search size={19} />} onClick={() => {}} />
        </div>
      </div>

      {/* Messages */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '12px 0', display: 'flex', flexDirection: 'column' }}>
        {loading ? <MessageSkeletons /> : (
          groupedMsgs.map((group, gi) => (
            <div key={gi} style={{ marginBottom: 2 }}>
              {group.showDate && <DateDivider date={group.messages[0].sentAt} />}
              {group.messages.map((msg, mi) => (
                <MessageBubble
                  key={msg.id}
                  msg={msg}
                  isFirst={mi === 0}
                  isLast={mi === group.messages.length - 1}
                  onLongPress={() => handleLongPress(msg)}
                  onDoubleTap={() => { setContextMsg(msg); setShowEmojis(true) }}
                  onReplySwipe={() => { setReplyTo(msg); inputRef.current?.focus() }}
                />
              ))}
            </div>
          ))
        )}
        {isTyping && <TypingIndicator />}
        {showSeenLabel && !isTyping && (
          <div style={{ display: 'flex', justifyContent: 'flex-end', padding: '2px 16px 0' }}>
            <span style={{ fontSize: 11, color: 'var(--text-tertiary)' }}>Seen</span>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* Reply / Edit preview */}
      <AnimatePresence>
        {(replyTo || editingMsg) && (
          <motion.div
            initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }}
            style={{ background: 'var(--surface-2)', borderTop: '1px solid var(--border-subtle)', padding: '10px 16px', display: 'flex', alignItems: 'center', gap: 10 }}
          >
            <div style={{ flex: 1 }}>
              <p style={{ fontSize: 12, color: 'var(--accent-bright)', fontWeight: 600 }}>
                {editingMsg ? 'Edit message' : 'Replying to'}
              </p>
              <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {(editingMsg || replyTo)?.encryptedPayload?.slice(0, 60)}
              </p>
            </div>
            <button onClick={() => { setReplyTo(null); setEditingMsg(null); setText('') }} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-tertiary)' }}>
              <X size={18} />
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Input */}
      <div style={{ padding: '10px 12px', paddingBottom: 'calc(env(safe-area-inset-bottom) + 10px)', background: 'rgba(10,10,15,0.92)', backdropFilter: 'blur(20px)', borderTop: '1px solid var(--border-subtle)', display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
        <button style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-tertiary)', padding: 6 }}>
          <Paperclip size={21} />
        </button>
        <div style={{ flex: 1, background: 'var(--surface-2)', border: '1px solid var(--border-subtle)', borderRadius: 22, display: 'flex', alignItems: 'center', padding: '8px 14px' }}>
          <input
            ref={inputRef} value={text}
            onChange={(e) => { setText(e.target.value); handleTyping() }}
            onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
            placeholder="Message"
            style={{ flex: 1, background: 'none', border: 'none', outline: 'none', fontSize: 15, color: 'var(--text-primary)' }}
          />
        </div>
        <motion.button
          whileTap={{ scale: 0.9 }} onClick={handleSend}
          style={{ width: 42, height: 42, background: text.trim() ? 'linear-gradient(135deg, #7c3aed, #8b5cf6)' : 'var(--surface-3)', border: 'none', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', transition: 'background 0.2s', flexShrink: 0 }}
        >
          {text.trim() ? <Send size={18} color="white" /> : <Mic size={18} color="var(--text-tertiary)" />}
        </motion.button>
      </div>

      {/* Context Menu */}
      <AnimatePresence>
        {contextMsg && (
          <motion.div
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            onClick={() => { setContextMsg(null); setShowEmojis(false) }}
            style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(4px)', zIndex: 200, display: 'flex', alignItems: 'flex-end', justifyContent: 'center', padding: '0 16px 32px' }}
          >
            <motion.div
              initial={{ y: 40, opacity: 0 }} animate={{ y: 0, opacity: 1 }} exit={{ y: 40, opacity: 0 }}
              onClick={(e) => e.stopPropagation()}
              style={{ background: 'var(--surface-2)', border: '1px solid var(--border-default)', borderRadius: 20, padding: 8, width: '100%', maxWidth: 380, boxShadow: '0 -8px 40px rgba(0,0,0,0.5)' }}
            >
              {showEmojis && (
                <motion.div
                  initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}
                  style={{ display: 'flex', justifyContent: 'space-around', padding: '12px 8px', borderBottom: '1px solid var(--border-subtle)', marginBottom: 4 }}
                >
                  {EMOJIS.map((emoji) => (
                    <motion.button key={emoji} whileHover={{ scale: 1.3 }} whileTap={{ scale: 0.9 }} onClick={() => handleReact(emoji)}
                      style={{ background: 'none', border: 'none', fontSize: 28, cursor: 'pointer', padding: 4 }}>
                      {emoji}
                    </motion.button>
                  ))}
                </motion.div>
              )}
              {([
                { action: 'reply' as ContextAction, icon: <Reply size={18} />, label: 'Reply' },
                { action: 'react' as ContextAction, icon: <SmilePlus size={18} />, label: 'React' },
                ...(contextMsg.isMine ? [{ action: 'edit' as ContextAction, icon: <Edit2 size={18} />, label: 'Edit' }] : []),
                { action: 'copy' as ContextAction, icon: <Copy size={18} />, label: 'Copy' },
                { action: 'forward' as ContextAction, icon: <Forward size={18} />, label: 'Forward' },
                { action: 'pin' as ContextAction, icon: <Pin size={18} />, label: contextMsg.isPinned ? 'Unpin' : 'Pin' },
                ...(contextMsg.isMine ? [{ action: 'delete' as ContextAction, icon: <Trash2 size={18} />, label: 'Delete', danger: true }] : []),
              ] as Array<{ action: ContextAction; icon: React.ReactNode; label: string; danger?: boolean }>).map(({ action, icon, label, danger }) => (
                <button key={action} onClick={() => handleContextAction(action, contextMsg)}
                  style={{ display: 'flex', alignItems: 'center', gap: 14, width: '100%', padding: '13px 16px', background: 'none', border: 'none', cursor: 'pointer', borderRadius: 12, color: danger ? 'var(--danger)' : 'var(--text-primary)', fontSize: 15, fontWeight: 500 }}>
                  {icon}{label}
                </button>
              ))}
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

function MessageBubble({ msg, isFirst, isLast, onLongPress, onDoubleTap, onReplySwipe }: {
  msg: Message; isFirst: boolean; isLast: boolean
  onLongPress: () => void; onDoubleTap: () => void; onReplySwipe: () => void
}) {
  const longPressTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)
  const tapCount = useRef(0)
  const tapTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)
  const x = useMotionValue(0)
  const replyOpacity = useTransform(x, [0, 40], [0, 1])
  const isMine = msg.isMine

  function handleTouchStart() {
    longPressTimer.current = setTimeout(() => { onLongPress(); vibrate(15) }, 500)
  }
  function handleTouchEnd() { clearTimeout(longPressTimer.current) }
  function handleClick() {
    tapCount.current++
    clearTimeout(tapTimer.current)
    tapTimer.current = setTimeout(() => { if (tapCount.current >= 2) onDoubleTap(); tapCount.current = 0 }, 300)
  }

  return (
    <div style={{ display: 'flex', justifyContent: isMine ? 'flex-end' : 'flex-start', padding: `${isFirst ? 6 : 1}px 12px`, position: 'relative' }}>
      {!isMine && (
        <motion.div style={{ position: 'absolute', left: 20, top: '50%', transform: 'translateY(-50%)', opacity: replyOpacity }}>
          <Reply size={16} color="var(--accent-bright)" />
        </motion.div>
      )}
      <motion.div
        drag={!isMine ? 'x' : false}
        dragConstraints={{ left: 0, right: 60 }}
        dragElastic={0.3}
        style={{ x, maxWidth: '78%', cursor: 'pointer' }}
        onDragEnd={() => { if (x.get() > 35) { onReplySwipe(); vibrate(10) }; x.set(0) }}
        onTouchStart={handleTouchStart}
        onTouchEnd={handleTouchEnd}
        onClick={handleClick}
        initial={{ opacity: 0, scale: 0.92, y: 4 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        transition={{ type: 'spring', stiffness: 400, damping: 30 }}
      >
        {msg.replyPreview && (
          <div style={{ background: isMine ? 'rgba(255,255,255,0.1)' : 'var(--surface-3)', borderLeft: `3px solid ${isMine ? 'rgba(255,255,255,0.4)' : 'var(--accent)'}`, borderRadius: 8, padding: '6px 10px', marginBottom: 4, fontSize: 12, color: isMine ? 'rgba(255,255,255,0.7)' : 'var(--text-secondary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {msg.replyPreview}
          </div>
        )}
        <div className={isMine ? 'bubble-mine' : 'bubble-other'} style={{ padding: msg.mediaUrl ? '0' : '10px 14px', overflow: msg.mediaUrl ? 'hidden' : 'visible', position: 'relative' }}>
          {msg.mediaUrl && msg.mediaType === 'IMAGE' && (
            <img src={msg.mediaUrl} style={{ maxWidth: 240, maxHeight: 320, borderRadius: 14, display: 'block', objectFit: 'cover' }} />
          )}
          {msg.encryptedPayload && (
            <p style={{ fontSize: 15, lineHeight: 1.5, color: isMine ? 'white' : 'var(--text-primary)', wordBreak: 'break-word', padding: msg.mediaUrl ? '8px 12px 4px' : '0' }}>
              {msg.encryptedPayload}
            </p>
          )}
          {msg.locationLat && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '4px 0' }}>
              <MapPin size={14} color={isMine ? 'rgba(255,255,255,0.7)' : 'var(--accent-bright)'} />
              <span style={{ fontSize: 13, color: isMine ? 'rgba(255,255,255,0.9)' : 'var(--text-secondary)' }}>{msg.locationLabel || 'Live Location'}</span>
            </div>
          )}
          <div style={{ display: 'flex', alignItems: 'center', gap: 4, justifyContent: 'flex-end', marginTop: 4, padding: msg.mediaUrl ? '0 10px 8px' : '0' }}>
            {msg.isEdited && <span style={{ fontSize: 10, color: isMine ? 'rgba(255,255,255,0.5)' : 'var(--text-tertiary)' }}>edited</span>}
            <span style={{ fontSize: 11, color: isMine ? 'rgba(255,255,255,0.55)' : 'var(--text-tertiary)' }}>{formatMessageTime(msg.sentAt)}</span>
            {isMine && (
              msg.status === 'SENDING'
                ? <Clock size={13} color="rgba(255,255,255,0.55)" />
                : msg.status === 'SEEN'
                  ? <CheckCheck size={14} color="#53bdeb" />
                  : <Check size={14} color="rgba(255,255,255,0.55)" />
            )}
          </div>
        </div>
        {msg.reactionCounts && Object.keys(msg.reactionCounts).length > 0 && (
          <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap', marginTop: 4, justifyContent: isMine ? 'flex-end' : 'flex-start' }}>
            {Object.entries(msg.reactionCounts).map(([emoji, count]) => (
              <div key={emoji} style={{ background: 'var(--surface-2)', border: '1px solid var(--border-subtle)', borderRadius: 12, padding: '2px 8px', fontSize: 13, display: 'flex', alignItems: 'center', gap: 4, color: 'var(--text-secondary)' }}>
                {emoji}{count > 1 && <span style={{ fontSize: 11 }}>{count}</span>}
              </div>
            ))}
          </div>
        )}
      </motion.div>
    </div>
  )
}

function TypingIndicator() {
  return (
    <div style={{ display: 'flex', justifyContent: 'flex-start', padding: '6px 16px' }}>
      <div style={{ background: 'var(--surface-2)', border: '1px solid var(--border-subtle)', borderRadius: '18px 18px 18px 4px', padding: '12px 16px', display: 'flex', gap: 4, alignItems: 'center' }}>
        {[0, 1, 2].map((i) => (
          <motion.div key={i} animate={{ y: [0, -4, 0] }} transition={{ duration: 0.6, repeat: Infinity, delay: i * 0.15 }}
            style={{ width: 7, height: 7, borderRadius: '50%', background: 'var(--text-tertiary)' }} />
        ))}
      </div>
    </div>
  )
}

function DateDivider({ date }: { date: string }) {
  const d = new Date(date)
  const today = new Date()
  const yesterday = new Date(today); yesterday.setDate(yesterday.getDate() - 1)
  let label = d.toLocaleDateString('en-US', { month: 'long', day: 'numeric' })
  if (d.toDateString() === today.toDateString()) label = 'Today'
  else if (d.toDateString() === yesterday.toDateString()) label = 'Yesterday'

  return (
    <div style={{ display: 'flex', justifyContent: 'center', padding: '12px 0 6px' }}>
      <div style={{ background: 'var(--surface-2)', border: '1px solid var(--border-subtle)', borderRadius: 20, padding: '4px 14px', fontSize: 12, color: 'var(--text-tertiary)', fontWeight: 500 }}>
        {label}
      </div>
    </div>
  )
}

function HeaderBtn({ icon, onClick }: { icon: React.ReactNode; onClick: () => void }) {
  return (
    <button onClick={onClick} style={{ width: 36, height: 36, background: 'none', border: 'none', borderRadius: 10, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', color: 'var(--text-secondary)' }}>
      {icon}
    </button>
  )
}

function MessageSkeletons() {
  const widths = [160, 200, 130, 180, 150, 210]
  return (
    <div style={{ padding: '12px 16px', display: 'flex', flexDirection: 'column', gap: 12 }}>
      {widths.map((w, i) => (
        <div key={i} style={{ display: 'flex', justifyContent: i % 2 === 0 ? 'flex-end' : 'flex-start' }}>
          <div style={{ height: 40, width: w, background: 'var(--surface-2)', borderRadius: 14, animation: 'pulse 1.5s infinite' }} />
        </div>
      ))}
    </div>
  )
}

type MessageGroup = { messages: Message[]; showDate: boolean }

function groupMessages(msgs: Message[]): MessageGroup[] {
  const groups: MessageGroup[] = []
  let lastDate = ''
  msgs.forEach((msg) => {
    const msgDate = new Date(msg.sentAt).toDateString()
    const showDate = msgDate !== lastDate
    if (showDate) lastDate = msgDate
    const lastGroup = groups[groups.length - 1]
    const isSameSender = lastGroup && !showDate && lastGroup.messages[lastGroup.messages.length - 1]?.isMine === msg.isMine
    if (isSameSender) { lastGroup.messages.push(msg) }
    else { groups.push({ messages: [msg], showDate }) }
  })
  return groups
}

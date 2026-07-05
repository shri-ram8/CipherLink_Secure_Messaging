import { Client, IMessage, StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || 'http://localhost:8080/ws'

type EventHandler = (data: Record<string, unknown>) => void
type QueuedSend = { destination: string; body: object; headers: Record<string, string> }

function getFreshToken(): string | null {
  if (typeof window === 'undefined') return null
  return localStorage.getItem('cl_access_token')
}

class CipherLinkWS {
  private client: Client | null = null
  private subscriptions: Map<string, StompSubscription> = new Map()
  private handlers: Map<string, Set<EventHandler>> = new Map()
  private connected = false

  // Anything published while the socket isn't connected (brief network
  // blip, reconnect-in-progress, tab just woke up, etc.) is held here
  // instead of being silently dropped, and gets flushed the moment
  // onConnect fires again. This is what was making sends/reactions
  // vanish with zero feedback whenever the socket wasn't in a perfectly
  // connected state at the exact millisecond you tapped something.
  private outbox: QueuedSend[] = []

  // token param kept for backwards-compat call sites, but we no longer
  // trust it after the first connect — every (re)connect attempt re-reads
  // the latest token from localStorage instead of relying on a stale
  // closure, which was the earlier cause of permanently-failed reconnects.
  connect(_token?: string) {
    if (this.client?.active) return

    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL) as WebSocket,

      // Runs immediately before EVERY connection attempt, including
      // automatic reconnects — so this is where we must pull the
      // freshest token, not once at Client-construction time.
      beforeConnect: async () => {
        const token = getFreshToken()
        if (this.client) {
          this.client.connectHeaders = {
            Authorization: token ? `Bearer ${token}` : '',
          }
        }
      },

      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      onConnect: () => {
        this.connected = true
        this.subscribeToQueues()
        this.publish('/app/connect', {})
        this.emit('connected', {})
        this.flushOutbox()
      },

      onDisconnect: () => {
        this.connected = false
        this.emit('disconnected', {})
      },

      onWebSocketClose: () => {
        this.connected = false
      },

      onStompError: (frame) => {
        console.error('STOMP error:', frame)
      },
    })

    this.client.activate()
  }

  private subscribeToQueues() {
    if (!this.client) return
    const token = getFreshToken()
    const headers = { Authorization: `Bearer ${token}` }

    const sub1 = this.client.subscribe('/user/queue/messages', (msg) => {
      this.handleMessage(msg)
    }, headers)

    const sub2 = this.client.subscribe('/user/queue/typing', (msg) => {
      this.handleMessage(msg)
    }, headers)

    const sub3 = this.client.subscribe('/user/queue/calls', (msg) => {
      this.handleMessage(msg)
    }, headers)

    const sub4 = this.client.subscribe('/user/queue/location', (msg) => {
      this.handleMessage(msg)
    }, headers)

    const sub5 = this.client.subscribe('/user/queue/group-messages', (msg) => {
      this.handleMessage(msg)
    }, headers)

    const sub6 = this.client.subscribe('/topic/presence', (msg) => {
      this.handleMessage(msg)
    }, headers)

    this.subscriptions.set('messages', sub1)
    this.subscriptions.set('typing', sub2)
    this.subscriptions.set('calls', sub3)
    this.subscriptions.set('location', sub4)
    this.subscriptions.set('group-messages', sub5)
    this.subscriptions.set('presence', sub6)
  }

  private handleMessage(msg: IMessage) {
    try {
      const data = JSON.parse(msg.body)
      const event = data.event as string
      if (event) this.emit(event, data)
      this.emit('*', data)
    } catch (e) {
      console.error('WS parse error:', e)
    }
  }

  private flushOutbox() {
    if (this.outbox.length === 0) return
    const pending = this.outbox
    this.outbox = []
    pending.forEach(({ destination, body, headers }) => this.publish(destination, body, headers))
  }

  publish(destination: string, body: object = {}, headers: Record<string, string> = {}) {
    if (!this.client?.active || !this.connected) {
      // Queue instead of dropping — flushed automatically on next onConnect.
      this.outbox.push({ destination, body, headers })
      return
    }
    const token = getFreshToken()
    this.client.publish({
      destination,
      body: JSON.stringify(body),
      // Fresh token always wins — extra headers passed in can add other
      // fields, but must NOT be allowed to reintroduce a stale
      // Authorization value, so it's applied last.
      headers: { ...headers, Authorization: `Bearer ${token}` },
    })
  }

  on(event: string, handler: EventHandler) {
    if (!this.handlers.has(event)) this.handlers.set(event, new Set())
    this.handlers.get(event)!.add(handler)
    return () => this.off(event, handler)
  }

  off(event: string, handler: EventHandler) {
    this.handlers.get(event)?.delete(handler)
  }

  private emit(event: string, data: Record<string, unknown>) {
    this.handlers.get(event)?.forEach((h) => h(data))
  }

  // ── Chat actions ──────────────────────────────
  sendMessage(payload: object) {
    this.publish('/app/chat.sendMessage', payload)
  }

  sendGroupMessage(payload: object) {
    this.publish('/app/chat.sendGroupMessage', payload)
  }

  markSeen(messageId: string) {
    this.publish('/app/chat.markSeen', { messageId })
  }

  react(messageId: string, emoji: string) {
    this.publish('/app/chat.react', { messageId, emoji })
  }

  editMessage(messageId: string, newPayload: string) {
    this.publish('/app/chat.editMessage', { messageId, newPayload })
  }

  deleteMessage(messageId: string) {
    this.publish('/app/chat.deleteMessage', { messageId })
  }

  typingStarted(conversationId: string) {
    this.publish('/app/chat.typingStarted', { conversationId })
  }

  typingStopped(conversationId: string) {
    this.publish('/app/chat.typingStopped', { conversationId })
  }

  // ── Call signaling ────────────────────────────
  initiateCall(payload: object) {
    this.publish('/app/call.initiate', payload)
  }

  answerCall(payload: object) {
    this.publish('/app/call.answer', payload)
  }

  endCall(otherUserId: string, reason = 'ENDED') {
    this.publish('/app/call.end', { otherUserId, reason })
  }

  sendIceCandidate(targetUserId: string, candidate: string) {
    this.publish('/app/call.ice', { targetUserId, candidate })
  }

  // ── Location ──────────────────────────────────
  broadcastLocation(conversationId: string, lat: number, lng: number) {
    this.publish('/app/location.update', { conversationId, latitude: lat, longitude: lng })
  }

  disconnect() {
    const token = getFreshToken()
    if (token) this.publish('/app/disconnect', {})
    this.client?.deactivate()
    this.subscriptions.clear()
    this.connected = false
  }

  isConnected() {
    return this.connected
  }
}

export const ws = new CipherLinkWS()

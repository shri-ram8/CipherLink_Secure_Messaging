'use client'
import { useEffect, useRef } from 'react'
import { ws } from '@/lib/websocket'
import { useAuthStore } from '@/store/auth'
import { useChatStore } from '@/store/chat'
import { useCallStore } from '@/store/calls'
import type { Message } from '@/types'

export function useWebSocket() {
  const { accessToken, isAuthenticated } = useAuthStore()
  const {
    addMessage,
    reconcileMessage,
    updateMessage,
    removeMessage,
    setTyping,
    setOnline,
    addGroupMessage,
    updateLastMessage,
    incrementUnread,
    activeConversationId,
  } = useChatStore()
  const { setIncomingCall } = useCallStore()

  const connectedRef = useRef(false)

  useEffect(() => {
    if (!isAuthenticated || !accessToken || connectedRef.current) return

    ws.connect(accessToken)
    connectedRef.current = true

    const unsubs = [
      ws.on('ReceiveMessage', (data) => {
        const msg = data.data as Message
        if (msg) {
          addMessage(msg)
          updateLastMessage(msg.conversationId, msg.encryptedPayload || '')
          if (msg.conversationId !== activeConversationId) {
            incrementUnread(msg.conversationId)
          }
        }
      }),

      ws.on('MessageSent', (data) => {
        const msg = data.data as Message
        const clientTempId = (data as { clientTempId?: string }).clientTempId
        if (msg && clientTempId) reconcileMessage(clientTempId, msg)
        else if (msg) addMessage(msg)
      }),

      ws.on('MessageSeen', (data) => {
        const { messageId } = data as { messageId: string }
        if (messageId) updateMessage(messageId, { status: 'SEEN' })
      }),

      ws.on('MessageReactionUpdated', (data) => {
        const { messageId, reactions } = data as {
          messageId: string
          reactions: Record<string, number>
        }
        if (messageId) updateMessage(messageId, { reactionCounts: reactions })
      }),

      ws.on('MessageEdited', (data) => {
        const { messageId, newPayload } = data as { messageId: string; newPayload: string }
        if (messageId) updateMessage(messageId, { encryptedPayload: newPayload, isEdited: true })
      }),

      ws.on('MessageDeleted', (data) => {
        const { messageId, conversationId } = data as {
          messageId: string
          conversationId: string
        }
        if (messageId && conversationId) removeMessage(messageId, conversationId)
      }),

      ws.on('UserTyping', (data) => {
        const { userId } = data as { userId: string }
        if (userId) {
          setTyping(userId, true)
          setTimeout(() => setTyping(userId, false), 3000)
        }
      }),

      ws.on('UserStoppedTyping', (data) => {
        const { userId } = data as { userId: string }
        if (userId) setTyping(userId, false)
      }),

      ws.on('UserOnline', (data) => {
        const { userId } = data as { userId: string }
        if (userId) setOnline(userId, true)
      }),

      ws.on('UserOffline', (data) => {
        const { userId } = data as { userId: string }
        if (userId) setOnline(userId, false)
      }),

      ws.on('ReceiveGroupMessage', (data) => {
        const msg = data.data as {
          id: string
          groupId: string
          senderDeviceId: string
          senderUserId: string
          senderName: string
          encryptedPayload: string
          sentAt: string
        }
        if (msg) addGroupMessage(msg)
      }),

      ws.on('IncomingCall', (data) => {
        const { callerId, callerName, callerAvatarUrl, callType, roomId, offer } = data as {
          callerId: string
          callerName: string
          callerAvatarUrl?: string
          callType: 'VOICE' | 'VIDEO'
          roomId: string
          offer: string
        }
        setIncomingCall({ callerId, callerName, callerAvatarUrl, callType, roomId, offer })
      }),
    ]

    return () => {
      unsubs.forEach((u) => u())
    }
  }, [isAuthenticated, accessToken])

  return { ws }
}

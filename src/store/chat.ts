'use client'
import { create } from 'zustand'
import type { Conversation, Message, Group, GroupMessage } from '@/types'

interface ChatState {
  conversations: Conversation[]
  activeConversationId: string | null
  messages: Record<string, Message[]>
  typingUsers: Record<string, boolean>
  onlineUsers: Set<string>
  groups: Group[]
  groupMessages: Record<string, GroupMessage[]>

  setConversations: (convs: Conversation[]) => void
  setActiveConversation: (id: string | null) => void
  setMessages: (convId: string, msgs: Message[]) => void
  prependMessages: (convId: string, msgs: Message[]) => void
  addMessage: (msg: Message) => void
  reconcileMessage: (clientTempId: string, realMsg: Message) => void
  updateMessage: (msgId: string, update: Partial<Message>) => void
  removeMessage: (msgId: string, convId: string) => void
  setTyping: (userId: string, isTyping: boolean) => void
  setOnline: (userId: string, online: boolean) => void
  setGroups: (groups: Group[]) => void
  setGroupMessages: (groupId: string, msgs: GroupMessage[]) => void
  addGroupMessage: (msg: GroupMessage) => void
  incrementUnread: (convId: string) => void
  clearUnread: (convId: string) => void
  updateLastMessage: (convId: string, text: string) => void
}

export const useChatStore = create<ChatState>((set, get) => ({
  conversations: [],
  activeConversationId: null,
  messages: {},
  typingUsers: {},
  onlineUsers: new Set(),
  groups: [],
  groupMessages: {},

  setConversations: (convs) => set({ conversations: convs }),

  setActiveConversation: (id) => set({ activeConversationId: id }),

  setMessages: (convId, msgs) =>
    set((s) => ({ messages: { ...s.messages, [convId]: msgs } })),

  prependMessages: (convId, msgs) =>
    set((s) => ({
      messages: {
        ...s.messages,
        [convId]: [...msgs, ...(s.messages[convId] || [])],
      },
    })),

  addMessage: (msg) =>
    set((s) => {
      const existing = s.messages[msg.conversationId] || []
      // Avoid duplicates
      if (existing.some((m) => m.id === msg.id)) return s
      return {
        messages: {
          ...s.messages,
          [msg.conversationId]: [...existing, msg],
        },
      }
    }),

  reconcileMessage: (clientTempId, realMsg) =>
    set((s) => {
      const existing = s.messages[realMsg.conversationId] || []
      const tempIdx = existing.findIndex((m) => m.id === clientTempId)
      // If the server ack raced ahead of the optimistic insert somehow,
      // fall back to a plain de-duped append instead of losing the message.
      if (tempIdx === -1) {
        if (existing.some((m) => m.id === realMsg.id)) return s
        return {
          messages: { ...s.messages, [realMsg.conversationId]: [...existing, realMsg] },
        }
      }
      const updated = [...existing]
      updated[tempIdx] = realMsg
      return { messages: { ...s.messages, [realMsg.conversationId]: updated } }
    }),

  updateMessage: (msgId, update) =>
    set((s) => {
      const newMessages = { ...s.messages }
      for (const convId in newMessages) {
        const idx = newMessages[convId].findIndex((m) => m.id === msgId)
        if (idx !== -1) {
          const updated = [...newMessages[convId]]
          updated[idx] = { ...updated[idx], ...update }
          newMessages[convId] = updated
          break
        }
      }
      return { messages: newMessages }
    }),

  removeMessage: (msgId, convId) =>
    set((s) => ({
      messages: {
        ...s.messages,
        [convId]: (s.messages[convId] || []).filter((m) => m.id !== msgId),
      },
    })),

  setTyping: (userId, isTyping) =>
    set((s) => ({ typingUsers: { ...s.typingUsers, [userId]: isTyping } })),

  setOnline: (userId, online) => {
    const newSet = new Set(get().onlineUsers)
    if (online) newSet.add(userId)
    else newSet.delete(userId)
    set({ onlineUsers: newSet })
    // Also update conversations
    set((s) => ({
      conversations: s.conversations.map((c) =>
        c.otherUserId === userId ? { ...c, isOnline: online } : c
      ),
    }))
  },

  setGroups: (groups) => set({ groups }),

  setGroupMessages: (groupId, msgs) =>
    set((s) => ({ groupMessages: { ...s.groupMessages, [groupId]: msgs } })),

  addGroupMessage: (msg) =>
    set((s) => {
      const existing = s.groupMessages[msg.groupId] || []
      if (existing.some((m) => m.id === msg.id)) return s
      return {
        groupMessages: {
          ...s.groupMessages,
          [msg.groupId]: [...existing, msg],
        },
      }
    }),

  incrementUnread: (convId) =>
    set((s) => ({
      conversations: s.conversations.map((c) =>
        c.id === convId ? { ...c, unreadCount: c.unreadCount + 1 } : c
      ),
    })),

  clearUnread: (convId) =>
    set((s) => ({
      conversations: s.conversations.map((c) =>
        c.id === convId ? { ...c, unreadCount: 0 } : c
      ),
    })),

  updateLastMessage: (convId, text) =>
    set((s) => ({
      conversations: s.conversations.map((c) =>
        c.id === convId
          ? { ...c, lastMessage: text, lastMessageAt: new Date().toISOString() }
          : c
      ),
    })),
}))

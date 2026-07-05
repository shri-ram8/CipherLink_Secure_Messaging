'use client'
import { create } from 'zustand'

export type CallState = 'idle' | 'outgoing' | 'incoming' | 'active' | 'ended'

interface IncomingCall {
  callerId: string
  callerName: string
  callerAvatarUrl?: string
  callType: 'VOICE' | 'VIDEO'
  roomId: string
  offer: string
}

interface CallStore {
  state: CallState
  incomingCall: IncomingCall | null
  activeCallId: string | null
  activeCallType: 'VOICE' | 'VIDEO' | null
  otherUserId: string | null
  otherUserName: string | null
  otherUserAvatar?: string
  startTime: number | null
  isMuted: boolean
  isCameraOff: boolean
  isSpeakerOn: boolean

  setIncomingCall: (call: IncomingCall) => void
  setOutgoing: (userId: string, name: string, avatar: string | undefined, type: 'VOICE' | 'VIDEO', callId: string) => void
  setActive: () => void
  endCall: () => void
  setMuted: (muted: boolean) => void
  setCameraOff: (off: boolean) => void
  setSpeaker: (on: boolean) => void
}

export const useCallStore = create<CallStore>((set) => ({
  state: 'idle',
  incomingCall: null,
  activeCallId: null,
  activeCallType: null,
  otherUserId: null,
  otherUserName: null,
  otherUserAvatar: undefined,
  startTime: null,
  isMuted: false,
  isCameraOff: false,
  isSpeakerOn: true,

  setIncomingCall: (call) =>
    set({ state: 'incoming', incomingCall: call }),

  setOutgoing: (userId, name, avatar, type, callId) =>
    set({
      state: 'outgoing',
      otherUserId: userId,
      otherUserName: name,
      otherUserAvatar: avatar,
      activeCallType: type,
      activeCallId: callId,
      incomingCall: null,
    }),

  setActive: () =>
    set({ state: 'active', startTime: Date.now() }),

  endCall: () =>
    set({
      state: 'idle',
      incomingCall: null,
      activeCallId: null,
      activeCallType: null,
      otherUserId: null,
      otherUserName: null,
      otherUserAvatar: undefined,
      startTime: null,
      isMuted: false,
      isCameraOff: false,
    }),

  setMuted: (muted) => set({ isMuted: muted }),
  setCameraOff: (off) => set({ isCameraOff: off }),
  setSpeaker: (on) => set({ isSpeakerOn: on }),
}))

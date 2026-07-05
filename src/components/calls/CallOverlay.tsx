'use client'
import { useEffect, useRef, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useCallStore } from '@/store/calls'
import { useAuthStore } from '@/store/auth'
import { callApi } from '@/lib/api'
import { ws } from '@/lib/websocket'
import { Avatar } from '@/components/ui/avatar-cl'
import { formatCallDuration } from '@/lib/utils'
import {
  Phone, PhoneOff, Mic, MicOff, Video, VideoOff,
  Volume2, VolumeX, RotateCcw
} from 'lucide-react'

export function CallOverlay() {
  const {
    state,
    incomingCall,
    activeCallId,
    activeCallType,
    otherUserId,
    otherUserName,
    otherUserAvatar,
    startTime,
    isMuted,
    isCameraOff,
    isSpeakerOn,
    setActive,
    endCall,
    setMuted,
    setCameraOff,
    setSpeaker,
  } = useCallStore()

  const { userId } = useAuthStore()

  const [elapsed, setElapsed] = useState(0)
  const timerRef = useRef<ReturnType<typeof setInterval> | undefined>(undefined)

  const localVideoRef = useRef<HTMLVideoElement>(null)
  const remoteVideoRef = useRef<HTMLVideoElement>(null)
  const pcRef = useRef<RTCPeerConnection | null>(null)
  const localStreamRef = useRef<MediaStream | null>(null)

  // Timer
  useEffect(() => {
    if (state === 'active' && startTime) {
      timerRef.current = setInterval(() => {
        setElapsed(Math.floor((Date.now() - startTime) / 1000))
      }, 1000)
    } else {
      clearInterval(timerRef.current)
      setElapsed(0)
    }
    return () => clearInterval(timerRef.current)
  }, [state, startTime])

  // WebRTC setup
  useEffect(() => {
    if (state === 'outgoing' && activeCallType) {
      initWebRTC(activeCallType, true)
    }
  }, [state, activeCallType])

  useEffect(() => {
    // Listen for ICE candidates and answers
    const unsub = ws.on('IceCandidate', async (data) => {
      const { candidate } = data as { candidate: string }
      if (pcRef.current && candidate) {
        try {
          await pcRef.current.addIceCandidate(JSON.parse(candidate))
        } catch {}
      }
    })

    const unsub2 = ws.on('CallAnswered', async (data) => {
      const { answer } = data as { answer: string }
      if (pcRef.current && answer) {
        try {
          await pcRef.current.setRemoteDescription(JSON.parse(answer))
          setActive()
        } catch {}
      }
    })

    const unsub3 = ws.on('CallEnded', () => {
      cleanup()
      endCall()
    })

    return () => { unsub(); unsub2(); unsub3() }
  }, [])

  async function initWebRTC(type: 'VOICE' | 'VIDEO', isInitiator: boolean, offer?: string) {
    const pc = new RTCPeerConnection({
      iceServers: [{ urls: 'stun:stun.l.google.com:19302' }],
    })
    pcRef.current = pc

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: true,
        video: type === 'VIDEO',
      })
      localStreamRef.current = stream
      stream.getTracks().forEach((t) => pc.addTrack(t, stream))

      if (localVideoRef.current && type === 'VIDEO') {
        localVideoRef.current.srcObject = stream
      }
    } catch {}

    pc.ontrack = (e) => {
      if (remoteVideoRef.current) {
        remoteVideoRef.current.srcObject = e.streams[0]
      }
    }

    pc.onicecandidate = (e) => {
      if (e.candidate && otherUserId) {
        ws.sendIceCandidate(otherUserId, JSON.stringify(e.candidate))
      }
    }

    if (isInitiator) {
      const offerSdp = await pc.createOffer()
      await pc.setLocalDescription(offerSdp)
      ws.initiateCall({
        receiverId: otherUserId,
        callType: type,
        offer: JSON.stringify(offerSdp),
      })
    } else if (offer) {
      await pc.setRemoteDescription(JSON.parse(offer))
      const answer = await pc.createAnswer()
      await pc.setLocalDescription(answer)
      ws.answerCall({ callerId: incomingCall?.callerId, answer: JSON.stringify(answer) })
      setActive()
    }
  }

  async function acceptCall() {
    if (!incomingCall) return
    await initWebRTC(incomingCall.callType, false, incomingCall.offer)
  }

  async function declineCall() {
    if (incomingCall) {
      ws.endCall(incomingCall.callerId, 'DECLINED')
    }
    cleanup()
    endCall()
  }

  async function hangUp() {
    if (activeCallId) {
      await callApi.endCall(activeCallId, 'ENDED', elapsed).catch(() => {})
    }
    if (otherUserId) ws.endCall(otherUserId)
    cleanup()
    endCall()
  }

  function cleanup() {
    localStreamRef.current?.getTracks().forEach((t) => t.stop())
    pcRef.current?.close()
    pcRef.current = null
    localStreamRef.current = null
  }

  function toggleMute() {
    const stream = localStreamRef.current
    if (stream) {
      stream.getAudioTracks().forEach((t) => { t.enabled = isMuted })
    }
    setMuted(!isMuted)
  }

  function toggleCamera() {
    const stream = localStreamRef.current
    if (stream) {
      stream.getVideoTracks().forEach((t) => { t.enabled = isCameraOff })
    }
    setCameraOff(!isCameraOff)
  }

  if (state === 'idle') return null

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        style={{
          position: 'fixed',
          inset: 0,
          zIndex: 500,
          background: '#0a0a0f',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          paddingTop: 'env(safe-area-inset-top)',
          paddingBottom: 'env(safe-area-inset-bottom)',
        }}
      >
        {/* Video streams */}
        {activeCallType === 'VIDEO' && (
          <>
            <video
              ref={remoteVideoRef}
              autoPlay
              playsInline
              style={{
                position: 'absolute',
                inset: 0,
                width: '100%',
                height: '100%',
                objectFit: 'cover',
              }}
            />
            <video
              ref={localVideoRef}
              autoPlay
              playsInline
              muted
              style={{
                position: 'absolute',
                top: 80,
                right: 16,
                width: 100,
                height: 140,
                borderRadius: 12,
                objectFit: 'cover',
                border: '2px solid rgba(255,255,255,0.2)',
                zIndex: 10,
              }}
            />
          </>
        )}

        {/* Background glow for voice */}
        {activeCallType !== 'VIDEO' && (
          <div
            style={{
              position: 'absolute',
              inset: 0,
              background: 'radial-gradient(ellipse at center 30%, rgba(109,40,217,0.2) 0%, transparent 70%)',
            }}
          />
        )}

        {/* Content */}
        <div
          style={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 20,
            position: 'relative',
            zIndex: 5,
          }}
        >
          {/* Avatar */}
          <motion.div
            animate={state === 'active' ? { scale: [1, 1.03, 1] } : {}}
            transition={{ duration: 2, repeat: Infinity }}
          >
            <div
              style={{
                padding: 4,
                background: state === 'active'
                  ? 'conic-gradient(from 0deg, #8b5cf6, #a78bfa, #c4b5fd, #8b5cf6)'
                  : 'var(--surface-2)',
                borderRadius: '50%',
              }}
            >
              <div style={{ background: '#0a0a0f', borderRadius: '50%', padding: 3 }}>
                <Avatar src={otherUserAvatar} name={otherUserName || ''} size="2xl" />
              </div>
            </div>
          </motion.div>

          <div style={{ textAlign: 'center' }}>
            <h2 style={{ fontSize: 28, fontWeight: 700, color: 'white', letterSpacing: '-0.02em' }}>
              {state === 'incoming' ? incomingCall?.callerName : otherUserName}
            </h2>
            <p style={{ fontSize: 16, color: 'rgba(255,255,255,0.5)', marginTop: 8 }}>
              {state === 'incoming'
                ? `Incoming ${incomingCall?.callType === 'VIDEO' ? 'video' : 'voice'} call`
                : state === 'outgoing'
                ? 'Calling…'
                : formatCallDuration(elapsed)}
            </p>
          </div>
        </div>

        {/* Controls */}
        <div style={{ width: '100%', padding: '0 32px 40px', position: 'relative', zIndex: 5 }}>
          {state === 'incoming' ? (
            <div style={{ display: 'flex', justifyContent: 'space-around', alignItems: 'center' }}>
              <CallButton
                icon={<PhoneOff size={26} />}
                label="Decline"
                color="#ef4444"
                bg="rgba(239,68,68,0.15)"
                onClick={declineCall}
              />
              <CallButton
                icon={<Phone size={26} />}
                label="Accept"
                color="#10b981"
                bg="rgba(16,185,129,0.15)"
                onClick={acceptCall}
              />
            </div>
          ) : (
            <div>
              {/* Secondary controls */}
              <div style={{ display: 'flex', justifyContent: 'space-around', marginBottom: 32 }}>
                <CallButton
                  icon={isMuted ? <MicOff size={22} /> : <Mic size={22} />}
                  label={isMuted ? 'Unmute' : 'Mute'}
                  color={isMuted ? '#ef4444' : 'rgba(255,255,255,0.7)'}
                  bg="rgba(255,255,255,0.08)"
                  onClick={toggleMute}
                  size="sm"
                />
                {activeCallType === 'VIDEO' && (
                  <CallButton
                    icon={isCameraOff ? <VideoOff size={22} /> : <Video size={22} />}
                    label={isCameraOff ? 'Show cam' : 'Hide cam'}
                    color="rgba(255,255,255,0.7)"
                    bg="rgba(255,255,255,0.08)"
                    onClick={toggleCamera}
                    size="sm"
                  />
                )}
                <CallButton
                  icon={isSpeakerOn ? <Volume2 size={22} /> : <VolumeX size={22} />}
                  label="Speaker"
                  color={isSpeakerOn ? 'var(--accent-bright)' : 'rgba(255,255,255,0.7)'}
                  bg={isSpeakerOn ? 'rgba(139,92,246,0.15)' : 'rgba(255,255,255,0.08)'}
                  onClick={() => setSpeaker(!isSpeakerOn)}
                  size="sm"
                />
              </div>

              {/* End call */}
              <div style={{ display: 'flex', justifyContent: 'center' }}>
                <CallButton
                  icon={<PhoneOff size={26} />}
                  label="End"
                  color="white"
                  bg="#ef4444"
                  onClick={hangUp}
                />
              </div>
            </div>
          )}
        </div>
      </motion.div>
    </AnimatePresence>
  )
}

function CallButton({
  icon,
  label,
  color,
  bg,
  onClick,
  size = 'md',
}: {
  icon: React.ReactNode
  label: string
  color: string
  bg: string
  onClick: () => void
  size?: 'sm' | 'md'
}) {
  const dim = size === 'md' ? 72 : 58

  return (
    <motion.button
      whileHover={{ scale: 1.05 }}
      whileTap={{ scale: 0.9 }}
      onClick={onClick}
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 8,
        background: 'none',
        border: 'none',
        cursor: 'pointer',
      }}
    >
      <div
        style={{
          width: dim,
          height: dim,
          borderRadius: '50%',
          background: bg,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color,
          border: `1px solid ${color === 'white' ? 'transparent' : 'rgba(255,255,255,0.08)'}`,
        }}
      >
        {icon}
      </div>
      <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.5)', fontWeight: 500 }}>{label}</span>
    </motion.button>
  )
}

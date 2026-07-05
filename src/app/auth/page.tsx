'use client'
import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useRouter } from 'next/navigation'
import { initializeApp, getApps } from 'firebase/app'
import {
  getAuth,
  RecaptchaVerifier,
  signInWithPhoneNumber,
  ConfirmationResult,
} from 'firebase/auth'
import { useAuthStore } from '@/store/auth'
import { authApi, userApi } from '@/lib/api'
import { ChevronRight, Shield, Sparkles } from 'lucide-react'
import { toast } from '@/components/ui/toaster'

// Initialize Firebase (users configure via env)
const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID,
}

let app: ReturnType<typeof initializeApp>
if (typeof window !== 'undefined') {
  app = getApps().length ? getApps()[0] : initializeApp(firebaseConfig)
}

type Step = 'phone' | 'otp' | 'profile'

export default function AuthPage() {
  const router = useRouter()
  const { setAuth, setProfile } = useAuthStore()

  const [step, setStep] = useState<Step>('phone')
  const [phone, setPhone] = useState('')
  const [otp, setOtp] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [loading, setLoading] = useState(false)
  const [confirmResult, setConfirmResult] = useState<ConfirmationResult | null>(null)
  const [firebaseToken, setFirebaseToken] = useState('')

  async function handleSendOtp() {
    if (!phone || phone.length < 10) {
      toast('Enter a valid phone number', 'error')
      return
    }
    setLoading(true)
    try {
      const auth = getAuth(app)
      const verifier = new RecaptchaVerifier(auth, 'recaptcha-container', {
        size: 'invisible',
      })
      const result = await signInWithPhoneNumber(auth, phone, verifier)
      setConfirmResult(result)
      setStep('otp')
    } catch (e: unknown) {
      toast((e as Error).message || 'Failed to send OTP', 'error')
    } finally {
      setLoading(false)
    }
  }

  async function handleVerifyOtp() {
    if (!confirmResult || otp.length !== 6) {
      toast('Enter the 6-digit code', 'error')
      return
    }
    setLoading(true)
    try {
      const cred = await confirmResult.confirm(otp)
      const token = await cred.user.getIdToken()
      setFirebaseToken(token)

      const authRes = await authApi.verifyOtp(token, phone) as {
        accessToken: string
        refreshToken: string
        userId: string
        deviceId: string
        isNewUser: boolean
      }

      setAuth(authRes)

      if (authRes.isNewUser) {
        setStep('profile')
      } else {
        const profile = await userApi.getProfile() as { userId: string; phoneNumber: string; displayName: string; profilePictureUrl?: string }
        setProfile(profile)
        router.replace('/chat')
      }
    } catch (e: unknown) {
      toast((e as Error).message || 'Invalid code', 'error')
    } finally {
      setLoading(false)
    }
  }

  async function handleSetProfile() {
    if (!displayName.trim()) {
      toast('Enter your name', 'error')
      return
    }
    setLoading(true)
    try {
      const profile = await userApi.updateProfile({ displayName }) as { userId: string; phoneNumber: string; displayName: string; profilePictureUrl?: string }
      setProfile(profile)
      router.replace('/chat')
    } catch (e: unknown) {
      toast((e as Error).message || 'Failed to save profile', 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      style={{
        minHeight: '100dvh',
        background: 'var(--bg-primary)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '24px 24px calc(env(safe-area-inset-bottom) + 24px)',
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      {/* Background glow */}
      <div
        style={{
          position: 'absolute',
          top: '-20%',
          left: '50%',
          transform: 'translateX(-50%)',
          width: '120vw',
          height: '60vh',
          background: 'radial-gradient(ellipse, rgba(139,92,246,0.12) 0%, transparent 70%)',
          pointerEvents: 'none',
        }}
      />
      <div
        style={{
          position: 'absolute',
          bottom: '10%',
          right: '-10%',
          width: '50vw',
          height: '50vw',
          background: 'radial-gradient(circle, rgba(109,40,217,0.06) 0%, transparent 70%)',
          pointerEvents: 'none',
        }}
      />

      <div id="recaptcha-container" />

      <motion.div
        style={{ width: '100%', maxWidth: 380, display: 'flex', flexDirection: 'column', gap: 48 }}
      >
        {/* Logo */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          style={{ textAlign: 'center' }}
        >
          <div
            style={{
              width: 72,
              height: 72,
              background: 'linear-gradient(135deg, #6d28d9, #8b5cf6)',
              borderRadius: 20,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 20px',
              boxShadow: '0 0 40px rgba(139,92,246,0.4)',
            }}
          >
            <Sparkles size={32} color="white" />
          </div>
          <h1
            style={{
              fontSize: 32,
              fontWeight: 800,
              letterSpacing: '-0.03em',
              background: 'linear-gradient(135deg, #f1f0ff, #a78bfa)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              backgroundClip: 'text',
            }}
          >
            CipherLink
          </h1>
          <p style={{ color: 'var(--text-tertiary)', fontSize: 15, marginTop: 6 }}>
            Private. Social. Yours.
          </p>
        </motion.div>

        {/* Step content */}
        <AnimatePresence mode="wait">
          {step === 'phone' && (
            <StepCard key="phone">
              <StepTitle>What's your number?</StepTitle>
              <StepSub>We'll send a verification code to confirm it's you.</StepSub>
              <input
                className="input-field"
                type="tel"
                placeholder="+91 98765 43210"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSendOtp()}
                style={{ fontSize: 18, letterSpacing: '0.02em', marginBottom: 8 }}
              />
              <button
                className="btn-primary"
                onClick={handleSendOtp}
                disabled={loading}
                style={{ marginTop: 8 }}
              >
                {loading ? 'Sending…' : 'Send Code'}
              </button>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  justifyContent: 'center',
                  marginTop: 20,
                }}
              >
                <Shield size={13} color="var(--text-tertiary)" />
                <span style={{ fontSize: 12, color: 'var(--text-tertiary)' }}>
                  End-to-end encrypted
                </span>
              </div>
            </StepCard>
          )}

          {step === 'otp' && (
            <StepCard key="otp">
              <StepTitle>Enter the code</StepTitle>
              <StepSub>Sent to {phone}</StepSub>
              <OtpInput value={otp} onChange={setOtp} />
              <button
                className="btn-primary"
                onClick={handleVerifyOtp}
                disabled={loading || otp.length !== 6}
                style={{ marginTop: 8 }}
              >
                {loading ? 'Verifying…' : 'Continue'}
              </button>
              <button
                onClick={() => { setStep('phone'); setOtp('') }}
                style={{
                  marginTop: 12,
                  background: 'none',
                  border: 'none',
                  color: 'var(--accent-bright)',
                  fontSize: 14,
                  cursor: 'pointer',
                  width: '100%',
                  textAlign: 'center',
                }}
              >
                Change number
              </button>
            </StepCard>
          )}

          {step === 'profile' && (
            <StepCard key="profile">
              <StepTitle>What should we call you?</StepTitle>
              <StepSub>This is how you'll appear to others.</StepSub>
              <input
                className="input-field"
                type="text"
                placeholder="Your name"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSetProfile()}
                autoFocus
                style={{ fontSize: 18 }}
              />
              <button
                className="btn-primary"
                onClick={handleSetProfile}
                disabled={loading || !displayName.trim()}
                style={{ marginTop: 8, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}
              >
                {loading ? 'Saving…' : (
                  <>
                    Get Started
                    <ChevronRight size={18} />
                  </>
                )}
              </button>
            </StepCard>
          )}
        </AnimatePresence>
      </motion.div>
    </div>
  )
}

function StepCard({ children }: { children: React.ReactNode }) {
  return (
    <motion.div
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: -20 }}
      transition={{ type: 'spring', stiffness: 300, damping: 28 }}
      style={{
        background: 'var(--surface-1)',
        border: '1px solid var(--border-subtle)',
        borderRadius: 20,
        padding: '28px 24px',
        display: 'flex',
        flexDirection: 'column',
        gap: 16,
      }}
    >
      {children}
    </motion.div>
  )
}

function StepTitle({ children }: { children: React.ReactNode }) {
  return (
    <h2 style={{ fontSize: 22, fontWeight: 700, letterSpacing: '-0.02em', color: 'var(--text-primary)' }}>
      {children}
    </h2>
  )
}

function StepSub({ children }: { children: React.ReactNode }) {
  return (
    <p style={{ fontSize: 14, color: 'var(--text-secondary)', marginTop: -8 }}>
      {children}
    </p>
  )
}

function OtpInput({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  const digits = value.split('').concat(Array(6 - value.length).fill(''))

  return (
    <div style={{ display: 'flex', gap: 10, justifyContent: 'center' }}>
      {digits.map((d, i) => (
        <div
          key={i}
          style={{
            width: 48,
            height: 56,
            background: 'var(--surface-2)',
            border: `2px solid ${d ? 'var(--accent)' : 'var(--border-subtle)'}`,
            borderRadius: 12,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 22,
            fontWeight: 700,
            color: 'var(--text-primary)',
            transition: 'border-color 0.2s',
          }}
        >
          {d || (i === value.length ? <CursorBlink /> : '')}
        </div>
      ))}
      <input
        type="number"
        inputMode="numeric"
        value={value}
        onChange={(e) => {
          const v = e.target.value.replace(/\D/g, '').slice(0, 6)
          onChange(v)
        }}
        style={{
          position: 'absolute',
          opacity: 0,
          width: 1,
          height: 1,
          pointerEvents: 'none',
        }}
        autoFocus
      />
    </div>
  )
}

function CursorBlink() {
  return (
    <motion.div
      animate={{ opacity: [1, 0, 1] }}
      transition={{ duration: 1, repeat: Infinity }}
      style={{ width: 2, height: 22, background: 'var(--accent)', borderRadius: 1 }}
    />
  )
}

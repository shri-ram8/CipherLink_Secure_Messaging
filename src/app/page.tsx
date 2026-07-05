'use client'
import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useAuthStore } from '@/store/auth'

export default function RootPage() {
  const router = useRouter()
  const { isAuthenticated } = useAuthStore()

  useEffect(() => {
    if (isAuthenticated) {
      router.replace('/chat')
    } else {
      router.replace('/auth')
    }
  }, [isAuthenticated])

  return (
    <div
      style={{
        height: '100dvh',
        background: 'var(--bg-primary)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <div
        style={{
          width: 48,
          height: 48,
          borderRadius: 14,
          background: 'linear-gradient(135deg, #6d28d9, #8b5cf6)',
          boxShadow: '0 0 30px rgba(139,92,246,0.4)',
          animation: 'pulse 1.5s infinite',
        }}
      />
    </div>
  )
}

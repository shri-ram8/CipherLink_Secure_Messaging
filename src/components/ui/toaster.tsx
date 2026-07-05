'use client'
import { create } from 'zustand'
import { motion, AnimatePresence } from 'framer-motion'
import { useEffect } from 'react'
import { X } from 'lucide-react'

interface Toast {
  id: string
  message: string
  type?: 'default' | 'success' | 'error'
  duration?: number
}

interface ToastStore {
  toasts: Toast[]
  add: (toast: Omit<Toast, 'id'>) => void
  remove: (id: string) => void
}

export const useToastStore = create<ToastStore>((set) => ({
  toasts: [],
  add: (toast) => {
    const id = Math.random().toString(36).slice(2)
    set((s) => ({ toasts: [...s.toasts, { ...toast, id }] }))
    setTimeout(() => set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })), toast.duration || 3000)
  },
  remove: (id) => set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })),
}))

export function toast(message: string, type: Toast['type'] = 'default') {
  useToastStore.getState().add({ message, type })
}

export function Toaster() {
  const { toasts, remove } = useToastStore()

  return (
    <div
      style={{
        position: 'fixed',
        top: 'calc(env(safe-area-inset-top) + 16px)',
        left: '50%',
        transform: 'translateX(-50%)',
        zIndex: 9999,
        display: 'flex',
        flexDirection: 'column',
        gap: 8,
        pointerEvents: 'none',
        width: 'min(400px, calc(100vw - 32px))',
      }}
    >
      <AnimatePresence>
        {toasts.map((t) => (
          <motion.div
            key={t.id}
            initial={{ opacity: 0, y: -12, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -8, scale: 0.96 }}
            transition={{ type: 'spring', stiffness: 400, damping: 30 }}
            style={{
              background:
                t.type === 'error'
                  ? 'rgba(239,68,68,0.15)'
                  : t.type === 'success'
                  ? 'rgba(16,185,129,0.15)'
                  : 'rgba(30,30,46,0.95)',
              backdropFilter: 'blur(20px)',
              border: `1px solid ${
                t.type === 'error'
                  ? 'rgba(239,68,68,0.3)'
                  : t.type === 'success'
                  ? 'rgba(16,185,129,0.3)'
                  : 'rgba(139,92,246,0.2)'
              }`,
              borderRadius: 12,
              padding: '12px 16px',
              color: 'var(--text-primary)',
              fontSize: 14,
              fontWeight: 500,
              pointerEvents: 'auto',
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              boxShadow: '0 8px 32px rgba(0,0,0,0.4)',
            }}
          >
            <span style={{ flex: 1 }}>{t.message}</span>
            <button
              onClick={() => remove(t.id)}
              style={{ color: 'var(--text-tertiary)', cursor: 'pointer', flexShrink: 0 }}
            >
              <X size={14} />
            </button>
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  )
}

'use client'
import { useEffect, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useRouter } from 'next/navigation'
import { userApi } from '@/lib/api'
import { BottomNav } from '@/components/layout/BottomNav'
import { ArrowLeft, Eye, Clock, CheckCheck, BookOpen, User, ChevronDown } from 'lucide-react'
import type { PrivacySettings, VisibilityOption } from '@/types'
import { toast } from '@/components/ui/toaster'

const OPTIONS: { value: VisibilityOption; label: string }[] = [
  { value: 'EVERYONE', label: 'Everyone' },
  { value: 'CONTACTS', label: 'Contacts' },
  { value: 'NOBODY', label: 'Nobody' },
]

const SETTINGS_CONFIG = [
  {
    key: 'onlineStatusVisibility' as keyof PrivacySettings,
    icon: Eye,
    label: 'Online Status',
    desc: 'Who can see when you\'re online',
  },
  {
    key: 'lastSeenVisibility' as keyof PrivacySettings,
    icon: Clock,
    label: 'Last Seen',
    desc: 'Who can see your last seen time',
  },
  {
    key: 'readReceiptsVisibility' as keyof PrivacySettings,
    icon: CheckCheck,
    label: 'Read Receipts',
    desc: 'Who can see when you\'ve read messages',
  },
  {
    key: 'storyVisibility' as keyof PrivacySettings,
    icon: BookOpen,
    label: 'Stories',
    desc: 'Who can view your stories',
  },
  {
    key: 'profilePictureVisibility' as keyof PrivacySettings,
    icon: User,
    label: 'Profile Picture',
    desc: 'Who can see your profile photo',
  },
  {
    key: 'aboutVisibility' as keyof PrivacySettings,
    icon: User,
    label: 'About',
    desc: 'Who can see your bio',
  },
]

export default function PrivacyPage() {
  const router = useRouter()
  const [settings, setSettings] = useState<PrivacySettings | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [openPicker, setOpenPicker] = useState<keyof PrivacySettings | null>(null)

  useEffect(() => {
    userApi.getPrivacy()
      .then((data) => setSettings(data as PrivacySettings))
      .finally(() => setLoading(false))
  }, [])

  async function handleChange(key: keyof PrivacySettings, value: VisibilityOption) {
    if (!settings) return
    const updated = { ...settings, [key]: value }
    setSettings(updated)
    setOpenPicker(null)
    setSaving(true)
    try {
      await userApi.updatePrivacy(updated)
      toast('Saved', 'success')
    } catch {
      toast('Failed to save', 'error')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div
      style={{
        minHeight: '100dvh',
        background: 'var(--bg-primary)',
        paddingTop: 'env(safe-area-inset-top)',
        paddingBottom: 100,
      }}
    >
      {/* Header */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          padding: '16px 16px 12px',
          position: 'sticky',
          top: 0,
          background: 'rgba(10,10,15,0.92)',
          backdropFilter: 'blur(20px)',
          borderBottom: '1px solid var(--border-subtle)',
          zIndex: 10,
        }}
      >
        <button
          onClick={() => router.back()}
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)', padding: 4 }}
        >
          <ArrowLeft size={22} />
        </button>
        <h1 style={{ fontSize: 20, fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
          Privacy
        </h1>
        {saving && (
          <motion.span
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            style={{ marginLeft: 'auto', fontSize: 13, color: 'var(--accent-bright)' }}
          >
            Saving…
          </motion.span>
        )}
      </div>

      <div style={{ padding: '24px 16px', display: 'flex', flexDirection: 'column', gap: 10 }}>
        {/* Intro card */}
        <div
          style={{
            background: 'var(--accent-ultra-dim)',
            border: '1px solid var(--border-default)',
            borderRadius: 16,
            padding: '14px 16px',
            marginBottom: 8,
          }}
        >
          <p style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.5 }}>
            Control who can see your information. Changes take effect immediately.
          </p>
        </div>

        {loading
          ? [...Array(6)].map((_, i) => <SettingSkeleton key={i} />)
          : SETTINGS_CONFIG.map(({ key, icon: Icon, label, desc }) => (
              <SettingRow
                key={key}
                icon={<Icon size={18} />}
                label={label}
                desc={desc}
                value={settings?.[key] as VisibilityOption || 'EVERYONE'}
                isOpen={openPicker === key}
                onToggle={() => setOpenPicker(openPicker === key ? null : key)}
                onChange={(v) => handleChange(key, v)}
              />
            ))}
      </div>

      <BottomNav />
    </div>
  )
}

function SettingRow({
  icon,
  label,
  desc,
  value,
  isOpen,
  onToggle,
  onChange,
}: {
  icon: React.ReactNode
  label: string
  desc: string
  value: VisibilityOption
  isOpen: boolean
  onToggle: () => void
  onChange: (v: VisibilityOption) => void
}) {
  const currentLabel = OPTIONS.find((o) => o.value === value)?.label || value

  return (
    <div
      style={{
        background: 'var(--surface-1)',
        border: `1px solid ${isOpen ? 'var(--border-default)' : 'var(--border-subtle)'}`,
        borderRadius: 16,
        overflow: 'hidden',
        transition: 'border-color 0.2s',
      }}
    >
      <motion.button
        whileTap={{ backgroundColor: 'var(--surface-2)' }}
        onClick={onToggle}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 14,
          width: '100%',
          padding: '16px 16px',
          background: 'none',
          border: 'none',
          cursor: 'pointer',
          textAlign: 'left',
        }}
      >
        <div
          style={{
            width: 38,
            height: 38,
            background: 'var(--accent-ultra-dim)',
            border: '1px solid var(--border-subtle)',
            borderRadius: 10,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'var(--accent-bright)',
            flexShrink: 0,
          }}
        >
          {icon}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <p style={{ fontSize: 15, fontWeight: 600, color: 'var(--text-primary)' }}>{label}</p>
          <p style={{ fontSize: 12, color: 'var(--text-tertiary)', marginTop: 2 }}>{desc}</p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexShrink: 0 }}>
          <span
            style={{
              fontSize: 13,
              fontWeight: 600,
              color: 'var(--accent-bright)',
              background: 'var(--accent-ultra-dim)',
              padding: '3px 10px',
              borderRadius: 20,
            }}
          >
            {currentLabel}
          </span>
          <motion.div animate={{ rotate: isOpen ? 180 : 0 }} transition={{ duration: 0.2 }}>
            <ChevronDown size={16} color="var(--text-tertiary)" />
          </motion.div>
        </div>
      </motion.button>

      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.22, ease: 'easeInOut' }}
            style={{ overflow: 'hidden' }}
          >
            <div
              style={{
                borderTop: '1px solid var(--border-subtle)',
                padding: '8px 12px 12px',
                display: 'flex',
                flexDirection: 'column',
                gap: 4,
              }}
            >
              {OPTIONS.map((opt) => {
                const isSelected = opt.value === value
                return (
                  <motion.button
                    key={opt.value}
                    whileTap={{ scale: 0.98 }}
                    onClick={() => onChange(opt.value)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 12,
                      padding: '12px 14px',
                      background: isSelected ? 'var(--accent-ultra-dim)' : 'none',
                      border: `1px solid ${isSelected ? 'var(--border-default)' : 'transparent'}`,
                      borderRadius: 12,
                      cursor: 'pointer',
                      textAlign: 'left',
                      transition: 'all 0.15s',
                    }}
                  >
                    <div
                      style={{
                        width: 20,
                        height: 20,
                        borderRadius: '50%',
                        border: `2px solid ${isSelected ? 'var(--accent)' : 'var(--border-default)'}`,
                        background: isSelected ? 'var(--accent)' : 'transparent',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexShrink: 0,
                        transition: 'all 0.15s',
                      }}
                    >
                      {isSelected && (
                        <motion.div
                          initial={{ scale: 0 }}
                          animate={{ scale: 1 }}
                          style={{
                            width: 8,
                            height: 8,
                            borderRadius: '50%',
                            background: 'white',
                          }}
                        />
                      )}
                    </div>
                    <span
                      style={{
                        fontSize: 15,
                        fontWeight: isSelected ? 600 : 400,
                        color: isSelected ? 'var(--text-primary)' : 'var(--text-secondary)',
                      }}
                    >
                      {opt.label}
                    </span>
                  </motion.button>
                )
              })}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

function SettingSkeleton() {
  return (
    <div
      style={{
        background: 'var(--surface-1)',
        border: '1px solid var(--border-subtle)',
        borderRadius: 16,
        padding: '16px',
        display: 'flex',
        alignItems: 'center',
        gap: 14,
      }}
    >
      <div style={{ width: 38, height: 38, borderRadius: 10, background: 'var(--surface-2)', animation: 'pulse 1.5s infinite' }} />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div style={{ height: 14, width: '40%', background: 'var(--surface-2)', borderRadius: 7, animation: 'pulse 1.5s infinite' }} />
        <div style={{ height: 11, width: '65%', background: 'var(--surface-2)', borderRadius: 5, animation: 'pulse 1.5s infinite' }} />
      </div>
    </div>
  )
}

'use client'
import { useEffect, useRef, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useRouter } from 'next/navigation'
import { userApi } from '@/lib/api'
import { useAuthStore } from '@/store/auth'
import { Avatar } from '@/components/ui/avatar-cl'
import { BottomNav } from '@/components/layout/BottomNav'
import { fileToBase64 } from '@/lib/utils'
import { toast } from '@/components/ui/toaster'
import { Camera, Edit2, LogOut, Shield, ChevronRight, Bell, Moon, HelpCircle } from 'lucide-react'
import type { UserProfile } from '@/types'

export default function ProfilePage() {
  const router = useRouter()
  const { profile, setProfile, logout } = useAuthStore()
  const [editing, setEditing] = useState(false)
  const [displayName, setDisplayName] = useState(profile?.displayName || '')
  const [aboutText, setAboutText] = useState('')
  const [saving, setSaving] = useState(false)
  const fileRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (!profile) {
      userApi.getProfile().then((p) => {
        const prof = p as UserProfile
        setProfile(prof)
        setDisplayName(prof.displayName || '')
      })
    } else {
      setDisplayName(profile.displayName || '')
    }
  }, [])

  async function handleSave() {
    if (!displayName.trim()) return
    setSaving(true)
    try {
      const updated = await userApi.updateProfile({ displayName, aboutText }) as UserProfile
      setProfile(updated)
      setEditing(false)
      toast('Profile updated', 'success')
    } catch {
      toast('Failed to save', 'error')
    } finally {
      setSaving(false)
    }
  }

  async function handleAvatarChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    try {
      const b64 = await fileToBase64(file)
      const res = await userApi.uploadAvatar(b64, file.type) as { profilePictureUrl: string }
      if (profile && res.profilePictureUrl) {
        setProfile({ ...profile, profilePictureUrl: res.profilePictureUrl })
        toast('Avatar updated', 'success')
      }
    } catch {
      toast('Failed to upload', 'error')
    }
  }

  function handleLogout() {
    logout()
    router.replace('/auth')
  }

  return (
    <div style={{ height: '100dvh', background: 'var(--bg-primary)', display: 'flex', flexDirection: 'column', paddingTop: 'env(safe-area-inset-top)', overflowY: 'auto' }}>
      {/* Header */}
      <div style={{ padding: '20px 20px 0', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexShrink: 0 }}>
        <h1 style={{ fontSize: 28, fontWeight: 800, letterSpacing: '-0.03em', background: 'linear-gradient(135deg, #f1f0ff, #a78bfa)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', backgroundClip: 'text' }}>
          Profile
        </h1>
        <motion.button whileTap={{ scale: 0.9 }} onClick={() => setEditing(!editing)}
          style={{ width: 36, height: 36, background: editing ? 'var(--accent-ultra-dim)' : 'var(--surface-2)', border: `1px solid ${editing ? 'var(--border-default)' : 'var(--border-subtle)'}`, borderRadius: 10, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', color: editing ? 'var(--accent-bright)' : 'var(--text-secondary)' }}>
          <Edit2 size={16} />
        </motion.button>
      </div>

      <div style={{ padding: '28px 20px', paddingBottom: 100 }}>
        {/* Avatar */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16, marginBottom: 32 }}>
          <div style={{ position: 'relative' }}>
            <Avatar src={profile?.profilePictureUrl} name={profile?.displayName} size="2xl" />
            <motion.button whileHover={{ scale: 1.1 }} whileTap={{ scale: 0.9 }} onClick={() => fileRef.current?.click()}
              style={{ position: 'absolute', bottom: 4, right: 4, width: 32, height: 32, background: 'linear-gradient(135deg, #7c3aed, #8b5cf6)', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', border: '3px solid var(--bg-primary)', cursor: 'pointer' }}>
              <Camera size={14} color="white" />
            </motion.button>
            <input ref={fileRef} type="file" accept="image/*" style={{ display: 'none' }} onChange={handleAvatarChange} />
          </div>

          <AnimatePresence mode="wait">
            {editing ? (
              <motion.div key="editing" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }}
                style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: 12 }}>
                <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} placeholder="Display name" className="input-field" style={{ textAlign: 'center', fontSize: 18, fontWeight: 600 }} autoFocus />
                <input value={aboutText} onChange={(e) => setAboutText(e.target.value)} placeholder="About (optional)" className="input-field" style={{ textAlign: 'center', fontSize: 15 }} />
                <div style={{ display: 'flex', gap: 10 }}>
                  <button className="btn-ghost" onClick={() => { setEditing(false); setDisplayName(profile?.displayName || '') }} style={{ flex: 1 }}>Cancel</button>
                  <button className="btn-primary" onClick={handleSave} disabled={saving} style={{ flex: 1 }}>{saving ? 'Saving…' : 'Save'}</button>
                </div>
              </motion.div>
            ) : (
              <motion.div key="viewing" initial={{ opacity: 0 }} animate={{ opacity: 1 }} style={{ textAlign: 'center' }}>
                <h2 style={{ fontSize: 24, fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>{profile?.displayName || 'Your Name'}</h2>
                <p style={{ fontSize: 15, color: 'var(--text-tertiary)', marginTop: 4 }}>{profile?.phoneNumber || ''}</p>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* Menu */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <MenuSection title="Account">
            <MenuItem icon={<Shield size={18} />} label="Privacy" onClick={() => router.push('/privacy')} />
            <MenuItem icon={<Bell size={18} />} label="Notifications" onClick={() => {}} />
          </MenuSection>
          <MenuSection title="Appearance">
            <MenuItem icon={<Moon size={18} />} label="Dark mode" value="On" onClick={() => {}} />
          </MenuSection>
          <MenuSection title="Support">
            <MenuItem icon={<HelpCircle size={18} />} label="Help Center" onClick={() => {}} />
          </MenuSection>
          <MenuSection title="">
            <MenuItem icon={<LogOut size={18} />} label="Log out" danger onClick={handleLogout} hiddenArrow />
          </MenuSection>
        </div>

        <p style={{ textAlign: 'center', marginTop: 28, fontSize: 12, color: 'var(--text-tertiary)' }}>CipherLink v1.0.0</p>
      </div>

      <BottomNav />
    </div>
  )
}

function MenuSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      {title && <p style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-tertiary)', letterSpacing: '0.06em', textTransform: 'uppercase', marginBottom: 8, paddingLeft: 4 }}>{title}</p>}
      <div style={{ background: 'var(--surface-1)', border: '1px solid var(--border-subtle)', borderRadius: 16, overflow: 'hidden' }}>
        {children}
      </div>
    </div>
  )
}

function MenuItem({ icon, label, value, onClick, danger, hiddenArrow }: {
  icon: React.ReactNode; label: string; value?: string
  onClick: () => void; danger?: boolean; hiddenArrow?: boolean
}) {
  return (
    <motion.button whileTap={{ backgroundColor: 'var(--surface-2)' }} onClick={onClick}
      style={{ display: 'flex', alignItems: 'center', gap: 14, width: '100%', padding: '15px 16px', background: 'none', border: 'none', borderBottom: '1px solid var(--border-subtle)', cursor: 'pointer', textAlign: 'left', color: danger ? 'var(--danger)' : 'var(--text-primary)', transition: 'background 0.15s' }}>
      <span style={{ color: danger ? 'var(--danger)' : 'var(--accent-bright)', flexShrink: 0 }}>{icon}</span>
      <span style={{ flex: 1, fontSize: 15, fontWeight: 500 }}>{label}</span>
      {value && <span style={{ fontSize: 14, color: 'var(--text-tertiary)' }}>{value}</span>}
      {!hiddenArrow && <ChevronRight size={16} color="var(--text-tertiary)" />}
    </motion.button>
  )
}

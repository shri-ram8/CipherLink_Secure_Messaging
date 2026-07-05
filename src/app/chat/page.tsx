'use client'
import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { useRouter } from 'next/navigation'
import { userApi, messageApi } from '@/lib/api'
import { useChatStore } from '@/store/chat'
import { Avatar } from '@/components/ui/avatar-cl'
import { BottomNav } from '@/components/layout/BottomNav'
import { ArrowLeft, Search, UserPlus, Phone } from 'lucide-react'
import type { Contact, Conversation } from '@/types'
import { toast } from '@/components/ui/toaster'

function contactDisplayName(c: Contact): string {
  return c.nickname || c.displayName || c.phoneNumber
}

export default function NewChatPage() {
  const router = useRouter()
  const { setConversations, conversations } = useChatStore()
  const [contacts, setContacts] = useState<Contact[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [adding, setAdding] = useState(false)
  const [newPhone, setNewPhone] = useState('')
  const [newName, setNewName] = useState('')
  const [showAdd, setShowAdd] = useState(false)
  const [searchResult, setSearchResult] = useState<{ found: boolean; checked: boolean } | null>(null)
  const [searching, setSearching] = useState(false)

  useEffect(() => {
    userApi.getContacts()
      .then((data) => setContacts(data as Contact[]))
      .finally(() => setLoading(false))
  }, [])

  const filtered = contacts.filter(
    (c) =>
      !c.isBlocked &&
      (contactDisplayName(c).toLowerCase().includes(search.toLowerCase()) ||
        c.phoneNumber.includes(search))
  )

  async function startChat(contact: Contact) {
    try {
      const existing = conversations.find((c) => c.otherUserId === contact.contactUserId)
      if (existing) {
        router.push(`/chat/${existing.id}`)
        return
      }
      // Backend expects receiverPhoneNumber
      const conv = await messageApi.createConversation(contact.phoneNumber) as Conversation
      setConversations([conv, ...conversations])
      router.push(`/chat/${conv.id}`)
    } catch {
      toast('Failed to open chat', 'error')
    }
  }

  // Check if phone number exists on CipherLink before saving
  async function checkPhone() {
    if (!newPhone.trim()) {
      toast('Enter a phone number', 'error')
      return
    }
    setSearching(true)
    setSearchResult(null)
    try {
      // Try to create a conversation — if user exists it works, if not throws
      await userApi.addContact(newPhone.trim(), newName.trim() || newPhone.trim())
      const updated = await userApi.getContacts() as Contact[]
      setContacts(updated)
      setShowAdd(false)
      setNewPhone('')
      setNewName('')
      setSearchResult(null)
      toast('Contact added!', 'success')
    } catch (e: unknown) {
      const msg = (e as Error).message || ''
      if (msg.includes('not found') || msg.includes('not on CipherLink')) {
        setSearchResult({ found: false, checked: true })
        toast('This number is not on CipherLink', 'error')
      } else if (msg.includes('already added')) {
        toast('Contact already added', 'error')
      } else {
        toast(msg || 'Failed to add contact', 'error')
      }
    } finally {
      setSearching(false)
    }
  }

  async function addContact() {
    if (!newPhone.trim() || !newName.trim()) {
      toast('Enter name and phone number', 'error')
      return
    }
    setAdding(true)
    try {
      await userApi.addContact(newPhone, newName)
      const updated = await userApi.getContacts() as Contact[]
      setContacts(updated)
      setShowAdd(false)
      setNewPhone('')
      setNewName('')
      toast('Contact added!', 'success')
    } catch (e: unknown) {
      toast((e as Error).message || 'Failed to add contact', 'error')
    } finally {
      setAdding(false)
    }
  }

  return (
    <div
      style={{
        height: '100dvh',
        background: 'var(--bg-primary)',
        display: 'flex',
        flexDirection: 'column',
        paddingTop: 'env(safe-area-inset-top)',
      }}
    >
      {/* Header */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          padding: '14px 16px',
          borderBottom: '1px solid var(--border-subtle)',
          flexShrink: 0,
        }}
      >
        <button
          onClick={() => router.back()}
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)', padding: 4 }}
        >
          <ArrowLeft size={22} />
        </button>
        <h1 style={{ fontSize: 18, fontWeight: 700, color: 'var(--text-primary)', flex: 1 }}>
          New Message
        </h1>
        <motion.button
          whileTap={{ scale: 0.9 }}
          onClick={() => setShowAdd(!showAdd)}
          style={{
            width: 36,
            height: 36,
            background: showAdd ? 'var(--accent-ultra-dim)' : 'var(--surface-2)',
            border: `1px solid ${showAdd ? 'var(--border-default)' : 'var(--border-subtle)'}`,
            borderRadius: 10,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            color: showAdd ? 'var(--accent-bright)' : 'var(--text-secondary)',
          }}
        >
          <UserPlus size={16} />
        </motion.button>
      </div>

      {/* Add contact form */}
      {showAdd && (
        <motion.div
          initial={{ height: 0, opacity: 0 }}
          animate={{ height: 'auto', opacity: 1 }}
          exit={{ height: 0, opacity: 0 }}
          style={{
            padding: '14px 16px',
            borderBottom: '1px solid var(--border-subtle)',
            background: 'var(--surface-1)',
            display: 'flex',
            flexDirection: 'column',
            gap: 10,
          }}
        >
          <p style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)' }}>
            Add Contact on CipherLink
          </p>
          <input
            placeholder="Name (e.g. Rahul)"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            style={{
              background: 'var(--surface-2)',
              border: '1px solid var(--border-subtle)',
              borderRadius: 10,
              padding: '10px 12px',
              fontSize: 14,
              color: 'var(--text-primary)',
              outline: 'none',
              width: '100%',
              boxSizing: 'border-box' as const,
            }}
          />
          <div style={{ display: 'flex', gap: 8 }}>
            <input
              placeholder="+91 98765 43210"
              value={newPhone}
              onChange={(e) => { setNewPhone(e.target.value); setSearchResult(null) }}
              onKeyDown={(e) => e.key === 'Enter' && addContact()}
              type="tel"
              style={{
                flex: 1,
                background: 'var(--surface-2)',
                border: `1px solid ${searchResult?.checked && !searchResult.found ? '#ef4444' : 'var(--border-subtle)'}`,
                borderRadius: 10,
                padding: '10px 12px',
                fontSize: 14,
                color: 'var(--text-primary)',
                outline: 'none',
              }}
            />
          </div>
          {searchResult?.checked && !searchResult.found && (
            <p style={{ fontSize: 12, color: '#ef4444', marginTop: -4 }}>
              ✗ This number is not on CipherLink
            </p>
          )}
          <button
            className="btn-primary"
            onClick={addContact}
            disabled={adding || searching}
            style={{ padding: '11px 20px' }}
          >
            {adding || searching ? 'Saving…' : 'Add Contact'}
          </button>
        </motion.div>
      )}

      {/* Search */}
      <div style={{ padding: '12px 16px', flexShrink: 0 }}>
        <div
          style={{
            background: 'var(--surface-2)',
            border: '1px solid var(--border-subtle)',
            borderRadius: 14,
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            padding: '10px 14px',
          }}
        >
          <Search size={16} color="var(--text-tertiary)" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search contacts"
            autoFocus
            style={{
              background: 'none',
              border: 'none',
              outline: 'none',
              flex: 1,
              fontSize: 15,
              color: 'var(--text-primary)',
            }}
          />
        </div>
      </div>

      {/* Contacts list */}
      <div style={{ flex: 1, overflowY: 'auto' }}>
        {loading ? (
          <div style={{ padding: '20px 20px', display: 'flex', flexDirection: 'column', gap: 16 }}>
            {[...Array(8)].map((_, i) => (
              <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                <div style={{ width: 44, height: 44, borderRadius: '50%', background: 'var(--surface-2)', animation: 'pulse 1.5s infinite' }} />
                <div style={{ flex: 1, height: 14, background: 'var(--surface-2)', borderRadius: 7, animation: 'pulse 1.5s infinite' }} />
              </div>
            ))}
          </div>
        ) : filtered.length === 0 ? (
          <div style={{ padding: 40, textAlign: 'center', color: 'var(--text-tertiary)' }}>
            <Phone size={32} style={{ margin: '0 auto 12px', display: 'block', opacity: 0.4 }} />
            <p style={{ fontSize: 15 }}>
              {search ? `No contacts matching "${search}"` : 'No contacts yet'}
            </p>
            <p style={{ fontSize: 13, marginTop: 6 }}>
              Tap the + button to add contacts
            </p>
          </div>
        ) : (
          <>
            <p style={{ padding: '8px 20px 4px', fontSize: 12, fontWeight: 600, color: 'var(--text-tertiary)', letterSpacing: '0.06em', textTransform: 'uppercase' }}>
              Contacts — {filtered.length}
            </p>
            {filtered.map((contact, i) => (
              <motion.div
                key={contact.contactUserId}
                initial={{ opacity: 0, y: 6 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.03 }}
                onClick={() => startChat(contact)}
                whileTap={{ backgroundColor: 'rgba(139,92,246,0.06)' }}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 14,
                  padding: '12px 20px',
                  cursor: 'pointer',
                }}
              >
                <Avatar name={contactDisplayName(contact)} size="md" />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <p style={{ fontSize: 15, fontWeight: 600, color: 'var(--text-primary)' }}>
                    {contactDisplayName(contact)}
                  </p>
                  <p style={{ fontSize: 13, color: 'var(--text-tertiary)', marginTop: 2 }}>
                    {contact.phoneNumber}
                  </p>
                </div>
              </motion.div>
            ))}
          </>
        )}
      </div>
      <BottomNav />
    </div>
  )
}

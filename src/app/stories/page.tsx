'use client'
import { useEffect, useState, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { storyApi } from '@/lib/api'
import { resolveMediaUrl } from '@/lib/utils'
import { useAuthStore } from '@/store/auth'
import { Avatar } from '@/components/ui/avatar-cl'
import { BottomNav } from '@/components/layout/BottomNav'
import { Plus, X, Send, Heart, ChevronLeft, ChevronRight, Eye } from 'lucide-react'
import type { Story, StoryFeedUser } from '@/types'
import { toast } from '@/components/ui/toaster'

export default function StoriesPage() {
  const { profile } = useAuthStore()
  const [feed, setFeed] = useState<StoryFeedUser[]>([])
  const [myStories, setMyStories] = useState<Story[]>([])
  const [loading, setLoading] = useState(true)
  const [viewingUser, setViewingUser] = useState<StoryFeedUser | null>(null)
  const [storyIndex, setStoryIndex] = useState(0)
  const [uploading, setUploading] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    loadFeed()
  }, [])

  async function loadFeed() {
    try {
      const [feedData, myData] = await Promise.all([
        storyApi.getFeed() as Promise<StoryFeedUser[]>,
        storyApi.getMyStories() as Promise<Story[]>,
      ])
      setFeed(feedData || [])
      setMyStories(myData || [])
    } finally {
      setLoading(false)
    }
  }

  function handleAddStoryClick() {
    fileInputRef.current?.click()
  }

  async function handleFileSelected(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return

    const isImage = file.type.startsWith('image/')
    const isVideo = file.type.startsWith('video/')
    if (!isImage && !isVideo) {
      toast('Please select an image or video', 'error')
      return
    }

    setUploading(true)
    try {
      // Convert to base64
      const base64 = await new Promise<string>((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = () => {
          const result = reader.result as string
          resolve(result.split(',')[1]) // strip data:...;base64, prefix
        }
        reader.onerror = reject
        reader.readAsDataURL(file)
      })

      // Get file extension e.g. ".jpg", ".mp4"
      const ext = file.name.includes('.')
        ? '.' + file.name.split('.').pop()!.toLowerCase()
        : isImage ? '.jpg' : '.mp4'

      // Send base64 directly to story endpoint — backend saves the file itself
      await storyApi.createStory({
        type: isImage ? 'IMAGE' : 'VIDEO',
        base64Content: base64,
        fileExtension: ext,
        caption: '',
      })

      toast('Story posted!', 'success')
      await loadFeed()
    } catch (err) {
      toast((err as Error).message || 'Failed to post story', 'error')
    } finally {
      setUploading(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  function openStory(user: StoryFeedUser) {
    setViewingUser(user)
    setStoryIndex(0)
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
      <div style={{ padding: '20px 20px 16px', flexShrink: 0 }}>
        <h1
          style={{
            fontSize: 28,
            fontWeight: 800,
            letterSpacing: '-0.03em',
            background: 'linear-gradient(135deg, #f1f0ff, #a78bfa)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            backgroundClip: 'text',
          }}
        >
          Stories
        </h1>
      </div>

      <div style={{ flex: 1, overflowY: 'auto', paddingBottom: 80 }}>
        {/* My story */}
        <div style={{ padding: '0 16px 20px' }}>
          <p style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-tertiary)', letterSpacing: '0.06em', textTransform: 'uppercase', marginBottom: 14 }}>
            Your Story
          </p>

          {/* Hidden file input */}
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*,video/*"
            style={{ display: 'none' }}
            onChange={handleFileSelected}
          />

          <div style={{ display: 'flex', gap: 16 }}>
            {/* Add story */}
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={handleAddStoryClick}
                disabled={uploading}
                style={{
                  width: 72,
                  height: 72,
                  borderRadius: '50%',
                  background: uploading ? 'var(--surface-3)' : 'var(--surface-2)',
                  border: '2px dashed var(--border-default)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  cursor: uploading ? 'not-allowed' : 'pointer',
                  position: 'relative',
                  overflow: 'hidden',
                }}
              >
                {myStories.length > 0 && myStories[0].contentUrl ? (
                  <img src={resolveMediaUrl(myStories[0].contentUrl)} style={{ width: '100%', height: '100%', objectFit: 'cover', position: 'absolute' }} />
                ) : null}
                <div
                  style={{
                    width: 26,
                    height: 26,
                    background: uploading ? 'var(--surface-3)' : 'linear-gradient(135deg, #7c3aed, #8b5cf6)',
                    borderRadius: '50%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    border: '3px solid var(--bg-primary)',
                    position: 'absolute',
                    bottom: 2,
                    right: 2,
                  }}
                >
                  <Plus size={14} color="white" />
                </div>
              </motion.button>
              <span style={{ fontSize: 11, color: 'var(--text-tertiary)', textAlign: 'center', maxWidth: 72, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {uploading ? 'Posting…' : 'Add Story'}
              </span>
            </div>

            {/* My existing stories */}
            {myStories.length > 0 && (
              <StoryCircle
                user={{
                  userId: profile?.userId || '',
                  userName: 'My Story',
                  userProfilePic: profile?.profilePictureUrl,
                  stories: myStories,
                  hasUnviewed: false,
                }}
                onPress={() => openStory({
                  userId: profile?.userId || '',
                  userName: 'My Story',
                  userProfilePic: profile?.profilePictureUrl,
                  stories: myStories,
                  hasUnviewed: false,
                })}
              />
            )}
          </div>
        </div>

        {/* Divider */}
        <div style={{ height: 1, background: 'var(--border-subtle)', margin: '0 16px 20px' }} />

        {/* Feed */}
        <div style={{ padding: '0 16px' }}>
          <p style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-tertiary)', letterSpacing: '0.06em', textTransform: 'uppercase', marginBottom: 14 }}>
            Recent
          </p>

          {loading ? (
            <StorySkeletons />
          ) : feed.length === 0 ? (
            <EmptyFeed />
          ) : (
            <div>
              {feed.map((user, i) => (
                <motion.div
                  key={user.userId}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i * 0.05 }}
                  onClick={() => openStory(user)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 14,
                    padding: '10px 0',
                    cursor: 'pointer',
                  }}
                >
                  <Avatar
                    src={user.userProfilePic}
                    name={user.userName}
                    size="lg"
                    hasStory
                    storyViewed={!user.hasUnviewed}
                  />
                  <div style={{ flex: 1 }}>
                    <p style={{ fontSize: 15, fontWeight: 600, color: 'var(--text-primary)' }}>
                      {user.userName}
                    </p>
                    <p style={{ fontSize: 13, color: 'var(--text-tertiary)', marginTop: 2 }}>
                      {user.stories.length} {user.stories.length === 1 ? 'story' : 'stories'}
                    </p>
                  </div>
                  {user.hasUnviewed && (
                    <div
                      style={{
                        width: 8,
                        height: 8,
                        borderRadius: '50%',
                        background: 'var(--accent)',
                      }}
                    />
                  )}
                </motion.div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Story viewer */}
      <AnimatePresence>
        {viewingUser && (
          <StoryViewer
            user={viewingUser}
            initialIndex={storyIndex}
            onClose={() => setViewingUser(null)}
          />
        )}
      </AnimatePresence>

      <BottomNav />
    </div>
  )
}

function StoryCircle({ user, onPress }: { user: StoryFeedUser; onPress: () => void }) {
  return (
    <div
      style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}
      onClick={onPress}
    >
      <Avatar
        src={user.userProfilePic}
        name={user.userName}
        size="lg"
        hasStory
        storyViewed={!user.hasUnviewed}
      />
      <span style={{ fontSize: 11, color: 'var(--text-secondary)', maxWidth: 72, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', textAlign: 'center' }}>
        {user.userName}
      </span>
    </div>
  )
}

// ── Story Viewer ──────────────────────────────────

function StoryViewer({
  user,
  initialIndex,
  onClose,
}: {
  user: StoryFeedUser
  initialIndex: number
  onClose: () => void
}) {
  const [current, setCurrent] = useState(initialIndex)
  const [progress, setProgress] = useState(0)
  const [paused, setPaused] = useState(false)
  const [reply, setReply] = useState('')
  const intervalRef = useRef<ReturnType<typeof setInterval> | undefined>(undefined)
  const DURATION = 5000

  const story = user.stories[current]

  useEffect(() => {
    storyApi.viewStory(story.id).catch(() => {})
  }, [current])

  useEffect(() => {
    setProgress(0)
    clearInterval(intervalRef.current)

    if (!paused) {
      intervalRef.current = setInterval(() => {
        setProgress((p) => {
          if (p >= 100) {
            goNext()
            return 0
          }
          return p + 100 / (DURATION / 100)
        })
      }, 100)
    }

    return () => clearInterval(intervalRef.current)
  }, [current, paused])

  function goNext() {
    if (current < user.stories.length - 1) {
      setCurrent((c) => c + 1)
    } else {
      onClose()
    }
  }

  function goPrev() {
    if (current > 0) {
      setCurrent((c) => c - 1)
    }
  }

  async function sendReply() {
    if (!reply.trim()) return
    try {
      await storyApi.replyToStory(story.id, reply)
      setReply('')
      toast('Replied!', 'success')
    } catch {
      toast('Failed to send reply', 'error')
    }
  }

  async function sendReaction(emoji: string) {
    await storyApi.reactToStory(story.id, emoji)
    toast(`${emoji} Reacted!`, 'success')
  }

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      style={{
        position: 'fixed',
        inset: 0,
        background: 'black',
        zIndex: 1000,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* Story content */}
      <div style={{ flex: 1, position: 'relative', overflow: 'hidden' }}>
        {story.type === 'IMAGE' && story.contentUrl && (
          <motion.img
            key={story.id}
            initial={{ scale: 1.05, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ duration: 0.3 }}
            src={resolveMediaUrl(story.contentUrl)}
            style={{
              width: '100%',
              height: '100%',
              objectFit: 'cover',
              position: 'absolute',
            }}
          />
        )}

        {story.type === 'TEXT' && (
          <div
            style={{
              position: 'absolute',
              inset: 0,
              background: story.backgroundColor || 'linear-gradient(135deg, #6d28d9, #8b5cf6)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: 32,
            }}
          >
            <p style={{ fontSize: 28, fontWeight: 700, color: 'white', textAlign: 'center', lineHeight: 1.3 }}>
              {story.caption}
            </p>
          </div>
        )}

        {/* Top gradient */}
        <div
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            height: 120,
            background: 'linear-gradient(to bottom, rgba(0,0,0,0.6), transparent)',
          }}
        />

        {/* Bottom gradient */}
        <div
          style={{
            position: 'absolute',
            bottom: 0,
            left: 0,
            right: 0,
            height: 180,
            background: 'linear-gradient(to top, rgba(0,0,0,0.8), transparent)',
          }}
        />

        {/* Progress bars */}
        <div
          style={{
            position: 'absolute',
            top: 'calc(env(safe-area-inset-top) + 12px)',
            left: 12,
            right: 12,
            display: 'flex',
            gap: 4,
          }}
        >
          {user.stories.map((_, i) => (
            <div
              key={i}
              style={{
                flex: 1,
                height: 2,
                background: 'rgba(255,255,255,0.3)',
                borderRadius: 1,
                overflow: 'hidden',
              }}
            >
              <motion.div
                style={{
                  height: '100%',
                  background: 'white',
                  borderRadius: 1,
                  width: i < current ? '100%' : i === current ? `${progress}%` : '0%',
                }}
              />
            </div>
          ))}
        </div>

        {/* Header */}
        <div
          style={{
            position: 'absolute',
            top: 'calc(env(safe-area-inset-top) + 24px)',
            left: 16,
            right: 16,
            display: 'flex',
            alignItems: 'center',
            gap: 12,
          }}
        >
          <Avatar src={user.userProfilePic} name={user.userName} size="sm" />
          <div style={{ flex: 1 }}>
            <p style={{ color: 'white', fontWeight: 700, fontSize: 15 }}>{user.userName}</p>
            <p style={{ color: 'rgba(255,255,255,0.6)', fontSize: 12 }}>
              {new Date(story.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            </p>
          </div>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <Eye size={14} color="rgba(255,255,255,0.6)" />
            <span style={{ color: 'rgba(255,255,255,0.6)', fontSize: 13 }}>{story.viewCount}</span>
          </div>
          <button
            onClick={onClose}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'white', padding: 4 }}
          >
            <X size={22} />
          </button>
        </div>

        {/* Tap zones */}
        <div
          style={{ position: 'absolute', left: 0, top: 80, width: '35%', bottom: 120 }}
          onClick={goPrev}
          onTouchStart={() => setPaused(true)}
          onTouchEnd={() => setPaused(false)}
        />
        <div
          style={{ position: 'absolute', right: 0, top: 80, width: '35%', bottom: 120 }}
          onClick={goNext}
          onTouchStart={() => setPaused(true)}
          onTouchEnd={() => setPaused(false)}
        />

        {/* Caption */}
        {story.caption && story.type !== 'TEXT' && (
          <p
            style={{
              position: 'absolute',
              bottom: 100,
              left: 20,
              right: 20,
              color: 'white',
              fontSize: 16,
              fontWeight: 500,
              textShadow: '0 1px 4px rgba(0,0,0,0.5)',
            }}
          >
            {story.caption}
          </p>
        )}
      </div>

      {/* Bottom bar */}
      <div
        style={{
          padding: '12px 16px',
          paddingBottom: 'calc(env(safe-area-inset-bottom) + 12px)',
          background: 'rgba(0,0,0,0.8)',
          display: 'flex',
          alignItems: 'center',
          gap: 10,
        }}
      >
        {/* Quick reactions */}
        <div style={{ display: 'flex', gap: 6 }}>
          {['❤️', '😂', '😮'].map((emoji) => (
            <motion.button
              key={emoji}
              whileHover={{ scale: 1.2 }}
              whileTap={{ scale: 0.9 }}
              onClick={() => sendReaction(emoji)}
              style={{ background: 'none', border: 'none', fontSize: 22, cursor: 'pointer' }}
            >
              {emoji}
            </motion.button>
          ))}
        </div>

        <div
          style={{
            flex: 1,
            background: 'rgba(255,255,255,0.1)',
            border: '1px solid rgba(255,255,255,0.15)',
            borderRadius: 22,
            display: 'flex',
            alignItems: 'center',
            padding: '8px 14px',
          }}
        >
          <input
            value={reply}
            onChange={(e) => setReply(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && sendReply()}
            placeholder="Reply…"
            style={{
              background: 'none',
              border: 'none',
              outline: 'none',
              flex: 1,
              fontSize: 14,
              color: 'white',
            }}
          />
        </div>

        <motion.button
          whileHover={{ scale: 1.1 }}
          whileTap={{ scale: 0.9 }}
          onClick={sendReply}
          style={{
            width: 38,
            height: 38,
            background: reply ? 'linear-gradient(135deg, #7c3aed, #8b5cf6)' : 'rgba(255,255,255,0.1)',
            border: 'none',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
          }}
        >
          <Send size={16} color="white" />
        </motion.button>
      </div>
    </motion.div>
  )
}

function StorySkeletons() {
  return (
    <div style={{ display: 'flex', gap: 14, overflowX: 'auto', padding: '0 0 8px' }}>
      {[...Array(5)].map((_, i) => (
        <div key={i} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8, flexShrink: 0 }}>
          <div style={{ width: 68, height: 68, borderRadius: '50%', background: 'var(--surface-2)', animation: 'pulse 1.5s infinite' }} />
          <div style={{ width: 50, height: 10, borderRadius: 5, background: 'var(--surface-2)', animation: 'pulse 1.5s infinite' }} />
        </div>
      ))}
    </div>
  )
}

function EmptyFeed() {
  return (
    <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--text-tertiary)' }}>
      <p style={{ fontSize: 16, marginBottom: 8 }}>No stories yet</p>
      <p style={{ fontSize: 14 }}>Stories from your contacts will appear here</p>
    </div>
  )
}

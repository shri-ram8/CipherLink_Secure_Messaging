'use client'
import { getInitials } from '@/lib/utils'
import { cn } from '@/lib/utils'

interface AvatarProps {
  src?: string
  name?: string
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl'
  isOnline?: boolean
  hasStory?: boolean
  storyViewed?: boolean
  className?: string
  onClick?: () => void
}

const sizeMap = {
  xs: { outer: 28, inner: 24, text: 10, dot: 8 },
  sm: { outer: 36, inner: 32, text: 13, dot: 10 },
  md: { outer: 44, inner: 40, text: 15, dot: 11 },
  lg: { outer: 56, inner: 52, text: 18, dot: 13 },
  xl: { outer: 72, inner: 68, text: 24, dot: 14 },
  '2xl': { outer: 96, inner: 92, text: 32, dot: 16 },
}

export function Avatar({
  src,
  name,
  size = 'md',
  isOnline,
  hasStory,
  storyViewed,
  className,
  onClick,
}: AvatarProps) {
  const dims = sizeMap[size]
  const initials = getInitials(name)

  return (
    <div
      className={cn('relative flex-shrink-0 select-none', onClick && 'cursor-pointer', className)}
      style={{ width: dims.outer, height: dims.outer }}
      onClick={onClick}
    >
      {/* Story ring */}
      {hasStory && (
        <div
          className={storyViewed ? 'story-ring-seen' : 'story-ring'}
          style={{
            position: 'absolute',
            inset: 0,
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <div
            style={{
              width: dims.inner,
              height: dims.inner,
              borderRadius: '50%',
              overflow: 'hidden',
              border: `2px solid var(--bg-primary)`,
              flexShrink: 0,
            }}
          >
            {src ? (
              <img
                src={src}
                alt={name}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              />
            ) : (
              <div
                style={{
                  width: '100%',
                  height: '100%',
                  background: 'linear-gradient(135deg, #6d28d9, #8b5cf6)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: dims.text,
                  fontWeight: 700,
                  color: 'white',
                }}
              >
                {initials}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Regular avatar */}
      {!hasStory && (
        <div
          style={{
            width: dims.outer,
            height: dims.outer,
            borderRadius: '50%',
            overflow: 'hidden',
            flexShrink: 0,
          }}
        >
          {src ? (
            <img
              src={src}
              alt={name}
              style={{ width: '100%', height: '100%', objectFit: 'cover' }}
            />
          ) : (
            <div
              style={{
                width: '100%',
                height: '100%',
                background: 'linear-gradient(135deg, #6d28d9, #8b5cf6)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: dims.text,
                fontWeight: 700,
                color: 'white',
              }}
            >
              {initials}
            </div>
          )}
        </div>
      )}

      {/* Online dot */}
      {isOnline && (
        <div
          style={{
            position: 'absolute',
            bottom: 1,
            right: 1,
            width: dims.dot,
            height: dims.dot,
            borderRadius: '50%',
            background: '#10b981',
            border: `2px solid var(--bg-primary)`,
          }}
        />
      )}
    </div>
  )
}

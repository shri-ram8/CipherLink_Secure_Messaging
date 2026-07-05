import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'
import { formatDistanceToNow, format, isToday, isYesterday, parseISO } from 'date-fns'
import { BASE_URL } from './api'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

// Backend returns relative paths for uploaded files (e.g. "/media/stories/x.jpg").
// Those need the API origin prefixed, since they're served by the backend, not Next.js.
export function resolveMediaUrl(url?: string | null): string | undefined {
  if (!url) return undefined
  if (/^(https?:|data:|blob:)/i.test(url)) return url
  return `${BASE_URL}${url.startsWith('/') ? '' : '/'}${url}`
}

export function formatMessageTime(dateStr: string): string {
  const date = typeof dateStr === 'string' ? parseISO(dateStr) : new Date(dateStr)
  return format(date, 'h:mm a')
}

export function formatConversationTime(dateStr?: string): string {
  if (!dateStr) return ''
  const date = typeof dateStr === 'string' ? parseISO(dateStr) : new Date(dateStr)
  if (isToday(date)) return format(date, 'h:mm a')
  if (isYesterday(date)) return 'Yesterday'
  return format(date, 'MMM d')
}

export function formatLastSeen(dateStr?: string): string {
  if (!dateStr) return 'last seen recently'
  const date = typeof dateStr === 'string' ? parseISO(dateStr) : new Date(dateStr)
  if (isToday(date)) return `last seen at ${format(date, 'h:mm a')}`
  if (isYesterday(date)) return `last seen yesterday at ${format(date, 'h:mm a')}`
  return `last seen ${format(date, 'MMM d')}`
}

export function formatDuration(seconds?: number): string {
  if (!seconds) return '0:00'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

export function formatCallDuration(seconds?: number): string {
  if (!seconds) return ''
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
  return `${m}:${s.toString().padStart(2, '0')}`
}

export function getInitials(name?: string): string {
  if (!name) return '?'
  return name
    .split(' ')
    .map((n) => n[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()
}

export function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const result = reader.result as string
      resolve(result.split(',')[1])
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

export function isImageUrl(url?: string): boolean {
  if (!url) return false
  return /\.(jpg|jpeg|png|gif|webp|svg)(\?.*)?$/i.test(url)
}

export function isVideoUrl(url?: string): boolean {
  if (!url) return false
  return /\.(mp4|webm|ogg|mov)(\?.*)?$/i.test(url)
}

export function extractUrlsFromText(text: string): string[] {
  const urlRegex = /(https?:\/\/[^\s]+)/g
  return text.match(urlRegex) || []
}

export function truncate(str: string, maxLength: number): string {
  if (str.length <= maxLength) return str
  return str.slice(0, maxLength) + '…'
}

export function vibrate(pattern: number | number[] = 10) {
  if (typeof navigator !== 'undefined' && navigator.vibrate) {
    navigator.vibrate(pattern)
  }
}

export const EMOJIS = ['❤️', '😂', '😮', '😢', '👏', '🔥']

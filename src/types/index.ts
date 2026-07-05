// ── Auth ─────────────────────────────────────────
export interface AuthResponse {
  accessToken: string
  refreshToken: string
  userId: string
  deviceId: string
  isNewUser: boolean
}

// ── User ─────────────────────────────────────────
export interface UserProfile {
  userId: string
  phoneNumber: string
  displayName: string
  aboutText?: string
  profilePictureUrl?: string
}

// ── Conversation ──────────────────────────────────
export interface Conversation {
  id: string
  otherUserId: string
  otherUserPhone: string
  otherUserName: string
  otherUserAvatarUrl?: string
  lastMessage?: string
  createdAt: string
  lastMessageAt?: string
  unreadCount: number
  isOnline: boolean
  lastSeen?: string
}

// ── Message ───────────────────────────────────────
export interface Message {
  id: string
  conversationId: string
  senderDeviceId: string
  encryptedPayload?: string
  isMine: boolean
  status: 'SENDING' | 'SENT' | 'DELIVERED' | 'SEEN'
  clientTempId?: string
  sentAt: string
  seenAt?: string
  isDeleted: boolean
  isEdited: boolean
  editedAt?: string
  isPinned: boolean
  replyToMessageId?: string
  replyPreview?: string
  mediaUrl?: string
  mediaType?: 'IMAGE' | 'VIDEO' | 'AUDIO' | 'DOCUMENT'
  voiceDurationSeconds?: number
  locationLat?: number
  locationLng?: number
  locationLabel?: string
  reactionCounts?: Record<string, number>
  myReaction?: string
  linkPreview?: LinkPreview
  senderName?: string
  senderAvatarUrl?: string
}

// ── Link Preview ──────────────────────────────────
export interface LinkPreview {
  url: string
  title?: string
  description?: string
  imageUrl?: string
  siteName?: string
}

// ── Story ─────────────────────────────────────────
export interface Story {
  id: string
  userId: string
  userName: string
  userProfilePic?: string
  type: 'IMAGE' | 'VIDEO' | 'TEXT'
  contentUrl?: string
  caption?: string
  backgroundColor?: string
  createdAt: string
  expiresAt: string
  viewCount: number
  hasViewed: boolean
}

export interface StoryFeedUser {
  userId: string
  userName: string
  userProfilePic?: string
  stories: Story[]
  hasUnviewed: boolean
}

// ── Group ─────────────────────────────────────────
export interface Group {
  id: string
  name: string
  description?: string
  groupPictureUrl?: string
  createdByUserId: string
  createdAt: string
  memberCount: number
  myRole: 'ADMIN' | 'MEMBER'
}

export interface GroupMessage {
  id: string
  groupId: string
  senderDeviceId: string
  senderUserId: string
  senderName: string
  encryptedPayload: string
  sentAt: string
}

// ── Call ──────────────────────────────────────────
export interface CallRecord {
  callId: string
  callerId: string
  receiverId: string
  callType: 'VOICE' | 'VIDEO'
  status: 'MISSED' | 'ANSWERED' | 'DECLINED' | 'ENDED'
  roomId: string
  startedAt: string
  answeredAt?: string
  endedAt?: string
  durationSeconds?: number
  otherUserName: string
  otherUserAvatarUrl?: string
  isIncoming: boolean
}

// ── Privacy ───────────────────────────────────────
export interface PrivacySettings {
  onlineStatusVisibility: VisibilityOption
  lastSeenVisibility: VisibilityOption
  readReceiptsVisibility: VisibilityOption
  storyVisibility: VisibilityOption
  profilePictureVisibility: VisibilityOption
  aboutVisibility: VisibilityOption
}

export type VisibilityOption = 'EVERYONE' | 'CONTACTS' | 'NOBODY'

// ── Contact ───────────────────────────────────────
export interface Contact {
  contactUserId: string
  phoneNumber: string
  displayName: string
  nickname?: string
  isBlocked: boolean
  isOnCipherLink: boolean
}

// ── Live Location ─────────────────────────────────
export interface LiveLocation {
  id: string
  userId: string
  conversationId: string
  latitude: number
  longitude: number
  accuracy?: number
  expiresAt: string
  isActive: boolean
}

// ── WebSocket Events ──────────────────────────────
export interface WsEvent<T = unknown> {
  event: string
  data?: T
  [key: string]: unknown
}

// ── Paged Result ──────────────────────────────────
export interface PagedResult<T> {
  content: T[]
  page: number
  pageSize: number
  totalCount: number
  totalPages: number
}

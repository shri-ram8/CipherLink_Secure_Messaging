export const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

function getToken(): string | null {
  if (typeof window === 'undefined') return null
  return localStorage.getItem('cl_access_token')
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${BASE_URL}${path}`, { ...options, headers })

  if (res.status === 401 || res.status === 403) {
    // Try refresh
    const refreshToken = localStorage.getItem('cl_refresh_token')
    if (refreshToken) {
      try {
        const refreshRes = await fetch(`${BASE_URL}/api/v1/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken }),
        })
        if (refreshRes.ok) {
          const data = await refreshRes.json()
          localStorage.setItem('cl_access_token', data.accessToken)
          localStorage.setItem('cl_refresh_token', data.refreshToken)
          // Retry original
          headers['Authorization'] = `Bearer ${data.accessToken}`
          const retry = await fetch(`${BASE_URL}${path}`, { ...options, headers })
          if (!retry.ok) throw new Error(`HTTP ${retry.status}`)
          return retry.json()
        }
      } catch {
        localStorage.clear()
        window.location.href = '/auth'
      }
    }
    localStorage.clear()
    window.location.href = '/auth'
    throw new Error('Unauthorized')
  }

  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || `HTTP ${res.status}`)
  }

  if (res.status === 204) return null as T
  return res.json()
}

// ── Auth ─────────────────────────────────────────
export const authApi = {
  verifyOtp: (firebaseToken: string, _phoneNumber: string) =>
    request('/api/v1/auth/verify-otp', {
      method: 'POST',
      body: JSON.stringify({ firebaseIdToken: firebaseToken }),
    }),
  refresh: (refreshToken: string) =>
    request('/api/v1/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    }),
}

// ── User ─────────────────────────────────────────
export const userApi = {
  getProfile: () => request('/api/v1/user/profile'),
  updateProfile: (data: { displayName?: string; aboutText?: string }) =>
    request('/api/v1/user/profile', { method: 'PUT', body: JSON.stringify(data) }),
  uploadAvatar: (base64Data: string, mimeType: string) =>
    request('/api/v1/user/profile/avatar', {
      method: 'POST',
      body: JSON.stringify({ base64ImageData: base64Data, mimeType }),
    }),
  getContacts: () => request('/api/v1/user/contacts'),
  addContact: (phoneNumber: string, name: string) =>
    request('/api/v1/user/contacts', {
      method: 'POST',
      body: JSON.stringify({ phoneNumber, nickname: name }),
    }),
  blockContact: (contactUserId: string) =>
    request(`/api/v1/user/contacts/${contactUserId}/block`, { method: 'PUT' }),
  getPrivacy: () => request('/api/v1/user/privacy'),
  updatePrivacy: (data: object) =>
    request('/api/v1/user/privacy', { method: 'PUT', body: JSON.stringify(data) }),
}

// ── Messages ──────────────────────────────────────
export const messageApi = {
  getConversations: () => request('/api/v1/message/conversations'),
  createConversation: (receiverPhoneNumber: string) =>
    request('/api/v1/message/conversations', {
      method: 'POST',
      body: JSON.stringify({ receiverPhoneNumber }),
    }),
  getMessages: (conversationId: string, page = 1, pageSize = 50) =>
    request(`/api/v1/message/conversations/${conversationId}/messages?page=${page}&pageSize=${pageSize}`),
  markSeen: (conversationId: string) =>
    request(`/api/v1/message/conversations/${conversationId}/seen`, { method: 'POST' }),
  deleteMessage: (messageId: string) =>
    request(`/api/v1/message/messages/${messageId}`, { method: 'DELETE' }),
  editMessage: (messageId: string, newPayload: string) =>
    request(`/api/v1/message/messages/${messageId}`, {
      method: 'PUT',
      body: JSON.stringify({ newPayload }),
    }),
  reactToMessage: (messageId: string, emoji: string) =>
    request(`/api/v1/message/messages/${messageId}/react`, {
      method: 'POST',
      body: JSON.stringify({ emoji }),
    }),
  forwardMessage: (messageId: string, targetConversationIds: string[], targetGroupIds: string[]) =>
    request(`/api/v1/message/messages/${messageId}/forward`, {
      method: 'POST',
      body: JSON.stringify({ targetConversationIds, targetGroupIds }),
    }),
  pinMessage: (messageId: string, pin: boolean) =>
    request(`/api/v1/message/messages/${messageId}/pin?pin=${pin}`, { method: 'PUT' }),
  getPinnedMessages: (conversationId: string) =>
    request(`/api/v1/message/conversations/${conversationId}/pinned`),
  searchMessages: (conversationId: string, q: string) =>
    request(`/api/v1/message/conversations/${conversationId}/search?q=${encodeURIComponent(q)}`),
  getLinkPreview: (url: string) =>
    request('/api/v1/message/link-preview', { method: 'POST', body: JSON.stringify({ url }) }),
}

// ── Stories ───────────────────────────────────────
export const storyApi = {
  getFeed: () => request('/api/v1/stories'),
  getMyStories: () => request('/api/v1/stories/mine'),
  createStory: (data: object) =>
    request('/api/v1/stories', { method: 'POST', body: JSON.stringify(data) }),
  viewStory: (storyId: string) =>
    request(`/api/v1/stories/${storyId}/view`, { method: 'POST' }),
  reactToStory: (storyId: string, emoji: string) =>
    request(`/api/v1/stories/${storyId}/react`, {
      method: 'POST',
      body: JSON.stringify({ emoji }),
    }),
  replyToStory: (storyId: string, message: string) =>
    request(`/api/v1/stories/${storyId}/reply`, {
      method: 'POST',
      body: JSON.stringify({ message }),
    }),
  deleteStory: (storyId: string) =>
    request(`/api/v1/stories/${storyId}`, { method: 'DELETE' }),
  getViewers: (storyId: string) => request(`/api/v1/stories/${storyId}/viewers`),
}

// ── Groups ────────────────────────────────────────
export const groupApi = {
  getMyGroups: () => request('/api/group/my'),
  createGroup: (data: object) =>
    request('/api/group', { method: 'POST', body: JSON.stringify(data) }),
  updateGroup: (groupId: string, data: object) =>
    request(`/api/group/${groupId}`, { method: 'PUT', body: JSON.stringify(data) }),
  getMembers: (groupId: string) => request(`/api/group/${groupId}/members`),
  addMembers: (groupId: string, userIds: string[]) =>
    request(`/api/group/${groupId}/members`, {
      method: 'POST',
      body: JSON.stringify({ userIds }),
    }),
  removeMember: (groupId: string, targetUserId: string) =>
    request(`/api/group/${groupId}/members/${targetUserId}`, { method: 'DELETE' }),
  leaveGroup: (groupId: string) =>
    request(`/api/group/${groupId}/leave`, { method: 'POST' }),
  promoteToAdmin: (groupId: string, targetUserId: string) =>
    request(`/api/group/${groupId}/promote/${targetUserId}`, { method: 'POST' }),
  getMessages: (groupId: string, page = 1) =>
    request(`/api/group/${groupId}/messages?page=${page}`),
  markSeen: (groupMessageId: string) =>
    request(`/api/group/messages/${groupMessageId}/seen`, { method: 'POST' }),
}

// ── Calls ─────────────────────────────────────────
export const callApi = {
  initiateCall: (receiverUserId: string, callType: 'VOICE' | 'VIDEO') =>
    request('/api/v1/calls/initiate', {
      method: 'POST',
      body: JSON.stringify({ receiverId: receiverUserId, callType }),
    }),
  endCall: (callId: string, status: string, durationSeconds?: number) =>
    request(`/api/v1/calls/${callId}/end`, {
      method: 'PUT',
      body: JSON.stringify({ status, durationSeconds }),
    }),
  getHistory: () => request('/api/v1/calls/history'),
  getMissedCount: () => request('/api/v1/calls/missed/count'),
}

// ── Media ─────────────────────────────────────────
export const mediaApi = {
  upload: (base64Data: string, mimeType: string, fileName?: string) =>
    request('/api/v1/media/upload', {
      method: 'POST',
      body: JSON.stringify({ base64Data, mimeType, fileName }),
    }),
  registerFcm: (fcmToken: string) =>
    request('/api/v1/media/register-fcm', {
      method: 'POST',
      body: JSON.stringify({ fcmToken }),
    }),
}

// ── Live Location ─────────────────────────────────
export const locationApi = {
  start: (conversationId: string, latitude: number, longitude: number, durationMinutes: number) =>
    request('/api/v1/location/start', {
      method: 'POST',
      body: JSON.stringify({ conversationId, latitude, longitude, durationMinutes }),
    }),
  update: (locationId: string, latitude: number, longitude: number, accuracy?: number) =>
    request(`/api/v1/location/${locationId}`, {
      method: 'PUT',
      body: JSON.stringify({ latitude, longitude, accuracy }),
    }),
  stop: (locationId: string) =>
    request(`/api/v1/location/${locationId}`, { method: 'DELETE' }),
  getActive: (conversationId: string) =>
    request(`/api/v1/location/conversation/${conversationId}`),
}

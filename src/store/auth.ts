'use client'
import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { UserProfile } from '@/types'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  userId: string | null
  deviceId: string | null
  profile: UserProfile | null
  isAuthenticated: boolean

  setAuth: (data: {
    accessToken: string
    refreshToken: string
    userId: string
    deviceId: string
  }) => void
  setProfile: (profile: UserProfile) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      userId: null,
      deviceId: null,
      profile: null,
      isAuthenticated: false,

      setAuth: (data) => {
        localStorage.setItem('cl_access_token', data.accessToken)
        localStorage.setItem('cl_refresh_token', data.refreshToken)
        set({
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
          userId: data.userId,
          deviceId: data.deviceId,
          isAuthenticated: true,
        })
      },

      setProfile: (profile) => set({ profile }),

      logout: () => {
        localStorage.removeItem('cl_access_token')
        localStorage.removeItem('cl_refresh_token')
        set({
          accessToken: null,
          refreshToken: null,
          userId: null,
          deviceId: null,
          profile: null,
          isAuthenticated: false,
        })
      },
    }),
    {
      name: 'cl-auth',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        userId: state.userId,
        deviceId: state.deviceId,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
)

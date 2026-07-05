import type { Metadata, Viewport } from 'next'
import './globals.css'
import { Toaster } from '@/components/ui/toaster'
import { WebSocketProvider } from '@/components/providers/WebSocketProvider'

export const metadata: Metadata = {
  title: 'CipherLink',
  description: 'Private. Social. Yours.',
  manifest: '/manifest.json',
  appleWebApp: { capable: true, statusBarStyle: 'black-translucent' },
}

export const viewport: Viewport = {
  themeColor: '#0a0a0f',
  width: 'device-width',
  initialScale: 1,
  maximumScale: 1,
  viewportFit: 'cover',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body>
        <WebSocketProvider>
          {children}
        </WebSocketProvider>
        <Toaster />
      </body>
    </html>
  )
}

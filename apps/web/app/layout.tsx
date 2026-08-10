import type { Metadata, Viewport } from 'next';
import type { ReactNode } from 'react';
import '@cerp/core-web/styles.css';
import './globals.css';

export const metadata: Metadata = {
  title: 'Construction Hub',
  description: 'ERP for Properties & Construction',
  manifest: '/manifest.json',
  icons: {
    icon: [{ url: '/icons/favicon-32.png', sizes: '32x32', type: 'image/png' }],
    apple: [{ url: '/icons/apple-touch-icon.png', sizes: '180x180', type: 'image/png' }],
  },
};

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  viewportFit: 'cover',
  themeColor: '#d97706',
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}

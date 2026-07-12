import type { Metadata } from 'next';
import type { ReactNode } from 'react';
import '@cerp/core-web/styles.css';
import './globals.css';

export const metadata: Metadata = {
  title: 'Acme Property Compliance',
  description: 'Demo customer overlay on top of cerp core',
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}

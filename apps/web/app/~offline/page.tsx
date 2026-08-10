'use client';

// Precached by next-pwa (see `fallbacks.document` in next.config.ts) and
// served by the service worker in place of the browser's own offline error
// page whenever a navigation request fails with no network. Deliberately
// styled with inline styles rather than Tailwind classes: this page must
// render correctly from the precache alone, without depending on the app's
// CSS chunk also having made it into that same cache.
export default function OfflinePage() {
  return (
    <main
      style={{
        display: 'flex',
        minHeight: '100vh',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '0.75rem',
        padding: '2rem',
        textAlign: 'center',
        backgroundColor: '#111827',
        color: '#ffffff',
        fontFamily: 'system-ui, -apple-system, sans-serif',
      }}
    >
      <div
        style={{
          width: '3rem',
          height: '3rem',
          borderRadius: '9999px',
          backgroundColor: '#d97706',
        }}
        aria-hidden="true"
      />
      <h1 style={{ fontSize: '1.25rem', fontWeight: 600, margin: 0 }}>You&apos;re offline</h1>
      <p style={{ maxWidth: '28rem', color: '#d4d4d8', margin: 0 }}>
        No connection right now. Anything you capture stays saved on this device and uploads
        automatically once you&apos;re back online.
      </p>
      <button
        type="button"
        onClick={() => window.location.reload()}
        style={{
          marginTop: '0.5rem',
          padding: '0.5rem 1.25rem',
          borderRadius: '0.5rem',
          border: 'none',
          backgroundColor: '#d97706',
          color: '#111827',
          fontWeight: 600,
          cursor: 'pointer',
        }}
      >
        Try again
      </button>
    </main>
  );
}

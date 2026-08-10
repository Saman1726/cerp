import type { NextConfig } from 'next';
import path from 'node:path';
import withPWAInit from '@ducanh2912/next-pwa';

const nextConfig: NextConfig = {
  output: 'standalone',
  outputFileTracingRoot: path.join(__dirname, '../../'),
  transpilePackages: ['@cerp/core-web', '@cerp/shared-types'],
};

const withPWA = withPWAInit({
  dest: 'public',
  register: true,
  reloadOnOnline: true,
  // A field worker who reopens the app mid-drive with zero signal gets this
  // instead of the browser's default offline error page.
  fallbacks: {
    document: '/~offline',
  },
  cacheOnFrontEndNav: true,
  workboxOptions: {
    // Add project-specific runtime caching (e.g. GET /api/v1/projects) here
    // with `extendDefaultRuntimeCaching: true` if read-access-while-offline
    // is needed later; the write path (photo capture) is already covered by
    // the IndexedDB queue in @cerp/core-web/site-capture, which doesn't rely
    // on the service worker at all.
    disableDevLogs: true,
  },
});

// @ducanh2912/next-pwa generates the service worker via workbox-webpack-plugin,
// which needs the classic webpack compiler (see its `webpack` peer dependency) -
// Turbopack, Next 16's default, doesn't run webpack plugins at all and errors
// outright if the exported config carries a `webpack()` key. So this only wraps
// the config for the production build - run as `next build --webpack` (see
// package.json) - and leaves `next dev` on plain Turbopack, since there's no
// service worker to exercise locally anyway.
export default process.env.NODE_ENV === 'production' ? withPWA(nextConfig) : nextConfig;

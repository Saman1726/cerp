import nextConfig from '@cerp/eslint-config/next.mjs';

const config = [
  ...nextConfig,
  {
    rules: {
      '@next/next/no-html-link-for-pages': 'off',
    },
  },
];

export default config;

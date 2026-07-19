import nextConfig from '@cerp/eslint-config/next.mjs';

const config = [
  ...nextConfig,
  {
    rules: {
      '@next/next/no-html-link-for-pages': 'off',
      '@typescript-eslint/no-unused-vars': ['error', {
        argsIgnorePattern: '^_',
      }],
    },
  },
];

export default config;

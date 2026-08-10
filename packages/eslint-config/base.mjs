/* @ts-check */
import js from '@eslint/js';
import tseslint from 'typescript-eslint';
import prettier from 'eslint-config-prettier';
import importPlugin from 'eslint-plugin-import';

export default tseslint.config(
  js.configs.recommended,
  ...tseslint.configs.recommended,
  prettier,
  {
    // public/sw.js & friends are next-pwa/Workbox build output (regenerated
    // by `next build --webpack`, content-hashed filenames), not source -
    // same category as .next/** just below.
    ignores: [
      'dist/**',
      '.next/**',
      '.turbo/**',
      'coverage/**',
      'node_modules/**',
      'public/sw.js',
      'public/workbox-*.js',
      'public/swe-worker-*.js',
      'public/fallback-*.js',
    ],
  },
  {
    files: ['**/*.{ts,tsx,js,jsx}'],
    plugins: {
      import: importPlugin,
    },
    settings: {
      'import/resolver': {
        typescript: {
          project: ['packages/*/tsconfig.json', 'apps/*/tsconfig.json', 'services/*/tsconfig.json'],
        },
        node: {
          extensions: ['.js', '.jsx', '.ts', '.tsx'],
        },
      },
    },
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: ['apps/*', 'services/*'],
              message:
                'Do not import across apps/services. Use packages/* public API instead.',
            },
            {
              group: ['@cerp/*/src/**', '@cerp/*/dist/**'],
              message:
                'Do not import package internals (src or dist). Use public exports or documented subpath exports.',
            },
          ],
        },
      ],
      'import/no-internal-modules': [
        'error',
        {
          forbid: ['@cerp/*/src/**', '@cerp/*/dist/**'],
        },
      ],
    },
  },
  {
    files: ['**/*.spec.ts', '**/*.e2e-spec.ts', '**/*.test.ts', '**/*.spec.tsx', '**/*.test.tsx'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: ['apps/*', 'services/*'],
              message:
                'Do not import across apps/services. Use packages/* public API instead.',
            }
          ],
        },
      ],
    },
  },
);

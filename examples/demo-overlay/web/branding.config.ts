import type { BrandingConfig } from '@cerp/core-web/shell';

// Deliberately distinct from the reference app's branding.config.ts - proves
// the branding extension point actually changes what renders, not just that
// it typechecks.
export const branding: BrandingConfig = {
  productName: 'Acme Property Compliance',
  colors: {
    primary: '#166534',
    primaryForeground: '#ffffff',
    background: '#f0fdf4',
  },
};

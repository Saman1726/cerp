// @ts-check
import nextCoreWebVitals from 'eslint-config-next/core-web-vitals';
import base from './base.mjs';

// eslint-config-next ships a native flat-config array now - no legacy
// FlatCompat bridge needed (that path hit a circular-reference crash when
// eslint-plugin-react's config objects were re-validated through it).
export default [...base, ...nextCoreWebVitals];

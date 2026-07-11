import { z } from 'zod';

export const loginRequestSchema = z.object({
  tenantSlug: z
    .string()
    .min(2)
    .max(32)
    .regex(/^[a-z][a-z0-9-]*$/, 'Lowercase letters, numbers and hyphens only'),
  email: z.string().email(),
  password: z.string().min(8),
});
export type LoginRequest = z.infer<typeof loginRequestSchema>;

export interface TenantSummary {
  slug: string;
  name: string;
}

export interface AuthUser {
  id: string;
  email: string;
  role: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
  tenant: TenantSummary;
}

import { z } from 'zod';

export const loginRequestSchema = z.object({
  username: z.string().min(1),
  password: z.string().min(6),
});
export type LoginRequest = z.infer<typeof loginRequestSchema>;


export type AccountType = 'USER' | 'VENDOR' | 'COMPANY';

export interface AuthUser {
  id: string;
  email: string;
  role: string;
  // Optional: a token refresh can't recover this today (the dev JWT never carried it),
  // so callers must handle it being absent rather than treating it as always present.
  type?: AccountType;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
}

import { z } from 'zod';

export interface Property {
  id: string;
  name: string;
  address: string;
  createdAt: string;
  updatedAt: string;
}

export const createPropertyRequestSchema = z.object({
  name: z.string().min(1).max(200),
  address: z.string().min(1).max(500),
});
export type CreatePropertyRequest = z.infer<typeof createPropertyRequestSchema>;

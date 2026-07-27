import { z } from 'zod';

// ── Vendor Category Types ──

/**
 * Predefined construction category codes used across the system.
 * These categories are seeded in the database and can only be selected
 * from (not custom-created) when vendors register or edit profiles.
 */
export const VENDOR_CATEGORY_CODES = [
  // Trade & Services
  'foundation_structural',
  'electrical',
  'plumbing_hvac',
  'carpentry_framing',
  'masonry_concrete',
  'roofing_waterproofing',
  'painting_finishing',
  'glazing_windows',
  'metal_works_steel',
  'flooring_tiling',
  'fire_protection',
  'insulation_energy',
  // Materials & Supplies
  'lumber_building_materials',
  'doors_windows_supplies',
  'safety_equipment_supplies',
  // Equipment & Technology
  'construction_equipment_rental',
  'construction_tools',
  'smart_building_systems',
  'building_management_software',
  // Specialized Services
  'site_prep_excavation',
  'landscaping_exterior',
  'sustainability_green_building',
  'structural_engineering',
  'architectural_services',
  'logistics_cranes',
] as const;

export type VendorCategoryCode = (typeof VENDOR_CATEGORY_CODES)[number];

export interface VendorCategory {
  id: string;
  code: VendorCategoryCode;
  name: string;
  description: string | null;
  icon: string | null; // lucide-react icon name
  displayOrder: number;
  groupId: string | null;
  groupName: string | null;
}

export interface VendorCategoryGroup {
  id: string;
  name: string;
  description: string | null;
  displayOrder: number;
  isActive: boolean;
  categories: VendorCategory[];
}

// ── Vendor Types ──

export type VendorStatus = 'pending' | 'approved' | 'rejected';

export interface Vendor {
  id: string;
  tenantId: string;
  legalName: string;
  status: VendorStatus;
  taxProfile?: Record<string, unknown>;
  /** @deprecated Use categoriesLink instead. Kept for backward compatibility. */
  categories: string[];
  categoriesLink: VendorCategoryLink[];
  city?: string;
  ratingAvg?: number;
  createdAt: string;
  updatedAt?: string;
}

export interface VendorCategoryLink {
  categoryId: string;
  categoryName: string;
  categoryCode: VendorCategoryCode;
}

export interface CreateVendorRequest {
  legalName: string;
  city?: string;
  taxProfile?: Record<string, unknown>;
  categoryIds: string[]; // UUIDs of category definitions
}

export interface UpdateVendorRequest {
  legalName?: string;
  city?: string;
  taxProfile?: Record<string, unknown>;
  categoryIds?: string[];
}

// ── Category Management Types ──

export type CategoryGroupResponse = {
  id: string;
  name: string;
  description: string | null;
  displayOrder: number;
  isActive: boolean;
  categories: VendorCategory[];
};

export interface CreateCategoryGroupRequest {
  name: string;
  description?: string;
  displayOrder?: number;
}

export interface UpdateCategoryGroupRequest {
  name?: string;
  description?: string;
  displayOrder?: number;
  isActive?: boolean;
}

export interface CreateCategoryRequest {
  groupId: string;
  code: VendorCategoryCode;
  name: string;
  description?: string;
  icon?: string;
  displayOrder?: number;
}

export interface UpdateCategoryRequest {
  name?: string;
  description?: string;
  icon?: string;
  displayOrder?: number;
  isActive?: boolean;
}

// ── User Profile Types ──

export type UserProfileType = 'individual' | 'company' | 'vendor';

export interface UserProfile {
  id: string;
  email: string;
  name: string;
  phone?: string;
  type: UserProfileType;
  createdAt: string;
  updatedAt?: string;
}

export interface UpdateUserProfileRequest {
  name?: string;
  phone?: string;
  avatarUrl?: string;
}

// ── Company/Organisation Profile Types ──

export interface CompanyProfile {
  id: string;
  tenantId: string;
  name: string;
  legalName: string;
  description?: string;
  industry?: string;
  companySize?: string;
  website?: string;
  addressLine1?: string;
  addressLine2?: string;
  postalCode?: string;
  city?: string;
  countryCode?: string;
  phone?: string;
  email?: string;
  taxId?: string;
  vatNumber?: string;
  registrationNumber?: string;
  avatarUrl?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface CreateCompanyProfileRequest {
  name: string;
  legalName?: string;
  description?: string;
  industry?: string;
  companySize?: string;
  website?: string;
  addressLine1?: string;
  addressLine2?: string;
  postalCode?: string;
  city?: string;
  countryCode?: string;
  phone?: string;
  email?: string;
  taxId?: string;
  vatNumber?: string;
  registrationNumber?: string;
}

export interface UpdateCompanyProfileRequest {
  name?: string;
  legalName?: string;
  description?: string;
  industry?: string;
  companySize?: string;
  website?: string;
  addressLine1?: string;
  addressLine2?: string;
  postalCode?: string;
  city?: string;
  countryCode?: string;
  phone?: string;
  email?: string;
  taxId?: string;
  vatNumber?: string;
  registrationNumber?: string;
  avatarUrl?: string;
}

// ── Vendor Profile Types ──

export interface VendorProfile {
  id: string;
  tenantId: string;
  vendorId: string;
  legalName: string;
  companyName?: string;
  contactPerson?: string;
  contactEmail?: string;
  contactPhone?: string;
  website?: string;
  addressLine1?: string;
  addressLine2?: string;
  postalCode?: string;
  city?: string;
  countryCode?: string;
  taxProfile?: Record<string, unknown>;
  categories: VendorCategoryLink[];
  status: VendorStatus;
  ratingAvg?: number;
  avatarUrl?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface UpdateVendorProfileRequest {
  legalName?: string;
  companyName?: string;
  contactPerson?: string;
  contactEmail?: string;
  contactPhone?: string;
  website?: string;
  addressLine1?: string;
  addressLine2?: string;
  postalCode?: string;
  city?: string;
  countryCode?: string;
  taxProfile?: Record<string, unknown>;
  categoryIds?: string[];
  avatarUrl?: string;
}

// ── Admin/Manager Types ──

export type UserRole = 'ADMIN' | 'USER' | 'VIEWER' | 'EDITOR';

export interface UserListItem {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  status: 'ACTIVE' | 'INVITED' | 'SUSPENDED';
  lastLoginAt?: string;
  createdAt: string;
}

export interface AssignUserRoleRequest {
  userId: string;
  role: UserRole;
}

export interface CreateUserAdminRequest {
  email: string;
  name: string;
  role?: UserRole;
}

// ── Validation Schemas ──

export const createVendorSchema = z.object({
  legalName: z.string().min(1, 'Legal name is required').max(200),
  city: z.string().max(100).optional(),
  taxProfile: z.record(z.string(), z.unknown()).optional(),
  categoryIds: z.array(z.string().uuid()).min(1, 'At least one category is required'),
});

export const updateVendorSchema = z.object({
  legalName: z.string().max(200).optional(),
  city: z.string().max(100).optional(),
  taxProfile: z.record(z.string(), z.unknown()).optional(),
  categoryIds: z.array(z.string().uuid()).optional(),
});

export const createCategoryGroupSchema = z.object({
  name: z.string().min(1).max(100),
  description: z.string().max(500).optional(),
  displayOrder: z.number().int().nonnegative().optional(),
});

export const updateCategoryGroupSchema = z.object({
  name: z.string().max(100).optional(),
  description: z.string().max(500).optional(),
  displayOrder: z.number().int().nonnegative().optional(),
  isActive: z.boolean().optional(),
});

export const createCategorySchema = z.object({
  groupId: z.string().uuid(),
  code: z.enum(VENDOR_CATEGORY_CODES),
  name: z.string().min(1).max(200),
  description: z.string().max(500).optional(),
  icon: z.string().max(50).optional(),
  displayOrder: z.number().int().nonnegative().optional(),
});

export const updateCategorySchema = z.object({
  name: z.string().max(200).optional(),
  description: z.string().max(500).optional(),
  icon: z.string().max(50).optional(),
  displayOrder: z.number().int().nonnegative().optional(),
  isActive: z.boolean().optional(),
});

export const updateUserProfileSchema = z.object({
  name: z.string().min(1).max(200).optional(),
  phone: z.string().max(50).optional(),
  avatarUrl: z.string().url().optional(),
});

export const updateCompanyProfileSchema = z.object({
  name: z.string().max(200).optional(),
  legalName: z.string().max(200).optional(),
  description: z.string().max(2000).optional(),
  industry: z.string().max(100).optional(),
  companySize: z.string().max(50).optional(),
  website: z.string().url().optional(),
  addressLine1: z.string().max(200).optional(),
  addressLine2: z.string().max(200).optional(),
  postalCode: z.string().max(20).optional(),
  city: z.string().max(100).optional(),
  countryCode: z.string().max(10).optional(),
  phone: z.string().max(50).optional(),
  email: z.string().email().optional(),
  taxId: z.string().max(100).optional(),
  vatNumber: z.string().max(100).optional(),
  registrationNumber: z.string().max(100).optional(),
  avatarUrl: z.string().url().optional(),
});

export const updateVendorProfileSchema = z.object({
  legalName: z.string().max(200).optional(),
  companyName: z.string().max(200).optional(),
  contactPerson: z.string().max(200).optional(),
  contactEmail: z.string().email().optional(),
  contactPhone: z.string().max(50).optional(),
  website: z.string().url().optional(),
  addressLine1: z.string().max(200).optional(),
  addressLine2: z.string().max(200).optional(),
  postalCode: z.string().max(20).optional(),
  city: z.string().max(100).optional(),
  countryCode: z.string().max(10).optional(),
  taxProfile: z.record(z.string(), z.unknown()).optional(),
  categoryIds: z.array(z.string().uuid()).optional(),
  avatarUrl: z.string().url().optional(),
});

// ── Construction Site (Property Domain) ──

export interface Unit {
  id: string;
  label: string;
  usableArea: number;
  bedrooms: number;
  bathrooms: number;
  balconies: number;
  toilets: number;
}

export interface Floor {
  id: string;
  type: string;
  usableArea: number;
  totalArea: number;
  units: Unit[];
}

export interface Building {
  id: string;
  name: string;
  usableArea: number; // sum of floors
  totalArea: number; // sum of floors
  floors: Floor[];
}

export interface ConstructionSite {
  id: string;
  name: string;
  address: string;
  buildings: Building[];
  createdAt: string; // ISO timestamp
  updatedAt: string; // ISO timestamp
}

// Create/Update request shapes (frontend convenience)
export interface CreateConstructionSiteRequest {
  name: string;
  address: string;
}
export interface UpdateConstructionSiteRequest {
  name?: string;
  address?: string;
}

export interface CreateBuildingRequest {
  name: string;
}
export interface UpdateBuildingRequest {
  name?: string;
}

export interface CreateFloorRequest {
  type: string;
  usableArea: number;
  totalArea: number;
}
export interface UpdateFloorRequest {
  type?: string;
  usableArea?: number;
  totalArea?: number;
}

export interface CreateUnitRequest {
  label: string;
  usableArea: number;
  bedrooms: number;
  bathrooms: number;
  balconies: number;
  toilets: number;
}
export interface UpdateUnitRequest {
  label?: string;
  usableArea?: number;
  bedrooms?: number;
  bathrooms?: number;
  balconies?: number;
  toilets?: number;
}
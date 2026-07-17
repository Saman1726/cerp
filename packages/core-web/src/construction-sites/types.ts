export interface SimpleActionState {
  error?: string;
}

export type SimpleFormAction = (formData: FormData) => Promise<void>;

export interface ConstructionSitesActions {
  createSite: SimpleFormAction;
  deleteSite: SimpleFormAction;
  duplicateSite: SimpleFormAction;
  updateSite: SimpleFormAction;

  addBuilding: SimpleFormAction;
  deleteBuilding: SimpleFormAction;
  duplicateBuilding: SimpleFormAction;
  updateBuilding: SimpleFormAction;

  addFloor: SimpleFormAction;
  deleteFloor: SimpleFormAction;
  duplicateFloor: SimpleFormAction;
  updateFloor: SimpleFormAction;

  addUnit: SimpleFormAction;
  deleteUnit: SimpleFormAction;
  duplicateUnit: SimpleFormAction;
  updateUnit: SimpleFormAction;
}

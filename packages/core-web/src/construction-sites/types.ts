export interface SimpleActionState {
  error?: string;
}

export type SimpleFormAction = (formData: FormData) => Promise<void>;

export interface ConstructionSitesActions {
  createSite: SimpleFormAction;
  deleteSite: SimpleFormAction;

  addBuilding: SimpleFormAction;
  deleteBuilding: SimpleFormAction;

  addFloor: SimpleFormAction;
  deleteFloor: SimpleFormAction;

  addUnit: SimpleFormAction;
  deleteUnit: SimpleFormAction;
}

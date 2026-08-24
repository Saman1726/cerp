export interface SimpleActionState {
  error?: string;
}

export type SimpleFormAction = (formData: FormData) => Promise<void>;

export interface IfcFileActions {
  triggerPreview: (siteId: string, fileId: string) => Promise<void>;
  triggerBlenderRender: (siteId: string, fileId: string, view?: string) => Promise<void>;
  listFiles: (siteId: string) => Promise<IFCFile[]>;
  uploadFile: SimpleFormAction;
  deleteFile: SimpleFormAction;
}

export interface ConstructionSitesActions extends IfcFileActions {
  createSite: SimpleFormAction;
  deleteSite: SimpleFormAction;
  duplicateSite: SimpleFormAction;
  updateSite: SimpleFormAction;
  importIfc: SimpleFormAction;

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

export interface IFCFile {
  id: string;
  fileName: string;
  fileSize?: number;
  fileKey?: string;
  ifcFileId?: string;
  status: 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  previewUrl?: string;
  metadataUrl?: string;
  exportUrl?: string;
  blenderRenderUrl?: string;
  blenderRenderView?: string;
  createdAt: string;
}

export interface IFCFileCreateRequest {
  objectKey: string;
  fileName: string;
  contentLength?: number;
  fileKey?: string;
  ifcFileId?: string;
}

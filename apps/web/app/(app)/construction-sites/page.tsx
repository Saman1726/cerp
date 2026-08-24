import { ConstructionSitesPageBody } from '@cerp/core-web/construction-sites';
import {
  createSiteAction,
  deleteSiteAction,
  addBuildingAction,
  deleteBuildingAction,
  addFloorAction,
  deleteFloorAction,
  addUnitAction,
  deleteUnitAction,
  updateSiteAction,
  duplicateSiteAction,
  importIfcAction,
  updateBuildingAction,
  duplicateBuildingAction,
  updateFloorAction,
  duplicateFloorAction,
  updateUnitAction,
  duplicateUnitAction,
  uploadIfcFileAction,
  deleteIfcFileAction,
  triggerPreviewAction,
  triggerBlenderRenderAction,
  listIfcFilesAction,
} from './actions';

export default function ConstructionSitesPage() {
  const siteActions = {
    createSite: createSiteAction,
    deleteSite: deleteSiteAction,
    duplicateSite: duplicateSiteAction,
    updateSite: updateSiteAction,
    importIfc: importIfcAction,
    addBuilding: addBuildingAction,
    deleteBuilding: deleteBuildingAction,
    duplicateBuilding: duplicateBuildingAction,
    updateBuilding: updateBuildingAction,
    addFloor: addFloorAction,
    deleteFloor: deleteFloorAction,
    duplicateFloor: duplicateFloorAction,
    updateFloor: updateFloorAction,
    addUnit: addUnitAction,
    deleteUnit: deleteUnitAction,
    duplicateUnit: duplicateUnitAction,
    updateUnit: updateUnitAction,
    uploadFile: uploadIfcFileAction,
    deleteFile: deleteIfcFileAction,
    triggerPreview: triggerPreviewAction,
    triggerBlenderRender: triggerBlenderRenderAction,
    listFiles: listIfcFilesAction,
  };

  return <ConstructionSitesPageBody actions={siteActions} />;
}

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
  updateBuildingAction,
  duplicateBuildingAction,
  updateFloorAction,
  duplicateFloorAction,
  updateUnitAction,
  duplicateUnitAction,
} from './actions';

export default function ConstructionSitesPage() {
  const siteActions = {
    createSite: createSiteAction,
    deleteSite: deleteSiteAction,
    duplicateSite: duplicateSiteAction,
    updateSite: updateSiteAction,
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
  };

  return <ConstructionSitesPageBody actions={siteActions} />;
}

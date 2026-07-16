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
} from './actions';

export default function ConstructionSitesPage() {
  const siteActions = {
    createSite: createSiteAction,
    deleteSite: deleteSiteAction,
    addBuilding: addBuildingAction,
    deleteBuilding: deleteBuildingAction,
    addFloor: addFloorAction,
    deleteFloor: deleteFloorAction,
    addUnit: addUnitAction,
    deleteUnit: deleteUnitAction,
  };

  return <ConstructionSitesPageBody actions={siteActions} />;
}
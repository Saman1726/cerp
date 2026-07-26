import { ConstructionEngineeringPageBodyClient } from '@cerp/core-web/construction-engineering';
import {
  issueWorkOrderAction,
  createProjectActionWrapper as createProjectAction,
  deleteProjectActionWrapper as deleteProjectAction,
  createSubProjectActionWrapper as createSubProjectAction,
  deleteSubProjectActionWrapper as deleteSubProjectAction,
  assignUsersToProjectAction,
  removeUserFromProjectAction,
} from './actions';
import { apiClient } from '@cerp/core-web/lib/api-client';
import type { Project, WorkOrder, ConstructionSite } from '@cerp/shared-types';
import type { Partner } from '@cerp/core-web/partners';
import { SESSION_COOKIE } from '@cerp/core-web/lib/session-cookies';

// Get the current user's info from the JWT token for project filtering
// Returns { userId, roles } where userId is the JWT sub field
async function getCurrentUserInfo(): Promise<{ userId: string; roles: string[] } | null> {
  try {
    const { cookies } = await import('next/headers');
    const store = await cookies();
    const token = store.get(SESSION_COOKIE)?.value;
    if (!token) return null;

    const parts = token.split('.');
    if (parts.length < 2) return null;

    const payload = Buffer.from(parts[1] ?? '', 'base64').toString('utf-8');
    const parsed = JSON.parse(payload);
    const userId = parsed.sub || null;
    // Handle both old format (role singular) and new format (roles plural, comma-separated)
    let roles: string[] = [];
    if (parsed.roles) {
      roles = parsed.roles.split(',');
    } else if (parsed.role) {
      roles = [parsed.role];
    }
    console.log('[construction page] JWT sub:', userId, 'roles:', roles);
    return userId ? { userId, roles } : null;
  } catch (err) {
    console.error('[construction page] Failed to parse JWT:', err);
    return null;
  }
}

// Check if the user has admin or manager roles
function isUserAdminOrManager(roles: string[]): boolean {
  return roles.some(role => ['admin', 'manager'].includes(role.toLowerCase()));
}

export default async function ConstructionPage() {
  const [workOrders, constructionSites]: [WorkOrder[], ConstructionSite[]] = await Promise.all([
    apiClient.get<WorkOrder[]>('/work-orders'),
    apiClient.get<ConstructionSite[]>('/construction-sites'),
  ]);

  const contractors: Partner[] = [];

  // Get current user's info for project filtering
  const userInfo = await getCurrentUserInfo();
  const userEmail = userInfo?.userId || null;
  const isAdminOrManager = userInfo ? isUserAdminOrManager(userInfo.roles) : false;

  let projects: Project[] = [];
  try {
    // Admin/manager users see ALL projects; vendor users only see assigned ones
    const query = (userEmail && !isAdminOrManager) ? `?userId=${encodeURIComponent(userEmail)}` : '';
    const rawProjects = await apiClient.get<Record<string, unknown>[]>(`/api/v1/projects${query}`);
    projects = (rawProjects || []).map((p) => ({
      ...p,
      type: (p.projectType as string) || (p.type as string),
      subProjects: (p.subProjects as unknown[]) || [],
      assignedUserIds: (p.assignedUserIds as string[]) || [],
    })) as unknown as Project[];
  } catch {
    console.warn('Failed to fetch projects, returning empty array');
  }

  return (
    <ConstructionEngineeringPageBodyClient
      projects={projects}
      workOrders={workOrders}
      contractors={contractors}
      constructionSites={constructionSites}
      formAction={issueWorkOrderAction}
      createProjectAction={createProjectAction}
      createSubProjectAction={createSubProjectAction}
      deleteProjectAction={deleteProjectAction}
      deleteSubProjectAction={deleteSubProjectAction}
      currentUserEmail={userEmail || undefined}
      assignUsersAction={assignUsersToProjectAction}
      removeUserAction={removeUserFromProjectAction}
    />
  );
}

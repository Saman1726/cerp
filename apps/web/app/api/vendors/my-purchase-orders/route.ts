import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const SESSION_COOKIE = 'cerp_session';
const WORKFLOW_SERVICE_URL = (process.env.NEXT_PUBLIC_WORKFLOW_SERVICE_URL || process.env.WORKFLOW_SERVICE_URL || 'http://workflow-service-java:8089').replace(/\/$/, '');

/**
 * GET /api/vendors/my-purchase-orders
 *
 * Returns procurement sagas (purchase orders) assigned to the current vendor.
 * The vendor ID is resolved by first fetching the vendor profile to get the vendor's
 * vendorId, then querying the workflow service for sagas where data.vendorId matches.
 */
export async function GET(request: NextRequest) {
  const headers = new Headers();
  headers.set('Content-Type', 'application/json');

  let token: string | null = null;

  // Extract token from cookie
  const cookieHeader = request.headers.get('cookie');
  if (cookieHeader) {
    const cookiesArr = cookieHeader.split(';').map(c => c.trim());
    for (const cookie of cookiesArr) {
      const [key, ...valueParts] = cookie.split('=');
      if (key === SESSION_COOKIE && valueParts.length > 0) {
        token = valueParts.join('=');
        try {
          token = decodeURIComponent(token);
        } catch {
          // Token might not be URL-encoded, use as-is
        }
        break;
      }
    }
  }

  if (!token) {
    try {
      const cookieStore = await cookies();
      token = cookieStore.get(SESSION_COOKIE)?.value ?? null;
    } catch {
      // skip
    }
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  try {
    // Step 1: Get the vendor profile to find vendorId
    const vendorServiceUrl = (process.env.NEXT_PUBLIC_VENDOR_SERVICE_URL || process.env.VENDOR_SERVICE_URL || 'http://vendor-service-java:8085').replace(/\/$/, '');
    const vendorProfileRes = await fetch(`${vendorServiceUrl}/vendors/profile`, { headers });

    if (!vendorProfileRes.ok) {
      return NextResponse.json(
        { error: 'Failed to fetch vendor profile', details: await vendorProfileRes.text() },
        { status: vendorProfileRes.status }
      );
    }

    const vendorProfile = await vendorProfileRes.json();
    const vendorId = vendorProfile.vendorId || vendorProfile.id;

    if (!vendorId) {
      return NextResponse.json({ items: [], total: 0, page: 1, pageSize: 25 });
    }

    // Step 2: Query workflow service for PROCUREMENT_WORKFLOW sagas
    const workflowUrl = `${WORKFLOW_SERVICE_URL}/api/v1/sagas?sagaType=PROCUREMENT_WORKFLOW&pageSize=200`;
    const sagasRes = await fetch(workflowUrl, { headers });

    if (!sagasRes.ok) {
      return NextResponse.json(
        { error: 'Failed to fetch sagas', details: await sagasRes.text() },
        { status: sagasRes.status }
      );
    }

    const sagasData = await sagasRes.json();
    const allSagas: Record<string, unknown>[] = Array.isArray(sagasData?.items)
      ? sagasData.items
      : [];

    // Step 3: Filter sagas where data.vendorId matches the current vendor
    const assignedSagas = allSagas.filter((saga) => {
      const data = (saga.data ?? {}) as Record<string, unknown>;
      return data.vendorId === vendorId;
    });

    // Map to simplified PO summary format
    const items = assignedSagas.map((saga) => {
      const data = (saga.data ?? {}) as Record<string, unknown>;
      const steps = (saga.steps as unknown[] | undefined) || [];
      return {
        sagaId: String(saga.sagaId ?? ''),
        status: String(saga.status ?? ''),
        currentStep: String(saga.currentStep ?? ''),
        vendorId: String(data.vendorId ?? ''),
        projectId: String(data.projectId ?? ''),
        materialId: String(data.materialId ?? ''),
        quantity: Number(data.quantity ?? 0),
        description: String(data.description ?? ''),
        createdAt: String(saga.createdAt ?? ''),
        updatedAt: String(saga.updatedAt ?? ''),
        stepCount: steps.length,
      };
    });

    return NextResponse.json({
      items,
      total: items.length,
      page: 1,
      pageSize: 25,
    });
  } catch (error) {
    console.error('Failed to proxy my-purchase-orders request:', error);
    return NextResponse.json(
      { error: 'Failed to connect to services' },
      { status: 503 }
    );
  }
}
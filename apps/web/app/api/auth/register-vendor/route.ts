import { NextRequest, NextResponse } from 'next/server';
import { IDENTITY_SERVICE_URL, VENDOR_SERVICE_URL } from '@cerp/core-web/lib/env';

// Every registration lands on this shared default tenant (seeded by identity-service's
// V6 migration). Users only get a tenant of their own if an Admin later assigns them a
// private one - that's a separate, not-yet-built admin action, not part of registration.
const DEFAULT_TENANT_ID = '00000000-0000-0000-0000-000000000001';

interface VendorRegistrationRequest {
  username: string;
  email: string;
  password: string;
  vendorData: {
    legalName: string;
    contactName?: string;
    phone?: string;
    website?: string;
    categories?: string[];
    address?: string;
    city?: string;
    country?: string;
  };
}

interface CreateUserResponse {
  id: string;
  email: string;
  name: string;
  status: string;
  createdAt: string;
  updatedAt: string | null;
}

interface CreateMembershipResponse {
  id: string;
  tenantId: string;
  userId: string;
  roles: string[];
  scopes: Record<string, unknown>;
  createdAt: string;
  updatedAt: string | null;
}

interface CreateVendorResponse {
  id: string;
  tenantId: string;
  legalName: string;
  status: string;
  categories: string[];
  contactName: string | null;
  phone: string | null;
  website: string | null;
  address: string | null;
  city: string | null;
  country: string | null;
  createdAt: string;
  updatedAt: string | null;
}

export async function POST(request: NextRequest): Promise<NextResponse> {
  try {
    const body: VendorRegistrationRequest = await request.json();

    const { username, email, password, vendorData } = body;

    // Validate required fields
    if (!username || !email || !password) {
      return NextResponse.json(
        { message: 'Username, email, and password are required' },
        { status: 400 }
      );
    }

    if (!vendorData?.legalName) {
      return NextResponse.json(
        { message: 'Company legal name is required' },
        { status: 400 }
      );
    }

    // Step 1: Check if user with this email already exists
    const existingUsersRes = await fetch(`${IDENTITY_SERVICE_URL}/users`, {
      method: 'GET',
    });

    if (existingUsersRes.ok) {
      const existingUsers: CreateUserResponse[] = await existingUsersRes.json();
      const existingUser = existingUsers.find((u: CreateUserResponse) => u.email === email);

      if (existingUser) {
        return NextResponse.json(
          {
            message: 'A vendor account with this email already exists. Please use a different email or contact support.',
            code: 'USER_ALREADY_EXISTS'
          },
          { status: 409 }
        );
      }
    }

    // Step 2: Create the user account with password
    const userPayload = {
      email,
      name: vendorData.contactName || vendorData.legalName,
      password,
      type: 'VENDOR',
    };

    const userRes = await fetch(`${IDENTITY_SERVICE_URL}/users`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(userPayload),
    });

    if (!userRes.ok) {
      const errorData = await userRes.json().catch(() => ({}));
      return NextResponse.json(
        { message: `Failed to create user: ${errorData.message || userRes.statusText}` },
        { status: userRes.status }
      );
    }

    const user: CreateUserResponse = await userRes.json();

    try {
      // Step 3: Create a membership on the default tenant with vendor role
      const membershipPayload = {
        tenantId: DEFAULT_TENANT_ID,
        userId: user.id,
        roles: ['vendor'],
        scopes: {
          vendorId: null, // Will be updated after vendor creation
          selfService: true,
        },
      };

      const membershipRes = await fetch(`${IDENTITY_SERVICE_URL}/memberships`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(membershipPayload),
      });

      if (!membershipRes.ok) {
        const errorData = await membershipRes.json().catch(() => ({}));
        await fetch(`${IDENTITY_SERVICE_URL}/users/${user.id}`, {
          method: 'DELETE',
        }).catch(() => null);
        return NextResponse.json(
          { message: `Failed to create membership: ${errorData.message || membershipRes.statusText}` },
          { status: membershipRes.status }
        );
      }

      // Step 4: Create the vendor record
      const vendorPayload = {
        legalName: vendorData.legalName,
        contactName: vendorData.contactName,
        phone: vendorData.phone,
        website: vendorData.website,
        categories: vendorData.categories || [],
        address: vendorData.address,
        city: vendorData.city,
        country: vendorData.country,
        status: 'pending',
        userId: user.id,
        tenantId: DEFAULT_TENANT_ID,
      };

      const vendorRes = await fetch(`${VENDOR_SERVICE_URL}/vendors`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(vendorPayload),
      });

      if (!vendorRes.ok) {
        const errorData = await vendorRes.json().catch(() => ({}));
        await fetch(`${IDENTITY_SERVICE_URL}/users/${user.id}`, {
          method: 'DELETE',
        }).catch(() => null);
        return NextResponse.json(
          { message: `Failed to create vendor record: ${errorData.message || vendorRes.statusText}` },
          { status: vendorRes.status }
        );
      }

      const vendor: CreateVendorResponse = await vendorRes.json();

      // Step 5: Update the membership scopes with the vendor ID
      const membershipsRes = await fetch(`${IDENTITY_SERVICE_URL}/memberships`, {
        headers: { 'Content-Type': 'application/json' },
      });

      let membershipId: { id: string; scopes: Record<string, unknown> } | null = null;
      if (membershipsRes.ok) {
        const memberships: CreateMembershipResponse[] = await membershipsRes.json();
        const found = memberships.find((m: CreateMembershipResponse) => m.tenantId === DEFAULT_TENANT_ID && m.userId === user.id);
        if (found?.id) {
          membershipId = { id: found.id, scopes: found.scopes || {} };
        }
      }

      if (membershipId) {
        await fetch(`${IDENTITY_SERVICE_URL}/memberships/${membershipId.id}`, {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            scopes: {
              ...membershipId.scopes,
              vendorId: vendor.id,
            },
          }),
        }).catch(() => null);
      }

      // Step 6: Create a Freemium subscription (every account gets one, regardless of type)
      const subscriptionPayload = { userId: user.id, plan: 'FREEMIUM' };

      const subscriptionRes = await fetch(`${IDENTITY_SERVICE_URL}/subscriptions`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(subscriptionPayload),
      });

      if (!subscriptionRes.ok) {
        const errorData = await subscriptionRes.json().catch(() => ({}));
        await fetch(`${IDENTITY_SERVICE_URL}/users/${user.id}`, {
          method: 'DELETE',
        }).catch(() => null);
        return NextResponse.json(
          { message: `Failed to create subscription: ${errorData.message || subscriptionRes.statusText}` },
          { status: subscriptionRes.status }
        );
      }

      return NextResponse.json(
        {
          message: 'Vendor registration submitted successfully',
          data: {
            userId: user.id,
            tenantId: DEFAULT_TENANT_ID,
            vendorId: vendor.id,
          },
        },
        { status: 201 }
      );
    } catch (error) {
      // Clean up if membership/vendor steps fail after user creation
      await fetch(`${IDENTITY_SERVICE_URL}/users/${user.id}`, {
        method: 'DELETE',
      }).catch(() => null);
      throw error;
    }
  } catch (error) {
    console.error('Vendor registration error:', error);
    return NextResponse.json(
      { message: 'Internal server error during vendor registration' },
      { status: 500 }
    );
  }
}

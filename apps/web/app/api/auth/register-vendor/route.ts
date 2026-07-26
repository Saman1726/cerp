import { NextRequest, NextResponse } from 'next/server';

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

interface CreateTenantResponse {
  id: string;
  name: string;
  region: string | null;
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

    // Step 1: Create a default tenant for the vendor
    const tenantPayload = {
      name: vendorData.legalName,
      region: vendorData.country || '',
    };

    const identityUrl = process.env.IDENTITY_SERVICE_URL || process.env.API_URL;
    const vendorUrl = process.env.VENDOR_SERVICE_URL || process.env.API_URL;

    const tenantRes = await fetch(`${identityUrl}/tenants`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(tenantPayload),
    });

    if (!tenantRes.ok) {
      const errorData = await tenantRes.json().catch(() => ({}));
      return NextResponse.json(
        { message: `Failed to create tenant: ${errorData.message || tenantRes.statusText}` },
        { status: tenantRes.status }
      );
    }

    const tenant: CreateTenantResponse = await tenantRes.json();

    // Step 2: Create the user account with password
    const userPayload = {
      email,
      name: vendorData.contactName || vendorData.legalName,
      password,
    };

    const userRes = await fetch(`${identityUrl}/users`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(userPayload),
    });

    if (!userRes.ok) {
      const errorData = await userRes.json().catch(() => ({}));
      // Clean up tenant if user creation fails
      await fetch(`${identityUrl}/tenants/${tenant.id}`, {
        method: 'DELETE',
      }).catch(() => null);
      return NextResponse.json(
        { message: `Failed to create user: ${errorData.message || userRes.statusText}` },
        { status: userRes.status }
      );
    }

    const user: CreateUserResponse = await userRes.json();

    // Step 3: Create a membership with vendor role
    const membershipPayload = {
      tenantId: tenant.id,
      userId: user.id,
      roles: ['vendor'],
      scopes: {
        vendorId: null, // Will be updated after vendor creation
        selfService: true,
      },
    };

    const membershipRes = await fetch(`${identityUrl}/memberships`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(membershipPayload),
    });

    if (!membershipRes.ok) {
      const errorData = await membershipRes.json().catch(() => ({}));
      // Clean up user and tenant if membership creation fails
      await fetch(`${identityUrl}/users/${user.id}`, {
        method: 'DELETE',
      }).catch(() => null);
      await fetch(`${identityUrl}/tenants/${tenant.id}`, {
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
      tenantId: tenant.id,
    };

    const vendorRes = await fetch(`${vendorUrl}/vendors`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(vendorPayload),
    });

    if (!vendorRes.ok) {
      const errorData = await vendorRes.json().catch(() => ({}));
      // Clean up all created resources if vendor creation fails
      await fetch(`${identityUrl}/memberships`, {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tenantId: tenant.id, userId: user.id }),
      }).catch(() => null);
      await fetch(`${identityUrl}/users/${user.id}`, {
        method: 'DELETE',
      }).catch(() => null);
      await fetch(`${identityUrl}/tenants/${tenant.id}`, {
        method: 'DELETE',
      }).catch(() => null);
      return NextResponse.json(
        { message: `Failed to create vendor record: ${errorData.message || vendorRes.statusText}` },
        { status: vendorRes.status }
      );
    }

    const vendor: CreateVendorResponse = await vendorRes.json();

    // Step 5: Update the membership scopes with the vendor ID
    const membershipId = await fetch(`${identityUrl}/memberships`, {
      headers: { 'Content-Type': 'application/json' },
    })
      .then((res) => res.json())
      .then((memberships: CreateMembershipResponse[]) =>
        memberships.find((m: CreateMembershipResponse) => m.tenantId === tenant.id && m.userId === user.id)
      )
      .catch(() => null);

    if (membershipId?.id) {
      await fetch(`${identityUrl}/memberships/${membershipId.id}`, {
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

    return NextResponse.json(
      {
        message: 'Vendor registration submitted successfully',
        data: {
          userId: user.id,
          tenantId: tenant.id,
          vendorId: vendor.id,
        },
      },
      { status: 201 }
    );
  } catch (error) {
    console.error('Vendor registration error:', error);
    return NextResponse.json(
      { message: 'Internal server error during vendor registration' },
      { status: 500 }
    );
  }
}
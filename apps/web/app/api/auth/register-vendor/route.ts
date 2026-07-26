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

    // Service URLs - use direct service URLs for correct path format
    const identityServiceUrl = process.env.IDENTITY_SERVICE_URL || 'http://localhost:8090';
    const vendorServiceUrl = process.env.VENDOR_SERVICE_URL || 'http://localhost:8085';

    // Step 1: Check if user with this email already exists
    const existingUsersRes = await fetch(`${identityServiceUrl}/users`, {
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

    // Step 2: Create a default tenant for the vendor
    const tenantPayload = {
      name: vendorData.legalName,
      region: vendorData.country || '',
    };

    const tenantRes = await fetch(`${identityServiceUrl}/tenants`, {
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

    try {
      // Step 3: Create the user account with password
      const userPayload = {
        email,
        name: vendorData.contactName || vendorData.legalName,
        password,
      };

      const userRes = await fetch(`${identityServiceUrl}/users`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(userPayload),
      });

      if (!userRes.ok) {
        const errorData = await userRes.json().catch(() => ({}));
        // Clean up tenant if user creation fails
        await fetch(`${identityServiceUrl}/tenants/${tenant.id}`, {
          method: 'DELETE',
        }).catch(() => null);
        return NextResponse.json(
          { message: `Failed to create user: ${errorData.message || userRes.statusText}` },
          { status: userRes.status }
        );
      }

      const user: CreateUserResponse = await userRes.json();

      try {
        // Step 4: Create a membership with vendor role
        const membershipPayload = {
          tenantId: tenant.id,
          userId: user.id,
          roles: ['vendor'],
          scopes: {
            vendorId: null, // Will be updated after vendor creation
            selfService: true,
          },
        };

        const membershipRes = await fetch(`${identityServiceUrl}/memberships`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(membershipPayload),
        });

        if (!membershipRes.ok) {
          const errorData = await membershipRes.json().catch(() => ({}));
          // Clean up user and tenant if membership creation fails
          await fetch(`${identityServiceUrl}/users/${user.id}`, {
            method: 'DELETE',
          }).catch(() => null);
          await fetch(`${identityServiceUrl}/tenants/${tenant.id}`, {
            method: 'DELETE',
          }).catch(() => null);
          return NextResponse.json(
            { message: `Failed to create membership: ${errorData.message || membershipRes.statusText}` },
            { status: membershipRes.status }
          );
        }

        // Step 5: Create the vendor record
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

        const vendorRes = await fetch(`${vendorServiceUrl}/vendors`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(vendorPayload),
        });

        if (!vendorRes.ok) {
          const errorData = await vendorRes.json().catch(() => ({}));
          // Clean up all created resources if vendor creation fails
          await fetch(`${identityServiceUrl}/users/${user.id}`, {
            method: 'DELETE',
          }).catch(() => null);
          await fetch(`${identityServiceUrl}/tenants/${tenant.id}`, {
            method: 'DELETE',
          }).catch(() => null);
          return NextResponse.json(
            { message: `Failed to create vendor record: ${errorData.message || vendorRes.statusText}` },
            { status: vendorRes.status }
          );
        }

        const vendor: CreateVendorResponse = await vendorRes.json();

        // Step 6: Update the membership scopes with the vendor ID
        const membershipsRes = await fetch(`${identityServiceUrl}/memberships`, {
          headers: { 'Content-Type': 'application/json' },
        });

        let membershipId: { id: string; scopes: Record<string, unknown> } | null = null;
        if (membershipsRes.ok) {
          const memberships: CreateMembershipResponse[] = await membershipsRes.json();
          const found = memberships.find((m: CreateMembershipResponse) => m.tenantId === tenant.id && m.userId === user.id);
          if (found?.id) {
            membershipId = { id: found.id, scopes: found.scopes || {} };
          }
        }

        if (membershipId) {
          await fetch(`${identityServiceUrl}/memberships/${membershipId.id}`, {
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
        // Clean up if membership/vendor steps fail after user/tenant creation
        await fetch(`${identityServiceUrl}/users/${user.id}`, {
          method: 'DELETE',
        }).catch(() => null);
        await fetch(`${identityServiceUrl}/tenants/${tenant.id}`, {
          method: 'DELETE',
        }).catch(() => null);
        throw error;
      }
    } catch (error) {
      if (error instanceof Error && error.message.includes('Failed to create')) {
        throw error; // Already handled
      }
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
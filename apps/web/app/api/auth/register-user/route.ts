import { NextRequest, NextResponse } from 'next/server';

interface UserRegistrationRequest {
  username: string;
  email: string;
  password: string;
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


export async function POST(request: NextRequest): Promise<NextResponse> {
  try {
    const body: UserRegistrationRequest = await request.json();

    const { username, email, password } = body;

    // Validate required fields
    if (!username || !email || !password) {
      return NextResponse.json(
        { message: 'Username, email, and password are required' },
        { status: 400 }
      );
    }

    // Validate password length
    if (password.length < 6) {
      return NextResponse.json(
        { message: 'Password must be at least 6 characters long' },
        { status: 400 }
      );
    }

    const identityServiceUrl = process.env.IDENTITY_SERVICE_URL || 'http://localhost:8090';

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
            message: 'A user account with this email already exists. Please use a different email or contact support.',
            code: 'USER_ALREADY_EXISTS'
          },
          { status: 409 }
        );
      }
    }

    // Step 2: Create a default tenant for the user
    const tenantPayload = {
      name: `${username}'s Workspace`,
      region: '',
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
        name: username,
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
        // Step 4: Create a membership with user role
        const membershipPayload = {
          tenantId: tenant.id,
          userId: user.id,
          roles: ['user'],
          scopes: {
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

        return NextResponse.json(
          {
            message: 'User registration successful',
            data: {
              userId: user.id,
              tenantId: tenant.id,
            },
          },
          { status: 201 }
        );
      } catch (error) {
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
        throw error;
      }
      throw error;
    }
  } catch (error) {
    console.error('User registration error:', error);
    return NextResponse.json(
      { message: 'Internal server error during user registration' },
      { status: 500 }
    );
  }
}
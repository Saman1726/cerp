import { NextRequest, NextResponse } from 'next/server';
import { IDENTITY_SERVICE_URL } from '@cerp/core-web/lib/env';

// Every registration lands on this shared default tenant (seeded by identity-service's
// V6 migration). Users only get a tenant of their own if an Admin later assigns them a
// private one - that's a separate, not-yet-built admin action, not part of registration.
const DEFAULT_TENANT_ID = '00000000-0000-0000-0000-000000000001';

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
            message: 'A user account with this email already exists. Please use a different email or contact support.',
            code: 'USER_ALREADY_EXISTS'
          },
          { status: 409 }
        );
      }
    }

    // Step 2: Create the user account with password
    const userPayload = {
      email,
      name: username,
      password,
      type: 'USER',
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
      // Step 3: Create a membership on the default tenant with user role
      const membershipPayload = {
        tenantId: DEFAULT_TENANT_ID,
        userId: user.id,
        roles: ['user'],
        scopes: {
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

      // Step 4: Create a Freemium subscription (every account gets one, regardless of type)
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
          message: 'User registration successful',
          data: {
            userId: user.id,
            tenantId: DEFAULT_TENANT_ID,
          },
        },
        { status: 201 }
      );
    } catch (error) {
      await fetch(`${IDENTITY_SERVICE_URL}/users/${user.id}`, {
        method: 'DELETE',
      }).catch(() => null);
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

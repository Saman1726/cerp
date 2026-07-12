import { NextResponse } from 'next/server';
import { clearSession } from '@cerp/core-web/lib/session';

export async function POST(request: Request): Promise<NextResponse> {
  await clearSession();
  return NextResponse.redirect(new URL('/login', request.url));
}

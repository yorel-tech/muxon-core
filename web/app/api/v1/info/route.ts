import { NextRequest, NextResponse } from 'next/server';

export async function GET(request: NextRequest) {
  const apiBase = process.env.NEXT_PUBLIC_API_BASE || 'http://localhost:8080';

  const authHeader = request.headers.get('authorization');
  const cookieHeader = request.headers.get('cookie');
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  };
  if (authHeader) {
    headers['Authorization'] = authHeader;
  }
  if (cookieHeader) {
    headers['Cookie'] = cookieHeader;
  }

  try {
    const response = await fetch(`${apiBase}/api/v1/info`, {
      method: 'GET',
      headers,
    });

    if (!response.ok) {
      return NextResponse.json(
        { error: 'Failed to fetch product info' },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Error proxying /api/v1/info to backend:', error);
    return NextResponse.json(
      { error: 'Failed to fetch product info' },
      { status: 500 }
    );
  }
}

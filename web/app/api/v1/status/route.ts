import { NextRequest, NextResponse } from 'next/server';

export async function GET(request: NextRequest) {
  const apiBase = process.env.NEXT_PUBLIC_API_BASE || 'http://localhost:8080';
  
  // Forward Authorization header and cookies from the browser request
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
    const response = await fetch(`${apiBase}/api/v1/status`, {
      method: 'GET',
      headers,
    });

    if (!response.ok) {
      return NextResponse.json(
        { systemStatus: 'NOTREADY' },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Error proxying to backend:', error);
    return NextResponse.json(
      { systemStatus: 'NOTREADY' },
      { status: 500 }
    );
  }
}

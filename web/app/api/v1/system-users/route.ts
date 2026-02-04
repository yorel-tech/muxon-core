import { NextRequest, NextResponse } from 'next/server';

export async function GET(request: NextRequest) {
  const apiBase = process.env.NEXT_PUBLIC_API_BASE || 'http://localhost:8080';
  
  // Get all cookies from request
  const cookieHeader = request.headers.get('cookie');
  const authHeader = request.headers.get('authorization');
  
  // Debug logging - show all cookies
  console.log('[System Users Route] Incoming headers:', {
    authHeader: authHeader ? 'present' : 'missing',
    cookieHeader: cookieHeader ? 'present' : 'missing',
    allCookies: cookieHeader,
  });
  
  // Parse cookies to see what we have
  const cookies: Record<string, string> = {};
  if (cookieHeader) {
    cookieHeader.split(';').forEach(cookie => {
      const [name, value] = cookie.trim().split('=');
      if (name && value) {
        cookies[name.trim()] = value.trim();
      }
    });
  }
  console.log('[System Users Route] Parsed cookies:', cookies);
  
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  };
  if (authHeader) {
    headers['Authorization'] = authHeader;
  }
  if (cookieHeader) {
    headers['Cookie'] = cookieHeader;
  }
  
  console.log('[System Users Route] Forwarding headers:', {
    hasAuth: !!headers['Authorization'],
    hasCookie: !!headers['Cookie'],
  });
  
  try {
    const response = await fetch(`${apiBase}/api/v1/system-users`, {
      method: 'GET',
      headers,
    });
    
    console.log('[System Users Route] Backend response status:', response.status);
    
    if (!response.ok) {
      // If unauthorized, return empty array instead of error
      // This allows wizard to open even if user is not authenticated
      if (response.status === 401) {
        console.log('[System Users Route] User not authenticated, returning empty array');
        return NextResponse.json([]);
      }
      return NextResponse.json(
        [],
        { status: response.status }
      );
    }
    
    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Error proxying to backend:', error);
    // On error, return empty array to allow wizard to open
    return NextResponse.json([]);
  }
}

export async function POST(request: NextRequest) {
  const apiBase = process.env.NEXT_PUBLIC_API_BASE || 'http://localhost:8080';
  
  // Get all cookies from request
  const cookieHeader = request.headers.get('cookie');
  const authHeader = request.headers.get('authorization');
  
  console.log('[System Users Route] POST Incoming headers:', {
    authHeader: authHeader ? 'present' : 'missing',
    cookieHeader: cookieHeader ? 'present' : 'missing',
  });
  
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
    const body = await request.json();
    console.log('[System Users Route] POST body:', body);
    
    const response = await fetch(`${apiBase}/api/v1/system-users`, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
    });
    
    console.log('[System Users Route] Backend response status:', response.status);
    
    if (!response.ok) {
      const errorText = await response.text();
      console.error('[System Users Route] Backend error:', errorText);
      return NextResponse.json(
        { error: errorText || 'Failed to add system users' },
        { status: response.status }
      );
    }
    
    const data = await response.json();
    return NextResponse.json(data, { status: 201 });
  } catch (error) {
    console.error('[System Users Route] Error proxying POST to backend:', error);
    return NextResponse.json(
      { error: 'Failed to add system users' },
      { status: 500 }
    );
  }
}

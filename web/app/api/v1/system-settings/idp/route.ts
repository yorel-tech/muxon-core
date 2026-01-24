import { NextRequest, NextResponse } from 'next/server';

export async function GET(request: NextRequest) {
  const apiBase = process.env.NEXT_PUBLIC_API_BASE || 'http://localhost:8080';
  
  // Get all cookies from the request
  const cookieHeader = request.headers.get('cookie');
  const authHeader = request.headers.get('authorization');
  
  // Debug logging - show all cookies
  console.log('[IDP Route] Incoming headers:', {
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
  console.log('[IDP Route] Parsed cookies:', cookies);
  
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  };
  if (authHeader) {
    headers['Authorization'] = authHeader;
  }
  if (cookieHeader) {
    headers['Cookie'] = cookieHeader;
  }
  
  console.log('[IDP Route] Forwarding headers:', {
    hasAuth: !!headers['Authorization'],
    hasCookie: !!headers['Cookie'],
  });
  
  try {
    const response = await fetch(`${apiBase}/api/v1/system-settings/idp`, {
      method: 'GET',
      headers,
    });
    
    console.log('[IDP Route] Backend response status:', response.status);
    
    if (!response.ok) {
      // If unauthorized, return empty config instead of error
      // This allows the wizard to open even if user is not authenticated
      if (response.status === 401) {
        console.log('[IDP Route] User not authenticated, returning empty config');
        return NextResponse.json({});
      }
      return NextResponse.json(
        {},
        { status: response.status }
      );
    }
    
    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Error proxying to backend:', error);
    // On error, return empty config to allow wizard to open
    return NextResponse.json({});
  }
}

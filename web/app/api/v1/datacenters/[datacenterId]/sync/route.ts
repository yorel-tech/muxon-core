import { NextRequest, NextResponse } from 'next/server';

const BACKEND_API_BASE = process.env.NEXT_PUBLIC_API_BASE || 'http://localhost:8080';

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ datacenterId: string }> }
) {
  try {
    const { datacenterId } = await params;

    // Build backend URL - Note: Check if backend has a sync endpoint
    // For now, we'll try to call it, but it may not exist in the backend
    const backendUrl = new URL(`${BACKEND_API_BASE}/api/v1/datacenters/${datacenterId}/sync`);

    // Forward the request to the backend API
    const response = await fetch(backendUrl.toString(), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        // Forward authorization header if present
        ...(request.headers.get('authorization') && {
          Authorization: request.headers.get('authorization')!,
        }),
      },
      body: JSON.stringify({}),
    });

    if (!response.ok) {
      const errorText = await response.text();
      return NextResponse.json(
        { error: `Backend API error: ${response.status} ${response.statusText}`, details: errorText },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Error in datacenter sync API route:', error);
    return NextResponse.json(
      { error: 'Internal server error', message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}

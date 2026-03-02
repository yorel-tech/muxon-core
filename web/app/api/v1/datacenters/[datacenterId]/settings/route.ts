import { NextRequest, NextResponse } from 'next/server';

const BACKEND_API_BASE = process.env.NEXT_PUBLIC_API_BASE || 'http://localhost:8080';

async function proxy(
  request: NextRequest,
  params: Promise<{ datacenterId: string }>,
  method: string,
  body?: string
) {
  try {
    const { datacenterId } = await params;
    const url = `${BACKEND_API_BASE}/api/v1/datacenters/${datacenterId}/settings`;
    const response = await fetch(url, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(request.headers.get('authorization') && {
          Authorization: request.headers.get('authorization')!,
        }),
      },
      ...(body !== undefined && { body }),
    });
    if (!response.ok) {
      const errorText = await response.text();
      return NextResponse.json(
        { error: `Backend API error: ${response.status} ${response.statusText}`, details: errorText },
        { status: response.status }
      );
    }
    if (response.status === 204) return new NextResponse(null, { status: 204 });
    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Error in datacenter settings API route:', error);
    return NextResponse.json(
      { error: 'Internal server error', message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ datacenterId: string }> }
) {
  return proxy(request, params, 'GET');
}

export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ datacenterId: string }> }
) {
  const body = await request.text();
  return proxy(request, params, 'PUT', body || undefined);
}

export async function PATCH(
  request: NextRequest,
  { params }: { params: Promise<{ datacenterId: string }> }
) {
  const body = await request.text();
  return proxy(request, params, 'PATCH', body || undefined);
}

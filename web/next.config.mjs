export default {
  reactStrictMode: true,
  async rewrites() {
    const apiBase = process.env.NEXT_PUBLIC_API_BASE || 'http://localhost:8080';
    return [
      {
        source: '/api/v1/:path*',
        destination: `${apiBase}/api/v1/:path*`,
      },
      {
        source: '/swagger-ui',
        destination: `${apiBase}/swagger-ui.html`,
      },
      {
        source: '/swagger-ui/:path*',
        destination: `${apiBase}/swagger-ui/:path*`,
      },
      {
        source: '/v3/api-docs',
        destination: `${apiBase}/v3/api-docs`,
      },
      {
        source: '/v3/api-docs/:path*',
        destination: `${apiBase}/v3/api-docs/:path*`,
      },
    ];
  },
  asyncHeaders: {
    source: '/api/(.*)',
    headers: [
      {
        key: 'Access-Control-Allow-Origin',
        value: 'http://localhost:8080',
      },
      {
        key: 'Access-Control-Allow-Methods',
        value: 'GET, POST, PUT, DELETE, PATCH, OPTIONS',
      },
      {
        key: 'Access-Control-Allow-Headers',
        value: 'Content-Type, Authorization',
      },
    ],
  },
};

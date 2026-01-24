export default {
  reactStrictMode: true,
  asyncHeaders: {
    source: '/api/(.*)',
    headers: [
      {
        key: 'Access-Control-Allow-Origin',
        value: 'http://localhost:4000',
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

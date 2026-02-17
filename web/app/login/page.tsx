export default function Login() {
  return (
    <main className="max-w-full px-3 py-8">
      <h1 className="text-2xl font-semibold">Login</h1>
      <p className="mt-2 text-gray-600">Configure OIDC with Keycloak/your IdP; this page will redirect to your provider.</p>
      <pre className="mt-4 p-4 bg-gray-100 rounded">
        OIDC_ISSUER_URL=https://idp.example.com/realms/infron
        OIDC_CLIENT_ID=infron-web
      </pre>
    </main>
  );
}

import fs from 'fs';
import path from 'path';
import { NextResponse } from 'next/server';
import { loadEnvConfig } from '@next/env';

export const dynamic = 'force-dynamic';

function readEnvLocalFile(): Record<string, string> {
  const p = path.join(process.cwd(), '.env.local');
  if (!fs.existsSync(p)) return {};
  const raw = fs.readFileSync(p, 'utf8');
  const out: Record<string, string> = {};
  for (const line of raw.split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const eq = trimmed.indexOf('=');
    if (eq <= 0) continue;
    const key = trimmed.slice(0, eq).trim();
    let val = trimmed.slice(eq + 1).trim();
    if (
      (val.startsWith('"') && val.endsWith('"')) ||
      (val.startsWith("'") && val.endsWith("'"))
    ) {
      val = val.slice(1, -1);
    }
    out[key] = val;
  }
  return out;
}

/** Dynamic key access so Next does not compile-time-inline empty NEXT_PUBLIC_* in this route. */
function envOrFile(key: string, file: Record<string, string>): string {
  const fromProcess = process.env[key];
  if (typeof fromProcess === 'string' && fromProcess.trim() !== '') return fromProcess;
  const fromFile = file[key];
  return typeof fromFile === 'string' ? fromFile : '';
}

export async function GET() {
  loadEnvConfig(process.cwd());
  const file = readEnvLocalFile();

  const authority = envOrFile('NEXT_PUBLIC_OIDC_AUTHORITY', file).trim();
  const clientId = envOrFile('NEXT_PUBLIC_OIDC_CLIENT_ID', file).trim();
  const redirectUri = envOrFile('NEXT_PUBLIC_OIDC_REDIRECT_URI', file).trim();
  const postLogoutRedirectUri = envOrFile('NEXT_PUBLIC_OIDC_POST_LOGOUT_REDIRECT_URI', file).trim();
  const scope = (
    envOrFile('NEXT_PUBLIC_OIDC_SCOPE', file).trim() || 'openid profile email'
  ).trim();

  return NextResponse.json({
    authority,
    clientId,
    redirectUri,
    postLogoutRedirectUri,
    scope,
  });
}

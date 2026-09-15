const base = (import.meta.env.VITE_API_BASE || 'http://127.0.0.1:8081/api/v1').replace(/\/$/, '');
export async function request<T>(path: string, method = 'GET', data?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  if (method !== 'GET') {
    const csrf = await request<{token:string}>('/auth/csrf');
    headers['X-CSRF-TOKEN'] = csrf.token;
    headers['Content-Type'] = 'application/json';
  }
  const response = await fetch(base + path, {method, headers, credentials:'include', body:data === undefined ? undefined : JSON.stringify(data)});
  if (response.status === 401) { location.href = '/login'; throw new Error('登录已失效，请重新登录'); }
  const result = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(result.message || `请求失败（${response.status}）`);
  return result.data;
}

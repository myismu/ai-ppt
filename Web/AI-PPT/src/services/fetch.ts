import { useUserStore } from '@/store/user'

const request = async (url: string, options: RequestInit): Promise<Response> => {
  // 从 store 中获取 Token，与 axios 拦截器保持一致
  const userStore = useUserStore()
  const authHeaders: Record<string, string> = {}
  if (userStore.token) {
    authHeaders['Authorization'] = `Bearer ${userStore.token}`
  }

  const response = await fetch(import.meta.env.VITE_APP_BASE_API + url, {
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders,
      ...options.headers,
    },
    ...options,
  })

  const contentType = response.headers.get('content-type')
  const isStreamResponse = contentType && (
    contentType.includes('text/event-stream') ||
    contentType.includes('application/octet-stream')
  )

  if (!isStreamResponse) {
    try {
      const jsonResponse = await response.json()
      return jsonResponse
    } 
    catch (err) {
      throw new Error('服务器返回了非流响应')
    }
  }

  return response
}

export default request
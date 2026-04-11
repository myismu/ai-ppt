import axios from 'axios'
import message from '@/utils/message'
import { useUserStore } from '@/store/user'
import router from '@/router'

const instance = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 1000 * 300,
})

instance.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    return config
  },
  error => Promise.reject(error)
)

instance.interceptors.response.use(
  response => {
    if (response.status >= 200 && response.status < 400) {
      return Promise.resolve(response.data)
    }

    message.error('未知的请求错误！')
    return Promise.reject(response)
  },
  error => {
    if (error && error.response) {
      if (error.response.status === 401) {
        message.error('登录已过期，请重新登录')
        const userStore = useUserStore()
        userStore.logout()
        router.replace('/login')
        return Promise.reject(error.message)
      }
      else if (error.response.status >= 400 && error.response.status < 500) {
        return Promise.reject(error.message)
      }
      else if (error.response.status >= 500) {
        return Promise.reject(error.message)
      }
      
      message.error(error.response.data?.message || '服务器遇到未知错误！')
      return Promise.reject(error.message)
    }

    message.error('连接到服务器失败 或 服务器响应超时！')
    return Promise.reject(error)
  }
)

export default instance
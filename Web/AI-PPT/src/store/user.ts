import { defineStore } from 'pinia'
import { ref } from 'vue'

const TOKEN_KEY = 'pptist_token'
const SYSTEM_CODE = 'userHubTest' // According to LoginController.java

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref<any>(null)

  const setToken = (value: string) => {
    localStorage.setItem(TOKEN_KEY, value)
    token.value = value
  }

  const logout = () => {
    localStorage.removeItem(TOKEN_KEY)
    token.value = ''
    userInfo.value = null
  }

  return { 
    token, 
    userInfo, 
    setToken, 
    logout 
  }
})

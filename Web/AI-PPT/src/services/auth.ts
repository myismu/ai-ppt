import axios from './axios'
import { SERVER_URL } from './index'

export default {
  login(data: any): Promise<any> {
    return axios.post(`${SERVER_URL}/auth/login`, data)
  },

  getUserInfo(): Promise<any> {
    return axios.get(`${SERVER_URL}/auth/getUserInfo`)
  },

  outLogin(): Promise<any> {
    return axios.get(`${SERVER_URL}/auth/OutLogin`)
  }
}

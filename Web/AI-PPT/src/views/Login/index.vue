<template>
  <div class="login-wrapper">
    <div class="login-container">
      <div class="login-header">
        <h1>AI PPT</h1>
        <p>欢迎回到您的智能演示工作空站</p>
      </div>
      
      <div class="login-form">
        <div class="form-item">
          <label>账号</label>
          <Input 
            v-model:value="form.username" 
            placeholder="请输入账号" 
            @keyup.enter="handleLogin"
          />
        </div>
        
        <div class="form-item">
          <label>密码</label>
          <Input 
            v-model:value="form.password" 
            type="password" 
            placeholder="请输入密码" 
            @keyup.enter="handleLogin"
          />
        </div>
        
        <div class="form-item action-item">
          <Button 
            type="primary" 
            class="submit-btn" 
            @click="handleLogin"
            :loading="loading"
          >
            登录
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import authApi from '@/services/auth'
import message from '@/utils/message'
import Input from '@/components/Input.vue'
import Button from '@/components/Button.vue'

const userStore = useUserStore()
const router = useRouter()

const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  systemCode: 'userHubTest'
})

const handleLogin = async () => {
  if (!form.username || !form.password) {
    message.warning('请输入账号和密码')
    return
  }
  
  loading.value = true
  try {
    const res = await authApi.login(form)
    if (res?.data?.token || res?.token) {
      const tokenStr = res.data?.token || res.token
      userStore.setToken(tokenStr)
      message.success('登录成功')
      
      // 获取用户详情并保存
      const userInfo = await authApi.getUserInfo()
      if (userInfo && userInfo.data) {
        userStore.userInfo = userInfo.data
      }
      
      // 路由跳转
      router.replace('/dashboard')
    } else {
      message.error(res?.message || '登录失败')
    }
  } catch (error: any) {
    console.error('Login error:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100vw;
  background-color: #f5f7fa;
  background-image: linear-gradient(135deg, #e0eafc 0%, #cfdef3 100%);
}

.login-container {
  width: 400px;
  padding: 40px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
  
  h1 {
    font-size: 28px;
    font-weight: 600;
    color: #333;
    margin-bottom: 10px;
  }
  
  p {
    font-size: 14px;
    color: #666;
  }
}

.login-form {
  .form-item {
    margin-bottom: 20px;
    
    label {
      display: block;
      margin-bottom: 8px;
      font-size: 14px;
      color: #333;
      font-weight: 500;
    }
  }
  
  .action-item {
    margin-top: 30px;
    margin-bottom: 0;
  }
  
  .submit-btn {
    width: 100%;
    height: 40px;
    font-size: 16px;
  }
}
</style>

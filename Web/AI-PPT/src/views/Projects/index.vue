<template>
  <div class="projects-workstation-v2">
    <!-- 背景装饰层 -->
    <div class="bg-glow-1"></div>
    <div class="bg-glow-2"></div>
    
    <!-- 侧边栏：玻璃拟态 -->
    <aside class="sidebar-v2">
      <div class="brand-area">
        <div class="logo-box">
          <i-icon-park-outline:magic class="icon" />
        </div>
        <div class="brand-info">
          <div class="name">AI PPT</div>
          <div class="version">V2.0 PRO</div>
        </div>
      </div>

      <nav class="nav-menu-v2">
        <div class="menu-item" :class="{ active: currentTab === 'all' }" @click="switchTab('all')">
          <i-icon-park-outline:folder-block class="icon" />
          <span>我的创作</span>
        </div>
        <div class="menu-item">
          <i-icon-park-outline:star class="icon" />
          <span>收藏夹</span>
        </div>
        <div class="menu-item" :class="{ active: currentTab === 'recycle' }" @click="switchTab('recycle')">
          <i-icon-park-outline:delete class="icon" />
          <span>回收站</span>
        </div>
      </nav>

      <div class="sidebar-footer">
        <div class="storage-card">
          <div class="label">云端存储空间</div>
          <div class="progress-mini"><div class="fill" style="width: 45%"></div></div>
          <div class="detail">已使用 45% (2.2GB / 5GB)</div>
        </div>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content-v2">
      <!-- 顶部条 -->
      <header class="workstation-header">
        <div class="search-bar-v2">
          <i-icon-park-outline:search class="icon" />
          <input type="text" placeholder="搜索我的演示文稿..." />
        </div>

        <div class="header-right-v2">
          <div class="user-chip-v2" v-if="userInfo">
            <img class="avatar" :src="userInfo.avatar || 'https://api.dicebear.com/7.x/notionists/svg?seed=' + userInfo.username" alt="Avatar"/>
            <div class="info">
              <span class="name">{{ userInfo.username || '创作专家' }}</span>
              <span class="role">Premium User</span>
            </div>
            <i-icon-park-outline:down class="arrow" />
          </div>
          <button class="btn-logout-v2" @click="handleLogout" title="退出登录">
            <i-icon-park-outline:logout />
          </button>
        </div>
      </header>

      <!-- 欢迎与操作区 (回收站不显示) -->
      <section class="welcome-section" v-if="currentTab === 'all'">
        <div class="text-group">
          <h2>欢迎回来，{{ userInfo?.username || '创作者' }}</h2>
          <p>今天想开启什么样的精彩演示？</p>
        </div>
        <button class="btn-create-v2" @click="handleCreate">
          <i-icon-park-outline:plus class="icon" />
          <span>开启 AI 智能创作</span>
        </button>
      </section>
      
      <section class="welcome-section" v-else>
        <div class="text-group">
          <h2>回收站</h2>
          <p>项目将在回收站中保留，您可以随时恢复或彻底删除。</p>
        </div>
      </section>

      <!-- 项目展示区 -->
      <div class="content-body-v2">
        <!-- 加载中 -->
        <div class="loading-state-v2" v-if="loading">
          <div class="ai-loader"></div>
          <span>正在同步云端数据...</span>
        </div>

        <!-- 空态 -->
        <div class="empty-state-v2" v-else-if="projects.length === 0">
          <div class="empty-art">
            <i-icon-park-outline:doc-detail class="icon" />
          </div>
          <h3>{{ currentTab === 'all' ? '开启您的第一篇杰作' : '回收站空空如也' }}</h3>
          <p>{{ currentTab === 'all' ? '让 AI 助您完成从灵感到演示的全过程' : '这里没有任何被删除的项目' }}</p>
          <button class="btn-ghost" @click="handleCreate" v-if="currentTab === 'all'">立即尝试</button>
        </div>

        <!-- 项目列表：Bento Grid 风格 -->
        <div class="projects-grid-v2" v-else>
          <div 
            class="project-card-v2" 
            v-for="(project, index) in projects" 
            :key="project.id"
            :style="{ '--delay': index * 0.05 + 's' }"
            @click="handleOpen(project.id)"
          >
            <div class="card-inner">
              <div class="card-preview-v2">
                <div class="preview-overlay">
                  <!-- 常规模式：编辑与删除 -->
                  <template v-if="currentTab === 'all'">
                    <div class="p-btn edit" @click.stop="handleOpen(project.id)" title="进入编辑"><i-icon-park-outline:edit /></div>
                    <div class="p-btn del" @click.stop="handleDelete(project)" title="移至回收站"><i-icon-park-outline:delete /></div>
                  </template>
                  <!-- 回收站模式：恢复与彻底删除 -->
                  <template v-else>
                    <div class="p-btn edit" @click.stop="handleRestore(project)" title="恢复项目"><i-icon-park-outline:refresh /></div>
                    <div class="p-btn del" @click.stop="handleHardDelete(project)" title="永久删除"><i-icon-park-outline:caution /></div>
                  </template>
                </div>
                <!-- 动态生成的艺术背景 -->
                <div class="art-bg" :style="{ background: getArtBg(index) }">
                  <i-icon-park-outline:ppt class="bg-icon" />
                  <span class="page-tag">{{ project.slideCount || 0 }} P</span>
                </div>
              </div>
              <div class="card-info-v2">
                <div class="title">{{ project.title || '未命名项目' }}</div>
                <div class="meta">
                  <span class="time">{{ formatTime(project.updateTime) }}</span>
                  <span class="dot">·</span>
                  <span class="status" :style="{ color: currentTab === 'recycle' ? '#94a3b8' : '#10b981' }">
                    {{ currentTab === 'recycle' ? '已删除' : '已同步' }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 极简删除确认 (移至回收站) -->
    <div class="modal-v2-overlay" v-if="deleteTarget" @click="deleteTarget = null">
      <div class="modal-v2" @click.stop>
        <div class="modal-icon"><i-icon-park-outline:caution /></div>
        <h3>确认移至回收站？</h3>
        <p>项目「{{ deleteTarget.title }}」将被临时移除，您可以随时在回收站中找回。</p>
        <div class="modal-footer">
          <button class="m-btn cancel" @click="deleteTarget = null">我再想想</button>
          <button class="m-btn confirm" @click="confirmDelete">确认移除</button>
        </div>
      </div>
    </div>

    <!-- 彻底删除确认 (物理删除) -->
    <div class="modal-v2-overlay" v-if="hardDeleteTarget" @click="hardDeleteTarget = null">
      <div class="modal-v2" @click.stop>
        <div class="modal-icon" style="background: #fee2e2; color: #ef4444;"><i-icon-park-outline:delete-five /></div>
        <h3 style="color: #ef4444;">彻底永久删除？</h3>
        <p>警告：项目「{{ hardDeleteTarget.title }}」及其所有云端静态资源将被物理抹除，此操作不可逆！</p>
        <div class="modal-footer">
          <button class="m-btn cancel" @click="hardDeleteTarget = null">取消</button>
          <button class="m-btn confirm" style="background: #ef4444;" @click="confirmHardDelete">确定永久删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import api from '@/services'
import message from '@/utils/message'

interface ProjectItem {
  id: number
  title: string
  coverUrl: string | null
  slideCount: number
  status: number
  createTime: string
  updateTime: string
}

const router = useRouter()
const userStore = useUserStore()
const userInfo = ref(userStore.userInfo)
const projects = ref<ProjectItem[]>([])
const loading = ref(true)
const deleteTarget = ref<ProjectItem | null>(null)
const hardDeleteTarget = ref<ProjectItem | null>(null) // 彻底删除目标

// 当前标签页: 'all' 表示我的创作, 'recycle' 表示回收站
const currentTab = ref<'all' | 'recycle'>('all')

// 获取项目列表
const fetchProjects = async () => {
  loading.value = true
  try {
    // 如果是回收站，传入 status = -1
    const status = currentTab.value === 'recycle' ? -1 : undefined
    const res = await api.listProjects(status)
    if (res.code === 200 && res.data) projects.value = res.data
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

// 切换标签页
const switchTab = (tab: 'all' | 'recycle') => {
  if (currentTab.value === tab) return
  currentTab.value = tab
  fetchProjects()
}

// 新建项目
const handleCreate = async () => {
  try {
    const res = await api.createProject('未命名演示文稿')
    if (res.code === 200 && res.data?.id) router.push(`/editor/${res.data.id}`)
  } catch (err) {
    message.error('创建失败')
  }
}

// 打开项目
const handleOpen = (id: number) => {
  if (currentTab.value === 'recycle') return // 回收站内不可编辑
  router.push(`/editor/${id}`)
}

// 删除项目（逻辑删除）
const handleDelete = (project: ProjectItem) => {
  deleteTarget.value = project
}

// 彻底删除项目
const handleHardDelete = (project: ProjectItem) => {
  hardDeleteTarget.value = project
}

// 恢复项目
const handleRestore = async (project: ProjectItem) => {
  try {
    const res = await api.restoreProject(project.id)
    if (res.code === 200) {
      message.success('已恢复至创作列表')
      fetchProjects()
    }
  } catch (err) {
    message.error('恢复失败')
  }
}

// 确认彻底删除
const confirmHardDelete = async () => {
  if (!hardDeleteTarget.value) return
  try {
    const res = await api.hardDeleteProject(hardDeleteTarget.value.id)
    if (res.code === 200) {
      message.success('已永久删除')
      fetchProjects()
    }
  } catch (err) {
    message.error('彻底删除失败')
  } finally {
    hardDeleteTarget.value = null
  }
}

// 确认删除（移至回收站）
const confirmDelete = async () => {
  if (!deleteTarget.value) return
  try {
    const res = await api.deleteProject(deleteTarget.value.id)
    if (res.code === 200) {
      message.success('已移至回收站')
      fetchProjects()
    }
  } catch (err) {
    message.error('操作失败')
  } finally {
    deleteTarget.value = null
  }
}

const handleLogout = () => {
  userStore.logout()
  router.replace('/login')
}

const formatTime = (time: string) => {
  if (!time) return ''
  const d = new Date(time)
  const diff = new Date().getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}m 前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}h 前`
  return `${d.getMonth() + 1}/${d.getDate()}`
}

const getArtBg = (index: number) => {
  const gradients = [
    'linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%)',
    'linear-gradient(135deg, #0ea5e9 0%, #2563eb 100%)',
    'linear-gradient(135deg, #f43f5e 0%, #fb7185 100%)',
    'linear-gradient(135deg, #10b981 0%, #059669 100%)',
    'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)'
  ]
  return gradients[index % gradients.length]
}

onMounted(() => fetchProjects())
</script>

<style lang="scss" scoped>
.projects-workstation-v2 {
  display: flex;
  min-height: 100vh;
  background: #f8fafc;
  position: relative;
  overflow: hidden;
  font-family: 'Inter', 'SF Pro Display', sans-serif;
  color: #1e293b;
}

// ==================== 背景装饰 ====================
.bg-glow-1 { position: absolute; top: -10%; right: -5%; width: 40vw; height: 40vw; background: radial-gradient(circle, rgba(79, 70, 229, 0.05) 0%, transparent 70%); z-index: 0; }
.bg-glow-2 { position: absolute; bottom: -10%; left: -5%; width: 35vw; height: 35vw; background: radial-gradient(circle, rgba(6, 182, 212, 0.05) 0%, transparent 70%); z-index: 0; }

// ==================== 侧边栏 ====================
.sidebar-v2 {
  width: 260px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(25px);
  border-right: 1px solid rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  padding: 32px 20px;
  z-index: 10;

  .brand-area {
    display: flex; align-items: center; gap: 14px; margin-bottom: 48px;
    .logo-box {
      width: 42px; height: 42px; border-radius: 12px; background: linear-gradient(135deg, #4f46e5, #06b6d4);
      display: flex; align-items: center; justify-content: center; color: #fff; font-size: 24px;
      box-shadow: 0 8px 16px rgba(79, 70, 229, 0.25);
    }
    .brand-info {
      .name { font-size: 18px; font-weight: 900; letter-spacing: -0.5px; }
      .version { font-size: 10px; font-weight: 800; color: #7c3aed; background: rgba(124, 58, 237, 0.1); padding: 1px 6px; border-radius: 4px; margin-top: 2px; display: inline-block; }
    }
  }

  .nav-menu-v2 {
    flex: 1; display: flex; flex-direction: column; gap: 8px;
    .menu-item {
      height: 44px; padding: 0 16px; border-radius: 12px; display: flex; align-items: center; gap: 12px;
      color: #64748b; font-weight: 600; font-size: 14px; cursor: pointer; transition: all 0.2s;
      &:hover { background: rgba(0,0,0,0.03); color: #1e293b; }
      &.active { background: #fff; color: $themeColor; box-shadow: 0 4px 12px rgba(0,0,0,0.04); border: 1px solid rgba(0,0,0,0.02); }
      .icon { font-size: 18px; }
    }
  }

  .storage-card {
    background: #fff; border-radius: 16px; padding: 16px; border: 1px solid rgba(0,0,0,0.03);
    .label { font-size: 11px; font-weight: 800; color: #94a3b8; margin-bottom: 8px; }
    .progress-mini { height: 6px; background: #f1f5f9; border-radius: 3px; overflow: hidden; margin-bottom: 8px; 
      .fill { height: 100%; background: linear-gradient(90deg, #4f46e5, #06b6d4); }
    }
    .detail { font-size: 10px; color: #64748b; font-weight: 600; }
  }
}

// ==================== 主内容 ====================
.main-content-v2 {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0 48px;
  position: relative;
  z-index: 5;
  overflow-y: auto;
}

.workstation-header {
  height: 80px; display: flex; align-items: center; justify-content: space-between; flex-shrink: 0;
  
  .search-bar-v2 {
    width: 400px; height: 44px; background: rgba(0,0,0,0.03); border-radius: 12px; display: flex; align-items: center; padding: 0 16px; gap: 12px;
    .icon { color: #94a3b8; }
    input { background: transparent; border: none; outline: none; flex: 1; font-size: 14px; font-weight: 600; color: #1e293b;
      &::placeholder { color: #cbd5e1; }
    }
    &:focus-within { background: #fff; box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.1); }
  }

  .header-right-v2 { display: flex; align-items: center; gap: 20px; }
  
  .user-chip-v2 {
    display: flex; align-items: center; gap: 10px; padding: 4px 12px 4px 4px; background: #fff; border-radius: 30px; border: 1px solid rgba(0,0,0,0.04); cursor: pointer; transition: all 0.2s;
    &:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
    .avatar { width: 32px; height: 32px; border-radius: 50%; border: 2px solid #fff; }
    .info { display: flex; flex-direction: column; 
      .name { font-size: 12px; font-weight: 800; color: #1e293b; line-height: 1.2; }
      .role { font-size: 10px; font-weight: 600; color: #94a3b8; }
    }
    .arrow { font-size: 12px; color: #cbd5e1; }
  }

  .btn-logout-v2 {
    width: 40px; height: 40px; border-radius: 50%; border: none; background: #fff; color: #94a3b8; cursor: pointer; transition: all 0.2s;
    display: flex; align-items: center; justify-content: center; font-size: 18px;
    &:hover { color: #1e293b; background: #f1f5f9; }
  }
}

.welcome-section {
  padding: 24px 0 40px; display: flex; align-items: flex-end; justify-content: space-between; border-bottom: 1px solid rgba(0,0,0,0.04);
  .text-group {
    h2 { font-size: 28px; font-weight: 900; letter-spacing: -1px; margin-bottom: 4px; }
    p { font-size: 15px; color: #64748b; font-weight: 500; }
  }
  .btn-create-v2 {
    height: 52px; padding: 0 32px; border-radius: 16px; border: none; background: #1e293b; color: #fff;
    font-size: 15px; font-weight: 800; display: flex; align-items: center; gap: 12px; cursor: pointer; transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
    &:hover { transform: translateY(-4px); background: #0f172a; box-shadow: 0 16px 32px rgba(0, 0, 0, 0.15); }
    &:active { transform: translateY(0); }
  }
}

.content-body-v2 { padding: 40px 0; }

.loading-state-v2 {
  display: flex; flex-direction: column; align-items: center; padding: 100px 0; gap: 20px; color: #94a3b8;
  .ai-loader { width: 40px; height: 40px; border: 3px solid #f1f5f9; border-top-color: #1e293b; border-radius: 50%; animation: spin 0.8s linear infinite; }
}

@keyframes spin { to { transform: rotate(360deg); } }

.empty-state-v2 {
  display: flex; flex-direction: column; align-items: center; padding: 80px 0;
  .empty-art { 
    width: 120px; height: 120px; background: #fff; border-radius: 40px; display: flex; align-items: center; justify-content: center; font-size: 48px; color: #e2e8f0; margin-bottom: 32px; box-shadow: 0 20px 40px rgba(0,0,0,0.03); 
    .icon { animation: float 3s ease-in-out infinite; }
  }
  h3 { font-size: 20px; font-weight: 800; margin-bottom: 12px; }
  p { font-size: 14px; color: #94a3b8; margin-bottom: 32px; }
  .btn-ghost { padding: 10px 32px; border-radius: 12px; border: 2px solid #e2e8f0; background: transparent; font-weight: 800; color: #64748b; cursor: pointer; transition: all 0.2s; &:hover { border-color: #1e293b; color: #1e293b; background: #fff; } }
}

@keyframes float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-10px); } }

.projects-grid-v2 {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 32px;
}

.project-card-v2 {
  animation: slideUp 0.6s cubic-bezier(0.2, 0.8, 0.2, 1) forwards;
  animation-delay: var(--delay);
  opacity: 0;

  .card-inner {
    cursor: pointer; position: relative;
    &:hover {
      .card-preview-v2 { transform: translateY(-8px); box-shadow: 0 20px 40px rgba(0,0,0,0.1); .preview-overlay { opacity: 1; } }
      .card-info-v2 .title { color: #1e293b; }
    }
  }

  .card-preview-v2 {
    height: 160px; border-radius: 20px; overflow: hidden; position: relative; transition: all 0.4s cubic-bezier(0.2, 0.8, 0.2, 1);
    background: #e2e8f0; box-shadow: 0 10px 20px rgba(0,0,0,0.04);
    
    .art-bg { 
      width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; position: relative;
      .bg-icon { font-size: 48px; color: rgba(255,255,255,0.2); }
      .page-tag { position: absolute; bottom: 12px; right: 12px; background: rgba(0,0,0,0.2); backdrop-filter: blur(8px); color: #fff; padding: 2px 10px; border-radius: 20px; font-size: 10px; font-weight: 800; }
    }

    .preview-overlay {
      position: absolute; inset: 0; background: rgba(0,0,0,0.2); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; gap: 12px; opacity: 0; transition: opacity 0.3s; z-index: 2;
      .p-btn { 
        width: 38px; height: 38px; border-radius: 12px; background: #fff; display: flex; align-items: center; justify-content: center; font-size: 18px; color: #1e293b; transition: all 0.2s;
        &:hover { transform: scale(1.1); &.del { color: #ef4444; } &.edit { color: #1e293b; } }
      }
    }
  }

  .card-info-v2 {
    padding: 16px 4px;
    .title { font-size: 15px; font-weight: 700; color: #334155; margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; transition: color 0.2s; }
    .meta { 
      display: flex; align-items: center; gap: 6px; font-size: 12px; color: #94a3b8; font-weight: 600;
      .status { color: #64748b; }
    }
  }
}

@keyframes slideUp { from { opacity: 0; transform: translateY(30px); } to { opacity: 1; transform: translateY(0); } }

// ==================== 弹窗 ====================
.modal-v2-overlay {
  position: fixed; inset: 0; background: rgba(15, 23, 42, 0.6); backdrop-filter: blur(8px); display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.modal-v2 {
  width: 400px; background: #fff; border-radius: 24px; padding: 40px; text-align: center; box-shadow: 0 40px 80px rgba(0,0,0,0.2); animation: pop 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  .modal-icon { width: 64px; height: 64px; background: #f8fafc; color: #475569; border-radius: 20px; display: flex; align-items: center; justify-content: center; font-size: 32px; margin: 0 auto 24px; }
  h3 { font-size: 20px; font-weight: 900; margin-bottom: 12px; }
  p { font-size: 15px; color: #64748b; line-height: 1.6; margin-bottom: 32px; }
  .modal-footer { display: flex; gap: 12px; 
    .m-btn { flex: 1; height: 48px; border-radius: 14px; border: none; font-weight: 800; font-size: 14px; cursor: pointer; transition: all 0.2s; 
      &.cancel { background: #f1f5f9; color: #64748b; &:hover { background: #e2e8f0; } }
      &.confirm { background: #1e293b; color: #fff; box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1); &:hover { background: #0f172a; transform: translateY(-2px); } }
    }
  }
}

@keyframes pop { from { opacity: 0; transform: scale(0.9) translateY(20px); } to { opacity: 1; transform: scale(1) translateY(0); } }
</style>

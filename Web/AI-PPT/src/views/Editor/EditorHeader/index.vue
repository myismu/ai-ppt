<template>
  <div class="editor-header-v2">
    <!-- MacOS 风格控制点 -->
    <div class="header-left">
      <div class="window-controls">
        <span class="dot red"></span>
        <span class="dot yellow"></span>
        <span class="dot green"></span>
      </div>

      <Popover trigger="click" placement="bottom-start" v-model:value="mainMenuVisible">
        <template #content>
          <div class="main-menu-v2">
            <div class="ai-menu-card" @click="openAIPPTDialog(); mainMenuVisible = false">
              <div class="icon-box"><i-custom:click /></div>
              <div class="text-content">
                <div class="title-gradient">AIPPT 智能创作</div>
                <div class="subtitle">输入一句话，全自动排版生成</div>
              </div>
            </div>
            
            <Divider :margin="12" />
            
            <div class="import-section-v2">
              <div class="section-label">导入本地素材</div>
              <div class="import-grid-v2">
                <FileInput class="import-item" accept=".pptx" @change="files => { importPPTXFile(files); mainMenuVisible = false }">
                  <i-icon-park-outline:file-ppt />
                  <span>PPTX</span>
                </FileInput>
                <FileInput class="import-item" accept=".json" @change="files => { importJSON(files); mainMenuVisible = false }">
                  <i-icon-park-outline:file-code />
                  <span>JSON</span>
                </FileInput>
                <FileInput class="import-item" accept=".pptist" @change="files => { importSpecificFile(files); mainMenuVisible = false }">
                  <i-icon-park-outline:data-display />
                  <span>PPTIST</span>
                </FileInput>
              </div>
            </div>

            <Divider :margin="12" />
            
            <div class="menu-list-v2">
              <PopoverMenuItem @click="setDialogForExport('pptx')"><i-icon-park-outline:download /> 导出演示文稿</PopoverMenuItem>
              <PopoverMenuItem @click="resetSlides(); mainMenuVisible = false"><i-icon-park-outline:refresh /> 重置画布</PopoverMenuItem>
              <PopoverMenuItem @click="openMarkupPanel(); mainMenuVisible = false"><i-icon-park-outline:mark /> 页面类型标注</PopoverMenuItem>
              <PopoverMenuItem @click="exportAITemplate(); mainMenuVisible = false"><i-icon-park-outline:data-file /> 导出为 AI 模板</PopoverMenuItem>
              <PopoverMenuItem @click="mainMenuVisible = false; hotkeyDrawerVisible = true"><i-icon-park-outline:command /> 快捷键指南</PopoverMenuItem>
            </div>
          </div>
        </template>
        <div class="action-btn menu-trigger"><i-icon-park-outline:hamburger-button /></div>
      </Popover>

      <div class="action-btn" v-tooltip="'返回工作台'" @click="goBackToProjects()">
        <i-icon-park-outline:left />
      </div>
    </div>

    <!-- 居中标题区 -->
    <div class="header-center">
      <div class="app-brand">
        <span class="brand-text">AI 智能 PPT 创作引擎</span>
        <span class="brand-version">V2.0</span>
      </div>
      <div class="title-editor">
        <Input 
          v-if="editingTitle"
          ref="titleInputRef"
          v-model:value="titleValue" 
          class="title-input-v2"
          @blur="handleUpdateTitle()" 
          @keydown.enter="handleUpdateTitle()"
        />
        <div v-else class="title-display" @click="startEditTitle()">
          {{ title }}
          <i-icon-park-outline:edit class="edit-icon" />
        </div>
      </div>
    </div>

    <!-- 右侧功能区 -->
    <div class="header-right">
      <div class="save-indicator" :class="saveStatus">
        <span class="indicator-dot"></span>
        <span class="indicator-text">{{ saveStatusText }}</span>
      </div>

      <div class="user-chip" v-if="userStore.userInfo">
        <img :src="userStore.userInfo.avatar || 'https://api.dicebear.com/7.x/notionists/svg?seed=' + userStore.userInfo.userName" class="avatar" />
        <span class="name">{{ userStore.userInfo.userName || userStore.userInfo.nickName }}</span>
      </div>

      <div class="button-group-v2">
        <button class="btn-icon secondary" v-tooltip="'云端保存'" @click="handleCloudSave()">
          <i-icon-park-outline:cloud-storage />
        </button>
        <button class="btn-ai-v2" @click="openAIPPTDialog()">
          <i-icon-park-outline:magic />
          <span>AIPPT智能创作</span>
        </button>
        <button class="btn-icon primary" v-tooltip="'导出'" @click="setDialogForExport('pptx')">
          <i-icon-park-outline:download />
        </button>
        <button class="btn-icon secondary" v-tooltip="'返回工作台'" @click="goBackToProjects()">
          <i-icon-park-outline:home />
        </button>
      </div>
    </div>

    <Drawer :width="320" v-model:visible="hotkeyDrawerVisible" placement="right">
      <HotkeyDoc />
      <template v-slot:title>快捷键指南</template>
    </Drawer>
    
    <FullscreenSpin :loading="exporting" tip="正在处理中..." />
  </div>
</template>

<script lang="ts" setup>
import { computed, nextTick, ref, useTemplateRef } from 'vue'
import { storeToRefs } from 'pinia'
import { useMainStore, useSlidesStore } from '@/store'
import useImport from '@/hooks/useImport'
import useSlideHandler from '@/hooks/useSlideHandler'
import useCoverCapture from '@/hooks/useCoverCapture'
import { downloadAITemplate } from '@/utils/aiTemplateParser'
import type { DialogForExportTypes } from '@/types/export'
import api from '@/services'
import message from '@/utils/message'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'

import HotkeyDoc from './HotkeyDoc.vue'
import FileInput from '@/components/FileInput.vue'
import FullscreenSpin from '@/components/FullscreenSpin.vue'
import Drawer from '@/components/Drawer.vue'
import Input from '@/components/Input.vue'
import Popover from '@/components/Popover.vue'
import PopoverMenuItem from '@/components/PopoverMenuItem.vue'
import Divider from '@/components/Divider.vue'

const mainStore = useMainStore()
const slidesStore = useSlidesStore()
const userStore = useUserStore()
const router = useRouter()
const { title, saveStatus } = storeToRefs(slidesStore)
const { importSpecificFile, importPPTXFile, importJSON, exporting } = useImport()
const { resetSlides } = useSlideHandler()
const { captureAndSaveCover } = useCoverCapture()

const mainMenuVisible = ref(false)
const hotkeyDrawerVisible = ref(false)
const editingTitle = ref(false)
const titleValue = ref('')
const titleInputRef = useTemplateRef<InstanceType<typeof Input>>('titleInputRef')

const saveStatusText = computed(() => {
  const map = { saved: '已保存', saving: '同步中...', unsaved: '待保存' }
  return map[saveStatus.value] || ''
})

const startEditTitle = () => {
  titleValue.value = title.value
  editingTitle.value = true
  nextTick(() => titleInputRef.value?.focus())
}

const handleUpdateTitle = () => {
  slidesStore.setTitle(titleValue.value)
  editingTitle.value = false
}

const setDialogForExport = (type: DialogForExportTypes) => {
  mainStore.setDialogForExport(type)
  mainMenuVisible.value = false
}

const openMarkupPanel = () => mainStore.setMarkupPanelState(true)

const exportAITemplate = () => {
  if (slidesStore.slides.length === 0) return
  const fullData = { 
    slides: slidesStore.slides, 
    theme: slidesStore.theme, 
    width: slidesStore.width, 
    height: slidesStore.height 
  }
  downloadAITemplate(fullData, titleValue.value || '自定义AI模板')
}

const openAIPPTDialog = () => mainStore.setAIPPTDialogState(true)

const handleCloudSave = async () => {
  const currentProjectId = slidesStore.projectId
  if (!currentProjectId) return message.warning('当前项目未关联')
  try {
    slidesStore.setSaveStatus('saving')
    const res = await api.saveProject({
      id: currentProjectId, title: title.value,
      contentJson: JSON.stringify(slidesStore.slides),
      themeJson: JSON.stringify(slidesStore.theme),
      refDocuments: JSON.stringify(slidesStore.refDocuments),
      slideCount: slidesStore.slides.length,
    })
    if (res.code === 200) {
      slidesStore.setSaveStatus('saved')
      message.success('云端已同步')
      // 异步截取第一页作为封面（不阻塞主保存流程）
      captureAndSaveCover(currentProjectId).catch(() => {})
    } else {
      slidesStore.setSaveStatus('unsaved')
      message.error(res.message || '保存失败')
    }
  } catch (err: any) {
    slidesStore.setSaveStatus('unsaved')
    message.error('网络错误')
  }
}

const goBackToProjects = () => router.push('/dashboard')


</script>

<style lang="scss" scoped>
.editor-header-v2 {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 56px;
  user-select: none;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;

  .window-controls {
    display: flex;
    gap: 8px;
    margin-right: 10px;
    .dot {
      width: 12px; height: 12px; border-radius: 50%;
      &.red { background: #ff5f56; }
      &.yellow { background: #ffbd2e; }
      &.green { background: #27c93f; }
    }
  }

  .action-btn {
    width: 34px; height: 34px; border-radius: 10px;
    display: flex; align-items: center; justify-content: center;
    color: #64748b; cursor: pointer; transition: all 0.2s;
    &:hover { background: rgba(0,0,0,0.05); color: #1e293b; }
    font-size: 18px;
  }
}

.header-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  flex: 2;

  .app-brand {
    display: flex; align-items: center; gap: 6px;
    .brand-text { font-size: 11px; font-weight: 800; color: #94a3b8; letter-spacing: 1px; text-transform: uppercase; }
    .brand-version { font-size: 10px; background: $themeColor; color: #fff; padding: 1px 5px; border-radius: 4px; font-weight: 900; }
  }

  .title-editor {
    .title-display {
      font-size: 14px; font-weight: 700; color: #334155; display: flex; align-items: center; gap: 6px; cursor: pointer;
      padding: 2px 8px; border-radius: 6px; transition: background 0.2s;
      &:hover { background: rgba(0,0,0,0.04); .edit-icon { opacity: 1; } }
      .edit-icon { font-size: 12px; color: #94a3b8; opacity: 0; }
    }
    .title-input-v2 {
      width: 240px; height: 24px;
      ::v-deep(input) { font-size: 14px; font-weight: 700; text-align: center; background: transparent; border: none; border-bottom: 2px solid $themeColor; border-radius: 0; padding: 0; }
    }
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  justify-content: flex-end;
  flex: 1;

  .save-indicator {
    display: flex; align-items: center; gap: 6px; font-size: 12px; font-weight: 600; color: #94a3b8;
    .indicator-dot { width: 6px; height: 6px; border-radius: 50%; background: #cbd5e1; }
    &.saved { color: #22c55e; .indicator-dot { background: #22c55e; } }
    &.saving { color: #f59e0b; .indicator-dot { background: #f59e0b; animation: pulse 1.5s infinite; } }
  }

  .user-chip {
    display: flex; align-items: center; gap: 8px; padding: 4px 12px 4px 4px; background: #f8fafc; border-radius: 20px; border: 1px solid #f1f5f9;
    .avatar { width: 24px; height: 24px; border-radius: 50%; }
    .name { font-size: 12px; font-weight: 700; color: #475569; }
  }

  .button-group-v2 {
    display: flex; align-items: center; gap: 8px;
    
    .btn-icon {
      width: 36px; height: 36px; border-radius: 10px; border: none; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all 0.2s; font-size: 18px;
      &.secondary { background: #f1f5f9; color: #64748b; &:hover { background: #e2e8f0; color: #1e293b; } }
      &.primary { background: rgba($themeColor, 0.1); color: $themeColor; &:hover { background: $themeColor; color: #fff; } }
      &.danger { background: rgba(#ef4444, 0.1); color: #ef4444; &:hover { background: #ef4444; color: #fff; } }
    }

    .btn-ai-v2 {
      height: 36px; padding: 0 16px; border-radius: 10px; border: none; background: linear-gradient(135deg, #4f46e5, #06b6d4); color: #fff; font-size: 13px; font-weight: 700; display: flex; align-items: center; gap: 8px; cursor: pointer; transition: transform 0.2s, box-shadow 0.2s;
      box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
      &:hover { transform: translateY(-1px); box-shadow: 0 6px 16px rgba(79, 70, 229, 0.3); }
      &:active { transform: translateY(0); }
    }
  }
}

.main-menu-v2 {
  width: 320px; padding: 4px;
  
  .ai-menu-card {
    background: linear-gradient(135deg, #f5f3ff, #ede9fe); border-radius: 12px; padding: 16px; display: flex; align-items: center; gap: 16px; cursor: pointer; transition: all 0.2s;
    &:hover { transform: scale(1.02); box-shadow: 0 8px 24px rgba(139, 92, 246, 0.15); }
    
    .icon-box { width: 44px; height: 44px; border-radius: 12px; background: #fff; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #7c3aed; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
    .title-gradient { font-size: 15px; font-weight: 800; background: linear-gradient(135deg, #4f46e5, #06b6d4); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
    .subtitle { font-size: 11px; color: #7c3aed; font-weight: 600; margin-top: 2px; }
  }

  .import-section-v2 {
    padding: 8px;
    .section-label { font-size: 11px; font-weight: 800; color: #94a3b8; text-transform: uppercase; margin-bottom: 10px; }
    .import-grid-v2 {
      display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px;
      .import-item {
        display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 10px; border-radius: 10px; background: #f8fafc; border: 1px solid #f1f5f9; cursor: pointer; transition: all 0.2s;
        &:hover { background: #fff; border-color: $themeColor; transform: translateY(-2px); }
        i { font-size: 20px; color: #64748b; }
        span { font-size: 10px; font-weight: 700; color: #475569; }
      }
    }
  }

  .menu-list-v2 {
    display: flex; flex-direction: column; gap: 2px;
  }
}

@keyframes pulse { 0%, 100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.5; transform: scale(1.2); } }
</style>
<template>
  <template v-if="slides.length">
    <router-view />
  </template>
  <FullscreenSpin tip="数据加载中，请稍等 ..." v-else loading :mask="false" />
</template>

<script lang="ts" setup>
import { onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useMainStore, useSnapshotStore, useSlidesStore } from '@/store'
import { LOCALSTORAGE_KEY_DISCARDED_DB } from '@/configs/storage'
import { deleteDiscardedDB } from '@/utils/database'
import api from '@/services'
import FullscreenSpin from '@/components/FullscreenSpin.vue'

const route = useRoute()
const mainStore = useMainStore()
const slidesStore = useSlidesStore()
const snapshotStore = useSnapshotStore()
const { slides, theme, title, projectId, saveStatus } = storeToRefs(slidesStore)
const { databaseId } = storeToRefs(mainStore)

// 自动保存的 debounce 定时器
let saveTimer: ReturnType<typeof setTimeout> | null = null

/**
 * 根据路由 ID 加载项目数据（方案 B：两步加载）
 * 1. 从 ppt-service 获取项目元数据（含 contentUrl 路径）
 * 2. 用 contentUrl 直接从 file-service 拉取 JSON 文件
 */
const loadProject = async (id: string) => {
  try {
    const res = await api.getProjectDetail(id)
    if (res.code === 200 && res.data) {
      const project = res.data
      slidesStore.setProjectId(project.id)
      slidesStore.setTitle(project.title || '未命名演示文稿')

      // 获取主题数据
      let themeData
      if (project.themeUrl) {
        try {
          const themeRes = await fetch(project.themeUrl)
          themeData = await themeRes.json()
        } catch (e) {
          console.error('加载主题失败', e)
        }
      }

      // 如果有 contentUrl，从 file-service 拉取 JSON 文件
      if (project.contentUrl) {
        try {
          const response = await fetch(project.contentUrl)
          const slidesData = await response.json()
          slidesStore.setSlides(slidesData, themeData)
        } catch (fetchErr) {
          console.error('加载内容 JSON 文件失败:', fetchErr)
          const defaultSlides = await api.getTemplateData('slides')
          slidesStore.setSlides(defaultSlides, themeData)
        }
      } else {
        // 新项目，尚无内容文件，加载默认 slides 模板
        const defaultSlides = await api.getTemplateData('slides')
        slidesStore.setSlides(defaultSlides, themeData)
      }
    } else {
      // 项目不存在，加载默认数据
      const defaultSlides = await api.getTemplateData('slides')
      slidesStore.setSlides(defaultSlides)
    }
  } catch (err) {
    console.error('加载项目失败:', err)
    const defaultSlides = await api.getTemplateData('slides')
    slidesStore.setSlides(defaultSlides)
  }
}

/**
 * 执行自动保存
 */
const doAutoSave = async () => {
  if (!projectId.value || !slides.value.length) return

  slidesStore.setSaveStatus('saving')
  try {
    await api.saveProject({
      id: projectId.value,
      title: title.value,
      contentJson: JSON.stringify(slides.value),
      themeJson: JSON.stringify(theme.value),
      slideCount: slides.value.length,
    })
    slidesStore.setSaveStatus('saved')
  } catch (err) {
    console.error('自动保存失败:', err)
    slidesStore.setSaveStatus('unsaved')
  }
}

/**
 * 触发延迟自动保存（3 秒 debounce）
 */
const triggerAutoSave = () => {
  if (!projectId.value) return
  slidesStore.setSaveStatus('unsaved')
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(doAutoSave, 3000)
}

// 监听幻灯片和主题变化，触发自动保存
watch(
  () => [JSON.stringify(slides.value), JSON.stringify(theme.value), title.value],
  () => {
    triggerAutoSave()
  },
  { deep: false } // 用 JSON.stringify 避免深度监听的性能开销
)

onMounted(async () => {
  const projectIdParam = route.params.id as string
  if (projectIdParam) {
    await loadProject(projectIdParam)
  } else {
    // 兜底：没有 ID 时加载默认数据
    const defaultSlides = await api.getTemplateData('slides')
    slidesStore.setSlides(defaultSlides)
  }

  await deleteDiscardedDB()
  snapshotStore.initSnapshotDatabase()
})

// 页面离开前保存（路由切换时触发）
onUnmounted(() => {
  if (saveTimer) {
    clearTimeout(saveTimer)
    // 离开时立即保存一次
    doAutoSave()
  }
})

// 页面刷新/关闭时的清理
window.addEventListener('beforeunload', () => {
  // 尝试在关闭前触发保存（不保证成功，但 3 秒 debounce 机制已有保障）
  if (saveTimer) {
    clearTimeout(saveTimer)
    doAutoSave()
  }

  const discardedDB = localStorage.getItem(LOCALSTORAGE_KEY_DISCARDED_DB)
  const discardedDBList: string[] = discardedDB ? JSON.parse(discardedDB) : []
  discardedDBList.push(databaseId.value)
  const newDiscardedDB = JSON.stringify(discardedDBList)
  localStorage.setItem(LOCALSTORAGE_KEY_DISCARDED_DB, newDiscardedDB)
})
</script>


import { createApp, h, nextTick } from 'vue'
import { toJpeg } from 'html-to-image'
import { useSlidesStore } from '@/store'
import api from '@/services'
import ThumbnailSlide from '@/views/components/ThumbnailSlide/index.vue'

/**
 * 封面截图 Composable
 * 
 * 在保存时自动将第一页幻灯片渲染为缩略图并上传到 file-service，
 * 实现项目列表封面的自动化生成。
 * 
 * 原理：
 * 1. 创建离屏容器 → 挂载 ThumbnailSlide 组件渲染第一页
 * 2. 使用 html-to-image 截图为 JPEG (60% 质量, 400px 宽度)
 * 3. 上传到 file-service → 返回封面路径
 * 4. 调用 saveProject 更新数据库 coverUrl
 */
export default function useCoverCapture() {
  const slidesStore = useSlidesStore()

  /**
   * 捕获第一页幻灯片作为封面图
   * @param projectId 项目 ID
   * @returns 封面图在 file-service 的相对路径，失败返回 null
   */
  const captureAndUploadCover = async (projectId: number | string): Promise<string | null> => {
    const firstSlide = slidesStore.slides[0]
    if (!firstSlide) return null

    // 创建离屏容器
    const container = document.createElement('div')
    container.style.cssText = 'position:absolute;left:-9999px;top:-9999px;z-index:-1;overflow:hidden;'
    document.body.appendChild(container)

    // 创建挂载点
    const mountEl = document.createElement('div')
    container.appendChild(mountEl)

    try {
      // 使用 createApp 挂载一个独立的 ThumbnailSlide 实例
      const app = createApp({
        render() {
          return h(ThumbnailSlide, {
            slide: firstSlide,
            size: 400,
            visible: true,
          })
        },
      })

      // ThumbnailSlide 内部通过 useSlidesStore 获取 viewportRatio 等
      // 由于 Pinia 已在全局注册，createApp 需要手动注入 Pinia
      const { createPinia } = await import('pinia')
      const pinia = createPinia()
      
      // 复制当前 store 数据到新的 pinia 实例
      const currentState = slidesStore.$state
      app.use(pinia)
      const newSlidesStore = useSlidesStore(pinia)
      newSlidesStore.$patch(currentState)

      app.mount(mountEl)

      // 等待渲染稳定
      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 300))

      // 找到实际的 .thumbnail-slide DOM 节点
      const slideDom = mountEl.querySelector('.thumbnail-slide') as HTMLElement
      if (!slideDom) {
        app.unmount()
        return null
      }

      // 使用 html-to-image 截图为 JPEG
      const dataUrl = await toJpeg(slideDom, {
        quality: 0.6,
        width: 400,
        height: Math.round(400 * (slidesStore.viewportRatio)),
        pixelRatio: 1,
        fontEmbedCSS: '', // 跳过字体嵌入，加速截图
      })

      // 销毁 Vue 实例
      app.unmount()

      // dataURL → Blob → File
      const blob = dataURLtoBlob(dataUrl)
      const file = new File([blob], 'cover.jpg', { type: 'image/jpeg' })

      // 上传到 file-service
      const uploadRes = await api.uploadProjectAsset(projectId, file)
      if (uploadRes?.code === 200 && uploadRes?.data) {
        return uploadRes.data
      }
      return null
    }
    catch (err) {
      console.warn('[CoverCapture] 封面截图失败:', err)
      return null
    }
    finally {
      // 清理离屏容器
      document.body.removeChild(container)
    }
  }

  /**
   * 保存封面路径到数据库
   */
  const saveCoverUrl = async (projectId: number | string, coverUrl: string) => {
    try {
      await api.saveProject({ id: projectId, coverUrl })
    }
    catch (err) {
      console.warn('[CoverCapture] 封面路径保存失败:', err)
    }
  }

  /**
   * 一键截图并保存封面（异步执行，不阻塞主流程）
   */
  const captureAndSaveCover = async (projectId: number | string) => {
    const coverUrl = await captureAndUploadCover(projectId)
    if (coverUrl) {
      await saveCoverUrl(projectId, coverUrl)
    }
  }

  return {
    captureAndUploadCover,
    saveCoverUrl,
    captureAndSaveCover,
  }
}

/**
 * 将 dataURL 转换为 Blob 对象
 */
function dataURLtoBlob(dataUrl: string): Blob {
  const arr = dataUrl.split(',')
  const mime = arr[0].match(/:(.*?);/)![1]
  const bstr = atob(arr[1])
  let n = bstr.length
  const u8arr = new Uint8Array(n)
  while (n--) {
    u8arr[n] = bstr.charCodeAt(n)
  }
  return new Blob([u8arr], { type: mime })
}

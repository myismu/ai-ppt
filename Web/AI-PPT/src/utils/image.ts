import api from '@/services'
import { useSlidesStore } from '@/store'
import message from '@/utils/message'

interface ImageSize {
  width: number
  height: number
}

/**
 * 获取图片的原始宽高
 * @param src 图片地址
 */
export const getImageSize = (src: string): Promise<ImageSize> => {
  return new Promise(resolve => {
    const img = document.createElement('img')
    img.src = src
    img.style.opacity = '0'
    document.body.appendChild(img)

    img.onload = () => {
      const imgWidth = img.clientWidth
      const imgHeight = img.clientHeight
    
      img.onload = null
      img.onerror = null

      document.body.removeChild(img)

      resolve({ width: imgWidth, height: imgHeight })
    }

    img.onerror = () => {
      img.onload = null
      img.onerror = null
    }
  })
}

/**
 * 压缩图片
 * @param file 原文件
 * @param quality 压缩质量 (0-1)
 */
export const compressImage = (file: File, quality = 0.6): Promise<File> => {
  return new Promise((resolve) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        const canvas = document.createElement('canvas')
        let width = img.width
        let height = img.height
        
        // 限制最大宽高，避免长图撑爆内存
        const MAX_SIZE = 1920
        if (width > MAX_SIZE || height > MAX_SIZE) {
          if (width > height) {
            height = Math.round((height * MAX_SIZE) / width)
            width = MAX_SIZE
          } else {
            width = Math.round((width * MAX_SIZE) / height)
            height = MAX_SIZE
          }
        }
        
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        if (!ctx) return resolve(file)
        
        ctx.drawImage(img, 0, 0, width, height)
        canvas.toBlob((blob) => {
          if (blob) {
            const compressedFile = new File([blob], file.name, {
              type: file.type === 'image/png' ? 'image/png' : 'image/jpeg',
              lastModified: Date.now(),
            })
            resolve(compressedFile)
          } else {
            resolve(file)
          }
        }, file.type === 'image/png' ? 'image/png' : 'image/jpeg', quality)
      }
      img.onerror = () => resolve(file)
      img.src = e.target?.result as string
    }
    reader.onerror = () => resolve(file)
    reader.readAsDataURL(file)
  })
}

/**
 * 读取图片文件，优先并尝试上传到服务端以获取绝对路径，失败则回退到返回 dataURL (Base64)
 * @param file 图片文件
 */
export const getImageDataURL = async (file: File): Promise<string> => {
  const slidesStore = useSlidesStore()
  const projectId = slidesStore.projectId

  let uploadFile = file
  // 超过 3MB 且是图片，则询问压缩
  if (file.type.startsWith('image/') && file.size > 3 * 1024 * 1024) {
    if (window.confirm('当前图片超过 3MB，为加快上传并在展示时节省流量，是否自动进行压缩处理？\n\n（点击【确定】进行压缩，推荐；点击【取消】原图直接上传）')) {
      message.success('正在压缩图片...')
      uploadFile = await compressImage(file, 0.7)
    }
  }

  if (projectId) {
    try {
      const res = await api.uploadProjectAsset(projectId, uploadFile)
      if (res.code === 200 && res.data) {
        return res.data
      }
    } catch (e) {
      console.error('上传图片失败, 回退到 Base64 本地转码:', e)
      message.warning('上传失败，已转为临时本地模式')
    }
  } else {
    message.warning('请先保存项目再上传图片以防止内容丢失')
  }

  // 降级本地读取 Base64
  return new Promise(resolve => {
    const reader = new FileReader()
    reader.addEventListener('load', () => {
      resolve(reader.result as string)
    })
    reader.readAsDataURL(uploadFile)
  })
}

/**
 * 判断是否为SVG代码字符串
 * @param text 待验证文本
 */
export const isSVGString = (text: string): boolean => {
  const svgRegex = /<svg[\s\S]*?>[\s\S]*?<\/svg>/i
  if (!svgRegex.test(text)) return false

  try {
    const parser = new DOMParser()
    const doc = parser.parseFromString(text, 'image/svg+xml')
    return doc.documentElement.nodeName === 'svg'
  } 
  catch {
    return false
  }
}

/**
 * SVG代码转文件
 * @param svg SVG代码
 */
export const svg2File = (svg: string): File => {
  const blob = new Blob([svg], { type: 'image/svg+xml' })
  return new File([blob], `${Date.now()}.svg`, { type: 'image/svg+xml' })
}
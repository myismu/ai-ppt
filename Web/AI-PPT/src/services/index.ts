import axios from './axios'
import fetchRequest from './fetch'

// export const SERVER_URL = 'http://localhost:5000'
export const SERVER_URL = (import.meta.env.MODE === 'development') ? '/api' : 'https://server.pptist.cn'

// 静态资源基础路径（走网关 → file-service）
// 开发环境通过 Vite proxy 转发，生产环境直接指向网关地址
export const STATIC_URL = (import.meta.env.MODE === 'development') ? '' : 'https://server.pptist.cn'

interface ImageSearchPayload {
  query: string;
  orientation?: 'landscape' | 'portrait' | 'square' | 'all';
  locale?: 'zh' | 'en';
  order?: 'popular' | 'latest';
  size?: 'large' | 'medium' | 'small';
  image_type?: 'all' | 'photo' | 'illustration' | 'vector';
  page?: number;
  per_page?: number;
}

interface AIPPTOutlinePayload {
  content: string
  language: string
  model: string
}

interface AIPPTPayload {
  content: string
  language: string
  style: string
  model: string
}

interface AIWritingPayload {
  content: string
  command: string
}

export default {
  // 获取模板 JSON 数据
  getTemplateData(path: string): Promise<any> {
    // 如果 path 已经是以 .json 结尾的完整路径，则直接请求
    const url = path.endsWith('.json') ? `${STATIC_URL}${path}` : `${STATIC_URL}/templates/${path}.json`
    return axios.get(url)
  },

  searchImage(body: ImageSearchPayload): Promise<any> {
    return axios.post(`${SERVER_URL}/tools/img_search`, body)
  },

  AIGenerateImage(prompt: string): Promise<any> {
    return axios.post(`${SERVER_URL}/tools/img_generate`, { prompt })
  },

  AIPPT_Outline({
    content,
    language,
    model,
  }: AIPPTOutlinePayload): Promise<any> {
    return fetchRequest(`${SERVER_URL}/tools/aippt_outline`, {
      method: 'POST',
      body: JSON.stringify({
        content,
        language,
        model,
        stream: true,
      }),
    })
  },

  AIPPT({
    content,
    language,
    style,
    model,
  }: AIPPTPayload): Promise<any> {
    return fetchRequest(`${SERVER_URL}/tools/aippt`, {
      method: 'POST',
      body: JSON.stringify({
        content,
        language,
        model,
        style,
        stream: true,
      }),
    })
  },

  AI_Writing({
    content,
    command,
  }: AIWritingPayload): Promise<any> {
    return fetchRequest(`${SERVER_URL}/tools/ai_writing`, {
      method: 'POST',
      body: JSON.stringify({
        content,
        command,
        model: 'GLM-4.5-Flash',
        stream: true,
      }),
    })
  },

  createProject(title: string): Promise<any> {
    return axios.post(`${SERVER_URL}/ppt/create`, { title })
  },

  // 保存 PPT 项目（支持完整字段: 标题、内容、主题、页数）
  saveProject(data: {
    id: number | string
    title?: string
    contentJson?: string
    themeJson?: string
    slideCount?: number
  }): Promise<any> {
    return axios.post(`${SERVER_URL}/ppt/save`, data)
  },

  // 上传项目独占的富媒体资源（图片、视频、音频），持久化存储防止内容丢失
  uploadProjectAsset(projectId: number | string, file: File): Promise<any> {
    const formData = new FormData()
    formData.append('file', file)
    return axios.post(`${SERVER_URL}/file/upload/project-asset?projectId=${projectId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 获取用户项目列表
  listProjects(status?: number): Promise<any> {
    return axios.get(`${SERVER_URL}/ppt/list`, { params: { status } })
  },

  // 获取项目详情（含完整 contentJson 和 themeJson）
  getProjectDetail(id: number | string): Promise<any> {
    return axios.get(`${SERVER_URL}/ppt/detail/${id}`)
  },

  // 删除项目（逻辑删除）
  deleteProject(id: number | string): Promise<any> {
    return axios.delete(`${SERVER_URL}/ppt/delete/${id}`)
  },

  // 恢复已删除的项目
  restoreProject(id: number | string): Promise<any> {
    return axios.put(`${SERVER_URL}/ppt/restore/${id}`)
  },

  // 彻底删除项目（物理删除）
  hardDeleteProject(id: number | string): Promise<any> {
    return axios.delete(`${SERVER_URL}/ppt/hardDelete/${id}`)
  },

  getTemplates(): Promise<any> {
    return axios.get(`${SERVER_URL}/ppt/templates`)
  },
}
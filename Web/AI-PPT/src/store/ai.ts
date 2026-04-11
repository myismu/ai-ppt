import { defineStore } from 'pinia'

export interface AiLog {
  time: string
  type: 'status' | 'error' | 'success' | 'content'
  message: string
}

export const useAiStore = defineStore('ai', {
  state: () => ({
    isGenerating: false,      // 是否正在生成中
    currentProgress: 0,       // 进度百分比 (0-100)
    logs: [] as AiLog[],      // 执行日志列表
    currentContent: '',       // 当前正在蹦出来的纯文本内容 (如大纲)
  }),
  actions: {
    addLog(message: string, type: AiLog['type'] = 'status') {
      this.logs.push({
        time: new Date().toLocaleTimeString().split(' ')[0],
        type,
        message
      })
      
      // 模拟进度条增长
      if (this.currentProgress < 95) {
        this.currentProgress += Math.floor(Math.random() * 5) + 2
      }
    },
    updateContent(delta: string) {
      this.currentContent += delta
    },
    setGenerating(val: boolean) {
      this.isGenerating = val
      if (val) this.reset()
    },
    reset() {
      this.logs = []
      this.currentContent = ''
      this.currentProgress = 0
    }
  }
})

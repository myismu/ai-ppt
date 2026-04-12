<template>
  <div class="ai-engine-container">
    <div class="ai-console-window">
      <!-- 终端头部 (固定) -->
      <div class="terminal-header">
        <div class="dots">
          <span class="dot red" @click="close()"></span>
          <span class="dot yellow"></span>
          <span class="dot green"></span>
        </div>
        <div class="title-area">
          <span class="title-text">AI 智能 PPT 创作引擎 V2.0</span>
        </div>
        <div class="status-indicator" v-if="loading || outlineCreating">
          <span class="status-dot pulsing"></span>
          <span class="status-text">处理中...</span>
        </div>
        <div class="close-btn" @click="close()" title="关闭">
          <i-icon-park-outline:close />
        </div>
      </div>

      <!-- 终端主体内容区 (可滚动) -->
      <div class="terminal-main" ref="scrollContainer">
        
        <!-- 步骤一：任务初始化 -->
        <div v-if="step === 'setup'" class="setup-view">
          <div class="welcome-msg">> SYSTEM: 准备就绪。请输入您的 PPT 主题或核心创意：</div>
          
          <div class="input-wrapper">
            <span class="prompt">$</span>
            <input 
              ref="inputRef"
              v-model="keyword"
              class="terminal-input"
              placeholder="例如：2025年人工智能教育应用白皮书"
              @keydown.enter="createOutline()"
            />
            <FileInput accept=".pdf,.doc,.docx" @change="handleFileUpload">
              <div class="upload-btn" :class="{ 'is-uploading': uploading }" title="上传参考文档（PDF/Word）">
                <i-icon-park-outline:paperclip /> {{ uploading ? '上传中...' : '参考文档' }}
              </div>
            </FileInput>
            <div class="run-btn" @click="createOutline()">
              <i-icon-park-outline:send /> 立即启动
            </div>
          </div>
          
          <div v-if="uploadedFileName" class="uploaded-file-info">
            <i-icon-park-outline:file-doc class="icon" /> 已上传参考资料: <span>{{ uploadedFileName }}</span>
            <i-icon-park-outline:close-small class="remove-btn" @click="removeUploadedFile" title="移除" />
          </div>

          <div class="config-grid">
            <div class="config-row">
              <span class="label">语言:</span>
              <div class="options">
                <span v-for="l in languages" :key="l" 
                  :class="['opt', { active: language === l }]" @click="language = l">{{ l }}</span>
              </div>
            </div>
            <div class="config-row">
              <span class="label">模型:</span>
              <div class="options">
                <span v-for="m in models" :key="m.id" 
                  :class="['opt', { active: modelId === m.id }]" 
                  @click="modelId = m.id">{{ m.provider ? m.provider.toUpperCase() : 'AI引擎' }}<span v-if="m.modelName" style="font-size:10px;opacity:0.6;"> ({{m.modelName}})</span></span>
              </div>
            </div>
            <div class="config-row">
              <span class="label">风格:</span>
              <div class="options">
                <span v-for="s in styles" :key="s" 
                  :class="['opt', { active: style === s }]" @click="style = s">{{ s }}</span>
              </div>
            </div>
          </div>

          <div class="setup-footer-links">
            <div class="recommends-box">
              <div class="section-title">> 启发式主题推荐:</div>
              <div class="recommends">
                <span v-for="r in recommends" :key="r" class="r-tag" @click="setKeyword(r)"># {{ r }}</span>
              </div>
            </div>
            <div v-if="outline" class="history-link" @click="step = 'outline'">
              [ 检测到上次生成的大纲内容，点击此处继续编辑 ]
            </div>
          </div>
        </div>

        <!-- 步骤二：大纲分析与生成 -->
        <div v-if="step === 'outline'" class="outline-view">
          <div class="log-line">> [SYSTEM] 解析任务：{{ keyword }}</div>
          <div class="log-line">> [AI] 正在实时构建逻辑大纲...</div>
          
          <div class="outline-container" v-if="outlineCreating">
            <div class="streaming-text">{{ outline }}<span class="cursor">_</span></div>
          </div>
          
          <div class="editor-container" v-else>
            <div class="success-banner">> 大纲生成完成。您可以对内容进行最后的校对和修改：</div>
            <div class="editor-wrapper">
              <OutlineEditor v-model:value="outline" />
            </div>
          </div>
        </div>

        <!-- 步骤三：视觉模板匹配 -->
        <div v-if="step === 'template'" class="template-view">
          <div class="template-msg">> SYSTEM: 大纲已确认。请为您的演示文稿选择一套视觉风格：</div>
          <div class="template-grid">
            <div class="template-card" 
              v-for="t in templates" :key="t.id"
              :class="{ active: selectedTemplate === t.id }"
              @click="selectedTemplate = t.id"
            >
              <img :src="t.cover" />
              <div class="card-overlay" v-if="selectedTemplate === t.id">
                <i-icon-park-outline:check-one class="check-icon" />
              </div>
              <div class="template-name">{{ t.name }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 固定底部按钮区域 -->
      <div class="terminal-action-bar" v-if="(step === 'outline' || step === 'template') && !outlineCreating && !loading">
        <template v-if="step === 'outline'">
          <div class="next-btn" @click="step = 'template'">确认内容，下一步选择模板</div>
          <div class="back-link" @click="step = 'setup'">放弃并重新开始</div>
        </template>
        <template v-if="step === 'template'">
          <div class="submit-btn" @click="createPPT()">
            <i-icon-park-outline:magic class="icon" /> 开始全自动 AI 视觉排版
          </div>
          <div class="back-link" @click="step = 'outline'">返回修改大纲</div>
        </template>
      </div>

      <!-- 终端页脚：进度条 -->
      <div class="terminal-footer" v-if="outlineCreating || loading">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: aiStore.currentProgress + '%' }"></div>
        </div>
        <div class="progress-info">
          <span>当前任务进度: {{ aiStore.currentProgress }}%</span>
          <span>{{ loading ? '正在进行视觉渲染...' : '正在深度分析内容...' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, useTemplateRef, watch, nextTick } from 'vue'
import { storeToRefs } from 'pinia'
import api from '@/services'
import useAIPPT from '@/hooks/useAIPPT'
import useSlideHandler from '@/hooks/useSlideHandler'
import type { AIPPTSlide } from '@/types/AIPPT'
import type { Slide, SlideTheme } from '@/types/slides'
import message from '@/utils/message'
import { decrypt } from '@/utils/crypto'
import { useMainStore, useSlidesStore, useAiStore } from '@/store'
import OutlineEditor from '@/components/OutlineEditor.vue'
import FileInput from '@/components/FileInput.vue'

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
}>()

const mainStore = useMainStore()
const slidesStore = useSlidesStore()
const aiStore = useAiStore()
const { templates } = storeToRefs(slidesStore)

const { resetSlides, isEmptySlide } = useSlideHandler()
const { AIPPT, presetImgPool, getMdContent } = useAIPPT()

const language = ref('中文')
const style = ref('通用')
const img = ref('ai-search')
const keyword = ref('')
const outline = ref('')
const selectedTemplate = ref('template_1')
const loading = ref(false)
const outlineCreating = ref(false)
const overwrite = ref(true)
const step = ref<'setup' | 'outline' | 'template'>('setup')
const modelId = ref<number>()
const models = ref<any[]>([])
const languages = ref<string[]>(['中文', 'English', '日本語'])
const styles = ref<string[]>(['通用', '学术风', '职场风', '营销风'])
const recommends = ref<string[]>(['2025科技前沿动态', '大数据如何改变世界', '餐饮市场调查研究', 'AIGC在教育领域的应用', '社交媒体品牌营销', '年度工作总结展望'])

const uploadedFileUrl = ref('')
const uploadedFileName = ref('')
const uploading = ref(false)

const scrollContainer = ref<HTMLElement | null>(null)
const inputRef = useTemplateRef<HTMLInputElement>('inputRef')

const handleFileUpload = async (files: FileList) => {
  if (!files.length) return
  const file = files[0]
  uploading.value = true
  
  // 优先使用当前项目ID，如果不存在则使用临时目录 temp_doc
  const targetId = slidesStore.projectId || 'temp_doc'

  try {
    const res = await api.uploadProjectAsset(targetId, file)
    if (res.code === 200 && res.data) {
      // 移除 '/projects/' 前缀，适配后端 AiService 的基础路径拼接
      uploadedFileUrl.value = res.data.replace(/^\/projects\//, '')
      uploadedFileName.value = file.name
      message.success('文档上传成功')
    } else {
      message.error(res.msg || '上传失败')
    }
  } catch (err) {
    console.error(err)
    message.error('上传出错')
  } finally {
    uploading.value = false
  }
}

const removeUploadedFile = async () => {
  if (!uploadedFileUrl.value) return
  
  try {
    // 还原完整的 URL 路径进行删除
    const fullUrl = '/projects/' + uploadedFileUrl.value
    await api.deleteProjectAsset(fullUrl)
  } catch (err) {
    console.error('删除服务器文件失败:', err)
  } finally {
    // 无论后端删除成功与否，前端均清空状态
    uploadedFileUrl.value = ''
    uploadedFileName.value = ''
  }
}

onMounted(() => {
  setTimeout(() => inputRef.value?.focus(), 500)
  fetchCloudTemplates()
  fetchAiModels()
  fetchConfigOptions()
})

const fetchConfigOptions = async () => {
  try {
    const res = await api.getConfigOptions()
    if (res.code === 200 && res.data) {
      if (res.data.language?.length) {
        languages.value = res.data.language
        language.value = languages.value[0]
      }
      if (res.data.style?.length) {
        styles.value = res.data.style
        style.value = styles.value[0]
      }
      if (res.data.recommend?.length) {
        recommends.value = res.data.recommend
      }
    }
  } catch (err) {
    console.error('获取配置选项失败，使用默认值:', err)
  }
}

const fetchAiModels = async () => {
  try {
    const res = await api.getAiModels('text')
    if (res.code === 200 && res.data) {
      models.value = res.data
      if (models.value.length > 0) {
        // Find default or first
        const def = models.value.find((m: any) => m.isDefault === 1)
        modelId.value = def ? def.id : models.value[0].id
      }
    }
  } catch (err) {
    console.error('获取大模型列表失败:', err)
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (scrollContainer.value) {
      scrollContainer.value.scrollTop = scrollContainer.value.scrollHeight
    }
  })
}

watch([() => outline.value, () => aiStore.logs.length], scrollToBottom)
watch(() => outlineCreating.value, (val) => {
  if (!val) setTimeout(scrollToBottom, 100)
})

// 优化：从后端获取模板时，同时记录 fileUrl
const fetchCloudTemplates = async () => {
  try {
    const res = await api.getTemplates()
    if (res.code === 200 && res.data) {
      const cloudTemplates = res.data.map((item: any) => {
        const fileUrl = item.fileUrl || item.file_url || ''
        return {
          id: fileUrl || `cloud_${item.id}`, // 优先使用 fileUrl 作为唯一标识
          name: item.name,
          cover: item.coverUrl,
          fileUrl,
          origin: '后端预置'
        }
      })
      slidesStore.setTemplates(cloudTemplates)
    }
  } catch (err) {
    console.error('获取后端模板失败:', err)
  }
}

const setKeyword = (val: string) => {
  keyword.value = val
  inputRef.value?.focus()
}

const handleOutlineLine = (line: string) => {
  try {
    const data = JSON.parse(line)
    if (data.type === 'status') aiStore.addLog(data.content)
    else if (data.type === 'content') {
      outline.value += data.content
      aiStore.updateContent(data.content)
    }
  } catch (e) {
    outline.value += line
  }
}

const createOutline = async () => {
  if (!keyword.value && !uploadedFileUrl.value) return message.error('请输入主题或上传参考文档')
  outlineCreating.value = true
  aiStore.reset()
  outline.value = '' 
  step.value = 'outline'

  let stream: any
  if (uploadedFileUrl.value) {
    stream = await api.DocumentToPPT({ prompt: keyword.value, fileUrl: uploadedFileUrl.value, modelId: modelId.value })
  } else {
    stream = await api.AIPPT_Outline({ content: keyword.value, language: language.value, modelId: modelId.value })
  }

  if (typeof stream === 'object' && stream.state === -1) {
    outlineCreating.value = false
    step.value = 'setup'
    return message.error('并发过高，请稍后再试')
  }

  const reader = stream.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let lineBuffer = ''
  
  const readStream = () => {
    reader.read().then(({ done, value }: any) => {
      if (done) {
        if (lineBuffer.trim()) handleOutlineLine(lineBuffer.trim())
        outline.value = getMdContent(outline.value).replace(/<!--[\s\S]*?-->/g, '')
        outlineCreating.value = false
        return
      }
      lineBuffer += decoder.decode(value, { stream: true })
      const lines = lineBuffer.split('\n')
      lineBuffer = lines.pop() || ''
      for (const line of lines) { if (line.trim()) handleOutlineLine(line.trim()) }
      readStream()
    })
  }
  readStream()
}

const handlePptLine = (line: string, templateSlides: Slide[], styleKey: string) => {
  try {
    const data = JSON.parse(line)
    if (data.type === 'status') aiStore.addLog(data.content)
    else if (data.data) {
      // 传递 fetchPath (即模板文件路径) 作为 styleKey，确保换布局时可以正确请求模板数据
      AIPPT(templateSlides, [data], styleKey, img.value)
    }
  } catch (e) {}
}

const createPPT = async () => {

  loading.value = true
  mainStore.setAIPPTDialogState('running')
  aiStore.setGenerating(true)
  if (overwrite.value) resetSlides()

  // 1. 获取当前选中的模板对象
  const currentTemplate = templates.value.find(t => t.id === selectedTemplate.value)
  console.log('AI-PPT: selectedId =', selectedTemplate.value, 'foundTemplate =', currentTemplate)
  // 2. 核心修复点：优先使用后端返回的 fileUrl（路径），如果没有则兜底使用 ID
  const fetchPath = currentTemplate?.fileUrl || (currentTemplate as any)?.file_url || selectedTemplate.value
  console.log('AI-PPT: final fetchPath =', fetchPath)

  const stream = await api.AIPPT({ content: outline.value, language: language.value, style: style.value, modelId: modelId.value })
  const templateData = await api.getTemplateData(fetchPath)
  const templateSlides = templateData.slides

  const reader = stream.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let lineBuffer = ''
  
  const readStream = () => {
    reader.read().then(({ done, value }: any) => {
      if (done) {
        if (lineBuffer.trim()) handlePptLine(lineBuffer.trim(), templateSlides, fetchPath)
        loading.value = false
        setTimeout(() => {
          aiStore.setGenerating(false)
          mainStore.setAIPPTDialogState(false)
        }, 2000)
        return
      }
      lineBuffer += decoder.decode(value, { stream: true })
      const lines = lineBuffer.split('\n')
      lineBuffer = lines.pop() || ''
      for (const line of lines) { if (line.trim()) handlePptLine(line.trim(), templateSlides, fetchPath) }
      readStream()
    })
  }
  readStream()
}

const close = () => mainStore.setAIPPTDialogState(false)
</script>

<style lang="scss" scoped>
.ai-engine-container {
  margin: -20px;
  padding: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  background: transparent;
}

.ai-console-window {
  width: 1000px;
  height: 680px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(25px);
  border-radius: 20px;
  box-shadow: 0 50px 120px rgba(0, 0, 0, 0.18), 0 0 0 1px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: 'Fira Code', 'Courier New', monospace;
  position: relative;
}

.terminal-header {
  height: 48px;
  background: rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  padding: 0 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
  
  .dots {
    display: flex; gap: 12px;
    .dot {
      width: 13px; height: 13px; border-radius: 50%; cursor: pointer;
      &.red { background: #ff5f56; }
      &.yellow { background: #ffbd2e; }
      &.green { background: #27c93f; }
    }
  }
  
  .title-area {
    flex: 1; text-align: center;
    .title-text { font-size: 14px; color: #666; font-weight: 800; letter-spacing: 1.5px; }
  }

  .status-indicator {
    display: flex; align-items: center; gap: 10px; color: $themeColor;
    .status-dot { width: 10px; height: 10px; background: $themeColor; border-radius: 50%; }
    .status-text { font-size: 11px; font-weight: 900; }
  }

  .close-btn {
    width: 30px; height: 30px; border-radius: 8px;
    display: flex; align-items: center; justify-content: center;
    cursor: pointer; color: #999; font-size: 16px;
    transition: all 0.2s ease;
    margin-left: 12px;
    &:hover { background: rgba(0, 0, 0, 0.08); color: #333; }
  }
}

.terminal-main {
  flex: 1;
  padding: 35px;
  overflow-y: auto;
  scroll-behavior: smooth;
  &::-webkit-scrollbar { width: 5px; }
  &::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.08); border-radius: 10px; }
}

.terminal-action-bar {
  height: 80px;
  background: rgba(255, 255, 255, 0.95);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  padding: 0 35px;
  gap: 30px;
  flex-shrink: 0;
  z-index: 10;

  .next-btn, .submit-btn {
    background: $themeColor; color: #fff; padding: 14px 35px; border-radius: 12px;
    cursor: pointer; font-weight: 800; font-size: 15px; 
    box-shadow: 0 8px 20px rgba($themeColor, 0.2);
    transition: transform 0.2s;
    &:hover { transform: translateY(-1px); background: $themeHoverColor; }
  }
  
  .submit-btn {
    background: linear-gradient(90deg, #33bcfc, #d897fd);
    box-shadow: 0 10px 25px rgba(51, 188, 252, 0.25);
    display: flex; align-items: center; gap: 10px;
  }

  .back-link {
    font-size: 13px; color: #888; cursor: pointer; font-weight: 600;
    text-decoration: underline;
    &:hover { color: #444; }
  }
}

.terminal-footer {
  height: 70px;
  padding: 0 35px;
  background: rgba(0,0,0,0.03);
  display: flex;
  flex-direction: column;
  justify-content: center;
  flex-shrink: 0;
  
  .progress-bar { height: 8px; background: rgba(0,0,0,0.06); border-radius: 4px; overflow: hidden; margin-bottom: 8px; }
  .progress-fill { height: 100%; background: linear-gradient(90deg, #33bcfc, #d897fd); transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1); }
  .progress-info { display: flex; justify-content: space-between; font-size: 11px; color: #999; font-weight: 800; text-transform: uppercase; }
}

.setup-view {
  .welcome-msg { color: #444; margin-bottom: 25px; font-size: 16px; font-weight: 500; }
  .input-wrapper {
    display: flex; align-items: center; gap: 20px; margin-bottom: 15px;
    .prompt { color: $themeColor; font-weight: 900; font-size: 24px; }
    .terminal-input {
      flex: 1; background: transparent; border: none; outline: none;
      font-size: 22px; color: #222; font-weight: 700;
      border-bottom: 3px solid rgba($themeColor, 0.1);
      padding: 10px 0;
      &:focus { border-bottom-color: $themeColor; }
    }
    .upload-btn {
      background: rgba($themeColor, 0.1); color: $themeColor; padding: 12px 20px; border-radius: 10px;
      cursor: pointer; font-weight: 700; font-size: 14px; display: flex; align-items: center; gap: 6px;
      transition: all 0.2s;
      &:hover { background: rgba($themeColor, 0.2); }
      &.is-uploading { opacity: 0.7; cursor: not-allowed; }
    }
    .run-btn {
      background: $themeColor; color: #fff; padding: 12px 28px; border-radius: 10px;
      cursor: pointer; font-weight: 800; font-size: 15px; display: flex; align-items: center; gap: 8px;
      &:hover { background: $themeHoverColor; }
    }
  }
  .uploaded-file-info {
    display: flex; align-items: center; gap: 8px; font-size: 13px; color: #666; margin-bottom: 30px;
    background: #f8f9fa; padding: 10px 15px; border-radius: 8px; border: 1px solid #eee; width: fit-content;
    .icon { color: $themeColor; font-size: 16px; }
    span { font-weight: 600; color: #333; }
    .remove-btn { cursor: pointer; color: #999; margin-left: 10px; font-size: 16px; transition: color 0.2s; &:hover { color: #ff5f56; } }
  }
  .config-grid {
    display: flex; flex-direction: column; gap: 20px; margin-bottom: 40px;
    .config-row {
      display: flex; align-items: center; gap: 30px;
      .label { width: 70px; color: #888; font-size: 14px; font-weight: 600; }
      .options {
        display: flex; gap: 15px;
        .opt {
          padding: 6px 18px; border-radius: 8px; font-size: 13px; color: #555;
          border: 1px solid #ddd; cursor: pointer; transition: all 0.2s;
          &.active { background: $themeColor; color: #fff; border-color: $themeColor; box-shadow: 0 4px 10px rgba($themeColor, 0.3); }
          &:hover:not(.active) { background: #eee; }
        }
      }
    }
  }
  .setup-footer-links {
    display: flex; flex-direction: column; gap: 25px;
    .history-link { font-size: 13px; color: $themeColor; font-weight: 800; text-decoration: underline; cursor: pointer; padding: 10px 0; }
  }
  .recommends-box {
    .section-title { font-size: 13px; color: #888; margin-bottom: 15px; font-weight: 600; }
    .recommends {
      display: flex; flex-wrap: wrap; gap: 12px;
      .r-tag { font-size: 13px; color: $themeColor; background: rgba($themeColor, 0.06); padding: 6px 16px; border-radius: 20px; cursor: pointer; border: 1px solid transparent; &:hover { background: rgba($themeColor, 0.12); border-color: rgba($themeColor, 0.2); } }
    }
  }
}

.outline-view {
  .log-line { color: $themeColor; font-weight: 800; margin-bottom: 10px; font-size: 14px; }
  .streaming-text { white-space: pre-wrap; color: #333; line-height: 1.9; font-size: 15px; }
  .cursor { display: inline-block; width: 10px; height: 18px; background: $themeColor; margin-left: 5px; animation: blink 0.8s infinite; }
  .editor-wrapper { margin-bottom: 20px; background: #fff; border-radius: 16px; padding: 15px; border: 1px solid #eee; box-shadow: 0 10px 30px rgba(0,0,0,0.03); }
}

.template-view {
  .template-msg { color: #333; margin-bottom: 25px; font-weight: 800; font-size: 16px; }
  .template-grid {
    display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; padding-bottom: 20px;
    .template-card {
      border-radius: 14px; overflow: hidden; border: 2px solid #eee; position: relative; cursor: pointer; transition: all 0.3s;
      img { width: 100%; height: 150px; object-fit: cover; }
      .template-name { padding: 12px; font-size: 13px; text-align: center; color: #444; background: #fff; font-weight: 600; }
      &.active { border-color: $themeColor; transform: translateY(-5px); box-shadow: 0 15px 35px rgba($themeColor, 0.15); .template-name { color: $themeColor; } }
      .card-overlay {
        position: absolute; inset: 0; background: rgba($themeColor, 0.15); display: flex; align-items: center; justify-content: center;
        .check-icon { font-size: 40px; color: $themeColor; filter: drop-shadow(0 2px 10px rgba(255,255,255,0.5)); }
      }
    }
  }
}

@keyframes blink { 0%, 100% { opacity: 0; } 50% { opacity: 1; } }
@keyframes pulse {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba($themeColor, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 10px rgba($themeColor, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba($themeColor, 0); }
}

@media screen and (width <= 800px) {
  .ai-console-window { width: 95vw; height: 90vh; }
  .template-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>

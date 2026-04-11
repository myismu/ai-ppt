<template>
  <Teleport to="body">
    <transition name="fade">
      <div v-if="aiStore.isGenerating" class="ai-console-overlay">
        <div class="ai-console-window">
          <div class="header">
            <div class="dots">
              <span class="dot red"></span>
              <span class="dot yellow"></span>
              <span class="dot green"></span>
            </div>
            <span class="title">AI 智能分析引擎</span>
            <div class="header-right">
              <span class="status-dot pulsing"></span>
              RUNNING
            </div>
          </div>
          
          <div class="terminal-body" ref="logContainer">
            <div v-for="(log, index) in aiStore.logs" :key="index" class="log-line">
              <span class="time">[{{ log.time }}]</span>
              <span class="msg" :class="log.type">> {{ log.message }}</span>
            </div>
            
            <!-- 实时打字机预览区 -->
            <div v-if="aiStore.currentContent" class="content-preview">
              <div class="preview-header">LIVE CONTENT PREVIEW:</div>
              <div class="preview-text">
                {{ aiStore.currentContent }}<span class="cursor">_</span>
              </div>
            </div>
          </div>
          
          <div class="footer-progress">
            <div class="progress-info">
              <span>GENERATING SYSTEM...</span>
              <span>{{ aiStore.currentProgress }}%</span>
            </div>
            <div class="progress-track">
              <div class="progress-bar" :style="{ width: aiStore.currentProgress + '%' }"></div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, onUpdated, watch } from 'vue'
import { useAiStore } from '@/store/ai'

const aiStore = useAiStore()
const logContainer = ref<HTMLElement | null>(null)

// 自动滚动到底部
const scrollToBottom = () => {
  if (logContainer.value) {
    logContainer.value.scrollTop = logContainer.value.scrollHeight
  }
}

watch(() => aiStore.logs.length, () => {
  setTimeout(scrollToBottom, 50)
})

watch(() => aiStore.currentContent, () => {
  setTimeout(scrollToBottom, 50)
})
</script>

<style scoped lang="scss">
.ai-console-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
}

.ai-console-window {
  width: 700px;
  height: 450px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  box-shadow: 0 30px 60px rgba(0, 0, 0, 0.1), 0 0 0 1px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: 'Fira Code', 'Courier New', monospace;
}

.header {
  height: 44px;
  background: rgba(0, 0, 0, 0.02);
  display: flex;
  align-items: center;
  padding: 0 18px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  
  .dots {
    display: flex;
    gap: 8px;
    .dot {
      width: 12px;
      height: 12px;
      border-radius: 50%;
      &.red { background: #ff5f56; }
      &.yellow { background: #ffbd2e; }
      &.green { background: #27c93f; }
    }
  }
  
  .title {
    flex: 1;
    text-align: center;
    color: #666;
    font-size: 13px;
    font-weight: 600;
    letter-spacing: 0.5px;
  }
  
  .header-right {
    color: $themeColor;
    font-size: 11px;
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
  }
}

.terminal-body {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  font-size: 13.5px;
  line-height: 1.7;
  
  &::-webkit-scrollbar {
    width: 5px;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.1);
    border-radius: 10px;
  }

  .log-line {
    margin-bottom: 8px;
    .time {
      color: #aaa;
      margin-right: 12px;
      font-size: 12px;
    }
    .msg {
      &.status { color: #333; }
      &.content { color: #666; }
      &.error { color: #d73a49; }
      &.success { color: #28a745; font-weight: bold; }
    }
  }
}

.content-preview {
  margin-top: 24px;
  padding: 18px;
  background: rgba($themeColor, 0.03);
  border-left: 3px solid $themeColor;
  border-radius: 4px;
  
  .preview-header {
    font-size: 11px;
    color: $themeColor;
    margin-bottom: 10px;
    font-weight: bold;
    text-transform: uppercase;
  }
  
  .preview-text {
    color: #444;
    white-space: pre-wrap;
    word-break: break-all;
  }
}

.footer-progress {
  padding: 20px 24px 24px;
  background: rgba(0, 0, 0, 0.01);
  
  .progress-info {
    display: flex;
    justify-content: space-between;
    color: #999;
    font-size: 11px;
    margin-bottom: 10px;
    font-weight: 600;
  }
  
  .progress-track {
    height: 8px;
    background: rgba(0, 0, 0, 0.05);
    border-radius: 4px;
    overflow: hidden;
  }
  
  .progress-bar {
    height: 100%;
    background: linear-gradient(90deg, #33bcfc, #d897fd);
    transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }
}

.cursor {
  display: inline-block;
  width: 2px;
  height: 16px;
  background: $themeColor;
  margin-left: 4px;
  vertical-align: middle;
  animation: blink 0.8s infinite;
}

.status-dot.pulsing {
  width: 8px;
  height: 8px;
  background: $themeColor;
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 0; }
  50% { opacity: 1; }
}

@keyframes pulse {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(#33bcfc, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 8px rgba(#33bcfc, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(#33bcfc, 0); }
}

.fade-enter-active, .fade-leave-active { transition: all 0.4s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: scale(0.95); }
</style>

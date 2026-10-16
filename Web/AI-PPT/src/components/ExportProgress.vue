<template>
  <!-- 终端风格导出进度面板 -->
  <div class="export-progress-overlay" v-if="exporting">
    <div class="export-terminal">
      <!-- 终端头部（macOS 三点风格） -->
      <div class="terminal-header">
        <div class="dots">
          <span class="dot red"></span>
          <span class="dot yellow"></span>
          <span class="dot green"></span>
        </div>
        <div class="title-area">
          <span class="title-text">📦 PPT 导出引擎</span>
        </div>
        <div class="status-indicator">
          <span class="status-dot pulsing"></span>
          <span class="status-text">{{ progress >= 100 ? '已完成' : '处理中...' }}</span>
        </div>
      </div>

      <!-- 终端日志区 -->
      <div class="terminal-body" ref="logContainer">
        <div class="log-line system">> [SYSTEM] 初始化导出引擎...</div>
        <div class="log-line" v-for="(log, i) in logs" :key="i" :class="{ success: log.startsWith('✓'), info: log.startsWith('>') }">
          {{ log }}
        </div>
        <span class="cursor" v-if="progress < 100">_</span>
      </div>

      <!-- 底部进度条 -->
      <div class="terminal-footer">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progress + '%' }"></div>
        </div>
        <div class="progress-info">
          <span>导出进度: {{ progress }}%</span>
          <span>{{ progress >= 100 ? '🎉 文件即将下载' : `正在渲染幻灯片...` }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { watch, ref, useTemplateRef, nextTick } from 'vue'

const props = withDefaults(defineProps<{
  exporting: boolean
  progress: number
  logs: string[]
}>(), {
  exporting: false,
  progress: 0,
  logs: () => [],
})

const logContainer = useTemplateRef<HTMLElement>('logContainer')

// 日志自动滚动到底部
watch(() => props.logs.length, () => {
  nextTick(() => {
    if (logContainer.value) {
      logContainer.value.scrollTop = logContainer.value.scrollHeight
    }
  })
})
</script>

<style lang="scss" scoped>
.export-progress-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.3s ease;
}

.export-terminal {
  width: 560px;
  max-height: 420px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(25px);
  border-radius: 20px;
  box-shadow: 0 50px 120px rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: 'Fira Code', 'Courier New', monospace;
  animation: pop 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.terminal-header {
  height: 44px;
  background: rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  flex-shrink: 0;

  .dots {
    display: flex; gap: 10px;
    .dot {
      width: 12px; height: 12px; border-radius: 50%;
      &.red { background: #ff5f56; }
      &.yellow { background: #ffbd2e; }
      &.green { background: #27c93f; }
    }
  }

  .title-area {
    flex: 1; text-align: center;
    .title-text { font-size: 13px; color: #666; font-weight: 800; letter-spacing: 1px; }
  }

  .status-indicator {
    display: flex; align-items: center; gap: 8px; color: $themeColor;
    .status-dot { width: 8px; height: 8px; background: $themeColor; border-radius: 50%; }
    .status-text { font-size: 11px; font-weight: 900; }
  }
}

.terminal-body {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  scroll-behavior: smooth;
  min-height: 180px;
  max-height: 240px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.06); border-radius: 10px; }

  .log-line {
    font-size: 13px;
    line-height: 1.9;
    color: #555;
    font-weight: 500;

    &.system { color: $themeColor; font-weight: 700; }
    &.success { color: #10b981; font-weight: 700; }
    &.info { color: #64748b; }
  }

  .cursor {
    display: inline-block;
    width: 8px; height: 16px;
    background: $themeColor;
    margin-left: 4px;
    animation: blink 0.8s infinite;
    vertical-align: text-bottom;
  }
}

.terminal-footer {
  height: 60px;
  padding: 0 20px;
  background: rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
  justify-content: center;
  flex-shrink: 0;

  .progress-bar {
    height: 6px;
    background: rgba(0, 0, 0, 0.06);
    border-radius: 3px;
    overflow: hidden;
    margin-bottom: 6px;
  }

  .progress-fill {
    height: 100%;
    background: linear-gradient(90deg, #33bcfc, #d897fd);
    transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    border-radius: 3px;
  }

  .progress-info {
    display: flex;
    justify-content: space-between;
    font-size: 11px;
    color: #999;
    font-weight: 700;
  }
}

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes pop { from { opacity: 0; transform: scale(0.9) translateY(20px); } to { opacity: 1; transform: scale(1) translateY(0); } }
@keyframes blink { 0%, 100% { opacity: 0; } 50% { opacity: 1; } }
@keyframes pulse {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba($themeColor, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 8px rgba($themeColor, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba($themeColor, 0); }
}

.pulsing { animation: pulse 2s infinite; }
</style>

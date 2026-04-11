<template>
  <div class="pptist-editor-v2">
    <!-- 背景层：微妙的渐变或网格，增强 AI 科技感 -->
    <div class="editor-bg-mesh"></div>
    
    <div class="editor-main-container">
      <EditorHeader class="layout-header-v2" />
      
      <div class="layout-content-v2">
        <aside class="layout-content-left-v2">
          <Thumbnails />
        </aside>
        
        <main class="layout-content-center-v2">
          <div class="center-content-wrapper">
            <CanvasTool class="center-top-v2" />
            <div class="canvas-viewport" :style="{ height: `calc(100% - ${remarkHeight + 48}px)` }">
              <Canvas />
            </div>
            <Remark
              class="center-bottom-v2" 
              v-model:height="remarkHeight" 
              :style="{ height: `${remarkHeight}px` }"
            />
          </div>
        </main>
        
        <aside class="layout-content-right-v2">
          <Toolbar />
        </aside>
      </div>
    </div>
  </div>

  <!-- 浮动面板 -->
  <SelectPanel v-if="showSelectPanel" />
  <SearchPanel v-if="showSearchPanel" />
  <NotesPanel v-if="showNotesPanel" />
  <MarkupPanel v-if="showMarkupPanel" />
  <SymbolPanel v-if="showSymbolPanel" />
  <ImageLibPanel v-if="showImageLibPanel" />

  <Modal
    :visible="!!dialogForExport" 
    :width="680"
    @closed="closeExportDialog()"
  >
    <ExportDialog />
  </Modal>

  <Modal
    :visible="!!showAIPPTDialog" 
    :width="1000"
    :closeOnClickMask="false"
    :closeOnEsc="false"
    closeButton
    :wrapStyle="{ opacity: showAIPPTDialog === 'running' ? 0 : 1, transition: 'opacity 0.5s' }"
    @closed="closeAIPPTDialog()"
  >
    <AIPPTDialog />
  </Modal>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useMainStore } from '@/store'
import useGlobalHotkey from '@/hooks/useGlobalHotkey'
import usePasteEvent from '@/hooks/usePasteEvent'

import EditorHeader from './EditorHeader/index.vue'
import Canvas from './Canvas/index.vue'
import CanvasTool from './CanvasTool/index.vue'
import Thumbnails from './Thumbnails/index.vue'
import Toolbar from './Toolbar/index.vue'
import Remark from './Remark/index.vue'
import ExportDialog from './ExportDialog/index.vue'
import SelectPanel from './SelectPanel.vue'
import SearchPanel from './SearchPanel.vue'
import NotesPanel from './NotesPanel.vue'
import SymbolPanel from './SymbolPanel.vue'
import MarkupPanel from './MarkupPanel.vue'
import ImageLibPanel from './ImageLibPanel.vue'
import AIPPTDialog from './AIPPTDialog.vue'
import Modal from '@/components/Modal.vue'

const mainStore = useMainStore()
const {
  dialogForExport,
  showSelectPanel,
  showSearchPanel,
  showNotesPanel,
  showSymbolPanel,
  showMarkupPanel,
  showImageLibPanel,
  showAIPPTDialog,
} = storeToRefs(mainStore)

const closeExportDialog = () => mainStore.setDialogForExport('')
const closeAIPPTDialog = () => mainStore.setAIPPTDialogState(false)

const remarkHeight = ref(40)

useGlobalHotkey()
usePasteEvent()
</script>

<style lang="scss" scoped>
.pptist-editor-v2 {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  position: relative;
  background: #f4f7fa;
  color: #1e293b;
  font-family: 'Inter', 'SF Pro Display', -apple-system, sans-serif;
}

.editor-bg-mesh {
  position: absolute;
  inset: 0;
  z-index: 0;
  background-image: 
    radial-gradient(at 0% 0%, rgba(79, 70, 229, 0.03) 0, transparent 50%), 
    radial-gradient(at 100% 100%, rgba(6, 182, 212, 0.03) 0, transparent 50%);
  pointer-events: none;
}

.editor-main-container {
  position: relative;
  z-index: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.layout-header-v2 {
  height: 56px;
  flex-shrink: 0;
  z-index: 100;
}

.layout-content-v2 {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding: 12px;
  gap: 12px;
}

.layout-content-left-v2 {
  width: 200px;
  height: 100%;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  overflow: hidden;
}

.layout-content-center-v2 {
  flex: 1;
  height: 100%;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.center-content-wrapper {
  height: 100%;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: inset 0 0 40px rgba(0, 0, 0, 0.01);
}

.center-top-v2 {
  height: 48px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.03);
}

.canvas-viewport {
  flex: 1;
  position: relative;
  overflow: hidden;
  padding: 0;
  display: flex;
  flex-direction: column;
  background: rgba(0, 0, 0, 0.02);
  
  ::v-deep(.canvas) {
    background-color: transparent !important;
  }
}

.center-bottom-v2 {
  background: rgba(255, 255, 255, 0.8);
  border-top: 1px solid rgba(0, 0, 0, 0.03);
}

.layout-content-right-v2 {
  width: 300px;
  height: 100%;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  overflow: hidden;
}
</style>
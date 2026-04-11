<template>
  <div class="canvas-tool-v2">
    <div class="tool-left">
      <div class="history-group">
        <span class="tool-item" :class="{ 'disable': !canUndo }" v-tooltip="'撤销'" @click="undo()">
          <i-icon-park-outline:back />
        </span>
        <span class="tool-item" :class="{ 'disable': !canRedo }" v-tooltip="'重做'" @click="redo()">
          <i-icon-park-outline:next />
        </span>
      </div>
      
      <Divider type="vertical" />
      
      <div class="view-group">
        <span class="tool-item" :class="{ 'active': showNotesPanel }" v-tooltip="'批注'" @click="toggleNotesPanel()">
          <i-icon-park-outline:comment />
        </span>
        <span class="tool-item" :class="{ 'active': showSelectPanel }" v-tooltip="'图层'" @click="toggleSelectPanel()">
          <i-icon-park-outline:move-one />
        </span>
      </div>
    </div>

    <div class="tool-center">
      <div class="insert-bar">
        <div class="insert-pill" @click="drawText()">
          <i-icon-park-outline:font-size />
          <span>文本</span>
        </div>
        
        <Popover trigger="click" v-model:value="shapePoolVisible" :offset="12">
          <template #content>
            <ShapePool @select="shape => drawShape(shape)" />
          </template>
          <div class="insert-pill">
            <i-icon-park-outline:graphic-design />
            <span>形状</span>
          </div>
        </Popover>

        <FileInput @change="files => insertImageElement(files)">
          <div class="insert-pill">
            <i-icon-park-outline:picture />
            <span>图片</span>
          </div>
        </FileInput>

        <Popover trigger="click" v-model:value="linePoolVisible" :offset="12">
          <template #content>
            <LinePool @select="line => drawLine(line)" />
          </template>
          <div class="insert-pill">
            <i-icon-park-outline:connection />
            <span>线条</span>
          </div>
        </Popover>

        <div class="insert-pill" @click="latexEditorVisible = true">
          <i-icon-park-outline:formula />
          <span>公式</span>
        </div>

        <Popover trigger="click" v-model:value="chartPoolVisible" :offset="12">
          <template #content>
            <ChartPool @select="chart => { createChartElement(chart); chartPoolVisible = false }" />
          </template>
          <div class="insert-pill">
            <i-icon-park-outline:chart-proportion />
            <span>图表</span>
          </div>
        </Popover>
      </div>
    </div>

    <div class="tool-right">
      <div class="zoom-pill">
        <span class="zoom-btn" @click="scaleCanvas('-')"><i-icon-park-outline:minus /></span>
        <Popover trigger="click" v-model:value="canvasScaleVisible">
          <template #content>
            <div class="scale-menu">
              <PopoverMenuItem v-for="item in canvasScalePresetList" :key="item" @click="applyCanvasPresetScale(item)">{{item}}%</PopoverMenuItem>
              <Divider :margin="4" />
              <PopoverMenuItem @click="resetCanvas(); canvasScaleVisible = false">自适应</PopoverMenuItem>
            </div>
          </template>
          <span class="zoom-text">{{ canvasScalePercentage }}</span>
        </Popover>
        <span class="zoom-btn" @click="scaleCanvas('+')"><i-icon-park-outline:plus /></span>
      </div>
      <span class="tool-item" v-tooltip="'全屏适应'" @click="resetCanvas()">
        <i-icon-park-outline:full-screen />
      </span>
    </div>

    <Modal v-model:visible="latexEditorVisible" :width="880">
      <LaTeXEditor @close="latexEditorVisible = false" @update="data => { createLatexElement(data); latexEditorVisible = false }" />
    </Modal>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useMainStore, useSnapshotStore } from '@/store'
import { getImageDataURL } from '@/utils/image'
import type { ShapePoolItem } from '@/configs/shapes'
import type { LinePoolItem } from '@/configs/lines'
import useScaleCanvas from '@/hooks/useScaleCanvas'
import useHistorySnapshot from '@/hooks/useHistorySnapshot'
import useCreateElement from '@/hooks/useCreateElement'

import ShapePool from './ShapePool.vue'
import LinePool from './LinePool.vue'
import ChartPool from './ChartPool.vue'
import TableGenerator from './TableGenerator.vue'
import MediaInput from './MediaInput.vue'
import LaTeXEditor from '@/components/LaTeXEditor/index.vue'
import FileInput from '@/components/FileInput.vue'
import Modal from '@/components/Modal.vue'
import Divider from '@/components/Divider.vue'
import Popover from '@/components/Popover.vue'
import PopoverMenuItem from '@/components/PopoverMenuItem.vue'

const mainStore = useMainStore()
const { creatingElement, creatingCustomShape, showSelectPanel, showSearchPanel, showNotesPanel, showSymbolPanel } = storeToRefs(mainStore)
const { canUndo, canRedo } = storeToRefs(useSnapshotStore())
const { redo, undo } = useHistorySnapshot()
const { scaleCanvas, setCanvasScalePercentage, resetCanvas, canvasScalePercentage } = useScaleCanvas()

const canvasScalePresetList = [200, 150, 125, 100, 75, 50]
const canvasScaleVisible = ref(false)

const applyCanvasPresetScale = (value: number) => {
  setCanvasScalePercentage(value)
  canvasScaleVisible.value = false
}

const { createImageElement, createChartElement, createTableElement, createLatexElement, createVideoElement, createAudioElement } = useCreateElement()

const insertImageElement = (files: FileList) => {
  const imageFile = files[0]
  if (!imageFile) return
  getImageDataURL(imageFile).then(dataURL => createImageElement(dataURL))
}

const shapePoolVisible = ref(false)
const linePoolVisible = ref(false)
const chartPoolVisible = ref(false)
const latexEditorVisible = ref(false)

const drawText = (vertical = false) => mainStore.setCreatingElement({ type: 'text', vertical })
const drawShape = (shape: ShapePoolItem) => {
  mainStore.setCreatingElement({ type: 'shape', data: shape })
  shapePoolVisible.value = false
}
const drawLine = (line: LinePoolItem) => {
  mainStore.setCreatingElement({ type: 'line', data: line })
  linePoolVisible.value = false
}

const toggleSelectPanel = () => mainStore.setSelectPanelState(!showSelectPanel.value)
const toggleNotesPanel = () => mainStore.setNotesPanelState(!showNotesPanel.value)
</script>

<style lang="scss" scoped>
.canvas-tool-v2 {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: transparent;
  user-select: none;
}

.tool-left, .tool-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.tool-right { justify-content: flex-end; }

.history-group, .view-group {
  display: flex;
  gap: 2px;
}

.tool-item {
  width: 32px; height: 32px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  color: #64748b; cursor: pointer; transition: all 0.2s;
  font-size: 16px;
  &:hover:not(.disable) { background: rgba(0,0,0,0.05); color: #1e293b; }
  &.active { background: rgba($themeColor, 0.1); color: $themeColor; }
  &.disable { opacity: 0.3; cursor: not-allowed; }
}

.tool-center {
  flex: 2;
  display: flex;
  justify-content: center;
}

.insert-bar {
  display: flex;
  align-items: center;
  background: rgba(0,0,0,0.03);
  padding: 4px;
  border-radius: 12px;
  gap: 4px;
}

.insert-pill {
  height: 32px; padding: 0 12px; border-radius: 8px;
  display: flex; align-items: center; gap: 6px;
  color: #475569; font-size: 13px; font-weight: 700;
  cursor: pointer; transition: all 0.2s;
  &:hover { background: #fff; color: $themeColor; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
  i { font-size: 15px; }
}

.zoom-pill {
  display: flex; align-items: center; background: rgba(0,0,0,0.03); border-radius: 10px; padding: 2px;
  .zoom-btn {
    width: 28px; height: 28px; display: flex; align-items: center; justify-content: center;
    border-radius: 6px; cursor: pointer; color: #64748b; transition: all 0.2s;
    &:hover { background: #fff; color: #1e293b; }
  }
  .zoom-text {
    width: 44px; text-align: center; font-size: 12px; font-weight: 800; color: #475569; cursor: pointer;
  }
}

.scale-menu {
  width: 100px; padding: 4px;
}
</style>
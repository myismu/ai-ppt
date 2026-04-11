<template>
  <div 
    class="thumbnails-v2"
    @mousedown="() => setThumbnailsFocus(true)"
    v-click-outside="() => setThumbnailsFocus(false)"
  >
    <div class="thumbnails-header">
      <div class="add-slide-v2" @click="createSlide()">
        <i-icon-park-outline:plus class="icon" />
        <span>添加页面</span>
      </div>
      <Popover trigger="click" placement="bottom-start" v-model:value="presetLayoutPopoverVisible" center>
        <template #content>
          <Templates 
            @select="slide => { createSlideByTemplate(slide); presetLayoutPopoverVisible = false }"
            @selectAll="({ slides, theme }) => { insertAllTemplates({ slides, theme }); presetLayoutPopoverVisible = false }"
          />
        </template>
        <div class="layout-trigger"><i-icon-park-outline:down /></div>
      </Popover>
    </div>

    <Draggable 
      class="thumbnail-list-v2"
      ref="thumbnailsRef"
      :modelValue="slides"
      :animation="200"
      :scroll="true"
      :scrollSensitivity="50"
      :disabled="!!editingSectionId"
      @end="handleDragEnd"
      itemKey="id"
      v-contextmenu="contextmenusThumbnails"
    >
      <template #item="{ element, index }">
        <div class="thumbnail-group">
          <!-- 章节标题样式优化 -->
          <div class="section-divider"
            :data-section-id="element?.sectionTag?.id || ''"
            v-if="element.sectionTag || (hasSection && index === 0)" 
            v-contextmenu="contextmenusSection"
            @dblclick="() => editSection(element?.sectionTag?.id || '')"
          >
            <div class="divider-line"></div>
            <div class="section-pill">
              <input 
                :id="`section-title-input-${element?.sectionTag?.id || 'default'}`" 
                type="text"
                :value="element?.sectionTag ? (element?.sectionTag?.title || '') : '默认分节'"
                placeholder="输入节名称"
                @blur="$event => saveSection($event)"
                @keydown.enter.stop="$event => saveSection($event)"
                v-if="editingSectionId === element?.sectionTag?.id || (index === 0 && editingSectionId === 'default')"
              >
              <span class="pill-text" v-else>
                {{ element?.sectionTag ? (element?.sectionTag?.title || '无标题节') : '默认节' }}
              </span>
            </div>
          </div>

          <div
            class="thumbnail-card"
            :class="{
              'active': slideIndex === index,
              'selected': selectedSlidesIndex.includes(index),
            }"
            @mousedown="$event => handleClickSlideThumbnail($event, index)"
            v-contextmenu="contextmenusThumbnailItem"
          >
            <div class="card-index">{{ index + 1 }}</div>
            <div class="card-preview">
              <ThumbnailSlide class="thumbnail" :slide="element" :size="140" :visible="index < slidesLoadLimit" />
              <div class="card-overlay"></div>
            </div>
            <div class="note-badge" v-if="element.notes && element.notes.length" @click="openNotesPanel()">
              <i-icon-park-outline:notes />
            </div>
          </div>
        </div>
      </template>
    </Draggable>

    <div class="thumbnails-footer">
      <span class="count-tag">Slide {{slideIndex + 1}} of {{slides.length}}</span>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, nextTick, ref, watch, useTemplateRef } from 'vue'
import { storeToRefs } from 'pinia'
import { useMainStore, useSlidesStore, useKeyboardStore } from '@/store'
import type { Slide, SlideTheme } from '@/types/slides'
import { isElementInViewport } from '@/utils/element'
import type { ContextmenuItem } from '@/components/Contextmenu/types'
import useSlideHandler from '@/hooks/useSlideHandler'
import useSectionHandler from '@/hooks/useSectionHandler'
import useLoadSlides from '@/hooks/useLoadSlides'
import useAddSlidesOrElements from '@/hooks/useAddSlidesOrElements'
import useAIPPT from '@/hooks/useAIPPT'
import api from '@/services'

import ThumbnailSlide from '@/views/components/ThumbnailSlide/index.vue'
import Templates from './Templates.vue'
import Popover from '@/components/Popover.vue'
import Draggable from 'vuedraggable'

const mainStore = useMainStore()
const slidesStore = useSlidesStore()
const keyboardStore = useKeyboardStore()
const { selectedSlidesIndex: _selectedSlidesIndex, thumbnailsFocus } = storeToRefs(mainStore)
const { slides, slideIndex, currentSlide } = storeToRefs(slidesStore)
const { ctrlKeyState, shiftKeyState } = storeToRefs(keyboardStore)

const { slidesLoadLimit } = useLoadSlides()
const { changeSlideLayout } = useAIPPT()

const selectedSlidesIndex = computed(() => [..._selectedSlidesIndex.value, slideIndex.value])

const presetLayoutPopoverVisible = ref(false)

const hasSection = computed(() => {
  return slides.value.some(item => item.sectionTag)
})

const { addSlidesFromData } = useAddSlidesOrElements()

const {
  copySlide,
  pasteSlide,
  createSlide,
  createSlideByTemplate,
  copyAndPasteSlide,
  deleteSlide,
  cutSlide,
  selectAllSlide,
  sortSlides,
  isEmptySlide,
} = useSlideHandler()

const {
  createSection,
  removeSection,
  removeAllSection,
  removeSectionSlides,
  updateSectionTitle,
} = useSectionHandler()

// 页面被切换时
const thumbnailsRef = useTemplateRef<InstanceType<typeof Draggable>>('thumbnailsRef')
watch(() => slideIndex.value, () => {
  if (selectedSlidesIndex.value.length) {
    mainStore.updateSelectedSlidesIndex([])
  }
  nextTick(() => {
    const activeThumbnailRef: HTMLElement = thumbnailsRef.value?.$el?.querySelector('.thumbnail-card.active')
    if (thumbnailsRef.value && activeThumbnailRef && !isElementInViewport(activeThumbnailRef, thumbnailsRef.value.$el)) {
      setTimeout(() => {
        activeThumbnailRef.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }, 100)
    }
  })
}, { immediate: true })

const changeSlideIndex = (index: number) => {
  mainStore.setActiveElementIdList([])
  if (slideIndex.value === index) return
  slidesStore.updateSlideIndex(index)
}

const handleClickSlideThumbnail = (e: MouseEvent, index: number) => {
  if (editingSectionId.value) return
  const isMultiSelected = selectedSlidesIndex.value.length > 1
  if (isMultiSelected && selectedSlidesIndex.value.includes(index) && e.button !== 0) return

  if (ctrlKeyState.value) {
    if (slideIndex.value === index) {
      if (!isMultiSelected) return
      const newSelectedSlidesIndex = selectedSlidesIndex.value.filter(item => item !== index)
      mainStore.updateSelectedSlidesIndex(newSelectedSlidesIndex)
      changeSlideIndex(selectedSlidesIndex.value[0])
    }
    else {
      const newSelectedSlidesIndex = selectedSlidesIndex.value.includes(index) 
        ? selectedSlidesIndex.value.filter(item => item !== index)
        : [...selectedSlidesIndex.value, index]
      mainStore.updateSelectedSlidesIndex(newSelectedSlidesIndex)
    }
  }
  else if (shiftKeyState.value) {
    let minIndex = Math.min(...selectedSlidesIndex.value)
    let maxIndex = index
    if (index < minIndex) {
      maxIndex = Math.max(...selectedSlidesIndex.value)
      minIndex = index
    }
    const newSelectedSlidesIndex = []
    for (let i = minIndex; i <= maxIndex; i++) newSelectedSlidesIndex.push(i)
    mainStore.updateSelectedSlidesIndex(newSelectedSlidesIndex)
  }
  else {
    mainStore.updateSelectedSlidesIndex([])
    changeSlideIndex(index)
  }
}

const setThumbnailsFocus = (focus: boolean) => {
  if (thumbnailsFocus.value === focus) return
  mainStore.setThumbnailsFocus(focus)
  if (!focus) mainStore.updateSelectedSlidesIndex([])
}

const handleDragEnd = (eventData: { newIndex: number; oldIndex: number }) => {
  const { newIndex, oldIndex } = eventData
  if (newIndex === undefined || oldIndex === undefined || newIndex === oldIndex) return
  sortSlides(newIndex, oldIndex)
}

const openNotesPanel = () => mainStore.setNotesPanelState(true)

const editingSectionId = ref('')

const editSection = (id: string) => {
  mainStore.setDisableHotkeysState(true)
  editingSectionId.value = id || 'default'
  nextTick(() => {
    const inputRef = document.querySelector(`#section-title-input-${id || 'default'}`) as HTMLInputElement
    inputRef.focus()
  })
}

const saveSection = (e: FocusEvent | KeyboardEvent) => {
  const title = (e.target as HTMLInputElement).value
  updateSectionTitle(editingSectionId.value, title)
  editingSectionId.value = ''
  mainStore.setDisableHotkeysState(false)
}

const insertAllTemplates = ({ slides, theme }: { slides: Slide[], theme: Partial<SlideTheme> }) => {
  if (isEmptySlide.value) slidesStore.setSlides(slides, theme)
  else addSlidesFromData(slides)
}

const contextmenusSection = (el: HTMLElement): ContextmenuItem[] => {
  const sectionId = el.dataset.sectionId!
  return [
    { text: '删除分节', handler: () => removeSection(sectionId) },
    { text: '删除分节及幻灯片', handler: () => { mainStore.setActiveElementIdList([]); removeSectionSlides(sectionId) } },
    { text: '清除所有分节', handler: removeAllSection },
    { text: '重命名', handler: () => editSection(sectionId) },
  ]
}

const contextmenusThumbnails = (): ContextmenuItem[] => [
  { text: '粘贴', subText: 'Ctrl + V', handler: pasteSlide },
  { text: '全选', subText: 'Ctrl + A', handler: selectAllSlide },
  { text: '新建页面', subText: 'Enter', handler: createSlide },
]

const contextmenusThumbnailItem = (): ContextmenuItem[] => [
  { text: '剪切', subText: 'Ctrl + X', handler: cutSlide },
  { text: '复制', subText: 'Ctrl + C', handler: copySlide },
  { text: '粘贴', subText: 'Ctrl + V', handler: pasteSlide },
  { text: '全选', subText: 'Ctrl + A', handler: selectAllSlide },
  { divider: true },
  { text: '新建页面', subText: 'Enter', handler: createSlide },
  { text: '复制页面', subText: 'Ctrl + D', handler: copyAndPasteSlide },
  { text: '删除页面', subText: 'Delete', handler: () => deleteSlide() },
  { divider: true },
  {
    text: 'AI 智能换布局',
    handler: async () => {
      const templateId = currentSlide.value.styleKey || 'template_1'
      mainStore.setDisableHotkeysState(true)
      try {
        const templateData = await api.getTemplateData(templateId)
        if (templateData && templateData.slides) {
          changeSlideLayout(currentSlide.value.id, templateData.slides)
        }
      } catch (err) {
        console.error('获取排版模板失败:', err)
      } finally {
        mainStore.setDisableHotkeysState(false)
      }
    },
    disable: !currentSlide.value.type,
  },
  { text: '新建分节', handler: createSection, disable: !!currentSlide.value.sectionTag },
]
</script>

<style lang="scss" scoped>
.thumbnails-v2 {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: transparent;
  user-select: none;
}

.thumbnails-header {
  padding: 16px;
  display: flex;
  gap: 8px;
  
  .add-slide-v2 {
    flex: 1;
    height: 36px;
    background: linear-gradient(135deg, #4f46e5, #7c3aed);
    border-radius: 10px;
    display: flex; align-items: center; justify-content: center; gap: 8px;
    color: #fff; font-size: 13px; font-weight: 700;
    cursor: pointer; transition: all 0.2s;
    box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
    &:hover { transform: translateY(-1px); box-shadow: 0 6px 16px rgba(79, 70, 229, 0.3); }
    .icon { font-size: 16px; }
  }

  .layout-trigger {
    width: 36px; height: 36px;
    background: #fff; border-radius: 10px;
    display: flex; align-items: center; justify-content: center;
    color: #64748b; cursor: pointer; transition: all 0.2s;
    border: 1px solid #f1f5f9;
    &:hover { background: #f8fafc; color: $themeColor; border-color: $themeColor; }
  }
}

.thumbnail-list-v2 {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 20px;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.05); border-radius: 10px; }
}

.thumbnail-group {
  margin-bottom: 4px;
}

.section-divider {
  margin: 16px 0 12px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;

  .divider-line {
    position: absolute; width: 100%; height: 1px; background: rgba(0,0,0,0.05);
  }

  .section-pill {
    position: relative;
    background: #f1f5f9;
    padding: 2px 12px;
    border-radius: 20px;
    font-size: 10px;
    font-weight: 800;
    color: #94a3b8;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    border: 1px solid #fff;
    box-shadow: 0 2px 4px rgba(0,0,0,0.02);
    
    input { background: transparent; border: none; outline: none; width: 80px; text-align: center; color: #475569; font-weight: 800; }
  }
}

.thumbnail-card {
  display: flex;
  gap: 12px;
  padding: 8px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  margin-bottom: 8px;

  .card-index {
    width: 20px; font-size: 11px; font-weight: 800; color: #cbd5e1; display: flex; align-items: flex-start; padding-top: 4px;
  }

  .card-preview {
    flex: 1;
    position: relative;
    border-radius: 8px;
    overflow: hidden;
    border: 2px solid transparent;
    transition: all 0.2s;
    background: #fff;
    box-shadow: 0 4px 12px rgba(0,0,0,0.05);

    .thumbnail { transform: scale(1.01); transition: transform 0.3s; }
  }

  &:hover {
    background: rgba(255,255,255,0.5);
    .card-preview { border-color: rgba($themeColor, 0.3); .thumbnail { transform: scale(1.05); } }
    .card-index { color: #94a3b8; }
  }

  &.active {
    background: #fff;
    box-shadow: 0 10px 25px rgba(0,0,0,0.05);
    .card-preview { border-color: $themeColor; box-shadow: 0 0 0 4px rgba($themeColor, 0.1); }
    .card-index { color: $themeColor; }
  }

  &.selected:not(.active) {
    background: rgba($themeColor, 0.05);
    .card-preview { border-color: rgba($themeColor, 0.5); }
  }

  .note-badge {
    position: absolute; right: 12px; top: 12px;
    width: 20px; height: 20px; border-radius: 6px;
    background: $themeColor; color: #fff;
    display: flex; align-items: center; justify-content: center;
    font-size: 12px; box-shadow: 0 4px 8px rgba($themeColor, 0.3);
    z-index: 5;
  }
}

.thumbnails-footer {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.5);
  border-top: 1px solid rgba(0,0,0,0.03);

  .count-tag {
    font-size: 11px; font-weight: 800; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px;
  }
}
</style>
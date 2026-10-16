<template>
  <div class="text-style-panel">
    <div class="ai-actions" style="margin-bottom: 10px;">
      <Popover trigger="click" placement="bottom" style="width: 100%;">
        <template #content>
          <div style="padding: 12px; display: flex; flex-direction: column; gap: 10px; width: 220px;">
             <!-- 自定义意图 -->
             <div style="display: flex; gap: 5px;">
               <Input v-model:value="customPrompt" placeholder="或者输入你的想法..." @enter="handleAIPolish(customPrompt)" />
               <Button type="primary" @click="handleAIPolish(customPrompt)" :disabled="isAIWriting || !customPrompt"><i-icon-park-outline:send /></Button>
             </div>
             <Divider style="margin: 2px 0;" />
             
             <!-- 预设快捷项 -->
             <Button @click="handleAIPolish('polish')" :disabled="isAIWriting">✨ 润色文字</Button>
             <Button @click="handleAIPolish('expand')" :disabled="isAIWriting">✨ 丰富扩写</Button>
             <Button @click="handleAIPolish('summarize')" :disabled="isAIWriting">✨ 总结摘要</Button>
             <Button @click="handleAIPolish('translate')" :disabled="isAIWriting">✨ 翻译为英文</Button>
          </div>
        </template>
        <Button class="preset-style-item" style="width: 100%; height: 40px; margin-bottom: 8px; border: none; background: linear-gradient(135deg, #4F46E5, #06B6D4); color: #fff; font-weight: bold; border-radius: 8px;">
          {{ isAIWriting ? 'AI 思考中...' : '✨ AI 智能写作' }}
        </Button>
      </Popover>
    </div>

    <div class="preset-style">
      <div 
        class="preset-style-item"
        v-for="item in presetStyles"
        :key="item.label"
        :style="item.style"
        @click="emitBatchRichTextCommand(item.cmd)"
      >{{item.label}}</div>
    </div>

    <Divider />
    <RichTextBase />
    <Divider />

    <div class="row">
      <div style="width: 40%;">行间距：</div>
      <Select style="width: 60%;"
        :value="lineHeight || 1"
        @update:value="value => updateText({ lineHeight: value as number })"
        :options="lineHeightOptions.map(item => ({
          label: item + '倍', value: item
        }))"
      >
        <template #icon>
          <i-icon-park-outline:row-height />
        </template>
      </Select>
    </div>
    <div class="row">
      <div style="width: 40%;">段间距：</div>
      <Select style="width: 60%;"
        :value="paragraphSpace || 0"
        @update:value="value => updateText({ paragraphSpace: value as number })"
        :options="paragraphSpaceOptions.map(item => ({
          label: item + 'px', value: item
        }))"
      >
        <template #icon>
          <i-icon-park-outline:vertical-spacing-between-items />
        </template>
      </Select>
    </div>
    <div class="row">
      <div style="width: 40%;">字间距：</div>
      <Select style="width: 60%;"
        :value="wordSpace || 0"
        @update:value="value => updateText({ wordSpace: value as number })"
        :options="wordSpaceOptions.map(item => ({
          label: item + 'px', value: item
        }))"
      >
        <template #icon>
          <i-icon-park-outline:fullwidth />
        </template>
      </Select>
    </div>
    <div class="row">
      <div style="width: 40%;">文本框填充：</div>
      <Popover trigger="click" style="width: 60%;">
        <template #content>
          <ColorPicker
            :modelValue="fill"
            @update:modelValue="value => updateText({ fill: value })"
          />
        </template>
        <ColorButton :color="fill" />
      </Popover>
    </div>

    <Divider />
    <ElementOutline />
    <Divider />
    <ElementShadow />
    <Divider />
    <ElementOpacity />
  </div>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useMainStore, useSlidesStore } from '@/store'
import type { PPTTextElement } from '@/types/slides'
import emitter, { EmitterEvents, type RichTextAction } from '@/utils/emitter'
import useHistorySnapshot from '@/hooks/useHistorySnapshot'
import api from '@/services'
import Button from '@/components/Button.vue'
import Input from '@/components/Input.vue'

import ElementOpacity from '../common/ElementOpacity.vue'
import ElementOutline from '../common/ElementOutline.vue'
import ElementShadow from '../common/ElementShadow.vue'
import RichTextBase from '../common/RichTextBase.vue'
import ColorButton from '@/components/ColorButton.vue'
import ColorPicker from '@/components/ColorPicker/index.vue'
import Divider from '@/components/Divider.vue'
import Select from '@/components/Select.vue'
import Popover from '@/components/Popover.vue'

const customPrompt = ref('')

// 注意，存在一个未知原因的BUG，如果文本加粗后文本框高度增加，画布的可视区域定位会出现错误
// 因此在执行预置样式命令时，将加粗命令放在尽可能靠前的位置，避免字号增大后再加粗
const presetStyles = [
  {
    label: '大标题',
    style: {
      fontSize: '26px',
      fontWeight: 700,
    },
    cmd: [
      { command: 'clear' },
      { command: 'bold' },
      { command: 'fontsize', value: '66px' },
      { command: 'align', value: 'center' },
    ],
  },
  {
    label: '小标题',
    style: {
      fontSize: '22px',
      fontWeight: 700,
    },
    cmd: [
      { command: 'clear' },
      { command: 'bold' },
      { command: 'fontsize', value: '40px' },
      { command: 'align', value: 'center' },
    ],
  },
  {
    label: '正文',
    style: {
      fontSize: '20px',
    },
    cmd: [
      { command: 'clear' },
      { command: 'fontsize', value: '20px' },
    ],
  },
  {
    label: '正文[小]',
    style: {
      fontSize: '18px',
    },
    cmd: [
      { command: 'clear' },
      { command: 'fontsize', value: '18px' },
    ],
  },
  {
    label: '注释 1',
    style: {
      fontSize: '16px',
      fontStyle: 'italic',
    },
    cmd: [
      { command: 'clear' },
      { command: 'fontsize', value: '16px' },
      { command: 'em' },
    ],
  },
  {
    label: '注释 2',
    style: {
      fontSize: '16px',
      textDecoration: 'underline',
    },
    cmd: [
      { command: 'clear' },
      { command: 'fontsize', value: '16px' },
      { command: 'underline' },
    ],
  },
]

const mainStore = useMainStore()
const slidesStore = useSlidesStore()
const { handleElement, handleElementId } = storeToRefs(mainStore)

const { addHistorySnapshot } = useHistorySnapshot()

const updateText = (props: Partial<PPTTextElement>) => {
  slidesStore.updateElement({ id: handleElementId.value, props })
  addHistorySnapshot()
}

const fill = ref<string>('#000')
const lineHeight = ref<number>()
const wordSpace = ref<number>()
const paragraphSpace = ref<number>()

watch(handleElement, () => {
  if (!handleElement.value || handleElement.value.type !== 'text') return

  fill.value = handleElement.value.fill || '#fff'
  lineHeight.value = handleElement.value.lineHeight || 1.5
  wordSpace.value = handleElement.value.wordSpace || 0
  paragraphSpace.value = handleElement.value.paragraphSpace === undefined ? 5 : handleElement.value.paragraphSpace
  emitter.emit(EmitterEvents.SYNC_RICH_TEXT_ATTRS_TO_STORE)
}, { deep: true, immediate: true })

const lineHeightOptions = [0.9, 1.0, 1.15, 1.2, 1.4, 1.5, 1.8, 2.0, 2.5, 3.0]
const wordSpaceOptions = [0, 1, 2, 3, 4, 5, 6, 8, 10]
const paragraphSpaceOptions = [0, 5, 10, 15, 20, 25, 30, 40, 50, 80]

// 发送富文本设置命令（批量）
const emitBatchRichTextCommand = (action: RichTextAction[]) => {
  emitter.emit(EmitterEvents.RICH_TEXT_COMMAND, { action })
}

const isAIWriting = ref(false)

const handleAIPolish = async (command: string) => {
  if (!handleElement.value || handleElement.value.type !== 'text') return
  const originalText = handleElement.value.content
  
  isAIWriting.value = true
  let newContent = ''
  
  try {
    const stream = await api.AI_Writing({ content: originalText, command })
    if (typeof stream === 'object' && stream.state === -1) {
      isAIWriting.value = false
      return
    }
    
    const reader = stream.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let lineBuffer = ''

    const handleLine = (line: string) => {
      try {
        const data = JSON.parse(line)
        if (data.type === 'content') {
          newContent += data.content
          slidesStore.updateElement({ 
            id: handleElementId.value, 
            props: { content: newContent } 
          })
        }
      } catch (e) {
        // 如果不是 JSON，尝试直接追加（兼容逻辑）
        if (!line.startsWith('{')) {
          newContent += line
          slidesStore.updateElement({ 
            id: handleElementId.value, 
            props: { content: newContent } 
          })
        }
      }
    }
    
    const readStream = () => {
      reader.read().then(({ done, value }: { done: boolean; value?: Uint8Array }) => {
        if (done) {
          if (lineBuffer.trim()) handleLine(lineBuffer.trim())
          isAIWriting.value = false
          addHistorySnapshot()
          return
        }
        
        lineBuffer += decoder.decode(value, { stream: true })
        const lines = lineBuffer.split('\n')
        lineBuffer = lines.pop() || ''

        for (const line of lines) {
          if (line.trim()) handleLine(line.trim())
        }
        readStream()
      })
    }
    readStream()
  } catch (e) {
    console.error(e)
    isAIWriting.value = false
  }
}
</script>

<style lang="scss" scoped>
.text-style-panel {
  user-select: none;
}
.row {
  width: 100%;
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}
.preset-style {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.preset-style-item {
  width: 50%;
  height: 50px;
  border: solid 1px #d6d6d6;
  box-sizing: border-box;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  cursor: pointer;
  transition: all $transitionDelay;

  &:hover {
    border-color: $themeColor;
    color: $themeColor;
    z-index: 1;
  }

  &:nth-child(2n) {
    margin-left: -1px;
  }
  &:nth-child(n+3) {
    margin-top: -1px;
  }
}
</style>
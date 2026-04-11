import { ref } from 'vue'
import { nanoid } from 'nanoid'
import type { ImageClipDataRange, PPTElement, PPTImageElement, PPTShapeElement, PPTTextElement, Slide, TextType } from '@/types/slides'
import type { AIPPTSlide } from '@/types/AIPPT'
import { useSlidesStore, useAiStore } from '@/store'
import useAddSlidesOrElements from './useAddSlidesOrElements'
import useSlideHandler from './useSlideHandler'
import api from '@/services'

interface ImgPoolItem {
  id: string
  src: string
  width: number
  height: number
}

export default () => {
  const slidesStore = useSlidesStore()
  const aiStore = useAiStore()
  const { addSlidesFromData } = useAddSlidesOrElements()
  const { isEmptySlide } = useSlideHandler()

  const imgPool = ref<ImgPoolItem[]>([])
  const transitionIndex = ref(0)
  const transitionTemplate = ref<Slide | null>(null)

  const checkTextType = (el: PPTElement, type: TextType) => {
    return (el.type === 'text' && el.textType === type) || (el.type === 'shape' && el.text && el.text.type === type)
  }
  
  const getUseableTemplates = (templates: Slide[], n: number, type: TextType) => {
    if (n === 1) {
      const list = templates.filter(slide => {
        const items = slide.elements.filter(el => checkTextType(el, type))
        const titles = slide.elements.filter(el => checkTextType(el, 'title'))
        const texts = slide.elements.filter(el => checkTextType(el, 'content'))
  
        return !items.length && titles.length === 1 && texts.length === 1
      })
  
      if (list.length) return list
    }
  
    let target: Slide | null = null
  
    const list = templates.filter(slide => {
      const len = slide.elements.filter(el => checkTextType(el, type)).length
      return len >= n
    })
    if (list.length === 0) {
      const sorted = templates.sort((a, b) => {
        const aLen = a.elements.filter(el => checkTextType(el, type)).length
        const bLen = b.elements.filter(el => checkTextType(el, type)).length
        return aLen - bLen
      })
      target = sorted[sorted.length - 1]
    }
    else {
      target = list.reduce((closest, current) => {
        const currentLen = current.elements.filter(el => checkTextType(el, type)).length
        const closestLen = closest.elements.filter(el => checkTextType(el, type)).length
        return (currentLen - n) <= (closestLen - n) ? current : closest
      })
    }
  
    return templates.filter(slide => {
      const len = slide.elements.filter(el => checkTextType(el, type)).length
      const targetLen = target!.elements.filter(el => checkTextType(el, type)).length
      return len === targetLen
    })
  }
  
  const getAdaptedFontsize = ({
    text,
    fontSize,
    fontFamily,
    width,
    height,
    lineHeight,
    maxLine,
  }: {
    text: string
    fontSize: number
    fontFamily: string
    width: number
    height: number
    lineHeight: number
    maxLine: number
  }) => {
    const canvas = document.createElement('canvas')
    const context = canvas.getContext('2d')!
  
    let newFontSize = fontSize
    const minFontSize = 10
  
    while (newFontSize >= minFontSize) {
      context.font = `${newFontSize}px ${fontFamily}`
      const textWidth = context.measureText(text).width
      const line = Math.ceil(textWidth / width)

      if (maxLine > 1 && height) {
        const heightOfLine = Math.max(newFontSize, 16) * (newFontSize < 15 ? 1.2 : lineHeight) * 1.2
        const totalHeight = line * heightOfLine
        if (totalHeight <= height) return newFontSize
      }
      if (line <= maxLine) return newFontSize

      const step = newFontSize <= 22 ? 1 : 2
      newFontSize = newFontSize - step
    }
  
    return minFontSize
  }
  
  const getFontInfo = (htmlString: string) => {
    const fontSizeRegex = /font-size:\s*(\d+(?:\.\d+)?)\s*px/i
    const fontFamilyRegex = /font-family:\s*['"]?([^'";]+)['"]?\s*(?=;|>|$)/i
  
    const defaultInfo = {
      fontSize: 16,
      fontFamily: 'Microsoft Yahei',
    }
  
    const fontSizeMatch = htmlString.match(fontSizeRegex)
    const fontFamilyMatch = htmlString.match(fontFamilyRegex)
  
    return {
      fontSize: fontSizeMatch ? (+fontSizeMatch[1].trim()) : defaultInfo.fontSize,
      fontFamily: fontFamilyMatch ? fontFamilyMatch[1].trim() : defaultInfo.fontFamily,
    }
  }
  
  const getNewTextElement = ({
    el,
    text,
    maxLine,
    longestText,
    digitPadding,
  }: {
    el: PPTTextElement | PPTShapeElement
    text: string
    maxLine: number
    longestText?: string
    digitPadding?: boolean
  }): PPTTextElement | PPTShapeElement => {
    const padding = 10
    const width = el.width - padding * 2 - 2
    const height = el.height - padding * 2 - 2
    const lineHeight = el.type === 'text' ? (el.lineHeight || 1.5) : 1.2
    let content = el.type === 'text' ? el.content : el.text!.content
  
    const fontInfo = getFontInfo(content)
    const size = getAdaptedFontsize({
      text: longestText || text,
      fontSize: fontInfo.fontSize,
      fontFamily: fontInfo.fontFamily,
      width,
      height,
      lineHeight,
      maxLine,
    })
  
    const parser = new DOMParser()
    const doc = parser.parseFromString(content, 'text/html')
  
    const treeWalker = document.createTreeWalker(doc.body, NodeFilter.SHOW_TEXT)
  
    const firstTextNode = treeWalker.nextNode()
    if (firstTextNode) {
      if (digitPadding && firstTextNode.textContent && firstTextNode.textContent.length === 2 && text.length === 1) {
        firstTextNode.textContent = '0' + text
      }
      else firstTextNode.textContent = text

      let node
      while ((node = treeWalker.nextNode())) {
        node.parentNode?.removeChild(node)
      }
    }
  
    if (doc.body.innerHTML.indexOf('font-size') === -1) {
      const p = doc.querySelector('p')
      if (p) p.style.fontSize = '16px'
    }
  
    content = doc.body.innerHTML.replace(/font-size:(.+?)px/g, `font-size: ${size}px`)
  
    return el.type === 'text' ? { ...el, content, lineHeight: size < 15 ? 1.2 : el.lineHeight } : { ...el, text: { ...el.text!, content } }
  }

  const getUseableImage = (el: PPTImageElement): ImgPoolItem | null => {
    let img: ImgPoolItem | null = null
  
    let imgs = []
  
    if (el.width === el.height) imgs = imgPool.value.filter(img => img.width === img.height)
    else if (el.width > el.height) imgs = imgPool.value.filter(img => img.width > img.height)
    else imgs = imgPool.value.filter(img => img.width <= img.height)
    if (!imgs.length) imgs = imgPool.value
  
    img = imgs[Math.floor(Math.random() * imgs.length)]
    imgPool.value = imgPool.value.filter(item => item.id !== img!.id)
  
    return img
  }
  
  const getNewImgElement = (el: PPTImageElement): PPTImageElement => {
    const img = getUseableImage(el)
    if (!img) return el
  
    let scale = 1
    let w = el.width
    let h = el.height
    let range: ImageClipDataRange = [[0, 0], [0, 0]]
    const radio = el.width / el.height
    if (img.width / img.height >= radio) {
      scale = img.height / el.height
      w = img.width / scale
      const diff = (w - el.width) / 2 / w * 100
      range = [[diff, 0], [100 - diff, 100]]
    }
    else {
      scale = img.width / el.width
      h = img.height / scale
      const diff = (h - el.height) / 2 / h * 100
      range = [[0, diff], [100, 100 - diff]]
    }
    const clipShape = (el.clip && el.clip.shape) ? el.clip.shape : 'rect'
    const clip = { range, shape: clipShape }
    const src = img.src
  
    return { ...el, src, clip }
  }
  
  const getMdContent = (content: string) => {
    const regex = /```markdown([^```]*)```/
    const match = content.match(regex)
    if (match) return match[1].trim()
    return content.replace('```markdown', '').replace('```', '')
  }

  const presetImgPool = (imgs: ImgPoolItem[]) => {
    imgPool.value = imgs
  }

  let imgPlaceholderCounter = 0

  const processImageElement = (el: PPTImageElement, imgMode: string | undefined, contextText: string): PPTImageElement => {
    if (imgMode === 'ai-create') {
      imgPlaceholderCounter++
      const placeholderSrc = `https://via.placeholder.com/800x600/4F46E5/ffffff?text=AI-GEN-${imgPlaceholderCounter}`
      
      import('@/services').then(({ default: api }) => {
        api.AIGenerateImage(contextText).then((res: any) => {
          const url = res?.data?.url || res?.url
          if (url && !url.includes('placeholder')) {
            const slides = slidesStore.slides
            for (let i = 0; i < slides.length; i++) {
              const targetEl = slides[i].elements.find(
                e => e.type === 'image' && (e as PPTImageElement).src === placeholderSrc
              )
              if (targetEl) {
                slidesStore.updateElement({ id: targetEl.id, slideId: slides[i].id, props: { src: url } })
                break
              }
            }
          }
        }).catch((err: any) => {
          console.error('[AI生图] 前端请求失败:', err)
        })
      })
      return { ...el, src: placeholderSrc }
    } else if (imgPool.value.length) {
      return getNewImgElement(el)
    }
    return el
  }

  // 辅助方法：单页渲染逻辑
  const renderSingleSlide = (template: Slide, aiData: any, styleKey: string, imgMode?: string): Slide => {
    const elements = template.elements.map(el => {
      if (el.type === 'image' && el.imageType) return processImageElement(el, imgMode, aiData.data.title || '商务配图')
      if (el.type !== 'text' && el.type !== 'shape') return el
      
      if (checkTextType(el, 'title') && aiData.data.title) {
        return getNewTextElement({ el, text: aiData.data.title, maxLine: 2 })
      }
      
      if (checkTextType(el, 'content') && aiData.data.text) {
        return getNewTextElement({ el, text: aiData.data.text, maxLine: 8 })
      }

      if (aiData.data.items && aiData.data.items.length > 0) {
        if (checkTextType(el, 'item')) {
          const itemEls = template.elements.filter(e => checkTextType(e, 'item'))
          const index = itemEls.findIndex(e => e.id === el.id)
          const item = aiData.data.items[index]
          if (item) return getNewTextElement({ el, text: item.text || item, maxLine: 4 })
        }
        if (checkTextType(el, 'itemTitle')) {
          const itemTitleEls = template.elements.filter(e => checkTextType(e, 'itemTitle'))
          const index = itemTitleEls.findIndex(e => e.id === el.id)
          const item = aiData.data.items[index]
          if (item && item.title) return getNewTextElement({ el, text: item.title, maxLine: 1 })
        }
      }
      return el
    })

    return {
      ...template,
      id: nanoid(10),
      type: aiData.type, // 持久化页面类型 (cover, content...)
      styleKey: styleKey, // 持久化模板ID (template_1...)
      layoutId: template.id, // 持久化当前版式在模板中的原始ID
      elements,
      remark: aiData.remark || '',
    }
  }

  // 辅助方法：安全提取元素中的文字内容并过滤 HTML
  const extractText = (el: PPTElement | undefined): string => {
    if (!el) return ''
    let content = ''
    if (el.type === 'text') content = el.content
    else if (el.type === 'shape' && el.text) content = el.text.content
    return content ? content.replace(/<[^>]+>/g, '') : ''
  }

  // 核心增强：为指定幻灯片更换布局
  const changeSlideLayout = async (slideId: string, currentTemplateSlides: Slide[]) => {
    const slide = slidesStore.slides.find(s => s.id === slideId)
    if (!slide || !slide.type) return

    const contentData: any = { items: [] }
    const titleEl = slide.elements.find(el => checkTextType(el, 'title'))
    const textEl = slide.elements.find(el => checkTextType(el, 'content'))
    contentData.title = extractText(titleEl)
    contentData.text = extractText(textEl)

    const itemEls = slide.elements.filter(el => checkTextType(el, 'item'))
    const itemTitleEls = slide.elements.filter(el => checkTextType(el, 'itemTitle'))
    itemEls.forEach((el, index) => {
      contentData.items.push({
        title: extractText(itemTitleEls[index]),
        text: extractText(el)
      })
    })

    // 1. 尝试在当前模板内部寻找替代品 (排除掉当前正在使用的 layoutId)
    let candidateTemplates = currentTemplateSlides.filter(t => t.type === slide.type && t.id !== (slide as any).layoutId)

    // 2. 如果当前模板没有其他同类型版式，则随机挑选一个其他模板库的同类型版式
    if (!candidateTemplates.length) {
      const otherTemplates = slidesStore.templates.filter(t => t.id !== slide.styleKey)
      const randomOtherTemplateId = otherTemplates[Math.floor(Math.random() * otherTemplates.length)].id
      const otherTemplateData = await api.getTemplateData(randomOtherTemplateId)
      candidateTemplates = otherTemplateData.slides.filter((t: Slide) => t.type === slide.type)
    }

    if (!candidateTemplates.length) return

    const newTemplate = candidateTemplates[Math.floor(Math.random() * candidateTemplates.length)]
    const newSlide = renderSingleSlide(newTemplate, { type: slide.type, data: contentData, remark: slide.remark || '' }, slide.styleKey || 'template_1', undefined)
    
    // 重要修复：正确调用 updateSlide，使用 (props, slideId) 签名
    slidesStore.updateSlide({
      elements: newSlide.elements,
      layoutId: newTemplate.id, // 更新为新版式的 Layout ID
    }, slideId)
  }

  const AIPPT = (templateSlides: Slide[], _AISlides: AIPPTSlide[], styleKey: string, imgMode?: string) => {
    slidesStore.updateSlideIndex(slidesStore.slides.length - 1)

    const AISlides: AIPPTSlide[] = []
    for (const template of _AISlides) {
      if (template.type === 'content') {
        const items = template.data.items
        if (items.length === 5 || items.length === 6) {
          const items1 = items.slice(0, 3)
          const items2 = items.slice(3)
          AISlides.push({ ...template, data: { ...template.data, items: items1 } })
          AISlides.push({ ...template, data: { ...template.data, items: items2 }, offset: 3 })
        }
        else if (items.length === 7 || items.length === 8) {
          const items1 = items.slice(0, 4)
          const items2 = items.slice(4)
          AISlides.push({ ...template, data: { ...template.data, items: items1 } })
          AISlides.push({ ...template, data: { ...template.data, items: items2 }, offset: 4 })
        }
        else if (items.length === 9 || items.length === 10) {
          const items1 = items.slice(0, 3)
          const items2 = items.slice(3, 6)
          const items3 = items.slice(6)
          AISlides.push({ ...template, data: { ...template.data, items: items1 } })
          AISlides.push({ ...template, data: { ...template.data, items: items2 }, offset: 3 })
          AISlides.push({ ...template, data: { ...template.data, items: items3 }, offset: 6 })
        }
        else if (items.length > 10) {
          const items1 = items.slice(0, 4)
          const items2 = items.slice(4, 8)
          const items3 = items.slice(8)
          AISlides.push({ ...template, data: { ...template.data, items: items1 } })
          AISlides.push({ ...template, data: { ...template.data, items: items2 }, offset: 4 })
          AISlides.push({ ...template, data: { ...template.data, items: items3 }, offset: 8 })
        }
        else {
          AISlides.push(template)
        }
      }
      else if (template.type === 'contents') {
        const items = template.data.items
        if (items.length === 11) {
          const items1 = items.slice(0, 6)
          const items2 = items.slice(6)
          AISlides.push({ ...template, data: { ...template.data, items: items1 } })
          AISlides.push({ ...template, data: { ...template.data, items: items2 }, offset: 6 })
        }
        else if (items.length > 11) {
          const items1 = items.slice(0, 10)
          const items2 = items.slice(10)
          AISlides.push({ ...template, data: { ...template.data, items: items1 } })
          AISlides.push({ ...template, data: { ...template.data, items: items2 }, offset: 10 })
        }
        else {
          AISlides.push(template)
        }
      }
      else AISlides.push(template)
    }

    const coverTemplates = templateSlides.filter(slide => slide.type === 'cover')
    const contentsTemplates = templateSlides.filter(slide => slide.type === 'contents')
    const transitionTemplates = templateSlides.filter(slide => slide.type === 'transition')
    const contentTemplates = templateSlides.filter(slide => slide.type === 'content')
    const endTemplates = templateSlides.filter(slide => slide.type === 'end')

    if (!transitionTemplate.value) {
      const _transitionTemplate = transitionTemplates[Math.floor(Math.random() * transitionTemplates.length)]
      transitionTemplate.value = _transitionTemplate
    }

    const slides = []
    
    let processedCount = 0
    for (const item of AISlides) {
      processedCount++
      // 核心修复：根据当前 store 中的幻灯片数量动态计算页码
      const currentPageNum = (isEmptySlide.value ? 0 : slidesStore.slides.length) + processedCount
      
      const slideTypeDesc = {
        'cover': '封面页',
        'contents': '目录页',
        'transition': '过渡页',
        'content': '正文页',
        'end': '结束页'
      }[item.type as string] || '幻灯片'
      
      aiStore.addLog(`正在渲染第 ${currentPageNum} 页: ${slideTypeDesc}...`, 'status')

      if (item.type === 'cover') {
        const coverTemplate = coverTemplates[Math.floor(Math.random() * coverTemplates.length)]
        slides.push(renderSingleSlide(coverTemplate, item, styleKey, imgMode))
      }
      else if (item.type === 'contents') {
        const _contentsTemplates = getUseableTemplates(contentsTemplates, item.data.items.length, 'item')
        const contentsTemplate = _contentsTemplates[Math.floor(Math.random() * _contentsTemplates.length)]
        slides.push(renderSingleSlide(contentsTemplate, item, styleKey, imgMode))
      }
      else if (item.type === 'transition') {
        transitionIndex.value = transitionIndex.value + 1
        slides.push(renderSingleSlide(transitionTemplate.value!, item, styleKey, imgMode))
      }
      else if (item.type === 'content') {
        const _contentTemplates = getUseableTemplates(contentTemplates, item.data.items.length, 'item')
        const contentTemplate = _contentTemplates[Math.floor(Math.random() * _contentTemplates.length)]
        slides.push(renderSingleSlide(contentTemplate, item, styleKey, imgMode))
      }
      else if (item.type === 'end') {
        const endTemplate = endTemplates[Math.floor(Math.random() * endTemplates.length)]
        slides.push(renderSingleSlide(endTemplate, item, styleKey, imgMode))
      }
    }

    for (const slide of slides) {
      for (const el of slide.elements) {
        if ('lock' in el) delete (el as any).lock
      }
    }

    if (isEmptySlide.value) slidesStore.setSlides(slides)
    else addSlidesFromData(slides)
  }

  return {
    presetImgPool,
    AIPPT,
    getMdContent,
    changeSlideLayout,
  }
}

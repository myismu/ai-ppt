import type { Slide, PPTTextElement, PPTElement } from '@/types/slides'
import { saveAs } from 'file-saver'

export interface AITemplateConfig {
  name?: string;
  category?: string;
}

/**
 * 将当前编辑器的完整数据，自动解析并转换为兼容 PPTist 的 AI 智能格式模板
 * @param fullData 原始完整 PPT 数据对象 (包含 slides, theme, width, height)
 */
export const extractToAITemplate = (fullData: any): string => {
  const rawSlides: Slide[] = fullData.slides || [];
  const aiTemplateSlides: any[] = []

  rawSlides.forEach((slide, index) => {
    // 深拷贝以防破坏原数据
    const newSlide = JSON.parse(JSON.stringify(slide))
    const textElements = newSlide.elements.filter((el: PPTElement) => el.type === 'text') as PPTTextElement[]

    // --- 1. 启发式判断 Slide 类型 ---
    let slideType = 'content'
    if (index === 0) {
      slideType = 'cover'
    } else if (index === rawSlides.length - 1) {
      slideType = 'end'
    } else {
      const isContents = textElements.some(el => el.content.includes('目录') || el.content.toUpperCase().includes('CONTENT'))
      if (isContents) {
        slideType = 'contents'
      } else {
        if (textElements.length > 0 && textElements.length <= 3) {
          slideType = 'transition'
        }
      }
    }
    newSlide.type = slideType

    // --- 2. 启发式标记元素类型 ---
    if (textElements.length > 0) {
      const textFeatures = textElements.map(el => {
        const sizeMatch = el.content.match(/font-size:\s*(\d+)px/);
        const fontSize = sizeMatch ? parseInt(sizeMatch[1]) : 16;
        return { id: el.id, fontSize, contentLength: el.content.replace(/<[^>]+>/g, '').length };
      });
      textFeatures.sort((a, b) => b.fontSize - a.fontSize);
      const biggestFontId = textFeatures[0].id;
      const maxLengthFeature = [...textFeatures].sort((a, b) => b.contentLength - a.contentLength)[0];

      newSlide.elements.forEach((el: any) => {
        if (el.type === 'text') {
          if (el.id === biggestFontId) el.textType = 'title'
          else if (slideType === 'content' || slideType === 'contents') {
            if (el.id === maxLengthFeature.id) el.textType = slideType === 'contents' ? 'item' : 'content'
            else if (el.groupId) {
              const plainText = el.content.replace(/<[^>]+>/g, '').trim();
              if (/^\d+/.test(plainText) && plainText.length <= 4) el.textType = 'itemNumber'
              else el.textType = 'itemTitle'
            } else el.textType = 'item'
          } else {
            if (el.id === maxLengthFeature.id) el.textType = 'content'
          }
        } else if (el.type === 'image') {
          if (el.width > 500 && slideType === 'cover') el.imageType = 'background'
          else el.imageType = 'pageFigure'
        }
      });
    }
    aiTemplateSlides.push(newSlide)
  })

  // 返回完整的 PPT 对象结构，确保包含 theme, width, height 等
  return JSON.stringify({
    ...fullData,
    slides: aiTemplateSlides
  }, null, 2)
}

/**
 * 触发浏览器下载解析后的模板
 */
export const downloadAITemplate = (fullData: any, fileName: string) => {
  try {
    let safeFileName = fileName.replace(/[^\u4e00-\u9fa5a-zA-Z0-9_-]/g, '').trim();
    if (!safeFileName) safeFileName = 'ai_ppt_template';

    const templateData = extractToAITemplate(fullData);
    const blob = new Blob([templateData], { type: 'application/json' });
    saveAs(blob, `${safeFileName}_ai_template.json`);
    
    console.log(`[AI解析工具] 转换成功: ${safeFileName}_ai_template.json`);
  } catch (err) {
    console.error('[AI解析工具] 导出严重错误:', err);
    alert('导出解析过程中断，查看控制台输出以获取错误详情。');
  }
}

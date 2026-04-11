import { nanoid } from 'nanoid'
import { templateDB, type LocalTemplate } from '@/utils/database'
import type { Slide, SlideType, SlideTheme } from '@/types/slides'
import message from '@/utils/message'

export default () => {
  /**
   * 自动根据幻灯片内容猜测其类型
   * @param slide 幻灯片页面
   * @param index 页面索引
   * @param total 总页面数
   */
  const autoLabelSlide = (slide: Slide, index: number, total: number): SlideType => {
    // 第一页默认为封面
    if (index === 0) return 'cover'
    // 最后一页默认为结束页
    if (index === total - 1) return 'end'

    const elements = slide.elements
    // 统计文本内容（去除HTML标签）
    const textContent = elements
      .filter(el => el.type === 'text')
      .map(el => (el as any).content.replace(/<[^>]+>/g, ''))
      .join(' ')

    // 目录关键词判定
    if (/目录|contents|index/i.test(textContent)) return 'contents'
    
    // 元素数量判定：如果页面元素非常少，可能是过渡页
    if (elements.length > 0 && elements.length <= 3) {
      return 'transition'
    }

    // 默认作为内容页
    return 'content'
  }

  /**
   * 保存模板到本地数据库
   * @param name 模板名称
   * @param cover 封面图（base64）
   * @param slides 幻灯片数据
   * @param theme 主题数据
   */
  const saveLocalTemplate = async (name: string, cover: string, slides: Slide[], theme: SlideTheme) => {
    const newTemplate: LocalTemplate = {
      id: nanoid(10),
      name,
      cover,
      slides,
      theme,
      time: new Date().getTime(),
    }
    await templateDB.templates.put(newTemplate)
    message.success('模板保存成功')
    return newTemplate
  }

  /**
   * 获取所有本地模板
   */
  const getLocalTemplates = async () => {
    return await templateDB.templates.orderBy('time').reverse().toArray()
  }

  /**
   * 删除本地模板
   * @param id 模板ID
   */
  const deleteLocalTemplate = async (id: string) => {
    await templateDB.templates.delete(id)
    message.success('模板已删除')
  }

  return {
    autoLabelSlide,
    saveLocalTemplate,
    getLocalTemplates,
    deleteLocalTemplate,
  }
}

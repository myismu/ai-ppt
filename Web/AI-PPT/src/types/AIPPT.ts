export interface AIPPTCover {
  type: 'cover'
  data: {
    title: string
    text: string
  }
  remark?: string
}

export interface AIPPTContents {
  type: 'contents'
  data: {
    items: string[]
  }
  offset?: number
  remark?: string
}

export interface AIPPTTransition {
  type: 'transition'
  data: {
    title: string
    text: string
  }
  remark?: string
}

export interface AIPPTContent {
  type: 'content'
  data: {
    title: string
    items: {
      title: string
      text: string
    }[]
  },
  offset?: number
  remark?: string
}

export interface AIPPTEnd {
  type: 'end'
}

export type AIPPTSlide = AIPPTCover | AIPPTContents | AIPPTTransition | AIPPTContent | AIPPTEnd
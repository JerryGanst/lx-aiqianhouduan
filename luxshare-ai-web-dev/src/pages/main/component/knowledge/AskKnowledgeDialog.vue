<template>
  <div class="directory_chat" :class="{ maximized: isMaximized, 'initial-hidden': isInitialHidden }" ref="chatRoot">
    <div class="ask_top">
      <div class="left_title">问知识库</div>
      <div class="right_methods">
        <div class="clear_msg" @mouseenter="showClearTip = true" @mouseleave="showClearTip = false" @click="clearChatHistory">
          <img src="@/assets/knowledgeBase/clear_chat.png" style="width: 16.08px; height: 16.08px" />
          <transition name="fade">
            <div v-if="showClearTip" class="tooltip-bottom">清空对话</div>
          </transition>
        </div>
        <div
          class="max_chat_box"
          @click="toggleMaximize"
          @mouseenter="showMaxTip = true"
          @mouseleave="showMaxTip = false"
        >
          <img :src="isMaximized ? toRestoreIcon : toMaxIcon" style="width: 16.08px; height: 16.08px" />
          <transition name="fade">
            <div v-if="showMaxTip" class="tooltip-bottom">{{ isMaximized ? '还原' : '最大化' }}</div>
          </transition>
        </div>
        <div
          class="close_chat_box"
          @mouseenter="showCloseTip = true"
          @mouseleave="showCloseTip = false"
          @click="handleClose"
        >
          <img src="@/assets/knowledgeBase/close.png" style="width: 16.08px; height: 16.08px" />
          <transition name="fade">
            <div v-if="showCloseTip" class="tooltip-bottom">关闭</div>
          </transition>
        </div>
      </div>
    </div>
    <div class="chat_area" v-if="dirChatCurrent.messages.length > 0">
      <div class="ask_chat_list" v-for="(item, index) in dirChatCurrent.messages" :key="index">
        <div class="ask_message" v-if="index % 2 === 0">
          <div class="ask_message__tags" v-if="item.selectedTags && item.selectedTags.length">
            <AskKnowledgeSelectedTag
              v-for="tag in item.selectedTags"
              :key="tag.id || tag.name"
              :tag-name="tag.name"
            />
          </div>
          <div class="ask_message__content">{{ item.content }}</div>
        </div>
        <div class="answer_area" v-if="index % 2 !== 0 && item.thinking">
          <!-- 思考过程展示 -->
          <div v-if="item.thinking" class="thinking_area">
            <div class="left_robot">
              <img src="@/assets/knowledgeBase/robot.png" style="width: 52px; height: 52px;" />
            </div>
            <div class="right_content">
              {{item.thinking}}
            </div>
          </div>
          <!-- 回答内容 -->
          <MarkdownRenderer
              :markdown="item.after"
              class="normal-text"
              style="font-size: 18px; line-height: 1.6; background-color: #fafafa; color: #333"
          />
          <!-- 附件列表 -->
          <div v-if="item.sources && item.sources.length > 0" class="source_area">
            <div class="query_source">附件</div>
            <div v-for="(it, idx) in processedData(item.sources)" :key="idx" style="display: block">
              <a class="href_source" @click="toDoc(it)">
                {{ it.document_title }}(第{{ it.page.join('/') }}页)
              </a>
            </div>
          </div>
        </div>
        <div v-if="index === dirChatCurrent.messages.length - 1 && !isLoading && messageId" class="refresh_area">
          <div>
            <img src="@/assets/refresh.png" style="margin-left: 10px" class="query_common_img" @click="refreshData" />
          </div>
          <div>
            <img src="@/assets/up.png" @click="upCommon" class="query_common_img" style="margin-left: 15px" />
          </div>
          <div>
            <img src="@/assets/down.png" style="margin-left: 15px" @click="downCommon" class="query_common_img" />
          </div>
        </div>
      </div>
    </div>
    <div class="ask_input_area">
      <div
        class="ask-input-wrapper"
        :class="{ 'ask-input-wrapper--focused': isInputFocused }"
        @click="focusInput"
      >
        <AskKnowledgeSelectedTag
          v-for="tag in selectedTags"
          :key="tag.id"
          :tag-id="tag.id"
          :tag-name="tag.name"
          closable
          @remove="handleTagRemove"
        />
        <textarea
          ref="inputRef"
          v-model="inputText"
          class="ask-input-textarea"
          :placeholder="inputPlaceholder"
          @focus="handleInputFocus"
          @blur="handleInputBlur"
          @keydown="handleKeydown"
        ></textarea>
      </div>
      <!-- 发送图标 -->
      <div class="send-icon" :class="{ 'loading': isLoading }">
        <img
          :src="isLoading ? imageC : (inputText ? imageB : imageA)"
          class="arrow"
          @click="handleSendClick"
          :style="{ cursor: 'pointer' }"
        />
      </div>
    </div>
  </div>

  <AskKnowledgeTagPanel
    v-if="isTagPanelVisible"
    :visible="isTagPanelVisible"
    :position="tagPanelPosition"
    :selected-tag-ids="selectedTagIds"
    @select-tag="handleTagSelected"
    @request-close="onTagPanelClose"
    @availability-change="handleTagAvailabilityChange"
  />

  <!-- 评价弹窗 -->
  <el-dialog
    v-model="commonVisible"
    title="评价"
    width="500px"
    :before-close="handleCommonClose"
    style="border-radius: 10px"
  >
    <el-input
      v-model="commonQuestion"
      placeholder="请输入您的宝贵建议"
      :maxlength="4096"
      style="width: 100%"
      clearable
      type="textarea"
      rows="5"
    />
    <div class="button-item_common">
      <el-button @click="commonVisible = false" style="width: 100px; height: 40px; margin-left: 15px">取消</el-button>
      <el-button type="primary" @click="submitCommon" style="width: 100px; height: 40px">提交</el-button>
    </div>
  </el-dialog>

  <!-- 清空对话确认弹窗 -->
  <DeleteConfirmDialog
    v-model:visible="clearConfirmVisible"
    title="确定清空当前所有对话？"
    description="清空当前对话后，对话将无法恢复和找回，请谨慎操作"
    @confirm="executeClearChatHistory"
    @cancel="cancelClearChatHistory"
  />
</template>

<script setup>
import { reactive, ref, toRaw, nextTick, onMounted, onBeforeUnmount, computed } from 'vue'
import toMaxIcon from '@/assets/knowledgeBase/to_max.png'
import toRestoreIcon from '@/assets/knowledgeBase/to_min.png'
import imageC from '@/assets/stop.png'
import imageB from '@/assets/arrow_blue.png'
import imageA from '@/assets/arrow_gray.png'
import MarkdownRenderer from "@/pages/main/component/markdown.vue";
import { ElMessage } from "element-plus";
import { useShared } from "@/utils/useShared.js";
import { KnowledgeSelect } from "@/utils/common.js"
import { delay, isCompleteJSON } from "@/utils/common.js";
import request from '@/utils/request'
import eventBus from '@/utils/eventBus'
import DeleteConfirmDialog from '@/pages/main/component/options/deleteConfirmDialog.vue'
import AskKnowledgeTagPanel from '@/pages/main/component/knowledge/AskKnowledgeTagPanel.vue'
import AskKnowledgeSelectedTag from '@/pages/main/component/knowledge/AskKnowledgeSelectedTag.vue'

const props = defineProps({
  folderId: { type: [Number, String], default: 0 },
  folderName: { type: String, default: '问知识库' }
})

const emit = defineEmits(['close'])

const showClearTip = ref(false)
const showCloseTip = ref(false)
const showMaxTip = ref(false)
const isMaximized = ref(false)
const inputText = ref('')
const inputRef = ref(null)
const messageId = ref('')
// 初始化隐藏除输入框外的其它区域
const isInitialHidden = ref(true)
// 清空对话确认弹窗
const clearConfirmVisible = ref(false)

// 新增状态变量
const isDisabled = ref(false)
const commonVisible = ref(false)
const commonQuestion = ref('')
let lastRefreshTime = 0

const isTagPanelVisible = ref(false)
const tagPanelPosition = reactive({ x: 0, y: 0 })
const selectedTags = ref([])
const lastHashIndex = ref(null)
const isInputFocused = ref(false)
const hasTagOptions = ref(true)

const selectedTagIds = computed(() => selectedTags.value.map(tag => tag.id))
const inputPlaceholder = computed(() =>
  hasTagOptions.value ? '输入#，可指定标签问答' : '基于知识库提问'
)

const focusInput = () => {
  const el = inputRef.value
  if (el && typeof el.focus === 'function') {
    el.focus()
  }
}

const handleInputFocus = () => {
  isInputFocused.value = true
}

const handleInputBlur = () => {
  isInputFocused.value = false
}

const normalizeTagId = (id) => (id ?? '').toString()

const collectSelectedTagIds = (items) => {
  const result = []
  const visited = new Set()

  const traverse = (value) => {
    if (!value) return

    if (Array.isArray(value)) {
      value.forEach(traverse)
      return
    }

    if (typeof value === 'object') {
      if (Object.prototype.hasOwnProperty.call(value, 'id')) {
        const normalized = normalizeTagId(value.id)
        if (normalized && !visited.has(normalized)) {
          visited.add(normalized)
          result.push(normalized)
        }
      }

      Object.values(value).forEach(traverse)
    }
  }

  traverse(items)

  return result
}

const handleTagSelected = (item) => {
  if (!item) return
  const rawId = item?.targetId ?? item?.id ?? item?.targetName
  const tagName = item?.targetName ?? item?.name ?? ''
  if (!rawId) return

  const normalizedId = normalizeTagId(rawId)
  if (selectedTagIds.value.includes(normalizedId)) {
    return
  }

  selectedTags.value = [
    ...selectedTags.value,
    {
      id: normalizedId,
      rawId,
      name: tagName
    }
  ]

  if (lastHashIndex.value !== null) {
    const currentValue = inputText.value ?? ''
    inputText.value =
      currentValue.slice(0, lastHashIndex.value) + currentValue.slice(lastHashIndex.value + 1)

    nextTick(() => {
      const el = inputRef.value
      if (el && typeof el.setSelectionRange === 'function') {
        el.setSelectionRange(lastHashIndex.value, lastHashIndex.value)
      }
      focusInput()
    })
  } else {
    nextTick(() => {
      focusInput()
    })
  }

  lastHashIndex.value = null
}

const onTagPanelClose = () => {
  isTagPanelVisible.value = false
  lastHashIndex.value = null
}

const handleTagRemove = (id) => {
  const normalizedId = normalizeTagId(id)
  selectedTags.value = selectedTags.value.filter(tag => normalizeTagId(tag.id) !== normalizedId)
  nextTick(() => {
    focusInput()
  })
}

const handleTagAvailabilityChange = (available) => {
  const normalized = Boolean(available)
  hasTagOptions.value = normalized
  if (!normalized) {
    onTagPanelClose()
  }
}

const caretMirrorProperties = [
  'direction',
  'boxSizing',
  'width',
  'height',
  'overflowX',
  'overflowY',
  'borderTopWidth',
  'borderRightWidth',
  'borderBottomWidth',
  'borderLeftWidth',
  'paddingTop',
  'paddingRight',
  'paddingBottom',
  'paddingLeft',
  'fontStyle',
  'fontVariant',
  'fontWeight',
  'fontStretch',
  'fontSize',
  'fontSizeAdjust',
  'lineHeight',
  'fontFamily',
  'textAlign',
  'textTransform',
  'textIndent',
  'textDecoration',
  'letterSpacing',
  'wordSpacing',
  'tabSize',
  'MozTabSize'
]

let caretMirrorDiv = null

const getCaretViewportRect = (element, position) => {
  if (!element) return null
  const doc = element.ownerDocument
  const win = doc.defaultView || window
  const computed = win.getComputedStyle(element)

  if (!caretMirrorDiv) {
    caretMirrorDiv = doc.createElement('div')
    caretMirrorDiv.setAttribute('data-ask-knowledge-caret-mirror', 'true')
    doc.body.appendChild(caretMirrorDiv)
  }

  const mirror = caretMirrorDiv
  const style = mirror.style
  style.position = 'absolute'
  style.visibility = 'hidden'
  style.whiteSpace = 'pre-wrap'
  style.wordWrap = 'break-word'
  style.pointerEvents = 'none'

  caretMirrorProperties.forEach(prop => {
    style[prop] = computed[prop]
  })

  const rect = element.getBoundingClientRect()
  const scrollX = win.scrollX || win.pageXOffset || 0
  const scrollY = win.scrollY || win.pageYOffset || 0
  style.left = `${rect.left + scrollX}px`
  style.top = `${rect.top + scrollY}px`
  style.width = `${rect.width}px`
  style.height = `${rect.height}px`

  const value = element.value ?? ''
  mirror.textContent = value.slice(0, position)

  const span = doc.createElement('span')
  span.textContent = value.slice(position) || '.'
  mirror.appendChild(span)

  mirror.scrollTop = element.scrollTop
  mirror.scrollLeft = element.scrollLeft

  const spanRect = span.getBoundingClientRect()
  const result = {
    left: spanRect.left,
    top: spanRect.top,
    right: spanRect.right,
    bottom: spanRect.bottom
  }

  mirror.removeChild(span)
  mirror.textContent = ''

  return result
}

const showTagPanelAtCaret = (element) => {
  if (!element) return
  if (!hasTagOptions.value) {
    hideTagPanel()
    return
  }
  const selectionEnd = element.selectionEnd ?? element.selectionStart ?? 0
  const anchorIndex = Math.max(0, selectionEnd - 1)
  const value = element.value ?? ''
  if (value[anchorIndex] !== '#') return

  const caretRect = getCaretViewportRect(element, anchorIndex + 1)
  if (!caretRect) return

  const horizontalOffset = 4
  tagPanelPosition.x = caretRect.left + horizontalOffset
  tagPanelPosition.y = caretRect.top
  isTagPanelVisible.value = true
  lastHashIndex.value = anchorIndex
}

const hideTagPanel = () => {
  onTagPanelClose()
}

const toggleMaximize = () => {
  isMaximized.value = !isMaximized.value
}

const { userInfo, knowSelect } = useShared()

const isDepartmentKnowledge = computed(() => knowSelect.value === KnowledgeSelect.DEPARTMENT)

// 根容器和悬浮提示元素
const chatRoot = ref(null)
let citeTooltipEl = null

const ensureCiteTooltip = () => {
  if (citeTooltipEl) return citeTooltipEl
  citeTooltipEl = document.createElement('div')
  const s = citeTooltipEl.style
  s.position = 'fixed'
  s.zIndex = '10000'
  s.height = '44px'
  s.lineHeight = '44px'
  s.borderRadius = '8px'
  s.background = '#fff'
  s.border = '1px solid #d3d3d3'
  s.boxShadow = '0 4px 8px #b7b8b9'
  s.padding = '0 12px'
  s.whiteSpace = 'nowrap'
  s.pointerEvents = 'none'
  s.display = 'none'
  document.body.appendChild(citeTooltipEl)
  return citeTooltipEl
}

const showCiteTooltip = (anchor) => {
  const tip = ensureCiteTooltip()
  tip.textContent = anchor?.dataset?.full || ''
  tip.style.display = 'inline-block'
  // 先设置显示再测量宽度
  const aRect = anchor.getBoundingClientRect()
  const tRect = tip.getBoundingClientRect()
  let left = aRect.left + aRect.width / 2 - tRect.width / 2
  const margin = 8
  left = Math.max(margin, Math.min(left, window.innerWidth - tRect.width - margin))
  const top = aRect.bottom + 6
  tip.style.left = left + 'px'
  tip.style.top = top + 'px'
}

const hideCiteTooltip = () => {
  if (citeTooltipEl) citeTooltipEl.style.display = 'none'
}

const handleMouseOver = (e) => {
  const target = e.target
  if (!(target instanceof Element)) return
  const a = target.closest('a[data-full]')
  if (a && chatRoot.value && chatRoot.value.contains(a)) {
    showCiteTooltip(a)
  }
}

const handleMouseOut = (e) => {
  const target = e.target
  if (!(target instanceof Element)) return
  const a = target.closest('a[data-full]')
  if (a && chatRoot.value && chatRoot.value.contains(a)) {
    hideCiteTooltip()
  }
}

onMounted(() => {
  if (chatRoot.value) {
    chatRoot.value.addEventListener('mouseover', handleMouseOver)
    chatRoot.value.addEventListener('mouseout', handleMouseOut)
  }
})

onBeforeUnmount(() => {
  if (chatRoot.value) {
    chatRoot.value.removeEventListener('mouseover', handleMouseOver)
    chatRoot.value.removeEventListener('mouseout', handleMouseOut)
  }
  if (citeTooltipEl && citeTooltipEl.parentNode) {
    citeTooltipEl.parentNode.removeChild(citeTooltipEl)
    citeTooltipEl = null
  }
  if (caretMirrorDiv && caretMirrorDiv.parentNode) {
    caretMirrorDiv.parentNode.removeChild(caretMirrorDiv)
    caretMirrorDiv = null
  }
})

// 将 after 文本中的 {{CITE:id}} 替换为指向 sources 中文档链接的超链接
const getLastPathSegment = path => {
  if (!path) return ''
  const normalized = String(path).replace(/\\/g, '/').split('/')
  const last = normalized.filter(Boolean).pop()
  return last || ''
}

const replaceCitationsWithLinks = (text, sources = []) => {
  if (!text) return ''
  // 在进行 CITE 替换前，为裸露 URL 两侧插入空格，防止其与中文或标点连在一起
  const addSpacesAroundUrls = (str) => {
    let s = String(str)
    // URL 主体字符：排除空白和常见中英标点与括号，避免把“，。”“）”等误并入 URL
    const urlBody = "[^\\s<>,，。；;：:！!？?、（）()\\[\\]{}]+"
    const urlHead = new RegExp(`(^|[^\\s])((?:https?:\\/\\/)${urlBody})`, 'g')
    const urlTail = new RegExp(`((?:https?:\\/\\/)${urlBody})(?=[^\\s])`, 'g')
    // 若 URL 前一个字符不是空白，则在前面插入一个空格（不在行首时）
    s = s.replace(urlHead, (m, pre, url) => (pre ? pre + ' ' : '') + url)
    // 若 URL 后面紧跟的不是空白（含中文标点等），在 URL 与其后字符之间插入空格
    s = s.replace(urlTail, '$1 ')
    return s
  }
  const spacedText = addSpacesAroundUrls(text)
  // 新规则：CITE 中的数字为 1 起始的下标映射到 sources 数组
  return String(spacedText).replace(/\{\{CITE:([\d,]+)\}\}/g, (_, ids) => {
    // 处理多个索引的情况，如 "1,3,5" → sources[0], sources[2], sources[4]
    const idArray = ids.split(',').map(id => id.trim())
    // 聚合同一文档的页码：key = 标题 + URL，值为 { title, url, pages:Set }
    const linkMap = new Map()

    idArray.forEach(id => {
      const index = Number(id) - 1
      if (!Number.isInteger(index) || index < 0 || index >= sources.length) return
      const src = sources[index] || {}
      const title = getLastPathSegment(src.document_title)
      if (!title) return
      // 优先使用直链，缺失时也生成一个可点击的占位链接
      const url = (src.fileUrl || src.file_url) || `doc://${encodeURIComponent(title)}`

      const key = `${title}||${url}`
      if (!linkMap.has(key)) {
        linkMap.set(key, { title, url, pages: new Set() })
      }
      const entry = linkMap.get(key)
      const pageVal = src.page
      if (Array.isArray(pageVal)) {
        pageVal.forEach(p => {
          if (p !== undefined && p !== null) entry.pages.add(p)
        })
      } else if (pageVal !== undefined && pageVal !== null) {
        entry.pages.add(pageVal)
      }
    })

    const links = []
    linkMap.forEach(entry => {
      const pageArr = Array.from(entry.pages).sort((a, b) => a - b)
      const pageSuffix = pageArr.length > 0 ? ` (第${pageArr.join('/')}页)` : ''
      const firstPage = pageArr.length > 0 ? pageArr[0] : 1
      let wrappedUrl = entry.url
      if (entry.url && /^https?:/i.test(entry.url)) {
        const pick = (s) => (s || '').split('?')[0].split('#')[0]
        let ext = ''
        try {
          const target = decodeURIComponent(pick(entry.url)) || decodeURIComponent(pick(entry.title))
          const m = target.match(/\.([a-zA-Z0-9]+)$/)
          ext = m ? m[1].toLowerCase() : ''
        } catch (_) { ext = '' }
        if (ext === 'ppt' || ext === 'pptx') {
          wrappedUrl = `/ppt-viewer.html?src=${encodeURIComponent(entry.url)}&page=${encodeURIComponent(firstPage)}`
        } else if (ext === 'pdf') {
          wrappedUrl = `/pdf-viewer.html?src=${encodeURIComponent(entry.url)}&page=${encodeURIComponent(firstPage)}`
        } else {
          wrappedUrl = entry.url
        }
      }
      links.push(`[${entry.title}${pageSuffix}](${wrappedUrl})`)
    })

    // 用逗号连接多个链接
    return links.join(', ')
  })
}

// 不合并页码的 CITE 替换：同一文档不同页各自生成一个独立链接，链接之间空格分隔
const replaceCitationsWithLinksNoMerge = (text, sources = []) => {
  if (!text) return ''
  const addSpacesAroundUrls = (str) => {
    let s = String(str)
    const urlBody = "[^\\s<>,，。；;：:！!？?、（）()\\[\\]{}]+"
    const urlHead = new RegExp(`(^|[^\\s])((?:https?:\\/\\/)${urlBody})`, 'g')
    const urlTail = new RegExp(`((?:https?:\\/\\/)${urlBody})(?=[^\\s])`, 'g')
    s = s.replace(urlHead, (m, pre, url) => (pre ? pre + ' ' : '') + url)
    s = s.replace(urlTail, '$1 ')
    return s
  }
  const spacedText = addSpacesAroundUrls(text)
  return String(spacedText).replace(/\{\{CITE:([\d,]+)\}\}/g, (_, ids) => {
    const idArray = ids.split(',').map(id => id.trim())
    const links = []
    idArray.forEach(id => {
      const index = Number(id) - 1
      if (!Number.isInteger(index) || index < 0 || index >= sources.length) return
      const src = sources[index] || {}
      const title = getLastPathSegment(src.document_title)
      const baseUrl = src.fileUrl || src.file_url || ''
      const pages = Array.isArray(src.page) ? src.page : [src.page]
      const pick = (s) => (s || '').split('?')[0].split('#')[0]
      let ext = ''
      if (baseUrl && /^https?:/i.test(baseUrl)) {
        try {
          const target = decodeURIComponent(pick(baseUrl)) || decodeURIComponent(pick(title))
          const m = target.match(/\.([a-zA-Z0-9]+)$/)
          ext = m ? m[1].toLowerCase() : ''
        } catch (_) { ext = '' }
      }
      pages.filter(p => p !== undefined && p !== null).forEach(p => {
        let url = baseUrl
        if (baseUrl && /^https?:/i.test(baseUrl)) {
          if (ext === 'ppt' || ext === 'pptx') {
            url = `/ppt-viewer.html?src=${encodeURIComponent(baseUrl)}&page=${encodeURIComponent(p)}`
          } else if (ext === 'pdf') {
            url = `/pdf-viewer.html?src=${encodeURIComponent(baseUrl)}&page=${encodeURIComponent(p)}`
          }
        }
        // 仅展示页码数字作为链接文本，同时在 data-full 存完整提示文案
        const fullText = `${title ? title : ''}（第${p}页）`
        const esc = (s) => String(s)
          .replace(/&/g, '&amp;')
          .replace(/"/g, '&quot;')
          .replace(/</g, '&lt;')
          .replace(/>/g, '&gt;')
        links.push(`<a href="${esc(url || '#')}" class="kb-cite" data-full="${esc(fullText)}">${esc(p)}</a>`)
      })
    })
    return links.join(' ')
  })
}

const handleClose = () => {
  emit('close')
}

let isLoading = ref(false)
let abortController = ref(null)
// 文件聊天参数
const dirChatQuery = reactive({
  //通用模式数据对象
  messages: []
})

// dirChatQueryCpy的全权代表
const dirChatCurrent = reactive({
  //通用模式数据对象
  messages: []
})

// 设置流式入参
const setStreamValues = streamInput => {
  for (let j = 0; j < streamInput.messages.length; j++) {
    if (j % 2 === 0) {
      streamInput.messages[j].role = 'user'
    } else {
      streamInput.messages[j].role = 'assistant'
    }
  }
  streamInput.userId = userInfo.value.id
  streamInput.sessionId = 'empty'
  streamInput.model = 0
  streamInput.personalKnowledgeBase = !isDepartmentKnowledge.value
  if (isDepartmentKnowledge.value) {
    streamInput.departmentKnowledgeBase = true
    streamInput.chatType = 'department_partial'
  } else {
    streamInput.chatType = 'partial'
  }
  streamInput.files = []
  streamInput.folderId = props.folderId
}

// sources 处理：按文件名去重并聚合页码（与 @index.vue 行为一致）
const processedData = sources => {
  const result = []
  const map = new Map()
  if (!Array.isArray(sources)) return result
  sources.forEach(item => {
    const title = item.document_title || ''
    const key = `${title}` // 仅按标题合并，忽略 document_id 差异
    if (!map.has(key)) {
      map.set(key, {
        document_title: title,
        file_url: item.fileUrl || item.file_url || '',
        page: new Set()
      })
    }
    map.get(key).page.add(item.page)
    // 以最新的 file_url 覆盖为空的旧值
    const ref = map.get(key)
    if (!ref.file_url && (item.fileUrl || item.file_url)) {
      ref.file_url = item.fileUrl || item.file_url
    }
  })
  map.forEach(value => {
    result.push({
      document_title: value.document_title.includes('/')
        ? value.document_title.slice(value.document_title.lastIndexOf('/') + 1)
        : value.document_title,
      page: Array.from(value.page).sort((a, b) => a - b),
      file_url: value.file_url
    })
  })
  // 保持“附件”区域为合并显示（同一文档多页合并为 1 条，page 为数组）
  return result
}

// 打开文档链接
const toDoc = data => {
  if (!data || !data.file_url) return
  const pageList = Array.isArray(data.page) ? data.page : [data.page].filter(v => v !== undefined && v !== null)
  const firstPage = pageList.length > 0 ? pageList[0] : 1
  // 根据扩展名选择合适的预览器（pdf 或 ppt/pptx）
  const getExt = (url, title) => {
    try {
      const u = decodeURIComponent((url || '').split('?')[0])
      const t = decodeURIComponent((title || '').split('?')[0])
      const target = u || t || ''
      const m = target.match(/\.([a-zA-Z0-9]+)$/)
      return m ? m[1].toLowerCase() : ''
    } catch (_) {
      return ''
    }
  }
  const ext = getExt(data.file_url, data.document_title)
  let viewer = ''
  if (ext === 'pdf') {
    viewer = `/pdf-viewer.html?src=${encodeURIComponent(data.file_url)}&page=${encodeURIComponent(firstPage)}`
  } else if (ext === 'ppt' || ext === 'pptx') {
    viewer = `/ppt-viewer.html?src=${encodeURIComponent(data.file_url)}&page=${encodeURIComponent(firstPage)}`
  } else {
    viewer = data.file_url
  }
  window.open(viewer, '_blank')
}

// 保存目录聊天记录（参考 KnowledgeDrawer.vue）
const saveChatRecord = () => {
  try {
    const firstUserMessage = dirChatQuery.messages.find(msg => msg.role === 'user')
    const titleStr = firstUserMessage?.content || dirChatQuery.messages?.[0]?.content || ''
    request
      .post('/Message/save', {
        userId: userInfo.value.id,
        type: '通用模式',
        id: messageId.value || '',
        data: dirChatQuery.messages,
        isThink: false,
        title: titleStr
      })
      .then(res => {
        if (res.status) {
          messageId.value = res.data
          eventBus.emit('fetchChatList', '')
        }
      })
      .catch(() => {})
  } catch (_) {}
}

// 清空对话
const clearChatHistory = () => {
  // 显示确认弹窗
  clearConfirmVisible.value = true
}

// 执行清空对话操作
const executeClearChatHistory = () => {
  // 如果有正在进行的请求，先中止它
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
  isLoading.value = false
  dirChatQuery.messages = []
  dirChatCurrent.messages = []
  messageId.value = ''
}

// 取消清空对话
const cancelClearChatHistory = () => {
  clearConfirmVisible.value = false
}

// 点赞
const upCommon = async () => {
  if (!userInfo.value || !userInfo.value.id) {
    ElMessage.warning('请先登录再使用')
    return
  }
  if (isDisabled.value) return
  isDisabled.value = true
  setTimeout(() => {
    isDisabled.value = false
  }, 3000)

  request
    .post('/Message/feedback', {
      id: messageId.value,
      feedback: { agree: true, content: '' }
    })
    .then(res => {
      if (res.status) {
        ElMessage.success('谢谢您的点赞,您的支持是我们最大的动力！')
      }
    })
    .catch(() => {})
}

// 点踩弹窗
const downCommon = () => {
  commonVisible.value = true
}

// 提交点踩评价
const submitCommon = async () => {
  if (!userInfo.value || !userInfo.value.id) {
    ElMessage.warning('请先登录再使用')
    return
  }
  request
    .post('/Message/feedback', {
      id: messageId.value,
      feedback: { agree: false, content: commonQuestion.value }
    })
    .then(res => {
      if (res.status) {
        ElMessage.success('评价成功,我们会继续努力的！')
        commonVisible.value = false
        commonQuestion.value = ''
      }
    })
    .catch(() => {
      commonVisible.value = false
      commonQuestion.value = ''
    })
}

const handleCommonClose = done => {
  done()
}

// 刷新功能
const refreshData = () => {
  const now = Date.now()

  // Check if less than 1.5 seconds have passed since last click
  if (now - lastRefreshTime < 1500) {
    return
  }

  lastRefreshTime = now
  if (isLoading.value) {
    ElMessage.warning('有问答正在进行中,请稍后再试')
    return
  }
  
  // 获取最后一个用户消息的内容
  const lastUserMessage = dirChatQuery.messages[dirChatQuery.messages.length - 2]
  if (!lastUserMessage || !lastUserMessage.content) {
    ElMessage.warning('没有可刷新的内容')
    return
  }
  
  // 重新发送最后一个用户消息
  inputText.value = lastUserMessage.content
  handleSendClick(true)
}

const handleKeydown = event => {
  if (event.key === '#') {
    requestAnimationFrame(() => {
      const target = event.target instanceof HTMLTextAreaElement
        ? event.target
        : inputRef.value
      showTagPanelAtCaret(target)
    })
    return
  }

  if (event.key === 'Escape') {
    hideTagPanel()
  }

  if (event.key === 'Backspace') {
    const target = event.target instanceof HTMLTextAreaElement ? event.target : inputRef.value
    if (target && target.selectionStart === 0 && target.selectionEnd === 0) {
      if (selectedTags.value.length > 0) {
        event.preventDefault()
        const lastTag = selectedTags.value[selectedTags.value.length - 1]
        handleTagRemove(lastTag?.id)
      }
      return
    }
  }

  if (event.key === 'Enter' && event.shiftKey) {
    return
  }
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    // 如果正在加载中，提示用户不应发送消息
    if (isLoading.value) {
      ElMessage.warning('有问答正在进行中,请稍后再试')
      return
    }
    handleSendClick()
    return
  }
  if ((event.ctrlKey || event.metaKey) && (event.key === 'a' || event.key === 'A')) {
    event.preventDefault()
    const el = inputRef.value ?? event.target
    if (el && typeof el.select === 'function') {
      el.select()
    }
  }
}


const handleBuffer = buffer => {
  if (isCompleteJSON(buffer)) {
    try {
      const jsonData = JSON.parse(buffer)
      const serverMessage = jsonData?.message || jsonData?.msg
      if (serverMessage) {
        ElMessage.error(serverMessage)
      } else if (jsonData.code === 400) {
        ElMessage.error('文本过长，请重新尝试')
      } else {
        ElMessage.error('文本异常,请稍后再试')
      }
    } catch (e) {
      ElMessage.error('数据格式异常')
    }
    isLoading.value = false
    // 缓冲区错误时也要保存已经输出的流式数据
    if (dirChatCurrent.messages.length > 0) {
      // 临时保存当前状态到 dirChatQuery，然后保存
      const tempMessages = [...dirChatCurrent.messages]
      dirChatQuery.messages = tempMessages
      saveChatRecord()
    }
  }
}

// 从 buffer 中稳健提取以 `data:` 开头的 JSON 事件
const extractSSEEventsFromBuffer = (buf) => {
  const events = []
  if (!buf) return { events, rest: '' }
  let i = 0
  while (true) {
    const start = buf.indexOf('data:', i)
    if (start === -1) break
    let j = start + 5
    // 跳过空白，定位到第一个 '{'
    while (j < buf.length && /\s/.test(buf[j])) j++
    const open = buf.indexOf('{', j)
    if (open === -1) {
      // 没有出现 '{'，说明 JSON 不完整，保留剩余部分等待下次
      i = start
      break
    }
    // 以括号配平的方式提取完整 JSON，处理字符串与转义
    let depth = 0
    let inString = false
    let escape = false
    let end = -1
    for (let k = open; k < buf.length; k++) {
      const ch = buf[k]
      if (inString) {
        if (escape) {
          escape = false
        } else if (ch === '\\') {
          escape = true
        } else if (ch === '"') {
          inString = false
        }
      } else {
        if (ch === '"') {
          inString = true
        } else if (ch === '{') {
          depth++
        } else if (ch === '}') {
          depth--
          if (depth === 0) {
            end = k
            break
          }
        }
      }
    }
    if (end === -1) {
      // JSON 未闭合，保留从 data: 开始的部分，下轮再拼
      i = start
      break
    }
    const jsonStr = buf.slice(open, end + 1)
    events.push(jsonStr)
    i = end + 1
  }
  const rest = buf.slice(i)
  return { events, rest }
}

// 应用后端事件到 assistant 消息，并同步视图
const applyServerEventToAssistant = async (payload, assistantMsg, dirChatQueryCpy) => {
  const { content, type, sources } = payload || {}
  if (type === 'reasoning') {
    assistantMsg.before += content || ''
  } else if (type === 'streaming') {
    assistantMsg.after += content || ''
    assistantMsg.after = replaceCitationsWithLinksNoMerge(assistantMsg.after, assistantMsg.sources)
  } else if (type === 'process') {
    assistantMsg.thinking = content || ''
    await delay(250)
  } else if (type === 'final_answer') {
    if (Array.isArray(sources) && sources.length > 0) {
      assistantMsg.sources = sources
    }
    assistantMsg.after = replaceCitationsWithLinksNoMerge(content || '', assistantMsg.sources)
  }

  // 同步视图（覆盖最后一条 assistant）
  dirChatQueryCpy.messages.splice(-1, 1, {
    ...toRaw(assistantMsg),
    before: assistantMsg.before,
    after: assistantMsg.after,
    content: (assistantMsg.before || '') + (assistantMsg.after || ''),
    sources: assistantMsg.sources,
    thinking: assistantMsg.thinking
  })

  // 驱动实时渲染与滚动
  dirChatCurrent.messages = [...dirChatQueryCpy.messages]
  nextTick(() => {
    const chatArea = document.querySelector('.chat_area')
    if (chatArea) {
      chatArea.scrollTop = chatArea.scrollHeight
    }
  })
}

const handleSteamResult = async (streamResult, assistantMsg, dirChatQueryCpy) => {
  const reader = streamResult.body.getReader()
  const decoder = new TextDecoder() // 启用流模式解码
  let buffer = '' // 缓冲区用于存储不完整的数据
  try {
    while (true) {
      const { value, done } = await reader.read()
      if (done) {
        // 结束前处理 buffer 中可能残留的事件
        if (buffer && buffer.trim()) {
          const { events, rest } = extractSSEEventsFromBuffer(buffer)
          for (const jsonStr of events) {
            try {
              const payload = JSON.parse(jsonStr)
              await applyServerEventToAssistant(payload, assistantMsg, dirChatQueryCpy)
            } catch (e) {
              console.error('JSON 解析失败(END):', jsonStr, '错误:', e)
              ElMessage.error('数据格式异常')
            }
          }
          if (rest && rest.trim()) {
            handleBuffer(rest)
          }
        }
        // 从cpy拷贝来的拷贝回去
        dirChatQuery.messages = JSON.parse(JSON.stringify(dirChatQueryCpy.messages))
        isLoading.value = false
        // 结束后确保滚动到底部
        await nextTick(() => {
          const chatArea = document.querySelector('.chat_area')
          if (chatArea) {
            chatArea.scrollTop = chatArea.scrollHeight
          }
        })
        // 保存对话
        saveChatRecord()
        break
      }
      buffer += decoder.decode(value, { stream: true })
      const { events, rest } = extractSSEEventsFromBuffer(buffer)
      buffer = rest
      for (const jsonStr of events) {
        try {
          const payload = JSON.parse(jsonStr)
          await applyServerEventToAssistant(payload, assistantMsg, dirChatQueryCpy)
        } catch (e) {
          console.error('JSON 解析失败:', jsonStr, '错误:', e)
          ElMessage.error('数据格式异常')
        }
      }
      if (buffer && buffer.trim()) {
        // 可能是非 SSE 的完整错误 JSON，尝试处理
        handleBuffer(buffer)
      }
    }
  } catch (error) {
    if (error.name === 'AbortError') {
      // 请求被中止，不需要显示错误信息（因为 handleStopRequest 已经显示了）
      console.log('请求被中止')
      // 中止时也要保存已经输出的流式数据
      if (dirChatCurrent.messages.length > 0) {
        // 临时保存当前状态到 dirChatQuery，然后保存
        const tempMessages = [...dirChatCurrent.messages]
        dirChatQuery.messages = tempMessages
        saveChatRecord()
      }
    } else {
      console.error('流式处理错误:', error)
      ElMessage.error('数据处理异常')
      // 错误时也要保存已经输出的流式数据
      if (dirChatCurrent.messages.length > 0) {
        // 临时保存当前状态到 dirChatQuery，然后保存
        const tempMessages = [...dirChatCurrent.messages]
        dirChatQuery.messages = tempMessages
        saveChatRecord()
      }
    }
    isLoading.value = false
  }
}

const handleSendClick = async (isRefresh = false) => {
  hideTagPanel()
  // 如果正在加载中，则终止请求
  if (isLoading.value) {
    handleStopRequest()
    return
  }
  // 验证输入内容
  if (!inputText.value || !inputText.value.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  const selectedTagSnapshot = toRaw(selectedTags.value).map(tag => ({
    id: tag?.id ?? '',
    rawId: tag?.rawId ?? '',
    name: tag?.name ?? ''
  })).filter(tag => tag.id || tag.name)

  const selectedTagIdList = collectSelectedTagIds(selectedTagSnapshot)

  // 开启加载状态
  isLoading.value = true
  // 创建新的 AbortController
  abortController.value = new AbortController()
  // 用户点击发送后，恢复弹窗完整显示
  isInitialHidden.value = false
  const inputTextCpy = inputText.value
  inputText.value = ''
  selectedTags.value = []
  lastHashIndex.value = null
  // 当前发送的消息
  const currentDirData = {
    role: 'user',
    content: inputTextCpy || '',
    files: [],
    personalKnowledge: !isDepartmentKnowledge.value,
    selectedTags: selectedTagSnapshot
  }
  // 复制文件聊天参数
  let dirChatQueryCpy = JSON.parse(JSON.stringify(dirChatQuery))
  
  // 如果是刷新操作，删除最后两个消息（用户消息和助手回复）
  if (isRefresh && dirChatQueryCpy.messages.length > 1) {
    dirChatQueryCpy.messages.splice(-2, 2)
    // 不重置 messageId，使用上一次 save 返回的 id 值
  }
  
  dirChatQueryCpy.messages.push(currentDirData)
  // 封装流式入参(奇数个入参：之前的问答+当前问题)
  const streamInput = JSON.parse(JSON.stringify(dirChatQueryCpy))
  if (selectedTagIdList.length > 0) {
    streamInput.tagList = selectedTagIdList
  }
  setStreamValues(streamInput)

  const assistantMsg = {
    role: 'assistant',
    content: '',
    before: '',
    after: '',
    sources: [],
    thinking: ''
  }
  // save的入参加上当前的回答
  dirChatQueryCpy.messages.push(assistantMsg)
  // 把current与cpy关联起来 后面对临时变量cpy的操作就是对current的操作
  dirChatCurrent.messages = dirChatQueryCpy.messages
  // 首次渲染后滚动到底部
  nextTick(() => {
    const chatArea = document.querySelector('.chat_area')
    if (chatArea) {
      chatArea.scrollTop = chatArea.scrollHeight
    }
  })

  try {
    // 直接使用 fetch API 获取流式响应
    const res = await fetch(import.meta.env.VITE_API_BASE_URL + '/AI/unifiedChat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(streamInput),
      signal: abortController.value.signal // 添加 abort signal
    })

    if (res.status === 429) {
      isLoading.value = false
      ElMessage.error('服务器繁忙,请稍后再试')
      return
    }

    // 非 2xx 响应时，读取并展示后端返回的错误 message
    if (!res.ok) {
      let errorText = ''
      try {
        errorText = await res.text()
      } catch (_) {}
      let errorJson = null
      try {
        errorJson = JSON.parse(errorText)
      } catch (_) {}
      const serverMessage = errorJson?.message || errorJson?.msg || '请求失败，请稍后重试'
      ElMessage.error(serverMessage)
      // 错误时也要保存已经输出的流式数据
      if (dirChatCurrent.messages.length > 0) {
        const tempMessages = [...dirChatCurrent.messages]
        dirChatQuery.messages = tempMessages
        saveChatRecord()
      }
      isLoading.value = false
      return
    }

    await handleSteamResult(res, assistantMsg, dirChatQueryCpy)
  } catch (error) {
    if (error.name === 'AbortError') {
      // 请求被中止，不需要显示消息（因为 handleStopRequest 已经显示了）
      // 中止时也要保存已经输出的流式数据
      if (dirChatCurrent.messages.length > 0) {
        // 临时保存当前状态到 dirChatQuery，然后保存
        const tempMessages = [...dirChatCurrent.messages]
        dirChatQuery.messages = tempMessages
        saveChatRecord()
      }
    } else {
      ElMessage.error('请求失败，请稍后重试')
      // 错误时也要保存已经输出的流式数据
      if (dirChatCurrent.messages.length > 0) {
        // 临时保存当前状态到 dirChatQuery，然后保存
        const tempMessages = [...dirChatCurrent.messages]
        dirChatQuery.messages = tempMessages
        saveChatRecord()
      }
    }
    isLoading.value = false
  }
}

// 终止当前请求并保存已有内容
const handleStopRequest = () => {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
  isLoading.value = false
  // 终止时也要保存已经输出的流式数据
  if (dirChatCurrent.messages.length > 0) {
    const tempMessages = [...dirChatCurrent.messages]
    dirChatQuery.messages = tempMessages
    saveChatRecord()
  }
  ElMessage.success('请求已中止')
}

// 暴露方法给父组件
const enterTransparentMode = () => {
  // 仅进入透明态，保留输入区可交互，满足“关闭但保留输入框”的需求
  isInitialHidden.value = true
}

defineExpose({ clearChatHistory, executeClearChatHistory, enterTransparentMode })
</script>

<style scoped lang="less">
.left_title {
  font-size: 16px;
  color: #333333
}
.refresh_area {
  margin-top: 20px;
  width: 100%;
  display: flex;

  .query_common_img {
    width: 18px;
    height: 18px;
    cursor: pointer;
  }
}
.chat_area {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
  width: 862px;
  margin: 0 auto 10px;
}

.chat_area::-webkit-scrollbar {
  display: none; /* Chrome, Safari and Opera */
}

.ask_message {
  background-color: #1b6cff;
  border-radius: 10px;
  padding: 13px 15px;
  float: right;
  color: #fff;
  max-width: 552px;
  overflow: hidden;
  text-overflow: ellipsis;
  margin: 10px 4px 10px auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ask_message__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
}

.ask_message__tags :deep(.ask-knowledge-selected-tag__text) {
  color: #ffffff;
}

.ask_message__content {
  word-break: break-word;
}

.answer_area {
  clear: both;
  border-radius: 10px;
  width: 852px;
  box-sizing: border-box;
  margin: 10px 4px 10px auto;
}

.thinking_area {
  margin-bottom: 10px;
  padding: 8px 0;
  display: flex;
  align-items: center;
  img {
    margin-right: 10px;
  }
  .right_content {
    color: #868686;
  }
}
.directory_chat {
  position: absolute;
  bottom: 53px;
  left: 50%;
  transform: translateX(-50%);
  width: 896px;
  height: 80vh;
  background: #fff;
  filter: drop-shadow(0 4px 16px #b7b8b94d);
  z-index: 10;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  padding-bottom: 0;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);

  // 初始化完全透明（保留输入区域可见与可交互）
  &.initial-hidden {
    background: transparent;
    filter: none;
    box-shadow: none;
    border: none;
    pointer-events: none; // 允许点击穿透容器

    .ask_top,
    .chat_area,
    .refresh_area {
      opacity: 0;
      pointer-events: none;
    }

    // 仅保留输入区域可交互
    .ask_input_area {
      pointer-events: auto;
    }
  }

  // 最大化状态样式
  &.maximized {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    width: calc(100vw - 316px); // 减去左侧导航栏宽度
    height: 100vh;
    margin-left: 316px; // 左侧导航栏宽度
    transform: none;
    border-radius: 0;
    z-index: 9999;
  }

  .ask_top {
    width: auto;
    height: 24px;
    display: flex;
    justify-content: space-between;

    .right_methods {
      display: flex;

      .clear_msg {
        width: 24px;
        height: 24px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 6px;
        cursor: pointer;
        position: relative;
        transition: background-color 0.2s ease;
      }

      .clear_msg:hover {
        background-color: #eee;
      }

      .max_chat_box {
        width: 24px;
        height: 24px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 6px;
        cursor: pointer;
        position: relative;
        transition: background-color 0.2s ease;
        margin-left: 16px;
      }

      .max_chat_box:hover {
        background-color: #eee;
      }

      .close_chat_box {
        width: 24px;
        height: 24px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 6px;
        cursor: pointer;
        position: relative;
        transition: background-color 0.2s ease;
        margin-left: 24px;
      }

      .close_chat_box:hover {
        background-color: #eee;
      }
    }

    margin: 24px 24px 10px;
  }

  .tooltip-bottom {
    position: absolute;
    top: calc(100% + 5px);
    left: 50%;
    transform: translateX(-50%);
    background: #000;
    color: white;
    padding: 6px 12px;
    border-radius: 4px;
    font-size: 14px;
    white-space: nowrap;
    z-index: 100;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  }

  .tooltip-bottom::after {
    content: '';
    position: absolute;
    bottom: 100%;
    left: 50%;
    transform: translateX(-50%);
    border: 5px solid transparent;
    border-bottom-color: #000;
  }

  .fade-enter-active,
  .fade-leave-active {
    transition: opacity 0.2s ease;
  }

  .fade-enter-from,
  .fade-leave-to {
    opacity: 0;
  }

  .ask_chat_area {
    margin-left: 24px;
    margin-right: 24px;
    margin-bottom: 10px;
    width: auto;
    flex: 1;
    transition: margin-bottom 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .ask_input_area {
    width: 862px;
    margin: 0 auto 24px;
    margin-top: auto;
    position: relative;
    transition: margin-bottom 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .ask-input-wrapper {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    width: 100%;
    min-height: 48px;
    padding: 12px 64px 12px 16px;
    box-sizing: border-box;
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    background: #fff;
    cursor: text;
    transition: border-color 0.2s ease, box-shadow 0.2s ease;
  }

  .ask-input-wrapper--focused {
    border-color: #1b6cff;
    box-shadow: 0 0 0 2px rgba(27, 108, 255, 0.15);
  }

  .ask-input-textarea {
    flex: 1;
    min-width: 120px;
    height: 48px;
    min-height: 48px;
    border: none;
    outline: none;
    resize: none;
    font-size: 16px;
    line-height: 24px;
    color: #333;
    font-family: inherit;
    padding: 12px 0;
    margin: 0;
    box-sizing: border-box;
  }

  .ask-input-textarea::placeholder {
    color: #b7b8b9;
  }

  // 最大化时调整聊天区域和输入区域
  &.maximized {
    .ask_chat_area {
      margin-bottom: 20px;
      transition: margin-bottom 0.5s cubic-bezier(0.4, 0, 0.2, 1);
    }

    .ask_input_area {
      margin-bottom: 20px;
      transition: margin-bottom 0.5s cubic-bezier(0.4, 0, 0.2, 1);
    }
  }
}

.send-icon {
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  display: flex;
}

.send-icon img {
  width: 24px;
  height: 24px;
  transition: opacity 0.2s ease;
}

.query_source {
  margin-top: 30px;
  padding: 0 10px;
}

.href_source {
  margin-top: 10px;
  color: #1b6cff;
  cursor: pointer;
  padding: 0 10px;
  font-size: 14px;
}

/* 评价弹窗样式 */
.button-item_common {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

/* Markdown 渲染内容中的超链接现代化样式 */
.normal-text :deep(a) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 8px;
  border-radius: 8px;
  color: #16a34a; /* 绿色：更易识别为可点击链接 */
  text-decoration: none;
  border: 1px solid #e5e7eb; /* 初始即显示浅灰边框 */
  background-color: transparent;
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
  word-break: break-word;
  font-size: 0.92em; /* 链接相对正文略小 */
}

.normal-text :deep(a:hover) {
  background-color: #ecfdf5; /* 浅绿色悬浮底色 */
  border-color: #86efac; /* 浅绿边框 */
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.normal-text :deep(a:hover) {
  color: #15803d; /* 深绿，悬浮更明显 */
}

.normal-text :deep(a:active) {
  background-color: #dcfce7; /* 浅绿 */
  border-color: #86efac;
}

.normal-text :deep(a:visited) {
  color: #15803d; /* 访问过的深绿 */
}

.normal-text :deep(a:focus-visible) {
  outline: 0;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.25);
}

/* 知识库内联引用链接样式覆盖（小圆角、蓝色背景、无边框、高度20px） */
.answer_area .normal-text :deep(a) {
  height: 20px !important;
  line-height: 20px !important;
  padding: 0 8px !important;
  border-radius: 2px !important;
  background: #e6f4ff !important;
  color: #1b6cff !important;
  font-size: 12px !important;
  vertical-align: middle !important;
  position: relative; /* 微调垂直位置 */
  top: -2px; /* 上移 2px */
  border: none !important;
  box-shadow: none !important;
  text-decoration: none !important;
}
.answer_area .normal-text :deep(a:hover),
.answer_area .normal-text :deep(a:active) {
  background: #d0e4ff !important;
  color: #1b6cff !important;
  border: none !important;
  box-shadow: none !important;
}
.answer_area .normal-text :deep(a:visited) {
  color: #1b6cff !important;
}
.answer_area .normal-text :deep(a:focus-visible) {
  outline: none !important;
  box-shadow: none !important;
}

/* 页码样式已并入链接文本，保留（无效果）占位，便于未来扩展 */
</style>

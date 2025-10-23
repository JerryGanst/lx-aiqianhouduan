<template>
  <!-- 自定义右侧面板，无遮罩，左侧可点击 -->
  <transition name="drawer-slide">
    <div v-show="visible" ref="drawerRef" class="knowledge-drawer" :style="{ width: drawerSize }" role="dialog" aria-modal="false" @click.stop @mousedown.stop>
    <div class="drawer-header">
      <div class="drawer-header-left">
        <img class="drawer-logo" src="@/assets/chat_logo.png" alt="logo" />
        <span class="drawer-title">{{ title }}</span>
      </div>
      <div class="drawer-close" @click="closeDrawer">
        <img src="@/assets/close.png" alt="关闭" class="close-icon" />
      </div>
    </div>
    <div class="drawer-body">
      <!-- 欢迎界面 - 当没有消息时显示 -->
      <div v-if="fileChatCurrent.messages.length === 0" class="welcome-section">
        <div class="welcome-content">
          <div class="welcome-avatar">
            <img src="@/assets/robot.png" alt="AI Assistant" />
          </div>
          <h3 class="welcome-title">欢迎使用知识库聊天</h3>
          <p class="welcome-subtitle">我是您的AI助手，可以帮您分析文档内容</p>
          <div class="welcome-features">
            <div class="feature-item">
              <div class="feature-icon">📄</div>
              <span>文档分析</span>
            </div>
            <div class="feature-item">
              <div class="feature-icon">💡</div>
              <span>智能问答</span>
            </div>
            <div class="feature-item">
              <div class="feature-icon">🔍</div>
              <span>深度理解</span>
            </div>
          </div>
          <div class="welcome-tips">
            <p>💡 提示：您可以询问关于文档的任何问题</p>
          </div>
        </div>
      </div>

      <div class="chat_area" v-if="fileChatCurrent.messages.length > 0">
        <div v-for="(item, index) in fileChatCurrent.messages" :key="index">
          <div v-if="index === 0" class="top_file_area" :title="fileInfo.name">
            <div class="left_img">
              <img :src="getFileImgByExtension(fileInfo)" />
            </div>
            <div class="right_file_msg">
              <div class="file_title" :title="fileInfo.name">{{ truncateFileName(fileInfo.name) }}</div>
              <div class="file_msg">
                {{ fileInfo.extension }} {{ fileInfo.size ? (fileInfo.size / 1024).toFixed(1) : 0 }}KB
              </div>
            </div>
          </div>
          <!-- 用户发的消息 -->
          <div class="ask_message" v-if="index % 2 === 0">
            {{item.content}}
          </div>
          <!-- 回答 -->
          <div class="answer_area" v-if="index % 2 !== 0 && item.thinking">
            <!-- 思考过程展示 -->
            <div v-if="item.thinking" class="thinking_area">
              <MarkdownRenderer
                :markdown="item.thinking"
                class="normal-text"
                style="
                  font-size: 13px;
                  line-height: 24px;
                  padding: 0px 10px;
                  background-color: transparent;
                  color: #666;
                "
              />
            </div>
            <!-- 回答内容 -->
            <MarkdownRenderer
              :markdown="item.after"
              class="normal-text"
              style="font-size: 16px; line-height: 1.6; background-color: transparent; color: #333; padding: 16px 0;"
            />
          </div>
          <div v-if="index === fileChatQuery.messages.length - 1 && !isLoading && messageId" class="refresh_area">
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
      <div class="input_area">
        <el-input
          ref="inputRef"
          v-model="inputText"
          type="textarea"
          placeholder="请输入您的问题,换行请按下Shift+Enter"
          @keydown="handleKeydown"
        />
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
  </div>
  </transition>
  
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
</template>

<script setup>
import { computed, reactive, ref, toRaw, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import { DRAWER_WIDTH } from '@/utils/constants.js'
import MarkdownRenderer from '@/pages/main/component/markdown.vue'
import imageB from '@/assets/arrow_blue.png'
import imageA from '@/assets/arrow_gray.png'
import imageC from '@/assets/stop.png'
import { delay, getFileImgByExtension, isCompleteJSON } from '@/utils/common.js'
import { useShared } from '@/utils/useShared.js'
import { KnowledgeSelect } from "@/utils/common.js"
import request from '@/utils/request'
import eventBus from '@/utils/eventBus'

import { ElMessage } from 'element-plus'

const { userInfo, knowSelect } = useShared()

const isDepartmentKnowledge = computed(() => knowSelect.value === KnowledgeSelect.DEPARTMENT)

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '文件聊天' },
  fileId: { type: [String, Number], default: null },
  fileInfo: { type: [Object], default: null },
  // 评价用的消息记录ID（与通用页一致），需要父级在保存后传入
  messageId: { type: [String, Number], default: '' }
})

const emit = defineEmits(['update:modelValue', 'before-close'])
const messageId = ref(props.messageId || '')

const drawerRef = ref(null)
const visible = computed({
  get: () => props.modelValue,
  set: v => emit('update:modelValue', v)
})

const drawerSize = computed(() => `${DRAWER_WIDTH}px`)

const inputText = ref('')
const inputRef = ref(null)

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

const handleKeydown = event => {
  if (event.key === 'Enter' && event.shiftKey) {
    // Shift+Enter 换行，不阻止默认行为
    return
  }
  if (event.key === 'Enter' && !event.shiftKey) {
    // 普通 Enter 发送消息
    event.preventDefault()
    // 如果正在加载中，提示用户不应发送消息
    if (isLoading.value) {
      ElMessage.warning('有问答正在进行中,请稍后再试')
      return
    }
    handleSendClick()
    return
  }
  // Ctrl/Cmd + A 全选
  if ((event.ctrlKey || event.metaKey) && (event.key === 'a' || event.key === 'A')) {
    event.preventDefault()
    const el = inputRef.value?.textarea ?? inputRef.value?.input ?? event.target
    if (el && typeof el.select === 'function') {
      el.select()
    }
  }
}

// 文件聊天参数
const fileChatQuery = reactive({
  //通用模式数据对象
  messages: []
})

// fileChatQueryCpy的全权代表
const fileChatCurrent = reactive({
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
    streamInput.chatType = 'department_single'
  } else {
    streamInput.chatType = 'single'
  }
  streamInput.files = []
  streamInput.fileId = props.fileInfo.fileId
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
    } catch (_) {
      ElMessage.error('数据格式异常')
    }
    isLoading.value = false
    // 缓冲区错误时也要保存已经输出的流式数据
    if (fileChatCurrent.messages.length > 0) {
      // 临时保存当前状态到 fileChatQuery，然后保存
      const tempMessages = [...fileChatCurrent.messages]
      fileChatQuery.messages = tempMessages
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
const applyServerEventToAssistant = async (payload, assistantMsg, fileChatQueryCpy) => {
  const { content, type, sources } = payload || {}
  if (type === 'reasoning') {
    assistantMsg.before += content || ''
  } else if (type === 'streaming') {
    assistantMsg.after += content || ''
    // 流式过程中也尝试替换 CITE 为链接（sources 可能稍后到达，最终会在 final_answer 再替换一次）
    assistantMsg.after = replaceCitationsWithLinks(assistantMsg.after, assistantMsg.sources)
  } else if (type === 'process') {
    assistantMsg.thinking = content || ''
    await delay(250)
  } else if (type === 'final_answer') {
    if (Array.isArray(sources) && sources.length > 0) {
      assistantMsg.sources = sources
    }
    // 最终回答里进行一次完整替换
    assistantMsg.after = replaceCitationsWithLinks(content || '', assistantMsg.sources)
  }

  // 同步视图（覆盖最后一条 assistant）
  fileChatQueryCpy.messages.splice(-1, 1, {
    ...toRaw(assistantMsg),
    before: assistantMsg.before,
    after: assistantMsg.after,
    content: (assistantMsg.before || '') + (assistantMsg.after || ''), // 兼容旧字段
    sources: assistantMsg.sources,
    thinking: assistantMsg.thinking
  })

  // 驱动实时渲染与滚动
  fileChatCurrent.messages = [...fileChatQueryCpy.messages]
  nextTick(() => {
    const chatArea = document.querySelector('.chat_area')
    if (chatArea) {
      chatArea.scrollTop = chatArea.scrollHeight
    }
  })
}

const handleSteamResult = async (streamResult, assistantMsg, fileChatQueryCpy) => {
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
              await applyServerEventToAssistant(payload, assistantMsg, fileChatQueryCpy)
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
        fileChatQuery.messages = JSON.parse(JSON.stringify(fileChatQueryCpy.messages))
        isLoading.value = false
        
        // 流式输出完成后，滚动到 refresh_area 位置
        nextTick(() => {
          const chatArea = document.querySelector('.chat_area')
          const refreshArea = document.querySelector('.refresh_area')
          if (chatArea && refreshArea) {
            // 滚动到 refresh_area 的位置，确保完全可见
            refreshArea.scrollIntoView({ behavior: 'smooth', block: 'end' })
          }
        })
        // 保存记录以获取可用于评价的消息ID
        saveChatRecord()
        
        break
      }
      buffer += decoder.decode(value, { stream: true })
      const { events, rest } = extractSSEEventsFromBuffer(buffer)
      buffer = rest
      for (const jsonStr of events) {
        try {
          const payload = JSON.parse(jsonStr)
          await applyServerEventToAssistant(payload, assistantMsg, fileChatQueryCpy)
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
      // 请求被中止，不需要显示错误信息
      console.log('请求被中止')
      // 中止时也要保存已经输出的流式数据
      if (fileChatCurrent.messages.length > 0) {
        // 临时保存当前状态到 fileChatQuery，然后保存
        const tempMessages = [...fileChatCurrent.messages]
        fileChatQuery.messages = tempMessages
        saveChatRecord()
      }
    } else {
      console.error('流式处理错误:', error)
      ElMessage.error('数据处理异常')
      // 错误时也要保存已经输出的流式数据
      if (fileChatCurrent.messages.length > 0) {
        // 临时保存当前状态到 fileChatQuery，然后保存
        const tempMessages = [...fileChatCurrent.messages]
        fileChatQuery.messages = tempMessages
        saveChatRecord()
      }
    }
    isLoading.value = false
  }
}

let isLoading = ref(false)
let abortController = ref(null)
const isDisabled = ref(false)
const commonVisible = ref(false)
const commonQuestion = ref('')
let lastRefreshTime = 0

const handleSendClick = async (isRefresh = false) => {
  // 如果正在加载中，则终止请求（这是终止按钮的功能）
  if (isLoading.value) {
    handleStopRequest()
    return
  }
  
  // 验证输入内容
  if (!inputText.value || !inputText.value.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  
  // 开启加载状态
  isLoading.value = true
  // 创建新的 AbortController
  abortController.value = new AbortController()
  
  const inputTextCpy = inputText.value
  inputText.value = ''
  // 当前发送的消息
  const currentFileData = {
    role: 'user',
    content: inputTextCpy || '',
    files: [],
    personalKnowledge: !isDepartmentKnowledge.value
  }
  // 复制文件聊天参数
  let fileChatQueryCpy = JSON.parse(JSON.stringify(fileChatQuery))
  
  // 如果是刷新操作，删除最后两个消息（用户消息和助手回复）
  if (isRefresh && fileChatQueryCpy.messages.length > 1) {
    fileChatQueryCpy.messages.splice(-2, 2)
    // 不重置 messageId，使用上一次 save 返回的 id 值
  }
  
  fileChatQueryCpy.messages.push(currentFileData)
  // 封装流式入参(奇数个入参：之前的问答+当前问题)
  const streamInput = JSON.parse(JSON.stringify(fileChatQueryCpy))
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
  fileChatQueryCpy.messages.push(assistantMsg)
  // 把current与cpy关联起来 后面对临时变量cpy的操作就是对current的操作
  fileChatCurrent.messages = fileChatQueryCpy.messages

  // 消息发送后立即滚动到底部
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
      if (fileChatCurrent.messages.length > 0) {
        const tempMessages = [...fileChatCurrent.messages]
        fileChatQuery.messages = tempMessages
        saveChatRecord()
      }
      isLoading.value = false
      return
    }

    await handleSteamResult(res, assistantMsg, fileChatQueryCpy)
  } catch (error) {
    if (error.name === 'AbortError') {
      // 请求被中止
      ElMessage.success('请求已中止')
      // 中止时也要保存已经输出的流式数据
      if (fileChatCurrent.messages.length > 0) {
        // 临时保存当前状态到 fileChatQuery，然后保存
        const tempMessages = [...fileChatCurrent.messages]
        fileChatQuery.messages = tempMessages
        saveChatRecord()
      }
    } else {
      ElMessage.error('请求失败，请稍后重试')
      // 错误时也要保存已经输出的流式数据
      if (fileChatCurrent.messages.length > 0) {
        // 临时保存当前状态到 fileChatQuery，然后保存
        const tempMessages = [...fileChatCurrent.messages]
        fileChatQuery.messages = tempMessages
        saveChatRecord()
      }
    }
    isLoading.value = false
  }
}

const handleStopRequest = () => {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
  isLoading.value = false
  
  // 终止时也要保存已经输出的流式数据
  // 使用 fileChatCurrent.messages 来获取当前正在流式输出的数据
  if (fileChatCurrent.messages.length > 0) {
    // 临时保存当前状态到 fileChatQuery，然后保存
    const tempMessages = [...fileChatCurrent.messages]
    fileChatQuery.messages = tempMessages
    saveChatRecord()
  }
  
  // 移除这里的消息，避免重复显示
  // ElMessage.success('请求已中止')
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

  // 通用页使用的是 /Message/feedback，知识库聊天需要有消息记录ID
  // 若父级有传入 messageId 则优先使用；否则回退到文件ID保持行为一致
  request
    .post('/Message/feedback', {
      id: messageId.value || props.fileInfo.fileId,
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
      id: messageId.value || props.fileInfo.fileId,
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

// 保存知识库聊天记录，拿到 messageId 用于点赞/点踩
const saveChatRecord = () => {
  try {
    // 获取第一个用户消息作为标题，如果没有则使用文件信息
    const firstUserMessage = fileChatQuery.messages.find(msg => msg.role === 'user')
    const titleStr = firstUserMessage?.content || fileChatQuery.messages?.[0]?.content || ''
    request
      .post('/Message/save', {
        userId: userInfo.value.id,
        type: '通用模式',
        id: messageId.value || '', // 使用上一次返回的 messageId，如果是第一次则为空字符串
        data: fileChatQuery.messages,
        isThink: false,
        title: titleStr
      })
      .then(res => {
        if (res.status) {
          messageId.value = res.data
          // 保存成功后刷新对话列表
          eventBus.emit('fetchChatList', '')
        }
      })
      .catch(() => {})
  } catch (_) {}
}

const onBeforeClose = done => {
  // 通知父组件抽屉即将关闭
  emit('before-close', { fileId: props.fileId })
  // 调用 done() 允许抽屉关闭
  done()
}

const closeDrawer = () => {
  // 通知父组件抽屉即将关闭
  emit('before-close', { fileId: props.fileId })
  // 关闭抽屉
  visible.value = false
}

const truncateFileName = fileName => {
  if (!fileName) return ''
  return fileName.length > 16 ? fileName.substring(0, 16) + '...' : fileName
}

// 清空聊天记录
const clearChatHistory = () => {
  // 如果有正在进行的请求，先中止它
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
  isLoading.value = false
  fileChatQuery.messages = []
  fileChatCurrent.messages = []
  messageId.value = ''
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
  const lastUserMessage = fileChatQuery.messages[fileChatQuery.messages.length - 2]
  if (!lastUserMessage || !lastUserMessage.content) {
    ElMessage.warning('没有可刷新的内容')
    return
  }
  
  // 重新发送最后一个用户消息
  inputText.value = lastUserMessage.content
  handleSendClick(true)
}

// 监听文件切换：当预览文件变化时，清空聊天记录并回到初始界面
watch(
  () => props.fileInfo && props.fileInfo.fileId,
  (newVal, oldVal) => {
    if (!newVal || newVal === oldVal) return
    clearChatHistory()
  }
)

// 使用 Element Plus 的遮罩点击关闭，不进行全局点击监听

// 暴露方法给父组件调用
defineExpose({
  clearChatHistory
})
</script>

<style scoped>
/* 抽屉滑动动画 */
.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: transform 0.3s ease-in-out;
}

.drawer-slide-enter-from {
  transform: translateX(100%);
}

.drawer-slide-leave-to {
  transform: translateX(100%);
}

/* 自定义右侧抽屉容器：固定在右侧，无遮罩，左侧可点击 */
.knowledge-drawer {
  position: fixed;
  top: 0;
  right: 0;
  height: 100vh;
  background-color: #fff;
  border-left: 1px solid var(--el-border-color-lighter);
  z-index: 1000;
  display: flex;
  flex-direction: column;
}

.drawer-body {
  position: relative;
  height: 100%;
  padding-bottom: 160px; /* 确保底部输入区域不会遮挡内容 (120px + 24px + 16px额外安全距离) */
  overflow: hidden;
  margin-left: 23px;
  margin-right: 23px;
}

/* 欢迎界面 */
.welcome-section {
  display: flex;
  align-items: center;
  justify-content: center;
  height: calc(100vh - 240px); /* 减去header高度(约80px)和input_area高度(120px)和间距(24px+16px) */
  background: linear-gradient(135deg, #f9fafb 0%, #f0f7ff 100%);
  margin-right: 2px; /* 移除右侧边距，让欢迎界面与输入框右侧真正平齐 */
  border-radius: 6px; /* 轻微圆角 */
  overflow: hidden; /* 裁切渐变背景以贴合圆角 */
}

.welcome-content {
  text-align: center;
  max-width: 400px;
  padding: 40px 20px;
}

.welcome-avatar {
  margin-bottom: 24px;
}

.welcome-avatar img {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  box-shadow: 0 4px 20px rgba(27, 108, 255, 0.15);
  animation: float 3s ease-in-out infinite;
}

.welcome-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.welcome-subtitle {
  font-size: 16px;
  color: #666;
  margin-bottom: 32px;
  line-height: 1.5;
}

.welcome-features {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 32px;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
  border: 1px solid rgba(27, 108, 255, 0.1);
}

.feature-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(27, 108, 255, 0.15);
  border-color: rgba(27, 108, 255, 0.3);
}

.feature-icon {
  font-size: 24px;
  margin-bottom: 4px;
}

.feature-item span {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.welcome-tips {
  background: rgba(240, 247, 255, 0.8);
  border-radius: 12px;
  padding: 16px;
  border-left: 4px solid #1b6cff;
  backdrop-filter: blur(10px);
}

.welcome-tips p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-10px);
  }
}

.top_file_area {
  display: flex;
  align-items: center;
  justify-content: left;

  .left_img {
    margin-right: 8px;
    margin-left: 16px;
    transform: translateY(3px);

    img {
      width: 36px;
      height: 45.02px;
    }
  }

  .right_file_msg {
    .file_title {
      max-width: 180px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 18px;
      color: #333;
      font-weight: 400;
    }

    .file_msg {
      color: #aaaaaa;
    }
  }
}

.chat_area {
  height: calc(100vh - 240px); /* 减去header高度(约80px)和input_area高度(120px)和间距(24px+16px) */
  overflow-x: hidden;
  overflow-y: scroll;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
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
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  margin: 10px 4px 10px auto;
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

.answer_area {
  clear: both;
  border-radius: 10px;
  background: #fafafa;
  width: 530px;
  box-sizing: border-box;
  padding: 16px;
  margin: 12px 0;
}

/* 与 AskKnowledgeDialog.vue 保持一致的现代化链接样式 */
/* 此页面不再渲染为链接，以下样式移除以恢复默认文本颜色与样式 */

.thinking_area {
  margin-bottom: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}



.top_file_area {
  width: 240px;
  height: 66px;
  background-color: #eff6ff;
  border-radius: 10px;
  margin-left: 293px;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-left: 24px;
  margin-right: 24px;
  margin-top: 24px;
  margin-bottom: 24px;
}

.drawer-header-left {
  display: flex;
  align-items: center;
}

.drawer-close {
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 4px;
  transition: background-color 0.2s ease;
}

.drawer-close:hover {
  background-color: #f5f5f5;
}

.close-icon {
  width: 16px;
  height: 16px;
}

.drawer-logo {
  width: 33px;
  height: 33px;
  margin-right: 8px;
}

.drawer-title {
  font-size: 16px;
  font-weight: 500;
}

/* add a subtle left border on the actual drawer panel */
:deep(.knowledge-drawer .el-drawer) {
  position: relative;
  border-left: 1px solid var(--el-border-color-lighter);
}

/* ensure content doesn't get hidden behind the bottom input */
:deep(.knowledge-drawer .el-drawer__body) {
  padding-bottom: 140px;
}

.input_area {
  position: absolute;
  left: 50%;
  bottom: 25px;
  transform: translateX(-50%);
  width: 530px;
  height: 120px;
  display: block;
}

/* 强制隐藏 textarea 的 resize 图标 */
:deep(.el-textarea__inner) {
  resize: none !important;
  height: 120px !important;
  border: 1px solid #1b6cff !important;
  overflow-y: auto !important;
  /* 让 Firefox 显示可见滚动条 */
  scrollbar-width: thin;
  /* Firefox: 拇指为深色，轨道透明 */
  scrollbar-color: rgba(0, 0, 0, 0.38) transparent;
}

/* 强制该抽屉内的输入框圆角为 8px，覆盖全局 16px */
:deep(.knowledge-drawer .el-textarea__inner) {
  border-radius: 8px !important;
}

:deep(.knowledge-drawer .el-textarea) {
  border-radius: 8px !important;
  overflow: hidden !important;
}

.send-icon {
  position: absolute;
  right: 20px;
  bottom: 13px;
  cursor: pointer;
  display: flex;
}

.send-icon img {
  width: 30px;
  height: 30px;
  transition: opacity 0.2s ease;
}

.send-icon.loading img {
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.6;
  }
  50% {
    opacity: 1;
  }
}

/* Markdown 渲染内容中的超链接现代化样式 */
.normal-text :deep(a) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 8px;
  border-radius: 8px;
  color: #16a34a;
  text-decoration: none;
  border: 1px solid #e5e7eb;
  background-color: transparent;
  transition: color 0.2s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
  word-break: break-word;
  font-size: 0.92em;
}

.normal-text :deep(a:hover) {
  background-color: #ecfdf5;
  border-color: #86efac;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  color: #15803d;
}

.normal-text :deep(a:active) {
  background-color: #dcfce7;
  border-color: #86efac;
}

.normal-text :deep(a:visited) {
  color: #15803d;
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
  position: relative;
  top: -2px;
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

/* 覆盖全局对 textarea 滚动条的透明设置，确保在本抽屉内可见 */
:deep(.knowledge-drawer .el-textarea__inner::-webkit-scrollbar) {
  width: 8px !important;
  height: 8px !important;
  opacity: 1 !important;
}

:deep(.knowledge-drawer .el-textarea__inner::-webkit-scrollbar-track) {
  background: transparent !important;
  border-radius: 8px !important;
  opacity: 1 !important;
}

:deep(.knowledge-drawer .el-textarea__inner::-webkit-scrollbar-thumb) {
  background: rgba(0, 0, 0, 0.38) !important;
  border-radius: 8px !important;
  border: 2px solid transparent !important;
  background-clip: padding-box !important;
  opacity: 1 !important;
}

:deep(.knowledge-drawer .el-textarea__inner::-webkit-scrollbar-thumb:hover) {
  background: rgba(0, 0, 0, 0.48) !important;
}
</style>
<style>
/* 仅影响当前抽屉的遮罩为透明，保留点击遮罩关闭行为 */
.el-overlay.no-mask-overlay { background-color: transparent !important; }
</style>

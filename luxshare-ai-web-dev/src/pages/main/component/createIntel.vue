<template>
  <div class="create_main">
    <!-- 临时去掉条件intelList.length > 0 -->
    <div class="create_ask">
      <div class="main_content" v-if="currentAgentType !== RESUME_AGENT_TYPE">
        <div class="sample_item" ref="messageContainerIntel" @scroll="checkScrollPosition">
          <div class="content_tip">
            <div class="content_robot"><img src="@/assets/robot.png" /></div>
            <div class="tip_text">
              Hi,我是你创建的
              <span :style="{ fontWeight: 600 }">{{ currentIntel.name }}</span>
              <span v-if="COMPARE_AGENT_TYPE === currentAgentType">, 请先上传标准图再上传对比图比较</span>
              <span v-else-if="TABLE_AGENT_TYPE === currentAgentType">, 请先在个人知识库中上传您所需要的EXCEL表格</span>
              <span v-else-if="RESUME_AGENT_TYPE === currentAgentType">, 我将为你精确筛选合适的人员</span>
              <span v-else>, 我将为你生成灵感，设计独属于你的风格。</span>
            </div>
          </div>
          <template v-if="intelQuery.messages.length > 0 && !limitIntelLoading">
            <div class="sample_chat" v-for="(item, index) in intelQuery.messages">
              <div
                v-if="index % 2 === 0 && item.files && item.files.length > 0"
                class="sample_chat_file"
                :style="{ marginTop: index === 0 ? '28px' : '40px' }"
              >
                <div v-for="its in item.files" class="item_files" @click="showListFile(its)">
                  <span style="display: flex; align-items: center">
                    <img :src="getFileImgByOriginFile(its)" style="width: 24px; height: 30px" />
                  </span>
                  <span style="padding-left: 10px" class="file_name">{{ its.originalFileName }}</span>
                </div>
              </div>
              <div
                v-if="index % 2 === 0"
                class="sample_chat_query"
                :style="{
                  marginTop: item.content
                    ? item.files && item.files.length > 0
                      ? '10px'
                      : index === 0
                        ? '30px'
                        : '40px'
                    : '0px',
                  padding: item.content ? '13px 15px' : '0px'
                }"
              >
                {{ item.content }}
              </div>
              <!-- <MarkdownRenderer v-if="index % 2 !== 0" :markdown="item.content" type="answer" /> -->
              <div v-if="index % 2 !== 0 && item.isNewData" class="stream-response">
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
                <MarkdownRenderer
                  :markdown="item.before"
                  class="normal-text"
                  style="
                    font-size: 13px;
                    line-height: 24px;
                    padding: 0px 10px;
                    background-color: transparent;
                    color: #666;
                  "
                />
                <!-- 后半部分 -->
                <MarkdownRenderer v-if="item.hasSplit" :markdown="item.after" class="normal-text"/>
                <div style="margin-top: 12px"><a :href="item.downloadUrl" target="_blank" style="cursor: pointer !important;">{{ getLastPathSegment(item.objectName) }}</a></div>
              </div>
              <MarkdownRenderer v-if="index % 2 !== 0 && !item.isNewData" :markdown="item.content" />
            </div>
          </template>
          <template
            v-if="intelCurrent.messages.length > 0 && limitIntelLoading && currentAgentType === loadingIntelType"
          >
            <div class="sample_chat" v-for="(item, index) in intelCurrent.messages">
              <div
                v-if="index % 2 === 0 && item.files && item.files.length > 0"
                class="sample_chat_file"
                :style="{ marginTop: index === 0 ? '30px' : '40px' }"
              >
                <div v-for="its in item.files" class="item_files" @click="showListFile(its)">
                  <span style="display: flex; align-items: center">
                    <img :src="getFileImgByOriginFile(its)" style="width: 24px; height: 30px" />
                  </span>
                  <span style="padding-left: 10px" class="file_name">{{ its.originalFileName }}</span>
                </div>
              </div>
              <div
                v-if="index % 2 === 0"
                class="sample_chat_query"
                :style="{
                  marginTop: item.content
                    ? item.files && item.files.length > 0
                      ? '10px'
                      : index === 0
                        ? '30px'
                        : '40px'
                    : '0px',
                  padding: item.content ? '13px 15px' : '0px'
                }"
              >
                {{ item.content }}
              </div>
              <div class="tip_load" v-if="index === intelCurrent.messages.length - 1">
                <span><img src="@/assets/robot.png" style="width: 36px; height: 36px" /></span>
                <span style="padding-left: 10px">正在为您解答,请稍等</span>
                <span>{{ dots }}</span>
              </div>
              <div style="margin-top: 12px">{{ item.thinking }}</div>
              <MarkdownRenderer v-if="index % 2 !== 0" :markdown="item.content" type="answer" />
            </div>
          </template>
        </div>
        <!-- 添加滚动到底部按钮 -->
        <div v-if="showScrollButton" class="scroll-to-bottom" :class="{ loading: isIntelLoad }" @click="scrollToBottom">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M12 19V5M5 12l7 7 7-7" stroke="currentColor" stroke-width="2" />
          </svg>

          <!-- 外围旋转圆环 - 只有在加载时显示 -->
          <div v-if="isIntelLoad" class="loading-ring"></div>
        </div>
        <div class="query_common" v-if="!limitIntelLoading && intelQuery.messages.length > 0">
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
      <div class="main_content" v-if="currentAgentType === RESUME_AGENT_TYPE">
        <send-resume-msg
          v-if="isSendResumeMsgPage"
          @add-job-description-file="handleJobJdButtonClick"
          @add-resume-file="handleResumeButtonClick"
          @submit-resume="handleResumeSubmit"
        ></send-resume-msg>
        <resume-agent @preview-file="showListFile" v-else></resume-agent>
      </div>
      <div class="select_content" v-if="currentAgentType !== RESUME_AGENT_TYPE">
        <div class="textarea" :class="[hasAttachments ? 'sampleAreaAry' : 'sampleArea']">
          <el-input
            v-model="intelQuestion"
            placeholder="请输入您的问题,换行请按下Shift+Enter"
            style="width: 100%"
            class="custom-input"
            clearable
            @keydown.enter.prevent="samplePost"
            @keyup.shift.enter.prevent="handleShiftEnter('textareaInputIntel', $event)"
            ref="textareaInputIntel"
            :maxlength="4096"
            type="textarea"
            :rows="dynamicRows"
          />
          <div class="filesList" v-if="hasAttachments">
            <div
              v-if="hasJobJdFile"
              class="file-item job-jd"
              style="cursor: pointer; margin-left: 5px"
              @click="showJobJdFile"
            >
              <span style="display: flex; align-items: center">
                <img :src="getFileImgByOriginFile(jobJdFile)" style="width: 22px; height: 28px" />
              </span>
              <span class="file_name job-jd-name" style="padding-left: 10px; overflow: hidden; padding-top: 8px">
                {{ jobJdFile.originalFileName }}
              </span>
              <span
                class="file-remove"
                @click.stop="removeJobJdFile"
              >
                <img src="@/assets/close.png" style="width: 10px; height: 10px" />
              </span>
            </div>
            <div
              v-for="(item, index) in fileInputAry"
              style="cursor: pointer"
              @click="showListFile(item)"
            >
              <span style="display: flex; align-items: center">
                <img :src="getFileImgByOriginFile(item)" style="width: 22px; height: 28px" />
              </span>
              <span style="padding-left: 10px; width: 50px; overflow: hidden; padding-top: 8px" class="file_name">
                {{ item.originalFileName }}
              </span>
              <span
                style="
                  position: absolute;
                  width: 16px;
                  height: 16px;
                  right: 0px;
                  top: 0px;
                  cursor: pointer;
                  display: flex;
                  justify-content: center;
                  align-items: center;
                "
                @click.stop="deleteImg(index)"
              >
                <img src="@/assets/close.png" style="width: 10px; height: 10px" />
              </span>
            </div>
          </div>
          <!-- 发送图标 -->
          <div class="send-icon">
            <div
              v-if="currentAgentType === RESUME_AGENT_TYPE && showUploadButton"
              class="resume-icon tooltip-wrapper"
              :class="{ 'is-hovered': showResumeJdTip }"
              @mouseenter="showResumeJdTip = true"
              @mouseleave="showResumeJdTip = false"
              @click="handleJobJdButtonClick"
              style="transform: translateY(-5px)"
            >
              <img :src="requestInfoImg" alt="岗位JD" style="width: 18px; height: 18px;"/>
              <transition name="fade">
                <div v-if="showResumeJdTip" class="tooltip">岗位JD</div>
              </transition>
            </div>
            <div
              v-if="currentAgentType === RESUME_AGENT_TYPE && showUploadButton"
              class="resume-icon tooltip-wrapper"
              :class="{ 'is-hovered': showResumeFileTip }"
              @mouseenter="showResumeFileTip = true"
              @mouseleave="showResumeFileTip = false"
              @click="handleResumeButtonClick"
              style="transform: translateY(-5px)"
            >
              <img :src="resumeFileImg" alt="简历" style="width: 18px; height: 18px;"/>
              <transition name="fade">
                <div v-if="showResumeFileTip" class="tooltip">简历</div>
              </transition>
            </div>
            <div v-if="currentAgentType !== RESUME_AGENT_TYPE" class="tooltip-wrapper" ref="wrapperRef" v-show="showUploadButton">
              <img src="@/assets/file.png" class="arrow" @click="showFileSample('sample')" style="margin-right: 10px" />
              <FileMenu
                :showFileMenu="showFileMenu"
                :handleFileSelect="handleFileSelect"
                localType="sample"
                knowledgeType="sample"
                :currentAgentType="currentAgentType"
                restrict-by-agent-type
              />
            </div>
            <img
              :src="
                intelQuery.isLoading && loadingIntelId && loadingIntelId === currentIntelId
                  ? imageC
                  : intelQuestion || hasAttachments
                    ? imageB
                    : imageA
              "
              class="arrow"
              @click="submitSampleSend"
            />
          </div>
        </div>
      </div>
    </div>
  </div>

  <FileUpload ref="fileRefs"></FileUpload>
  <commonUploadModal ref="commonUploadModals"></commonUploadModal>
  <FilePreUpload ref="filePreRef"></FilePreUpload>

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
<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, toRaw } from 'vue'
import { useShared } from '@/utils/useShared'
import { ElMessage } from 'element-plus' // 引入 ElMessage
import request from '@/utils/request' // 导入封装的 axios 方法
import eventBus from '@/utils/eventBus'
import FileUpload from '../component/fileUploadModal.vue'
import commonUploadModal from '../component/commonUploadModal.vue'
import FilePreUpload from '../component/filePreModal.vue'
import imageB from '@/assets/arrow_blue.png'
import imageA from '@/assets/arrow_gray.png'
import imageC from '@/assets/stop.png'
import text from '@/assets/text.png'
import requestInfoImg from '@/assets/agent/request_info.svg'
import resumeFileImg from '@/assets/agent/resume_file.svg'
import MarkdownRenderer from '../component/markdown.vue' // 引入 Markdown 渲染组件
import FileMenu from '../component/options/fileMenu.vue'
import {
  getAgentChatByAgentId,
  getAgentChatByChatId,
  getExcelChatById,
  getExcelChatByUserId,
  getImageRecognitionsByUserId,
  getImgRecognitionById,
  getResumeTaskById,
  getResumeTaskByUserId,
  getNewChatId,
  saveAgentChat,
  saveExcelChat,
  saveImgRecognition,
  createResumeAnalysis,
  getResumeTaskCallback
} from '../../../api/agent/actions'
import { delay, getFileImgByOriginFile, isCompleteJSON } from '@/utils/common.js'
import { loadingIntelTypeHandlerMap } from '@/utils/agent/saveAgentHandlers.js'
import {
  COMPARE_AGENT_TYPE,
  DEFAULT_AGENT_TYPE,
  FILE_LIST_LIMIT,
  RESUME_AGENT_TYPE,
  TABLE_AGENT_TYPE
} from '@/utils/constants.js'
import ResumeAgent from '@/pages/main/component/agent/resumeAgent.vue'
import SendResumeMsg from '@/pages/main/component/agent/sendResumeMsg.vue'

const {
  intelList,
  answerListIntel,
  activeIndexIntel,
  currentIntel,
  limitIntelLoading,
  fileInputAry,
  jobJdFile,
  isLogin,
  textareaInputIntel,
  intelQuestion,
  dynamicRows,
  handleShiftEnter,
  isDragOver,
  fileAry,
  messageContainerIntel,
  intelQuery,
  intelCurrent,
  dots,
  isIntelStop,
  currentIntelId,
  recordId,
  agentChatList,
  conversationId,
  loadingIntelId,
  loadingIntelType,
  tempChatId,
  currentAgentType,
  finalTitle,
  excelChatRepeat,
  isIntelLoad,
  resumeTaskState,
  userInputContent,
  isSendResumeMsgPage,
  updateResumeTaskState
} = useShared()
const formIntel = ref({
  name: '',
  description: '',
  nickName: '',
  tone: '',
  id: ''
})

const showUploadButton = ref(true)
const showResumeJdTip = ref(false)
const showResumeFileTip = ref(false)
const hasJobJdFile = computed(
  () => !!(jobJdFile.value && jobJdFile.value.originalFileName)
)
const hasAttachments = computed(
  () => hasJobJdFile.value || (fileInputAry.value && fileInputAry.value.length > 0)
)
const isDisabled = ref(false)
const commonQuestion = ref('')
const limitQuery = ref('')
const fileRefs = ref(null)
const commonUploadModals = ref(null)
const isComputed = ref(false)
const filePreRef = ref(null)
const commonVisible = ref(false)
const type = ref('create')
const currentRequestUrl = ref('')
const scrollPosition = ref(0) // 记录滚动位置
const showScrollButton = ref(false) // 是否显示滚动按钮
const userScrolledUp = ref(false) // 用户是否向上滚动
let interval
const RESUME_TASK_POLL_INTERVAL = 3000
const RESUME_TASK_TYPE = 'batch_match_task'
let resumeTaskPollTimer: ReturnType<typeof setTimeout> | null = null
type ResumeTaskPersistContext = {
  id: string
  resumeRepeat: Record<string, any> | null
  hasSaved: boolean
}

const extractRankingSummary = (
  rankingResult: Record<string, any> | null | undefined,
  keys: string[]
) => {
  if (!rankingResult || typeof rankingResult !== 'object') {
    return ''
  }

  for (const key of keys) {
    const value = rankingResult[key]
    if (typeof value === 'string') {
      return value
    }
  }

  return ''
}

const resumeTaskPersistContext = reactive<ResumeTaskPersistContext>({
  id: '',
  resumeRepeat: null,
  hasSaved: false
})

const resetResumeTaskPersistContext = () => {
  resumeTaskPersistContext.id = ''
  resumeTaskPersistContext.resumeRepeat = null
  resumeTaskPersistContext.hasSaved = false
}
const placeholderText = ref(`# 设定
你是一位营销文案奇才，擅长通过对话引导用户明确其产品或服务需求，并能创作出既幽默诙谐又信息准确、吸引力十足的广告语、宣传文案和社交媒体内容。

#  技能
## 技能1：需求挖掘与沟通
- 通过提问和互动，帮助用户清晰定义他们的产品特性和目标受众。
- 识别用户的核心价值主张，并将其转化为文案的关键信息。`)
const checkScrollPosition = () => {
  if (!messageContainerIntel.value) return

  const container = messageContainerIntel.value
  const { scrollTop, scrollHeight, clientHeight } = container

  // 更新滚动位置状态
  scrollPosition.value = scrollTop

  // 检查用户是否手动向上滚动
  userScrolledUp.value = scrollHeight - scrollTop > clientHeight + 150

  // 决定是否显示滚动按钮
  if (isIntelLoad.value) {
    // 流式输出过程中始终展示跳转到结尾按钮
    showScrollButton.value = true
  } else {
    showScrollButton.value = userScrolledUp.value
  }
}

const clearResumeTaskPolling = () => {
  if (resumeTaskPollTimer !== null) {
    clearTimeout(resumeTaskPollTimer)
    resumeTaskPollTimer = null
  }
}

const initResumes = () => {
  if (currentAgentType.value !== RESUME_AGENT_TYPE) {
    return
  }
  isSendResumeMsgPage.value = true
  resumeTaskState.text = ''
  resumeTaskState.ranking = []
  resumeTaskState.rankingResult = null
  resumeTaskState.executiveSummary = ''
  resumeTaskState.panelSummary = ''
  resumeTaskState.jdFile = null
  resumeTaskState.resumeFiles = []
  userInputContent.value = ''
}

const createNewConversation = () => {
  // 新建对话时清空侧边栏选中状态，避免旧会话仍保持高亮
  activeIndexIntel.value = ''
  if (currentAgentType.value === COMPARE_AGENT_TYPE) {
    showUploadButton.value = true
  }
  // 1. 重置消息列表
  intelQuery.messages = []
  intelCurrent.messages = []

  // 2. 清空输入内容
  intelQuestion.value = ''

  // 3. 清空文件列表
  fileInputAry.value = []
  jobJdFile.value = null
  initResumes()
  updateResumeTaskState()
  resetResumeTaskPersistContext()
  clearResumeTaskPolling()

  // 4. 重置加载状态 - 只有当前智能体不在加载中时才重置
  if (!loadingIntelId.value || loadingIntelId.value !== currentIntelId.value) {
    isIntelLoad.value = false
  }
  // 5.新建对话时 不展示流式输出框
  limitIntelLoading.value = false
  isIntelStop.value = false

  // 6. 重置会话ID
  conversationId.value = ''
  recordId.value = ''

  // 7. 重置评价相关
  commonQuestion.value = ''

  // 8. 停止进行中的请求 - 只有当前智能体不在加载中时才停止
  if (!loadingIntelId.value || loadingIntelId.value !== currentIntelId.value) {
    if (interval) {
      clearInterval(interval)
    }
    if (currentRequestUrl.value) {
      request.cancelRequest(currentRequestUrl.value)
    }
  }

  // 8. 重置UI状态
  dynamicRows.value = 1
  showScrollButton.value = false
  userScrolledUp.value = false

  // 9. 滚动到顶部
  nextTick(() => {
    // 滚动到对话顶部
    if (messageContainerIntel.value) {
      messageContainerIntel.value.scrollTop = 0
    }
  })
}

// 新增映射表
const agentTypeMap = {
  compare: {
    getChatFn: getImgRecognitionById,
    getListFn: getImageRecognitionsByUserId,
    chatUrl: '/AI/imageRecognition'
  },
  table: {
    getChatFn: getExcelChatById,
    getListFn: getExcelChatByUserId,
    chatUrl: '/AI/excelChat'
  },
  resume: {
    getChatFn: getResumeTaskById,
    getListFn: getResumeTaskByUserId,
    chatUrl: '/AI/agentChat'
  },
  default: {
    getChatFn: getAgentChatByChatId,
    getListFn: getAgentChatByAgentId,
    chatUrl: '/AI/agentChat'
  }
}

const cancelIntel = () => {}
// 点号变化逻辑
const updateDots = () => {
  if (dots.value.length >= 5) {
    dots.value = '.' // 重置为一个点
  } else {
    dots.value += '.' // 增加一个点
  }
}
const refreshData = () => {
  if (isIntelLoad.value) {
    ElMessage.warning('有问答正在进行中,请稍后再试')
    return
  }
  let ary = []
  if (activeIndexIntel.value || activeIndexIntel.value === 0) {
    const length = intelQuery.messages.length
    if (length === 1) {
      ary = intelQuery.messages[0].files
    } else if (length > 1) {
      if (intelQuery.messages[length - 1].role === 'user') {
        ary = intelQuery.messages[length - 1].files
      } else {
        ary = intelQuery.messages[length - 2].files
      }
    }
  }
  fileInputAry.value = ary
  submitSample(intelQuery.messages[intelQuery.messages.length - 2].content, true)
}
const showFileMenu = ref(false)
const showFileSample = val => {
  showFileMenu.value = !showFileMenu.value
}
const handleCommonClose = done => {
  // 这里可以添加一些关闭前的逻辑
  done()
}

const validResumesInput = () => {
  if (currentAgentType.value !== RESUME_AGENT_TYPE) {
    return true
  }
  const jdText = intelQuestion.value || ''
  const jdFileId = resolveUploadedFileId(jobJdFile.value)
  const resumeFileIds = Array.isArray(fileInputAry.value)
    ? fileInputAry.value
      .map(item => resolveUploadedFileId(item))
      .filter((id): id is string => !!id)
    : []
  const hasJdContent = !!jdText.trim() || !!jdFileId

  if (!hasJdContent) {
    ElMessage.warning('请提供岗位JD文本或上传JD文件')
    return false
  }

  if (resumeFileIds.length === 0) {
    ElMessage.warning('请至少上传一份简历文件')
    return false
  }
  initResumes()
  return true
}
const submitSampleSend = () => {
  if (!validResumesInput()) {
    return
  }
  if (currentAgentType.value === RESUME_AGENT_TYPE) {
    isSendResumeMsgPage.value = false
  }
  if (intelQuery.isLoading && loadingIntelId.value && loadingIntelId.value === currentIntelId.value) {
    stopQuery()
  } else {
    submitSample()
  }
}

const handleResumeSubmit = () => {
  submitSampleSend()
}
const stopQuery = async () => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo'))
  request
    .post('/AI/stop?userId=' + userInfo.id, {
      // showLoading: true
    })
    .then(res => {
      if (res.status) {
        cancelCurrentRequest()
      }
    })
    .catch(err => {})
}

const handleFileSelect = (val1, val2) => {
  showFileMenu.value = false
  if (!isLogin.value) {
    ElMessage.warning('请先登录再使用')
    return false
  }
  nextTick(() => {
    if (val1 === 'local') {
      fileRefs.value.openFile(val2, fileInputAry.value)
    } else {
      commonUploadModals.value.openFile(val2)
    }
  })
}

const handleResumeButtonClick = () => {
  handleFileSelect('local', 'sample')
}

const handleJobJdButtonClick = () => {
  if (!isLogin.value) {
    ElMessage.warning('请先登录再使用')
    return
  }
  nextTick(() => {
    fileRefs.value.openFile('jobJd')
  })
}

const showJobJdFile = () => {
  if (!hasJobJdFile.value) {
    return
  }
  fileAry.value = [jobJdFile.value]
  filePreRef.value.openFile('sample')
}

const removeJobJdFile = () => {
  jobJdFile.value = null
}
const deleteImg = index => {
  fileInputAry.value.splice(index, 1)
  if (!fileInputAry.value || fileInputAry.value.length === 0) {
    fileInputAry.value = []
  }
}

const limitIntelId = ref('')
// 终止请求方法
const cancelCurrentRequest = async val => {
  const { chatUrl } = agentTypeMap[currentAgentType.value] || agentTypeMap.default
  try {
    await Promise.race([
      Promise.resolve(request.cancelRequest(chatUrl)),
      new Promise(resolve => setTimeout(resolve, 3000))
    ])
  } catch (e) {
    // ignore cancel timeout or errors
  }
  ElMessage.success('请求已中止')

  isIntelLoad.value = false
  limitIntelLoading.value = false
  isIntelStop.value = true
  intelQuery.messages = JSON.parse(JSON.stringify(intelCurrent.messages))
  const mes = intelQuery.messages
  postSample(currentIntelId.value, mes, tempChatId.value)

  // 如果当前智能体类型与正在加载的智能体类型不一样，调用新建对话的函数
  if (loadingIntelType.value && loadingIntelType.value !== currentAgentType.value) {
    createNewConversation()
  }

  loadingIntelId.value = ''
  intelQuery.isLoading = false
}
const isObject = variable => {
  const type = Object.prototype.toString.call(variable)
  return type === '[object PointerEvent]' || type === '[object KeyboardEvent]'
}
const samplePost = event => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault() // 阻止默认的换行行为
    if (isIntelLoad.value) {
      ElMessage.warning('有问答正在进行中,请稍后再试')
      return
    }
    submitSample()
  }
}
const checkIntelData = val => {
  if (!isLogin.value) {
    ElMessage({
      message: '请先登录再使用',
      type: 'warning'
    })

    return false
  }
  if (isObject(val) && !intelQuestion.value) {
    val = ''
    ElMessage.warning('请输入您的问题再发送')
    return false
  }
  if (val && !isObject(val)) {
    intelQuestion.value = val
  }
  if (!intelQuestion.value) {
    ElMessage.warning('请输入您的问题再发送')
    return false
  }
  return true
}

const updateFinalTitleFromMessages = (messages: Array<Record<string, any>> = []) => {
  if (!Array.isArray(messages) || messages.length === 0) {
    finalTitle.value = ''
    return
  }

  const firstMessage = messages[0] || {}
  const content = typeof firstMessage.content === 'string' ? firstMessage.content : ''
  const fileNames = Array.isArray(firstMessage.files)
    ? firstMessage.files
        .map((item: Record<string, any>) => item?.originalFileName)
        .filter((name): name is string => typeof name === 'string' && name.length > 0)
        .join(',')
    : ''

  finalTitle.value = content || fileNames || ''
}

const postSample = async (agentId, mes, chatId) => {
  let num = parseInt(localStorage.getItem('count'))
  num = num + 1
  localStorage.setItem('count', num)
  const messagesForTitle = Array.isArray(mes)
    ? mes
    : Array.isArray(mes?.messages)
      ? mes.messages
      : []
  updateFinalTitleFromMessages(messagesForTitle as Array<Record<string, any>>)

  loadingIntelType.value = loadingIntelType.value || DEFAULT_AGENT_TYPE
  const operationFn = loadingIntelTypeHandlerMap.get(loadingIntelType.value)

  if (operationFn) {
    let saveAgentResult = await operationFn(chatId, mes, agentId)
    if (saveAgentResult.status) {
      // 如果当前智能体为加载对话的智能体，则可设置会话ID
      if (loadingIntelId.value && currentIntelId.value === loadingIntelId.value) {
        conversationId.value = saveAgentResult.data.id
        // 智能体对话保存完成，左侧列表选中第一个
        activeIndexIntel.value = 0
        await getHistory()
      }
    } else {
      ElMessage.warning(saveAgentResult.message)
    }
  } else {
    console.error('Unknown operation')
    throw new Error('Unknown operation')
  }
}

const upCommon = async () => {
  if (!isLogin.value) {
    ElMessage.warning('请先登录再使用')
    return false
  }
  if (isDisabled.value) return // 如果按钮已禁用，直接返回
  let id = recordId.value
  isDisabled.value = true
  // 2 秒后重新启用按钮
  setTimeout(() => {
    isDisabled.value = false
  }, 3000)

  request
    .post('/Agent/feedback', {
      id: conversationId.value,
      feedback: {
        agree: true,
        content: ''
      }
      // showLoading: true
    })
    .then(res => {
      if (res.status) {
        ElMessage.success('谢谢您的点赞,您的支持是我们最大的动力！')
      } else {
        ElMessage.error('评价失败,请稍后再试')
      }
    })
    .catch(err => {
      console.error(err)
    })
}
const downCommon = () => {
  commonVisible.value = true
}
const submitCommon = async () => {
  if (!isLogin.value) {
    ElMessage.warning('请先登录再使用')
    return false
  }
  request
    .post('/Agent/feedback', {
      id: conversationId.value,
      feedback: {
        agree: false,
        content: commonQuestion.value
      }
      // showLoading: true
    })
    .then(res => {
      if (res.status) {
        ElMessage.success('评价成功,我们会继续努力的！')
        commonVisible.value = false
        commonQuestion.value = ''
      } else {
        ElMessage.error('评价失败,请稍后再试')
      }
    })
    .catch(err => {
      commonVisible.value = false
      commonQuestion.value = ''
      console.error('获取回复失败:', err)
    })
}
// 自动滚动
const autoScroll = () => {
  nextTick(() => {
    if (!messageContainerIntel.value || userScrolledUp.value) return

    const container = messageContainerIntel.value
    const { scrollHeight } = container

    // 仅在用户靠近底部时自动滚动
    container.scrollTop = scrollHeight
  })
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainerIntel.value) {
      messageContainerIntel.value.scrollTop = messageContainerIntel.value.scrollHeight
      userScrolledUp.value = false
      showScrollButton.value = false
    }
  })
}

const quickJSONCheck = str => {
  if (typeof str !== 'string') return false
  str = str.trim()
  return (str.startsWith('{') && str.endsWith('}')) || (str.startsWith('[') && str.endsWith(']'))
}

const getLastPathSegment = path => {
  if (!path) return ''
  const str = String(path)
  const normalized = str.replace(/\\/g, '/')
  const segments = normalized.split('/')
  // 过滤空段，返回最后一段
  const last = segments.filter(Boolean).pop()
  return last || ''
}

const scrollToLastestMessage = () => {
  nextTick(() => {
    // 滚动到底部
    if (messageContainerIntel.value) {
      const messages = messageContainerIntel.value.children
      if (messages.length > 0) {
        const lastMessage = messages[messages.length - 2]
        // 滚动到最后一个消息的开头部分
        lastMessage.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
    }
  })
}

const setMessages = async (params, isRefresh = false) => {
  if (conversationId.value) {
    let chatResult = await getImgRecognitionById(conversationId.value)
    if (chatResult.status) {
      params.messages = chatResult.data?.imgMessages
      if (params.messages.length >= 2 && isRefresh) {
        params.messages.splice(-2)
      }
    }
  }
}

// 当当前智能体ID与正在跑的智能体ID不同时
const checkWhetherCreateNewChat = async () => {
  if (currentAgentType.value === RESUME_AGENT_TYPE) {
    clearResumeTaskPolling()
    updateResumeTaskState()
  }
  if (loadingIntelType.value && loadingIntelType.value !== currentAgentType.value) {
    createNewConversation()
  }
  if (!currentIntelId.value || !loadingIntelId.value) return
  if (currentIntelId.value === loadingIntelId.value) {
    // 如果当前智能体正在加载中，不创建新对话，保持当前状态
    return
  }
  if (!conversationId.value) {
    // 如果切到其他智能体且没有点击会话，则新建会话
    createNewConversation()
  } else {
    intelQuestion.value = ''
    isIntelStop.value = false
    limitIntelLoading.value = false
    fileInputAry.value = []
    jobJdFile.value = null
    await getChatByAgentChatId(conversationId.value)
  }
}

const submitOperationMap = new Map([
  [
    COMPARE_AGENT_TYPE,
    async (params, filesSample, isRefresh) => {
      let images = filesSample.map(file => file.fileId)
      params.messages = []
      await setMessages(params, isRefresh)
      params.content = intelQuestion.value
      params.images = images
      params.sessionId = conversationId.value
    }
  ],
  [
    TABLE_AGENT_TYPE,
    async (params, filesSample, _) => {
      params.model = 0
      params.excelFiles = filesSample.map(file => file.fileId)
      params.userId = currentIntelId.value
      params.sessionId = conversationId.value
      // params.messages = []
      // await setMessages(params, isRefresh)
    }
  ],
  [
    DEFAULT_AGENT_TYPE,
    async (params, filesSample, _) => {
      params.model = 0
      params.files = filesSample
      params.agentId = currentIntelId.value
      params.sessionId = conversationId.value
    }
  ]
])
submitOperationMap.set(RESUME_AGENT_TYPE, submitOperationMap.get(DEFAULT_AGENT_TYPE))

const assistantMsgHandleMap = new Map([
  [
    COMPARE_AGENT_TYPE,
    async (assistantMsg, type, content, _) => {
      if (type === 'model_streaming') {
        assistantMsg.after += content
        assistantMsg.thinking = assistantMsg.thinking || ''
      } else {
        assistantMsg.thinking = content
        await delay(250)
      }
      assistantMsg.hasSplit = true
    }
  ],
  [
    TABLE_AGENT_TYPE,
    async (assistantMsg, type, content, _, metadata) => {
      if (type === 'final_answer') {
        assistantMsg.after += content
      } else if (['iteration_update', 'agent_thinking'].includes(type)) {
        assistantMsg.thinking = content
        await delay(250)
      } else if (type === 'complete_context') {
        excelChatRepeat.value = {
          type,
          content,
          metadata
        }
      } else if (type === 'fileReady') {
        let { objectName, downloadUrl } = JSON.parse(content)
        assistantMsg.objectName = objectName || ''
        assistantMsg.downloadUrl = downloadUrl || ''
      } else if (lastMessage.value && lastMessage.value.indexOf('complete_context') !== -1) {
        excelChatRepeat.value = JSON.parse(lastMessage.value)
      }
      assistantMsg.hasSplit = true
    }
  ],
  [
    DEFAULT_AGENT_TYPE,
    async (assistantMsg, _, content, isThink) => {
      if (assistantMsg.hasSplit) {
        // 已遇到分隔符，内容追加到后半部分
        assistantMsg.after += content
      } else {
        const sp = isThink ? '</think>' : ''
        // 检查当前数据块是否包含分隔符
        const splitIndex = content.indexOf(sp)
        if (splitIndex === -1) {
          // 未找到分隔符，全部追加到前半部分
          assistantMsg.before += content
        } else {
          // 找到分隔符，分割内容
          assistantMsg.before += content.slice(0, splitIndex)
          assistantMsg.after += content.slice(splitIndex + sp.length)
          assistantMsg.hasSplit = true
        }
      }
    }
  ]
])
assistantMsgHandleMap.set(RESUME_AGENT_TYPE, assistantMsgHandleMap.get(DEFAULT_AGENT_TYPE))

let lastMessage = ref('')

const preSaveOperations = new Map([
  [
    COMPARE_AGENT_TYPE,
    async title => {
      const userInfo = JSON.parse(localStorage.getItem('userInfo'))
      let saveImgRecognitionResult = await saveImgRecognition({
        userId: userInfo.id,
        imgMessages: [],
        title: title,
        id: ''
      })
      if (saveImgRecognitionResult) {
        conversationId.value = saveImgRecognitionResult.data.id
        // 智能体对话保存完成，左侧列表选中第一个
        activeIndexIntel.value = 0
        await getHistory()
      }
    }
  ],
  [
    TABLE_AGENT_TYPE,
    async title => {
      const userInfo = JSON.parse(localStorage.getItem('userInfo'))
      let excelSaveResult = await saveExcelChat({
        userId: userInfo.id,
        id: '',
        agentId: currentIntelId.value,
        messages: [],
        title: title
      })
      if (excelSaveResult) {
        conversationId.value = excelSaveResult.data.id
        // 智能体对话保存完成，左侧列表选中第一个
        activeIndexIntel.value = 0
        await getHistory()
      }
    }
  ],
  [
    DEFAULT_AGENT_TYPE,
    async title => {
      const userInfo = JSON.parse(localStorage.getItem('userInfo'))
      let saveAgentChatResult = await saveAgentChat({
        userId: userInfo.id,
        id: '',
        agentId: currentIntelId.value,
        messages: [],
        title: title
      })
      if (saveAgentChatResult) {
        conversationId.value = saveAgentChatResult.data.id
        // 智能体对话保存完成，左侧列表选中第一个
        activeIndexIntel.value = 0
        await getHistory()
      }
    }
  ]
])
const resumePreSaveRequired = agentType => agentType !== RESUME_AGENT_TYPE

const submitSample = async (val, isRefresh) => {
  // 有问答正在进行不允许提交
  if (intelQuery && intelQuery.isLoading) {
    ElMessage.warning('有问答正在进行中,请稍后再试')
    return
  }

  if (fileInputAry.value.length > 0 && currentAgentType.value === COMPARE_AGENT_TYPE) {
    if (fileInputAry.value.length !== 2) {
      ElMessage.warning('请上传两张图片比较')
      return
    }
    showUploadButton.value = false
  }
  // 新建对话没有conversationId
  if (!conversationId.value && resumePreSaveRequired(currentAgentType.value)) {
    const operationFn = preSaveOperations.get(currentAgentType.value)
    if (operationFn) {
      const attachmentNames = [
        ...(hasJobJdFile.value && jobJdFile.value?.originalFileName
          ? [jobJdFile.value.originalFileName]
          : []),
        ...((fileInputAry.value || []).map(item => item.originalFileName))
      ]
      const trimmedQuestion = (intelQuestion.value || '').trim()
      let preSaveTitle = trimmedQuestion

      if (!preSaveTitle) {
        if (currentAgentType.value === RESUME_AGENT_TYPE) {
          const jdFileName =
            jobJdFile.value?.originalFileName ||
            jobJdFile.value?.fileName ||
            ''
          preSaveTitle = jdFileName
        } else {
          preSaveTitle = attachmentNames.filter(Boolean).join(',')
        }
      }

      await operationFn(preSaveTitle)
    } else {
      throw new Error('Unknown operation')
    }
  }
  // 提交的一瞬间使用临时变量保存当前对话ID
  if (conversationId.value) {
    tempChatId.value = conversationId.value
  } else {
    tempChatId.value = ''
  }
  // 设置正在加载的智能体类型
  loadingIntelType.value = currentAgentType.value

  const attachments = []
  const resumeAttachmentsMeta: Array<{ type: 'jobJd' | 'resume'; file: Record<string, any> }> = []
  if (hasJobJdFile.value) {
    attachments.push(jobJdFile.value)
    resumeAttachmentsMeta.push({
      type: 'jobJd',
      file: JSON.parse(JSON.stringify(jobJdFile.value))
    })
  }
  if (Array.isArray(fileInputAry.value) && fileInputAry.value.length > 0) {
    attachments.push(...fileInputAry.value)
    resumeAttachmentsMeta.push(
      ...fileInputAry.value
        .filter(Boolean)
        .map(item => ({ type: 'resume' as const, file: JSON.parse(JSON.stringify(item)) }))
    )
  }
  if (attachments.length === 0) {
    if (!checkIntelData(val)) {
      return
    }
  }
  if (val) {
    intelQuestion.value = val
  }
  const queryValue = intelQuestion.value
  isIntelStop.value = false
  limitQuery.value = intelQuestion.value
  limitIntelLoading.value = true
  dynamicRows.value = 1
  isIntelLoad.value = true
  loadingIntelId.value = currentIntelId.value
  const userInfo = JSON.parse(localStorage.getItem('userInfo'))
  if (intelQuery.messages.length === 1 && intelQuery.messages[0].files) {
    intelQuery.messages = []
  }
  let filesSample = []
  const normalizedFiles = []
  if (attachments.length > 0) {
    attachments.forEach(originalFile => {
      const fileItem = JSON.parse(JSON.stringify(originalFile))
      const fileIdValue = fileItem.fileId
      if (isPureObject(fileIdValue)) {
        filesSample.push(fileIdValue)
      } else if (fileIdValue !== undefined && fileIdValue !== null) {
        const objSample = {
          fileId: fileIdValue,
          local: fileItem.local
        }
        filesSample.push(objSample)
      }

      const localValue =
        fileIdValue && typeof fileIdValue === 'object' && fileIdValue.local === false
          ? fileIdValue.local
          : fileItem.local
      fileItem.local = typeof localValue === 'boolean' ? localValue : true
      fileItem.fileId =
        fileIdValue && typeof fileIdValue === 'object' && fileIdValue.fileId
          ? fileIdValue.fileId
          : fileIdValue
      normalizedFiles.push(fileItem)
    })
  }
  const currentData = {
    role: 'user',
    content: queryValue ? queryValue : '',
    files: toRaw(JSON.parse(JSON.stringify(normalizedFiles))),
    resumeAttachments: resumeAttachmentsMeta.map(item => ({
      type: item.type,
      file: item.file
    }))
  }
  let mes
  if (isRefresh) {
    intelQuery.messages.length -= 2
  }
  mes = JSON.parse(JSON.stringify(intelQuery))
  mes.messages.push(currentData)
  const params = JSON.parse(JSON.stringify(mes))
  const isResumeAgent = currentAgentType.value === RESUME_AGENT_TYPE
  for (let j = 0; j < params.messages.length; j++) {
    if (j % 2 === 0) {
      params.messages[j].role = 'user'
    } else {
      params.messages[j].role = 'assistant'
    }
    if (params.messages[j].resumeAttachments) {
      delete params.messages[j].resumeAttachments
    }
  }
  params.userId = userInfo.id
  const operationFn = submitOperationMap.get(currentAgentType.value)

  if (operationFn) {
    // 提交会话流式入参设置
    await operationFn(params, filesSample, isRefresh)
  } else {
    throw new Error('Unknown operation')
  }
  intelQuestion.value = ''
  if (!isResumeAgent) {
    interval = setInterval(updateDots, 500)
  }

  nextTick(() => {
    if (messageContainerIntel.value) {
      messageContainerIntel.value.scrollTop = messageContainerIntel.value.scrollHeight
    }
  })
  const assistantMsg = {
    role: 'assistant',
    content: '',
    before: '',
    after: '',
    hasSplit: false,
    isNewData: true,
    thinking: '',
    objectName: '',
    downloadUrl: ''
  }
  if (!isResumeAgent) {
    mes.messages.push(assistantMsg)
  }
  // 使用一个对象记录哪些 content 已经有 user 了
  intelCurrent.messages = mes.messages
  intelQuery.isLoading = !isResumeAgent
  const isThink = false
  fileInputAry.value = []
  fileAry.value = []
  jobJdFile.value = null
  try {
    if (isResumeAgent) {
      clearResumeTaskPolling()
      resetResumeTaskPersistContext()
      const jdAttachment = resumeAttachmentsMeta.find(item => item.type === 'jobJd')
      const resumeFileIds = resumeAttachmentsMeta
        .filter(item => item.type === 'resume')
        .map(item => resolveUploadedFileId(item.file))
        .filter((id): id is string => !!id)
      const jdFileId = jdAttachment ? resolveUploadedFileId(jdAttachment.file) : null
      const jdText = currentData.content || ''

      updateResumeTaskState({ isWaiting: true, isCompleted: false, batchId: '' })
      limitIntelLoading.value = false
      isIntelLoad.value = false
      loadingIntelId.value = ''
      try {
        const response = await createResumeAnalysis({
          userId: userInfo.id,
          jdText,
          jdFile: jdFileId,
          resumes: resumeFileIds
        })
        if (!response?.status) {
          updateResumeTaskState()
          resetResumeTaskPersistContext()
          ElMessage.error(response?.message || '发起简历分析失败，请稍后再试')
          return
        }
        const resumeTaskId = response?.data?.id || ''
        if (resumeTaskId) {
          conversationId.value = resumeTaskId
        }
        resumeTaskPersistContext.id = resumeTaskId
        resumeTaskPersistContext.resumeRepeat = response?.data?.resumeRepeat || null
        resumeTaskPersistContext.hasSaved = false
        activeIndexIntel.value = 0
        await getHistory()
        const batchId = response?.data?.resumeRepeat?.batch_id || ''
        if (!batchId) {
          updateResumeTaskState()
          resetResumeTaskPersistContext()
          ElMessage.error('未获取到筛选任务编号')
          return
        }
        updateResumeTaskState({ isWaiting: true, isCompleted: false, batchId })
        startResumeTaskPolling(batchId)
      } catch (error) {
        console.error('发起简历分析失败:', error)
        updateResumeTaskState()
        resetResumeTaskPersistContext()
        ElMessage.error('发起简历分析失败，请稍后再试')
      } finally {
        intelQuery.isLoading = false
      }
      return
    }
    // 替换为实际的后端接口地址
    const { chatUrl } = agentTypeMap[currentAgentType.value] || agentTypeMap.default
    const res = await fetch(import.meta.env.VITE_API_BASE_URL + chatUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(params)
    })
    // 处理流式数据
    const reader = res.body.getReader()
    if (res.status === 429) {
      ElMessage.error('服务器繁忙,请稍后再试')
      return
    }
    const decoder = new TextDecoder() // 启用流模式解码
    let buffer = '' // 缓冲区用于存储不完整的数据

    while (true) {
      const { value, done } = await reader.read()
      if (done) {
        clearInterval(interval)
        isIntelLoad.value = false
        limitIntelLoading.value = false
        intelQuery.isLoading = false
        limitIntelId.value = ''
        currentRequestUrl.value = ''

        let saveAgentId = currentIntelId.value
        if (!loadingIntelId.value || loadingIntelId.value === currentIntelId.value) {
          intelQuery.messages = JSON.parse(JSON.stringify(intelCurrent.messages))
          scrollToLastestMessage()
        }
        // 如果存在正在加载的智能体 则保存时保存此智能体
        if (loadingIntelId.value) {
          saveAgentId = loadingIntelId.value
        }
        await postSample(saveAgentId, JSON.parse(JSON.stringify(intelCurrent.messages)), tempChatId.value)
        await checkWhetherCreateNewChat()
        // 在执行完save以后再把loading状态清空，并清空正在加载的智能体类型
        if (!limitIntelLoading.value) {
          loadingIntelId.value = ''
          loadingIntelType.value = ''
        }
        break
      }
      buffer += decoder.decode(value, { stream: true })
      // 使用更安全的分割方式（避免截断 JSON 结构）[3](@ref)
      const chunks = buffer.split(/(?=data:)/g)
      // 修复chunks被删除第一个元素问题
      buffer = chunks.pop() || ''
      lastMessage.value = ''
      let msgBuffer = buffer.match(/data:\s*({[\s\S]*?})(?=\ndata:|\n\n|$)/)
      if (msgBuffer) {
        // 保存最后一个数据包
        lastMessage.value = msgBuffer[1]
      }
      if (quickJSONCheck(buffer)) {
        const jsonData = JSON.parse(buffer)
        if (jsonData.code === 400) {
          ElMessage.warning(jsonData.message)
        }
      }

      for (const chunk of chunks) {
        // 1. 修复正则匹配语法
        const jsonMatch = chunk.match(/data:\s*({[\s\S]*?})(?=\ndata:|\n\n|$)/)
        // 2. 添加条件判断包裹
        if (jsonMatch) {
          autoScroll()
          try {
            let oriData = JSON.parse(jsonMatch[1])
            const { content, type, metadata } = oriData
            const operationFn = assistantMsgHandleMap.get(currentAgentType.value)
            if (operationFn) {
              await operationFn(assistantMsg, type, content, isThink, metadata)
            }

            // 立即更新视图（更新最后一条消息）
            intelCurrent.messages.splice(-1, 1, {
              ...toRaw(assistantMsg),
              before: assistantMsg.before,
              after: assistantMsg.after,
              content: assistantMsg.before + assistantMsg.after, // 兼容旧字段
              thinking: assistantMsg.thinking,
              objectName: assistantMsg.objectName,
              downloadUrl: assistantMsg.downloadUrl
            })
          } catch (e) {
            loadingIntelId.value = ''
            console.error('JSON 解析失败:', jsonMatch[1], '错误:', e)
            ElMessage.error('数据格式异常')
          }
        }
      }
    }
  } catch (error) {
    intelQuery.isLoading = false
    loadingIntelId.value = ''
    isIntelLoad.value = false
    limitIntelId.value = ''
    ElMessage.error('服务器繁忙,请稍后再试')
  }
}
const showListFile = val => {
  fileAry.value = []
  fileAry.value.push(val)
  filePreRef.value.openFile('sample')
}

const isPureObject = value => {
  // 排除 null 和基础类型
  if (typeof value !== 'object' || value === null) return false

  // 排除数组、日期、正则等
  const proto = Object.getPrototypeOf(value)
  return proto === Object.prototype || proto === null
}

const resolveUploadedFileId = (file: Record<string, any> | null | undefined): string | null => {
  if (!file) return null
  const fileIdValue = (file as Record<string, any>).fileId

  if (typeof fileIdValue === 'string') {
    return fileIdValue
  }

  if (fileIdValue && typeof fileIdValue === 'object') {
    if (typeof fileIdValue.fileId === 'string') {
      return fileIdValue.fileId
    }
    if (typeof fileIdValue.id === 'string') {
      return fileIdValue.id
    }
  }

  if (typeof (file as Record<string, any>).id === 'string') {
    return (file as Record<string, any>).id
  }

  if (typeof (file as Record<string, any>).file_id === 'string') {
    return (file as Record<string, any>).file_id
  }

  return null
}

const persistResumeTaskResult = async (callbackData: Record<string, any>) => {
  if (resumeTaskPersistContext.hasSaved) {
    return
  }

  const resumeHandler = loadingIntelTypeHandlerMap.get(RESUME_AGENT_TYPE)
  if (!resumeHandler) {
    return
  }

  const resumeMessages = Array.isArray(intelCurrent.messages)
    ? JSON.parse(JSON.stringify(intelCurrent.messages))
    : []

  if (!resumeTaskPersistContext.id) {
    return
  }

  updateFinalTitleFromMessages(resumeMessages)

  try {
    const saveResult = await resumeHandler(
      resumeTaskPersistContext.id,
      resumeMessages,
      currentIntelId.value,
      {
        resumeRepeat: resumeTaskPersistContext.resumeRepeat || undefined,
        resumeTaskRepeat: callbackData,
        complete: !!callbackData?.task_completed
      }
    )

    if (saveResult?.status) {
      resumeTaskPersistContext.hasSaved = true
      try {
        await getHistory()
      } catch (historyError) {
        console.error('刷新简历任务列表失败:', historyError)
      }
    } else if (saveResult && !saveResult.status) {
      console.error('保存简历任务失败:', saveResult.message)
    }
  } catch (error) {
    console.error('保存简历任务失败:', error)
  }
}

const startResumeTaskPolling = (batchId: string) => {
  clearResumeTaskPolling()
  if (!batchId) {
    return
  }

  const poll = async () => {
    try {
      const callbackRes = await getResumeTaskCallback({
        task_id: batchId,
        task_type: RESUME_TASK_TYPE
      })

      const callbackData = callbackRes?.data || {}
      const rankingResult = callbackData?.task_json?.ranking_result || null
      const rankingList = Array.isArray(rankingResult?.ranking)
        ? rankingResult.ranking
        : []

      const executiveSummary = extractRankingSummary(rankingResult, [
        'executive_summary',
        'executiveSummary'
      ])
      const panelSummary = extractRankingSummary(rankingResult, [
        'panel_summary',
        'panelSummary'
      ])

      if (callbackRes?.status) {
        updateResumeTaskState({
          ranking: rankingList,
          rankingResult,
          executiveSummary,
          panelSummary
        })
      }

      if (callbackRes?.status && callbackData?.task_completed) {
        updateResumeTaskState({ isCompleted: true })
        await persistResumeTaskResult(callbackData)
        clearResumeTaskPolling()
        return
      }
    } catch (error) {
      console.error('获取简历任务进度失败:', error)
      updateResumeTaskState()
      clearResumeTaskPolling()
      return
    }

    resumeTaskPollTimer = setTimeout(poll, RESUME_TASK_POLL_INTERVAL)
  }

  poll()
}
const submitSampleFile = val => {
  // isDragOver.value = false
  for (let i = 0; i < val.length; i++) {
    val[i].fileName = decodeURIComponent(val[i].fileName)
    val[i].originalFileName = decodeURIComponent(val[i].originalFileName)
  }

  // Merge existing files with newly selected ones and deduplicate by name
  const merged = [...fileInputAry.value, ...val]
  const unique = []
  const nameSet = new Set()
  for (const item of merged) {
    const name = item.originalFileName || item.fileName
    if (!nameSet.has(name)) {
      nameSet.add(name)
      unique.push(item)
    }
  }

  if (unique.length > FILE_LIST_LIMIT) {
    ElMessage.warning("最多上传" + FILE_LIST_LIMIT + "个文件")
    return
  }
  fileAry.value = unique
  fileInputAry.value = JSON.parse(JSON.stringify(unique))
}

const getInfo = async id => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo'))
  request
    .post('/Agent/findAgentChat?userId=' + userInfo.id + '&agentId=' + id)
    .then(res => {
      if (res.status) {
        intelQuery.messages = res?.data[0]?.messages ? res?.data[0]?.messages : []
        if (res.data && res.data.length > 0) {
          recordId.value = res.data[0].id
        } else {
          recordId.value = ''
        }
        nextTick(() => {
          // 滚动到底部
          if (messageContainerIntel.value) {
            const messages = messageContainerIntel.value.children
            if (messages.length > 0) {
              const lastMessage = messages[messages.length - 2]
              if (lastMessage) {
                // 滚动到最后一个消息的开头部分
                lastMessage.scrollIntoView({ behavior: 'smooth', block: 'start' })
              }
            }
          }
        })
      }
    })
    .catch(err => {
      console.error('获取回复失败:', err)
    })
}

// 将点击获取页面详情的数据转换成标准格式
const chatDetailOperationMap = new Map([
  [
    COMPARE_AGENT_TYPE,
    (intelQuery, chatResults) => {
      intelQuery.messages = chatResults?.data?.imgMessages ? chatResults?.data?.imgMessages : []
      for (let i = 0; i < intelQuery.messages.length; i++) {
        // 如果是回答 补充思考过程字段
        if (intelQuery.messages[i].role === 'assistant') {
          intelQuery.messages[i].thinking = intelQuery.messages[i].thinking || ''
        }
        let oriContent = intelQuery.messages[i].content
        if (oriContent && Array.isArray(oriContent)) {
          let files = []
          for (let j = 0; j < oriContent.length; j++) {
            if (oriContent[j].type === 'image_url') {
              files.push(oriContent[j]?.image_url?.image)
            }
          }
          intelQuery.messages[i].files = files
          intelQuery.messages[i].content = oriContent.find(content_item => content_item.type === 'text').text
        }
      }
    }
  ],
  [
    DEFAULT_AGENT_TYPE,
    (intelQuery, chatResults) => {
      intelQuery.messages = chatResults?.data?.messages ? chatResults?.data?.messages : []
    }
  ],
  [
    TABLE_AGENT_TYPE,
    (intelQuery, chatResults) => {
      intelQuery.messages = chatResults?.data?.messages ? chatResults?.data?.messages : []
    }
  ]
])
chatDetailOperationMap.set(RESUME_AGENT_TYPE, (intelQuery, chatResults) => {
  isSendResumeMsgPage.value = false
  const resumeData = chatResults?.data || {}

  const taskRepeatMessages = resumeData?.resumeTaskRepeat?.messages
  const fallbackMessages = resumeData?.messages
  if (Array.isArray(taskRepeatMessages)) {
    intelQuery.messages = taskRepeatMessages
  } else if (Array.isArray(fallbackMessages)) {
    intelQuery.messages = fallbackMessages
  } else {
    intelQuery.messages = []
  }

  resumeTaskPersistContext.id = resumeData?.id || ''
  resumeTaskPersistContext.resumeRepeat = resumeData?.resumeRepeat || null

  const isTaskCompletedFlag =
    typeof resumeData?.resumeTaskRepeat?.task_completed === 'boolean'
      ? resumeData.resumeTaskRepeat.task_completed
      : !!resumeData?.complete

  resumeTaskPersistContext.hasSaved = isTaskCompletedFlag

  const rankingResult =
    resumeData?.resumeTaskRepeat?.task_json?.ranking_result ||
    resumeData?.rankingResult ||
    null

  const rankingList = Array.isArray(resumeData?.ranking)
    ? resumeData.ranking
    : Array.isArray(rankingResult?.ranking)
      ? rankingResult.ranking
      : []

  const executiveSummary = extractRankingSummary(rankingResult, [
    'executive_summary',
    'executiveSummary'
  ])
  const panelSummary = extractRankingSummary(rankingResult, [
    'panel_summary',
    'panelSummary'
  ])

  const batchId = resumeData?.resumeRepeat?.batch_id || ''
  const isCompleted = isTaskCompletedFlag
  const shouldWait = !!batchId && !isCompleted

  clearResumeTaskPolling()
  if (shouldWait) {
    startResumeTaskPolling(batchId)
  }

  updateResumeTaskState({
    isWaiting: shouldWait,
    isCompleted,
    batchId,
    ranking: rankingList,
    rankingResult,
    jdFile: resumeData?.JDFile || null,
    resumeFiles: Array.isArray(resumeData?.resumeFiles)
      ? resumeData.resumeFiles
      : [],
    text: resumeData?.text || '',
    executiveSummary,
    panelSummary
  })
})

const getChatByAgentChatId = async chatId => {
  showUploadButton.value = currentAgentType.value !== COMPARE_AGENT_TYPE
  conversationId.value = chatId
  if (intelQuery.isLoading) {
    // 如果当前对话ID与正在输出的流式回答ID一致 则显示流式问答框 否则不显示
    limitIntelLoading.value = chatId === tempChatId.value
  }
  const { getChatFn } = agentTypeMap[currentAgentType.value] || agentTypeMap.default
  const chatResults = await getChatFn(chatId)
  if (chatResults.status) {
    const operationFn = chatDetailOperationMap.get(currentAgentType.value)
    if (operationFn) {
      operationFn(intelQuery, chatResults)
    }

    if (chatResults.data) {
      recordId.value = chatResults.data.id
    } else {
      recordId.value = ''
    }
    nextTick(() => {
      // 滚动到底部
      if (messageContainerIntel.value) {
        const messages = messageContainerIntel.value.children
        if (messages.length > 0) {
          const lastMessage = messages[messages.length - 2]
          if (lastMessage) {
            // 滚动到最后一个消息的开头部分
            lastMessage.scrollIntoView({ behavior: 'smooth', block: 'start' })
          }
        }
      }
    })
  } else if (currentAgentType.value === RESUME_AGENT_TYPE) {
    updateResumeTaskState()
    resetResumeTaskPersistContext()
  }
}
const getHistory = async val => {
  const { getListFn } = agentTypeMap[currentAgentType.value] || agentTypeMap.default
  const agentHistoryList = await getListFn(currentIntelId.value, val)

  if (agentHistoryList.status) {
    const historyData = Array.isArray(agentHistoryList.data) ? [...agentHistoryList.data] : []
    switch (currentAgentType.value) {
      case COMPARE_AGENT_TYPE:
        historyData.forEach(chat => {
          chat.agentChatId = chat.imgRecognitionId
        })
        break
      case TABLE_AGENT_TYPE:
        historyData.forEach(chat => {
          chat.agentChatId = chat.chatId
        })
        break
      case RESUME_AGENT_TYPE:
        historyData.forEach(chat => {
          chat.agentChatId = chat.id
          if (!chat.title && chat.resumeTitle) {
            chat.title = chat.resumeTitle
          }
        })
        break
      default:
        break
    }
    agentChatList.value = historyData
  }
}

// 点击新建对话按钮新建对话
const clickCreateNewConversation = () => {
  if (intelQuery.isLoading) {
    ElMessage.warning('有问答正在进行中,请稍后再试')
    return
  }
  createNewConversation()
}
// 组件挂载时订阅事件
onMounted(() => {
  eventBus.on('getHistoryData', getHistory)
  eventBus.on('closeIntel', cancelIntel)

  eventBus.on('submit-sampleFile', submitSampleFile)
  eventBus.on('getRecord', getInfo)
  eventBus.on('getChatByAgentChatId', getChatByAgentChatId)
  eventBus.on('createNewConversation', clickCreateNewConversation)
  activeIndexIntel.value = -1
  fileInputAry.value = []
  jobJdFile.value = null

  // 检查当前智能体是否正在进行对话
  if (loadingIntelId.value && loadingIntelId.value === currentIntelId.value && isIntelLoad.value) {
    // 如果当前智能体正在进行对话，保持加载状态
    // 这样可以确保按钮状态正确显示
    limitIntelLoading.value = tempChatId.value === conversationId.value
  }

  // 初始化先清空列表 提升用户体验
  agentChatList.value = []
  getHistory()
  // 只有当前智能体没有正在进行对话时才重置流式输出状态
  if (!loadingIntelId.value || loadingIntelId.value !== currentIntelId.value || !isIntelLoad.value) {
    limitIntelLoading.value = false
    intelQuery.messages = []
  }
})
// 组件卸载时关闭 SSE 连接
onUnmounted(() => {
  eventBus.off('getHistoryData', getHistory)
  eventBus.off('closeIntel', cancelIntel)
  eventBus.off('submit-sampleFile', submitSampleFile)
  eventBus.off('getRecord', getInfo)
  eventBus.off('getChatByAgentChatId', getChatByAgentChatId)
  eventBus.off('createNewConversation', clickCreateNewConversation)
  if (interval) {
    clearInterval(interval)
  }
  clearResumeTaskPolling()
  resetResumeTaskPersistContext()
  updateResumeTaskState()
  // 卸载时清空缓存文件
  fileInputAry.value = []
  jobJdFile.value = null
})
</script>
<style lang="less" scoped>
.scroll-to-bottom {
  position: absolute;
  right: 30px;
  bottom: 120px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #1b6cff;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(27, 108, 255, 0.3);
  transition: all 0.3s ease;
  z-index: 10;
}

/* 旋转圆环 */
.loading-ring {
  position: absolute;
  width: 52px; /* 比按钮稍大一些 */
  height: 52px; /* 比按钮稍大一些 */
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-top: 2px solid #66c7d8;
  border-radius: 50%;
  animation: spin 1.2s linear infinite;
}

@keyframes spin {
  0% {
    transform: translate(-50%, -50%) rotate(0deg);
  }
  100% {
    transform: translate(-50%, -50%) rotate(360deg);
  }
}

.scroll-to-bottom:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(27, 108, 255, 0.4);
}

.scroll-to-bottom svg {
  width: 24px;
  height: 24px;
  color: white;
}
.query_common {
  margin-top: 20px;
  width: 100%;
  display: flex;
  .query_common_img {
    width: 18px;
    height: 18px;
    cursor: pointer;
  }
}
.sample_item::-webkit-scrollbar {
  width: 1px; /* 滚动条宽度 */
}
.sample_item::-webkit-scrollbar-track {
  background: #fff; /* 轨道背景颜色 */
  border-radius: 0px; /* 轨道圆角 */
}
.sample_item::-webkit-scrollbar-thumb {
  background: #fff; /* 滑块颜色 */
  border-radius: 0px; /* 滑块圆角 */
  border: 1px solid #fff; /* 滑块边框 */
}
.sample_item::-webkit-scrollbar-thumb:hover {
  background: #fff; /* 滑块悬停时的颜色 */
}
.sample_item {
  width: 100%;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
  margin-top: 20px;
  scroll-behavior: smooth;
  .content_title {
    color: #333333;
    padding-top: 10px;
    font-size: 16px;
  }
  .content_tip {
    display: flex;
    flex-direction: row;
    margin-top: 20px;
    .content_robot {
      width: 46px;
      height: 46px;
      display: flex;
      justify-content: center;
      align-items: center;
      img {
        width: 36px;
        height: 36px;
      }
    }
    .tip_text {
      width: 100%;
      background-color: #fafafa;
      padding: 0px 20px;
      font-size: 14px;
      height: 46px;
      line-height: 46px;
      border-radius: 10px;
      letter-spacing: 1px;
      box-sizing: border-box;
      margin-left: 15px;
    }
  }
  .sample_chat {
    font-size: 14px;
    letter-spacing: 1px;
    width: 100%;
    .sample_chat_file {
      display: flex;
      flex-wrap: nowrap; /* 不允许换行 */
      gap: 10px 20px; /* 元素间距(可选) */
      justify-content: end; /* 左对齐(默认) */
      flex-direction: row;
      .item_files {
        display: flex;
        font-size: 14px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        padding: 7px 15px;
        color: rgb(51, 51, 51);
        background-color: #eff6ff;
        display: flex;
        align-items: center;
        border-radius: 10px;
        cursor: pointer;
        box-sizing: border-box;
        flex: 1 0 calc(20% - 25px); /* 基础宽度25% 减间距 */
        max-width: calc(20% - 25px); /* 防止内容撑破 */
        .file_name {
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }
    }
    .sample_chat_query {
      background-color: #1b6cff;
      border-radius: 10px;
      padding: 13px 15px;
      float: right;
      color: #fff;
      max-width: 600px;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
}
.tip_load {
  font-size: 12px;
  padding-left: 5px;
  letter-spacing: 1px;
  line-height: 20px;
}
.stream-response a {
  cursor: pointer !important;
}
.select_content {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  border-radius: 10px;
  flex-direction: column;
  margin-bottom: 10px;
  .textarea {
    width: 862px;
  }
}
  .tooltip-wrapper {
    position: relative;
    display: flex;
  }
  .sampleArea {
    .el-textarea__inner {
      padding: 18px 100px 18px 15px !important;
    }
}
.sampleAreaAry {
  .el-textarea__inner {
    padding: 56px 100px 18px 15px !important;
  }
}
.textarea {
  width: 726px;
  position: relative;
  /* 去掉 textarea 右下角的小图标 */
  .custom-input {
    :deep(.el-textarea__inner) {
      resize: none; /* 禁用调整大小功能 */
      scrollbar-width: thin;
      scrollbar-color: #e5e7eb transparent;
    }
    :deep(.el-textarea__inner::-webkit-scrollbar) {
      width: 8px !important;
    }
    :deep(.el-textarea__inner::-webkit-scrollbar-track) {
      background: transparent !important;
    }
    :deep(.el-textarea__inner::-webkit-scrollbar-thumb) {
      background: #e5e7eb !important;
      border-radius: 4px;
      border: none;
    }
  }

  .send-icon {
    position: absolute;
    right: 20px;
    bottom: 13px;
    cursor: pointer;
    transition: color 0.3s;
    display: flex;
  }
  .send-icon img {
    width: 30px;
    height: 30px;
  }
  .resume-icon {
    width: 40px;
    height: 40px;
    margin-right: 10px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background 0.2s ease;
    cursor: pointer;
  }
  .resume-icon img {
    width: 40px;
    height: 40px;
  }
  .resume-icon.is-hovered {
    background: #eee;
  }
  .filesList {
    position: absolute;
    top: 10px;
    left: 10px;
    display: flex;
    flex-wrap: wrap;
    font-size: 12px;
  }
  .filesList > div {
    margin-left: 10px;
    max-width: 80px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    display: flex;
    align-items: center;
    background-color: #f5f5f5;
    padding: 6px 10px;
    border-radius: 6px;
    position: relative;
  }
  .filesList > div:first-of-type {
    margin-left: 5px;
  }
  .file-remove {
    position: absolute;
    width: 16px;
    height: 16px;
    right: 0px;
    top: 0px;
    cursor: pointer;
    display: flex;
    justify-content: center;
    align-items: center;
  }
  .job-jd-name {
    width: 60px;
  }
}

.empty {
  width: 80%;
  margin-left: 10%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  .empty_create {
    width: 140px;
    height: 40px;
    border-radius: 10px;
    cursor: pointer;
    background-color: #1b6cff;
    color: #fff;
    text-align: center;
    line-height: 40px;
    font-size: 14px;
    margin-top: 40px;
  }
  .empty_text {
    color: #6a6a6a;
    font-size: 16px;
    margin-top: 40px;
  }
  .empty_img {
    width: 100px;
    height: 100px;
    img {
      width: 100%;
      height: 100%;
    }
  }
}
.create_main {
  width: 80%;
  margin-left: 10%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  background-color: #fff;
  .create_ask {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: flex-start;
    .main_content::-webkit-scrollbar {
      width: 1px; /* 滚动条宽度 */
    }
    .main_content::-webkit-scrollbar-track {
      background: #fff; /* 轨道背景颜色 */
      border-radius: 0px; /* 轨道圆角 */
    }
    .main_content::-webkit-scrollbar-thumb {
      background: #fff; /* 轨道背景颜色 */
      border-radius: 0px; /* 滑块圆角 */
      border: 1px solid #fff; /* 滑块边框 */
    }
    .main_content::-webkit-scrollbar-thumb:hover {
      background: #fff; /* 滑块悬停时的颜色 */
    }
    .main_content {
      width: 862px;
      box-sizing: border-box;
      overflow-y: auto;
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      margin: 0 auto 10px;
    }
  }
  .create_title {
    font-size: 18px;
    width: 100%;
    color: #262626;
    padding-top: 25px;
    display: flex;
    height: 26px;
    line-height: 26px;
    align-items: center;
    .create_back {
      width: 26px;
      height: 26px;
      display: flex;
      justify-content: center;
      align-items: center;
      box-sizing: border-box;
      border: 1px solid #d0e4ff;
      border-radius: 26px;
      cursor: pointer;
      img {
        width: 14px;
        height: 14px;
        float: left;
      }
    }
  }
  .create_content {
    width: 726px;
    display: flex;
    flex-direction: column;
    margin-top: 100px;
    color: #333333;
    font-size: 14px;
    line-height: 18px;
    margin-left: 50px;
    .create_name {
      display: flex;
      flex-direction: column;
      .create_input {
        margin-top: 5px;
        :deep(.el-input__wrapper) {
          border: 1px solid #b7b8b9;
          height: 36px;
          border-radius: 4px;
          line-height: 36px;
          box-sizing: border-box;
          padding-left: 15px;
        }
      }
    }

    .create_set {
      display: flex;
      flex-direction: column;
      margin-top: 30px;
      position: relative;
      .create_ai {
        width: 116px;
        border-radius: 4px;
        height: 32px;
        font-size: 14px;
        text-align: center;
        box-sizing: border-box;
        background-color: #e6f4ff;
        color: #1b6cff;
        line-height: 32px;
        position: absolute;
        text-indent: 20px;
        right: 0;
        top: -15px;
        background-image: url('@/assets/ai.png');
        background-repeat: no-repeat;
        background-size: 19px 16px;
        background-position: 16px 8px;
        cursor: pointer;
      }
      .create_loading {
        width: 86px;
        border-radius: 4px;
        height: 32px;
        font-size: 14px;
        text-align: center;
        box-sizing: border-box;
        background-color: #e6f4ff;
        color: #1b6cff;
        line-height: 32px;
        position: absolute;
        text-indent: 20px;
        right: 0;
        top: -15px;
        background-image: url('@/assets/loading.gif');
        background-repeat: no-repeat;
        background-size: 19px 16px;
        background-position: 16px 8px;
        cursor: pointer;
      }
      .create_input {
        margin-top: 5px;
        :deep(.el-textarea__inner) {
          border-radius: 4px !important;
          padding: 10px 15px !important;
          height: 200px;
          resize: none;
          border-radius: 4px;
        }
      }
    }
    .create_btn {
      display: flex;
      margin-top: 25px;
      justify-content: center;
      font-size: 14px;
      .create_cancel {
        width: 120px;
        height: 32px;
        text-align: center;
        line-height: 32px;
        border: 1px solid #dedede;
        border-radius: 4px;
        color: #333333;
        cursor: pointer;
      }
      .create_confirm {
        width: 120px;
        height: 32px;
        text-align: center;
        line-height: 32px;
        border: 1px solid #1b6cff;
        border-radius: 4px;
        background-color: #1b6cff;
        color: #fff;
        margin-left: 10px;
        cursor: pointer;
      }
    }
  }
}
</style>

<template>
  <DragUpload
    ref="dragUploads"
    @submit-tran="submitFileTran"
    @submit-final="submitFileFinal"
    v-if="isDragOver && isDragUploadPageType()"
  ></DragUpload>
  <template v-if="!isDragOver">
    <TranView
      ref="tranViewRef"
      v-if="pageType === 'tran'"
      v-model:selectedLan="selectedLan"
      v-model:newQuestion="newQuestion"
      :page-type="pageType"
      :is-drag-over="isDragOver"
      :trans-quest="transQuest"
      :limit-tran-loading="limitTranLoading"
      :current-trans-data="currentTransData"
      :trans-data="transData"
      :translation-process="translationDocumentProcess"
      :translation-final="translationDocumentFinal"
      :dots="dots"
      :is-supported-file-type="isSupportedFileType"
      :get-file-icon="getFileIcon"
      :show-pre-file="showPreFile"
      :refresh-data="refreshData"
      :up-common="upCommon"
      :down-common="downCommon"
      :lan-list="lanList"
      :tran-post="tranPost"
      :handle-shift-enter="handleShiftEnter"
      :dynamic-row-final="dynamicRowFinal"
      :show-file="showFile"
      :show-file-menu="showFileMenu"
      :handle-file-select="handleFileSelect"
      :current-agent-type="currentAgentType"
      :submit-tran-send="submitTranSend"
      :is-export-translation-document="useTranslationDocument"
      :toggle-export-translation-document="toggleTranslationDocument"
      :open-term-library="openKnowledge"
    />
    <FinalView
      ref="finalViewRef"
      v-else-if="pageType === 'final'"
      v-model:newQuestion="newQuestion"
      :page-type="pageType"
      :is-drag-over="isDragOver"
      :is-supported-file-type="isSupportedFileType"
      :final-quest="finalQuest"
      :get-file-icon="getFileIcon"
      :show-pre-file="showPreFile"
      :doc-ing="docIng"
      :dots="dots"
      :final-data="finalData"
      :refresh-data="refreshData"
      :up-common="upCommon"
      :down-common="downCommon"
      :dynamic-row-final="dynamicRowFinal"
      :handle-shift-enter="handleShiftEnter"
      :show-file="showFile"
      :show-file-menu="showFileMenu"
      :handle-file-select="handleFileSelect"
      :current-agent-type="currentAgentType"
      :submit-final-send="submitFinalSend"
      :final-post="finalPost"
      :final-ing="finalIng"
      :current-index="currentIndex"
      :active-index="activeIndex"
    />
    <template v-else>
      <div
        class="main_content"
        :style="{ marginBottom: isDragOver ? '0px' : '10px' }"
        ref="messageContainerTran"
      >
        <div
          class="title"
          v-if="isQueryPageType() || (pageType === 'sample' && !isDragOver)"
        >
          <img src="@/assets/logo2.png" class="title_src" />
          <div>
            <div class="title_top" style="line-height: 33px; font-weight: bold">
              Hello!我是立讯技术百事通，有什么问题欢迎咨询
            </div>
            <div class="title_item">
              <span>我可以帮您做这些事情</span>
            </div>
          </div>
        </div>
        <div class="content_list" v-if="isQueryPageType()">
          <HotSearch :list="hotSearchList" @item-click="submitQuestionCurrent" />
          <KnowledgeWorkshop @change-type="changeType" />
        </div>
        <div class="content_list" v-if="pageType === 'sample' && !isDragOver">
          <HotSearch :list="historyList" @item-click="submitSampleTitle" />
          <KnowledgeWorkshop @change-type="changeType" />
        </div>
      </div>
      <div class="select_content">
        <div
          class="tran_select"
          v-if="isSelectorPageType() && !isDragOver"
        >
          <el-radio-group v-model="selectedMode" @change="changeMode" :disabled="isSampleLoad" class="chat-type-group">
            <el-radio-button label="通用模式" value="通用模式">通用模式</el-radio-button>
            <el-radio-button label="人资行政专题" value="人资行政专题">人资行政专题</el-radio-button>
            <el-radio-button label="IT专题" value="IT专题">IT专题</el-radio-button>
            <el-tooltip content="该模式仅支持通过office网络访问" placement="top" v-if="isLaw === 'true' && !isNet">
              <el-radio-button label="法务专题" value="法务专题" disabled>法务专题</el-radio-button>
            </el-tooltip>
            <el-radio-button label="法务专题" value="法务专题" v-if="isLaw === 'true' && isNet">法务专题</el-radio-button>
<!--            <el-tooltip content="该模式仅支持通过office网络访问" placement="top" v-if="enableBoardOffice === 'true' && !isNet">-->
<!--              <el-radio-button label="董办专题" value="董办专题" disabled>董办领域</el-radio按钮>-->
<!--            </el-tooltip>-->
<!--            <el-radio-button label="董办专题" value="董办专题" v-if="enableBoardOffice === 'true' && isNet">董办领域</el-radio按钮>-->
          </el-radio-group>
        </div>
        <div class="textarea" v-if="isTextareaPageType()">
          <el-input v-select-all-on-ctrl-a
            v-model="newQuestion"
            placeholder="请输入您的问题,换行请按下Shift+Enter"
            style="width: 100%"
            class="custom-input topic-textarea"
            clearable
            @keydown.enter.prevent="summitPost"
            @keyup.shift.enter.prevent="handleShiftEnter('textareaInputQuery', $event)"
            ref="textareaInputQuery"
            type="textarea"
            :maxlength="4096"
            :rows="dynamicRows"
          />
          <div class="send-icon">
            <div
              class="tooltip-wrapper"
              @mouseenter="showModelTip = true"
              @mouseleave="showModelTip = false"
              v-if="isDeepModePageType()"
            >
              <img
                v-if="deepType"
                :src="deepSelect"
                class="arrow"
                @click="checkDeepType"
                :style="{'margin-right': '10px', 'background-color': showModelTip ? '#E8F2FF' : 'transparent','border-radius': '10px'}"
              />
              <img
                v-else
                :src="deep"
                class="arrow"
                @click="checkDeepType"
                :style="{'margin-right': '10px', 'background-color': showModelTip ? '#EEEEEE' : 'transparent','border-radius': '10px'}"
              />
              <transition name="fade">
                <div v-if="showModelTip" class="tooltip">{{ !deepType ? '切换成深度思考模式' : '切换成普通模式' }}</div>
              </transition>
            </div>
            <img
              :src="
                isSampleLoad && (currentIndex || currentIndex === 0) && currentIndex === activeIndex
                  ? imageC
                  : newQuestion
                    ? imageB
                    : imageA
              "
              class="arrow"
              @click="submitQuestionSend"
              v-if="pageType === 'query'"
            />
            <img
              :src="
                isSampleLoad && (currentIndex || currentIndex === 0) && currentIndex === activeIndex
                  ? imageC
                  : newQuestion
                    ? imageB
                    : imageA
              "
              class="arrow"
              @click="submitItSend"
              v-if="pageType === 'it'"
            />
            <img
              :src="
                isSampleLoad && (currentIndex || currentIndex === 0) && currentIndex === activeIndex
                  ? imageC
                  : newQuestion
                    ? imageB
                    : imageA
              "
              class="arrow"
              @click="submitLawSend"
              v-if="isNet && (pageType === 'law' || pageType === 'board')"
            />
          </div>
        </div>
        <div class="textarea sampleArea" v-if="pageType === 'sample' && !isDragOver">
          <el-input v-select-all-on-ctrl-a
            v-model="newQuestion"
            placeholder="请输入您的问题,换行请按下Shift+Enter"
            style="width: 100%"
            class="custom-input"
            clearable
            @keydown.enter.prevent="samplePost"
            @keyup.shift.enter.prevent="handleShiftEnter('textareaInputSampleCurrent', $event)"
            ref="textareaInputSampleCurrent"
            type="textarea"
            :maxlength="4096"
            :rows="dynamicRows"
          />
          <div class="send-icon">
            <div class="tooltip-wrapper" ref="wrapperRef">
              <img
                src="@/assets/file.png"
                class="arrow"
                @click="showFile('sample')"
                style="margin-right: 10px"
              />
              <FileMenu
                :showFileMenu="showFileMenu"
                :handleFileSelect="handleFileSelect"
                localType="sample"
                knowledgeType="sample"
                :currentAgentType="currentAgentType"
              />
            </div>
            <div class="tooltip-wrapper" @mouseenter="showModelTip = true" @mouseleave="showModelTip = false">
              <img v-if="deepType" :src="deepSelect" class="arrow" @click="checkDeepType" :style="{'margin-right': '10px', 'background-color': showModelTip ? '#E8F2FF' : 'transparent','border-radius': '10px'}" />
              <img v-else :src="deep" class="arrow" @click="checkDeepType" :style="{'margin-right': '10px', 'background-color': showModelTip ? '#EEEEEE' : 'transparent','border-radius': '10px'}" />
              <transition name="fade">
                <div v-if="showModelTip" class="tooltip">{{ !deepType ? '切换成深度思考模式' : '切换成普通模式' }}</div>
              </transition>
            </div>
            <div class="tooltip-wrapper" @mouseenter="showKnowledgeTip = true" @mouseleave="showKnowledgeTip = false">
              <img v-if="showKnowledgeTip" :src="useKnowledge ? activeKnowledgeHover : inactiveKnowledgeHover" class="arrow" @click="toggleKnowledge" style="margin-right: 10px" />
              <img v-else :src="useKnowledge ? activeKnowledge : inactiveKnowledge" class="arrow" @click="toggleKnowledge" style="margin-right: 10px" />
              <transition name="fade">
                <div v-if="showKnowledgeTip" class="tooltip">{{ !useKnowledge ? '引用个人知识库文件问答' : '不使用个人知识库文件' }}</div>
              </transition>
            </div>
            <img
              :src="
                isSampleLoad && (currentIndex || currentIndex === 0) && currentIndex === activeIndex
                  ? imageC
                  : newQuestion || fileInputAry.length > 0
                    ? imageB
                    : imageA
              "
              class="arrow"
              @click="submitSampleSend"
            />
          </div>
        </div>
      </div>
    </template>
  </template>
  <FileUpload ref="fileRef" @submit-tran="submitFileTran" @submit-final="submitFileFinal"></FileUpload>
  <FilePreUpload ref="filePreRef"></FilePreUpload>
  <commonUploadModal
    ref="commonUploadModals"
    @submit-tran="submitFileTran"
    @submit-final="submitFileFinal"
  ></commonUploadModal>
  <Knowledge ref="knowledge"></Knowledge>
</template>


<script setup>
import { ref, nextTick, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { ElMessage } from 'element-plus' // 引入 ElMessage
import FileUpload from './fileUploadModal.vue'
import FilePreUpload from './filePreModal.vue'
import Knowledge from './KnowledgeModal.vue'
import commonUploadModal from './commonUploadModal.vue'
import DragUpload from './dragUpload.vue'
import TranView from './tran/TranView.vue'
import FinalView from './final/FinalView.vue'
import { useShared } from '@/utils/useShared'
import { getFileImgByOriginFile } from '@/utils/common.js'
import imageB from '@/assets/arrow_blue.png'
import imageA from '@/assets/arrow_gray.png'
import imageC from '@/assets/stop.png'
import deep from '@/assets/deep.png'
import deepSelect from '@/assets/deepSelect.png'
import activeKnowledge from '@/assets/active_knowledge.png'
import inactiveKnowledge from '@/assets/inactive_knowledge.png'
import activeKnowledgeHover from '@/assets/active_knowledge_hover.png'
import inactiveKnowledgeHover from '@/assets/inactive_knowledge_hover.png'
import FileMenu from './options/fileMenu.vue'
import HotSearch from './options/HotSearch.vue'
import KnowledgeWorkshop from './options/KnowledgeWorkshop.vue'
const emit = defineEmits([
  'submit-tran',
  'submit-final',
  'cancel-currentRequest',
  'submit-question',
  'submit-sample-title',
  'sample-post',
  'summit-post',
  'submit-tranSend',
  'submit-finalSend',
  'up-common',
  'down-common',
  'refresh-data',
  'submit-questionSend',
  'submit-sampleSend',
  'submit-itSend',
  'submit-lawSend',
  'fetch-chat-list'
])
const {
  selectedMode,
  newQuestion,
  pageType,
  dynamicRows,
  finalData,
  handleShiftEnter,
  textareaInputQuery,
  textareaInputSampleCurrent,
  textareaInputTran,
  textareaInputFinal,
  finalIng,
  docIng,
  isSampleLoad,
  selectedLan,
  changeMode,
  transData,
  currentTransData,
  translationDocumentProcess,
  translationDocumentFinal,
  limitTranLoading,
  activeIndex,
  finalQuest,
  transQuest,
  dots,
  fileObj,
  fileAry,
  deepType,
  checkDeepType,
  toggleKnowledge,
  toggleTranslationDocument,
  useKnowledge,
  useTranslationDocument,
  showModelTip,
  fileInputAry,
  isLaw,
  enableBoardOffice,
  isNet,
  isLogin,
  dragUploads,
  isDragOver,
  currentIndex,
  currentAgentType,
  messageContainerTran
} = useShared()

// 辅助函数：根据文件名获取文件图标
const getFileIcon = (fileName) => {
  if (!fileName) return ''
  const fileObj = { originalFileName: fileName }
  return getFileImgByOriginFile(fileObj)
}

// 辅助函数：检查是否为支持的文件类型
const isSupportedFileType = (fileName) => {
  if (!fileName) return false
  const supportedExtensions = ['txt', 'doc', 'docx', 'pdf', 'ppt', 'pptx', 'xls', 'xlsx']
  const extension = fileName.split('.').pop().toLowerCase()
  return supportedExtensions.includes(extension)
}

// 辅助函数：检查是否为查询相关页面类型
const isQueryPageType = () => {
  return ['query', 'it', 'law', 'board'].includes(pageType.value)
}

// 辅助函数：检查是否为需要显示选择器的页面类型
const isSelectorPageType = () => {
  return ['query', 'sample', 'it', 'law', 'board'].includes(pageType.value)
}

// 辅助函数：检查是否为需要显示文本输入框的页面类型
const isTextareaPageType = () => {
  return ['query', 'it', 'law', 'board'].includes(pageType.value)
}

// 辅助函数：检查是否为需要显示深度模式切换的页面类型
const isDeepModePageType = () => {
  return ['query', 'it', 'board'].includes(pageType.value)
}

// 辅助函数：检查是否为需要拖拽上传的页面类型
const isDragUploadPageType = () => {
  return ['sample', 'tran', 'final'].includes(pageType.value)
}

const showKnowledgeTip = ref(false)
const fileRef = ref(null)
const filePreRef = ref(null)
const knowledge = ref(null)
const wrapperRef = ref(null)
const tranViewRef = ref(null)
const finalViewRef = ref(null)
const commonUploadModals = ref(null)

watch(
  () => tranViewRef.value?.textareaInputRef?.value,
  el => {
    textareaInputTran.value = el ?? null
  },
  { immediate: true }
)

watch(
  () => finalViewRef.value?.textareaInputRef?.value,
  el => {
    textareaInputFinal.value = el ?? null
  },
  { immediate: true }
)

watch(
  () => tranViewRef.value?.wrapperRef?.value,
  el => {
    wrapperRef.value = el ?? null
  },
  { immediate: true }
)

watch(
  () => finalViewRef.value?.wrapperRef?.value,
  el => {
    wrapperRef.value = el ?? null
  },
  { immediate: true }
)

watch(
  () => tranViewRef.value?.messageContainerTran?.value,
  el => {
    messageContainerTran.value = el ?? null
  },
  { immediate: true }
)

watch(
  () => finalViewRef.value?.messageContainerTran?.value,
  el => {
    messageContainerTran.value = el ?? null
  },
  { immediate: true }
)

const arrList = ref([
  {
    index: 1,
    name: '我进入立讯技术后如何选择导师'
  },
  {
    index: 2,
    name: '员工延假与销假如何进行'
  },
  {
    index: 3,
    name: '公司实习生的待遇怎么样'
  },
  {
    index: 4,
    name: '亲属回避包括哪些等级'
  },
  {
    index: 5,
    name: '工人是否有宗教信仰的自由'
  }
])
const itList = ref([
  {
    index: 1,
    name: 'mes系统的基础数据怎么维护'
  },
  {
    index: 2,
    name: 'mes操作的常见异常处理'
  },
  {
    index: 3,
    name: '会议室建设标准'
  },
  {
    index: 4,
    name: '桌面云规划怎么做？'
  },
  {
    index: 5,
    name: '产线设备数据采集怎么做？'
  }
])
const historyList = ref([
  {
    index: 1,
    name: '工作中遇到棘手问题怎么办'
  },
  {
    index: 2,
    name: '国务院发布的2025年法定节假日安排'
  },
  {
    index: 3,
    name: '如何锻炼身体'
  },
  {
    index: 4,
    name: '如何缓解工作压力'
  },
  {
    index: 5,
    name: '平常该如何注意营养搭配'
  }
])
const hotSearchList = computed(() => {
  if (pageType.value === 'query') {
    return arrList.value
  }
  if (pageType.value === 'it') {
    return itList.value
  }
  return []
})
const lanList = ref(['中文', '英文', '西班牙语', '越南语'])
const dynamicRowFinal = ref(1)
const isDeepShow = ref(false)
const tranPost = event => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault() // 阻止默认的换行行为
    emit('submit-tran')
  }
}
const submitFileTran = obj => {
  emit('submit-tran', '', false, obj)
}
const submitFileFinal = obj => {
  emit('submit-final', '', false, obj)
}

const openKnowledge = () => {
  if (!isLogin.value) {
    ElMessage.warning('请先登录再使用')
    return false
  }
  knowledge.value.openFile('')
}
const showFileMenu = ref(false)

const showPreFile = val => {
  if (!isLogin.value) {
    ElMessage.warning('请先登录再使用')
    return false
  }
  filePreRef.value.openFile(val)
}
const showFile = val => {
  showFileMenu.value = !showFileMenu.value
}
const handleFileSelect = (val1, val2) => {
  showFileMenu.value = false
  if (!isLogin.value) {
    ElMessage.warning('请先登录再使用')
    return false
  }
  if (val1 === 'local') {
    fileObj.value = ''
    fileAry.value = ''
    fileRef.value.openFile(val2)
  } else {
    commonUploadModals.value.openFile(val2)
  }
}

const finalPost = event => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault() // 阻止默认的换行行为
    emit('submit-final')
  }
}

const samplePost = event => {
  emit('sample-post', event)
}
const upCommon = event => {
  emit('up-common', event)
}
const downCommon = event => {
  emit('down-common', event)
}
const refreshData = event => {
  emit('refresh-data', event)
}

const summitPost = event => {
  emit('summit-post', event)
}

const submitQuestionCurrent = val => {
  emit('submit-question', val)
}
const submitSampleTitle = val => {
  emit('submit-sample-title', val)
}
const submitTranSend = val => {
  emit('submit-tranSend', val)
}
const submitQuestionSend = () => {
  emit('submit-questionSend')
}
const submitItSend = () => {
  emit('submit-itSend')
}
const submitLawSend = () => {
  emit('submit-lawSend')
}

const submitSampleSend = () => {
  emit('submit-sampleSend')
}

const submitFinalSend = val => {
  emit('submit-finalSend', val)
}
const changeType = val => {
  if (isSampleLoad.value || finalIng.value) {
    ElMessage.warning('有问答正在进行中,请稍后再试')
    return
  }
  activeIndex.value = ''
  transQuest.value = ''
  transData.value = ''
  finalData.value.data = []
  finalData.value.title = ''
  finalQuest.value = ''
  pageType.value = val

  // 当切换到翻译或总结页面时，触发列表重新加载
  if (val === 'tran' || val === 'final') {
    emit('fetch-chat-list', '', val)
  }
}
const changeDynamicRows = () => {
  dynamicRowFinal.value = 1
}

const handleClickOutside = event => {
  if (wrapperRef.value && !wrapperRef.value.contains(event.target)) {
    showFileMenu.value = false
  }
}
const setDrag = val => {
  if (['query','it','law','board'].includes(pageType.value)) {
    return
  }
  isDragOver.value = val
}

// 组件挂载后初始化
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  nextTick(() => {
    isLaw.value = localStorage.getItem('enableLaw')
    enableBoardOffice.value = localStorage.getItem('enableBoardOffice')
    const isNetValue = localStorage.getItem('isNet')
    isNet.value = isNetValue === 'true'
  })
})

// 组件卸载时关闭 SSE 连接
onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
defineExpose({ changeDynamicRows, fileRef, setDrag })
</script>

<style lang="less" scoped>
.textarea {
  width: 862px;
}
.custom-input {
  :deep(.el-textarea__inner) {
    height: 150px !important;
    min-height: 150px !important;
    max-height: 150px !important;
    border-color: #1b6cff !important;
    scrollbar-width: thin;
    overflow-y: auto !important;
    scrollbar-color: #e5e7eb transparent;
  }
}
.topic-textarea {
  :deep(.el-textarea__inner) {
    height: 150px !important;
    min-height: 150px !important;
    max-height: 150px !important;
    border-radius: 16px !important;
    padding: 18px 135px 18px 15px !important;
    resize: none;
    overflow-y: hidden;
    border: 1px solid #dcdfe6 !important;
    box-shadow: none;
  }
  :deep(.el-textarea__inner:focus) {
    border-color: #409eff !important;
    box-shadow: none;
  }
}
.tran_select {
  width: 862px;
  margin: 0 auto 10px auto;
  display: flex;
  justify-content: flex-start;
}
.sampleArea {
  :deep(.el-textarea__inner) {
    padding: 18px 135px 18px 15px !important;
    height: 150px !important;
    min-height: 150px !important;
    max-height: 150px !important;
    overflow-y: hidden;
    border-color: #1b6cff !important;
  }
}
.tooltip-wrapper {
  position: relative;
  display: flex;
  .file-menu {
    position: absolute;
    bottom: 140%;
    left: 50%;
    transform: translateX(-50%);
    background-color: white;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    padding: 8px 0;
    margin-bottom: 12px;
    z-index: 2000;
    min-width: 140px;
  }

  .menu-item {
    padding: 8px 16px;
    cursor: pointer;
  }

  .menu-item:hover {
    background-color: #e6f4ff;
  }

  .fade-enter-active,
  .fade-leave-active {
    transition: opacity 0.2s;
  }
  .fade-enter-from,
  .fade-leave-to {
    opacity: 0;
  }
}

.tooltip {
  position: absolute;
  bottom: calc(100% + 5px);
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

/* 添加淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 可选：现代浏览器箭头实现 */
.tooltip::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 5px solid transparent;
  border-top-color: #000;
}

:deep(.el-radio-button) {
  margin-left: 10px;
  border: 1px solid #91caff;
  border-radius: 10px;
  overflow: hidden;
  
  &.is-active {
    border: none !important;
    outline: none !important;
  }
}

:deep(.el-radio-button__inner) {
  border: none !important;
  outline: none !important;
}
:deep(.el-radio-button:first-child .el-radio-button__inner) {
  border: none !important;
  outline: none !important;
}
:deep(.el-radio-button.is-active .el-radio-button__original-radio:not(:disabled)+.el-radio-button__inner) {
  color: #1677FF !important;
  background-color: #e6f4ff !important;
  border: none !important;
  outline: none !important;
  box-shadow: none !important;
}
.chat-type-group :deep(.el-radio-button__inner) {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 统一 AskKnowledgeDialog.vue 的链接视觉，用于 MarkdownRenderer 内链接 */
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
</style>


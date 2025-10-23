import { ref, reactive, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { checkKnowledgeFile } from '@/api/knowledgeBase/actions.js' // 引入 ElMessage
import { ContentType, KnowledgeSelect } from '@/utils/common.js'
const currentQuestion = ref('') // 控制页面展示问答页还是介绍页
const newQuestion = ref('') // 文本域输入值
const intelQuestion = ref('') // 文本域输入值

const isSampleStop = ref(false) // 控制通用模式 终止之后的状态
const limitFile = ref({})
const limitFinalFile = ref({})
const isQueryStop = ref(false) // 控制人资模式 终止之后的状态
const limitLoading = ref(false) // 切换左侧历史记录，判断当前选中的记录是否处于问答状态中
const limitTranLoading = ref(false)
const limitQueryLoading = ref(false)
const questions = ref([]) // 左侧历史记录标题数组
const intelList = ref([])
const agentChatList = ref([])
const answerList = ref([]) //历史记录数组
const selectType = ref(ContentType.CONVERSATION)
const isTranStop = ref(false)
const recordId = ref('')
const answerListIntel = ref([])
const currentIntelId = ref('')
const tempChatId = ref('')
const finalTitle = ref('')
const excelChatRepeat = ref({})
const currentIntel = ref({
  name: '',
  role: '',
  tone: '',
  description: '',
  agentId: ''
})
// 设置当前智能体选中的类型
const currentAgentType = ref('')
const isCreate = ref(false)
const contentType = ref(ContentType.CONVERSATION)
// 判断是否是智能体对话详情页
const isAgentDetail = ref(false)
const isNavigatingFromAgentList = ref(false)
const currentId = ref('') // 左侧历史记录当前选中的id
const pageType = ref('sample') // 当前页面类型
const selectedMode = ref('通用模式') // 模式
const currentObj = ref({
  // 人资模式数据对象
  list: {},
  messages: {},
  messageList: [],
  thinking: ''
})
const limitSample = ref({
  messages: []
})
const userInputContent = ref('')
const tipQuery = ref('') // 翻译、总结、人资模式 问答框标题（问题）
const streamingQuestion = ref('') // 保存流式输出过程中正在加载的问题
const activeIndex = ref('') // 左侧栏当前选中的索引
const activeIndexIntel = ref('')
const queryTypes = ref([]) //左侧栏 问题+类型 比如如何打卡（'query'） 拼接成的数组对象
const userInfo = ref({
  department: '',
  departmentId: '',
  id: '',
  name: '',
  url: ''
})
const messageContainer = ref(null)
const messageContainerIntel = ref(null)
const messageContainerTran = ref(null)
const chatQuery = reactive({
  //通用模式数据对象
  messages: []
})
const loadingId = ref('')
const currentIndex = ref('')
const intelQuery = reactive({
  //智能体数据对象
  messages: []
})
const limitIntelLoading = ref(false)
const isIntelLoad = ref(false) // 添加智能体加载状态到全局状态
const chatCurrent = reactive({
  //通用模式数据对象
  messages: []
})
const intelCurrent = reactive({
  //通用模式数据对象
  messages: []
})
const isLogin = ref(false) // 判断是否登录
const dynamicRows = ref(1) // 问答文本域的动态行数（高度）
const isSampleLoad = ref(false) // 判断是否正在问答状态
const isNet = ref(true)
const dragUploads = ref(null)
const finalData = ref({
  title: '',
  data: []
})
const handleShiftEnter = (val, event) => {
  if (event && (event.key !== 'Enter' || !event.shiftKey)) {
    return
  }

  const refMap = {
    textareaInputQuery,
    textareaInputIntel,
    textareaInputSample,
    textareaInputSampleCurrent,
    textareaInputTran,
    textareaInputFinal
  }

  const modelMap = {
    textareaInputIntel: intelQuestion,
    textareaInputQuery: newQuestion,
    textareaInputSample: newQuestion,
    textareaInputSampleCurrent: newQuestion,
    textareaInputTran: newQuestion,
    textareaInputFinal: newQuestion
  }

  const textareaRef = refMap[val] || textareaInputFinal
  const modelRef = modelMap[val] || newQuestion

  const textareaComponent = textareaRef.value
  const textareaElement =
    event?.target ?? textareaComponent?.textarea ?? textareaComponent?.$refs?.textarea ?? null

  if (!textareaElement) {
    return
  }

  const startPos = textareaElement.selectionStart ?? textareaElement.value.length
  const endPos = textareaElement.selectionEnd ?? textareaElement.value.length
  const currentValue = textareaElement.value ?? ''
  const updatedValue = `${currentValue.substring(0, startPos)}\n${currentValue.substring(endPos)}`

  textareaElement.value = updatedValue
  modelRef.value = updatedValue

  const newPos = startPos + 1

  nextTick(() => {
    if (typeof textareaElement.setSelectionRange === 'function') {
      textareaElement.setSelectionRange(newPos, newPos)
    }
  })
}
const textareaInputQuery = ref(null) // 获取 textarea 元素的引用
const textareaInputSample = ref(null) // 获取 textarea 元素的引用
const textareaInputSampleCurrent = ref(null) // 获取 textarea 元素的引用
const textareaInputTran = ref(null) // 获取 textarea 元素的引用
const textareaInputFinal = ref(null) // 获取 textarea 元素的引用
const textareaInputIntel = ref(null) // 获取 textarea 元素的引用

const isSendResumeMsgPage = ref(true)
const transFile = ref({})
const finalFile = ref({})
const finalIng = ref(false)
const finalQuest = ref('')
const selectedLan = ref('中文')
const transData = ref('')
const currentTransData = ref('')
const translationDocumentProcess = ref('')
const translationDocumentFinal = ref('')
const transQuest = ref('')
const dots = ref('.') // 初始点号
const limitId = ref('')
const limitTranId = ref('')
const limitFinalId = ref('')
const limitQueryId = ref('')
const topicPreSaveId = ref('') // 专题模式预保存的对话ID，避免切换对话时被覆盖
const fileObj = ref('')
const fileAry = ref([])
const drayAry = ref([])
const deepType = ref(false)
const docIng = ref(false)
const tranIng = ref(false)
const currentTip = ref(false)
const limitAry = ref([])
const showFileTip = ref(false)
const showModelTip = ref(false)
const isLaw = ref(false)
const enableBoardOffice = ref(false)
const knowSelect = ref(KnowledgeSelect.PERSONAL)
const fileInputAry = ref([])
const jobJdFile = ref(null)
const useTranslationDocument = ref(false) // 是否输出翻译文档
const resumeTaskState = reactive({
  isWaiting: false,
  isCompleted: false,
  batchId: '',
  ranking: [],
  rankingResult: null,
  jdFile: null,
  resumeFiles: [],
  text: '',
  executiveSummary: '',
  panelSummary: ''
})
const isDragOver = ref(false)
const isIntelStop = ref(false)
const conversationId = ref('') // 保存填入对话ID
const loadingIntelId = ref('') // 当前正在发生对话的智能体
const loadingIntelType = ref('') // 当前正在发生对话的智能体类型
const useKnowledge = ref(false) // 是否启用知识库问答
const chatList = ref([]) // 对话的数组列表
const isObject = variable => {
  const type = Object.prototype.toString.call(variable)
  return type === '[object PointerEvent]' || type === '[object KeyboardEvent]'
}
const checkData = (val, options = {}) => {
  const { preserveInput = false } = options
  if (!isLogin.value) {
    ElMessage({
      message: '请先登录再使用',
      type: 'warning'
    })

    return false
  }
  if (isObject(val) && !newQuestion.value) {
    ElMessage.warning('请输入您的问题再发送')
    return false
  }

  let candidateValue = newQuestion.value

  if (val && !isObject(val)) {
    if (preserveInput) {
      candidateValue = val
    } else {
      newQuestion.value = val
      candidateValue = newQuestion.value
    }
  }
  if (!candidateValue) {
    ElMessage.warning('请输入您的问题再发送')
    return false
  }
  return true
}

// 1. 创建模式映射表（建议放在文件顶部作为常量）
export const MODE_MAPPING = new Map([
  ['通用模式', 'sample'],
  ['人资行政专题', 'query'],
  ['IT专题', 'it'],
  ['法务专题', 'law'],
  ['董办专题', 'board'],
  ['翻译', 'tran'],
  ['总结', 'final']
]);

const changeMode = () => {
  currentQuestion.value = false
  activeIndex.value = ''
  newQuestion.value = ''
  dynamicRows.value = 1
  currentId.value = ''
  drayAry.value = []

// 2. 通过Map获取值（带默认值）
  pageType.value = MODE_MAPPING.get(selectedMode.value) ?? 'final';
  chatQuery.messages = []
}
const checkDeepType = () => {
  if (isSampleLoad.value || finalIng.value) {
    ElMessage.warning('有问题正在回答中，请稍后再切换')
    return
  }
  deepType.value = !deepType.value
}

const toggleKnowledge = async () => {
  if (isSampleLoad.value || finalIng.value) {
    ElMessage.warning('有问题正在回答中，请稍后再切换')
    return
  }
  if (!useKnowledge.value) {
    let hasKnowledgeFile = await checkKnowledgeFile(userInfo.value.id)
    if (hasKnowledgeFile.status) {
      if (!hasKnowledgeFile.data) {
        ElMessage.warning('个人知识库文件目录为空，请至少上传一个个人知识库文件！')
        return
      }
    }
  }
  useKnowledge.value = !useKnowledge.value
}

const toggleTranslationDocument = () => {
  if (finalIng.value) {
    ElMessage.warning('有问题正在回答中，请稍后再修改')
    return
  }
  useTranslationDocument.value = !useTranslationDocument.value
}

export function useShared() {
  const updateCurrentQuestion = newName => {
    currentQuestion.value = newName
  }

  const updateNewQuestion = newName => {
    newQuestion.value = newName
  }
  const updateIsSampleStop = newName => {
    isSampleStop.value = newName
  }
  const updateIsIntelStop = newName => {
    isIntelStop.value = newName
  }
  const updateConversationId = newName => {
    conversationId.value = newName
  }

  const updateLoadingIntelId = newName => {
    loadingIntelId.value = newName
  }

  const updateLoadingIntelType = newName => {
    loadingIntelType.value = newName
  }

  const updateChatList = newName => {
    chatList.value = newName
  }


  const updateUseKnowledge = newName => {
    useKnowledge.value = newName
  }

  const updateIsQueryStop = newName => {
    isQueryStop.value = newName
  }
  const updateLimitLoading = newName => {
    limitLoading.value = newName
  }
  const updateLimitTranLoading = newName => {
    limitTranLoading.value = newName
  }
  const updateLimitQueryLoading = newName => {
    limitQueryLoading.value = newName
  }

  const updateQuestions = newName => {
    questions.value = newName
  }
  const updateIntelList = newName => {
    intelList.value = newName
  }
  const updateAgentChatList = newName => {
    agentChatList.value = newName
  }
  const updateAnswerList = newName => {
    answerList.value = newName
  }
  const updateIsTranStop = newName => {
    isTranStop.value = newName
  }

  const updateAnswerListIntel = newName => {
    answerListIntel.value = newName
  }
  const updateCurrentId = newName => {
    currentId.value = newName
  }
  const updatePageType = newName => {
    pageType.value = newName
  }
  const updateSelectedMode = newName => {
    selectedMode.value = newName
  }
  const updateCurrentObj = newName => {
    currentObj.value = newName
  }
  const updateCurrentIntel = newName => {
    currentIntel.value = newName
  }
  const updateCurrentAgentType = newName => {
    currentAgentType.value = newName
  }
  const updateloadingId = newName => {
    loadingId.value = newName
  }

  const updateTipQuery = newName => {
    tipQuery.value = newName
  }
  const updateCurrentTip = newName => {
    currentTip.value = newName
  }

  const updateActiveIndex = newName => {
    activeIndex.value = newName
  }
  const updateActiveIndexIntel = newName => {
    activeIndexIntel.value = newName
  }
  const updateLimitFile = newName => {
    limitFile.value = newName
  }
  const updateLimitFinalFile = newName => {
    limitFinalFile.value = newName
  }
  const updateQueryTypes = newName => {
    queryTypes.value = newName
  }
  const updateChatQuery = newName => {
    chatQuery.messages = newName
  }
  const updateIntelQuery = newName => {
    intelQuery.messages = newName
  }

  const updateChatCurrent = newName => {
    chatCurrent.messages = newName
  }
  const updateIntelCurrent = newName => {
    intelCurrent.messages = newName
  }

  const updateIsLogin = newName => {
    isLogin.value = newName
  }
  const updateDynamicRows = newName => {
    dynamicRows.value = newName
  }
  const updateIsSampleLoad = newName => {
    isSampleLoad.value = newName
  }
  const updateLimitSample = newName => {
    limitSample.value = newName
  }

  const updateFinalDataTitle = newName => {
    finalData.value.title = newName
  }
  const updateFinalData = newName => {
    finalData.value.data = newName
  }
  const updateFinalIng = newName => {
    finalIng.value = newName
  }
  const updateFinalQuest = newName => {
    finalQuest.value = newName
  }
  const updateSelectedLan = newName => {
    selectedLan.value = newName
  }
  const updateTransData = newName => {
    transData.value = newName
  }
  const updateCurrentTransData = newName => {
    currentTransData.value = newName
  }
  const updateIsSendResumeMsgPage = newName => {
    isSendResumeMsgPage.value = newName
  }
  const updateTranslationDocumentProcess = newValue => {
    translationDocumentProcess.value = newValue
  }
  const updateTranslationDocumentFinal = newValue => {
    translationDocumentFinal.value = newValue
  }

  const updateTransQuest = newName => {
    transQuest.value = newName
  }
  const updateDots = newName => {
    dots.value = newName
  }
  const updateLimitId = newName => {
    limitId.value = newName
  }
  const updateLimitTranId = newName => {
    limitTranId.value = newName
  }
  const updateLimitFinalId = newName => {
    limitFinalId.value = newName
  }
  const updateTransFile = newName => {
    transFile.value = newName
  }
  const updateFinalFile = newName => {
    finalFile.value = newName
  }
  const updateLimitQueryId = newName => {
    limitQueryId.value = newName
  }

  const updateIsIntelLoad = newName => {
    isIntelLoad.value = newName
  }

  const updateMessageContainer = newName => {
    messageContainer.value = newName
  }
  const updateMessageContainerTran = newName => {
    messageContainerTran.value = newName
  }

  const updateMessageContainerIntel = newName => {
    messageContainerIntel.value = newName
  }

  const updateFileObj = newName => {
    fileObj.value = newName
  }
  const updateFileAry = newName => {
    fileAry.value = newName
  }

  const updateDeepType = newName => {
    deepType.value = newName
  }
  const updateDocIng = newName => {
    docIng.value = newName
  }
  const updateTranIng = newName => {
    tranIng.value = newName
  }

  const updateLimitAry = newName => {
    limitAry.value = newName
  }
  const updateShowFileTip = newName => {
    showFileTip.value = newName
  }
  const updateShowModelTip = newName => {
    showModelTip.value = newName
  }
  const updateFileInputAry = newName => {
    fileInputAry.value = newName
  }
  const updateJobJdFile = newName => {
    jobJdFile.value = newName
  }
  const resetResumeTaskState = () => {
    resumeTaskState.isWaiting = false
    resumeTaskState.isCompleted = false
    resumeTaskState.batchId = ''
    resumeTaskState.ranking = []
    resumeTaskState.rankingResult = null
    resumeTaskState.jdFile = null
    resumeTaskState.resumeFiles = []
    resumeTaskState.text = ''
    resumeTaskState.executiveSummary = ''
    resumeTaskState.panelSummary = ''
  }
  const updateResumeTaskState = patch => {
    if (!patch) {
      resetResumeTaskState()
      return
    }

    if (Object.prototype.hasOwnProperty.call(patch, 'isWaiting')) {
      resumeTaskState.isWaiting = !!patch.isWaiting
    }
    if (Object.prototype.hasOwnProperty.call(patch, 'isCompleted')) {
      resumeTaskState.isCompleted = !!patch.isCompleted
    }
    if (Object.prototype.hasOwnProperty.call(patch, 'batchId')) {
      resumeTaskState.batchId = patch.batchId || ''
    }
    if (Object.prototype.hasOwnProperty.call(patch, 'ranking')) {
      resumeTaskState.ranking = Array.isArray(patch.ranking) ? [...patch.ranking] : []
    }
    if (Object.prototype.hasOwnProperty.call(patch, 'rankingResult')) {
      resumeTaskState.rankingResult = patch.rankingResult || null
    }
    if (Object.prototype.hasOwnProperty.call(patch, 'jdFile')) {
      resumeTaskState.jdFile = patch.jdFile || null
    }
    if (Object.prototype.hasOwnProperty.call(patch, 'resumeFiles')) {
      resumeTaskState.resumeFiles = Array.isArray(patch.resumeFiles)
        ? [...patch.resumeFiles]
        : []
    }
    if (Object.prototype.hasOwnProperty.call(patch, 'text')) {
      resumeTaskState.text = typeof patch.text === 'string' ? patch.text : ''
    }
    if (Object.prototype.hasOwnProperty.call(patch, 'executiveSummary')) {
      resumeTaskState.executiveSummary =
        typeof patch.executiveSummary === 'string' ? patch.executiveSummary : ''
    }
    if (Object.prototype.hasOwnProperty.call(patch, 'panelSummary')) {
      resumeTaskState.panelSummary =
        typeof patch.panelSummary === 'string' ? patch.panelSummary : ''
    }
  }
  const updateIsLaw = newName => {
    isLaw.value = newName
  }
  const updateEnableBoardOffice = newName => {
    enableBoardOffice.value = newName
  }
  const updateContentType = newName => {
    contentType.value = newName
  }
  const updateIsAgentDetail= newName => {
    isAgentDetail.value = newName
  }
  const updateIsNavigatingFromAgentList = newValue => {
    isNavigatingFromAgentList.value = !!newValue
  }
  const updateUserInputContent = newName => {
    userInputContent.value = newName
  }
  const updateKnowSelect = newName => {
    knowSelect.value = newName
  }
  const updateDragUploads = newName => {
    dragUploads.value = newName
  }
  const updateIsDragOver = newName => {
    isDragOver.value = newName
  }
  const updateIsCreate = newName => {
    isCreate.value = newName
  }
  const updateIsNet = newName => {
    isNet.value = newName
  }
  const updateIntelQuestion = newName => {
    intelQuestion.value = newName
  }
  const updateSelectType = newName => {
    selectType.value = newName
  }
  const updateLimitIntelLoading = newName => {
    limitIntelLoading.value = newName
  }
  const updateCurrentIntelId = newName => {
    currentIntelId.value = newName
  }
  const updateTempChatId = newName => {
    tempChatId.value = newName
  }
  const updateFinalTitle = newName => {
    finalTitle.value = newName
  }
  const updateExcelChatRepeat = newName => {
    excelChatRepeat.value = newName
  }
  const updateRecordId = newName => {
    recordId.value = newName
  }
  const updateCurrentIndex = newName => {
    currentIndex.value = newName
  }
  const updateDrayAry = newName => {
    drayAry.value = newName
  }
  watch(
    newQuestion,
    newVal => {
      if (!newVal.trim()) {
        newQuestion.value = ''
      }
    },
    { deep: true }
  )
  watch(
    intelQuestion,
    newVal => {
      if (!newVal.trim()) {
        intelQuestion.value = ''
      }
    },
    { deep: true }
  )
  return {
    currentQuestion,
    currentIndex,
    messageContainerTran,
    newQuestion,
    selectType,
    recordId,
    isNet,
    isTranStop,
    isCreate,
    currentIntelId,
    currentAgentType,
    tempChatId,
    finalTitle,
    excelChatRepeat,
    isSampleStop,
    isIntelStop,
    conversationId,
    loadingIntelId,
    loadingIntelType,
    chatList,
    useKnowledge,
    useTranslationDocument,
    isQueryStop,
    drayAry,
    isDragOver,
    limitLoading,
    limitFile,
    limitIntelLoading,
    isIntelLoad,
    intelQuestion,
    knowSelect,
    dragUploads,
    limitId,
    limitTranId,
    limitFinalId,
    transFile,
    finalFile,
    limitQueryId,
    topicPreSaveId,
    questions,
    loadingId,
    intelList,
    agentChatList,
    answerList,
    answerListIntel,
    intelCurrent,
    currentId,
    pageType,
    selectedMode,
    currentObj,
    currentIntel,
    tipQuery,
    streamingQuestion,
    currentTip,
    userInfo,
    activeIndex,
    activeIndexIntel,
    limitFinalFile,
    queryTypes,
    chatQuery,
    intelQuery,
    limitTranLoading,
    limitQueryLoading,
    isLogin,
    fileObj,
    fileAry,
    dynamicRows,
    isSampleLoad,
    limitSample,
    finalData,
    finalIng,
    finalQuest,
    selectedLan,
    transData,
    currentTransData,
    isSendResumeMsgPage,
    translationDocumentProcess,
    translationDocumentFinal,
    textareaInputQuery,
    textareaInputSample,
    textareaInputSampleCurrent,
    textareaInputTran,
    textareaInputFinal,
    textareaInputIntel,
    transQuest,
    dots,
    chatCurrent,
    messageContainer,
    messageContainerIntel,
    deepType,
    docIng,
    tranIng,
    limitAry,
    showFileTip,
    showModelTip,
    fileInputAry,
    jobJdFile,
    resumeTaskState,
    isLaw,
    enableBoardOffice,
    contentType,
    isAgentDetail,
    isNavigatingFromAgentList,
    userInputContent,
    changeMode,
    updateCurrentIntel,
    updateCurrentAgentType,
    updateIsIntelStop,
    updateConversationId,
    updateLoadingIntelId,
    updateLoadingIntelType,
    updateChatList,
    updateUseKnowledge,
    updateCurrentQuestion,
    updateLimitFinalFile,
    updateLimitTranId,
    updateLimitFinalId,
    updateTransFile,
    updateFinalFile,
    updateDrayAry,
    updateNewQuestion,
    updateIsSampleStop,
    updateIsQueryStop,
    updateLimitLoading,
    updateQuestions,
    updateAnswerList,
    updateLimitFile,
    updateAnswerListIntel,
    updateCurrentId,
    updatePageType,
    updateSelectedMode,
    updateCurrentIndex,
    updateCurrentObj,
    updateTipQuery,
    updateCurrentTransData,
    updateIsSendResumeMsgPage,
    updateActiveIndex,
    updateloadingId,
    updateIsTranStop,
    updateQueryTypes,
    updateChatQuery,
    updateIsLogin,
    updateDynamicRows,
    updateIsSampleLoad,
    updateFinalData,
    updateLimitSample,
    handleShiftEnter,
    updateFinalIng,
    updateFinalQuest,
    updateSelectedLan,
    updateTransData,
    updateTransQuest,
    updateDots,
    updateChatCurrent,
    updateTranslationDocumentProcess,
    updateTranslationDocumentFinal,
    updateLimitId,
    checkData,
    updateMessageContainer,
    updateMessageContainerTran,
    updateMessageContainerIntel,
    updateFileObj,
    updateDeepType,
    checkDeepType,
    toggleKnowledge,
    toggleTranslationDocument,
    updateDocIng,
    updateTranIng,
    updateLimitAry,
    updateShowFileTip,
    updateShowModelTip,
    updateFileAry,
    updateFileInputAry,
    updateJobJdFile,
    updateResumeTaskState,
    updateIsLaw,
    updateEnableBoardOffice,
    updateKnowSelect,
    updateDragUploads,
    updateIsDragOver,
    updateIntelList,
    updateAgentChatList,
    updateActiveIndexIntel,
    updateIsCreate,
    updateContentType,
    updateIsAgentDetail,
    updateIsNavigatingFromAgentList,
    updateUserInputContent,
    updateIsNet,
    updateIntelQuestion,
    updateCurrentTip,
    updateIntelQuery,
    updateIntelCurrent,
    updateSelectType,
    updateLimitIntelLoading,
    updateCurrentIntelId,
    updateTempChatId,
    updateFinalTitle,
    updateExcelChatRepeat,
    updateFinalDataTitle,
    updateRecordId,
    updateLimitTranLoading,
    updateLimitQueryLoading,
    updateLimitQueryId,
    updateIsIntelLoad
  }
}

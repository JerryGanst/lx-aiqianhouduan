import {
  getAgentChatByChatId,
  getExcelChatById,
  getImgRecognitionById,
  getResumeTaskById,
  saveAgentChat,
  saveExcelChat,
  saveImgRecognition,
  saveLastInfo,
  saveResumeTask
} from '@/api/agent/actions.js'
import { ElMessage } from 'element-plus'
import { useShared } from '@/utils/useShared'
import { COMPARE_AGENT_TYPE, DEFAULT_AGENT_TYPE, RESUME_AGENT_TYPE, TABLE_AGENT_TYPE } from '@/utils/constants.js'

const {finalTitle, excelChatRepeat} = useShared()

const compareHandler = async (chatId, message) => {
  if (chatId) {
    let chatResults
    chatResults = await getImgRecognitionById(chatId)
    if (!chatResults.status) {
      ElMessage.error(chatResults.message)
      return
    }
    finalTitle.value = chatResults.data?.title ?? finalTitle.value
  }

  // 深拷贝 防止修改intelQuery
  let msgForSave = JSON.parse(JSON.stringify(message))
  msgForSave.forEach(item => {
    if (item.role === 'user') {
      let chatMsg = {}
      let content = []
      chatMsg.type = 'text'
      chatMsg.text = item.content
      if (item.files && item.files.length > 0) {
        for (let i = 0; i < item.files.length; i++) {
          let urlMsg = {}
          urlMsg.type = 'image_url'
          urlMsg.image_url = { image: item.files[i] }
          content.push(urlMsg)
        }
      }
      content.push(chatMsg)
      item.content = content
      delete item.files
    } else if (item.role === 'assistant') {
      item.content = [{
        "type": "text",
        "text": item.content
      }]
    }
  })
  const userInfo = JSON.parse(localStorage.getItem('userInfo'))
  return await saveImgRecognition({
    userId: userInfo.id,
    imgMessages: msgForSave,
    title: finalTitle.value,
    id: chatId
  })
}

const defaultHandler = async (chatId, message, agentId) => {
  if (chatId) {
    let chatResults
    chatResults = await getAgentChatByChatId(chatId)
    if (!chatResults.status) {
      ElMessage.error(chatResults.message)
      return
    }
    finalTitle.value = chatResults.data?.title ?? finalTitle.value
  }
  const userInfo = JSON.parse(localStorage.getItem('userInfo'))
  return await saveAgentChat({
    userId: userInfo.id,
    id: chatId,
    agentId: agentId,
    messages: message,
    title: finalTitle.value
  })
}

const resumeHandler = async (chatId, message, agentId, options = {}) => {
  if (chatId) {
    try {
      const chatResults = await getResumeTaskById(chatId)
      if (chatResults?.status) {
        finalTitle.value = chatResults.data?.title ?? finalTitle.value
      }
    } catch (error) {
      console.error(error)
    }
  }

  const userInfo = JSON.parse(localStorage.getItem('userInfo'))
  const normalizedMessages = JSON.parse(JSON.stringify(message || []))
  const { resumeRepeat, resumeTaskRepeat, complete } = options || {}
  const payload = {
    id: chatId,
    userId: userInfo.id,
    title: finalTitle.value,
    resumeTaskRepeat: resumeTaskRepeat || {
      agentId,
      messages: normalizedMessages
    }
  }

  if (resumeRepeat !== undefined) {
    payload.resumeRepeat = resumeRepeat
  }

  if (typeof complete === 'boolean') {
    payload.complete = complete
  }

  return await saveResumeTask(payload)
}

const tableHandler = async (chatId, message, agentId) => {
  if (chatId) {
    let chatResults
    chatResults = await getExcelChatById(chatId)
    if (!chatResults.status) {
      ElMessage.error(chatResults.message)
      return
    }
    finalTitle.value = chatResults.data?.title ?? finalTitle.value
  }
  const userInfo = JSON.parse(localStorage.getItem('userInfo'))
  let excelSaveResult = await saveExcelChat({
    userId: userInfo.id,
    id: chatId,
    agentId: agentId,
    messages: message,
    title: finalTitle.value
  })
  if (excelSaveResult.status) {
    let saveLastParams = {
      chatId: excelSaveResult.data.id,
      excelChatRepeat: excelChatRepeat.value
    }
    await saveLastInfo(saveLastParams)
  }
  return excelSaveResult
}

export const loadingIntelTypeHandlerMap = new Map([
  [COMPARE_AGENT_TYPE, compareHandler],
  [DEFAULT_AGENT_TYPE, defaultHandler],
  [RESUME_AGENT_TYPE, resumeHandler],
  [TABLE_AGENT_TYPE, tableHandler]
]);

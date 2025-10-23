/**
 * 抽取接口调用方法
 */
import request from '@/utils/request.js'

const API = Object.freeze({
  // 获取当前用户下的智能体
  GET_AGENT_LIST_BY_USER_ID: '/Agent/findAgentByUserId?userId=',
  // 保存智能体
  SAVE_AGENT: '/Agent/saveAgent',
  // 获取智能体描述内容
  GET_AGENT_CONTENT: '/Agent/generateAgentDescription',
  // 删除智能体
  REMOVE_AGENT: '/Agent/deleteAgentById?agentId=',
  // 根据图片文件的对象名获得预览链接
  GET_AGENT_IMAGE_BY_OBJ: '/Agent/getPicUrl?objectName=',
  // 编辑智能体（根据智能体ID查询智能体详情）
  GET_AGENT_BY_AGENT_ID: '/Agent/findAgentById?agentId=',
  // 根据智能体ID获取智能体聊天记录
  GET_AGENT_CHAT_BY_AGENT_ID: '/Agent/findAgentChatByAgentId?agentId=',
  // 根据用户ID获取简历任务列表
  GET_RESUME_TASK_BY_USER_ID: '/Resume/getResumeTaskByUserId?userId=',
  // 保存智能体对话
  SAVE_AGENT_CHAT: '/Agent/saveAgentChat',
  // 保存简历任务
  SAVE_RESUME_TASK: '/Resume/saveResumeTask',
  // 发起简历分析
  CREATE_RESUME_ANALYSIS: '/AI/resumes',
  // 查询简历任务进度
  RESUME_TASK_CALLBACK: '/Resume/resumeTaskCallback',
  // 根据id删除聊天记录
  REMOVE_AGENT_CHAT_BY_ID: '/Agent/deleteAgentChatById?agentChatId=',
  // 批量删除智能体聊天记录
  BATCH_REMOVE_AGENT_CHAT_BY_IDS: '/Agent/deleteAgentChatByIds',
  // 根据聊天记录ID获取聊天记录
  GET_AGENT_CHAT_BY_CHAT_ID: '/Agent/findAgentChatByChatId?chatId=',
  // 根据简历任务ID获取任务详情
  GET_RESUME_TASK_BY_ID: '/Resume/getResumeTaskById?id=',
  // 修改简历任务标题
  CHANGE_RESUME_TASK_TITLE_BY_ID: '/Resume/changeResumeTaskTitleById?id=',
  // 删除简历任务
  DELETE_RESUME_TASK_BY_ID: '/Resume/deleteResumeTaskById?id=',
  // 修改智能体标题
  CHANGE_AGENT_CHAT_TITLE: '/Agent/updateAgentChatTitle?agentChatId=',
  // 修改图片对比智能体标题
  CHANGE_IMG_RECOGNITION_CHAT_TITLE: '/imageRecognition/changeImageRecognitionTitle?id=',
  // 根据用户id获取图片识别记录
  GET_IMG_RECOGNITIONS_BY_USER_ID: '/imageRecognition/getImageRecognitionsByUserId?userId=',
  // 保存用户图片识别记录
  SAVE_IMG_RECOGNITION: '/imageRecognition/saveImgRecognition',
  // 根据对话ID获取图片识别记录
  GET_IMG_RECOGNITION_BY_ID: '/imageRecognition/getImgRecognitionById?id=',
  // 根据对话ID删除图片识别记录
  DELETE_IMG_RECOGNITION_BY_ID: '/imageRecognition/deleteImgRecognitionById?id=',
  // 创建并保存新的 Excel 会话
  SAVE_EXCEL_CHAT: '/excelChat/saveExcelChat',
  // 根据主键 ID 获取会话详情
  GET_EXCEL_CHAT_BY_ID: '/excelChat/getExcelChatById?id=',
  // 根据用户 ID 获取会话列表
  GET_EXCEL_CHAT_BY_USER_ID: '/excelChat/getExcelChatByUserId?userId=',
  // 根据主键 ID 删除会话
  REMOVE_EXCEL_CHAT_BY_ID: '/excelChat/deleteExcelChatById?id=',
  // 保存会话最后一次补全记录
  SAVE_LAST_INFO: '/excelChat/saveLastInfo',
  // 根据主键 ID 更新会话标题
  UPDATE_EXCEL_CHAT_TITLE_BY_ID: '/excelChat/updateExcelChatTitleById?id=',
  // 获取对话ID
  GET_NEW_CHAT_ID: '/getRequestId'
});

export const getAgentListByUserId = (userId) => request.post(API.GET_AGENT_LIST_BY_USER_ID + userId, {})

export const saveAgent = (agentItem) => request.post(API.SAVE_AGENT, agentItem)

export const getAgentContent = (agentItem) => request.post(API.GET_AGENT_CONTENT, agentItem)

export const removeAgentById = (agentId) => request.post(API.REMOVE_AGENT + agentId, {})

export const getAgentImgByObj = (objectName) => request.post(API.GET_AGENT_IMAGE_BY_OBJ + objectName, {})

export const getAgentDetailById = (agentId) => request.post(API.GET_AGENT_BY_AGENT_ID + agentId, {})

export const getAgentChatByAgentId = (agentId, keyword) => request.post(API.GET_AGENT_CHAT_BY_AGENT_ID + agentId + '&keyword=' + (keyword || ''), {})

export const getResumeTaskByUserId = (userId, keyword) => {
  const search = keyword ? encodeURIComponent(keyword) : ''
  return request.post(`${API.GET_RESUME_TASK_BY_USER_ID}${userId}&keyword=${search}`, {})
}

export const saveAgentChat = (agentChats) => request.post(API.SAVE_AGENT_CHAT, agentChats)

export const saveResumeTask = resumeTask => request.post(API.SAVE_RESUME_TASK, resumeTask)

export const createResumeAnalysis = payload => request.post(API.CREATE_RESUME_ANALYSIS, payload)

export const getResumeTaskCallback = payload => request.post(API.RESUME_TASK_CALLBACK, payload)

export const removeAgentChatById = (chatId, userId) => request.post(API.REMOVE_AGENT_CHAT_BY_ID + chatId + '&userId=' + (userId || ''), {  })

export const batchRemoveAgentChatList = (chatIds) => request.post(API.BATCH_REMOVE_AGENT_CHAT_BY_IDS, chatIds)

export const getAgentChatByChatId = (chatId) => request.post(API.GET_AGENT_CHAT_BY_CHAT_ID + chatId , {  })

export const getResumeTaskById = (taskId) => request.post(API.GET_RESUME_TASK_BY_ID + taskId, {})

export const changeResumeTaskTitleById = (taskId, title) => request.post(
  API.CHANGE_RESUME_TASK_TITLE_BY_ID + taskId + '&title=' + (title || ''),
  {}
)

export const changeAgentChatTitle = (agentChatId, title) => request.post(API.CHANGE_AGENT_CHAT_TITLE + agentChatId + '&title=' + (title || ''), {  })

export const changeImgRecognitionChatTitle = (agentChatId, title) => request.post(API.CHANGE_IMG_RECOGNITION_CHAT_TITLE + agentChatId + '&title=' + (title || ''), {  })

export const deleteResumeTaskById = (taskId) => request.post(API.DELETE_RESUME_TASK_BY_ID + taskId, {})

export const getImageRecognitionsByUserId = (userId, keyword) => request.post(API.GET_IMG_RECOGNITIONS_BY_USER_ID + userId + '&keyword=' + (keyword || ''), {  })

export const saveImgRecognition = (agentChats) => request.post(API.SAVE_IMG_RECOGNITION, agentChats)

export const getImgRecognitionById = (chatId) => request.post(API.GET_IMG_RECOGNITION_BY_ID + chatId, {  })

export const deleteImgRecognitionById = (chatId) => request.post(API.DELETE_IMG_RECOGNITION_BY_ID + chatId)

export const saveExcelChat = (excelChats) => request.post(API.SAVE_EXCEL_CHAT , excelChats)

export const getExcelChatById = (chatId) => request.post(API.GET_EXCEL_CHAT_BY_ID + chatId, {})

export const getExcelChatByUserId = (userId, keyword) => request.post(API.GET_EXCEL_CHAT_BY_USER_ID  + userId + '&keyword=' + (keyword || ''), {})

export const removeExcelChatById = (chatId) => request.post(API.REMOVE_EXCEL_CHAT_BY_ID + chatId)

export const saveLastInfo = (lastInfo) => request.post(API.SAVE_LAST_INFO, lastInfo)

export const updateExcelChatTitle = (chatId, title) => request.post(API.UPDATE_EXCEL_CHAT_TITLE_BY_ID + chatId + '&title=' + (title || ''), {  })

export const getNewChatId = () => request.get(API.GET_NEW_CHAT_ID, {  })


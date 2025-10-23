/**
 * 抽取接口调用方法
 */
import request from '@/utils/request.js'

const API = Object.freeze({
  // 根据通用对话ID获取通用对话详情
  GET_MESSAGE_BY_CHAT_ID: '/Message/getMessageById?id=',
  // 通用对话列表接口
  GET_CHAT_MSG_LIST: '/Message/getMessageByUserId?userId=',
  // 通用流式接口
  SAMPLE_STREAM: '/AI/unifiedChat',
  // 批量删除对话
  BATCH_REMOVE_CHAT_LIST: '/Message/deleteMessageByIds',
});

export const getChatDetailByChatId = (chatId) => request.post(API.GET_MESSAGE_BY_CHAT_ID + chatId, {})

export const getChatMsgList = (userId, keyword, type) => request.post(API.GET_CHAT_MSG_LIST + userId + '&keyword=' + (keyword || '') + (type ? '&type=' + type : ''), {})

export const sampleStream = (streamInfo) => request.post(API.SAMPLE_STREAM, streamInfo)

export const batchRemoveChatList = (chatIdList) => request.post(API.BATCH_REMOVE_CHAT_LIST, chatIdList)

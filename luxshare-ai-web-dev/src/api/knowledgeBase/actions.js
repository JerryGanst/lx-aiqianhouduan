/**
 * 抽取接口调用方法
 */
import request from '@/utils/request.js'

const API = Object.freeze({
  // 根据个人知识库文件名获得预览链接
  GET_KNOWLEDGE_FILE_URL: '/Files/getKnowledgeFileUrl?objectName=',
  // 检查用户是否上传过知识库文件
  CHECK_KNOWLEDGE_FILE: '/UserInfo/checkKnowledgeFile?userId=',
  // 根据用户ID查询处级干部部门信息
  GET_DEPARTMENT_INFO_BY_USER_ID: '/UserInfo/getDepartmentInfoByUserId',
  // 获取文件夹列表
  GET_FOLDER_LIST: '/FileFolder/getFolderList',
  // 创建文件夹
  CREATE_FOLDER: '/FileFolder/createFolder',
  // 删除文件夹
  DELETE_FOLDER: '/FileFolder/deleteFolder',
  // 私有文件分享至部门
  SHARE_PRIVATE_FILE_TO_DEPARTMENT: '/Files/sharePrivateFileToDepartment',
  // 创建或保存目录标签
  SAVE_TARGET_FOLDER: '/TargetFolder/saveTargetFolder',
  // 获取用户的标签列表
  GET_TARGET_FOLDER_BY_USER_ID: '/TargetFolder/getTargetFolderByUserId',
});

export const getKnowledgeFileUrl = (fileName) => request.post(API.GET_KNOWLEDGE_FILE_URL + fileName, {})

export const checkKnowledgeFile = (userId) => request.get(API.CHECK_KNOWLEDGE_FILE + userId, {})

export const getFolderList = (id, isDepartment) => {
  const queryId = encodeURIComponent(id)
  const departmentFlag = isDepartment ? 'true' : 'false'
  return request.get(`${API.GET_FOLDER_LIST}?id=${queryId}&isDepartment=${departmentFlag}`, {})
}

export const createFolder = (data) => request.post(API.CREATE_FOLDER, data)

export const deleteFolder = (folderId, userId) => request.post(API.DELETE_FOLDER + '?folderId=' + folderId + '&userId=' + userId, {})

export const getDepartmentInfoByUserId = (userId) =>
  request.post(`${API.GET_DEPARTMENT_INFO_BY_USER_ID}?userId=${userId}`, {})

export const sharePrivateFileToDepartment = (fileId, userId, folderId) =>
  request.post(
    `${API.SHARE_PRIVATE_FILE_TO_DEPARTMENT}?fileId=${fileId}&userId=${userId}&folderId=${folderId}`,
    {}
  )

export const saveTargetFolder = (data) => request.post(API.SAVE_TARGET_FOLDER, data)

export const getTargetFolderByUserId = (userId) =>
  request.post(`${API.GET_TARGET_FOLDER_BY_USER_ID}?userId=${userId}`, {})

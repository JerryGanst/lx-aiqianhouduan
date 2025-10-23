<template>
  <div
    class="upload-layout"
    ref="uploadLayoutRef"
    @dragover.prevent="handleDragOver"
    @dragleave="handleDragLeave"
    @drop.prevent="handleDrop"
    style="position: relative"
    :class="{ 'drag-over': isDragOver }"
  >
    <!-- 左侧附件列表 -->
    <div class="file-list" :style="{ width: isPre ? '700px' : '100%' }" ref="leftPanel">
      <div class="file_search">
        <div class="file_left">
          <div class="file_content">
            <el-upload
              drag
              :auto-upload="false"
              :multiple="true"
              :accept="allowedFileTypes"
              :on-change="handleFileAdd"
              :show-file-list="false"
              :file-list="fileQueue"
              :before-upload="checkFileSize"
            >
              <div class="file_upload">
                <img src="@/assets/knowledgeBase/upload_file.png" style="width: 24px; height: 24px;">
                <span>上传文件</span>
              </div>
            </el-upload>
<!--            <div class="create_folder" @click="openCreateSubFolderDialog">新建文件夹</div>-->

            <div class="active">
              <div
                class="active_item"
                :style="{
                  background: activeIndex === 0 ? '#E6F4FF' : '',
                  color: activeIndex === 0 ? '#1B6CFF' : '#9D9D9D'
                }"
                @click="changeType(0)"
              >
                上传时间
                <img :src="activeIndex === 0 ? (timeSort ? down : up) : sort" />
              </div>
              <div
                class="active_item"
                :style="{
                  background: activeIndex === 1 ? '#E6F4FF' : '',
                  color: activeIndex === 1 ? '#1B6CFF' : '#9D9D9D'
                }"
                @click="changeType(1)"
              >
                文件名称
                <img :src="activeIndex === 1 ? (nameSort ? down : up) : sort" />
              </div>

              <div
                class="active_item"
                :style="{
                  background: activeIndex === 2 ? '#E6F4FF' : '',
                  color: activeIndex === 2 ? '#1B6CFF' : '#9D9D9D'
                }"
                @click="changeType(2)"
              >
                文件大小
                <img :src="activeIndex === 2 ? (sizeSort ? down : up) : sort" />
              </div>
            </div>
          </div>
        </div>
        <div class="file_right">
          <div class="file_content">
            <div class="file_info">
              <span style="font-size: 16px; color:#868686;">共{{ total }}项</span>
              <span style="padding-left: 10px;font-size: 16px; color:#868686;">存储空间 : 已使用{{ totalSize }}{{ point }}</span>
            </div>
            <div class="search-input-wrapper" ref="searchInputWrapper">
              <el-input
                v-model="searchText"
                placeholder="请输入关键词搜索"
                clearable
                @focus="handleSearchFocus"
                @clear="clearData"
                @keydown.enter.prevent="searchData"
              >
                <!-- 使用插槽自定义前缀图标并绑定事件 -->
                <template #suffix>
                  <el-icon class="search-icon" @click="searchData" style="cursor: pointer"><Search /></el-icon>
                </template>
              </el-input>
              <div
                v-if="isTagPanelVisible"
                class="tag-filter-panel-wrapper"
              >
                <TagFilterPanel
                  :tag-list="tagList"
                  :selected-tag-ids="selectedTagIds"
                  @update:selected-tag-ids="handleTagSelectionUpdate"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="file_item" ref="fileListContent">
        <div
          v-for="(file, index) in fileQueue"
          :key="file.uid"
          class="file-item"
          :class="{ 'uploading-file': file.status === 'pending' }"
          @click="getFile(file)"
          @contextmenu.prevent="showOptionsMenu(file, index, $event)"
          @mouseenter="handleFileMouseEnter(file)"
          @mouseleave="handleFileMouseLeave"
          style="position: relative"
          :style="{ width: '180px' }"
        >
                     <div class="file_img" :title="file.originalFileName">
             <img
               v-if="file.fileType"
               :src="getFileImgByOriginFile(file)"
             />
           </div>
          <div class="originalFileName" :style="{ width: isPre ? 'calc(100% - 16px)' : 'calc(100% - 16px)' }" :title="file.originalFileName">{{ file.originalFileName }}</div>
          <div style="font-size: 12px; color: #bebebe; margin-top: 2px">
            {{ file.fileSize ? (file.fileSize / 1024).toFixed(1) : 0 }}KB
          </div>
          <div v-if="hoveredFileId === file.uid && tagInFile.length" class="file-tags">
            <div v-for="tag in displayedTags" :key="tag.tagId" class="file-tag-item">
              <img src="@/assets/knowledgeBase/white_tag.svg" alt="tag icon" />
              <span>{{ tag.targetName }}</span>
            </div>
            <div v-if="hasExtraTags" class="file-tag-count">{{ totalTagCount }}</div>
          </div>
          <span @click.stop="openDeleteConfirm(index, $event)">
            <div style="width: 20px; height: 20px; cursor: pointer; position: absolute; right: 4px; top: 4px">
              <img src="@/assets/deleteFile.svg" style="width: 100%; height: 100%" />
            </div>
          </span>
        </div>
      </div>
      <FileContextMenu
        v-if="optionsContextMenu.visible"
        ref="optionsMenuRef"
        :context-menu="optionsContextMenu"
        :hasTitle="false"
        :menu-level="1"
        menuType="OPTIONS"
        :show-share-option="props.libraryType === 'personal'"
        @menu-click="handleOptionsMenuClick"
      />
      <FileContextMenu
        v-if="props.libraryType === 'personal' && contextMenu.visible"
        ref="contextMenuRef"
        :context-menu="contextMenu"
        :hasTitle="true"
        :menu-level="1"
        menuType="DEPARTMENT"
        @menu-hover="getDepartmentDirs"
      />
      <FileContextMenu
        v-if="props.libraryType === 'personal' && directoryContextMenu.visible"
        :context-menu="directoryContextMenu"
        :hasTitle="false"
        :menu-level="2"
        menuType="DIRECTORY"
        :parent-id="directoryContextMenu.parentId"
        @menu-click="handleDirectoryClick"
      />
      <EditTagModal
        v-model:visible="isEditTagModalVisible"
        :file-id="editingTagFileId"
        :is-department="props.libraryType !== 'personal'"
        :selected-tag-ids="editingTagTargetIds"
        @close="handleEditTagClose"
        @confirm="handleEditTagConfirm"
      />
      <div style="width: 100%; height: 50px; display: flex; justify-content: center; align-items: center">
        <!-- 分页器 -->
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[30, 50, 100]"
          :total="totals"
          layout="total, prev, pager, next, sizes"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
      <AskKnowledgeDialog
        ref="askKnowledgeDialogRef"
        v-if="isShowChatWithDirectory"
        :folder-id="props.folderId"
        :folder-name="props.folderName"
        @close="closeChatWithDirectoryDialog"
      />
    </div>

    <!-- 右侧上传区域 -->
    <div v-if="true" class="preview-file" :style="{ marginRight: drawer ? DRAWER_WIDTH + 'px' : '0' }">
      <UploadPreviewArea
        :is-pre="isPre"
        :overlay-width="overlayWidth"
        :preview-file-id="previewFileId"
        :file-info="fileInfo"
        :library-type="props.libraryType"
        :preview-type="previewType"
        :preview-content="previewContent"
        :is-xls="isXls"
        :loading="loading"
        :file-loading="fileLoading"
        v-model:is-drawer-open="drawer"
        :enable-file-ask="true"
        @closePre="closePre"
        @downloadFile="downloadFile"
        @toggleDrawer="toggleDrawer"
        @update:overlayWidth="val => overlayWidth = val"
      />
    </div>
    <DeleteConfirmDialog
      v-model:visible="deleteConfirmVisible"
      @confirm="handleConfirmDeleteFile"
      @cancel="deleteConfirmVisible = false"
    />

  </div>
</template>

<script setup>
import { ref, nextTick, watch, onMounted, reactive, onBeforeUnmount, computed } from 'vue'
import axios from 'axios'
import '@vue-office/excel/lib/index.css';
import eventBus from '@/utils/eventBus'
import down from '@/assets/arrow_up.png'
import up from '@/assets/arrow_down.png'
import sort from '@/assets/sort.png'
import { ElMessage } from 'element-plus' // 引入 ElMessage
import request from '@/utils/request' // 导入封装的 axios 方法
import { Search } from '@element-plus/icons-vue'
import { DRAWER_WIDTH } from '@/utils/constants'
import UploadPreviewArea from '@/pages/main/component/files/UploadPreviewArea.vue'
import AskKnowledgeDialog from '@/pages/main/component/knowledge/AskKnowledgeDialog.vue'
import EditTagModal from '@/pages/main/component/knowledge/EditTagModal.vue'
import FileContextMenu from '@/pages/main/component/knowledge/FileContextMenu.vue'
import DeleteConfirmDialog from '@/pages/main/component/options/deleteConfirmDialog.vue'
import { downloadFile, FILE_HANDLERS } from '@/utils/files/fileHandlers.js'
import { sharePrivateFileToDepartment, getTargetFolderByUserId } from '@/api/knowledgeBase/actions.js'
import { getFileImgByOriginFile } from '@/utils/common.js'
import TagFilterPanel from '@/pages/main/component/knowledge/TagFilterPanel.vue'

// 接收来自父组件的 folderId
const props = defineProps({
  folderId: {
    type: [Number, String],
    default: 0
  },
  folderName: {
    type: String,
    default: '问知识库'
  },
  libraryType: {
    type: String,
    default: 'personal'
  },
  departmentId: {
    type: [Number, String],
    default: null
  }
})

const isValidDepartmentId = value => value !== undefined && value !== null && value !== ''

const getCurrentUserInfo = () => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch (error) {
    console.error('解析用户信息失败', error)
    return {}
  }
}

const resolveDepartmentId = () => {
  if (props.libraryType !== 'department') {
    return null
  }
  if (isValidDepartmentId(props.departmentId)) {
    return props.departmentId
  }
  const userInfo = getCurrentUserInfo()
  if (isValidDepartmentId(userInfo.departmentId)) {
    return userInfo.departmentId
  }
  return null
}

const searchText = ref('')
const uploadLayoutRef = ref(null)
const leftPanel = ref(null)
const fileListContent = ref(null)
const searchInputWrapper = ref(null)
const contextMenuRef = ref(null)
const optionsMenuRef = ref(null)
const dialogVisible = ref(false)
const fileQueue = ref([])
const previewContent = ref(null)
const previewType = ref('')
const previewFileId = ref(null)
const isPre = ref(false)
const selectedKnow = ref(1)
const isPower = ref(false)
const isDragOver = ref(false)
const permission = ref([])
const currentPage = ref(1)
const pageSize = ref(100)
const totals = ref(100)
const isCollapsed = ref(false)
const loading = ref(false)
const fileLoading = ref(false)
const isXls = ref(false)
const knowOptions = ref([
  {
    value: 1,
    label: '个人知识库'
  },
  {
    value: 2,
    label: '公共知识库'
  }
])
const isShowChatWithDirectory = ref(true)
const deleteConfirmVisible = ref(false)
const deletingIndex = ref(null)
const isEditTagModalVisible = ref(false)
const editingTagFileId = ref(null)
const editingTagTargetIds = ref([])
const isTagPanelVisible = ref(false)
const tagList = ref([])
const selectedTagIds = ref([])
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  file: null,
  index: -1
})

const optionsContextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  file: null,
  index: -1
})

const directoryContextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  parentId: null
})

const MENU_WIDTH = 190
const MENU_HEIGHT = 154
const OPTIONS_MENU_HEIGHT = 120
const OPTIONS_MENU_SINGLE_HEIGHT = 80
const SUB_MENU_OFFSET_X = 8
const SUB_MENU_PADDING_TOP = 10 // 与 FileContextMenu 的内边距保持一致
const SUB_MENU_VERTICAL_SHIFT = 10 // 二级菜单整体上移的像素值

// 常量定义
const allowedFileTypes = '.doc,.docx,.txt,.pdf,.pptx,.ppt,.xls,.xlsx'
const activeIndex = ref(0)
const nameSort = ref(false)
const timeSort = ref(false)
const sizeSort = ref(false)
// 顶部图标提示显隐
const showClearTip = ref(false)
const showCloseTip = ref(false)
const showMaxTip = ref(false)

// 添加最大化状态控制
const isMaximized = ref(false)

const hoveredFileId = ref(null)
const tagInFile = ref([])

const displayedTags = computed(() => tagInFile.value.slice(0, 2))
const hasExtraTags = computed(() => tagInFile.value.length > 2)
const totalTagCount = computed(() => tagInFile.value.length)


const overlayWidth = ref(0) // 右侧覆盖左侧的宽度
const isDragging = ref(false)
const startX = ref(0)
const startOverlay = ref(0)

const startDrag = e => {
  isDragging.value = true
  startX.value = e.clientX
  startOverlay.value = overlayWidth.value

  document.addEventListener('mousemove', handleDrag)
  document.addEventListener('mouseup', stopDrag)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

const drawer = ref(false)
const knowledgeDrawerRef = ref(null)
const askKnowledgeDialogRef = ref(null)

const handleClose = (data) => {
    // 抽屉关闭时的处理逻辑，但不主动关闭抽屉
    // 抽屉的关闭由 v-model 控制
    console.log('Drawer closed:', data)
}

const toggleDrawer = () => {
  drawer.value = !drawer.value
}

const handleFileMouseEnter = file => {
  hoveredFileId.value = file?.uid ?? null
  if (file?.targetItems?.length) {
    tagInFile.value = file.targetItems.map(item => ({
      tagId: item.id,
      targetName: item.targetName ?? ''
    }))
  } else {
    tagInFile.value = []
  }
}

const handleFileMouseLeave = () => {
  hoveredFileId.value = null
}

const hideContextMenu = () => {
  contextMenu.visible = false
  contextMenu.file = null
  contextMenu.index = -1
  directoryContextMenu.visible = false
  directoryContextMenu.parentId = null
  optionsContextMenu.visible = false
  optionsContextMenu.file = null
  optionsContextMenu.index = -1
}

const getDepartmentDirs = payload => {
  if (!payload || !payload.menuId || !contextMenu.visible) {
    directoryContextMenu.visible = false
    directoryContextMenu.parentId = null
    return
  }

  const { menuId, event } = payload
  const containerRect = uploadLayoutRef.value?.getBoundingClientRect()
  if (!containerRect) {
    return
  }

  const hoveredElement = event?.currentTarget ?? event?.target
  const hoveredRect =
    typeof hoveredElement?.getBoundingClientRect === 'function'
      ? hoveredElement.getBoundingClientRect()
      : null

  const pointerY = event?.clientY ?? containerRect.top
  let nextMenuY = hoveredRect
    ? hoveredRect.top - containerRect.top - SUB_MENU_PADDING_TOP
    : pointerY - containerRect.top - SUB_MENU_PADDING_TOP
  const maxY = Math.max(containerRect.height - MENU_HEIGHT, 0)
  nextMenuY = Math.max(0, Math.min(nextMenuY - SUB_MENU_VERTICAL_SHIFT, maxY))

  directoryContextMenu.visible = true
  directoryContextMenu.x = contextMenu.x + MENU_WIDTH + SUB_MENU_OFFSET_X
  directoryContextMenu.y = nextMenuY
  directoryContextMenu.parentId = menuId
}

const handleDirectoryClick = async folderId => {
  if (!folderId) {
    return
  }

  const selectedFile = contextMenu.file
  const fileId = selectedFile?.id
  if (!fileId) {
    ElMessage.error('未获取到文件信息，无法分享')
    return
  }

  let userInfo = {}
  try {
    userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch (error) {
    console.error('解析用户信息失败', error)
    userInfo = {}
  }

  if (!userInfo?.id) {
    ElMessage.error('未获取到用户信息，无法分享')
    return
  }

  try {
    const response = await sharePrivateFileToDepartment(fileId, userInfo.id, folderId)
    if (response?.status) {
      ElMessage.success('分享成功')
      hideContextMenu()
    } else {
      ElMessage.error(response?.message || '分享失败')
    }
  } catch (error) {
    console.error('分享文件失败', error)
    const status = error?.response?.status
    const errorMessage = error?.response?.data?.message
    if (status === 422 && errorMessage) {
      ElMessage.error(errorMessage)
    } else {
      ElMessage.error('分享失败，请稍后重试')
    }
  }
}

const showOptionsMenu = (file, index, event) => {
  if (!file) return
  event?.stopPropagation?.()
  const containerRect = uploadLayoutRef.value?.getBoundingClientRect()
  if (!containerRect) return

  let x = event.clientX - containerRect.left
  let y = event.clientY - containerRect.top

  const menuHeight = props.libraryType === 'personal' ? OPTIONS_MENU_HEIGHT : OPTIONS_MENU_SINGLE_HEIGHT
  const maxX = Math.max(containerRect.width - MENU_WIDTH, 0)
  const maxY = Math.max(containerRect.height - menuHeight, 0)

  x = Math.max(0, Math.min(x, maxX))
  y = Math.max(0, Math.min(y, maxY))

  hideContextMenu()
  optionsContextMenu.visible = true
  optionsContextMenu.x = x
  optionsContextMenu.y = y
  optionsContextMenu.file = file
  optionsContextMenu.index = index
}

const handleOptionsMenuClick = menuId => {
  const { file, index, x, y } = optionsContextMenu
  hideContextMenu()
  if (menuId === 'share' && file) {
    openContextMenu(file, index, null, { x, y })
  } else if (menuId === 'editTag' && file) {
    editingTagFileId.value = file?.id ?? null
    editingTagTargetIds.value = Array.isArray(file?.targetItemIds)
      ? [...file.targetItemIds]
      : Array.isArray(file?.targetItems)
        ? file.targetItems.map(item => item?.id).filter(id => id != null)
        : []
    openEditTagModal()
  }
}

const openEditTagModal = () => {
  isEditTagModalVisible.value = true
}

const handleEditTagClose = () => {
  isEditTagModalVisible.value = false
  editingTagFileId.value = null
  editingTagTargetIds.value = []
}

const handleEditTagConfirm = () => {
  isEditTagModalVisible.value = false
  editingTagFileId.value = null
  editingTagTargetIds.value = []
  loading.value = true
  getFileList()
}

const openContextMenu = (file, index, event, position = null) => {
  if (!file) return
  if (props.libraryType !== 'personal') {
    hideContextMenu()
    return
  }
  event?.stopPropagation?.()
  const containerRect = uploadLayoutRef.value?.getBoundingClientRect()
  if (!containerRect) return

  let x
  let y

  if (event) {
    x = event.clientX - containerRect.left
    y = event.clientY - containerRect.top
  } else if (position) {
    x = position.x
    y = position.y
  } else {
    x = 0
    y = 0
  }

  const maxX = Math.max(containerRect.width - MENU_WIDTH, 0)
  const maxY = Math.max(containerRect.height - MENU_HEIGHT, 0)

  x = Math.max(0, Math.min(x, maxX))
  y = Math.max(0, Math.min(y, maxY))

  contextMenu.visible = true
  contextMenu.x = x
  contextMenu.y = y
  contextMenu.file = file
  contextMenu.index = index
  directoryContextMenu.visible = false
  directoryContextMenu.parentId = null
  optionsContextMenu.visible = false
}

const handleGlobalClick = event => {
  if (isTagPanelVisible.value) {
    const containerEl = searchInputWrapper.value
    if (containerEl && !containerEl.contains(event.target)) {
      isTagPanelVisible.value = false
    }
  }

  if (!contextMenu.visible && !optionsContextMenu.visible && !directoryContextMenu.visible) return
  if (event?.target?.closest?.('.file-context-menu')) {
    return
  }
  hideContextMenu()
}

const handleGlobalContextMenu = event => {
  if (!contextMenu.visible && !optionsContextMenu.visible && !directoryContextMenu.visible) return
  if (event?.target?.closest?.('.file-context-menu')) {
    return
  }
  hideContextMenu()
}

// 添加最大化功能处理函数
const toggleMaximize = () => {
  isMaximized.value = !isMaximized.value
}

// 点击抽屉外部时关闭抽屉（在无遮罩情况下）
const handleOutsideClick = (event) => {
  if (!drawer.value) return
  
  // 获取当前打开的抽屉面板
  const drawerPanels = document.querySelectorAll('.knowledge-drawer .el-drawer')
  let isClickInsideDrawer = false
  
  // 检查点击是否在当前抽屉面板内部
  drawerPanels.forEach(panel => {
    if (panel.contains(event.target)) {
      isClickInsideDrawer = true
    }
  })
  
  // 如果点击在抽屉外部，则关闭抽屉
  if (!isClickInsideDrawer) {
    // 异步关闭，避免影响当前点击行为
    setTimeout(() => {
      drawer.value = false
    }, 0)
  }
}

// 暂时禁用外部点击关闭功能，避免误触
// watch(drawer, (isOpen) => {
//   if (isOpen) {
//     // 延迟添加事件监听，确保抽屉已经渲染完成
//     nextTick(() => {
//       document.addEventListener('click', handleOutsideClick, true)
//     })
//   } else {
//     document.removeEventListener('click', handleOutsideClick, true)
//   }
// })

const handleDrag = e => {
  if (!isDragging.value) return

  const dx = startX.value - e.clientX // 向左拖动为负值
  let newOverlay = startOverlay.value + dx

  // 限制覆盖范围 (0到735px)
  newOverlay = Math.max(0, Math.min(newOverlay, 700))

  overlayWidth.value = newOverlay
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', handleDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

const closePre = () => {
  isPre.value = false
  // 清空聊天记录
  if (knowledgeDrawerRef.value) {
    knowledgeDrawerRef.value.clearChatHistory()
  }
  // 关闭文件预览后，重新显示问知识库弹窗
  isShowChatWithDirectory.value = true
}

const clearData = () => {
  searchText.value = ''
  getFileList()
}
// 搜索方法
const searchData = () => {
  getFileList()
  // 调用后端接口或其他搜索逻辑
}

const handleSearchFocus = async () => {
  if (!['personal', 'department'].includes(props.libraryType)) {
    return
  }

  if (!tagList.value.length) {
    await fetchTagList()
  }

  isTagPanelVisible.value = tagList.value.length > 0
}

const handleTagSelectionUpdate = newSelectedTagIds => {
  if (!Array.isArray(newSelectedTagIds)) {
    selectedTagIds.value = []
  } else {
    selectedTagIds.value = [...newSelectedTagIds]
  }
  getFileList()
}

const deleteData = id => {
  const userInfo = getCurrentUserInfo()
  const departmentId = resolveDepartmentId()
  const isDepartmentLibrary = props.libraryType === 'department'
  if (isDepartmentLibrary && !isValidDepartmentId(departmentId)) {
    ElMessage.error('未获取到部门信息，无法删除部门知识库文件')
    return
  }

  const requestConfig = isDepartmentLibrary
    ? {
        url: `/Files/departmentFileDelete?fileId=${id}&userId=${userInfo.id}&departmentId=${departmentId}`,
        payload: {}
      }
    : {
        url: '/Files/knowledgeFileDelete',
        payload: {
          file: [id],
          userId: userInfo.id,
          target: permission.value,
          isPublic: selectedKnow.value !== 1
        }
      }

  request
    .post(requestConfig.url, requestConfig.payload)
    .then(res => {
      if (res.status) {
        ElMessage.success("删除成功")
        getFileList()
      }
    })
    .catch(err => {
      getFileList()
      const responseMessage = err?.response?.data?.message
      const responseCode = err?.response?.data?.code
      ElMessage.error({
        message:
          responseCode === 401 && responseMessage
            ? responseMessage
            : '删除失败,请稍后再试',
        duration: 3000 // 显示3秒
      })
      console.error(err)
    })
}
// 新增删除处理函数
const handleDelete = (index, event) => {
  event?.stopPropagation() // 阻止事件冒泡
  hideContextMenu()
  const deletedFile = fileQueue.value[index]
  const id = deletedFile.id
  // 强制 DOM 更新（关键修复）
  // fileQueue.value.splice(index, 1)
  nextTick(() => {
    isPre.value = false
    // 删除文件时清空聊天记录
    if (knowledgeDrawerRef.value) {
      knowledgeDrawerRef.value.clearChatHistory()
    }
    deleteData(id)
  })
}

const openDeleteConfirm = (index, event) => {
  event?.stopPropagation()
  hideContextMenu()
  deletingIndex.value = index
  deleteConfirmVisible.value = true
}

const handleConfirmDeleteFile = () => {
  if (deletingIndex.value === null || deletingIndex.value === undefined) {
    deleteConfirmVisible.value = false
    return
  }
  // 复用原有删除逻辑
  handleDelete(deletingIndex.value)
  deletingIndex.value = null
  deleteConfirmVisible.value = false
}

const checkFileSize = file => {
  const isLt10M = file.size / 1024 / 1024 < 50
  if (!isLt10M) {
    ElMessage.warning('附件大小不能超过50MB!')
  }
  return isLt10M
}
// 新增独立上传队列（网页[5]思路扩展）
let uploadPromises = [] // 存储所有上传Promise
const startAutoUpload = async () => {
  try {
    uploadPromises = fileQueue.value.filter(f => f.status === 'pending').map(file => uploadSingleFile(file))

    // 等待全部完成（网页[7]）
    await Promise.all(uploadPromises)

    // 全部成功后刷新列表
    getFileList()

    // 清空队列
    // fileQueue.value = []
  } catch (error) {
    console.error('批量上传失败', error)
  }
}
const uploadSingleFile = async file => {
  return new Promise((resolve, reject) => {
    previewFileId.value = file.uid
    const userInfo = getCurrentUserInfo()
    const formData = new FormData()
    formData.append('file', file.raw)
    formData.append('userId', userInfo.id)
    formData.append('target', permission.value)
    formData.append('folderId', props.folderId)

    let uploadUrl = '/Files/knowledgeFileUpload'

    if (props.libraryType === 'department') {
      const departmentId = resolveDepartmentId()
      if (!isValidDepartmentId(departmentId)) {
        const errorMessage = '未获取到部门信息，无法上传部门知识库文件'
        file.status = 'error'
        ElMessage.error(errorMessage)
        reject(new Error(errorMessage))
        return
      }
      formData.append('departmentId', departmentId)
      formData.append('type', 'department')
    } else {
      formData.append('type', 'private')
    }

    axios
      .post(import.meta.env.VITE_API_BASE_URL + uploadUrl, formData, {
        onUploadProgress: progress => {
          file.progress = Math.round((progress.loaded / progress.total) * 100)
        }
      })
      .then(res => {
        if (res.data.status) {
          file.status = 'success'
          resolve()
        } else {
          file.status = 'error'
          ElMessage.error(res.data.message)
          getFileList()
          reject(new Error(res.data.message))
        }
      })
      .catch(error => {
        file.status = 'error'
        console.error('上传失败:', error)

        // 处理不同的错误情况
        if (error.response) {
          // 服务器返回了错误状态码 (如 422, 500 等)
          const errorMessage = error.response.data?.message || `上传失败: ${error.response.status}`
          ElMessage.error(errorMessage)
        } else if (error.request) {
          // 请求已发出但没有收到响应
          ElMessage.error('网络连接失败，请检查网络')
        } else {
          // 其他错误
          ElMessage.error('上传失败，请稍后重试')
        }

        getFileList()
        reject(error)
      })
  })
}
const uploadTimer = ref(null)
// 附件添加处理
const handleFileAdd = async uploadFile => {
  if (uploadFile.size / 1024 / 1024 > 50) {
    ElMessage.warning('附件大小不能超过50MB!')
    return
  }
  const file = {
    ...uploadFile,
    uid: uploadFile.uid,
    status: 'pending', // 新增状态字段
    fileSize: uploadFile.size,
    originalFileName: uploadFile.name
  }
  fileQueue.value = [file, ...fileQueue.value]
  // 自动触发上传（网页[5]防抖优化）
  clearTimeout(uploadTimer.value)
  uploadTimer.value = setTimeout(startAutoUpload, 300)
}

const fileInfo = ref({})

const handlePreview = async (file) => {
  if (!file) return

  const fileExtension = getTextAfterLastDot(file.name)
  fileLoading.value = true
  // 进入文件预览时隐藏问知识库弹窗
  isShowChatWithDirectory.value = false

  try {
    isPre.value = true
    fileInfo.value = file
    isXls.value = fileExtension === 'xls' // 设置是否为旧版Excel

    // 获取对应的处理器
    const handler = FILE_HANDLERS.get(fileExtension)

    if (handler) {
      const { content, type } = await handler(file)
      previewContent.value = content
      previewType.value = type
      
      // 如果预览的是 Excel 文件，关闭抽屉
      if (type === 'excel') {
        drawer.value = false
      }
    } else {
      previewContent.value = '不支持此附件预览'
      previewType.value = 'unsupported'
    }

  } catch (error) {
    console.error('预览失败:', error)
    previewContent.value = '附件预览失败'
    previewType.value = 'error'
  } finally {
    fileLoading.value = false
  }
}

const closeChatWithDirectoryDialog = () => {
  // 不销毁弹窗实例，只进入透明态，保留输入框
  if (askKnowledgeDialogRef.value && askKnowledgeDialogRef.value.enterTransparentMode) {
    askKnowledgeDialogRef.value.enterTransparentMode()
  } else {
    isShowChatWithDirectory.value = false
    // 兜底：若被销毁则立即重建以便下次点击仍可见输入框
    nextTick(() => {
      isShowChatWithDirectory.value = true
    })
  }
}

const sortFiles = val => {
  const type = val === 0 ? 'time' : val === 1 ? 'name' : 'size'
  const isUp = val === 0 ? timeSort.value : val === 1 ? nameSort.value : sizeSort.value
  const data = JSON.parse(JSON.stringify(fileQueue.value))
  return data.slice().sort((a, b) => {
    // 1. 分离 pending 与非 pending 项
    const aIsPending = a.status === 'pending'
    const bIsPending = b.status === 'pending'

    // pending项始终在前，且不参与后续排序[6](@ref)
    if (aIsPending && !bIsPending) return -1
    if (!aIsPending && bIsPending) return 1
    if (aIsPending && bIsPending) return 0 // 两pending项保持原顺序
    // 2. 非 pending 项按 type 和 sort 排序
    let compareValue
    switch (type) {
      case 'time':
        if (isUp) {
          compareValue = new Date(a.createTime) - new Date(b.createTime) // 时间戳比较[3](@ref)
        } else {
          compareValue = new Date(b.createTime) - new Date(a.createTime) // 时间戳比较[3](@ref)
        }

        break
      case 'size':
        if (isUp) {
          compareValue = a.fileSize - b.fileSize // 数值比较[2,5](@ref)
        } else {
          compareValue = b.fileSize - a.fileSize // 数值比较[2,5](@ref)
        }
        break
      case 'name':
        if (isUp) {
          compareValue = a.originalFileName.localeCompare(b.originalFileName, 'zh') // 数值比较[2,5](@ref)
        } else {
          compareValue = b.originalFileName.localeCompare(a.originalFileName, 'zh') // 数值比较[2,5](@ref)
        }
        break
      default:
        compareValue = 0
    }

    // 3. 根据 sort 控制方向[1,4](@ref)
    return compareValue
  })
}

const changeType = val => {
  activeIndex.value = val
  // 切换排序类型时隐藏问知识库弹窗
  const hasPending = fileQueue.value.some(item => item.status === 'pending')
  if (val === 1) {
    nameSort.value = !nameSort.value
  } else if (val === 0) {
    timeSort.value = !timeSort.value
  } else if (val === 2) {
    sizeSort.value = !sizeSort.value
  }
  if (hasPending) {
    // sortFiles(val)
    fileQueue.value = sortFiles(val)
    return
  }
  getFileList()
}
const total = ref(0)
const totalSize = ref(0)
const point = ref('KB')
const getInfo = val => {
  total.value = fileQueue.value.length
  let size = 0
  for (var i = 0; i < fileQueue.value.length; i++) {
    size = size + fileQueue.value[i].fileSize
  }
  if (size / 1024 < 1024) {
    point.value = 'KB'
    totalSize.value = (size / 1024).toFixed(1)
  } else {
    point.value = 'MB'
    totalSize.value = (size / 1024 / 1024).toFixed(1)
  }
}

const handleDragOver = () => {
  isDragOver.value = true
}

const handleDragLeave = () => {
  isDragOver.value = false
}
const handleDrop = e => {
  const files = Array.from(e.dataTransfer.files)
  if (!files[0]) {
    isDragOver.value = false
    return
  }
  const fileExtension = getTextAfterLastDot(files[0].name)
  const supportedFormats = ['txt', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'pdf']
  if (!supportedFormats.includes(fileExtension)) {
    isDragOver.value = false
    ElMessage.warning('暂不支持此格式上传')
    return
  }
  const data = {
    name: files[0].name,
    percentage: 0,
    size: files[0].size,
    status: 'ready',
    raw: files[0]
  }
  handleFileAdd(data)
}

const getFileList = () => {
  const userInfo = getCurrentUserInfo()
  if (!props.folderId) return

  const requestData = {
    userId: userInfo.id,
    target: permission.value,
    isPublic: selectedKnow.value !== 1,
    sortType: activeIndex.value === 0 ? 'time' : activeIndex.value === 1 ? 'name' : 'size',
    increase: activeIndex.value === 0 ? timeSort.value : activeIndex.value === 1 ? nameSort.value : sizeSort.value,
    keywords: searchText.value,
    page: currentPage.value,
    pageSize: pageSize.value,
    folderId: props.folderId,
    tagList: selectedTagIds.value
  }

  let requestUrl = '/FileFolder/getFilesInFolder'

  if (props.libraryType === 'department') {
    const departmentId = resolveDepartmentId()
    if (!isValidDepartmentId(departmentId)) {
      loading.value = false
      isDragOver.value = false
      ElMessage.error('未获取到部门信息，无法获取部门知识库文件')
      return
    }
    requestData.departmentId = departmentId
    requestUrl = '/FileFolder/getFilesInDepartmentFolder'
  }

  request
    .post(requestUrl, requestData)
    .then(res => {
      loading.value = false
      if (res.status) {
        fileQueue.value = res.data.content
        totals.value = res.data.totalElements
        isDragOver.value = false
        getInfo()
      } else {
        isDragOver.value = false
      }
    })
    .catch(err => {
      loading.value = false
      isDragOver.value = false
      console.error(err)
    })
}

// 当 folderId 变化时，刷新文件列表
watch(
  () => props.folderId,
  () => {
    // 仅当对话框在知识库文件页使用时刷新
    getFileList()
    // 重新选择文件夹时，确保“问知识库”弹窗重新显示
    isShowChatWithDirectory.value = true
  }
)

watch(
  () => props.libraryType,
  newLibraryType => {
    if (newLibraryType !== 'personal') {
      isTagPanelVisible.value = false
      selectedTagIds.value = []
    }
  }
)

watch(fileQueue, () => {
  hideContextMenu()
})
const handleSizeChange = val => {
  pageSize.value = val
  getFileList()
  // 这里通常调用API获取新数据
}

const handleCurrentChange = val => {
  currentPage.value = val
  getFileList()
  // 这里通常调用API获取新数据
}
const openFile = (val, ary) => {
  dialogVisible.value = true
  isPre.value = false
  const power = localStorage.getItem('powerList')
  permission.value = power.length > 0 ? power : ''
  // isPower.value = permission.value ? true : false
  isPower.value = false
  getFileList()
}
const getCollapsed = val => {
  isCollapsed.value = val
}
const getTextAfterLastDot = str => {
  const lastDotIndex = str.lastIndexOf('.')
  if (lastDotIndex === -1) return '' // 没有点号时返回空字符串
  return str.slice(lastDotIndex + 1)
}
const getFile = fileObj => {
  hideContextMenu()
  fileLoading.value = true
  // 切换文件时清空聊天记录
  if (knowledgeDrawerRef.value) {
    knowledgeDrawerRef.value.clearChatHistory()
  }
  // 使用 POST 请求（与后端 @PostMapping 匹配）
  fetch(import.meta.env.VITE_API_BASE_URL + '/Files/knowledgeFileById?id=' + fileObj.id, {
    method: 'POST',
    headers: { Accept: 'application/octet-stream' }, // 明确接收二进制
    responseType: 'blob' // 关键参数
  })
    .then(response => {
      // 从 Content-Disposition 中解析附件名
      const disposition = response.headers.get('Content-Disposition')
      let originalFileName = 'default_filename' // 默认附件名
      if (disposition && disposition.indexOf('filename=') !== -1) {
        originalFileName = disposition.split('filename=')[1].replace(/"/g, '')
      }

      // 获取二进制数据
      return response.blob().then(blob => ({ blob, originalFileName }))
    })
    .then(({ blob, originalFileName }) => {
      // 将 Blob 转换为 File 对象（类似 file.raw）
      const file = new File([blob], originalFileName, { type: blob.type })
      const fileOther = {
        raw: file,
        uid: file.lastModified,
        size: file.size,
        name: decodeURIComponent(fileObj.originalFileName),
        extension: getTextAfterLastDot(fileObj.originalFileName),
        cancel: null,
        source: null,
        fileId: fileObj.id
      }
      previewFileId.value = fileOther.uid
      fileLoading.value = false
      // 此时可以像处理 el-upload 的 file.raw 一样处理 file
      handlePreview(fileOther)
    })
    .catch(error => {
      fileLoading.value = false
      console.error('获取附件失败:', error)
    })
}
onMounted(() => {
  eventBus.on('setCollapsed', getCollapsed)
  document.addEventListener('click', handleGlobalClick)
  document.addEventListener('contextmenu', handleGlobalContextMenu)
  if (fileListContent.value) {
    fileListContent.value.addEventListener('scroll', hideContextMenu)
  }
  if (leftPanel.value) {
    leftPanel.value.addEventListener('scroll', hideContextMenu)
  }
  loading.value = true
  openFile()
})

const fetchTagList = async () => {
  try {
    const userInfoString = localStorage.getItem('userInfo')
    const userInfo = userInfoString ? JSON.parse(userInfoString) : null

    if (!userInfo?.id) {
      tagList.value = []
      return
    }

    const response = await getTargetFolderByUserId(userInfo.id)

    if (response?.status) {
      tagList.value = Array.isArray(response?.data) ? response.data : []
    } else {
      tagList.value = []
      if (response?.message) {
        ElMessage.error(response.message)
      }
    }
  } catch (error) {
    console.error('获取标签列表失败', error)
    ElMessage.error('获取标签列表失败，请稍后重试')
    tagList.value = []
  }
}

onBeforeUnmount(() => {
  document.removeEventListener('click', handleGlobalClick)
  document.removeEventListener('contextmenu', handleGlobalContextMenu)
  if (fileListContent.value) {
    fileListContent.value.removeEventListener('scroll', hideContextMenu)
  }
  if (leftPanel.value) {
    leftPanel.value.removeEventListener('scroll', hideContextMenu)
  }
  eventBus.off?.('setCollapsed', getCollapsed)
})

// 清空对话方法（页面切换时调用，不显示确认弹窗）
const clearChatHistory = () => {
  if (askKnowledgeDialogRef.value) {
    // 直接调用执行清空的方法，跳过确认弹窗
    askKnowledgeDialogRef.value.executeClearChatHistory()
  }
}

// 用户主动清空对话的方法（显示确认弹窗）
const clearChatHistoryWithConfirm = () => {
  if (askKnowledgeDialogRef.value) {
    askKnowledgeDialogRef.value.clearChatHistory()
  }
}

defineExpose({ openFile, clearChatHistory, clearChatHistoryWithConfirm })
</script>

<style scoped lang="less">
.directory_chat {
  position: absolute;
  top: 131px;
  left: 50%;
  transform: translateX(-50%);
  width: 896px;
  height: 929px;
  background: #fff;
  filter: drop-shadow(0 4px 16px #b7b8b94d);
  z-index: 10;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  padding-bottom: 0;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  
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
    background-color: #7f7f7f;
    transition: margin-bottom 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  }
  .ask_input_area {
    width: auto;
    height: 48px;
    margin: auto 24px 24px;
    background-color: #1B6CFF;
    transition: margin-bottom 0.5s cubic-bezier(0.4, 0, 0.2, 1);
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
/* 上传中状态样式 */
.uploading-file {
  background: rgba(0, 0, 0, 0.5) !important; /* 轨道背景颜色 */
  /* 可添加加载动画 */
  position: relative;
  color: rgba(0, 0, 0, 0.1) !important;
  div {
    color: rgba(0, 0, 0, 0.1) !important;
  }
}
.uploading-file::after {
  content: '上传中...';
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 12px;
  color: #fff;
}


.delete-icon {
  margin-right: 5px;
  width: 24px;
  height: 24px;
}
.upload_btn {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  margin-bottom: 5px;
}
.custom-upload-dialog {
  height: 1200px;
  --el-dialog-margin-top: 5vh;
}

.upload-layout {
  display: flex;
  height: 100%;
  gap: 20px;
  position: relative;
  overflow: hidden;
  :deep(.el-upload-dragger) {
    border: none !important;
    padding: 0px;
    background-color: transparent;
  }
}
.upload-layout.drag-over {
  border-color: #409eff;
  background-color: rgba(64, 158, 255, 0.1);
}

.file-list {
  width: 700px;
  margin-left: 40px;
  height: calc(100% - 20px);
  overflow-y: hidden;
  overflow-x: visible;
  margin-right: 40px;
  display: flex;
  position: relative;
  flex-direction: column;
  margin-top: 15px;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
  background: white; /* 确保背景不透明 */
  .file_item {
    overflow-y: auto;
    overflow-x: visible;
    margin-top: 100px;
    height: calc(100% - 155px);
    float: left;
  }
  .file_item::-webkit-scrollbar {
    width: 1px; /* 滚动条宽度 */
  }
  .file_item::-webkit-scrollbar-track {
    background: #fff; /* 轨道背景颜色 */
    border-radius: 0px; /* 轨道圆角 */
  }
  .file_item::-webkit-scrollbar-thumb {
    background: #fff; /* 轨道背景颜色 */
    border-radius: 0px; /* 滑块圆角 */
    border: 1px solid #fff; /* 滑块边框 */
  }
  .file_item::-webkit-scrollbar-thumb:hover {
    background: #fff; /* 滑块悬停时的颜色 */
  }
  .file_search {
    margin-left: 15px;
    position: absolute;
    top: 26px;
    display: flex;
    align-items: center;
    width: calc(100% - 30px);
         .file_left {
       display: flex;
       flex-direction: row;
       flex: 1;
       height: 34px;
       align-items: center;
       .file_content {
         display: flex;
         align-items: center;
        .active {
          display: flex;
          margin-top: 0;
          align-items: center;
          .active_item {
            width: 100px;
            height: 34px;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 4px;
            cursor: pointer;
            font-size: 14px;
            img {
              width: 20px;
              height: 20px;
              margin-left: 4px;
              transform: translateY(0.4px);
            }
          }
        }
        .file_select {
          color: #333333;
          font-size: 16px;
          line-height: 33px;
        }
        .file_upload {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 140px;
          height: 34px;
          line-height: 34px;
          border-radius: 22px;
          background: #fff;
          border: 1px solid #1b6cff;
          text-align: center;
          margin-right: 15px;
          font-size: 18px;
          font-weight: 400;
          color: #1b6cff;
          cursor: pointer;
          img {
            transform: translateY(0);
          }
        }
      }
    }
         .file_right {
       display: flex;
       flex-direction: row-reverse;
       flex: 1;
       margin-right: 108px;
       align-items: center;
       .file_info { white-space: nowrap; display: flex; align-items: center; height: 34px; margin-top: 0;margin-right: 36px}
        .file_content {
          display: flex; flex-direction: row; align-items: center; flex-wrap: nowrap;
          .search-input-wrapper {
            position: relative;
            width: 100%;
            max-width: 294px;
          }
          .tag-filter-panel-wrapper {
            position: absolute;
            top: calc(100% + 8px);
            left: 0;
            z-index: 20;
          }
          :deep(.el-input) {
            --el-input-height: 44px;
            --el-input-border-radius: 8px;
            --el-input-border-color: #DCE6FA;
            --el-input-hover-border-color: #DCE6FA;
            --el-input-focus-border-color: #409EFF;
          }
          :deep(input::placeholder) {
            font-size: 14px;
          }
          :deep(.el-input__wrapper) {
            width: 294px;
            height: 44px;
            box-shadow: none;
            box-sizing: border-box;
            border: 1px solid #DCE6FA;
            margin-top: 0;
          }
          :deep(.el-input__wrapper.is-focus),
          :deep(.el-input.is-focus .el-input__wrapper) {
            border-color: #409EFF !important;
            box-shadow: none;
          }
      }
    }
  }
  .upload_list {
    width: calc(100% - 30px);
    margin-left: 15px;
    position: absolute;
    top: 95px;
  }
}

.file-item {
  margin-bottom: 10px;
  border-radius: 6px;
  transition: all 0.3s ease;
  width: 15%;
  float: left;
  margin-left: 14px;
  background-color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  height: 100px;
  box-sizing: border-box;
  overflow: visible;
  font-size: 12px;
  cursor: pointer;
  position: relative;
  .file_img {
    width: 30px;
    height: 37px;
    margin-top: 4px;
    transition: all 0.3s ease;

    img {
      width: 100%;
      height: 100%;
      transition: all 0.3s ease;
    }
  }
}

.file-item:hover {
  background: #ebedf0;

  .file_img {
    transform: scale(1.03);

    img {
      transform: scale(1.03);
    }
  }
}

.file-tags {
  position: absolute;
  left: 50%;
  bottom: 6px;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 6px;
  z-index: 2;
}

.file-tag-item {
  width: 68px;
  height: 24px;
  border-radius: 12px;
  background: #b7b8b9;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 0 8px;
  box-sizing: border-box;
  color: #fff;
  font-weight: 400;
  font-size: 12px;
  line-height: 24px;
  gap: 4px;
}

.file-tag-item img {
  width: 12px;
  height: 12px;
}

.file-tag-item span {
  flex: 1;
  color: #fff;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-tag-count {
  width: 24px;
  height: 24px;
  border-radius: 12px;
  background: #b7b8b9;
  color: #fff;
  font-size: 12px;
  font-weight: 400;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 24px;
}

.preview-file {
  flex: 1;
  display: flex;
  min-width: 0;
}

.file-info {
  display: flex;
  margin-top: 8px;
  display: flex;
}

.originalFileName {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  white-space: normal;
  color: #333;
  font-size: 12px;
  padding-top: 4px;
  transition: all 0.3s ease;
  text-align: center; /* 居中文本，使其与上方图标对齐 */
  margin: 0 auto;      /* 当宽度小于容器时，整体块也保持居中 */
  word-break: break-all;
  hyphens: auto;
  line-height: 18px;
  max-height: 36px;
}

.file-type {
  color: #909399;
  font-size: 0.9em;
}

.file-actions {
  margin-top: 4px;
  text-align: right;
  display: flex;
  :deep(.el-button--small:hover) {
    color: #fff;
  }
}

// 移除抽屉左侧阴影
:global(.el-drawer) {
  box-shadow: none !important;
  border-left: 1px solid #f4f1f1;
}
</style>

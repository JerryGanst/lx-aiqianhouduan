<template>
  <!-- 右侧上传区域 -->
  <!--style用于操作拖拽-->
  <div
    class="upload-area"
    v-if="isPre"
    ref="rightPanel"
    :style="containerStyle"
  >
    <div class="drag-bar" @mousedown.stop="startDrag"></div>
    <div class="top_container">
      <div class="file_title_item">
        <div class="file_title_content" :title="fileInfo?.name">{{ fileInfo?.name || '文件名' }}</div>
        <div class="file_title_description">
          <span>大小 : {{ fileInfo.size ? (fileInfo.size / 1024).toFixed(1) : 0 }}KB</span>
          <span style="margin-left: 14px">类型 : {{ fileInfo.extension }}</span>
        </div>
      </div>
      
      <div class="download_file" @mouseenter="showDownloadTip = true" @mouseleave="showDownloadTip = false">
        <img src="@/assets/knowledgeBase/download.png" alt="下载" class="icon-img" @click="downloadFile(fileInfo)"/>
        <transition name="fade">
          <div v-if="showDownloadTip" class="tooltip-bottom">下载</div>
        </transition>
      </div>
      <div class="close_pre_dialog" :style="{ top: closeBtnTop + 'px' }" @click="closePre" @mouseenter="showCloseTip = true" @mouseleave="showCloseTip = false">
        <img src="@/assets/knowledgeBase/close.png" alt="关闭预览" class="icon-img" />
        <transition name="fade">
          <div v-if="showCloseTip" class="tooltip-bottom">关闭</div>
        </transition>
      </div>
    </div>
    <!-- 附件预览 -->
    <div
      v-if="previewFileId"
      class="preview-container"
      :key="previewFileId"
      style="margin: 15px"
      :style="{
        margin: previewFileId ? '0 15px 10px 15px' : '15px',
        width: 'auto',  // 改为自动宽度
        maxWidth: '100%' // 限制最大宽度
      }"
    >
      <div v-if="previewType === 'text'" class="text-preview" style="padding: 0 15px; width: 100%">
        <pre>{{ previewContent }}</pre>
      </div>
      <div v-else-if="previewType === 'html'" class="html-preview" v-html="previewContent" style="width: 100%"></div>
      <div v-else-if="previewType === 'pdf'" style="width: 100%">
        <iframe :src="previewContent" frameborder="0" class="pdf-frame"></iframe>
      </div>
      <div v-else-if="['pptx', 'ppt'].includes(previewType)" class="ppt-preview">
        <vue-office-pptx :src="previewContent" class="pptx-viewer" />
      </div>
      <div v-else-if="previewType === 'excel'" style="width: 1192px;height: 100%" >
        <vue-office-excel
          :src="previewContent"
          :options="{beforeTransformData, xls: isXls}"
        />
      </div>
      <div v-else class="unsupported-preview">暂不支持此格式预览</div>
    </div>
    <div
      v-else="previewFileId"
      class="preview-container"
      :style="{ margin: previewFileId ? '0 15px 10px 15px' : '15px' }"
    >
      <div style="width: 100%; display: flex; justify-content: center; margin-top: 154px">
        <img src="@/assets/no-file.png" style="width: 150px; height: 150px" />
      </div>
      <div class="unsupported-preview" style="padding: 0px">请先上传附件即可预览</div>
    </div>
    <!-- 文件提问按钮：预览为 excel(xls/xlsx) 时隐藏 -->
    <div v-if="enableFileAsk && previewFileId" class="file-ask-button" @click="toggleDrawer">
      <img src="@/assets/knowledgeBase/chat.png" alt="提问" class="ask-icon" />
      <span class="ask-text">当前文件提问</span>
    </div>
  </div>
  <div v-if="loading" class="loading-mask">
    <div class="loading-content">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      <span class="loading-text">数据加载中...</span>
    </div>
  </div>
  <div v-if="fileLoading" class="loading-mask">
    <div class="loading-content">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      <span class="loading-text">文件加载中...</span>
    </div>
  </div>
  
  <!-- 知识库抽屉 -->
  <KnowledgeDrawer
    ref="knowledgeDrawerRef"
    v-model="isDrawerOpen"
    :title="`${fileInfo?.name}`"
    :file-id="previewFileId"
    :file-info="fileInfo"
    @before-close="handleDrawerClose"
  />
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, watch, computed } from 'vue'
import VueOfficePptx from '@vue-office/pptx'
import VueOfficeExcel from '@vue-office/excel'
import '@vue-office/excel/lib/index.css'
import { Loading } from '@element-plus/icons-vue'
import { beforeTransformData } from "@/utils/common.js"
import { DRAWER_WIDTH } from '@/utils/constants'
import KnowledgeDrawer from '@/pages/main/component/knowledge/KnowledgeDrawer.vue'

const props = defineProps({
  isPre: Boolean,
  overlayWidth: Number,
  previewFileId: [String, Number],
  fileInfo: Object,
  previewType: String,
  previewContent: [String, Object],
  isXls: Boolean,
  loading: Boolean,
  fileLoading: Boolean,
  libraryType: {
    type: String,
    default: 'personal'
  },
  isLocal: {
    type: Boolean,
    default: false
  },
  isDrawerOpen: Boolean,
  enableFileAsk: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['closePre', 'downloadFile', 'update:overlayWidth', 'toggleDrawer', 'update:isDrawerOpen'])

const overlayWidth = ref(props.overlayWidth)
const isDragging = ref(false)
const startX = ref(0)
const startOverlay = ref(0)
const closeBtnTop = ref(28.3)
const showDownloadTip = ref(false)
const showCloseTip = ref(false)
const rightPanel = ref(null)
const knowledgeDrawerRef = ref(null)

// 处理抽屉的双向绑定
const isDrawerOpen = computed({
  get: () => props.isDrawerOpen,
  set: (value) => emit('update:isDrawerOpen', value)
})

// 计算容器需要的综合覆盖宽度：用户拖拽覆盖 + 抽屉打开时的固定宽度偏移
const combinedOverlay = computed(() => overlayWidth.value + (props.isDrawerOpen ? DRAWER_WIDTH : 0))

// 应用于上传区域容器的样式，使文件内容与拖拽条一起移动
const containerStyle = computed(() => ({
  'margin-left': `-${combinedOverlay.value}px`,
  width: `calc(100% + ${combinedOverlay.value}px)`
}))

const startDrag = e => {
  isDragging.value = true
  startX.value = e.clientX
  startOverlay.value = overlayWidth.value

  document.addEventListener('mousemove', handleDrag)
  document.addEventListener('mouseup', stopDrag)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

const handleDrag = e => {
  if (!isDragging.value) return

  const dx = startX.value - e.clientX
  let newOverlay = startOverlay.value + dx

  newOverlay = Math.max(0, Math.min(newOverlay, 700))

  overlayWidth.value = newOverlay
  emit('update:overlayWidth', newOverlay)
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', handleDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

const closePre = () => {
  // 重置所有悬浮提示框状态
  showDownloadTip.value = false
  showCloseTip.value = false
  // 清空聊天记录
  if (knowledgeDrawerRef.value) {
    knowledgeDrawerRef.value.clearChatHistory()
  }
  emit('closePre')
}

const downloadFile = (file) => {
  emit('downloadFile', { ...file, isLocal: props.isLocal, libraryType: props.libraryType })
}

const toggleDrawer = () => {
  emit('toggleDrawer')
}

const handleDrawerClose = () => {
  emit('toggleDrawer')
}

// 计算关闭按钮在视口中的 top，保证固定定位时与容器顶部保持 28.3px 的偏移
const updateCloseBtnTop = () => {
  const panel = rightPanel.value
  if (!panel) return
  const rect = panel.getBoundingClientRect()
  closeBtnTop.value = rect.top + 28.3
}

onMounted(() => {
  nextTick(updateCloseBtnTop)
  window.addEventListener('scroll', updateCloseBtnTop, true)
  window.addEventListener('resize', updateCloseBtnTop)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', updateCloseBtnTop, true)
  window.removeEventListener('resize', updateCloseBtnTop)
})

watch(() => props.isPre, async () => {
  await nextTick()
  updateCloseBtnTop()
})

watch(overlayWidth, async () => {
  await nextTick()
  updateCloseBtnTop()
})
</script>

<style scoped lang="less">
.loading-mask {
  position: absolute;
  top: 100px;
  left: 0;
  right: 0;
  bottom: 140px;
  background-color: transparent;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.loading-text {
  color: #1b6cff;
  font-size: 16px;
}

.is-loading {
  color: #1b6cff;
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}



.pdf-frame {
  width: 100%;
  height: calc(100vh - 180px);
  border: 0;
  box-shadow: none !important;
  outline: none !important;
}

.upload-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  box-shadow: -2px 0 5px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 2;
  background-color: #fff;
}

.preview-container {
  height: calc(100% - 160px);
  border-radius: 4px;
  margin: 0 15px 10px 15px;
  overflow: auto;
  background-color: #f8f9fb;
  display: flex;
  justify-content: center; /* 内容水平居中 */
  padding: 10px;
}

.preview-container::-webkit-scrollbar {
  width: 1px;
}

.preview-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 0px;
}

.preview-container::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 0px;
  border: 1px solid #f1f1f1;
}

.preview-container::-webkit-scrollbar-thumb:hover {
  background: #555;
}

.drag-bar {
  position: absolute;
  left: 0px;
  height: 100%;
  top: 0px;
  width: 5px;
  background-color: #f0f0f0;
  cursor: col-resize;
  flex-shrink: 0;
  z-index: 3;
  will-change: transform;
}

.drag-bar:hover {
  background-color: #d9d9d9;
}

.unsupported-preview {
  color: #909399;
  text-align: center;
  padding: 50px 0;
}

/* 针对不同类型预览的样式调整 */
.text-preview {
  width: auto;
  max-width: 100%;
  padding: 10px;
  white-space: pre-wrap;
  word-break: break-word;
}

.html-preview {
  width: auto;
  max-width: 100%;
  padding: 10px;
}

.ppt-preview {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.ppt-preview :deep(.pptx-preview-wrapper) {
  height: 100%;
  overflow: auto;
}

.pptx-viewer {
  width: 100%;
  height: 100%;
}



.top_container {
  position: relative;
  display: flex;
  align-items: flex-start;
  height: 100px;
}

.file_title_item {
  position: absolute;
  left: 17px;
  top: 28.3px;
  display: flex;
  flex-direction: column;
}
.file_title_item .file_title_content {
  font-size: 22px;
  font-weight: 400;
  line-height: 1.2;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file_title_item .file_title_description {
  margin-top: 8px;
  color: #7f7f7f;
  font-size: 14px;
  text-align: left;
}

.ask_by_file {
  position: absolute;
  top: 28.3px;
  left: 333.52px;
  width: 30.8px;
  height: 30.8px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.ask_by_file:hover {
  background-color: #eee;
}

.download_file {
  position: absolute;
  top: 28.3px;
  left: 385px;
  width: 30.8px;
  height: 30.8px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.download_file:hover {
  background-color: #eee;
}

.close_pre_dialog {
  position: fixed;
  right: 25px;
  z-index: 9999;
  cursor: pointer;
  width: 30.8px;
  height: 30.8px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: background-color 0.2s ease;
}

.close_pre_dialog:hover {
  background-color: #eee;
}

.icon-img {
  width: 18px;
  height: 17.99px;
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

/* 添加淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 文件提问按钮样式 */
.file-ask-button {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 140px;
  height: 36px;
  border-radius: 18px;
  background: #1b6cff;
  box-shadow: 0 2px 6px #0055ef66;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 1000;
  gap: 2.3px;
}

.ask-icon {
  width: 16px;
  height: 14.86px;
}

.ask-text {
  color: white;
  font-size: 14px;
  white-space: nowrap;
  transform: translateY(-1.5px);
}


</style>
<template>
  <div
    class="main_content"
    :style="{ marginBottom: isDragOver ? '0px' : '10px' }"
    v-if="pageType === 'tran' && !isDragOver"
    ref="messageContainerTran"
  >
    <div class="title" v-if="pageType === 'tran' && !isDragOver">
      <img src="@/assets/logo2.png" class="title_src" />
      <div>
        <div class="title_top" style="line-height: 30px; font-weight: bold">立讯技术AI翻译专家</div>
        <div class="title_top" style="font-size: 16px; font-weight: 400; line-height: 36px">
          熟练掌握翻译技巧～您的翻译好帮手
        </div>
      </div>
    </div>
    <div class="title_tran_tip" v-if="pageType === 'tran' && !isDragOver">
      <div
        v-if="isSupportedFileType(transQuest)"
        :style="{
          padding: transQuest ? '7px 15px' : '0px'
        }"
        @click="showPreFile('tran')"
        style="color: #333; background-color: #eff6ff; display: flex; align-items: center; cursor: pointer"
      >
        <span style="display: flex; align-items: center">
          <img :src="getFileIcon(transQuest)" style="width: 24px; height: 30px" />
        </span>
        <span style="padding-left: 10px">{{ transQuest }}</span>
      </div>
      <div
        v-else
        :style="{
          padding: transQuest ? '10px 15px' : '0px'
        }"
      >
        {{ transQuest }}
      </div>
    </div>
    <div v-if="pageType === 'tran' && !isDragOver && limitTranLoading && !isExportTranslationDocument" class="title_wait">
      <span>
        <img src="@/assets/robot.png" style="width: 36px; height: 36px" />
      </span>
      <span style="padding-left: 10px">翻译中...</span>
      <span v-if="!transData">{{ dots }}</span>
    </div>
    <div
      v-if="pageType === 'tran' && !isDragOver && isExportTranslationDocument"
      class="translation-document-output"
    >
      <div v-if="translationProcessText" class="translation-thinking">
        <div class="left_robot">
          <img src="@/assets/knowledgeBase/robot.png" style="width: 52px; height: 52px;" />
        </div>
        <div class="right_content" style="transform: translateY(15px)">{{ translationProcessText }}</div>
      </div>
      <div v-if="translationFinalMarkdown" class="translation-answer">
        <MarkdownRenderer
          :markdown="translationFinalMarkdown"
          class="normal-text"
          style="font-size: 18px; line-height: 1.6; background-color: #fafafa; color: #333"
        />
      </div>
    </div>
    <MarkdownRenderer
      class="title_tran_data normal-text"
      v-if="pageType === 'tran' && !isDragOver && limitTranLoading && !isExportTranslationDocument"
      :style="{ padding: currentTransData ? '0px 15px' : '0px' }"
      :markdown="currentTransData"
    />
    <MarkdownRenderer
      class="title_tran_data normal-text"
      v-if="pageType === 'tran' && !isDragOver && !limitTranLoading && !isExportTranslationDocument"
      :style="{ padding: transData ? '0px 15px' : '0px' }"
      :markdown="transData"
    />
    <div class="query_common" v-if="pageType === 'tran' && transQuest && !limitTranLoading && !isDragOver">
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
  <div class="select_content" v-if="pageType === 'tran' && !isDragOver">
    <div class="tran_select move_tran_selects" v-if="pageType === 'tran' && !isDragOver">
      <el-radio-group v-model="selectedLanModel">
        <el-radio-button v-for="item in lanList" :key="item" :label="item" :value="item">{{ item }}</el-radio-button>
      </el-radio-group>
      <div class="show_export_excel_switch" style="margin-left: auto">
        <span style="color: #7f7f7f">输出翻译文档(仅支持xlsx文档)</span>
        <el-switch
          :model-value="isExportTranslationDocument"
          style="margin-left: 10px"
          @change="toggleExportTranslationDocument"
        />
      </div>
    </div>
    <div class="textarea" v-if="pageType === 'tran' && !isDragOver">
      <el-input
        v-select-all-on-ctrl-a
        v-model="newQuestionModel"
        placeholder="请输入您翻译的文本,换行请按下Shift+Enter"
        style="width: 100%"
        class="custom-input"
        clearable
        @keydown.enter.prevent="tranPost"
        @keyup.shift.enter.prevent="handleShiftEnter('textareaInputTran', $event)"
        ref="textareaInputRef"
        type="textarea"
        :maxlength="4096"
        :rows="dynamicRowFinal"
      />
      <div class="send-icon">
        <div class="tooltip-wrapper" ref="wrapperRef">
          <img src="@/assets/file.png" class="arrow" @click="showFile('tran')" style="margin-right: 10px" />
          <FileMenu
            :showFileMenu="showFileMenu"
            :handleFileSelect="handleFileSelect"
            localType="tran"
            knowledgeType="tran"
            :currentAgentType="currentAgentType"
            :disableKnowledgeOption="isExportTranslationDocument"
          />
        </div>
        <div class="tooltip-wrapper term-library-wrapper" ref="termLibraryWrapperRef">
          <button
            type="button"
            class="term-library-button"
            :class="{ checked: isTermLibraryChecked }"
            @click.stop="toggleTermLibraryTooltip"
          >
            <img :src="termLibraryIcon" alt="" class="term-library-icon" />
            <span>术语库</span>
          </button>
          <TermLibraryTooltip
            :show="showTermLibraryTooltip"
            :checked="isTermLibraryChecked"
            menu-id="personal-term-library"
            @update-selected="handleTermLibrarySelected"
            @more-click="handleTermLibraryMoreClick"
          />
        </div>
        <img
          :src="limitTranLoading ? imageC : newQuestionModel ? imageB : imageA"
          class="arrow"
          @click="submitTranSend"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import FileMenu from '../options/fileMenu.vue'
import MarkdownRenderer from '../markdown.vue'
import imageB from '@/assets/arrow_blue.png'
import imageA from '@/assets/arrow_gray.png'
import imageC from '@/assets/stop.png'
import glossaryActiveIcon from '@/assets/knowledgeBase/glossary_active.svg'
import glossaryNormalIcon from '@/assets/knowledgeBase/glossary_normal.svg'
import TermLibraryTooltip from './TermLibraryTooltip.vue'

const selectedLanModel = defineModel('selectedLan')
const newQuestionModel = defineModel('newQuestion')

const props = defineProps({
  pageType: { type: String, default: '' },
  isDragOver: { type: Boolean, default: false },
  transQuest: { type: String, default: '' },
  limitTranLoading: { type: Boolean, default: false },
  currentTransData: { type: String, default: '' },
  transData: { type: String, default: '' },
  translationProcess: { type: String, default: '' },
  translationFinal: { type: String, default: '' },
  dots: { type: String, default: '' },
  isSupportedFileType: { type: Function, required: true },
  getFileIcon: { type: Function, required: true },
  showPreFile: { type: Function, required: true },
  refreshData: { type: Function, required: true },
  upCommon: { type: Function, required: true },
  downCommon: { type: Function, required: true },
  lanList: { type: Array, default: () => [] },
  tranPost: { type: Function, required: true },
  handleShiftEnter: { type: Function, required: true },
  dynamicRowFinal: { type: [Number, String, Object], required: true },
  showFile: { type: Function, required: true },
  showFileMenu: { type: Boolean, default: false },
  handleFileSelect: { type: Function, required: true },
  currentAgentType: { type: [String, Number, Object], default: '' },
  submitTranSend: { type: Function, required: true },
  openTermLibrary: { type: Function, default: null },
  isExportTranslationDocument: { type: Boolean, default: false },
  toggleExportTranslationDocument: { type: Function, required: true }
})

const messageContainerTran = ref(null)
const textareaInputRef = ref(null)
const wrapperRef = ref(null)
const termLibraryWrapperRef = ref(null)
const showTermLibraryTooltip = ref(false)
const isTermLibraryChecked = ref(true)

const translationProcessText = computed(() => {
  const content = props.translationProcess ?? ''
  if (typeof content !== 'string') {
    return ''
  }
  return content.trim()
})
const translationFinalMarkdown = computed(() => {
  const content = props.translationFinal
  if (typeof content !== 'string') {
    return content ? String(content) : ''
  }
  const trimmed = content.trim()
  if (!trimmed) {
    return ''
  }
  if (/^https?:\/\/\S+$/i.test(trimmed)) {
    return `[点击下载翻译后的 Excel 文件](${trimmed})`
  }
  return content
})

const termLibraryIcon = computed(() =>
  isTermLibraryChecked.value ? glossaryActiveIcon : glossaryNormalIcon
)

const toggleTermLibraryTooltip = () => {
  showTermLibraryTooltip.value = !showTermLibraryTooltip.value
}

const closeTermLibraryTooltip = () => {
  showTermLibraryTooltip.value = false
}

const handleDocumentClick = event => {
  if (!showTermLibraryTooltip.value) {
    return
  }

  const wrapper = termLibraryWrapperRef.value
  if (wrapper && !wrapper.contains(event.target)) {
    closeTermLibraryTooltip()
  }
}

const handleTermLibrarySelected = (_menuId, isSelected) => {
  isTermLibraryChecked.value = isSelected
}

const handleTermLibraryMoreClick = () => {
  closeTermLibraryTooltip()
}

const ensureValidSelection = () => {
  const options = Array.isArray(props.lanList) ? props.lanList : []
  if (!options.length) {
    return
  }

  if (!options.includes(selectedLanModel.value)) {
    selectedLanModel.value = options[0]
  }
}

onMounted(() => {
  ensureValidSelection()
  document.addEventListener('click', handleDocumentClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
})

watch(
  () => props.lanList,
  () => {
    ensureValidSelection()
  },
  { immediate: true }
)

watch(selectedLanModel, value => {
  if (!value) {
    ensureValidSelection()
  }
})

const scrollToBottom = () => {
  const container = messageContainerTran.value
  if (!container) {
    return
  }

  const distanceToBottom = container.scrollHeight - container.scrollTop - container.clientHeight
  if (props.limitTranLoading || distanceToBottom <= 20) {
    container.scrollTop = container.scrollHeight
  }
}

watch(
  () => [
    props.currentTransData,
    props.transData,
    props.limitTranLoading,
    translationProcessText.value,
    translationFinalMarkdown.value
  ],
  async () => {
    await nextTick()
    scrollToBottom()
  }
)

defineExpose({
  messageContainerTran,
  textareaInputRef,
  wrapperRef,
  termLibraryWrapperRef
})
</script>

<style scoped lang="less">
.custom-input {
  :deep(.el-textarea__inner) {
    height: 150px !important;
    min-height: 150px !important;
    max-height: 150px !important;
    border-color: #1b6cff !important;
    overflow-y: auto !important;
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
.move_tran_selects {
  transform: translateX(-10px);
}
.tran_select {
  width: 862px;
  margin: 0 auto 10px auto;
  display: flex;
  justify-content: flex-start;
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

.translation-document-output {
  padding: 0 15px;
}

.translation-thinking {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  color: #333;
  font-size: 16px;
  line-height: 1.6;
  margin-bottom: 12px;
}

.translation-thinking .left_robot {
  flex: 0 0 auto;
}

.translation-thinking .right_content {
  flex: 1;
  word-break: break-word;
  white-space: pre-wrap;
}

.translation-answer {
  border-radius: 16px;
  background: #fafafa;
  padding: 20px;
}

.send-icon {
  align-items: center;
}

.term-library-wrapper {
  align-items: center;
}

.term-library-button {
  width: 88px;
  height: 40px;
  border-radius: 10px;
  background: transparent;
  border: none;
  margin-right: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 16px;
  color: #4C4C4C;
  transition: background-color 0.2s ease;
}

.term-library-button.checked {
  color: #1B6CFF;
}

.term-library-button:hover {
  background: #eee;
}

.term-library-button.checked:hover {
  background: #e8f2ff;
}

.term-library-icon {
  width: 18px !important;
  height: 18px !important;
  transform: translateY(1.8px);
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

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

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

:deep(.el-radio-button.is-active .el-radio-button__original-radio:not(:disabled) + .el-radio-button__inner) {
  color: #1677ff !important;
  background-color: #e6f4ff !important;
  border: none !important;
  outline: none !important;
  box-shadow: none !important;
}

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

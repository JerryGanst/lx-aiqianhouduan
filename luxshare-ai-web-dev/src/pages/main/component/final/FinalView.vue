<template>
  <div
    class="main_content"
    :style="{ marginBottom: isDragOver ? '0px' : '10px' }"
    v-if="pageType === 'final' && !isDragOver"
    ref="messageContainerTran"
  >
    <div class="title" v-if="pageType === 'final' && !isDragOver">
      <img src="@/assets/logo2.png" class="title_src" />
      <div>
        <div class="title_top" style="line-height: 30px; font-weight: bold">立讯技术AI智能总结</div>
        <div class="title_top" style="font-size: 16px; font-weight: 400; line-height: 36px">
          精准概括，助您快速理解长文本
        </div>
      </div>
    </div>

    <div class="title_final_tip" v-if="pageType === 'final' && !isDragOver">
      <div
        v-if="isSupportedFileType(finalQuest)"
        @click="showPreFile('final')"
        :style="{
          padding: finalQuest ? '7px 15px' : '0px'
        }"
        style="
          color: #333;
          background-color: #eff6ff;
          display: flex;
          align-items: center;
          border-radius: 10px;
          cursor: pointer;
        "
      >
        <span style="display: flex; align-items: center">
          <img :src="getFileIcon(finalQuest)" style="width: 24px; height: 30px" />
        </span>
        <span style="padding-left: 10px">{{ finalQuest }}</span>
      </div>
      <div v-else class="title_final_query" :style="{ padding: finalQuest ? '10px 15px' : '0px' }">
        <div>{{ finalQuest }}</div>
      </div>
    </div>
    <div v-if="pageType === 'final' && docIng && !isDragOver" class="title_wait">
      <span>
        <img src="@/assets/robot.png" style="width: 36px; height: 36px" />
      </span>
      <span style="padding-left: 10px">正在为您总结,请稍等</span>
      <span v-if="!finalData.title">{{ dots }}</span>
    </div>
    <div
      class="title_final_data"
      v-if="pageType === 'final' && !isDragOver"
      :style="{ padding: finalData.title ? '15px 15px' : '0px' }"
    >
      <div v-if="finalData.title">
        <span>概括 :</span>
        <span>{{ finalData.title }}</span>
      </div>
      <div v-if="finalData?.data?.length > 0" style="margin-top: 15px">
        <div>关键词 :</div>
        <div v-for="items in finalData?.data" :key="items">
          {{ items }}
        </div>
      </div>
    </div>
    <div class="query_common" v-if="pageType === 'final' && finalQuest && !docIng && !isDragOver">
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
  <div class="select_content" v-if="pageType === 'final' && !isDragOver">
    <div class="textarea" v-if="pageType === 'final' && !isDragOver">
      <el-input
        v-select-all-on-ctrl-a
        v-model="newQuestionModel"
        placeholder="请输入您想总结的文本,换行请按下Shift+Enter"
        style="width: 100%"
        class="custom-input"
        clearable
        @keydown.enter.prevent="finalPost"
        @keyup.shift.enter.prevent="handleShiftEnter('textareaInputFinal', $event)"
        ref="textareaInputRef"
        type="textarea"
        :maxlength="4096"
        :rows="dynamicRowFinal"
      />
      <div class="send-icon">
        <div class="tooltip-wrapper" ref="wrapperRef">
          <img src="@/assets/file.png" class="arrow" @click="showFile('final')" style="margin-right: 10px" />
          <FileMenu
            :showFileMenu="showFileMenu"
            :handleFileSelect="handleFileSelect"
            localType="final"
            knowledgeType="final"
            :currentAgentType="currentAgentType"
          />
        </div>
        <img
          :src="
            finalIng && (currentIndex || currentIndex === 0) && currentIndex === activeIndex
              ? imageC
              : newQuestionModel
                ? imageB
                : imageA
          "
          class="arrow"
          @click="submitFinalSend"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import FileMenu from '../options/fileMenu.vue'
import imageB from '@/assets/arrow_blue.png'
import imageA from '@/assets/arrow_gray.png'
import imageC from '@/assets/stop.png'

const newQuestionModel = defineModel('newQuestion')

const props = defineProps({
  pageType: { type: String, default: '' },
  isDragOver: { type: Boolean, default: false },
  isSupportedFileType: { type: Function, required: true },
  finalQuest: { type: String, default: '' },
  getFileIcon: { type: Function, required: true },
  showPreFile: { type: Function, required: true },
  docIng: { type: Boolean, default: false },
  dots: { type: String, default: '' },
  finalData: { type: Object, default: () => ({}) },
  refreshData: { type: Function, required: true },
  upCommon: { type: Function, required: true },
  downCommon: { type: Function, required: true },
  dynamicRowFinal: { type: [Number, String, Object], required: true },
  handleShiftEnter: { type: Function, required: true },
  showFile: { type: Function, required: true },
  showFileMenu: { type: Boolean, default: false },
  handleFileSelect: { type: Function, required: true },
  currentAgentType: { type: [String, Number, Object], default: '' },
  submitFinalSend: { type: Function, required: true },
  finalPost: { type: Function, required: true },
  finalIng: { type: Boolean, default: false },
  currentIndex: { type: [Number, String], default: null },
  activeIndex: { type: [Number, String], default: null }
})

const messageContainerTran = ref(null)
const textareaInputRef = ref(null)
const wrapperRef = ref(null)

defineExpose({
  messageContainerTran,
  textareaInputRef,
  wrapperRef
})
</script>

<style scoped lang="less">
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
</style>

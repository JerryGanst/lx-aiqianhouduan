<template>
  <el-container class="container">
    <!-- 左侧栏 -->
    <AsizeComponent
      @change-history="fetchChatList"
      @set-isLaw="setlaw"
      @set-message="setMessage"
      @set-FileModel="setFileModel"
      @fetch-directory-detail="fetchDirectoryDetail"
      @clear-chat-history="clearChatHistory"
      @set-Net="setNet"
      ref="asizeRef"
    ></AsizeComponent>

    <!-- 右侧内容 -->
    <el-container>
      <el-main>
        <div
          v-if="isMobile && isCollapsed && (contentType !== ContentType.AGENT || isAgentDetail)"
          class="mobile-menu-icon"
          :style="{ left: menuIconLeft }"
          @click.stop="toggleCollapse"
        >
          <img :src="menuIcon" style="width: 44px; height: 44px;" />
        </div>
        <div
          v-if="isMobile && (contentType !== ContentType.AGENT || isAgentDetail) && !isCollapsed"
          class="mobile-overlay"
          @click="toggleCollapse"
        ></div>
        <div
          v-if="contentType === ContentType.CONVERSATION"
          style="width: 100%; height: 100vh"
          @dragover.prevent="handleDragOver"
          @dragleave="handleDragLeave"
          @drop.prevent="handleDrop"
          :class="{ 'drag-over': isDragOver }"
        >
          <div v-if="!currentQuestion" class="center-container" :style="{ paddingTop: isDragOver ? '0px' : '80px' }">
            <Entry
              @submit-tran="submitTran"
              @submit-final="submitFinal"
              @submit-question="submitQuestion"
              @cancel-currentRequest="cancelCurrentRequest(val)"
              @submit-sample-title="submitSampleTitle"
              @sample-post="samplePost"
              @summit-post="summitPost"
              @submit-tranSend="submitTranSend"
              @submit-finalSend="submitFinalSend"
              @up-common="upCommon"
              @down-common="downCommon"
              @refresh-data="refreshData"
              @submit-questionSend="submitQuestionSend"
              @submit-itSend="submitITSend"
              @submit-lawSend="submitLawSend"
              @submit-sampleSend="submitSampleSend"
              @fetch-chat-list="fetchChatList"
              ref="entryRef"
            ></Entry>
          </div>

          <div v-else class="center-container" style="padding-top: 0px">
            <DragUpload v-if="isDragOver" ref="dragUploads"></DragUpload>
            <template v-else>
              <div class="main_content" style="width: 862px" ref="chatRoot">
                <template v-if="['query', 'it', 'law', 'board'].includes(pageType)">
                  <div class="title_tiQuery">
                    <!--最新的对话提问内容-->
                    <div class="title_tiQuery_text" :style="{ padding: tipQuery ? '13px 15px' : '0px' }">
                      {{ tipQuery }}
                    </div>
                  </div>
                  <div class="title_float">
                    <template v-if="limitQueryLoading">
                      <span><img src="@/assets/robot.png" style="width: 36px; height: 36px" /></span>
                      <!--头里面流式在刷的消息-->
                      <span style="padding-left: 10px">{{ currentObj.thinking }}</span>
                    </template>
                  </div>
                  <template v-if="currentObj.messages?.type === 'final_answer' && !limitQueryLoading">
                    <div class="title_float" :style="{ paddingTop: currentObj.list?.content ? '10px' : '0px' }">
                      <!-- 思考完成后上方刷的消息 -->
                      <span>
                        {{ currentObj.thinking }}
                      </span>
                    </div>
                    <MarkdownRenderer :markdown="currentObj.messages?.content ?? ''" class="normal-text" />
                    <!--文件列表-->
                    <template v-if="currentObj.messages.sources">
                      <div class="query_source">附件</div>
                      <a class="href_source" v-for="(it, index) in processedData()" @click="toDoc(it)">
                        {{ it.document_title }}(第{{ it.page.join('/') }}页)
                      </a>
                    </template>
                    <!-- 刷新 点赞 点踩 -->
                    <div class="query_common">
                      <div>
                        <img
                          src="@/assets/refresh.png"
                          style="margin-left: 10px"
                          class="query_common_img"
                          @click="refreshData"
                        />
                      </div>
                      <div>
                        <img
                          src="@/assets/up.png"
                          @click="upCommon"
                          class="query_common_img"
                          style="margin-left: 15px"
                        />
                      </div>
                      <div>
                        <img
                          src="@/assets/down.png"
                          style="margin-left: 15px"
                          @click="downCommon"
                          class="query_common_img"
                        />
                      </div>
                    </div>
                  </template>
                </template>

                <div class="sample_item" ref="messageContainer">
                  <template v-if="pageType === 'sample'">
                    <div
                      class="sample_chat"
                      v-if="chatQuery.messages.length > 0 && !limitLoading"
                      v-for="(item, index) in chatQuery.messages"
                    >
                      <!-- 偶数 用户发的消息 这里发的消息为文件类型-->
                      <div
                        v-if="index % 2 === 0 && item.files && item.files.length > 0"
                        class="sample_chat_file"
                        :style="{ marginTop: index === 0 ? '68px' : '40px' }"
                      >
                        <div v-for="its in item.files" class="item_files" @click="showListFile(its)">
                          <span style="display: flex; align-items: center">
                            <img
                              :src="getFileImgByOriginFile(its)"
                              style="width: 24px; height: 30px"
                            />
                          </span>
                          <span style="padding-left: 10px" class="file_name">{{ its.originalFileName }}</span>
                        </div>
                      </div>
                      <!-- 偶数 用户发的消息为文本类型-->
                      <div
                        v-if="index % 2 === 0"
                        class="sample_chat_query"
                        :style="{
                          marginTop: item.content
                            ? item.files && item.files.length > 0
                              ? '10px'
                              : index === 0
                                ? '70px'
                                : '40px'
                            : '0px',
                          padding: item.content ? '13px 15px' : '0px'
                        }"
                      >
                        {{ item.content }}
                      </div>
                      <div v-if="index % 2 !== 0" class="stream-response">
                        <MarkdownRenderer
                          :markdown="item.thinking ?? ''"
                          class="normal-text"
                          style="
                            font-size: 13px;
                            line-height: 24px;
                            padding: 0px 10px;
                            background-color: transparent;
                            color: #666;
                          "
                        />
                        <MarkdownRenderer
                          :markdown="item.before ?? ''"
                          class="normal-text"
                          style="
                            font-size: 13px;
                            line-height: 24px;
                            padding: 0px 10px;
                            background-color: transparent;
                            color: #666;
                          "
                        />
                        <!-- 后半部分 -->
                        <MarkdownRenderer :markdown="item.after ?? ''" class="normal-text" />
                      </div>
                      <!-- isNewData恒为true以下应该是废弃代码-->
                      <!--                      <MarkdownRenderer v-if="index % 2 !== 0 && !item.isNewData" :markdown="item.content" />-->
                      <!--文件列表-->
                      <template v-if="item.sources && item.sources.length > 0">
                        <div class="query_source">附件</div>
                        <div v-for="it in processedData(item.sources)" style="display: block">
                          <a class="href_source" @click="toDoc(it)">
                            {{ it.document_title }}(第{{ it.page.join('/') }}页)
                          </a>
                        </div>
                      </template>
                    </div>
                    <div
                      class="sample_chat"
                      v-if="limitLoading && chatCurrent.messages.length > 0"
                      v-for="(item, index) in chatCurrent.messages"
                    >
                      <div
                        v-if="index % 2 === 0 && item.files && item.files.length > 0"
                        class="sample_chat_file"
                        :style="{ marginTop: index === 0 ? '70px' : '40px' }"
                      >
                        <div v-for="its in item.files" class="item_files" @click="showListFile(its)">
                          <span style="display: flex; align-items: center">
                            <img
                              :src="getFileImgByOriginFile(its)"
                              style="width: 24px; height: 30px"
                            />
                          </span>
                          <span style="padding-left: 10px" class="file_name">{{ its.originalFileName }}</span>
                        </div>
                      </div>
                      <div
                        v-if="index % 2 === 0"
                        class="sample_chat_query"
                        :style="{
                          marginTop: item.content
                            ? item.files && item.files.length > 0
                              ? '10px'
                              : index === 0
                                ? '70px'
                                : '40px'
                            : '0px',
                          padding: item.content ? '13px 15px' : '0px'
                        }"
                      >
                        {{ item.content }}
                      </div>
                      <div class="tip_load" v-if="index === chatCurrent.messages.length - 1">
                        <span><img src="@/assets/robot.png" style="width: 36px; height: 36px" /></span>
                        <span style="padding-left: 10px">正在为您解答,请稍等</span>
                        <span>{{ dots }}</span>
                      </div>
                      <div v-if="index % 2 !== 0" class="stream-response">
                        <MarkdownRenderer
                          :markdown="item.thinking ?? ''"
                          class="normal-text"
                          style="
                            font-size: 13px;
                            line-height: 24px;
                            padding: 0px 10px;
                            background-color: transparent;
                            color: #666;
                          "
                        />
                        <MarkdownRenderer
                          :markdown="item.before ?? ''"
                          class="normal-text"
                          style="
                            font-size: 13px;
                            line-height: 24px;
                            padding: 0px 10px;
                            background-color: transparent;
                            color: #666;
                          "
                        />
                        <!-- 后半部分 -->
                        <MarkdownRenderer :markdown="item.after ?? ''" class="normal-text" />
                      </div>
                      <!--文件列表-->
                      <template v-if="item.sources && item.sources.length > 0">
                        <div v-for="it in processedData(item.sources)" style="display: block">
                          <a class="href_source" @click="toDoc(it)">
                            {{ it.document_title }}(第{{ it.page.join('/') }}页)
                          </a>
                        </div>
                      </template>
                    </div>
                  </template>
                </div>

                <!-- 刷新 点赞 点踩-->
                <div
                  class="query_common"
                  v-if="pageType === 'sample' && !limitLoading && chatQuery.messages.length > 0"
                >
                  <div>
                    <img
                      src="@/assets/refresh.png"
                      style="margin-left: 10px"
                      class="query_common_img"
                      @click="refreshData"
                    />
                  </div>
                  <div>
                    <img src="@/assets/up.png" @click="upCommon" class="query_common_img" style="margin-left: 15px" />
                  </div>
                  <div>
                    <img
                      src="@/assets/down.png"
                      style="margin-left: 15px"
                      @click="downCommon"
                      class="query_common_img"
                    />
                  </div>
                </div>
              </div>
              <div class="query_content">
                <div class="tran_select" v-if="['query', 'sample', 'it', 'law', 'board'].includes(pageType)">
                  <el-radio-group v-model="selectedMode" @change="changeMode" :disabled="isSampleLoad" class="chat-type-group">
                    <el-radio-button label="通用模式" value="通用模式">通用模式</el-radio-button>
                    <el-radio-button label="人资行政专题" value="人资行政专题">人资行政专题</el-radio-button>
                    <el-radio-button label="IT专题" value="IT专题">IT专题</el-radio-button>
                    <el-tooltip
                      content="该模式仅支持通过office网络访问"
                      placement="top"
                      v-if="isLaw === 'true' && !isNet"
                    >
                      <el-radio-button label="法务专题" value="法务专题" disabled>法务专题</el-radio-button>
                    </el-tooltip>
                    <el-radio-button label="法务专题" value="法务专题" v-if="isLaw === 'true' && isNet">
                      法务专题
                    </el-radio-button>
<!--                    <el-tooltip-->
<!--                      content="该模式仅支持通过office网络访问"-->
<!--                      placement="top"-->
<!--                      v-if="enableBoardOffice === 'true' && !isNet"-->
<!--                    >-->
<!--                      <el-radio-button label="董办专题" value="董办专题" disabled>董办领域</el-radio-button>-->
<!--                    </el-tooltip>-->
<!--                    <el-radio-button label="董办专题" value="董办专题" v-if="enableBoardOffice === 'true' && isNet">-->
<!--                      董办领域-->
<!--                    </el-radio-button>-->
                  </el-radio-group>
                </div>
                <div class="textarea" v-if="['query', 'it', 'law', 'board'].includes(pageType)">
                  <el-input
                    v-model="newQuestion"
                    placeholder="请输入您的问题,换行请按下Shift+Enter"
                    class="custom-input"
                    style="width: 100%"
                    @keydown.enter.prevent="summitPost"
                    @keyup.shift.enter.prevent="handleShiftEnter('textareaInputQuery', $event)"
                    type="textarea"
                    :maxlength="4096"
                    ref="textareaInputQuery"
                    :rows="dynamicRows"
                  />
                  <!-- 发送图标 -->
                  <div class="send-icon">
                    <div
                      class="tooltip-wrapper"
                      @mouseenter="showModelTip = true"
                      @mouseleave="showModelTip = false"
                      v-if="['query', 'it', 'board'].includes(pageType)"
                    >
                      <img
                        v-if="deepType"
                        :src="deepSelect"
                        class="arrow"
                        @click="checkDeepType"
                        :style="{
                          'margin-right': '10px',
                          'background-color': showModelTip ? '#E8F2FF' : 'transparent',
                          'border-radius': '10px'
                        }"
                      />
                      <img
                        v-else
                        :src="deep"
                        class="arrow"
                        @click="checkDeepType"
                        :style="{
                          'margin-right': '10px',
                          'background-color': showModelTip ? '#EEEEEE' : 'transparent',
                          'border-radius': '10px'
                        }"
                      />

                      <transition name="fade">
                        <div v-if="showModelTip" class="tooltip">
                          {{ !deepType ? '切换成深度思考模式' : '切换成普通模式' }}
                        </div>
                      </transition>
                    </div>
                    <img
                      :src="isSampleLoad ? imageC : (newQuestion ? imageB : imageA)"
                      v-if="pageType === 'query'"
                      class="arrow"
                      @click="submitQuestionSend"
                    />
                    <img
                      :src="isSampleLoad ? imageC : (newQuestion ? imageB : imageA)"
                      v-if="pageType === 'it'"
                      class="arrow"
                      @click="submitITSend"
                    />
                    <img
                      :src="isSampleLoad ? imageC : (newQuestion ? imageB : imageA)"
                      class="arrow"
                      v-if="isNet && ['law', 'board'].includes(pageType)"
                      @click="submitLawSend"
                    />
                  </div>
                </div>
                <div
                  class="textarea"
                  :class="[fileInputAry && fileInputAry.length > 0 ? 'sampleAreaAry' : 'sampleArea']"
                  v-if="pageType === 'sample'"
                >
                  <el-input
                    v-model="newQuestion"
                    placeholder="请输入您的问题,换行请按下Shift+Enter"
                    style="width: 100%"
                    class="custom-input"
                    clearable
                    @keydown.enter.prevent="samplePost"
                    @keyup.shift.enter.prevent="handleShiftEnter('textareaInputSample', $event)"
                    ref="textareaInputSample"
                    :maxlength="4096"
                    type="textarea"
                    :rows="dynamicRows"
                  />
                  <div class="filesList" v-if="fileInputAry && fileInputAry.length > 0">
                    <div
                      v-for="(item, index) in fileInputAry"
                      style="cursor: pointer"
                      :style="{ marginLeft: index === 0 ? '5px' : '10px' }"
                      @click="showListFile(item)"
                    >
                      <span style="display: flex; align-items: center">
                        <img
                          :src="getFileImgByOriginFile(item)"
                          style="width: 22px; height: 28px"
                        />
                      </span>
                      <span
                        style="padding-left: 10px; width: 50px; overflow: hidden; padding-top: 8px"
                        class="file_name"
                      >
                        {{ item.originalFileName }}
                      </span>
                      <span
                        style="
                          position: absolute;
                          width: 16px;
                          height: 16px;
                          right: 0px;
                          top: 0px;
                          cursor: pointer;
                          display: flex;
                          justify-content: center;
                          align-items: center;
                        "
                        @click.stop="deleteImg(index)"
                      >
                        <img src="@/assets/close.png" style="width: 10px; height: 10px" />
                      </span>
                    </div>
                  </div>
                  <!-- 发送图标 -->
                  <div class="send-icon">
                    <div class="tooltip-wrapper" ref="wrapperRef">
                      <img
                        src="@/assets/file.png"
                        class="arrow"
                        @click="showFileSample('sample')"
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
                      <img
                        v-if="deepType"
                        :src="deepSelect"
                        class="arrow"
                        @click="checkDeepType"
                        :style="{
                          'margin-right': '10px',
                          'background-color': showModelTip ? '#E8F2FF' : 'transparent',
                          'border-radius': '10px'
                        }"
                      />
                      <img
                        v-else
                        :src="deep"
                        class="arrow"
                        @click="checkDeepType"
                        :style="{
                          'margin-right': '10px',
                          'background-color': showModelTip ? '#EEEEEE' : 'transparent',
                          'border-radius': '10px'
                        }"
                      />
                      <transition name="fade">
                        <div v-if="showModelTip" class="tooltip">
                          {{ !deepType ? '切换成深度思考模式' : '切换成普通模式' }}
                        </div>
                      </transition>
                    </div>
                    <div
                      class="tooltip-wrapper"
                      @mouseenter="showKnowledgeTip = true"
                      @mouseleave="showKnowledgeTip = false"
                    >
                      <img
                        :src="useKnowledge ? activeKnowledgeHover : inactiveKnowledgeHover"
                        class="arrow"
                        @click="toggleKnowledge"
                        style="margin-right: 10px"
                        v-if="showKnowledgeTip"
                      />
                      <img
                        :src="useKnowledge ? activeKnowledge : inactiveKnowledge"
                        class="arrow"
                        @click="toggleKnowledge"
                        style="margin-right: 10px"
                        v-else
                      />

                      <transition name="fade">
                        <div v-if="showKnowledgeTip" class="tooltip">
                          {{ !useKnowledge ? '引用个人知识库文件问答' : '不使用个人知识库文件' }}
                        </div>
                      </transition>
                    </div>
                    <img
                      :src="isSampleLoad ? imageC : (newQuestion || fileInputAry.length > 0 ? imageB : imageA)"
                      class="arrow"
                      @click="submitSampleSend"
                    />
                  </div>
                </div>
              </div>
            </template>
          </div>
          <FileUpload ref="fileRefs"></FileUpload>
          <commonUploadModal ref="commonUploadModals"></commonUploadModal>
          <FilePreUpload ref="filePreRef"></FilePreUpload>
        </div>
        <div v-if="contentType === ContentType.KNOWLEDGE" style="width: 100%; height: 100vh">
          <personModal ref="personModalRef" v-if="fileModal === FileModel.PERSONAL" :folder-id="folderId" :folder-name="folderName"></personModal>
          <departmentModal
            ref="departmentModalRef"
            v-if="fileModal === FileModel.DEPARTMENT"
            :folder-id="folderId"
            :folder-name="folderName"
            :department-id="departmentId"
          ></departmentModal>
          <commonModal ref="commonLedge" v-if="fileModal === FileModel.PUBLIC"></commonModal>
          <EmptyFolder v-if="fileModal === FileModel.EMPTY" @create-folder="openCreateFolder" />
        </div>
        <div v-if="contentType === ContentType.AGENT">
          <agent v-if="!isAgentDetail"></agent>
          <create-intel v-else></create-intel>
        </div>
      </el-main>
    </el-container>
  </el-container>
  <!-- 弹窗 -->

  <el-dialog
    v-model="commonVisible"
    title="评价"
    width="500px"
    :before-close="handleCommonClose"
    style="border-radius: 10px"
  >
    <el-input
      v-model="commonQuestion"
      placeholder="请输入您的宝贵建议"
      :maxlength="4096"
      style="width: 100%"
      clearable
      type="textarea"
      rows="5"
    />
    <div class="button-item_common">
      <el-button @click="commonVisible = false" style="width: 100px; height: 40px; margin-left: 15px">取消</el-button>
      <el-button type="primary" @click="submitCommon" style="width: 100px; height: 40px">提交</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
// 10.180.248.140
import AsizeComponent from './component/asize.vue'
import Entry from './component/entry.vue'
import FileUpload from './component/fileUploadModal.vue'
import FilePreUpload from './component/filePreModal.vue'
import commonUploadModal from './component/commonUploadModal.vue'
import commonModal from './component/commonModal.vue'
import personModal from './component/personModal.vue'
import departmentModal from './component/departmentModal.vue'
import EmptyFolder from './component/EmptyFolder.vue'
import DragUpload from './component/dragUpload.vue'
import createIntel from './component/createIntel.vue'
import { useShared } from '@/utils/useShared'
import eventBus from '@/utils/eventBus'
import { ElButton, ElMessage } from 'element-plus' // 引入 ElMessage
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch, toRaw, onBeforeUnmount } from 'vue'
import imageB from '@/assets/arrow_blue.png'
import imageA from '@/assets/arrow_gray.png'
import imageC from '@/assets/stop.png'
import deep from '@/assets/deep.png'
import deepSelect from '@/assets/deepSelect.png'
import request from '@/utils/request' // 导入封装的 axios 方法
import MarkdownRenderer from './component/markdown.vue'
import Agent from './component/agent/agent.vue'
import { ContentType, FileModel, delay, getFileImgByOriginFile, isCompleteJSON } from '@/utils/common.js'
import activeKnowledge from '@/assets/active_knowledge.png'
import inactiveKnowledge from '@/assets/inactive_knowledge.png'
import activeKnowledgeHover from '@/assets/active_knowledge_hover.png'
import inactiveKnowledgeHover from '@/assets/inactive_knowledge_hover.png'
import { getChatMsgList } from '@/api/chat/actions.js' // 引入 Markdown 渲染组件
import FileMenu from './component/options/fileMenu.vue'
import { FILE_LIST_LIMIT } from '@/utils/constants.js'
import menuIcon from '@/assets/travel.svg'

// 静态导入图片

// 变量区域
const {
  currentQuestion,
  newQuestion,
  isSampleStop,
  isQueryStop,
  limitLoading,
  currentId,
  pageType,
  selectedMode,
  currentObj,
  tipQuery,
  streamingQuestion,
  userInfo,
  activeIndex,
  chatQuery,
  isLogin,
  dynamicRows,
  limitSample,
  isSampleLoad,
  handleShiftEnter,
  textareaInputQuery,
  textareaInputSample,
  finalIng,
  drayAry,
  selectedLan,
  changeMode,
  transData,
  currentTransData,
  translationDocumentProcess,
  translationDocumentFinal,
  useTranslationDocument,
  transQuest,
  finalQuest,
  finalData,
  chatCurrent,
  limitId,
  limitTranId,
  limitQueryId,
  topicPreSaveId,
  checkData,
  dots,
  messageContainer,
  deepType,
  checkDeepType,
  docIng,
  tranIng,
  showModelTip,
  fileAry,
  fileInputAry,
  isLaw,
  enableBoardOffice,
  contentType,
  dragUploads,
  isDragOver,
  isNet,
  currentIndex,
  currentAgentType,
  limitFile,
  limitFinalFile,
  messageContainerTran,
  limitTranLoading,
  limitQueryLoading,
  isTranStop,
  isAgentDetail,
  toggleKnowledge,
  useKnowledge,
  knowSelect,
  chatList,
  transFile,
  finalFile,
  fileObj,
  limitFinalId
} = useShared()

const queryIng = ref(false)
const asizeRef = ref(null)
const entryRef = ref(null)
const currentSearchKeyword = ref('')

const commonLedge = ref(null)
const commonQuestion = ref('')
// 当前显示的消息内容
const filePreRef = ref(null)
const commonVisible = ref(false)
// 当前正在显示的消息索引
const currentMessageIndex = ref(0)
const isDisabled = ref(false)
const limitQuery = ref('')
const currentRequestUrl = ref('')
const fileRefs = ref(null)
const commonUploadModals = ref(null)
let interval
const wrapperRef = ref(null)
const showKnowledgeTip = ref(false)

// 引用链接悬浮提示
const chatRoot = ref(null)
let citeTooltipEl = null
const ensureCiteTooltip = () => {
  if (citeTooltipEl) return citeTooltipEl
  citeTooltipEl = document.createElement('div')
  const s = citeTooltipEl.style
  s.position = 'fixed'
  s.zIndex = '10000'
  s.height = '44px'
  s.lineHeight = '44px'
  s.borderRadius = '8px'
  s.background = '#fff'
  s.border = '1px solid #d3d3d3'
  s.boxShadow = '0 4px 8px #b7b8b9'
  s.padding = '0 12px'
  s.whiteSpace = 'nowrap'
  s.pointerEvents = 'none'
  s.display = 'none'
  document.body.appendChild(citeTooltipEl)
  return citeTooltipEl
}
const showCiteTooltip = anchor => {
  const tip = ensureCiteTooltip()
  tip.textContent = anchor?.dataset?.full || ''
  tip.style.display = 'inline-block'
  const aRect = anchor.getBoundingClientRect()
  const tRect = tip.getBoundingClientRect()
  let left = aRect.left + aRect.width / 2 - tRect.width / 2
  const margin = 8
  left = Math.max(margin, Math.min(left, window.innerWidth - tRect.width - margin))
  const top = aRect.bottom + 6
  tip.style.left = left + 'px'
  tip.style.top = top + 'px'
}
const hideCiteTooltip = () => {
  if (citeTooltipEl) citeTooltipEl.style.display = 'none'
}
const handleMouseOver = e => {
  const target = e.target
  if (!(target instanceof Element)) return
  const a = target.closest('a[data-full]')
  if (a && chatRoot.value && chatRoot.value.contains(a)) {
    showCiteTooltip(a)
  }
}
const handleMouseOut = e => {
  const target = e.target
  if (!(target instanceof Element)) return
  const a = target.closest('a[data-full]')
  if (a && chatRoot.value && chatRoot.value.contains(a)) {
    hideCiteTooltip()
  }
}

// 处理引用悬浮提示的事件绑定/解绑
const bindCiteEvents = el => {
  if (el && typeof el.addEventListener === 'function') {
    el.addEventListener('mouseover', handleMouseOver)
    el.addEventListener('mouseout', handleMouseOut)
  }
}
const unbindCiteEvents = el => {
  if (el && typeof el.removeEventListener === 'function') {
    el.removeEventListener('mouseover', handleMouseOver)
    el.removeEventListener('mouseout', handleMouseOut)
  }
}

const isMobile = ref(false)
const isCollapsed = ref(true)
const MOBILE_SIDEBAR_WIDTH = 236
const MENU_ICON_MARGIN = 3
const menuIconLeft = computed(() => {
  return `${(isCollapsed.value ? 0 : MOBILE_SIDEBAR_WIDTH) + MENU_ICON_MARGIN}px`
})
const updateIsMobile = () => {
  isMobile.value = window.innerWidth <= 768
}
const toggleCollapse = () => {
  eventBus.emit('toggleCollapse')
}
const collapsedListener = val => {
  isCollapsed.value = val
}

onMounted(() => {
  bindCiteEvents(chatRoot.value)
})

// chatRoot 在不同页面状态下可能动态出现，需要监听其变化
watch(chatRoot, (el, prev) => {
  unbindCiteEvents(prev)
  bindCiteEvents(el)
})

onBeforeUnmount(() => {
  unbindCiteEvents(chatRoot.value)
  if (citeTooltipEl && citeTooltipEl.parentNode) {
    citeTooltipEl.parentNode.removeChild(citeTooltipEl)
    citeTooltipEl = null
  }
})

let abortController = new AbortController()
const openCreateFolder = () => {
  // 触发左侧知识库的新建文件夹弹窗
  if (asizeRef.value && asizeRef.value.handleCreateFolder) {
    asizeRef.value.handleCreateFolder(knowSelect.value)
  }
}

const abortTranslation = () => {
  if (abortController) {
    abortController.abort()
    abortController = new AbortController() // 重置以便下次使用
  }
}

const fileModal = ref(FileModel.PERSONAL)
const setFileModel = val => {
  fileModal.value = val
  if (val !== FileModel.DEPARTMENT) {
    departmentId.value = null
  }
}

let folderId = ref(0)
let folderName = ref('问知识库')
let departmentId = ref(null)
const personModalRef = ref(null)
const departmentModalRef = ref(null)

const fetchDirectoryDetail = directoryInfo => {
  fileModal.value = directoryInfo.mode
  folderId.value = directoryInfo.folderId
  // 添加文件夹名称的处理
  if (directoryInfo.folderName) {
    folderName.value = directoryInfo.folderName
  }
  departmentId.value = directoryInfo.departmentId ?? null
}

// 清空对话处理函数
const clearChatHistory = () => {
  if (personModalRef.value?.clearChatHistory) {
    personModalRef.value.clearChatHistory()
  }
  if (departmentModalRef.value?.clearChatHistory) {
    departmentModalRef.value.clearChatHistory()
  }
}

const setNet = () => {
  nextTick(() => {
    const isNetValue = localStorage.getItem('isNet')
    isNet.value = isNetValue === 'true'
  })
}

const showListFile = val => {
  fileAry.value = []
  fileAry.value.push(val)
  filePreRef.value.openFile('sample')
}
const setMessage = val => {
  nextTick(() => {
    contentType.value = val
  })
}
const showFileMenu = ref(false)
const showFileSample = val => {
  showFileMenu.value = !showFileMenu.value
}
const handleDragOver = () => {
  if (['query', 'it', 'law', 'board'].includes(pageType.value)) {
    return
  }

  if (isSampleLoad.value || queryIng.value || docIng.value || tranIng.value || finalIng.value) {
    return
  }
  isDragOver.value = true
  nextTick(() => {
    if (entryRef.value) {
      entryRef.value.setDrag(isDragOver.value)
    }
  })
}
const getTextAfterLastDot = str => {
  const lastDotIndex = str.lastIndexOf('.')
  if (lastDotIndex === -1) return '' // 没有点号时返回空字符串
  return str.slice(lastDotIndex + 1)
}
const handleDragLeave = () => {
  if (['query', 'it', 'law', 'board'].includes(pageType.value)) {
    return
  }
  isDragOver.value = false
  nextTick(() => {
    if (entryRef.value) {
      entryRef.value.setDrag(isDragOver.value)
    }
  })
}
const handleDrop = e => {
  if (!isLogin.value) {
    isDragOver.value = false
    ElMessage.warning('请先登录再使用')
    return false
  }
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
  nextTick(() => {
    dragUploads.value.setFiles(data)
  })
}
const handleFileSelect = (val1, val2) => {
  showFileMenu.value = false
  if (!isLogin.value) {
    ElMessage.warning('请先登录再使用')
    return false
  }
  nextTick(() => {
    if (val1 === 'local') {
      fileRefs.value.openFile(val2, fileInputAry.value)
    } else {
      commonUploadModals.value.openFile(val2)
    }
  })
}

// 点号变化逻辑
const updateDots = () => {
  if (dots.value.length >= 5) {
    dots.value = '.' // 重置为一个点
  } else {
    dots.value += '.' // 增加一个点
  }
}

const MODE_MAPPING = new Map([
  ['人资行政专题', 'HR'],
  ['IT专题', 'IT'],
  ['法务专题', 'Law'],
  ['董办专题', 'board']
])

const toDoc = async data => {
  if (pageType.value === 'sample') {
    window.open(data.file_url, '_blank')
    return
  }
  if (['法务专题', '董办专题'].includes(selectedMode.value) && !isNet.value) {
    ElMessage.warning('该模式仅支持通过office网络访问')
    return
  }
  request
    .post(
      '/Files/getFileLinkByName?fileName=' + data.document_title + '&target=' + MODE_MAPPING.get(selectedMode.value)
    )
    .then(res => {
      if (res.status) {
        if (res.data) {
          window.open(res.data, '_blank')
        }
      }
    })
    .catch(err => {
      console.error('获取回复失败:', err)
    })
}
const handleCommonClose = done => {
  // 这里可以添加一些关闭前的逻辑
  done()
}

const processedData = computed(() => {
  return sources => {
    const result = []
    const map = new Map()
    if (pageType.value !== 'sample') {
      sources = currentObj.value.messages.sources
    }
    sources.forEach(item => {
      let key = `${item.document_id}-${item.document_title}`
      if (pageType.value === 'sample') {
        // 通用只根据title判断重复
        key = `${item.document_title}`
      }
      if (!map.has(key)) {
        map.set(key, {
          document_id: item.document_id,
          document_title: item.document_title,
          file_url: item.fileUrl,
          page: new Set()
        })
      }
      map.get(key).page.add(item.page)
    })

    if (pageType.value === 'sample') {
      map.forEach(value => {
        result.push({
          document_title: value.document_title.includes('/')
            ? value.document_title.slice(value.document_title.lastIndexOf('/') + 1)
            : value.document_title,
          page: Array.from(value.page).sort((a, b) => a - b),
          file_url: value.file_url
        })
      })
    } else {
      map.forEach(value => {
        result.push({
          document_title: value.document_title,
          page: Array.from(value.page).sort((a, b) => a - b)
        })
      })
    }

    return result
  }
})

// 将 after 文本中的 {{CITE:id}} 替换为指向 sources 中文档链接的超链接（与 AskKnowledgeDialog.vue 保持一致）
const getLastPathSegment = path => {
  if (!path) return ''
  const normalized = String(path).replace(/\\/g, '/').split('/')
  const last = normalized.filter(Boolean).pop()
  return last || ''
}

// 不合并页码的 CITE 替换：同一文档不同页各自生成独立链接，链接文本为页码
const replaceCitationsWithLinks = (text, sources = []) => {
  if (!text) return ''
  const addSpacesAroundUrls = (str) => {
    let s = String(str)
    const urlBody = "[^\\s<>,，。；;：:！!？?、（）()\\[\\]{}]+"
    const urlHead = new RegExp(`(^|[^\\s])((?:https?:\\/\\/)${urlBody})`, 'g')
    const urlTail = new RegExp(`((?:https?:\\/\\/)${urlBody})(?=[^\\s])`, 'g')
    s = s.replace(urlHead, (m, pre, url) => (pre ? pre + ' ' : '') + url)
    s = s.replace(urlTail, '$1 ')
    return s
  }
  const spacedText = addSpacesAroundUrls(text)
  return String(spacedText).replace(/\{\{CITE:([\d,]+)\}\}/g, (_, ids) => {
    const idArray = ids.split(',').map(id => id.trim())
    const links = []
    idArray.forEach(id => {
      const index = Number(id) - 1
      if (!Number.isInteger(index) || index < 0 || index >= sources.length) return
      const src = sources[index] || {}
      const title = getLastPathSegment(src.document_title)
      const baseUrl = src.fileUrl || src.file_url || ''
      const pages = Array.isArray(src.page) ? src.page : [src.page]
      const pick = (s) => (s || '').split('?')[0].split('#')[0]
      let ext = ''
      if (baseUrl && /^https?:/i.test(baseUrl)) {
        try {
          const target = decodeURIComponent(pick(baseUrl)) || decodeURIComponent(pick(title))
          const m = target.match(/\.([a-zA-Z0-9]+)$/)
          ext = m ? m[1].toLowerCase() : ''
        } catch (_) {
          ext = ''
        }
      }
      pages.filter(p => p !== undefined && p !== null).forEach(p => {
        let url = baseUrl
        if (baseUrl && /^https?:/i.test(baseUrl)) {
          if (ext === 'ppt' || ext === 'pptx') {
            url = `/ppt-viewer.html?src=${encodeURIComponent(baseUrl)}&page=${encodeURIComponent(p)}`
          } else if (ext === 'pdf') {
            url = `/pdf-viewer.html?src=${encodeURIComponent(baseUrl)}&page=${encodeURIComponent(p)}`
          }
        }
        const fullText = `${title ? title : ''}（第${p}页）`
        const esc = (s) => String(s)
          .replace(/&/g, '&amp;')
          .replace(/"/g, '&quot;')
          .replace(/</g, '&lt;')
          .replace(/>/g, '&gt;')
        // 同步 AskKnowledgeDialog.vue 的行为，提供悬浮提示文本
        links.push(
          `<a href="${esc(url || '#')}" class="kb-cite" data-full="${esc(fullText)}">${esc(p)}</a>`
        )
      })
    })
    return links.join(' ')
  })
}

let fullContent = '' // 保存所有已显示内容
let currentDisplayIndex = 0 // 当前显示位置

const display = async (message, batchSize = 12) => {
  return new Promise(resolve => {
    let i = 0
    const render = () => {
      const end = Math.min(i + batchSize, message.length)
      fullContent += message.substring(i, end)
      currentTransData.value = fullContent
      i = end

      if (i < message.length) {
        requestAnimationFrame(render)
      } else {
        resolve()
      }
    }
    requestAnimationFrame(render)
  })
}
// 改进后的消息序列显示函数
const displayMessagesTran = async () => {
  // 获取当前需要显示的新内容部分
  const newContent = currentTransData.value.slice(currentDisplayIndex)

  if (newContent) {
    await display(newContent) // 只显示新内容部分
    currentDisplayIndex = currentTransData.value.length // 更新显示位置
  }
}
const submitFinal = async (val, isRefresh, ob) => {
  // 设置当前预览文件
  fileObj.value = ob
  finalFile.value = ob || finalFile.value
  if (queryIng.value || docIng.value || tranIng.value || finalIng.value) {
    ElMessage.warning('有问答正在进行中,请稍后再试')
    return
  }
  if (!ob && !checkData(val, { preserveInput: !!isRefresh })) {
    return
  }
  isDragOver.value = false
  const queryData = isRefresh && typeof val !== 'undefined' ? val : newQuestion.value
  if (!isRefresh) {
    newQuestion.value = ''
  }
  finalData.value = {
    title: '',
    data: []
  }
  finalQuest.value = ''
  finalIng.value = true
  docIng.value = true
  if (isRefresh) {
    let current = currentId.value
    limitFinalId.value = current
    await asizeRef.value.deleteData(current, true)
    activeIndex.value = 0
    currentIndex.value = 0
  }
  // 在发送消息的一瞬间预保存对话（总结）
  if (!currentId.value || isRefresh) {
    const preTitle = ob ? ob.originalFileName : (queryData && queryData.trim() ? queryData.trim() : '新对话')
    try {
      const saveResult = await request.post('/Message/save', {
        userId: userInfo.value.id,
        type: '总结',
        id: '',
        data: [],
        title: preTitle
      })
      if (saveResult.status) {
        currentId.value = saveResult.data
        limitFinalId.value = saveResult.data
        await fetchChatList(undefined, 'final')
        activeIndex.value = 0
      }
    } catch (e) {
      console.error('总结预保存失败', e)
    }
  }
  let title = ''
  if (!isRefresh) {
    activeIndex.value = 0
    currentIndex.value = 0
  }
  interval = setInterval(updateDots, 500) // 每 500ms 更新一次
  currentRequestUrl.value = '/AI/summarize'
  finalQuest.value = ob ? ob.originalFileName : queryData
  const passQuery = ob ? ob.originalFileName : queryData
  entryRef.value.changeDynamicRows()
  if (ob && !isPureObject(ob.fileId)) {
    const objSample = {
      fileId: ob.fileId,
      local: ob.local
    }
    ob.fileId = objSample
  }
  nextTick(() => {
    if (entryRef.value?.fileRef) {
      entryRef.value.fileRef.closeFile()
    }
  })
  activeIndex.value = 0
  request
    .post('/AI/summarize', {
      user_id: userInfo.value.id,
      question: passQuery,
      file: finalFile.value
        ? finalFile.value.fileId
        : {
            fileId: ''
          }
    })
    .then(res => {
      finalIng.value = false
      docIng.value = false
      currentRequestUrl.value = ''
      currentIndex.value = ''
      clearInterval(interval)

      if (res.status) {
        finalData.value.title = res.data.summary

        if (res.data.key_points) {
          finalData.value.data = res.data.key_points
        }
        const obj = {
          question: passQuery,
          answer: finalData.value
        }

        postFinal(obj, title.replace(/\([^)]*\)/g, ''), ob)
      } else {
        if (res.code === 400) {
          const obj = {
            question: passQuery,
            answer: ''
          }
          postFinal(obj, title.replace(/\([^)]*\)/g, ''), ob)
          ElMessage.warning(res.message)
        }
      }
    })
    .catch(err => {
      currentIndex.value = ''
      if (err.message !== 'canceled') {
        ElMessage.error('总结失败' + err.message)
      }

      finalIng.value = false
      docIng.value = false
      clearInterval(interval)
    })
}
const samplePost = event => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault() // 阻止默认的换行行为
    if (isSampleLoad.value || queryIng.value || docIng.value || tranIng.value || finalIng.value) {
      ElMessage.warning('有问答正在进行中,请稍后再试')
      return
    }
    submitSample()
  }
}

const refreshData = () => {
  const now = Date.now()

  if (now - lastRefreshTime < 1500) {
    return
  }

  lastRefreshTime = now
  if (isSampleLoad.value || queryIng.value || docIng.value || tranIng.value || finalIng.value) {
    ElMessage.warning('有问答正在进行中,请稍后再试')
    return
  }
  if (['query', 'it', 'law', 'board'].includes(pageType.value)) {
    queryIng.value = false
    submitQuestion(tipQuery.value, true)
  } else if (pageType.value === 'tran') {
    let obj = ''
    if (activeIndex.value || activeIndex.value === 0) {
      obj = transFile.value
    }
    const val = transQuest.value
    submitTran(val, true, obj)
  } else if (pageType.value === 'final') {
    let obj = ''
    if (activeIndex.value || activeIndex.value === 0) {
      obj = finalFile.value
    }
    submitFinal(finalQuest.value, true, obj)
  } else if (pageType.value === 'sample') {
    let ary = []
    if (activeIndex.value || activeIndex.value === 0) {
      const length = chatQuery.messages.length
      if (length === 1) {
        ary = chatQuery.messages[0].files
      } else if (length > 1) {
        if (chatQuery.messages[length - 1].role === 'user') {
          ary = chatQuery.messages[length - 1].files
        } else {
          ary = chatQuery.messages[length - 2].files
        }
      }
    }
    fileInputAry.value = ary

    submitSample(chatQuery.messages[chatQuery.messages.length - 2].content, true)
  }
}
const upCommon = async () => {
  if (!isLogin.value) {
    ElMessage.warning('请先登录再使用')
    return false
  }
  if (isDisabled.value) return // 如果按钮已禁用，直接返回
  let id = ''
  if (currentId.value) {
    id = currentId.value
  }
  isDisabled.value = true
  // 2 秒后重新启用按钮
  setTimeout(() => {
    isDisabled.value = false
  }, 3000)
  request
    .post('/Message/feedback', {
      id: id,
      feedback: {
        agree: true,
        content: ''
      }
      // showLoading: true
    })
    .then(res => {
      if (res.status) {
        ElMessage.success('谢谢您的点赞,您的支持是我们最大的动力！')
      }
    })
    .catch(err => {
      console.error(err)
    })
}
const downCommon = () => {
  commonVisible.value = true
}
const submitCommon = async () => {
  if (!isLogin.value) {
    ElMessage.warning('请先登录再使用')
    return false
  }
  let id = ''
  if (currentId.value) {
    id = currentId.value
  }
  request
    .post('/Message/feedback', {
      id: id,
      feedback: {
        agree: false,
        content: commonQuestion.value
      }
    })
    .then(res => {
      if (res.status) {
        ElMessage.success('评价成功,我们会继续努力的！')
        commonVisible.value = false
        commonQuestion.value = ''
      }
    })
    .catch(err => {
      commonVisible.value = false
      commonQuestion.value = ''
      console.error('获取回复失败:', err)
    })
}
let lastClickTime = 0
let lastRefreshTime = 0
const submitQuestionSend = () => {
  const now = Date.now()

  if (now - lastClickTime < 1500) {
    return
  }

  lastClickTime = now
  if (isSampleLoad.value) {
    abortTranslation()
    cancelCurrentRequest('query')
    return
  }
  submitQuestion()
}
const submitITSend = () => {
  const now = Date.now()

  if (now - lastClickTime < 1500) {
    return
  }

  lastClickTime = now
  if (isSampleLoad.value) {
    abortTranslation()
    cancelCurrentRequest('it')
    return
  }
  submitQuestion()
}
const submitLawSend = () => {
  const now = Date.now()

  if (now - lastClickTime < 1500) {
    return
  }

  lastClickTime = now
  if (isSampleLoad.value) {
    abortTranslation()
    cancelCurrentRequest('law')
    return
  }
  submitQuestion()
}

const submitSampleSend = () => {
  const now = Date.now()

  if (now - lastClickTime < 1500) {
    return
  }

  lastClickTime = now
  if (isSampleLoad.value) {
    abortTranslation()
    cancelCurrentRequest('sample')
    return
  }
  submitSample()
}
const submitTranSend = () => {
  const now = Date.now()

  if (now - lastClickTime < 1500) {
    return
  }

  lastClickTime = now
  if (finalIng.value) {
    abortTranslation()
    finalIng.value = false
    tranIng.value = false
    cancelCurrentRequest('tran')
    return
  }
  submitTran()
}
const submitFinalSend = () => {
  if (finalIng.value) {
    cancelCurrentRequest('final')
    return
  }
  submitFinal()
}

const deleteImg = index => {
  fileInputAry.value.splice(index, 1)
  drayAry.value.splice(index, 1)
  if (!fileInputAry.value || fileInputAry.value.length === 0) {
    fileInputAry.value = []
  }
}
const submitSampleFile = val => {
  if (contentType.value !== ContentType.CONVERSATION) {
    return
  }
  currentQuestion.value = true
  isDragOver.value = false
  for (let i = 0; i < val.length; i++) {
    val[i].fileName = decodeURIComponent(val[i].fileName)
    val[i].originalFileName = decodeURIComponent(val[i].originalFileName)
  }

  // Merge existing files with newly selected ones and deduplicate by name
  const merged = [...fileInputAry.value, ...val]
  const unique = []
  const nameSet = new Set()
  for (const item of merged) {
    const name = item.originalFileName || item.fileName
    if (!nameSet.has(name)) {
      nameSet.add(name)
      unique.push(item)
    }
  }

  if (unique.length > FILE_LIST_LIMIT) {
    ElMessage.warning("最多上传" + FILE_LIST_LIMIT + "个文件")
    return
  }
  fileAry.value = unique
  fileInputAry.value = JSON.parse(JSON.stringify(unique))
}

const submitSampleTitle = val => {
  if (isSampleLoad.value || queryIng.value || docIng.value || tranIng.value || finalIng.value) {
    ElMessage.warning('有问答正在进行中,请稍后再试')
    return
  }
  submitSample(val)
}

// 自动滚动
const autoScroll = () => {
  nextTick(() => {
    const container = document.querySelector('.message-container')
    if (container) {
      container.scrollTop = container.scrollHeight + 100
    }
  })
}

const isPureObject = value => {
  // 排除 null 和基础类型
  if (typeof value !== 'object' || value === null) return false

  // 排除数组、日期、正则等
  const proto = Object.getPrototypeOf(value)
  return proto === Object.prototype || proto === null
}

const submitSample = async (val, isRefresh) => {

  const fileInput = fileInputAry.value
  if (fileInput.length === 0 || !fileInput) {
    if (!checkData(val, { preserveInput: !!isRefresh })) {
      return
    }
  }
  if (!isRefresh && val) {
    newQuestion.value = val
  }

  const queryValue = isRefresh && typeof val !== 'undefined' ? val : newQuestion.value

  // 在发送消息的一瞬间预保存对话，确保左侧列表能立即显示
  if (!currentId.value) {
    let title = ''
    if (newQuestion.value && newQuestion.value.trim()) {
      // 有文本内容时，使用文本作为标题
      title = newQuestion.value.trim()
    } else if (fileInput && fileInput.length > 0) {
      // 只发送文件时，用文件名逗号拼接作为标题
      title = fileInput.map(item => item.originalFileName).filter(Boolean).join(',')
    } else {
      // 默认标题
      title = '新对话'
    }
    
    try {
      const saveResult = await request.post('/Message/save', {
        userId: userInfo.value.id,
        type: '通用模式',
        id: '',
        data: [],
        isThink: deepType.value,
        title: title
      })
      
      if (saveResult.status) {
        // 保存成功后立即设置对话ID
        currentId.value = saveResult.data
        limitId.value = saveResult.data
        // 立即刷新左侧列表
        await fetchChatList(undefined, 'sample')
        // 选中新创建的对话
        activeIndex.value = 0
      }
    } catch (error) {
      console.error('预保存对话失败:', error)
      // 即使预保存失败，也继续执行后续逻辑
    }
  }
  
  currentQuestion.value = true
  isSampleStop.value = false
  dynamicRows.value = 1
  isSampleLoad.value = true
  limitLoading.value = true
  if (chatQuery.messages.length === 1 && chatQuery.messages[0].files) {
    chatQuery.messages = []
  }
  let filesSample = []
  if (fileInput && fileInput.length > 0) {
    for (let me = 0; me < fileInput.length; me++) {
      if (isPureObject(fileInput[me].fileId)) {
        filesSample.push(fileInput[me].fileId)
      } else {
        const objSample = {
          fileId: fileInput[me].fileId,
          local: fileInput[me].local
        }
        filesSample.push(objSample)
      }

      fileInput[me].local = fileInput[me].fileId.local === false ? fileInput[me].fileId.local : fileInput[me].local
      fileInput[me].fileId = fileInput[me].fileId.fileId ? fileInput[me].fileId.fileId : fileInput[me].fileId
    }
  }
  const currentData = {
    role: 'user',
    content: queryValue ? queryValue : '',
    files: toRaw(JSON.parse(JSON.stringify(fileInput))),
    personalKnowledge: useKnowledge.value
  }

  if (isRefresh) {
    chatQuery.messages.length -= 2
    currentIndex.value = activeIndex.value
  }
  let mes = JSON.parse(JSON.stringify(chatQuery))
  mes.messages.push(currentData)
  limitSample.value = JSON.parse(JSON.stringify(mes))
  const params = JSON.parse(JSON.stringify(mes))
  for (let j = 0; j < params.messages.length; j++) {
    if (j % 2 === 0) {
      params.messages[j].role = 'user'
    } else {
      params.messages[j].role = 'assistant'
    }
  }
  params.userId = userInfo.value.id
  params.sessionId = limitId.value || currentId.value || ''
  params.model = deepType.value ? 1 : 0
  params.personalKnowledgeBase = useKnowledge.value
  params.files = filesSample
  params.chatType = 'all'

  tipQuery.value = queryValue
  if (!isRefresh) {
    newQuestion.value = ''
  }
  let title = ''

  let id = currentId.value
  limitId.value = id
  currentIndex.value = activeIndex.value
  interval = setInterval(updateDots, 500)

  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  })

  const assistantMsg = {
    role: 'assistant',
    content: '',
    before: '',
    after: '',
    hasSplit: false,
    isNewData: true,
    sources: [],
    thinking: ''
  }
  mes.messages.push(assistantMsg)
  // 使用一个对象记录哪些 content 已经有 user 了
  chatCurrent.messages = mes.messages
  chatQuery.isLoading = true
  const isThink = deepType.value
  fileInputAry.value = []
  fileAry.value = []
  drayAry.value = []
  try {
    // 替换为实际的后端接口地址
    const res = await fetch(import.meta.env.VITE_API_BASE_URL + '/AI/unifiedChat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(params),
      signal: abortController.signal // 添加 abort signal
    })
    if (res.status === 429) {
      chatQuery.isLoading = false
      limitLoading.value = false
      isSampleLoad.value = false
      limitId.value = ''
      queryIng.value = false
      chatQuery.messages = JSON.parse(JSON.stringify(chatCurrent.messages))
      ElMessage.error('服务器繁忙,请稍后再试')
      return
    }
    // 非 2xx 响应时，读取并展示后端返回的错误 message
    if (!res.ok) {
      let errorText = ''
      try {
        errorText = await res.text()
      } catch (_) {}
      let errorJson = null
      try {
        errorJson = JSON.parse(errorText)
      } catch (_) {}
      const serverMessage = errorJson?.message || errorJson?.msg || '请求失败,请稍后再试'
      ElMessage.error(serverMessage)
      chatQuery.isLoading = false
      isSampleLoad.value = false
      limitLoading.value = false
      limitId.value = ''
      queryIng.value = false
      currentRequestUrl.value = ''
      chatQuery.messages = JSON.parse(JSON.stringify(chatCurrent.messages))
      return
    }
    // 处理流式数据
    const reader = res.body.getReader()
    const decoder = new TextDecoder() // 启用流模式解码
    let buffer = '' // 缓冲区用于存储不完整的数据

    while (true) {
      const { value, done } = await reader.read()

      if (done) {
        clearInterval(interval)
        currentIndex.value = ''
        isSampleLoad.value = false
        limitLoading.value = false
        chatQuery.isLoading = false
        limitId.value = ''
        currentRequestUrl.value = ''
        chatQuery.messages = JSON.parse(JSON.stringify(chatCurrent.messages))

        nextTick(() => {
          // 滚动到底部
          if (messageContainer.value) {
            const messages = messageContainer.value.children
            if (messages.length > 0) {
              const lastMessage = messages[messages.length - 2]
              // 滚动到最后一个消息的开头部分
              lastMessage.scrollIntoView({ behavior: 'smooth', block: 'start' })
            }
          }
        })
        postSample(id, title, isThink, filesSample)
        break
      }
      buffer += decoder.decode(value, { stream: true })
      // 使用更安全的分割方式（避免截断 JSON 结构）[3](@ref)
      const chunks = buffer.split(/(?=data:)/g)
      buffer = chunks.pop() || ''
      if (isCompleteJSON(buffer)) {
        const jsonData = JSON.parse(buffer)
        const serverMessage = jsonData?.message || jsonData?.msg
        if (serverMessage) {
          ElMessage.error(serverMessage)
        } else if (jsonData.code === 400) {
          ElMessage.error('文本过长，请重新尝试')
        } else {
          ElMessage.error('文本异常,请稍后再试')
        }
        isSampleLoad.value = false
        limitLoading.value = false
        chatQuery.isLoading = false
        limitId.value = ''
        currentRequestUrl.value = ''
        postSample(id, title, isThink, filesSample)
      }

      for (const chunk of chunks) {
        // 1. 修复正则匹配语法
        const jsonMatch = chunk.match(/data:\s*({[\s\S]*?})(?=\ndata:|\n\n|$)/)
        // 2. 添加条件判断包裹
        if (jsonMatch) {
          if (messageContainer.value) {
            messageContainer.value.scrollTop = messageContainer.value.scrollHeight
          }
          try {
            const { content, type, sources } = JSON.parse(jsonMatch[1])

            // 个人知识库字段处理逻辑
            if (useKnowledge.value) {
              if (type === 'reasoning') {
                assistantMsg.before += content
              } else if (type === 'streaming') {
                assistantMsg.after += content
                // 流式过程中替换 CITE（sources 可能稍后到达，最终在 final_answer 再完整替换）
                assistantMsg.after = replaceCitationsWithLinks(assistantMsg.after, assistantMsg.sources)
              } else if (type === 'process') {
                assistantMsg.thinking = content
                await delay(250)
              } else if (type === 'final_answer') {
                if (sources && sources.length > 0) {
                  assistantMsg.sources = sources
                }
                // 最终回答进行一次完整替换
                assistantMsg.after = replaceCitationsWithLinks(content, assistantMsg.sources)
              }
              assistantMsg.hasSplit = true
            } else {
              // 原始逻辑保存不变
              if (assistantMsg.hasSplit) {
                // 已遇到分隔符，内容追加到后半部分
                assistantMsg.after += content
              } else {
                const sp = isThink ? '</think>' : ''
                // 检查当前数据块是否包含分隔符
                const splitIndex = content.indexOf(sp)
                if (splitIndex === -1) {
                  // 未找到分隔符，全部追加到前半部分
                  assistantMsg.before += content
                } else {
                  // 找到分隔符，分割内容
                  assistantMsg.before += content.slice(0, splitIndex)
                  assistantMsg.after += content.slice(splitIndex + sp.length)
                  assistantMsg.hasSplit = true
                }
              }
            }
            // 立即更新视图（无需防抖）
            chatCurrent.messages.splice(-1, 1, {
              ...toRaw(assistantMsg),
              before: assistantMsg.before,
              after: assistantMsg.after,
              content: assistantMsg.before + assistantMsg.after, // 兼容旧字段
              sources: assistantMsg.sources,
              thinking: assistantMsg.thinking
            })
          } catch (e) {
            currentIndex.value = ''
            console.error('JSON 解析失败:', jsonMatch[1], '错误:', e)
            ElMessage.error('数据格式异常')
          }
        }
      }
    }
  } catch (error) {
    chatQuery.isLoading = false
    isSampleLoad.value = false
    limitLoading.value = false
    limitId.value = ''
    queryIng.value = false
    chatQuery.messages = JSON.parse(JSON.stringify(chatCurrent.messages))
    if (error.name !== 'AbortError') {
      ElMessage.error('服务器繁忙,请稍后再试')
    }
  }
}

const submitTran = async (val, isRefresh, sendingFile) => {
  fileObj.value = sendingFile
  if (!selectedLan.value) {
    ElMessage.warning("请选择您的目标翻译语言")
    return
  }
  transFile.value = sendingFile || transFile.value
  if (useTranslationDocument.value) {
    const currentFileName =
      (transFile.value &&
        (transFile.value.originalFileName || transFile.value.fileName)) ||
      ''
    if (currentFileName) {
      const fileExtension = currentFileName.split('.').pop().toLowerCase()
      if (fileExtension !== 'xlsx') {
        ElMessage.warning('请上传xlsx格式文档进行翻译')
        return
      }
    }
    const fileInfo = transFile.value?.fileId
    const normalizedFileId = isPureObject(fileInfo) ? fileInfo?.fileId : fileInfo
    if (!normalizedFileId) {
      ElMessage.warning('请先上传需要翻译的 Excel 文件')
      return
    }
  }
  if (queryIng.value || docIng.value || tranIng.value || finalIng.value) {
    ElMessage.warning('有问答正在进行中,请稍后再试')
    return
  }
  if (!sendingFile && !checkData(val, { preserveInput: !!isRefresh })) {
    return
  }
  isDragOver.value = false
  isTranStop.value = false
  finalIng.value = true
  tranIng.value = true
  interval = setInterval(updateDots, 500) // 每 500ms 更新一次
  const preservedInput = newQuestion.value
  limitQuery.value = preservedInput
  limitTranLoading.value = true
  fullContent = ''
  currentDisplayIndex = 0
  const queryValue = isRefresh && typeof val !== 'undefined' ? val : preservedInput
  const target = selectedLan.value
  if (isRefresh) {
    let current = currentId.value
    limitTranId.value = current
    await asizeRef.value.deleteData(current, true)
    activeIndex.value = 0
    currentIndex.value = 0
  }
  // 在发送消息的一瞬间预保存对话（翻译）
  if (!currentId.value || isRefresh) {
    const preTitle = sendingFile ? sendingFile.originalFileName : (queryValue && queryValue.trim() ? queryValue.trim() : '新对话')
    try {
      const saveResult = await request.post('/Message/save', {
        userId: userInfo.value.id,
        type: '翻译',
        id: '',
        data: [],
        title: preTitle
      })
      if (saveResult.status) {
        currentId.value = saveResult.data
        limitTranId.value = saveResult.data
        await fetchChatList(undefined, 'tran')
        activeIndex.value = 0
      }
    } catch (e) {
      console.error('翻译预保存失败', e)
    }
  }
  if (!isRefresh) {
    newQuestion.value = ''
  }
  transQuest.value = ''
  transData.value = ''
  currentTransData.value = ''
  translationDocumentProcess.value = ''
  translationDocumentFinal.value = ''
  let title = ''
  if (!isRefresh) {
    activeIndex.value = '0'
    currentIndex.value = 0
    currentIndex.value = activeIndex.value
  }

  transQuest.value = sendingFile ? sendingFile.originalFileName : queryValue
  const passQuery = sendingFile ? sendingFile.originalFileName : queryValue

  if (sendingFile && !isPureObject(sendingFile.fileId)) {
    const objSample = {
      fileId: sendingFile.fileId,
      local: sendingFile.local
    }
    sendingFile.fileId = objSample
  }
  nextTick(() => {
    if (entryRef.value?.fileRef) {
      entryRef.value.fileRef.closeFile()
    }
  })
  activeIndex.value = 0
  try {
    const requestBody = useTranslationDocument.value
      ? {
          sessionId: limitTranId.value || currentId.value || '',
          user_id: userInfo.value.id,
          target_language: selectedLan.value,
          source_text: sendingFile?.fileName ? '' : queryValue,
          file: sendingFile?.fileName && transFile.value.fileId ? transFile.value.fileId : { fileId: '' }
        }
      : {
          user_id: userInfo.value.id,
          source_text: sendingFile?.fileName ? '' : queryValue,
          target_language: selectedLan.value,
          file: sendingFile?.fileName && transFile.value.fileId ? transFile.value.fileId : { fileId: '' }
        }
    const res = await fetch(import.meta.env.VITE_API_BASE_URL + (useTranslationDocument.value ? '/AI/excelTranslate' : '/AI/translateStream'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(useTranslationDocument.value ? { Accept: 'text/event-stream' } : {})
      },
      body: JSON.stringify(requestBody),
      signal: abortController.signal // 添加 abort signal
    })
    if (res.status === 429) {
      ElMessage.error('服务器繁忙,请稍后再试')
      return
    }

    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let accumulatedContent = ''

    while (true) {
      const { value, done } = await reader.read()
      if (done) {
        // 确保处理完缓冲区剩余数据
        if (buffer.trim()) console.warn('未处理的缓冲区内容:', buffer)
        limitTranId.value = ''
        limitTranLoading.value = false
        break
      }
      if (isTranStop.value) {
        return
      }
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')

      // 保留最后不完整的行在缓冲区
      buffer = lines.pop() || ''
      if (isCompleteJSON(buffer)) {
        const jsonData = JSON.parse(buffer)
        if (jsonData.code === 400) {
          ElMessage.error('文本过长，请重新尝试')
        } else {
          ElMessage.error('文本异常,请稍后再试')
        }

        finalIng.value = false
        tranIng.value = false
        const passData = {
          question: passQuery,
          answer: ''
        }
        await postTran(passData, title.replace(/\([^)]*\)/g, ''), sendingFile, target)
      }
      for (const line of lines) {
        if (!line.startsWith('data:')) continue

        try {
          const jsonStr = line.substring(5).trim()
          if (!jsonStr) continue
          if (messageContainerTran.value && limitTranLoading.value) {
            messageContainerTran.value.scrollTop = messageContainerTran.value.scrollHeight
          }
          // 安全解析检查
          if (!isValidJson(jsonStr)) {
            console.warn('不完整JSON:', jsonStr)
            buffer = line + '\n' + buffer // 回退到缓冲区
            continue
          }

          const data = JSON.parse(jsonStr)
          if (useTranslationDocument.value) {
            const contentText = data.content !== undefined && data.content !== null ? String(data.content) : ''
            if (data.type === 'process') {
              translationDocumentProcess.value = contentText
              await delay(250)
            }
            if (data.type === 'final') {
              translationDocumentFinal.value = contentText
              transData.value = contentText
              limitTranLoading.value = false
              limitTranId.value = ''
              finalIng.value = false
              tranIng.value = false
              clearInterval(interval)
              const passData = {
                question: passQuery,
                answer: contentText
              }
              await postTran(passData, title.replace(/\([^)]*\)/g, ''), sendingFile, target)
              return
            }
            continue
          }

          if (data.content !== undefined) {
            accumulatedContent += data.content
            currentTransData.value = accumulatedContent
            await displayMessagesTran()
          }

          if (data.end === 2) {
            transData.value = JSON.parse(JSON.stringify(currentTransData.value))
            const passData = {
              question: passQuery,
              answer: accumulatedContent
            }
            await postTran(passData, title.replace(/\([^)]*\)/g, ''), sendingFile, target)
            accumulatedContent = ''
            limitTranLoading.value = false
            limitTranId.value = ''
          }
        } catch (error) {
          currentIndex.value = ''
          limitTranId.value = ''
          limitTranLoading.value = false
          if (error.name !== 'AbortError') {
            ElMessage.error('翻译失败' + error.message)
          } else {
            ElMessage.error('翻译失败')
          }
          finalIng.value = false
          tranIng.value = false
          clearInterval(interval)
        }
      }
    }
  } catch (error) {
    currentIndex.value = ''
    limitTranId.value = ''
    limitTranLoading.value = false
    if (error.name !== 'AbortError') {
      ElMessage.error('翻译失败' + error.message)
    }
    finalIng.value = false
    tranIng.value = false
    clearInterval(interval)
  }

  /**
   * 验证JSON字符串的完整性和有效性
   * @param {string} str - 待验证的JSON字符串
   * @returns {boolean} - 是否为有效的JSON格式
   */
  function isValidJson(str) {
    try {
      JSON.parse(str)
      return true
    } catch {
      // 如果解析失败，检查是否为完整的JSON结构
      return isCompleteJSON(str)
    }
  }
}
const summitPost = event => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault() // 阻止默认的换行行为
    submitQuestion()
  }
}

const submitQuestion = async (val, isRefresh) => {
  if (queryIng.value || docIng.value || tranIng.value || finalIng.value) {
    ElMessage.warning('有问答正在进行中,请稍后再试')
    return
  }
  if (['law', 'board'].includes(pageType.value) && !isNet.value) {
    ElMessage.warning('该模式仅支持通过office网络访问')
    return
  }
  if (!checkData(val, { preserveInput: !!isRefresh })) {
    return
  }
  currentQuestion.value = true
  isQueryStop.value = false
  dynamicRows.value = 1
  currentMessageIndex.value = 0
  currentObj.value.list = {}
  currentObj.value.messages = {}
  currentObj.value.messageList = []
  currentObj.value.thinking = ''
  const queryValue = isRefresh && typeof val !== 'undefined' ? val : newQuestion.value
  limitQueryLoading.value = true
  tipQuery.value = queryValue
  streamingQuestion.value = queryValue // 保存流式输出过程中正在加载的问题
  if (!isRefresh) {
    newQuestion.value = ''
  }
  queryIng.value = true
  isSampleLoad.value = true
  const pgType = pageType.value
  let title = ''
  if (isRefresh) {
    let current = currentId.value
    title = currentObj.value.title
    await asizeRef.value.deleteData(current, true)
  }

  // 专题模式：在人资/IT/法务等模式发送瞬间进行预保存，确保左侧列表立即出现
  if ((!currentId.value || isRefresh) && ['query', 'it', 'law', 'board'].includes(pgType)) {
    const typeMap = new Map([
      ['query', '人资行政专题'],
      ['it', 'IT专题'],
      ['law', '法务专题'],
      ['board', '董办专题']
    ])
    const preTitle = (queryValue && queryValue.trim()) ? queryValue.trim() : '新对话'
    try {
      const saveResult = await request.post('/Message/save', {
        userId: userInfo.value.id,
        type: typeMap.get(pgType),
        id: currentId.value || '',
        data: [],
        isThink: deepType.value,
        title: preTitle
      })
      if (saveResult.status) {
        // 专题模式使用独立的预保存ID，避免切换对话时被覆盖
        topicPreSaveId.value = saveResult.data
        currentId.value = saveResult.data
        limitId.value = saveResult.data
        limitQueryId.value = saveResult.data
        await fetchChatList(undefined, pgType)
        activeIndex.value = 0
      }
    } catch (e) {
      console.error('专题预保存失败', e)
    }
  }

  const isThink = deepType.value === true
  try {
    const typeMap = new Map([
      ['query', '人资行政专题'],
      ['it', 'IT专题'],
      ['law', '法务专题'],
      ['board', '董办专题']
    ])
    // 替换为实际的后端接口地址
    const res = await fetch(import.meta.env.VITE_API_BASE_URL + '/AI/query', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        question: queryValue,
        user_id: userInfo.value.id,
        model: deepType.value ? 1 : 0,
        type: typeMap.get(pageType.value)
      }),
      signal: abortController.signal // 添加 abort signal
    })
    // 处理流式数据
    const reader = res.body.getReader()
    if (res.status === 429) {
      ElMessage.error('服务器繁忙,请稍后再试')
      return
    }
    const decoder = new TextDecoder()
    let buffer = '' // 缓冲区用于存储不完整的数据
    while (true) {
      const { value, done } = await reader.read()
      if (done) {
        currentIndex.value = ''
        break
      }
      // 将二进制数据解码并添加到缓冲区
      buffer += decoder.decode(value, { stream: true })
      //处理buffer数据
      // 清理数据
      buffer = buffer.replace(/data:\s*/g, '')
      // 尝试按分隔符分割数据
      const jsonStr = buffer.split('\n\n')

      // 如果最后一个部分不完整，保留在缓冲区中
      buffer = jsonStr.pop() || ''

      let finalAnswer = {}

      jsonStr.forEach(element => {
        const type = JSON.parse(element).type
        if (type === 'final_answer') {
          queryIng.value = false
          limitQueryId.value = ''
          currentObj.value.messages = JSON.parse(element)
          finalAnswer = JSON.parse(element)
          // 流式结束：将 content 中的 {{CITE:..}} 替换为链接（与通用 after 一致）
          if (finalAnswer && typeof finalAnswer.content === 'string' && Array.isArray(finalAnswer.sources)) {
            const processed = replaceCitationsWithLinks(finalAnswer.content, finalAnswer.sources)
            finalAnswer.content = processed
            if (currentObj.value && currentObj.value.messages) {
              currentObj.value.messages.content = processed
            }
          }
          currentObj.value.messages.isHistory = true
          const paramsQuery = {
            title: title ? title : queryValue,
            queryValue: queryValue
          }
          limitQueryLoading.value = false
          streamingQuestion.value = '' // 清空流式输出过程中的问题缓存
          postQuestion(currentObj.value.thinking, finalAnswer, paramsQuery, pgType, isThink)
        } else {
          currentObj.value.thinking = ''
          currentObj.value.thinking = JSON.parse(element).content
          delay(250)
        }
      })
      // await displayMessagesSequentially()
    }
  } catch (error) {
    currentIndex.value = ''
    isSampleLoad.value = false
    queryIng.value = false
    limitQueryId.value = ''
    limitQueryLoading.value = false
    streamingQuestion.value = '' // 清空流式输出过程中的问题缓存
    if (error.name !== 'AbortError') {
      ElMessage.error('服务器繁忙,请稍后再试')
    }
  }
}

const postSample = async (ids, title, isThink, postSample) => {

  let titleStr = ''
  let num = parseInt(localStorage.getItem('count'))
  num = num + 1
  localStorage.setItem('count', num)
  if (chatQuery.messages[0].files && chatQuery.messages[0].files.length > 0) {
    for (var i = 0; i < chatQuery.messages[0].files.length; i++) {
      titleStr += chatQuery.messages[0].files[i].originalFileName + ','
    }
    titleStr = titleStr.substring(0, titleStr.length - 1)
  }
  
  // 使用预保存的对话ID进行更新
  const finalTitle = title ? title : (chatQuery.messages[0].content ? chatQuery.messages[0].content + titleStr : titleStr)
  
  request
    .post('/Message/save', {
      userId: userInfo.value.id,
      type: '通用模式',
      id: ids, // 使用预保存的对话ID
      data: chatQuery.messages,
      isThink: isThink ? true : false,
      title: finalTitle
    })
    .then(async res => {
      if (res.status) {
        // 如果预保存时没有设置ID，这里设置一下
        if (!currentId.value) {
          currentId.value = res.data
        }
        // 立即刷新左侧列表
        await fetchChatList(undefined, getChatTypeFromPageType(pageType.value))
        // 通用模式刷新列表后自动选择第一项
        if (pageType.value === 'sample') {
          setTimeout(() => {
            if (asizeRef.value && asizeRef.value.autoSelectFirstItem) {
              asizeRef.value.autoSelectFirstItem()
            }
          }, 100) // 延迟100ms确保列表已渲染完成
        } else {
          // 其他模式保持原有逻辑
          activeIndex.value = 0
        }
        if (ids !== res.data) {
          limitId.value = res.data
        }
      }
    })
    .catch(err => {
      console.error('更新对话失败:', err)
    })
}

const postQuestion = async (think, obj, val, type, isThink) => {
  let num = parseInt(localStorage.getItem('count'))
  num = num + 1
  localStorage.setItem('count', num)
  const typeMap = new Map([
    ['query', '人资行政专题'],
    ['it', 'IT专题'],
    ['law', '法务专题'],
    ['board', '董办专题']
  ])
  // 专题模式使用预保存的独立ID，避免切换对话时ID被覆盖
  const saveId = topicPreSaveId.value || currentId.value || ''
  request
    .post('/Message/save', {
      userId: userInfo.value.id,
      type: typeMap.get(pageType.value),
      title: val.title,
      id: saveId, // 使用预保存的独立ID
      isThink: isThink,
      data: {
        thinking: think,
        question: val.queryValue,
        answer: obj
      }
    })
    .then(async (res) => {
      if (res.status) {
        // 若未预保存，后端仍会返回新的id，这里兜底设置
        if (!currentId.value) currentId.value = res.data
         isSampleLoad.value = false
         await fetchChatList(undefined, type)
         // 专题模式刷新列表后自动选择第一项
         if (['query', 'it', 'law', 'board'].includes(pageType.value)) {
           setTimeout(() => {
             if (asizeRef.value && asizeRef.value.autoSelectFirstItem) {
               asizeRef.value.autoSelectFirstItem()
             }
           }, 100) // 延迟100ms确保列表已渲染完成
         }
         // 清理专题模式预保存ID，避免内存泄漏
         topicPreSaveId.value = ''
      } else {
        isSampleLoad.value = false
      }
      limitQueryLoading.value = false
      limitId.value = ''
    })
    .catch(err => {
      isSampleLoad.value = false
      // 出错时也要清理预保存ID
      topicPreSaveId.value = ''
      console.error('获取回复失败:', err)
    })
}

const postTran = async (obj, title, ob, target) => {
  let num = parseInt(localStorage.getItem('count'))
  num = num + 1
  localStorage.setItem('count', num)
  request
    .post('/Message/save', {
      userId: userInfo.value.id,
      type: '翻译',
      title: title ? title : obj.question,
      id: limitTranId.value || currentId.value || '',
      data: {
        question: obj.question,
        answer: obj.answer,
        target: target,
        files: ob ? ob : '',
        useTranslationDocument: useTranslationDocument.value,
        translationDocumentProcess: translationDocumentProcess.value,
        translationDocumentFinal: translationDocumentFinal.value
      }
    })
    .then(res => {
      limitFile.value = {}
      if (res.status) {
        if (!currentId.value) currentId.value = res.data
        finalIng.value = false
        tranIng.value = false
        fetchChatList(undefined, 'tran').then(() => {
          setTimeout(() => {
            if (asizeRef.value && asizeRef.value.autoSelectFirstItem) {
              asizeRef.value.autoSelectFirstItem()
            }
          }, 100)
        })
        activeIndex.value = 0
      } else {
        finalIng.value = false
        tranIng.value = false
      }
    })
    .catch(err => {
      limitFile.value = {}
      finalIng.value = false
      tranIng.value = false
      console.error('获取回复失败:', err)
    })
}

const postFinal = async (obj, title, ob) => {
  let num = parseInt(localStorage.getItem('count'))
  num = num + 1
  localStorage.setItem('count', num)
  request
    .post('/Message/save', {
      userId: userInfo.value.id,
      type: '总结',
      title: title ? title : obj.question,
      id: limitFinalId.value || currentId.value || '',
      data: {
        question: obj.question,
        answer: {
          key_points: obj.answer.data,
          summary: obj.answer.title
        },
        files: ob ? ob : ''
      }
    })
    .then(res => {
      limitFinalFile.value = {}
      if (res.status) {
        if (!currentId.value) currentId.value = res.data
        finalIng.value = false
        docIng.value = false
        fetchChatList(undefined, 'final').then(() => {
          setTimeout(() => {
            if (asizeRef.value && asizeRef.value.autoSelectFirstItem) {
              asizeRef.value.autoSelectFirstItem()
            }
          }, 100)
        })
        activeIndex.value = 0
      } else {
        finalIng.value = false
        docIng.value = false
      }
    })
    .catch(err => {
      limitFinalFile.value = {}
      console.error('获取回复失败:', err)
    })
}

// 根据pageType确定type的映射表
const getChatTypeFromPageType = (pageTypeValue) => {
  const typeMapping = new Map([
    ['tran', 'tran'],
    ['final', 'final'],
    ['query', 'hr'],
    ['it', 'it'],
    ['law', 'law'],
    ['board', 'board'],
    ['sample', 'sample']
  ])

  return typeMapping.get(pageTypeValue) || ''
}

// 重构并跑函数 用于替换下面的getHistory
const fetchChatList = async (keyword, setPageType) => {
  // 根据当前选择的模式确定type参数
  const type = getChatTypeFromPageType(setPageType || pageType.value)

  // 如果没有传入 keyword，使用当前的搜索关键词
  const searchKeyword = keyword !== undefined ? keyword : currentSearchKeyword.value

  let res = await getChatMsgList(userInfo.value.id, searchKeyword, type)
  if (!res.status) {
    ElMessage.error(res.message)
  }
  chatList.value = JSON.parse(JSON.stringify(res.data))
  if (setPageType) {
    pageType.value = setPageType
  }
}

// 终止请求方法
const cancelCurrentRequest = async val => {
  if (val === 'final') {
    request.cancelRequest(currentRequestUrl.value)
    ElMessage.success('请求已中止')
    finalIng.value = false
    docIng.value = false
    limitTranLoading.value = false
    limitQueryLoading.value = false
    let title = ''
    let ob = ''
    if (!ob && limitFinalFile.value.fileName) {
      ob = limitFinalFile.value
    }
    const obj = {
      question: finalQuest.value,
      answer: {
        title: '',
        data: []
      }
    }
    postFinal(obj, title, ob)
  } else {
    ElMessage.success('请求已中止')
    if (val === 'sample') {
      isSampleLoad.value = false
      limitLoading.value = false
      limitTranLoading.value = false
      limitQueryLoading.value = false
      isSampleStop.value = true
      // 将流式缓冲写回静态数据以便保存
      chatQuery.messages = JSON.parse(JSON.stringify(chatCurrent.messages))
      chatQuery.isLoading = false
      let title = ''
      // 兜底标题：优先用当前问题，其次拼接首条文件名
      if (!title) {
        const firstMsg = chatQuery.messages[0] || {}
        const fileNames = Array.isArray(firstMsg.files)
          ? firstMsg.files.map(f => f.originalFileName).filter(Boolean).join(',')
          : ''
        title = firstMsg.content || fileNames || '新对话'
      }
      // 选择保存用的会话ID：优先 limitId，其次 currentId，最后允许后端创建
      const id = limitId.value || currentId.value || ''
      postSample(id, title.replace(/\([^)]*\)/g, ''), deepType.value)
      limitId.value = ''
    }
    if (val === 'query' || val === 'it' || val === 'law' || val === 'board') {
      isSampleLoad.value = false
      limitLoading.value = false
      limitTranLoading.value = false
      limitQueryLoading.value = false
      isQueryStop.value = true
      queryIng.value = false
      const query = tipQuery.value
      let title = ''

      const paramsQuery = {
        title: title ? title : query,
        queryValue: query
      }
      currentObj.value.messages.type = 'final_answer'
      const isThink = deepType.value === true
      postQuestion(currentObj.value.thinking, { type: 'final_answer' }, paramsQuery, val, isThink)
    }
    if (val === 'tran') {
      finalIng.value = false
      tranIng.value = false
      limitTranLoading.value = false
      limitQueryLoading.value = false
      const query = transQuest.value
      let title = ''
      let obj = ''

      if (!obj && limitFile.value.fileName) {
        obj = limitFile.value
      }
      const passData = {
        question: query,
        answer: currentTransData.value
      }
      const target = selectedLan.value
      transData.value = JSON.parse(JSON.stringify(currentTransData.value))
      isTranStop.value = true
      postTran(passData, title, obj, target)
    }
  }
}
const getPower = () => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo'))
  request
    .post('/Files/permissionCheck?userId=' + userInfo.id)
    .then(res => {
      if (res.status) {
        localStorage.setItem('powerList', JSON.stringify(res.data))
        asizeRef.value.setPower(JSON.stringify(res.data))
      }
    })
    .catch(err => {
      console.error(err)
    })
}
const setlaw = () => {
  isLaw.value = localStorage.getItem('enableLaw')
  enableBoardOffice.value = localStorage.getItem('enableBoardOffice')
  getPower()
}
const handleClickOutside = event => {
  if (wrapperRef.value && !wrapperRef.value.contains(event.target)) {
    showFileMenu.value = false
  }
}
// 组件挂载时订阅事件
// 监听模式变化，重新获取对话列表
watch(
  () => pageType.value,
  (newMode, oldMode) => {
    if (newMode !== oldMode && newMode) {
      // 模式改变时重新获取对话列表
      fetchChatList(undefined, pageType.value)
    }
  }
)

onMounted(() => {
  eventBus.on('submit-sampleFile', submitSampleFile)
  eventBus.on('fetchChatList', (keyword) => {
    // 更新当前搜索关键词
    currentSearchKeyword.value = keyword || ''
    fetchChatList(keyword)
  })
  eventBus.on('checkQueryIngStatus', (callback) => {
    callback(queryIng.value)
  })
  eventBus.on('setCollapsed', collapsedListener)
  updateIsMobile()
  window.addEventListener('resize', updateIsMobile)
  document.addEventListener('click', handleClickOutside)
  nextTick(() => {
    isLaw.value = localStorage.getItem('enableLaw')
    enableBoardOffice.value = localStorage.getItem('enableBoardOffice')
    isNet.value = localStorage.getItem('isNet')
  })
})
// 组件卸载时关闭 SSE 连接
onBeforeUnmount(() => {
  eventBus.off('submit-sampleFile', submitSampleFile)
  eventBus.off('fetchChatList', fetchChatList)
  eventBus.off('checkQueryIngStatus')
  eventBus.off('setCollapsed', collapsedListener)
  window.removeEventListener('resize', updateIsMobile)
  document.removeEventListener('click', handleClickOutside)
})
// 组件卸载时关闭 SSE 连接
onUnmounted(() => {
  if (interval) {
    clearInterval(interval)
  }
})
</script>

<style lang="less">
.upload-layout.drag-over {
  border-color: #409eff;
  background-color: rgba(64, 158, 255, 0.1);
}
.sampleArea {
  .el-textarea__inner {
    padding: 18px 135px 18px 15px !important;
    height: 150px !important;
    min-height: 150px !important;
    max-height: 150px !important;
    overflow-y: auto !important;
    scrollbar-width: thin;
    scrollbar-color: #e5e7eb transparent;
    border-color: #1b6cff !important;
  }
}
.sampleAreaAry {
  .el-textarea__inner {
    padding: 56px 135px 18px 15px !important;
    height: 150px !important;
    min-height: 150px !important;
    max-height: 150px !important;
    overflow-y: auto !important;
    scrollbar-width: thin;
    scrollbar-color: #e5e7eb transparent;
    border-color: #1b6cff !important;
  }
}
.filesList {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  font-size: 12px;
  div {
    margin-left: 10px;

    max-width: 80px;
    white-space: nowrap; /* 禁止换行 */
    overflow: hidden; /* 隐藏溢出内容 */
    text-overflow: ellipsis; /* 超出部分显示... */ /* 必须设置宽度（或父容器有明确宽度） */
    display: flex;
    align-items: center;
    background-color: #f5f5f5;
    padding: 6px 10px;
    border-radius: 6px;
    position: relative;
  }
}
.tooltip-wrapper {
  position: relative;
  display: flex;
  .triangle {
    position: absolute;
    bottom: -9px;
    left: 50%;
    transform: translateX(-50%);
    width: 0;
    height: 0;
    border-left: 10px solid transparent;
    border-right: 10px solid transparent;
    border-top: 10px solid #fff;
  }
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
    color: #333;
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
.button-item_common {
  display: flex;
  width: 100%;
  margin-top: 25px;
  flex-direction: row;
  justify-content: center;
  margin-bottom: 5px;
}
.el-container :deep(.el-radio-button) {
  &.is-active {
    border: none !important;
    outline: none !important;
  }
}

.el-container :deep(.el-radio-button__inner) {
  display: flex;
  align-items: center;
  justify-content: center;
  border: none !important;
  outline: none !important;
}
.el-container {
  background-color: #fff;
}

.el-main {
  padding: 0px !important;
  // background: linear-gradient(
  //   to bottom,
  //   rgba(197, 208, 213, 0.2),
  //   /* 淡蓝色，透明度 60% */ rgba(188, 214, 218, 0.1) /* 更淡的蓝色，透明度 60% */
  // );
  .center-container {
    display: flex;
    align-items: center;
    height: 100%;
    box-sizing: border-box;
    padding-top: 80px;
    display: flex;
    flex-direction: column;
      .query_content {
        width: 100%;
        height: 220px;
        display: flex;
        justify-content: flex-end;
        align-items: center;
        border-radius: 10px;
        flex-direction: column;
        margin-bottom: 10px;
        .custom-input {
          :deep(.el-textarea__inner) {
            border-radius: 6px !important;
            padding: 10px 15px !important;
            resize: none;
            overflow-y: auto !important;
            scrollbar-width: thin;
            scrollbar-color: #e5e7eb transparent;
            border: 1px solid #dce6fa !important;
            box-shadow: none;
          }
          :deep(.el-textarea__inner:focus) {
            border-color: #409eff !important;
            box-shadow: none;
          }
        }
        .question-textarea {
          :deep(.el-textarea__inner) {
            height: 150px !important;
            min-height: 150px !important;
            max-height: 150px !important;
            border-radius: 6px !important;
            padding: 18px 135px 18px 15px !important;
            resize: none;
            overflow-y: auto !important;
            scrollbar-width: thin;
            scrollbar-color: #e5e7eb transparent;
            border: 1px solid #dce6fa !important;
            box-shadow: none;
          }
          :deep(.el-textarea__inner:focus) {
            border-color: #409eff !important;
            box-shadow: none;
          }
          :deep(.el-textarea__inner::-webkit-scrollbar) {
            width: 8px;
            opacity: 1;
          }
          :deep(.el-textarea__inner::-webkit-scrollbar-track) {
            background: transparent;
            border-radius: 16px;
            opacity: 1;
          }
          :deep(.el-textarea__inner::-webkit-scrollbar-thumb) {
            background: #e5e7eb;
            border-radius: 4px;
            border: none;
            opacity: 1;
          }
          :deep(.el-textarea__inner::-webkit-scrollbar-thumb:hover) {
            background: #d1d5db;
            opacity: 1;
          }
        }
        .tran_select {
          width: 862px;
          margin: 0 auto 10px auto;
          display: flex;
        justify-content: flex-start;
        .mode_title {
          padding-right: 10px;
          line-height: 32px;
          font-size: 14px;
          letter-spacing: 1px;
          color: #333;
        }
      }
    }
    .query_source {
      margin-top: 30px;
      padding: 0 10px;
    }
    .href_source {
      margin-top: 10px;
      color: #1b6cff;
      cursor: pointer;
      padding: 0 10px;
      font-size: 14px;
    }
    .query_common {
      margin-top: 20px;
      width: 100%;
      display: flex;
      .query_common_img {
        width: 18px;
        height: 18px;
        cursor: pointer;
      }
    }
    .select_content {
      width: 100%;
      display: flex;
      justify-content: flex-end;
      align-items: center;
      border-radius: 10px;
      flex-direction: column;
      height: 220px;
      margin-bottom: 10px;
      .tran_select {
        width: 862px;
        margin: 0 auto 10px auto;
        display: flex;
        justify-content: flex-start;
        .mode_title {
          padding-right: 10px;
          line-height: 32px;
          font-size: 14px;
          letter-spacing: 1px;
          color: #333;
        }
      }
    }
    .main_content::-webkit-scrollbar {
      width: 1px; /* 滚动条宽度 */
    }
    .main_content::-webkit-scrollbar-track {
      background: #fff; /* 轨道背景颜色 */
      border-radius: 0px; /* 轨道圆角 */
    }
    .main_content::-webkit-scrollbar-thumb {
      background: #fff; /* 轨道背景颜色 */
      border-radius: 0px; /* 滑块圆角 */
      border: 1px solid #fff; /* 滑块边框 */
    }
    .main_content::-webkit-scrollbar-thumb:hover {
      background: #fff; /* 滑块悬停时的颜色 */
    }
    .main_content {
      width: 862px;
      overflow-y: auto;
      overflow-x: hidden;
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      margin-bottom: 10px;
      /* WebKit 浏览器滚动条样式 */

      .sample_item::-webkit-scrollbar {
        width: 1px; /* 滚动条宽度 */
      }
      .sample_item::-webkit-scrollbar-track {
        background: #fff; /* 轨道背景颜色 */
        border-radius: 0px; /* 轨道圆角 */
      }
      .sample_item::-webkit-scrollbar-thumb {
        background: #fff; /* 滑块颜色 */
        border-radius: 0px; /* 滑块圆角 */
        border: 1px solid #fff; /* 滑块边框 */
      }
      .sample_item::-webkit-scrollbar-thumb:hover {
        background: #fff; /* 滑块悬停时的颜色 */
      }
      .sample_item {
        width: 100%;
        display: flex;
        flex-direction: column;
        overflow-y: auto;
        overflow-x: hidden;
        margin-top: 10px;
        scroll-behavior: smooth;
        .sample_chat {
          font-size: 14px;
          letter-spacing: 1px;
          width: 100%;
          .sample_chat_file {
            display: flex;
            flex-wrap: nowrap; /* 不允许换行 */
            gap: 10px 20px; /* 元素间距(可选) */
            justify-content: end; /* 左对齐(默认) */
            flex-direction: row-reverse;
            .item_files {
              display: flex;
              font-size: 14px;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
              padding: 7px 15px;
              color: rgb(51, 51, 51);
              background-color: #eff6ff;
              display: flex;
              align-items: center;
              border-radius: 10px;
              cursor: pointer;
              box-sizing: border-box;
              flex: 1 0 calc(20% - 25px); /* 基础宽度25% 减间距 */
              max-width: calc(20% - 25px); /* 防止内容撑破 */
              .file_name {
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
              }
            }
          }
          .sample_chat_query {
            background-color: #1b6cff;
            border-radius: 10px;
            padding: 13px 15px;
            float: right;
            color: #fff;
            max-width: 600px;
            overflow: hidden;
            text-overflow: ellipsis;
          }
        }
      }
      .tip_load {
        font-size: 12px;
        padding-left: 5px;
        letter-spacing: 1px;
        line-height: 20px;
      }
      .text_item {
        margin-top: 75px;
        display: flex;
        line-height: 30px;
        div {
          width: 100%;
          padding-left: 3px;
        }
        .title_src {
          margin-right: 4px;
          width: 30px;
          height: 30px;
          .title_final {
            width: 100%;
            padding-left: 3px;
          }
        }
      }
      .title_float {
        width: calc(100% - 10px);
        font-size: 13px;
        display: flex;
        padding: 0px 10px;
        letter-spacing: 1px;
        line-height: 24px;
        color: #666;
      }
      .title_tiQuery {
        display: flex;
        flex-direction: row-reverse;
        width: 100%;
        margin-top: 80px;
        .title_tiQuery_text {
          background-color: #1b6cff;
          border-radius: 10px;
          padding: 13px 15px;
          float: right;
          color: #fff;
          max-width: 600px;
          overflow: hidden;
          text-overflow: ellipsis;
          font-size: 14px;
          letter-spacing: 1px;
        }
      }
      .title {
        margin-bottom: 10px;
        display: flex;
        width: 100%;
        justify-content: center;
        .title_src {
          width: 60px;
          height: 60px;
          margin-right: 15px;
        }
        .title_top {
          font-size: 20px;
          color: #262626;
        }
        .title_item {
          height: 20px;
          line-height: 20px;
          margin-top: 7px;
          display: flex;
          line-height: 18px;
          color: #262626;
        }
      }
      .title_wait {
        margin-top: 10px;
        letter-spacing: 1px;
        font-size: 12px;
        line-height: 20px;
      }
      .title_final_tip {
        width: 100%;
        display: flex;
        flex-direction: row-reverse;
        font-size: 14px;
        letter-spacing: 1px;
        .title_final_query {
          background-color: #1b6cff;
          border-radius: 10px;
          float: right;
          color: #fff;
          max-width: 600px;
          overflow: hidden;
          text-overflow: ellipsis;
          letter-spacing: 1px;
          font-size: 14px;
          line-height: 24px;
        }
      }
      .title_final_data {
        margin-top: 10px;
        background-color: #fafafa;
        font-size: 14px;
        letter-spacing: 1px;
        line-height: 24px;
        border-radius: 10px;
        min-width: 696px;
      }
      .title_tran_tip {
        width: 100%;
        display: flex;
        flex-direction: row-reverse;
        div {
          background-color: #1b6cff;
          border-radius: 10px;
          float: right;
          color: #fff;
          max-width: 600px;
          line-height: 24px;
          overflow: hidden;
          text-overflow: ellipsis;
          font-size: 14px;
          letter-spacing: 1px;
        }
      }
      .title_tran_data {
        margin-top: 10px;
        background-color: #fafafa;
        font-size: 14px;
        letter-spacing: 1px;
        line-height: 24px;
        border-radius: 10px;
        min-width: 696px;
        will-change: contents;
        contain: content;
      }
      /* 知识库内联引用链接样式覆盖（小圆角、蓝色背景、无边框、高度20px） */
      .normal-text a {
        height: 20px !important;
        line-height: 20px !important;
        padding: 0 8px !important;
        border-radius: 2px !important;
        background: #e6f4ff !important;
        color: #1b6cff !important;
        font-size: 12px !important;
        vertical-align: middle !important;
        position: relative;
        top: -2px;
        border: none !important;
        box-shadow: none !important;
        text-decoration: none !important;
      }
      .normal-text a:hover,
      .normal-text a:active {
        background: #d0e4ff !important;
        color: #1b6cff !important;
        border: none !important;
        box-shadow: none !important;
      }
      .normal-text a:visited {
        color: #1b6cff !important;
      }
      .normal-text a:focus-visible {
        outline: none !important;
        box-shadow: none !important;
      }
      .content_list {
        display: flex;
        margin-top: 20px;
        height: 300px;
        width: 100%;
        .list_item {
          flex: 1;
          height: 326px;
          background-image: url('@/assets/arr.png');
          background-size: 100% 100%;
          // background: linear-gradient(
          //   to bottom,
          //   rgba(135, 206, 235, 0.3),
          //   /* 淡蓝色，透明度 60% */ rgba(224, 247, 250, 0.3) /* 更淡的蓝色，透明度 60% */
          // );
          .list_title {
            padding-left: 20px;
            font-size: 18px;
            color: #262626;
            font-weight: bold;
            margin-top: 25px;
            letter-spacing: 1px;
          }
          .list_tip {
            padding-left: 20px;
            font-size: 14px;
            color: #646464;
            margin-top: 4px;
            letter-spacing: 1px;
          }
          .list_arry {
            padding-left: 20px;
            margin-top: 20px;
            color: #252525;
            font-size: 14px;
            line-height: 20px;
            .arr_item {
              line-height: 36px;
              cursor: pointer;
              display: flex;
              .item_hover {
                padding-left: 3px;
                display: -webkit-box;
                -webkit-line-clamp: 1; /* 显示行数 */
                -webkit-box-orient: vertical;
                overflow: hidden;
              }
            }
            .arr_item:hover {
              color: #1b6cff;
            }
          }
          .img_list {
            display: flex;
            flex-direction: column;
            padding: 25px 20px;
            width: 100%;
            box-sizing: border-box;
            .img_item:hover {
              border: 1px solid #1b6cff;
            }
            .img_item {
              display: flex;
              background-color: #fff;
              border-radius: 10px;
              height: 56px;
              cursor: pointer;
              flex-direction: row;
              border: 1px solid #e6f2ff;

              .image {
                width: 40px;
                height: 40px;
                margin-top: 8px;
                margin-left: 12px;
                img {
                  width: 100%;
                  height: 100%;
                }
              }
              .img_text {
                height: 30px;
                padding-left: 12px;
                padding-top: 12px;
                letter-spacing: 1px;
                .text_title {
                  font-size: 14px;
                  color: #252525;
                  font-weight: bold;
                  line-height: 18px;
                }
                .text_content {
                  font-size: 12px;
                  line-height: 15px;
                  max-width: 220px;
                  color: #646464;
                  white-space: nowrap; /* 强制文本不换行 */
                  overflow: hidden; /* 隐藏超出容器的内容 */
                  text-overflow: ellipsis; /* 超出部分显示省略号 */
                }
              }
            }
          }
        }
      }
    }
  }
}

.container {
  height: 100vh;
  font-family: 'Source Han Sans CN';
}

.el-aside {
  overflow-x: hidden;
}

.custom-tooltip {
  max-width: 500px !important;
}

.textarea {
  width: 862px;
  position: relative;
  /* 去掉 textarea 右下角的小图标 */
  .custom-input textarea {
    resize: none; /* 禁用调整大小功能 */
    height: 150px !important;
    min-height: 150px !important;
    max-height: 150px !important;
    border-color: #1b6cff !important;
    overflow-y: auto !important;
    scrollbar-width: thin;
    scrollbar-color: #e5e7eb transparent;
  }

  .send-icon {
    position: absolute;
    right: 20px;
    bottom: 13px;
    cursor: pointer;
    transition: color 0.3s;
    display: flex;
  }
  .send-icon img {
    width: 30px;
    height: 30px;
  }
}

.mobile-menu-icon {
  position: fixed;
  top: 16px;
  z-index: 1000;
  img {
    width: 24px;
    height: 24px;
  }
}

.mobile-overlay {
  position: fixed;
  top: 0;
  left: 236px;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 999;
}

/* 光标闪烁动画 */
@keyframes blink {
  50% {
    border-color: transparent;
  }
}

@media (max-width: 768px) {
  .container {
    padding-bottom: 60px;
  }
}
</style>

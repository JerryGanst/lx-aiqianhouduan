<script setup lang="ts">
import { computed, nextTick, ref, watch, watchEffect } from 'vue'

import FileCard from './FileCard.vue'
import waitingIcon from '@/assets/agent/waiting.svg'
import sanjiaoIcon from '@/assets/agent/sanjiao.svg'
import { getFileImgByExtension } from '@/utils/common.js'
import { useShared } from '@/utils/useShared.js'

type ResumeAttachment = {
  type: 'jobJd' | 'resume'
  file: Record<string, any>
}

type FileCardItem = {
  fileName: string
  fileInfo: string
  imageUrl?: string
  fileId: string | null
  rawFile: Record<string, any> | null
}

const resumeItemStates = ref<boolean[]>([])

const toggleResumeItem = (index: number) => {
  if (index < 0 || index >= resumeItemStates.value.length) return
  resumeItemStates.value[index] = !resumeItemStates.value[index]
}

const emit = defineEmits<{
  (e: 'preview-file', file: Record<string, any>): void
}>()

const { intelCurrent, resumeTaskState, userInputContent, isNavigatingFromAgentList } = useShared()

const fileTypeMap: Record<string, string> = {
  doc: 'Word',
  docx: 'Word',
  xls: 'Excel',
  xlsx: 'Excel',
  ppt: 'PPT',
  pptx: 'PPT',
  pdf: 'PDF',
  txt: 'TXT'
}

const extractExtension = (file: Record<string, any>) => {
  const name: string = file?.originalFileName || file?.fileName || ''
  const ext = name.split('.').pop()
  return ext ? ext.toLowerCase() : ''
}

const formatFileType = (extension: string) => {
  if (!extension) return ''
  return fileTypeMap[extension] || extension.toUpperCase()
}

const extractFileSize = (file: Record<string, any>) => {
  const sizeCandidate = [file?.fileSize, file?.size, file?.file_size, file?.fileLength]
    .map(value => (typeof value === 'string' ? Number(value) : value))
    .find(value => typeof value === 'number' && !Number.isNaN(value))

  return sizeCandidate ?? 0
}

const formatFileSize = (size: number) => {
  if (!size) return ''
  if (size < 1024) {
    return `${size}B`
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)}KB`
  }

  return `${(size / (1024 * 1024)).toFixed(1)}MB`
}

const resolveFileId = (file: Record<string, any>) => {
  if (!file || typeof file !== 'object') return null
  const fileIdValue = (file as Record<string, any>).fileId

  if (typeof fileIdValue === 'string') {
    return fileIdValue
  }

  if (fileIdValue && typeof fileIdValue === 'object') {
    if (typeof fileIdValue.fileId === 'string') {
      return fileIdValue.fileId
    }
    if (typeof fileIdValue.id === 'string') {
      return fileIdValue.id
    }
  }

  if (typeof (file as Record<string, any>).id === 'string') {
    return (file as Record<string, any>).id
  }

  if (typeof (file as Record<string, any>).file_id === 'string') {
    return (file as Record<string, any>).file_id
  }

  return null
}

const buildFileCard = (file: Record<string, any>): FileCardItem | null => {
  if (!file) return null
  const extension = extractExtension(file)
  const typeLabel = formatFileType(extension)
  const sizeLabel = formatFileSize(extractFileSize(file))
  const infoParts = [typeLabel, sizeLabel].filter(Boolean)

  return {
    fileName: file?.originalFileName || file?.fileName || '',
    fileInfo: infoParts.join('·'),
    imageUrl: getFileImgByExtension({
      extension,
      fileUrl: file?.fileUrl
    }),
    fileId: resolveFileId(file),
    rawFile: file
  }
}

const lastUserMessage = computed(() => {
  const messages = intelCurrent?.messages || []
  for (let i = messages.length - 1; i >= 0; i--) {
    const message = messages[i]
    if (message?.role === 'user') {
      return message
    }
  }
  return null
})

const stateJdFile = ref<Record<string, any> | null>(null)
const stateResumeFiles = ref<Record<string, any>[]>([])

watchEffect(() => {
  stateJdFile.value = resumeTaskState?.jdFile || null
})

watchEffect(() => {
  const filesFromState = resumeTaskState?.resumeFiles
  stateResumeFiles.value = Array.isArray(filesFromState) ? [...filesFromState] : []
})

const resolvedAttachments = computed<ResumeAttachment[]>(() => {
  const message = lastUserMessage.value as Record<string, any> | null
  if (!message) return []

  const messageAttachments = Array.isArray(message?.resumeAttachments) ? message.resumeAttachments : []

  if (messageAttachments.length > 0) {
    return messageAttachments
      .filter((item: Record<string, any>) => item && item.file)
      .map((item: Record<string, any>) => ({
        type: item.type,
        file: item.file
      })) as ResumeAttachment[]
  }

  const files = Array.isArray(message?.files) ? message.files : []
  return files.map((file: Record<string, any>, index: number) => ({
    type: index === 0 ? 'jobJd' : 'resume',
    file
  })) as ResumeAttachment[]
})

const jdFileCard = computed<FileCardItem | null>(() => {
  const fileFromState = stateJdFile.value
  return buildFileCard(fileFromState)
})

const resumeFileCards = computed<FileCardItem[]>(() => {
  return stateResumeFiles.value
    .map(file => buildFileCard(file))
    .filter((item): item is FileCardItem => Boolean(item))
})

const handleFileCardPreview = (file: Record<string, any> | null | undefined) => {
  if (!file || typeof file !== 'object') return
  emit('preview-file', file)
}

const syncUserInputContent = () => {
  if (isNavigatingFromAgentList.value) {
    isNavigatingFromAgentList.value = false
    return
  }
  const messageContent = lastUserMessage.value?.content
  if (typeof messageContent === 'string' && messageContent.trim()) {
    userInputContent.value = messageContent
  } else {
    userInputContent.value = resumeTaskState.text || ''
  }

  const attachments = Array.isArray(lastUserMessage.value?.resumeAttachments)
    ? lastUserMessage.value.resumeAttachments
    : []

  if (attachments.length > 0) {
    const jdAttachment = attachments.find(
      (item: Record<string, any>) => item?.type === 'jobJd' && item?.file
    ) as ResumeAttachment | undefined

    const resumeAttachmentFiles = attachments
      .filter((item: Record<string, any>) => item?.type === 'resume' && item?.file)
      .map((item: Record<string, any>) => item.file)

    resumeTaskState.jdFile = jdAttachment ? jdAttachment.file : null
    resumeTaskState.resumeFiles = resumeAttachmentFiles
  }
}

watch(lastUserMessage, syncUserInputContent, { immediate: true })

watch(
  () => resumeTaskState.text,
  newText => {
      userInputContent.value = typeof newText === 'string' ? newText : ''
  },
  { immediate: true }
)
const showWaitingTips = computed(() => resumeTaskState.isWaiting && !resumeTaskState.isCompleted)
const showCompletedTips = computed(() => resumeTaskState.isWaiting && resumeTaskState.isCompleted)
const resumeRankingList = computed(() => {
  const rankingFromState = Array.isArray(resumeTaskState?.ranking) ? resumeTaskState.ranking : []

  if (rankingFromState.length > 0) {
    return rankingFromState
  }

  const rankingResult = resumeTaskState?.rankingResult
  if (rankingResult && Array.isArray(rankingResult.ranking)) {
    return rankingResult.ranking
  }

  return []
})

watch(
  resumeRankingList,
  newList => {
    resumeItemStates.value = newList.map(() => true)
  },
  { immediate: true }
)

const formatConcerns = (concerns: unknown) => {
  if (Array.isArray(concerns)) {
    return concerns.filter(item => typeof item === 'string' && item.trim()).join('，')
  }
  if (typeof concerns === 'string') {
    return concerns
  }
  return ''
}

const formatScore = (score: unknown) => {
  if (typeof score === 'number') {
    return Number.isFinite(score) ? score.toFixed(2) : ''
  }
  if (typeof score === 'string') {
    return score
  }
  return ''
}

const pickSummaryValue = (...values: unknown[]) => {
  for (const value of values) {
    if (typeof value === 'string' && value.trim()) {
      return value
    }
  }
  return ''
}

const executiveSummary = computed(() =>
  pickSummaryValue(
    resumeTaskState.executiveSummary,
    resumeTaskState?.rankingResult?.executive_summary,
    resumeTaskState?.rankingResult?.executiveSummary
  )
)

const panelSummary = computed(() =>
  pickSummaryValue(
    resumeTaskState.panelSummary,
    resumeTaskState?.rankingResult?.panel_summary,
    resumeTaskState?.rankingResult?.panelSummary
  )
)

const showSummaryContent = computed(() => !!(executiveSummary.value || panelSummary.value))

const resultListRef = ref<HTMLElement | null>(null)

const scrollResultListToBottom = async () => {
  await nextTick()
  const element = resultListRef.value
  if (!element) return

  requestAnimationFrame(() => {
    element.scrollIntoView({ behavior: 'smooth', block: 'end' })
  })
}

watch(
  showCompletedTips,
  value => {
    if (value) {
      scrollResultListToBottom()
    }
  },
  { immediate: true }
)

watch(resumeRankingList, () => {
  if (showCompletedTips.value) {
    scrollResultListToBottom()
  }
})
</script>

<template>
  <div class="head_robot">
    <div class="left_img">
      <img src="@/assets/knowledgeBase/robot.png" style="width: 52px; height: 52px" />
    </div>
    <div class="right_content">Hi，我是你的 简历助手 ，请上传岗位JD文件/输入岗位JD描述，并且上传简历文件后筛选简历</div>
  </div>
  <div class="jd_file_msg" v-if="jdFileCard">
    <FileCard
      class="file-card-right"
      :image-url="jdFileCard.imageUrl"
      :file-name="jdFileCard.fileName"
      :file-info="jdFileCard.fileInfo"
      :file-id="jdFileCard.fileId || undefined"
      @preview="handleFileCardPreview(jdFileCard.rawFile)"
    />
  </div>
  <div class="resume_files" v-if="resumeFileCards.length">
    <FileCard
      v-for="(file, index) in resumeFileCards"
      :key="`${file?.fileName || ''}-${index}`"
      class="file-card-right"
      :image-url="file?.imageUrl"
      :file-name="file?.fileName"
      :file-info="file?.fileInfo"
      :file-id="file?.fileId || undefined"
      @preview="handleFileCardPreview(file?.rawFile)"
    />
  </div>
  <div class="my_input" v-if="userInputContent">
    {{ userInputContent }}
  </div>
  <div class="waiting_tips" v-if="showWaitingTips">
    <div class="waiting_tips_img">
      <img :src="waitingIcon" alt="等待中" />
    </div>
    <span>正在筛选中，该过程需要 3 - 5分钟，请耐心等候...</span>
  </div>
  <div class="waiting_tips" v-if="showCompletedTips" style="transform: translateX(25px)">已完成筛选</div>
  <div class="summary_content" v-if="showSummaryContent">
    <div class="high_summary">
      <span>高层摘要：</span>
      {{ executiveSummary }}
    </div>
    <div class="review_summary">
      <span>评审摘要：</span>
      {{ panelSummary }}
    </div>
  </div>
  <div class="result_list" ref="resultListRef">
    <div class="resume_item" v-for="(ob, index) in resumeRankingList" :key="ob?.resume_id || index">
      <div class="open_close_img" @click="toggleResumeItem(index)">
        <img :src="sanjiaoIcon" alt="展开折叠图标" :class="['toggle-icon', { 'is-open': resumeItemStates[index] }]" />
      </div>
      <div class="right_resume_content" :class="{ 'is-open': resumeItemStates[index] }">
        <div class="rank paragraph">
          <span>排名：</span>
          {{ ob?.rank ?? '' }}
        </div>
        <div class="resume_name paragraph">
          <span>候选人姓名：</span>
          {{ ob?.name ?? '' }}
        </div>
        <div class="final_score paragraph">
          <span>综合得分：</span>
          {{ formatScore(ob?.final_score) }}
        </div>
        <div class="high_lights paragraph">
          <div class="light" v-for="(highlight, highlightIndex) in ob?.highlights || []" :key="highlightIndex">
            <span>● 亮点 {{ highlightIndex + 1 }}：</span>
            {{ highlight }}
          </div>
        </div>
        <div class="concerns paragraph">
          <span>主要关注点：</span>
          {{ formatConcerns(ob?.concerns) }}
        </div>
        <div class="reasons paragraph">
          <span>评价：</span>
          {{ ob?.reasons ?? '' }}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
.light {
  margin-left: 30px;
}
.paragraph {
  span {
    font-weight: 700;
  }
}
.open_close_img {
  width: 24px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.review_summary {
  margin-top: 10px;
}
.toggle-icon {
  width: 12px;
  height: 12px;
  transform: rotate(-90deg);
  transition: transform 0.2s ease;
}

.toggle-icon.is-open {
  transform: rotate(0deg);
}

.resume_item {
  display: flex;
  margin-top: 10px;
}
.summary_content {
  width: 835px;
  border-radius: 10px;
  background: #fafafa;
  border: 1px solid #dedede;
  font-size: 16px;
  color: #333333;
  font-weight: 400;
  text-align: left;
  padding: 10px;
  box-sizing: border-box;
  line-height: 22px;
  transform: translateX(24px);
  margin-top: 10px;
  span {
    font-weight: 700;
  }
}
.right_resume_content {
  width: 835px;
  border-radius: 10px;
  background: #fafafa;
  border: 1px solid #dedede;
  font-size: 16px;
  color: #333333;
  font-weight: 400;
  text-align: left;
  padding: 10px;
  box-sizing: border-box;
  line-height: 22px;
  max-height: 38px;
  overflow: hidden;
  transition: max-height 0.2s ease;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}
.right_resume_content.is-open {
  max-height: none;
  display: block;
  -webkit-line-clamp: initial;
}
.waiting_tips {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  font-weight: 400;
  font-size: 16px;
  text-align: left;
  color: #7f7f7f;
}
.waiting_tips_img {
  width: 14px;
  height: 14px;
  display: flex;
  align-items: center;
  justify-content: center;

  img {
    width: 14px;
    height: 14px;
    animation: waiting-rotate 1.2s linear infinite;
  }
}

@keyframes waiting-rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
.head_robot {
  margin-top: 56px;
  display: flex;
}
.resume_files {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
  margin-left: auto;
}
.right_content {
  margin-left: 10px;
  color: #333333;
  font-size: 16px;
  font-weight: 400;
  border-radius: 10px;
  background: #fafafa;
  width: 702px;
  height: 44px;
  text-align: center;
  line-height: 44px;
}

.file-card-right {
  margin: 0;
}
.jd_file_msg {
  margin-left: auto;
}
.my_input {
  margin-top: 10px;
  margin-left: auto;
  max-width: 641px;
  border-radius: 10px;
  background: #eff6ff;
  font-weight: 400;
  font-size: 16px;
  text-align: left;
  color: #333;
  padding: 10px;
  box-sizing: border-box;
}
</style>

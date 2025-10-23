<script setup lang="ts">
import { ref, watch } from 'vue'
import FileUploadComponent from '@/pages/main/component/agent/fileUploadComponent.vue'
import FilePreUpload from '@/pages/main/component/filePreModal.vue'
import { useShared } from '@/utils/useShared'

type UploadedFile = Record<string, any>

const emit = defineEmits(['add-job-description-file', 'add-resume-file', 'submit-resume'])

const jobDescription = ref('')
const jobDescriptionFiles = ref<UploadedFile[]>([])
const resumeFiles = ref<UploadedFile[]>([])
const jobDescriptionFormatTips = '支持格式 .txt,.doc,.docx, 文件大小不超过20MB'

const { fileAry, jobJdFile, fileInputAry, intelQuestion, updateJobJdFile, updateFileInputAry } = useShared()

const filePreRef = ref<InstanceType<typeof FilePreUpload> | null>(null)

const resolveUploadedFileId = (file: UploadedFile | null | undefined): string | null => {
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

const isSameUploadedFile = (source: UploadedFile, target: UploadedFile) => {
  const sourceId = resolveUploadedFileId(source)
  const targetId = resolveUploadedFileId(target)

  if (sourceId && targetId) {
    return sourceId === targetId
  }

  const sourceName =
    (source?.originalFileName || source?.fileName || '').toString().toLowerCase()
  const targetName =
    (target?.originalFileName || target?.fileName || '').toString().toLowerCase()

  return !!sourceName && sourceName === targetName
}

watch(
  () => jobJdFile.value,
  newFile => {
    jobDescriptionFiles.value = newFile ? [newFile as UploadedFile] : []
  },
  { immediate: true, deep: true }
)

watch(
  () => fileInputAry.value,
  newFiles => {
    resumeFiles.value = Array.isArray(newFiles) ? [...(newFiles as UploadedFile[])] : []
  },
  { immediate: true, deep: true }
)

const handleAddJobDescriptionFile = (_source: string, _payload: unknown) => {
  emit('add-job-description-file')
}

const handleRemoveJobDescriptionFile = (_file: UploadedFile) => {
  updateJobJdFile(null)
}

const previewUploadedFile = (file: UploadedFile | null | undefined) => {
  if (!file || typeof file !== 'object') {
    return
  }

  fileAry.value = []

  if (Array.isArray(fileAry.value)) {
    fileAry.value.push(file)
  } else {
    fileAry.value = [file]
  }

  filePreRef.value?.openFile('sample')
}

const handlePreviewJobDescriptionFile = (file: UploadedFile) => {
  previewUploadedFile(file)
}

const handleAddResumeFile = (_source: string, _payload: unknown) => {
  emit('add-resume-file')
}

const handleRemoveResumeFile = (file: UploadedFile) => {
  const currentFiles = Array.isArray(fileInputAry.value) ? fileInputAry.value : []
  const filtered = currentFiles.filter(item => !isSameUploadedFile(item, file))
  updateFileInputAry(filtered)
}

const handlePreviewResumeFile = (file: UploadedFile) => {
  previewUploadedFile(file)
}

const handleCancel = () => {
  jobDescription.value = ''
  jobDescriptionFiles.value = []
  resumeFiles.value = []
  updateJobJdFile(null)
  updateFileInputAry([])
}

const handleSubmit = () => {
  emit('submit-resume')
}
</script>

<template>
  <div class="send-resume-msg">
    <div class="jd-section">
      <div class="section-title">岗位描述JD</div>
      <div class="textarea">
        <el-input
          v-model="intelQuestion"
          placeholder="请输入岗位描述JD,换行请按下Shift+Enter"
          style="width: 100%"
          class="custom-input"
          clearable
          type="textarea"
          :rows="6"
        />
      </div>
    </div>
    <div class="upload-wrapper jd-upload">
      <FileUploadComponent
        title="岗位描述JD文件上传"
        :format-tips="jobDescriptionFormatTips"
        :files="jobDescriptionFiles"
        local-type="jobJd"
        :auto-open-local-upload="true"
        @addFile="handleAddJobDescriptionFile"
        @removeFile="handleRemoveJobDescriptionFile"
        @preview-file="handlePreviewJobDescriptionFile"
      />
    </div>
    <div class="upload-wrapper resume-upload">
      <FileUploadComponent
        :is-required="true"
        title="简历文件上传"
        :files="resumeFiles"
        :auto-open-local-upload="true"
        @addFile="handleAddResumeFile"
        @removeFile="handleRemoveResumeFile"
        @preview-file="handlePreviewResumeFile"
      />
    </div>
    <div class="action-buttons">
      <button class="action-button cancel-button" @click="handleCancel">取消</button>
      <button class="action-button submit-button" @click="handleSubmit">提交</button>
    </div>
  </div>
  <FilePreUpload ref="filePreRef" />
</template>

<style scoped lang="less">
.send-resume-msg {
  .jd-section {
    margin-top: 50px;
    .section-title {
      font-size: 16px;
      color: #333;
      font-weight: 500;
      margin-bottom: 16px;
      margin-left: 5px;
    }
    .textarea {
      width: 100%;
      position: relative;
      .custom-input {
        :deep(.el-textarea__inner) {
          border-radius: 10px;
          border: 1px solid #d9d9d9;
          background: #fff;
          padding: 18px 100px 18px 15px !important;
          resize: none;
          font-size: 16px;
          line-height: 24px;
          scrollbar-width: thin;
          scrollbar-color: #e5e7eb transparent;
        }
        :deep(.el-textarea__inner:focus) {
          border-color: #1b6cff;
          box-shadow: 0 0 0 2px rgba(27, 108, 255, 0.1);
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
        }
      }
    }
  }
  .upload-wrapper {
    margin-top: 0;
    :deep(.file_component) {
      margin-top: 0;
    }
  }
  .jd-upload {
    margin-top: 10px;
  }
  .resume-upload {
    margin-top: 30px;
  }
  .action-buttons {
    margin-top: 76px;
    display: flex;
    justify-content: center;
    gap: 16px;
  }
  .action-button {
    width: 274px;
    height: 46px;
    border-radius: 6px;
    font-size: 16px;
    border: 1px solid transparent;
    cursor: pointer;
  }
  .cancel-button {
    background: #fff;
    border-color: #dedede;
    color: #333;
  }
  .submit-button {
    background: #1b6cff;
    color: #fff;
  }
}
</style>

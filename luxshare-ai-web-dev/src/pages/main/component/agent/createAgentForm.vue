<template>
  <div class="outer_container">
    <!-- 非常重要 左侧的宽度决定了右侧能展示多宽 -->
    <div class="create_main" :style="{ width: isPre ? '1138px' : '100%' }">
      <div class="create_title">
      <span class="create_back" @click="$emit('cancel')">
        <img src="../../../../assets/return.png" />
      </span>
        <span style="padding-left: 12px">{{ type === PageType.EDIT_PAGE ? '编辑智能体' : '创建智能体' }}</span>
      </div>
      <div class="create_content">
        <div class="agentImg">
          <el-upload
            :action="uploadUrl"
            list-type="picture-card"
            :limit="1"
            :on-exceed="exceedHandler"
            :on-success="successHandler"
            :on-preview="previewHandler"
            :before-upload="beforeAvatarUpload"
            :on-remove="removeHandler"
          >
            <div class="addIcon" />
            <img :src="headImgUrl" alt="" style="width: 100%; height: 100%" />
          </el-upload>

          <el-dialog v-model="dialogVisible">
            <img :src="dialogImageUrl" alt="Preview Image" style="width: 100%; height: 100%" />
          </el-dialog>
        </div>
        <div class="create_name">
          <div class="create_text">
            <span style="color: #ff4d4f">*</span>
            <span style="padding-left: 5px">智能体名称</span>
          </div>
          <div class="create_input">
            <el-input v-select-all-on-ctrl-a placeholder="给您的智能体取个名字吧" style="width: 100%" v-model="formIntel.name" maxlength="15">
              <template #suffix>
                <span class="char-counter">{{ formIntel.name.length }}/15</span>
              </template>
            </el-input>
          </div>
        </div>
        <div class="create_set">
          <div class="create_header">
            <div class="create_text">
              <span style="color: #ff4d4f">*</span>
              <span style="padding-left: 5px">智能体设定</span>
            </div>
            <div @click="$emit('add-intel')" :class="isComputed ? 'create_loading' : 'create_ai'">
              {{ isComputed ? '停止' : '智能补充' }}
            </div>
          </div>
          <div class="create_input">
            <el-input v-select-all-on-ctrl-a
              :placeholder="placeholderText"
              style="width: 100%; white-space: pre; font-family: monospace"
              v-model="formIntel.description"
              type="textarea"
            ></el-input>
          </div>
        </div>
        <div class="create_name">
          <FileUploadComponent
            :files="formIntel.files"
            @addFile="addFile"
            @removeFile="removeFile"
            @preview-file="previewFile"
          />
        </div>
        <div class="create_name">
          <div class="create_text">
            <span style="padding-left: 5px">智能体简介</span>
          </div>
          <div class="create_input">
            <el-input v-select-all-on-ctrl-a
              placeholder="(选填) 简单地介绍你的智能体吧"
              style="width: 100%"
              v-model="formIntel.introduction"
              maxlength="50"
            >
              <template #suffix>
                <span class="char-counter">{{ formIntel.introduction.length }}/50</span>
              </template>
            </el-input>
          </div>
        </div>
        <div class="create_btn">
          <div class="create_cancel" @click="$emit('cancel')">取消</div>
          <div class="create_confirm" @click="$emit('create', 'create')" v-if="type === PageType.CREATE_PAGE">创建</div>
          <div class="create_confirm" @click="$emit('create', 'edit')" v-if="type === PageType.EDIT_PAGE">保存</div>
        </div>
      </div>
    </div>
    <UploadPreviewArea
      :is-pre="isPre"
      :overlay-width="overlayWidth"
      :preview-file-id="previewFileId"
      :file-info="fileInfo"
      :is-local="fileInfo.isLocal"
      :preview-type="previewType"
      :preview-content="previewContent"
      :is-xls="isXls"
      :loading="false"
      :file-loading="fileLoading"
      @closePre="closePre"
      @downloadFile="downloadFile"
      @update:overlayWidth="val => overlayWidth = val"
    />
  </div>
  <commonUploadModal ref="commonUploadModals"></commonUploadModal>
  <FileUpload ref="fileRefs"></FileUpload>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAgentImgByObj } from '@/api/agent/actions.js'
import {PageType} from '../../../../utils/common.js'
import { watchEffect } from 'vue-demi'
import {useShared} from '@/utils/useShared.js'
import headImg from '@/assets/agent/head.png'
import FileUpload from '@/pages/main/component/fileUploadModal.vue'
import CommonUploadModal from "@/pages/main/component/commonUploadModal.vue"
import {DEFAULT_AGENT_TYPE} from "@/utils/constants.js";
import eventBus from "@/utils/eventBus.js";
import {getTextAfterLastDot} from '@/utils/common.js'
import FileUploadComponent from '@/pages/main/component/agent/fileUploadComponent.vue'
import UploadPreviewArea from '@/pages/main/component/files/UploadPreviewArea.vue'
import { downloadFile, FILE_HANDLERS } from '@/utils/files/fileHandlers.js'

const commonUploadModals = ref(null)
const fileRefs = ref(null)

const {currentAgentType} = useShared()
const isPre = ref(false)
// 暂时没有什么用法
const overlayWidth = ref(0)

let props = defineProps({
  formIntel: Object,
  type: Number,
  isComputed: Boolean
})

defineEmits(['cancel', 'create', 'add-intel'])

const placeholderText = ref(`# 设定
你是一位营销文案奇才，擅长通过对话引导用户明确其产品或服务需求，并能创作出既幽默诙谐又信息准确、吸引力十足的广告语、宣传文案和社交媒体内容。

#  技能
## 技能1：需求挖掘与沟通
- 通过提问和互动，帮助用户清晰定义他们的产品特性和目标受众。
- 识别用户的核心价值主张，并将其转化为文案的关键信息。

## 技能2：创意文案制作
- 根据用户需求，运用独特的幽默感和诙谐风格撰写广告语和宣传文案。
- 创作适合不同社交媒体平台的内容，确保文案在吸引注意力的同时，传递有效信息。

### 技能3：内容适应性
- 能够根据不同平台的特性（如Instagram的视觉焦点，Twitter的短文魅力，Facebook的互动性等）定制文案。
- 保证文案在保持幽默风格的同时，符合各平台的社区准则和用户偏好。

## 限制与注意事项
- 文案内容需保持正面、合法且尊重用户的品牌形象。
- 在幽默表达中避免冒犯或不适当的言辞，确保文案的广泛接受度。
- 确保文案的原创性，不侵犯任何知识产权。
- 在必要时，可以调用搜索引擎或知识库以获取行业趋势和流行话题，增强文案的相关性和时效性。`)

const dialogImageUrl = ref('')
const dialogVisible = ref(false)
// 做个标识是否是图片上传完成的状态
const finishedUploadHead = ref(false)

const exceedHandler = () => {
  ElMessage({
    type: 'warning',
    message: '只能上传一张智能体头像'
  })
}

const fileLoading = ref(false)
const previewFileId = ref(null)
const fileInfo = ref({})
const isXls = ref(false)
const previewContent = ref(null)
const previewType = ref('')
const previewFile = (file) => {
  fileLoading.value = true
  fetch(import.meta.env.VITE_API_BASE_URL + '/Files/getFileById', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      fileId: file.fileId,
      local: file.isLocal
    })
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
      const newFile = new File([blob], originalFileName, { type: blob.type })
      const fileOther = {
        raw: newFile,
        uid: newFile.lastModified,
        size: newFile.size,
        name: decodeURIComponent(file.fileName),
        extension: getTextAfterLastDot(file.fileName),
        cancel: null,
        source: null,
        fileId: file.fileId,
        isLocal: file.isLocal
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

const handlePreview = async (file) => {
  if (!file) return

  const fileExtension = getTextAfterLastDot(file.name)
  fileLoading.value = true

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

const successHandler = async response => {
  if (!response.status) {
    props.formIntel.agentPic = ''
    finishedUploadHead.value = true
    showOrHideHeaderImg('none')
    ElMessage.warning(response.message)
    return
  }
  let objectName = response.data
  let imgUrlResult = await getAgentImgByObj(objectName)
  if (imgUrlResult.status) {
    dialogImageUrl.value = imgUrlResult.data
    props.formIntel.agentPic = objectName
    finishedUploadHead.value = true
    showOrHideHeaderImg('none')
  }
}

const removeHandler = () => {
  const addIcon = document.querySelector('.addIcon')
  addIcon.style.bottom = '0'
  props.formIntel.agentPic = ''
  showOrHideHeaderImg('block')
}

const previewHandler = () => {
  dialogVisible.value = true
}

const beforeAvatarUpload = rawFile => {
  if (!rawFile.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件！')
    return false
  } else if (rawFile.size / 1024 / 1024 > 10) {
    ElMessage.error('图片大小不能超过 10MB!')
    return false
  }
  setPictureBottom('9px')
  return true
}

const uploadUrl = ref(import.meta.env.VITE_API_BASE_URL + '/Agent/uploadPic')
let headImgUrl = ref(headImg)

const setBackGroupColor = backGroupColor => {
  nextTick(() => {
    const uploadCard = document.querySelector('.el-upload--picture-card')
    if (uploadCard) {
      uploadCard.style.background = backGroupColor
    }
  }, 100)
}

const showOrHideHeaderImg = displayType => {
  nextTick(() => {
    const uploadCard = document.querySelector('.el-upload--picture-card')
    if (uploadCard) {
      uploadCard.style.display = displayType
    }
  }, 500)
}

const setPictureBottom = bottom => {
  nextTick(() => {
    const addIcon = document.querySelector('.addIcon')
    if (addIcon) {
      addIcon.style.bottom = bottom
    }
  }, 100)
}

watchEffect(() => {
  if (PageType.EDIT_PAGE !== props.type) {
    return
  }
  // 如果是编辑页面且传入url且用户尚未上传图片，则显示用户上传的图片
  if (!finishedUploadHead.value) {
    if (props.formIntel.agentPicUrl) {
      headImgUrl.value = props.formIntel.agentPicUrl
      setBackGroupColor('none')
    } else {
      setBackGroupColor('linear-gradient(180.00deg, #d0e4ff 0%, #fff 100%)')
      headImgUrl.value = headImg
    }
    setPictureBottom(0)
  } else {
    // 如果用户上传了图片，则显示默认头像
    headImgUrl.value = headImg
    setBackGroupColor('linear-gradient(180.00deg, #d0e4ff 0%, #fff 100%)')
    setPictureBottom('9px')
  }
})

// 上传附件
const addFile = (val1, val2) => {
  if (val1 === 'local') {
    fileRefs.value.openFile(val2, props.formIntel.files)
  } else {
    commonUploadModals.value.openFile(val2)
  }
}

const submitSampleFile = val => {
  for (let i = 0; i < val.length; i++) {
    val[i].fileName = decodeURIComponent(val[i].fileName)
    val[i].originalFileName = decodeURIComponent(val[i].originalFileName)
    const formIndex = props.formIntel.files.findIndex(
      item => item.fileName === val[i].originalFileName
    )
    if (formIndex !== -1) {
      ElMessage.warning("请勿添加同名文件！")
      return
    }
    props.formIntel.files.push({
      fileId: val[i].fileId?.fileId ?? val[i].fileId,
      fileName: val[i].originalFileName,
      extension: getTextAfterLastDot(val[i].originalFileName),
      isLocal: val[i].fileId?.local ?? val[i].local ?? false
    })
  }
}

const removeFile = (file) => {
  const formIndex = props.formIntel.files.findIndex(item => item.fileId === file.fileId)
  if (formIndex !== -1) {
    props.formIntel.files.splice(formIndex, 1)
  }
}

const closePre = () => {
  isPre.value = false
}

onMounted(() => {
  // 创建页清空文件数组
  if (PageType.CREATE_PAGE === props.type) {
    props.formIntel.files = []
  }
  // 设置为默认智能体 防止被表格智能体影响
  currentAgentType.value = DEFAULT_AGENT_TYPE
  eventBus.on('submit-sampleFile', submitSampleFile)
})

onUnmounted(() => {
  eventBus.off('submit-sampleFile', submitSampleFile)
})
</script>

<style lang="less" scoped>
.outer_container{
  // 添加外层容器 使弹框能展示在右侧，并限制其高度
  display: flex;
  height: 115vh;
}
.file_component {
  .file_list {
    border-radius: 10px;
    margin-top: 10px;
    width: 862px;
    overflow: hidden;
    .file_container {
      width: 100%;
      height: auto; /* 改为自动高度 */
      background-color: #f8f8f8;
      border-radius: 10px;
      display: flex;
      flex-wrap: wrap;
      padding: 16px; /* 添加内边距 */
      box-sizing: border-box; /* 确保内边距不影响总宽度 */

      .single_file {
        width: 400px;
        height: 42.5px;
        border-bottom: 1px solid #E1EEFF;
        box-sizing: border-box;
        margin-right: 16px; /* 添加右边距 */
        margin-bottom: 16px;
        &:nth-child(2n) {
          margin-right: 0; /* 每行第二个元素去掉右边距 */
        }

        &:last-child, &:nth-last-child(2) {
          margin-bottom: 0; /* 最后一行去掉下边距 */
        }
        display: flex;
        justify-content: space-between;
        align-items: center;
        .left_img_and_title {
          height: 29.82px;
          display: flex;
          .file_img {
            width: 30px;
            height: 30px;
            margin-right: 8px;
          }
          .file_title {
            font-size: 18px;
            line-height: 25px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            max-width: 330px;
          }
        }
        .right_delete_img {
          width: 14px;
          height: 13.99px;
        }
      }
    }
  }
  .add_file_label {
    font-size: 14px;
    font-weight: 400;
    color: #868686;
    margin-top: 10px;
  }
  .add_file_button {
    width: 180px;
    height: 46px;
    border-radius: 10px;
    border: 2px dashed #1b6cff;
    box-sizing: border-box;
    text-align: center;
    line-height: 40px;
    cursor: pointer;
    margin-top: 10px;
    img {
      vertical-align: middle;
      margin-right: 8px;
    }
    span {
      vertical-align: middle;
      display: inline-block;
      transform: translateY(-0.8px);
    }
  }
}
.create_main {
  width: 100%;
  margin-left: 0;
  height: 115vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  background-color: #fff;

  .create_title {
    font-size: 18px;
    width: 862px;  /* 新增：与create_content同宽 */
    margin: 25px auto 0;  /* 修改：居中显示 */
    color: #262626;
    padding-top: 25px;
    display: flex;
    height: 26px;
    line-height: 26px;
    align-items: center;

    .create_back {
      width: 26px;
      height: 26px;
      display: flex;
      justify-content: center;
      align-items: center;
      box-sizing: border-box;
      border: 1px solid #d0e4ff;
      border-radius: 26px;
      cursor: pointer;

      img {
        width: 14px;
        height: 14px;
        float: left;
      }
    }
  }

  .create_content {
    width: 862px;
    display: flex;
    flex-direction: column;
    margin: 100px auto 0;
    color: #333333;
    font-size: 14px;
    line-height: 18px;

    .agentImg {
      :deep(.el-upload--picture-card) {
        --el-upload-picture-card-size: 92px;
        background: linear-gradient(180deg, #d0e4ff 0%, #fff 100%);
      }
      margin-left: 385px;
      :deep(.el-upload-list--picture-card) {
        --el-upload-list-picture-card-size: 92px;
      }
      .addIcon {
        position: absolute;
        right: 0;
        bottom: 0;
        z-index: 1;
        width: 20px;
        height: 20px;
        background: #4285f4; /* Google蓝色，可调整 */
        border-radius: 50%;
        box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
      }

      /* 加号的横线 */
      .addIcon::before {
        content: '';
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        width: 10px; /* 加号横线长度 */
        height: 2px;
        background: white;
      }

      /* 加号的竖线 */
      .addIcon::after {
        content: '';
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        width: 2px;
        height: 10px; /* 加号竖线长度 */
        background: white;
      }
    }

    .create_name {
      display: flex;
      flex-direction: column;

      .create_input {
        margin-top: 5px;

        :deep(.el-input__wrapper) {
          height: 36px;
          border-radius: 6px;
          line-height: 36px;
          box-sizing: border-box;
          padding-left: 15px;
          /* 统一边框样式（参考 asize.vue 搜索历史输入框）*/
          border: 1px solid #DCE6FA;
          box-shadow: none;
        }
        :deep(.el-input) {
          --el-input-border-radius: 6px;
          --el-input-border-color: #DCE6FA;
          --el-input-hover-border-color: #DCE6FA;
          --el-input-focus-border-color: #409EFF;
        }
        :deep(.el-input__wrapper.is-focus),
        :deep(.el-input.is-focus .el-input__wrapper) {
          border-color: #409EFF !important;
          box-shadow: none;
        }
      }
      .create_text {
        margin-top: 25px;
        transform: translateY(-3px);
      }
    }

    .create_set {
      display: flex;
      flex-direction: column;
      margin-top: 30px;
      position: relative;

      .create_header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        width: 100%;
        margin-bottom: 5px;
        transform: translateY(1px);
      }

      .create_ai {
        width: 116px;
        border-radius: 14px;
        height: 28px;
        font-size: 14px;
        text-align: center;
        box-sizing: border-box;
        background-color: #e6f4ff;
        color: #1b6cff;
        line-height: 28px;
        text-indent: 20px;
        background-image: url('@/assets/ai.png');
        background-repeat: no-repeat;
        background-size: 19px 16px;
        background-position: 16px 5.5px;
        cursor: pointer;
      }

      .create_loading {
        width: 86px;
        border-radius: 14px;
        height: 28px;
        font-size: 14px;
        text-align: center;
        box-sizing: border-box;
        background-color: #e6f4ff;
        color: #1b6cff;
        line-height: 28px;
        text-indent: 20px;
        background-image: url('@/assets/loading.gif');
        background-repeat: no-repeat;
        background-size: 19px 16px;
        background-position: 16px 8px;
        cursor: pointer;
      }

      .create_input {
        margin-top: 5px;

        :deep(.el-textarea__inner) {
          border-radius: 6px !important;
          padding: 10px 15px !important;
          height: 200px;
          resize: none;
          overflow-y: auto; /* 超出高度时显示纵向滚动条 */
          border-radius: 6px;
          /* 统一边框样式（参考 asize.vue 搜索历史输入框）*/
          border: 1px solid #DCE6FA;
          box-shadow: none;
          /* Firefox 可见滚动条（轨道透明） */
          scrollbar-width: thin;
          scrollbar-color: #e5e7eb transparent;
        }
        /* WebKit 滚动条（轨道透明、滑块可见） */
        :deep(.el-textarea__inner::-webkit-scrollbar) { width: 8px !important; }
        :deep(.el-textarea__inner::-webkit-scrollbar-track) { background: transparent !important; }
        :deep(.el-textarea__inner::-webkit-scrollbar-thumb) { background: #e5e7eb !important; border-radius: 4px; border: none; }
        :deep(.el-textarea__inner:focus) {
          border-color: #409EFF !important;
          box-shadow: none;
        }
      }
    }

    .create_btn {
      display: flex;
      margin-top: 25px;
      justify-content: center;
      font-size: 14px;

      .create_cancel {
        width: 120px;
        height: 32px;
        text-align: center;
        line-height: 32px;
        border: 1px solid #dedede;
        border-radius: 4px;
        color: #333333;
        cursor: pointer;
      }

      .create_confirm {
        width: 120px;
        height: 32px;
        text-align: center;
        line-height: 32px;
        border: 1px solid #1b6cff;
        border-radius: 4px;
        background-color: #1b6cff;
        color: #fff;
        margin-left: 10px;
        cursor: pointer;
      }
    }
  }
}
</style>
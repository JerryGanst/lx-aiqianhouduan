<template>
  <div class="file_component">
    <div class="create_text">
      <span style="color: #ff4d4f" v-if="isRequired">*</span>
      <span style="padding-left: 5px">{{ title }}</span>
    </div>
    <div class="add_file_button" @click="showFileSample">
      <img src="@/assets/agent/addFile.png" style="width: 20px; height: 19.91px" />
      <span style="color: #1b6cff; font-size: 18px">添加附件</span>
      <FileMenu
        :showFileMenu="showFileMenu"
        :handleFileSelect="handleFileSelect"
        :localType="localType"
        :knowledgeType="knowledgeType"
        :currentAgentType="''"
      />
    </div>
    <div class="add_file_label">{{ formatTips }}</div>
    <div class="file_list">
      <div class="file_container" v-if="files.length > 0">
        <div class="single_file" v-for="item in files" :key="item.fileId">
          <div class="left_img_and_title" style="cursor: pointer" @click="previewFile(item)">
            <div class="file_img"><img :src="getFileImgByExtension(item)" style="width: 22px; height: 28px" /></div>
            <div class="file_title" :title="item.fileName">{{item.fileName}}</div>
          </div>
          <div class="right_delete_img" style="cursor: pointer"><img src="@/assets/close.png" alt="" style="width: 100%; height: 100%" @click="removeFile(item)"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, toRefs } from 'vue'
import { getFileImgByExtension } from '../../../../utils/common.js'
import FileMenu from '../options/fileMenu.vue'

const props = defineProps({
  title: {
    type: String,
    default: '智能体附件上传'
  },
  formatTips: {
    type: String,
    default: '支持格式: .doc,.docx,.txt,.pdf,pptx,.ppt,.xls,.xlsx，文件大小不超过 50 MB'
  },
  files: {
    type: Array,
    default: () => []
  },
  localType: {
    type: String,
    default: 'sample'
  },
  knowledgeType: {
    type: String,
    default: 'sample'
  },
  autoOpenLocalUpload: {
    type: Boolean,
    default: false
  },
  isRequired: {
    type: Boolean,
    default: false
  }
})

const { title, formatTips, files, localType, knowledgeType, autoOpenLocalUpload } = toRefs(props)

const emit = defineEmits(['addFile', 'removeFile', 'previewFile'])

const showFileMenu = ref(false)
const showFileSample = () => {
  if (autoOpenLocalUpload.value) {
    handleFileSelect('local', localType.value)
    return
  }
  showFileMenu.value = !showFileMenu.value
}

const handleFileSelect = (val1, val2) => {
  showFileMenu.value = false
  const payload = typeof val2 === 'undefined' ? (val1 === 'local' ? localType.value : knowledgeType.value) : val2
  emit('addFile', val1, payload)
}

const removeFile = (file) => {
  emit('removeFile', file)
}

const previewFile = (file) => {
  emit('previewFile', file)
}
</script>

<style lang="less" scoped>
.file_component {
  margin-top: 25px;
  .file_list {
    border-radius: 10px;
    margin-top: 10px;
    width: 862px;
    overflow: hidden;
    .file_container {
      width: 100%;
      height: auto;
      background-color: #f8f8f8;
      border-radius: 10px;
      display: flex;
      flex-wrap: wrap;
      padding: 16px;
      box-sizing: border-box;

      .single_file {
        width: 400px;
        height: 42.5px;
        border-bottom: 1px solid #E1EEFF;
        box-sizing: border-box;
        margin-right: 16px;
        margin-bottom: 16px;
        &:nth-child(2n) {
          margin-right: 0;
        }

        &:last-child, &:nth-last-child(2) {
          margin-bottom: 0;
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
            font-size: 14px;
            color: #333333;
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
    position: relative;
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
</style>
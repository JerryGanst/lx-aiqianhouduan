<template>
  <el-dialog
    v-model="dialogVisible"
    title=""
    :width="width"
    :before-close="handleClose"
    append-to-body
    align-center
    style="border-radius: 10px"
  >
    <div class="delete-confirm-content">
      <!-- 警告图标和标题 -->
      <div class="warning-header">
        <div class="warning-icon">
          <img src="@/assets/knowledgeBase/warning-temp.png" style="width: 18px;height: 18px;" />
        </div>
        <div class="title-text">{{ title }}</div>
      </div>
      
      <!-- 描述文本 -->
      <div class="description-text">{{ description }}</div>
    </div>
    
    <div class="button-container">
      <el-button @click="handleCancel" class="cancel-btn">取消</el-button>
      <el-button @click="handleConfirm" class="confirm-btn">确定</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Warning } from '@element-plus/icons-vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '确认删除当前选中文件吗？'
  },
  description: {
    type: String,
    default: '删除后文件将无法恢复和找回，请谨慎操作'
  },
  width: {
    type: String,
    default: '420px'
  }
})

const emit = defineEmits(['update:visible', 'confirm', 'cancel'])

const dialogVisible = ref(false)

// 监听visible变化
watch(() => props.visible, (newVal) => {
  dialogVisible.value = newVal
})

// 监听dialogVisible变化
watch(dialogVisible, (newVal) => {
  emit('update:visible', newVal)
})

const handleClose = (done) => {
  done()
}

const handleCancel = () => {
  dialogVisible.value = false
  emit('cancel')
}

const handleConfirm = () => {
  emit('confirm')
  dialogVisible.value = false
}
</script>

<style lang="less" scoped>
.delete-confirm-content {
  height: 100%;
  position: relative;
  
  .warning-header {
    margin-left: 30px;
    margin-top: 25.5px;
    position: relative;
    margin-bottom: 10px;
    
    .warning-icon {
      width: 20px;
      height: 20px;
      border-radius: 50%;
      background-color: #ffffff;
      display: flex;
      align-items: center;
      justify-content: center;
      position: absolute;
      left: 0;
      top: 0;
      
      .el-icon {
        color: #000;
        font-size: 12px;
      }
    }
    
    .title-text {
      font-weight: 500;
      font-size: 18px;
      text-align: left;
      color: #333;
      margin-left: 28px; // 20px + 8px，距离图标8px
      line-height: 1.4;
      transform: translateY(-1.5px);
    }
  }
  
  .description-text {
    font-weight: 400;
    font-size: 16px;
    text-align: left;
    color: #9d9d9d;
    line-height: 1.5;
    margin-left: 30px; // 与warning-header左边对齐
  }
}

.button-container {
  display: flex;
  justify-content: flex-start;
  gap: 12px;
  margin-top: 36px;
  margin-bottom: 42px;
  padding-left: 35px;
  min-height: 48px;
  
  .cancel-btn {
    width: 140px;
    height: 48px;
    border-radius: 10px;
    border: 1px solid #d9d9d9;
    background: #fff;
    color: #333;
    font-size: 16px;
    
    &:hover {
      border-color: #40a9ff;
      color: #40a9ff;
    }
  }
  
  .confirm-btn {
    width: 140px;
    height: 48px;
    border-radius: 10px;
    background: #ff4d4f;
    border: none;
    color: #fff;
    font-size: 16px;
    
    &:hover {
      background: #ff7875;
    }
    
    &:active {
      background: #d9363e;
    }
  }
}

// 覆盖el-dialog的默认样式
:deep(.el-dialog) {
  border-radius: 10px;
  
  .el-dialog__header {
    display: none; // 隐藏默认标题栏
  }
  
  .el-dialog__body {
    padding: 0;
  }
  
  .el-dialog__footer {
    display: none; // 隐藏默认底部按钮区域
  }
}
</style>

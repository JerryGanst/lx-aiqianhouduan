<template>
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    :width="width"
    :before-close="handleClose"
    append-to-body
    align-center
    style="border-radius: 10px"
  >
    <el-input
      v-model="inputValue"
      :placeholder="placeholder"
      style="width: 100%"
      clearable
      type="textarea"
      rows="5"
    />
    <div class="button-item_common">
      <el-button @click="handleCancel" style="width: 100px; height: 40px; margin-left: 15px">取消</el-button>
      <el-button type="primary" @click="handleConfirm" style="width: 100px; height: 40px">确定</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '编辑对话名称'
  },
  width: {
    type: String,
    default: '500px'
  },
  placeholder: {
    type: String,
    default: '请输入对话名称'
  },
  defaultValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:visible', 'confirm', 'cancel'])

const dialogVisible = ref(false)
const inputValue = ref('')

// 监听visible变化
watch(() => props.visible, (newVal) => {
  dialogVisible.value = newVal
  if (newVal) {
    inputValue.value = props.defaultValue
  }
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
  emit('confirm', inputValue.value)
  dialogVisible.value = false
}
</script>

<style lang="less" scoped>
.button-item_common {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style> 
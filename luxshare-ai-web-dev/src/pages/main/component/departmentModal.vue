<template>
  <PersonModal
    ref="personModalRef"
    :folder-id="props.folderId"
    :folder-name="props.folderName"
    :department-id="props.departmentId"
    library-type="department"
  />
</template>

<script setup>
import { ref } from 'vue'
import PersonModal from './personModal.vue'

const props = defineProps({
  folderId: {
    type: [Number, String],
    default: 0
  },
  folderName: {
    type: String,
    default: '问知识库'
  },
  departmentId: {
    type: [Number, String],
    default: null
  }
})

const personModalRef = ref(null)

const exposeMethod = methodName => {
  return (...args) => {
    if (personModalRef.value && typeof personModalRef.value[methodName] === 'function') {
      return personModalRef.value[methodName](...args)
    }
  }
}

defineExpose({
  openFile: exposeMethod('openFile'),
  clearChatHistory: exposeMethod('clearChatHistory'),
  clearChatHistoryWithConfirm: exposeMethod('clearChatHistoryWithConfirm')
})
</script>

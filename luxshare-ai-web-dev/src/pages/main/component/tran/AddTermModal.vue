<template>
  <transition name="add-term-modal-fade">
    <div
      v-if="visible"
      class="add-term-modal-overlay"
      @click="handleOverlayClick"
    >
      <div class="add-term-modal" @click.stop>
        <div class="add_top">
          <div class="head_title">{{ modalTitle }}</div>
          <div
            class="close_button"
            role="button"
            tabindex="0"
            @click="closeModal"
            @keydown.enter="closeModal"
            @mouseenter="showCloseTip = true"
            @mouseleave="showCloseTip = false"
            aria-label="关闭"
          >
            <img src="@/assets/knowledgeBase/close.png" alt="关闭" />
            <transition name="fade">
              <div v-if="showCloseTip" class="tooltip-bottom">关闭</div>
            </transition>
          </div>
        </div>
        <div class="add_content">
          <div class="input-wrapper">
            <textarea
              v-model="termForm.zh"
              class="add_input"
              placeholder="请在此输入中文词汇"
            ></textarea>
            <textarea
              v-model="termForm.en"
              class="add_input"
              placeholder="请在此输入英文词汇"
            ></textarea>
            <textarea
              v-model="termForm.es"
              class="add_input"
              placeholder="请在此输入西班牙语词汇"
            ></textarea>
            <textarea
              v-model="termForm.vi"
              class="add_input"
              placeholder="请在此输入越南语词汇"
            ></textarea>
          </div>
        </div>
        <div class="add_btns">
          <button class="btn cancel-btn" @click="closeModal">取消</button>
          <button class="btn save-btn" @click="handleSave">保存</button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  closeOnClickOverlay: {
    type: Boolean,
    default: true
  },
  term: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:visible', 'close'])

const showCloseTip = ref(false)
const modalTitle = computed(() => (props.term ? '编辑术语' : '添加术语'))
const termForm = reactive({
  zh: '',
  en: '',
  es: '',
  vi: ''
})

const resetForm = () => {
  termForm.zh = ''
  termForm.en = ''
  termForm.es = ''
  termForm.vi = ''
}

const fillForm = term => {
  if (!term) {
    resetForm()
    return
  }

  termForm.zh = term.zh || ''
  termForm.en = term.en || ''
  termForm.es = term.es || ''
  termForm.vi = term.vi || ''
}

const languageKeys = ['zh', 'en', 'es', 'vi']

const handleSave = () => {
  const trimmedValues = languageKeys.map(key => termForm[key].trim())
  const filledCount = trimmedValues.filter(value => value).length

  if (filledCount < 2) {
    ElMessage.warning('至少输入两种语言的词汇')
    return
  }

  languageKeys.forEach((key, index) => {
    termForm[key] = trimmedValues[index]
  })

  const [zh, en, es, vi] = trimmedValues
  const payload = { zh, en, es, vi }

  emit('save', payload)
  closeModal()
}

const handleOverlayClick = () => {
  if (!props.closeOnClickOverlay) {
    return
  }

  closeModal()
}

const closeModal = () => {
  resetForm()
  emit('close')
  emit('update:visible', false)
}

watch(
  () => props.visible,
  value => {
    if (!value) {
      resetForm()
      return
    }

    if (props.term) {
      fillForm(props.term)
      return
    }

    resetForm()
  }
)

watch(
  () => props.term,
  term => {
    if (props.visible && term) {
      fillForm(term)
    }
  }
)
</script>

<style scoped lang="less">
.add-term-modal-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  z-index: 3100;
}

.add-term-modal {
  width: 1006px;
  height: 420px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #eee;
  box-shadow: 0 0 10px #00000033;
  .add_top {
    height: 53.5px;
    width: 100%;
    display: flex;
    justify-content: space-between;
    border-bottom: 1px solid #ede9e9;
    align-items: center;
    .head_title {
      font-size: 22px;
      font-weight: 500;
      color: #000000;
      margin-left: 24px;
    }
  }
  .add_content {
    width: 100%;
    height: 290.5px;
    background-color: #fff;
    padding: 15.5px 16px 0;
    box-sizing: border-box;
    .input-wrapper {
      display: flex;
      gap: 10px;
      height: 100%;
    }
    .add_input {
      flex: 0 0 236px;
      width: 236px;
      height: 275px;
      background: #fff;
      border: 1px solid #dce6fa;
      border-radius: 4px;
      padding: 12px;
      font-size: 14px;
      line-height: 1.5;
      resize: none;
      box-sizing: border-box;
      outline: none;
    }
    .add_input:focus,
    .add_input:focus-visible {
      border-color: #409eff;
    }
    .add_input::placeholder {
      color: #9ea3b4;
    }
  }
  .add_btns {
    width: 100%;
    height: 70px;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 16px;
    padding: 0 16px 16px;
    box-sizing: border-box;
  }
  .btn {
    width: 110px;
    height: 38px;
    border-radius: 6px;
    border: 1px solid transparent;
    font-size: 16px;
    cursor: pointer;
    margin-top: 20px;
  }
  .cancel-btn {
    background: #fff;
    color: #000000;
    border-color: #dedede;
  }
  .save-btn {
    background: #1b6cff;
    color: #fff;
    border-color: #1b6cff;
  }
}

.close_button {
  margin-right: 25px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  transition: background-color 0.2s ease;
  outline: none;
}

.close_button:hover,
.close_button:focus-visible {
  background-color: #eee;
}

.close_button img {
  width: 16px;
  height: 16px;
}

.tooltip-bottom {
  position: absolute;
  top: calc(100% + 5px);
  left: 50%;
  transform: translateX(-50%);
  background: #000;
  color: #fff;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 14px;
  white-space: nowrap;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.tooltip-bottom::after {
  content: '';
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 5px solid transparent;
  border-bottom-color: #000;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.add-term-modal-fade-enter-active,
.add-term-modal-fade-leave-active {
  transition: opacity 0.2s ease;
}

.add-term-modal-fade-enter-from,
.add-term-modal-fade-leave-to {
  opacity: 0;
}
</style>

<template>
  <transition name="edit-tag-modal-fade">
    <div
      v-if="visible"
      class="edit-tag-modal-overlay"
      @click="handleOverlayClick"
    >
      <div class="edit-tag-modal" @click.stop>
        <div class="add_top">
          <div class="head_title">编辑标签</div>
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
          <div class="tag-input-wrapper">
            <div
              v-for="tag in selectedTags"
              :key="tag.id"
              class="selected-tag"
            >
              <span class="selected-tag__name">{{ tag.name }}</span>
              <button
                type="button"
                class="selected-tag__remove"
                aria-label="移除标签"
                @click="removeSelectedTag(tag.id)"
              >
                ×
              </button>
            </div>
            <input
              v-model="tagKeyword"
              class="tag-input"
              type="text"
            />
          </div>
          <div class="tag-section">
            <div class="tag-title">我的标签</div>
            <div class="tag-list">
              <span
                v-for="tag in tagList"
                :key="tag.tagId ?? tag.id"
                :class="['tag-item', { 'tag-item--selected': isTagSelected(tag.id ?? tag.tagId) }]"
                @click="toggleTagSelection(tag.id ?? tag.tagId)"
              >
                {{ tag.targetName }}
              </span>
            </div>
          </div>
        </div>
        <div class="add_btns">
          <button class="btn cancel-btn" @click="closeModal">取消</button>
          <button class="btn confirm-btn" :disabled="loading" @click="handleConfirm">
            {{ loading ? '保存中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getTargetFolderByUserId, saveTargetFolder } from '@/api/knowledgeBase/actions.js'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  closeOnClickOverlay: {
    type: Boolean,
    default: true
  },
  fileId: {
    type: [String, Number],
    default: null
  },
  isDepartment: {
    type: Boolean,
    default: false
  },
  selectedTagIds: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:visible', 'close', 'confirm'])

const showCloseTip = ref(false)
const tagKeyword = ref('')
const loading = ref(false)
const tagList = ref([])
const normalizeId = id => (id ?? '').toString()
const selectedTagIdSet = ref(new Set())

const selectedTags = computed(() => {
  const ids = selectedTagIdSet.value

  if (!ids?.size) {
    return []
  }

  return tagList.value
    .filter(tag => ids.has(normalizeId(tag.id ?? tag.tagId)))
    .map(tag => ({
      id: normalizeId(tag.id ?? tag.tagId),
      name: tag.targetName ?? ''
    }))
})

const syncSelectedTagIds = selectedTagIds => {
  if (!Array.isArray(selectedTagIds)) {
    selectedTagIdSet.value = new Set()
    return
  }

  selectedTagIdSet.value = new Set(selectedTagIds.map(normalizeId))
}

watch(
  () => props.selectedTagIds,
  newSelectedTagIds => {
    syncSelectedTagIds(newSelectedTagIds)
  },
  { immediate: true, deep: true }
)

const isTagSelected = tagId => selectedTagIdSet.value.has(normalizeId(tagId))

const toggleTagSelection = tagId => {
  const normalizedId = normalizeId(tagId)
  const updatedSet = new Set(selectedTagIdSet.value)

  if (updatedSet.has(normalizedId)) {
    updatedSet.delete(normalizedId)
  } else {
    updatedSet.add(normalizedId)
  }

  selectedTagIdSet.value = updatedSet
}

const removeSelectedTag = tagId => {
  const normalizedId = normalizeId(tagId)
  if (!selectedTagIdSet.value.has(normalizedId)) {
    return
  }

  const updatedSet = new Set(selectedTagIdSet.value)
  updatedSet.delete(normalizedId)
  selectedTagIdSet.value = updatedSet
}

const fetchTagList = async () => {
  try {
    const userInfoString = localStorage.getItem('userInfo')
    const userInfo = userInfoString ? JSON.parse(userInfoString) : null

    if (!userInfo?.id) {
      tagList.value = []
      return
    }

    const response = await getTargetFolderByUserId(userInfo.id)

    if (response?.status) {
      tagList.value = response?.data ?? []
    } else {
      tagList.value = []
      if (response?.message) {
        ElMessage.error(response.message)
      }
    }
  } catch (error) {
    console.error('获取标签列表失败', error)
    ElMessage.error('获取标签列表失败，请稍后重试')
    tagList.value = []
  }
}

const handleOverlayClick = () => {
  if (!props.closeOnClickOverlay) {
    return
  }

  closeModal()
}

const resetState = () => {
  tagKeyword.value = ''
}

const closeModal = () => {
  emit('close')
  emit('update:visible', false)
  resetState()
}

const handleConfirm = async () => {
  const targetName = tagKeyword.value.trim()

  if (!props.fileId) {
    ElMessage.error('未选择文件，无法保存标签')
    return
  }

  if (loading.value) {
    return
  }

  loading.value = true

  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo'))
    const tagIdList = Array.from(selectedTagIdSet.value)

    const tags = tagIdList.map(id => ({ id }))
    tags.push({ targetName })

    const payload = {
      fileId: props.fileId,
      isDepartment: props.isDepartment,
      userId: userInfo.id,
      tags
    }

    const response = await saveTargetFolder(payload)

    if (response?.status) {
      ElMessage.success('标签保存成功')
      emit('confirm', response?.data ?? null)
      closeModal()
    } else {
      ElMessage.error(response?.message || '标签保存失败')
    }
  } catch (error) {
    console.error('保存标签失败', error)
    ElMessage.error('标签保存失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

watch(
  () => props.visible,
  async visible => {
    if (visible) {
      await fetchTagList()
    } else {
      resetState()
      loading.value = false
    }
  }
)
</script>

<style scoped lang="less">
.edit-tag-modal-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  z-index: 3100;
}

.edit-tag-modal {
  width: 520px;
  min-height: 320px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #eee;
  box-shadow: 0 0 10px #00000033;
  display: flex;
  flex-direction: column;

  .add_top {
    height: 53.5px;
    width: 100%;
    display: flex;
    justify-content: space-between;
    border-bottom: 1px solid #ede9e9;
    align-items: center;
    .head_title {
      font-size: 18px;
      font-weight: 400;
      color: #333;
      margin-left: 18px;
    }
  }

  .add_content {
    flex: 1;
    width: 100%;
    background-color: #fff;
    padding: 15.5px 16px 0;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    align-items: center;

    .tag-input {
      flex: 1 0 160px;
      min-width: 120px;
      border: none;
      background: transparent;
      font-size: 16px;
      line-height: 32px;
      height: 32px;
      padding: 0 8px;
      outline: none;
      scrollbar-width: thin;
      scrollbar-color: #e5e7eb transparent;

      &:focus {
        outline: none;
      }
    }

    .tag-input-wrapper {
      width: 488px;
      min-height: 48px;
      max-height: 160px;
      border-radius: 8px;
      background: #fff;
      border: 1px solid #1b6cff;
      margin-top: 0;
      padding: 8px;
      box-sizing: border-box;
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      align-content: flex-start;
      gap: 8px;
      overflow-y: auto;
      scrollbar-width: thin;
      scrollbar-color: #e5e7eb transparent;

      &:focus-within {
        border-color: #1b6cff;
        box-shadow: none;
      }
    }

    .selected-tag {
      height: 32px;
      border-radius: 16px;
      background: #e8f2ff;
      display: inline-flex;
      align-items: center;
      justify-content: space-between;
      font-weight: 400;
      font-size: 16px;
      color: #1b6cff;
      padding: 0 12px;
      position: relative;
      gap: 6px;
      box-sizing: border-box;
      text-align: left;
      width: auto;
      min-width: 0;
      max-width: 148px;
    }

    .selected-tag__name {
      flex: 1;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      max-width: 100%;
    }

    .selected-tag__remove {
      border: none;
      background: transparent;
      color: #1b6cff;
      font-size: 16px;
      line-height: 1;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0;
      transform: scale(1.4);
      margin-top: 6px;
    }

    .selected-tag__remove:hover {
      opacity: 0.7;
    }

    .tag-section {
      width: 100%;
      margin-top: 24px;
      display: flex;
      flex-direction: column;
      align-items: flex-start;

      .tag-title {
        font-size: 16px;
        font-weight: 400;
        color: #333;
        margin-bottom: 10px;
      }

      .tag-list {
        max-height: 400px;
        overflow-y: auto;
        width: 100%;
        display: flex;
        flex-wrap: wrap;
        gap: 10px 20px;
        padding: 0 16px 24px 0;
        box-sizing: border-box;
        scrollbar-width: thin;
        scrollbar-color: #e5e7eb transparent;

        .tag-item {
          border-radius: 16px;
          background: #f6f6f6;
          height: 32px;
          padding: 0 16px;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          font-weight: 400;
          font-size: 14px;
          text-align: center;
          color: #8f8f8f;
          white-space: nowrap;
          cursor: pointer;

          &.tag-item--selected {
            background: #e8f2ff;
            color: #1b6cff;
          }
        }
      }
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

    &:disabled {
      cursor: not-allowed;
      opacity: 0.6;
    }
  }

  .cancel-btn {
    background: #fff;
    color: #000000;
    border-color: #dedede;
  }

  .confirm-btn {
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

.edit-tag-modal-fade-enter-active,
.edit-tag-modal-fade-leave-active {
  transition: opacity 0.2s ease;
}

.edit-tag-modal-fade-enter-from,
.edit-tag-modal-fade-leave-to {
  opacity: 0;
}
</style>

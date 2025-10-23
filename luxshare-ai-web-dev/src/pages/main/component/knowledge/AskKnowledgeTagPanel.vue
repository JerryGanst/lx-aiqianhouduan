<template>
  <Teleport to="body">
    <div
      v-if="visible"
      ref="panelRef"
      class="ask-knowledge-tag-panel"
      :style="panelStyle"
    >
      <ul class="tag-list">
        <li
          v-for="item in tagList"
          :key="item?.targetId ?? item?.id ?? item?.targetName"
          class="tag-list-item"
          @click="handleTagClick(item)"
        >
          <img :src="setTagIcon" alt="标签" class="tag-icon" />
          <span class="tag-name">{{ item?.targetName ?? '' }}</span>
        </li>
      </ul>
    </div>
  </Teleport>
</template>

<script setup>
import { Teleport, computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import setTagIcon from '@/assets/knowledgeBase/set_tag.svg'
import { getTargetFolderByUserId } from '@/api/knowledgeBase/actions.js'

const MAX_PANEL_HEIGHT = 300

const props = defineProps({
  visible: { type: Boolean, default: false },
  position: {
    type: Object,
    default: () => ({ x: 0, y: 0 })
  },
  width: { type: Number, default: 336 },
  height: { type: Number, default: 308 },
  selectedTagIds: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['request-close', 'select-tag', 'availability-change'])

const panelRef = ref(null)
const tagList = ref([])
const panelHeight = ref(0)
const hasFetched = ref(false)
const selectedTagIdSet = computed(() => {
  const ids = Array.isArray(props.selectedTagIds) ? props.selectedTagIds : []
  return new Set(ids.map(id => (id ?? '').toString()))
})

const getEffectiveHeight = () => {
  if (panelHeight.value) {
    return Math.min(panelHeight.value, MAX_PANEL_HEIGHT)
  }
  return Math.min(props.height, MAX_PANEL_HEIGHT)
}

const panelStyle = computed(() => {
  const x = Number(props.position?.x ?? 0)
  const y = Number(props.position?.y ?? 0)
  const width = props.width
  const height = getEffectiveHeight()
  const viewportWidth = window.innerWidth || width
  const viewportHeight = window.innerHeight || height

  let left = x
  let top = y - height

  if (left + width > viewportWidth - 8) {
    left = Math.max(8, viewportWidth - width - 8)
  }
  if (left < 8) {
    left = 8
  }

  if (top < 8) {
    top = 8
  }
  if (top + height > viewportHeight - 8) {
    top = Math.max(8, viewportHeight - height - 8)
  }

  return {
    left: `${left}px`,
    top: `${top}px`,
    width: `${width}px`,
    maxHeight: `${MAX_PANEL_HEIGHT}px`
  }
})

const updatePanelHeight = () => {
  nextTick(() => {
    if (panelRef.value) {
      panelHeight.value = panelRef.value.offsetHeight
    }
  })
}

const notifyAvailabilityChange = () => {
  emit('availability-change', tagList.value.length > 0)
}

const fetchTagList = async () => {
  try {
    const userInfoString = localStorage.getItem('userInfo')
    const userInfo = userInfoString ? JSON.parse(userInfoString) : null

    if (!userInfo?.id) {
      tagList.value = []
      notifyAvailabilityChange()
      return
    }

    const response = await getTargetFolderByUserId(userInfo.id)

    if (response?.status) {
      tagList.value = Array.isArray(response?.data) ? response.data : []
    } else {
      tagList.value = []
    }
  } catch (error) {
    console.error('获取标签列表失败', error)
    tagList.value = []
  } finally {
    hasFetched.value = true
    notifyAvailabilityChange()
    updatePanelHeight()
  }
}

const handleDocumentMouseDown = (event) => {
  const el = panelRef.value
  if (!el) return
  if (event.target instanceof Node && el.contains(event.target)) {
    return
  }
  emit('request-close')
}

const handleEscape = (event) => {
  if (event.key === 'Escape') {
    emit('request-close')
  }
}

const handleTagClick = (item) => {
  const id = item?.targetId ?? item?.id ?? item?.targetName
  if (!id) return

  const normalizedId = (id ?? '').toString()
  if (selectedTagIdSet.value.has(normalizedId)) {
    return
  }

  emit('select-tag', item)
  emit('request-close')
}

watch(
  () => props.visible,
  (value) => {
    if (value) {
      if (!hasFetched.value) {
        fetchTagList()
      } else {
        updatePanelHeight()
      }
    }
  }
)

watch(tagList, () => {
  notifyAvailabilityChange()
  updatePanelHeight()
})

onMounted(() => {
  document.addEventListener('mousedown', handleDocumentMouseDown)
  document.addEventListener('keydown', handleEscape)
  if (props.visible && !hasFetched.value) {
    fetchTagList()
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleDocumentMouseDown)
  document.removeEventListener('keydown', handleEscape)
})
</script>

<style scoped>
.ask-knowledge-tag-panel {
  position: fixed;
  z-index: 10000;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #eee;
  box-shadow: 0 0 10px #00000033;
  box-sizing: border-box;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: #e5e7eb transparent;
  padding: 4px;
}

.ask-knowledge-tag-panel::-webkit-scrollbar {
  width: 6px;
}

.ask-knowledge-tag-panel::-webkit-scrollbar-thumb {
  background-color: #e5e7eb;
  border-radius: 9999px;
}

.ask-knowledge-tag-panel::-webkit-scrollbar-track {
  background: transparent;
}

.tag-list {
  list-style: none;
  margin: 0;
  padding: 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tag-list-item {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 310px;
  height: 32px;
  border-radius: 6px;
  cursor: pointer;
  margin: 0 auto;
}

.tag-list-item:hover {
  background: #f6f6f6;
}

.tag-icon {
  width: 17px;
  height: 17px;
  flex-shrink: 0;
  margin-left: 6px;
}

.tag-name {
  font-weight: 400;
  font-size: 16px;
  text-align: left;
  color: #333;
}
</style>

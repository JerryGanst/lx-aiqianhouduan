<template>
  <div class="tag-filter-panel">
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
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  tagList: {
    type: Array,
    default: () => []
  },
  selectedTagIds: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:selectedTagIds', 'change'])

const normalizeId = id => (id ?? '').toString()

const selectedTagIdSet = computed(() => new Set((props.selectedTagIds ?? []).map(normalizeId)))

const isTagSelected = tagId => selectedTagIdSet.value.has(normalizeId(tagId))

const toggleTagSelection = tagId => {
  const normalizedId = normalizeId(tagId)
  const updatedSet = new Set(selectedTagIdSet.value)

  if (updatedSet.has(normalizedId)) {
    updatedSet.delete(normalizedId)
  } else {
    updatedSet.add(normalizedId)
  }

  const updatedIds = Array.from(updatedSet)
  emit('update:selectedTagIds', updatedIds)
  emit('change', updatedIds)
}
</script>

<style scoped lang="less">
.tag-filter-panel {
  width: 294px;
  height: 216px;
  background: #fff;
  filter: drop-shadow(0 3px 6px #0000001a);
  border-radius: 12px;
  padding: 16px 20px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tag-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: #e5e7eb transparent;
}

.tag-list::-webkit-scrollbar {
  width: 6px;
}

.tag-list::-webkit-scrollbar-track {
  background: transparent;
}

.tag-list::-webkit-scrollbar-thumb {
  background-color: #e5e7eb;
  border-radius: 999px;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 14px;
  border-radius: 16px;
  background: #f6f6f6;
  font-weight: 400;
  font-size: 14px;
  text-align: left;
  color: #8f8f8f;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.tag-item:hover {
  background: #e8f2ff;
  color:#1B6CFF;
}

.tag-item--selected {
  background-color: #1b6cff;
  color: #fff;
}
</style>

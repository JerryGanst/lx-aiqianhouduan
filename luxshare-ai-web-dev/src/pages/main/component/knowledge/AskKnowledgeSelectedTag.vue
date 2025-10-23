<template>
  <div class="ask-knowledge-selected-tag">
    <img :src="askTagIcon" alt="标签" class="ask-knowledge-selected-tag__icon" />
    <span class="ask-knowledge-selected-tag__text">{{ tagName }}</span>
    <button
      v-if="closable"
      type="button"
      class="ask-knowledge-selected-tag__remove"
      @click.stop="emit('remove', tagId)"
      aria-label="删除标签"
    >
      ×
    </button>
  </div>
</template>

<script setup>
import askTagIcon from '@/assets/knowledgeBase/ask_tag.svg'

defineProps({
  tagId: {
    type: [String, Number],
    default: ''
  },
  tagName: {
    type: String,
    default: ''
  },
  closable: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['remove'])
</script>

<style scoped>
.ask-knowledge-selected-tag {
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  height: 24px;
  background: transparent;
  border-radius: 12px;
  gap: 6px;
  padding: 0 6px 0 0;
}

.ask-knowledge-selected-tag__icon {
  width: 17px !important;
  height: 17px !important;
  flex-shrink: 0;
}

.ask-knowledge-selected-tag__text {
  font-weight: 400;
  font-size: 16px;
  text-align: left;
  color: #b7b8b9;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ask-knowledge-selected-tag__remove {
  border: none;
  background: transparent;
  color: #b7b8b9;
  cursor: pointer;
  padding: 0 4px;
  font-size: 16px;
  line-height: 1;
  flex-shrink: 0;
}

.ask-knowledge-selected-tag__remove:hover,
.ask-knowledge-selected-tag__remove:focus {
  color: #409eff;
  outline: none;
}
</style>

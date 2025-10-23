<template>
  <div class="info-row" @click="handleClick" @mouseenter="hoverMethod($event)">
    <div
      class="menu-title"
      :title="isTitleOverflow ? menuTitle : null"
    >
      <img v-if="iconSrc" :src="iconSrc" alt="" class="menu-icon" />
      <span>{{ displayTitle }}</span>
    </div>
    <div v-if="hasMoreAction" class="more_action">
      <img src="@/assets/knowledgeBase/more.svg" width="10" height="10" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import editTagIcon from '@/assets/knowledgeBase/edit_tags.svg'
import shareIcon from '@/assets/knowledgeBase/menu_share.svg'

const props = defineProps({
  menuId: {
    type: [String, Number],
    required: true
  },
  menuTitle: {
    type: String,
    required: true
  },
  hasMoreAction: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['menu-click', 'menu-hover'])

const isTitleOverflow = computed(() => props.menuTitle.length > 9)

const displayTitle = computed(() =>
  isTitleOverflow.value ? `${props.menuTitle.slice(0, 9)}...` : props.menuTitle
)

const iconSrc = computed(() => {
  const iconMap = {
    editTag: editTagIcon,
    share: shareIcon
  }
  return iconMap[props.menuId] || null
})

const handleClick = () => {
  emit('menu-click', props.menuId)
}

const hoverMethod = event => {
  emit('menu-hover', { menuId: props.menuId, event })
}
</script>

<style scoped lang="less">
.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 170px;
  height: 38px;
  gap: 8px;
  cursor: pointer;
  box-sizing: border-box;
  padding: 5px;
}

.menu-title {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.menu-icon {
  width: 17px;
  height: 17px;
  flex-shrink: 0;
}

.info-row:hover {
  border-radius: 6px;
  background: #ededed;
  width: 170px;
  height: 38px;
}
</style>

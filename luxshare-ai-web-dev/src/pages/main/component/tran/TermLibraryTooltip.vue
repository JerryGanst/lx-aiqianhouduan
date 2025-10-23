<template>
  <transition name="fade">
    <div v-if="show" class="term-library-tooltip" @click.stop>
      <div class="triangle"></div>
      <div class="tooltip-content">
        <MenuCheckBox
          class="tooltip-checkbox"
          :show="true"
          :checked="checked"
          :menu-id="menuId"
          @update-selected="handleUpdateSelected"
        />
        <span class="tooltip-text">个人术语库</span>
        <button class="more-button" type="button" @click.stop="handleMoreClick">
          <img :src="moreIcon" alt="更多" style="width: 8px;height: 8px; transform: translateX(1px)" />
        </button>
      </div>
    </div>
  </transition>
  <TermLibraryMoreModal v-model:visible="showModal" />
</template>

<script setup>
import { ref, toRefs } from 'vue'
import MenuCheckBox from '../options/menuCheckBox.vue'
import moreIcon from '@/assets/knowledgeBase/more.svg'
import TermLibraryMoreModal from './TermLibraryMoreModal.vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  checked: {
    type: Boolean,
    default: false
  },
  menuId: {
    type: [String, Number],
    default: ''
  }
})

const { show, checked, menuId } = toRefs(props)

const emit = defineEmits(['update-selected', 'more-click'])
const showModal = ref(false)

const handleUpdateSelected = (menuId, isSelected) => {
  emit('update-selected', menuId, isSelected)
}

const handleMoreClick = () => {
  showModal.value = true
  emit('more-click')
}
</script>

<style scoped lang="less">
.term-library-tooltip {
  position: absolute;
  bottom: 140%;
  left: 50%;
  transform: translateX(-50%);
  background-color: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 8px;
  margin-bottom: 12px;
  z-index: 2000;
  width: 240px;
  height: 50px;
  display: flex;
  align-items: center;
}

.tooltip-content {
  display: flex;
  align-items: center;
  width: 100%;
}

.tooltip-text {
  margin-left: 6px;
  color: #333;
  font-size: 14px;
  flex: 1;
}

.more-button {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #eee;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 0;
}

.more-button img {
  width: 10px;
  height: 10px;
}

.triangle {
  position: absolute;
  bottom: -9px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 10px solid transparent;
  border-right: 10px solid transparent;
  border-top: 10px solid #fff;
}

:deep(.check_box) {
  margin-right: 6px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

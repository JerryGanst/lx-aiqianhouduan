<template>
  <transition name="fade">
    <div v-if="showFileMenu" class="file-menu" @click.stop>
      <div class="triangle"></div>
      <div
        v-if="showLocalOption"
        class="menu-item"
        @click="handleFileSelect('local', localType)"
      >
        从本地读取
      </div>
      <div
        class="menu-item"
        @click="handleFileSelect('knowledge', knowledgeType)"
        v-if="showKnowledgeOption"
      >
        从知识库读取
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed } from 'vue'
import { TABLE_AGENT_TYPE, COMPARE_AGENT_TYPE } from '@/utils/constants.js'

const props = defineProps({
  showFileMenu: Boolean,
  handleFileSelect: Function,
  localType: {
    type: String,
    default: ''
  },
  knowledgeType: {
    type: String,
    default: ''
  },
  currentAgentType: {
    type: [String, Number],
    default: ''
  },
  disableKnowledgeOption: {
    type: Boolean,
    default: false
  },
  restrictByAgentType: {
    type: Boolean,
    default: false
  }
})

const showLocalOption = computed(
  () => !props.restrictByAgentType || TABLE_AGENT_TYPE !== props.currentAgentType
)

const showKnowledgeOption = computed(
  () =>
    !props.disableKnowledgeOption &&
    (!props.restrictByAgentType || COMPARE_AGENT_TYPE !== props.currentAgentType)
)
</script>

<style lang="less" scoped>
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

.file-menu {
  position: absolute;
  bottom: 140%;
  left: 50%;
  transform: translateX(-50%);
  background-color: white;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 8px 0;
  margin-bottom: 12px;
  z-index: 2000;
  min-width: 140px;
}

.menu-item {
  padding: 8px 16px;
  cursor: pointer;
  color: #333;
}

.menu-item:hover {
  background-color: #e6f4ff;
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


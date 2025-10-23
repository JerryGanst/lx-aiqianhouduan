<template>
  <el-menu-item
    class="agent_menu"
    :class="{ 'folder-menu-item': useFolderStyle }"
    ref="menuItemRef"
    @mouseenter="showActions = true"
    @mouseleave="handleMouseLeave"
    :style="{ backgroundColor: showActions ? '#DCE6FA' : '' }"
  >
    <MenuActions
      v-if="!hideMore"
      :menuId="menuId"
      :show="showMoreTips"
      :positionX="menuPositionX"
      :positionY="menuPositionY"
      @handle-edit="handleEditMenu"
      @handle-delete="handleDeleteMenu"
      @hide-dialog="showMoreTips = false"
      @active-menu="activeMenuStyle"
      @de-active-menu="deActiveMenuStyle"
    />
    <div class="menu_title" ref="menuTitleRef">
      <div class="asize_check_box">
        <MenuCheckBox
          :show="showCheckBox"
          :menuId="menuId"
          :checked="isSelected"
          @updateSelected="handleUpdateSelected"
        />
      </div>
      <div v-if="useFolderStyle" class="folder_icon">
        <img :src="folderIcon" alt="folder" />
      </div>
      <div class="title_content" :class="{ 'folder-title-content': useFolderStyle }">
        <span :class="{'active-span': isActive}">{{menuTitle}}</span>
      </div>
    </div>
    <div
      v-if="!hideMore"
      class="menu_more"
      v-show="showActions"
      @click.stop="toggleMoreTips"
      ref="moreButton"
    >
      <img :src="more" style="width: 24px; height: 24px; transform: translateY(1.6px)" />
    </div>
  </el-menu-item>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import type { ComponentPublicInstance } from 'vue'
import MenuActions from '../options/menuActions.vue'
import MenuCheckBox from './menuCheckBox.vue'
import more from '../../../../assets/agent/more.png'
import folderSimple from '../../../../assets/knowledgeBase/dir_simple.svg'
import folderActive from '../../../../assets/knowledgeBase/dir_active.svg'

const props = defineProps<{
  menuId: string | number
  menuTitle: string
  isActive: boolean
  showCheckBox?: boolean
  selected?: boolean
  useFolderStyle?: boolean
  hideMore?: boolean
}>()

const emit = defineEmits(['deleteMenu', 'editMenu', 'updateSelected'])

// 计算当前菜单项是否被选中
const isSelected = computed(() => {
  return props.selected || false
})
const useFolderStyle = computed(() => props.useFolderStyle ?? false)
const hideMore = computed(() => props.hideMore ?? false)
const folderIcon = computed(() => (props.isActive ? folderActive : folderSimple))
const menuItemRef = ref<ComponentPublicInstance | HTMLElement | null>(null)
const showActions = ref(false)
const showMoreTips = ref(false) // 新增：控制提示框显示状态
const moreButton = ref<HTMLElement | null>(null)
const isActionsHover = ref(false) // 追踪更多操作弹框是否被悬浮
// 新增：切换提示框显示状态
const toggleMoreTips = () => {
  if (hideMore.value) return
  if (moreButton.value) {
    const rect = moreButton.value.getBoundingClientRect()
    menuPositionY.value = rect.bottom - 12 // 正下方5px
    menuPositionX.value = rect.left - 2
  }
  showMoreTips.value = !showMoreTips.value
}
const menuPositionX = ref(0)
const menuPositionY = ref(0)
// 处理操作菜单事件
const handleEditMenu = (agentId: string | number) => {
  emit('editMenu', agentId)
}
const handleDeleteMenu = (agentId: string | number) => {
  // 这里添加实际删除逻辑
  emit('deleteMenu', agentId)
}

// 处理复选框选中状态变化
const handleUpdateSelected = (menuId: string | number, isSelected: boolean) => {
  emit('updateSelected', menuId, isSelected)
}

const activeMenuStyle = () => {
  showActions.value = true
  isActionsHover.value = true
}

const deActiveMenuStyle = () => {
  showActions.value = false
  isActionsHover.value = false
  showMoreTips.value = false
}

// 当鼠标离开当前 menuItem，且鼠标不在更多操作弹框内时，关闭弹框
const handleMouseLeave = () => {
  showActions.value = false
  // 延时以等待可能进入弹框的 mouseenter 事件先触发
  setTimeout(() => {
    if (!isActionsHover.value) {
      showMoreTips.value = false
    }
  }, 60)
}

const menuTitleRef = ref<HTMLDivElement | null>(null)
const nav = typeof navigator !== 'undefined' ? (navigator as Navigator & { msMaxTouchPoints?: number }) : undefined
const isTouchDevice =
  (typeof window !== 'undefined' && 'ontouchstart' in window) ||
  Boolean(nav && (nav.maxTouchPoints > 0 || Number(nav.msMaxTouchPoints ?? 0) > 0))

let cleanup: (() => void) | null = null

const toElement = (node: Node | null): Element | null => {
  if (!node) return null
  if (node.nodeType === Node.ELEMENT_NODE) {
    return node as Element
  }
  if (node.nodeType === Node.TEXT_NODE) {
    return (node.parentElement as Element) ?? null
  }
  return null
}

onMounted(() => {
  if (!isTouchDevice) return

  const menuHost = (() => {
    const current = menuItemRef.value
    if (!current) return null
    if (current instanceof HTMLElement) {
      return current
    }
    return (current.$el ?? null) as HTMLElement | null
  })()

  if (!menuHost) return
  const titleHost = menuTitleRef.value

  const clearSelection = () => {
    const selection = window.getSelection()
    if (selection && !selection.isCollapsed) {
      selection.removeAllRanges()
    }
  }

  const handleTouchStart = () => {
    clearSelection()
  }

  const handleSelectionChange = () => {
    const selection = window.getSelection()
    if (!selection || selection.isCollapsed) return

    const anchorEl = toElement(selection.anchorNode)
    const focusEl = toElement(selection.focusNode)

    if (
      (anchorEl && menuHost.contains(anchorEl)) ||
      (focusEl && menuHost.contains(focusEl))
    ) {
      selection.removeAllRanges()
    }
  }

  menuHost.addEventListener('touchstart', handleTouchStart, { passive: true })
  titleHost?.addEventListener('touchstart', handleTouchStart, { passive: true })
  document.addEventListener('selectionchange', handleSelectionChange)

  cleanup = () => {
    menuHost.removeEventListener('touchstart', handleTouchStart)
    titleHost?.removeEventListener('touchstart', handleTouchStart)
    document.removeEventListener('selectionchange', handleSelectionChange)
  }
})

onBeforeUnmount(() => {
  cleanup?.()
  cleanup = null
})
</script>

<style scoped lang="less">
.asize_check_box {
  transform: translateX(-5px);
}
/* 复选框样式已抽离到 menuCheckBox.vue */
.agent_menu {
  margin-left: 16px;
  margin-bottom: 16px;
  margin-top: 6px;
  width: 208px;
  height: 38px;
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  user-select: none !important;
  -webkit-user-select: none !important;
  -moz-user-select: none !important;
  -ms-user-select: none !important;
  -webkit-touch-callout: none !important;
  .menu_more {
    margin-right: 10px;
    color: #333333;
    font-size: 20px;
    user-select: none;
    display: flex;
    align-items: center;
    height: 100%;
    -webkit-tap-highlight-color: transparent;
  }
  .menu_title {
    font-size: 14px;
    font-weight: 400;
    align-items: center;
    user-select: none !important;
    -webkit-user-select: none !important;
    -moz-user-select: none !important;
    -ms-user-select: none !important;
    -webkit-touch-callout: none !important;
    display: flex;
    /* 让标题区域在父级 flex 中按剩余空间伸缩 */
    flex: 1 1 auto;
    /* 关键：在 flex 容器内允许收缩，从而触发省略号 */
    min-width: 0;
    color: #333333;
    line-height: 38px;
    .check_box {
      margin-right: 5px;
      display: flex;
      align-items: center;
      transform: translateY(1px);
    }
    .title_content {
      /* 文字容器占满剩余空间，并在超出时显示省略号 */
      flex: 1 1 auto;
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      display: flex;
      align-items: center;
      transform: translate(-6px, -0.5px);
      user-select: none !important;
      -webkit-user-select: none !important;
      -moz-user-select: none !important;
      -ms-user-select: none !important;
      -webkit-touch-callout: none !important;
    }

    .title_content span {
      user-select: none !important;
      -webkit-user-select: none !important;
      -moz-user-select: none !important;
      -ms-user-select: none !important;
      -webkit-touch-callout: none !important;
    }
    .active-span {
        color: #1b6cff;
    }
  }
  &.folder-menu-item {
    .menu_title {
      gap: 8px;
      box-sizing: border-box;
    }
    .folder_icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 24px;
      height: 24px;
      flex: 0 0 auto;
      img {
        width: 24px;
        height: 24px;
      }
    }
    .folder-title-content {
      /* 放宽展示宽度，保证至少可完整展示 8 个中文字符 */
      max-width: 144px;
    }
  }
  &:hover {
    background-color:#DCE6FA;
  }
}

@media (pointer: coarse) {
  :deep(.agent_menu),
  :deep(.agent_menu *) {
    user-select: none !important;
    -webkit-user-select: none !important;
    -moz-user-select: none !important;
    -ms-user-select: none !important;
    -webkit-touch-callout: none !important;
    touch-action: manipulation;
  }
}
</style>

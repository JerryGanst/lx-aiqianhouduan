<template>
  <div
    ref="menuRef"
    class="file-context-menu"
    :style="{ top: `${contextMenu.y}px`, left: `${contextMenu.x}px`, zIndex: 30 + menuLevel }"
  >
    <div class="file-context-menu__title" v-if="hasTitle">
      <img src="@/assets/knowledgeBase/share.svg" width="18" height="18" style="transform: translateY(3.3px)" />
      分享到：
    </div>
    <div class="file-context-menu__info">
      <template v-if="menuList.length > 0">
        <MenuInfoRow
          v-for="menu in menuList"
          :key="menu.id"
          :menu-id="menu.id"
          :menu-title="menu.title"
          :has-more-action="menu.hasMoreAction"
          @menu-click="handleMenuClick"
          @menu-hover="handleHover"
        />
      </template>
      <div v-else class="file-context-menu__empty">暂无数据</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, toRefs, watch } from 'vue'
import MenuInfoRow from '@/pages/main/component/knowledge/MenuInfoRow.vue'
import { getDepartmentInfoByUserId } from '@/api/knowledgeBase/actions.js'
import request from '@/utils/request.js'

const props = defineProps({
  contextMenu: {
    type: Object,
    required: true
  },
  hasTitle: {
    type: Boolean,
    default: false
  },
  menuType: {
    type: String,
    default: ''
  },
  menuLevel: {
    type: Number,
    default: 1
  },
  parentId: {
    type: [String, Number],
    default: null
  },
  showShareOption: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['menu-hover', 'menu-click'])

const { contextMenu } = toRefs(props)
const userInfo = ref({})
const menuList = ref([])

const menuRef = ref(null)
const handleMenuClick = menuId => {
  if (props.menuType === 'DEPARTMENT') {
    return
  }
  emit('menu-click', menuId)
}

const handleHover = payload => {
  if (props.menuType === 'DIRECTORY') {
    return
  }
  emit('menu-hover', payload)
}

const setDepartmentMenu = async () => {
  try {
    userInfo.value = JSON.parse(localStorage.getItem('userInfo')) || {}
  } catch (error) {
    console.error('解析用户信息失败', error)
    userInfo.value = {}
  }

  menuList.value = []

  let userId = userInfo.value?.id
  if (!userId) {
    return
  }

  try {
    const response = await getDepartmentInfoByUserId(userId)
    if (response?.status) {
      menuList.value = (response.data || []).map(department => ({
        id: department.id,
        title: department.name,
        hasMoreAction: true
      }))
    }
  } catch (error) {
    console.error('获取处级干部部门信息失败', error)
  }
}

const setOptionsMenu = () => {
  const options = []

  if (props.showShareOption) {
    options.push({ id: 'share', title: '分享', hasMoreAction: false })
  }

  menuList.value = options
}

const loadDirectoryMenu = async () => {
  if (props.menuType !== 'DIRECTORY') {
    return
  }

  if (!props.parentId) {
    menuList.value = []
    return
  }

  try {
    const response = await request.get('/FileFolder/getFolderList', {
      params: {
        id: props.parentId,
        isDepartment: true
      }
    })

    if (response?.status) {
      menuList.value = (response.data || []).map(folder => ({
        id: folder.id,
        title: folder.folderName,
        hasMoreAction: false
      }))
    } else {
      menuList.value = []
    }
  } catch (error) {
    console.error('获取部门文件夹失败', error)
    menuList.value = []
  }
}

onMounted(() => {
  if (props.menuType === 'OPTIONS') {
    setOptionsMenu()
  }
  if (props.menuType === 'DEPARTMENT') {
    setDepartmentMenu()
  }

  if (props.menuType === 'DIRECTORY') {
    loadDirectoryMenu()
  }
})

watch(
  () => props.showShareOption,
  () => {
    if (props.menuType === 'OPTIONS') {
      setOptionsMenu()
    }
  }
)

watch(
  () => props.parentId,
  () => {
    if (props.menuType === 'DIRECTORY') {
      loadDirectoryMenu()
    }
  }
)
</script>

<style scoped lang="less">
.file-context-menu {
  position: absolute;
  width: 190px;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 0 10px #0000004d;
  padding: 10px;
  box-sizing: border-box;
  z-index: 30;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  gap: 10px;
  pointer-events: auto;
}

.file-context-menu__title {
  font-size: 14px;
  font-weight: 400;
  color: #333;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.file-context-menu__info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 14px;
  color: #666;
}

.file-context-menu__empty {
  font-size: 12px;
  color: #999;
  text-align: center;
}

.file-context-menu__info .label {
  color: #999;
}

.file-context-menu__info .value {
  color: #333;
  flex: 1;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-context-menu__info .tips {
  background: #f0f6ff;
  color: #1b6cff;
  border-radius: 6px;
  padding: 6px;
  text-align: center;
}
</style>

<template>
  <div
    v-if="hasDepartmentKnowledge"
    class="asize_know"
    style="margin-top: 12px"
    @click="handleChangeFileModel"
    @mouseenter="isDepartmentHover = true"
    @mouseleave="isDepartmentHover = false"
    :style="{
      backgroundColor: isSelected ? '#1B6CFF' : 'transparent',
      color: isSelected ? '#ffffff' : '#333333'
    }"
  >
    <div class="leftImg" style="transform: translate(3.5px, 2.4px)">
      <img v-if="level" :src="isDepartmentHover ? department_black : (isSelected ? department_white : department_black)" style="width: 24px; height: 24px; transform: translate(-2.8px, -2px);">
      <el-icon v-else
        :size="19"
        :style="{
          color: isDepartmentHover ? '#333333' : (isSelected ? '#ffffff' : '#333333')
        }"
      >
        <OfficeBuilding />
      </el-icon>
    </div>
    <span class="knowledge-text">{{ departmentKnowledgeName }}</span>

    <div
      class="add_file_directory"
      @mouseenter="showDepartmentAddTip = true"
      @mouseleave="showDepartmentAddTip = false"
      @click.stop="handleCreateFolder"
    >
      <img :src="isDepartmentHover ? addFileDirectory : (isSelected ? addDirWhite : addFileDirectory)" style="width: 15px; height: 15px" />
      <transition name="fade">
        <div v-if="showDepartmentAddTip" class="tooltip-top">新建文件夹</div>
      </transition>
    </div>
  </div>
  <div
    class="folder_list"
    v-if="
      hasDepartmentKnowledge &&
      isSelected &&
      folderList.length > 0 &&
      !isFolderCollapsed
    "
  >
    <el-menu :default-active="activeFolderIndex" class="el_menu">
      <menu-item
        v-for="(folder, index) in folderList"
        :key="folder.id"
        :menu-id="folder.id"
        :menu-title="folder.folderName"
        :is-active="activeFolderIndex === index"
        :show-check-box="false"
        :use-folder-style="true"
        @edit-menu="handleEditFolder(folder)"
        @delete-menu="handleDeleteFolder(folder.id)"
        @click="handleSelectFolder(folder, index)"
      ></menu-item>
    </el-menu>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { OfficeBuilding } from '@element-plus/icons-vue'
import addDirWhite from '@/assets/knowledgeBase/add_dir_white.png'
import addFileDirectory from '@/assets/knowledgeBase/add_file_directory.png'
import MenuItem from '@/pages/main/component/options/menuItem.vue'
import department_black from '@/assets/knowledgeBase/department_black.svg'
import department_white from '@/assets/knowledgeBase/department_white.svg'

const props = defineProps({
  hasDepartmentKnowledge: {
    type: Boolean,
    required: true
  },
  departmentKnowledgeName: {
    type: String,
    required: true
  },
  departmentId: {
    type: [String, Number],
    required: true
  },
  level: {
    type: String,
    required: false
  },
  levelCode: {
    type: String,
    required: false
  },
  isSelected: {
    type: Boolean,
    required: true
  },
  folderList: {
    type: Array,
    required: true
  },
  activeFolderIndex: {
    type: [String, Number],
    required: true
  },
  isFolderCollapsed: {
    type: Boolean,
    required: true
  }
})

const emit = defineEmits(['change-file-model', 'create-folder', 'edit-folder', 'delete-folder', 'select-folder'])

const isDepartmentHover = ref(false)
const showDepartmentAddTip = ref(false)

const handleChangeFileModel = () => {
  emit('change-file-model', props.departmentId)
}

const handleCreateFolder = () => {
  emit('create-folder', props.departmentId)
}

const handleEditFolder = (folder) => {
  emit('edit-folder', {
    folderName: folder.folderName,
    folderId: folder.id,
    departmentId: props.departmentId
  })
}

const handleDeleteFolder = (folderId) => {
  emit('delete-folder', {
    folderId,
    departmentId: props.departmentId
  })
}

const handleSelectFolder = (folder, index) => {
  emit('select-folder', {
    folderId: folder.id,
    index,
    departmentId: props.departmentId
  })
}
</script>

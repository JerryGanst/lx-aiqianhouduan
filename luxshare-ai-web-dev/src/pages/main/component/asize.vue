<template>
  <el-aside :width="sidebarWidth" class="aside">
    <div class="aside_left" v-if="!isMobile">
      <img class="aside_left_img" src="@/assets/logo.png" />
      <div class="aside_left_message" @click="changeContent(ContentType.CONVERSATION)">
        <div
          class="aside_img"
          :style="{ backgroundColor: selectType === ContentType.CONVERSATION ? '#E6F4FF' : '#F7F7F7' }"
        >
          <img :src="selectType === ContentType.CONVERSATION ? messageBlue : messageGray" />
        </div>
        <div
          class="aside_message_text"
          :style="{ color: selectType === ContentType.CONVERSATION ? '#1B6CFF' : '#9D9D9D' }"
        >
          对话
        </div>
      </div>

      <div class="aside_left_file" v-if="isPowerFile" @click="changeContent(ContentType.KNOWLEDGE)">
        <div
          class="aside_img"
          :style="{ backgroundColor: selectType === ContentType.KNOWLEDGE ? '#E6F4FF' : '#F7F7F7' }"
        >
          <img
            :src="selectType === ContentType.KNOWLEDGE ? fileBlue : fileGray"
            :style="{ backgroundColor: selectType === ContentType.KNOWLEDGE ? '#E6F4FF' : '#F7F7F7' }"
          />
        </div>
        <div
          class="aside_message_text"
          :style="{ color: selectType === ContentType.KNOWLEDGE ? '#1B6CFF' : '#9D9D9D' }"
        >
          知识库
        </div>
      </div>
      <div class="aside_left_file" @click="changeContent(ContentType.AGENT)" style="top: 225px">
        <div class="aside_img" :style="{ backgroundColor: selectType === ContentType.AGENT ? '#E6F4FF' : '#F7F7F7' }">
          <img
            :src="selectType === ContentType.AGENT ? IntelligenceBlue : IntelligenceGray"
            :style="{ backgroundColor: selectType === ContentType.AGENT ? '#E6F4FF' : '#F7F7F7' }"
          />
        </div>
        <div class="aside_message_text" :style="{ color: selectType === ContentType.AGENT ? '#1B6CFF' : '#9D9D9D' }">
          智能体
        </div>
      </div>

      <div class="user-avatar-container" v-if="isLogin">
        <!-- 头像 -->
        <el-avatar
          :size="36"
          :src="userInfo.url"
          class="user-avatar"
          @mouseenter="showPopup = true"
          @mouseleave="showPopup = false"
        />

        <!-- 弹窗 -->
        <el-popover v-model:visible="showPopup" placement="top-end" :width="100" trigger="hover">
          <template #reference>
            <div class="popover-reference"></div>
          </template>

          <div class="user-info-popup">
            <div class="user-info">
              <el-avatar :size="36" :src="avatarUrl" class="el_avatar" />
              <div class="user-details">
                <div class="username">{{ userInfo.id }}</div>
                <div class="username">{{ userInfo.name }}</div>
              </div>
            </div>
            <el-divider />
            <el-button type="text" @click="handleLogout">退出登录</el-button>
          </div>
        </el-popover>
      </div>
      <div class="noLogin" v-if="!isLogin" @click="dialogVisible = true">登录</div>
    </div>

    <div
      class="aside_right"
      :class="{ collapsed: isCollapsed }"
      :style="{
        width: isCollapsed ? '0px' : '236px',
        borderRight: isCollapsed ? 'none' : '2px solid #EAEAEA'
      }"
      v-if="selectType !== ContentType.AGENT || isAgentDetail"
      ref="asideRightRef"
    >
      <div class="asize_message" v-if="selectType === ContentType.CONVERSATION">
        <div class="aside_right_btn">
          <div @click="startNewConversation" class="back_set">
            {{ isCollapsed ? '' : '开启新对话' }}
          </div>
        </div>
        <div style="width: 209px; margin-left: 16px; margin-top: 16px">
          <el-input v-select-all-on-ctrl-a
            v-model="searchText"
            placeholder="搜索历史对话"
            clearable
            @clear="clearChatData"
            @keydown.enter.prevent="searchChatData"
          >
            <template #suffix>
              <el-icon class="search-icon" @click="searchChatData" style="cursor: pointer"><Search /></el-icon>
            </template>
          </el-input>
        </div>
        <el-menu :default-active="activeIndex" class="el_menu" :style="{ maxHeight: chatMenuMaxHeight }" @contextmenu.prevent="onElMenuContextMenu">
          <menu-item
            v-for="(chat, index) in chatList"
            :key="chat.id"
            :menu-id="chat.id"
            :menu-title="chat.title"
            :is-active="activeIndex === index"
            :show-check-box="showBulkDeleteMode"
            :selected="selectedMenuIds.includes(chat.id)"
            @updateSelected="updateSelectedMenuIds"
            @edit-menu="handleEdit(chat.title, chat.id)"
            @delete-menu="handleConfirmDelete(chat.id)"
            @click="querySelect(chat.id, index)"
          ></menu-item>
        </el-menu>
        <div class="batch_remove" v-show="showBulkDeleteMode">
          <div class="select_all_to_remove">
            <MenuCheckBox 
              :show="showBulkDeleteMode" 
              :checked="isAllSelected"
              menuId="selectAll"
              @updateSelected="handleSelectAll"
            />
            <span>已选{{ selectedMenuIds.length }}个会话</span>
          </div>
          <div class="handle_remove" @click="handleBulkDelete">
            <span>删除</span>
          </div>
        </div>
      </div>
      <div class="asize_file" v-if="selectType === ContentType.KNOWLEDGE" style="position: relative;">
        <div
          class="asize_know"
          @click="changeFileModel(KnowledgeSelect.PERSONAL)"
          @mouseenter="isPersonalHover = true"
          @mouseleave="isPersonalHover = false"
          :style="{
            backgroundColor: knowSelect === KnowledgeSelect.PERSONAL ? '#1B6CFF' : 'transparent',
            color: knowSelect === KnowledgeSelect.PERSONAL ? '#ffffff' : '#333333',
          }"
        >
          <div class="leftImg">
            <img :src="isPersonalHover ? personBlack : (knowSelect === KnowledgeSelect.PERSONAL ? personWhite : personBlack)"  style="width: 24px; height: 24px">
          </div>
          <span class="knowledge-text">个人知识库</span>

          <div class="add_file_directory" @mouseenter="showAddTip = true" @mouseleave="showAddTip = false" @click.stop="handleCreateFolder(KnowledgeSelect.PERSONAL)">
            <img :src="isPersonalHover ? addFileDirectory : (knowSelect === KnowledgeSelect.PERSONAL ? addDirWhite : addFileDirectory)" style="width: 15px; height: 15px"/>
            <transition name="fade">
              <div v-if="showAddTip" class="tooltip-top">新建文件夹</div>
            </transition>
          </div>
        </div>
        <!-- 文件夹列表 -->
        <div class="folder_list" v-if="folderLists[KnowledgeSelect.PERSONAL].length > 0 && !isPersonalFolderCollapsed">
          <el-menu :default-active="activeFolderIndexMap[KnowledgeSelect.PERSONAL]" class="el_menu">
            <menu-item
              v-for="(folder, index) in folderLists[KnowledgeSelect.PERSONAL]"
              :key="folder.id"
              :menu-id="folder.id"
              :menu-title="folder.folderName"
              :is-active="activeFolderIndexMap[KnowledgeSelect.PERSONAL] === index"
              :show-check-box="false"
              :use-folder-style="true"
              @edit-menu="handleEditFolder(folder.folderName, folder.id, KnowledgeSelect.PERSONAL)"
              @delete-menu="handleConfirmDeleteFolder(folder.id, KnowledgeSelect.PERSONAL)"
              @click="handleFolderSelect(folder.id, index, KnowledgeSelect.PERSONAL, true)"
            ></menu-item>
          </el-menu>
        </div>


        <div
          class="asize_know"
          style="margin-top: 12px"
          @click="changeFileModel(KnowledgeSelect.PUBLIC)"
          @mouseenter="isCommonHover = true"
          @mouseleave="isCommonHover = false"
          :style="{
            backgroundColor: knowSelect === KnowledgeSelect.PUBLIC ? '#1B6CFF' : 'transparent',
            color: knowSelect === KnowledgeSelect.PUBLIC ? '#ffffff' : '#333333',
          }"
        >
          <div class="leftImg">
            <img :src="isCommonHover ? commonBlack : (knowSelect === KnowledgeSelect.PUBLIC ? commonWhite : commonBlack)" style="width: 15.6px; height: 15.6px; transform: translate(3.5px, 2.1px);">
          </div>
          公用知识库

        </div>
        <div class="know_list" v-if="knowSelect === KnowledgeSelect.PUBLIC && !isPublicKnowledgeCollapsed">
          <el-menu :default-active="publicActiveMenuId" class="el_menu">
            <menu-item
              v-for="(item, index) in powerArr"
              :key="item.target ?? index"
              :menu-id="item.target ?? index"
              :menu-title="item.name"
              :is-active="ItemSelect === index"
              :show-check-box="false"
              :use-folder-style="true"
              :hide-more="true"
              @click="knowItemSelect(index)"
            ></menu-item>
          </el-menu>
        </div>

        <template v-if="hasDepartmentKnowledge">
          <DepartmentKnowledgeSection
            v-for="department in departmentSections"
            :key="department.id"
            :has-department-knowledge="hasDepartmentKnowledge"
            :department-id="department.id"
            :department-knowledge-name="department.displayName"
            :is-selected="isDepartmentSelected(department.id)"
            :folder-list="getDepartmentFolderList(department.id)"
            :active-folder-index="getDepartmentActiveIndex(department.id)"
            :is-folder-collapsed="isDepartmentCollapsed(department.id)"
            :level="department.level"
            :levelCode="department.levelCode"
            @change-file-model="handleDepartmentChangeFileModel"
            @create-folder="handleDepartmentCreateFolder"
            @edit-folder="handleDepartmentEditFolder"
            @delete-folder="handleDepartmentDeleteFolder"
            @select-folder="handleDepartmentSelectFolder"
          />
        </template>
      </div>
      <div class="asize_message" v-if="selectType === ContentType.AGENT">
        <div class="aside_right_btn" style="display: flex">
          <div class="intel_img" @click="backToAgentList"><img src="@/assets/agent/back.png" /></div>
          <span class="intel_title">{{ currentIntel.name }}</span>
        </div>
        <div style="width: 209px; margin-left: 16px; margin-top: 16px">
          <el-input v-select-all-on-ctrl-a
            v-model="searchTextIntel"
            placeholder="搜索历史对话"
            clearable
            @clear="clearData"
            @keydown.enter.prevent="searchData"
          >
            <template #suffix>
              <el-icon class="search-icon" @click="searchData" style="cursor: pointer"><Search /></el-icon>
            </template>
          </el-input>
        </div>
        <el-menu :default-active="activeIndexIntel" class="el_menu" :style="{ maxHeight: agentMenuMaxHeight }" @contextmenu.prevent="onElMenuContextMenu">
          <menu-item
            v-for="(chat, index) in agentChatList"
            :key="chat.agentChatId"
            :menu-id="chat.agentChatId"
            :menu-title="chat.title"
            :is-active="activeIndexIntel === index"
            :show-check-box="showAgentBulkDeleteMode"
            :selected="selectedAgentMenuIds.includes(chat.agentChatId)"
            @updateSelected="updateSelectedAgentMenuIds"
            @edit-menu="handleEditIntel(chat.title, chat.agentChatId)"
            @delete-menu="handleConfirmDeleteIntel(chat.agentChatId)"
            @click="querySelectIntel(chat.agentChatId, index)"
          ></menu-item>
        </el-menu>
        <!-- 智能体批量删除模式 -->
        <div class="batch_remove" v-show="showAgentBulkDeleteMode">
          <div class="select_all_to_remove">
            <MenuCheckBox
              :show="showAgentBulkDeleteMode"
              :checked="isAgentAllSelected"
              menuId="selectAllAgent"
              @updateSelected="handleAgentSelectAll"
            />
            <span>已选{{ selectedAgentMenuIds.length }}个会话</span>
          </div>
          <div class="handle_remove" @click="handleAgentBulkDelete">
            <span>删除</span>
          </div>
        </div>

        <!-- 新建对话按钮 -->
        <div class="create_conversation" v-show="!showAgentBulkDeleteMode">
          <div class="create_conversation_btn" @click="createNewConversation">
            <img src="@/assets/agent/addconversation.png" />
            <span>新建对话</span>
          </div>
        </div>
      </div>
      <!-- 右键浮动按钮（悬浮于 .el_menu 上方） -->
      <div
        v-if="showContextBtn"
        class="context-action-btn"
        :class="{ 'is-active': (selectType === ContentType.CONVERSATION && showBulkDeleteMode) || (selectType === ContentType.AGENT && showAgentBulkDeleteMode) }"
        :style="{ top: contextBtnTop, left: contextBtnLeft }"
        @click.stop="selectType === ContentType.AGENT ? toggleAgentBulkDeleteMode() : toggleBulkDeleteMode()"
      >
        <div class="left_delete_img">
          <img
            :src="(selectType === ContentType.CONVERSATION && showBulkDeleteMode) || (selectType === ContentType.AGENT && showAgentBulkDeleteMode) ? cancelOperationIcon : removePicIcon"
            alt=""
            :style="{
              width: (selectType === ContentType.CONVERSATION && showBulkDeleteMode) || (selectType === ContentType.AGENT && showAgentBulkDeleteMode) ? '17px' : '24px',
              height: (selectType === ContentType.CONVERSATION && showBulkDeleteMode) || (selectType === ContentType.AGENT && showAgentBulkDeleteMode) ? '17px' : '24px',
              marginRight: (selectType === ContentType.CONVERSATION && showBulkDeleteMode) || (selectType === ContentType.AGENT && showAgentBulkDeleteMode) ? '3.2px' : '1.2px',
              transform: 'translateY(2.1px)'
            }"
          />
        </div>
        <div class="right_delete_content" :style="{ fontSize: '14px', color: (selectType === ContentType.CONVERSATION && showBulkDeleteMode) || (selectType === ContentType.AGENT && showAgentBulkDeleteMode) ? '#333333' : '#FF4D4F', transform: 'translateX(-0.4px)' }">{{ (selectType === ContentType.CONVERSATION && showBulkDeleteMode) || (selectType === ContentType.AGENT && showAgentBulkDeleteMode) ? '取消操作' : '批量删除' }}</div>
      </div>
    </div>
  </el-aside>
  <div
    v-if="!isMobile"
    class="foldable"
    v-show="selectType !== ContentType.AGENT || isAgentDetail"
    :style="{ left: isCollapsed ? '96px' : '330px' }"
  >
    <img :src="isCollapsed ? right : left" @click="toggleCollapse" />
  </div>
  <el-dialog v-model="dialogVisible" title="" width="400px" :before-close="handleClose" style="border-radius: 10px">
    <div class="login_title">
      <span><img src="@/assets/logo2.png" /></span>
      <span>立讯技术百事通</span>
    </div>
    <el-form :model="loginForm" :rules="rules" ref="loginForms" class="login-form">
      <el-form-item prop="username" class="form-item">
        <el-input v-model="loginForm.username" placeholder="请输入工号(用户名)" clearable>
          <template #prefix>
            <img src="@/assets/user.png" style="width: 20px; height: 20px" />
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password" class="form-item">
        <el-input
          v-model="loginForm.password"
          placeholder="请输入密码"
          clearable
          :type="passwordVisible ? 'text' : 'password'"
        >
          <template #prefix>
            <img src="@/assets/password.png" style="width: 20px; height: 20px" />
          </template>
          <template #suffix>
            <img
              v-if="loginForm.password"
              :src="passwordVisible ? View : Lock"
              alt=""
              @click="togglePasswordVisibility"
              style="cursor: pointer; width: 16px; height: 16px"
            />
          </template>
        </el-input>
      </el-form-item>
      <el-form-item class="button-item">
        <el-button type="primary" @click="submitForm" style="width: 100%; height: 40px">登录</el-button>
      </el-form-item>
    </el-form>
  </el-dialog>
  <edit-title-dialog
    v-model:visible="titleVisible"
    title="编辑对话名称"
    width="500px"
    placeholder="请输入对话名称"
    :default-value="titleQuestion"
    @confirm="handleTitleConfirm"
    @cancel="handleTitleCancel"
  />
  <edit-title-dialog
    v-model:visible="titleVisibleIntel"
    title="编辑智能体对话"
    width="500px"
    placeholder="请输入智能体名称"
    :default-value="titleQuestionIntel"
    @confirm="handleTitleConfirmIntel"
    @cancel="handleTitleCancelIntel"
  />
  <edit-title-dialog
    v-model:visible="createFolderVisible"
    title="新建文件夹"
    width="500px"
    placeholder="请输入文件夹名称"
    :default-value="folderName"
    @confirm="handleCreateFolderConfirm"
    @cancel="handleCreateFolderCancel"
  />
     <edit-title-dialog
     v-model:visible="renameFolderVisible"
     title="文件夹重命名"
     width="500px"
     placeholder="请输入文件夹名称"
     :default-value="folderName"
     @confirm="handleRenameFolderConfirm"
     @cancel="handleRenameFolderCancel"
   />
   
   <!-- 删除确认弹窗 -->
   <DeleteConfirmDialog
     v-model:visible="deleteConfirmVisible"
     :title="`确认删除当前选中文件夹吗？`"
     :description="`删除后文件夹无法恢复和找回，请谨慎操作`"
     @confirm="handleConfirmDeleteItem"
     @cancel="deleteConfirmVisible = false"
   />
   
   <!-- 对话删除确认弹窗 -->
   <DeleteConfirmDialog
     v-model:visible="deleteChatConfirmVisible"
     title="确认删除对话吗？"
     description="删除对话将不可恢复，请谨慎操作"
     @confirm="handleConfirmDeleteChat"
     @cancel="deleteChatConfirmVisible = false"
   />
   
   <!-- 智能体删除确认弹窗 -->
   <DeleteConfirmDialog
     v-model:visible="deleteAgentConfirmVisible"
     title="确认删除对话吗？"
     description="删除对话将不可恢复，请谨慎操作"
     @confirm="handleConfirmDeleteAgent"
     @cancel="deleteAgentConfirmVisible = false"
   />

   <!-- 批量删除确认弹窗 -->
   <DeleteConfirmDialog
     v-model:visible="bulkDeleteConfirmVisible"
     :title="bulkDeleteTitle"
     :description="bulkDeleteDescription"
     @confirm="handleConfirmBulkDelete"
     @cancel="bulkDeleteConfirmVisible = false"
   />

   <!-- 智能体批量删除确认弹窗 -->
   <DeleteConfirmDialog
     v-model:visible="agentBulkDeleteConfirmVisible"
   :title="agentBulkDeleteTitle"
    :description="agentBulkDeleteDescription"
    @confirm="handleConfirmAgentBulkDelete"
    @cancel="agentBulkDeleteConfirmVisible = false"
  />

  <!-- 移动端底部导航 -->
  <div v-if="isMobile" class="mobile-bottom-nav" :style="mobileBottomNavStyle">
    <div class="nav-item" @click="changeContent(ContentType.CONVERSATION)">
      <div class="nav-ref" :class="{ active: selectType === ContentType.CONVERSATION }">
        <img :src="selectType === ContentType.CONVERSATION ? messageBlue : messageGray" />
        <span>对话</span>
      </div>
    </div>
    <div class="nav-item" v-if="isPowerFile" @click="changeContent(ContentType.KNOWLEDGE)">
      <div class="nav-ref" :class="{ active: selectType === ContentType.KNOWLEDGE }">
        <img :src="selectType === ContentType.KNOWLEDGE ? fileBlue : fileGray" />
        <span>知识库</span>
      </div>
    </div>
    <div class="nav-item" @click="changeContent(ContentType.AGENT)">
      <div class="nav-ref" :class="{ active: selectType === ContentType.AGENT }">
        <img :src="selectType === ContentType.AGENT ? IntelligenceBlue : IntelligenceGray" />
        <span>智能体</span>
      </div>
    </div>
    <div class="nav-item">
      <template v-if="isLogin">
        <el-popover
          v-model:visible="showPopup"
          placement="top"
          :width="100"
          trigger="click"
          :teleported="false"
        >
          <div class="user-info-popup">
            <div class="user-info">
              <el-avatar :size="36" :src="avatarUrl" class="el_avatar" />
              <div class="user-details">
                <div class="username">{{ userInfo.id }}</div>
                <div class="username">{{ userInfo.name }}</div>
              </div>
            </div>
            <el-divider />
            <el-button type="text" @click="handleLogout">退出登录</el-button>
          </div>
          <template #reference>
            <div class="nav-ref">
              <el-avatar :size="24" :src="userInfo.url" />
              <span>我</span>
            </div>
          </template>
        </el-popover>
      </template>
      <template v-else>
        <div class="nav-ref" @click="dialogVisible = true">
          <el-icon><User /></el-icon>
          <span>登录</span>
        </div>
      </template>
    </div>
  </div>

   <!-- <commonModal ref="commonLedge"></commonModal> -->
</template>
<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick, reactive, watch } from 'vue'
import { MODE_MAPPING, useShared } from '@/utils/useShared'
import { ContentType, KnowledgeSelect, FileModel } from '@/utils/common'
import { ElButton, ElDivider, ElMessage, ElPopover } from 'element-plus' // 引入 ElMessage
import { networkState } from '@/utils/net'
import { encryptData } from '@/utils/rsa'
import { Search, User } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import eventBus from '@/utils/eventBus'
import Lock from '@/assets/lock.png' // 引入需要的图标
import View from '@/assets/view.png' // 引入需要的图标
import photo from '@/assets/chat.deepseek.com_.png'
 
import left from '@/assets/159@2x.png'
import right from '@/assets/162@2x.png'
import messageBlue from '@/assets/message_blue.png'
import messageGray from '@/assets/message_gray.png'
import fileBlue from '@/assets/file_blue.png'
import fileGray from '@/assets/file_gray.png'
import personWhite from '@/assets/knowledgeBase/person_white.svg'
import personBlack from '@/assets/knowledgeBase/person_black.svg'
import commonWhite from '@/assets/knowledgeBase/common_white.svg'
import commonBlack from '@/assets/knowledgeBase/common_black.svg'
import addDirWhite from '@/assets/knowledgeBase/add_dir_white.png'
import addFileDirectory from '@/assets/knowledgeBase/add_file_directory.png'
import IntelligenceGray from '@/assets/​​Intel_gray.png'
import IntelligenceBlue from '@/assets/​​Intel_blue.png'
import removePicIcon from '@/assets/conversation/remove_pic.png'
import cancelOperationIcon from '@/assets/conversation/cancelOperation.svg'
import request from '@/utils/request'
import {
  changeAgentChatTitle,
  changeImgRecognitionChatTitle,
  changeResumeTaskTitleById,
  deleteImgRecognitionById,
  deleteResumeTaskById,
  removeAgentChatById, removeExcelChatById, updateExcelChatTitle,
  batchRemoveAgentChatList
} from '@/api/agent/actions.js'
import MenuItem from '@/pages/main/component/options/menuItem.vue'
import MenuCheckBox from '@/pages/main/component/options/menuCheckBox.vue'
import EditTitleDialog from '@/pages/main/component/options/editTitleDialog.vue'
import DeleteConfirmDialog from '@/pages/main/component/options/deleteConfirmDialog.vue'
import DepartmentKnowledgeSection from '@/pages/main/component/knowledge/DepartmentKnowledgeSection.vue'
import { batchRemoveChatList, getChatDetailByChatId } from '@/api/chat/actions.js' // 导入封装的 axios 方法
import { getFolderList, createFolder, deleteFolder, getDepartmentInfoByUserId } from '@/api/knowledgeBase/actions.js' // 导入文件夹相关方法
import { COMPARE_AGENT_TYPE, DEFAULT_AGENT_TYPE, RESUME_AGENT_TYPE, TABLE_AGENT_TYPE } from '@/utils/constants.js' // 导入封装的 axios 方法
const isCollapsed = ref(false) // 左上角折叠控制
const showPopup = ref(false) // 是否展示左下角用户信息弹窗
const dialogVisible = ref(false) // 是否展示登录弹窗
const showAddTip = ref(false) // 是否展示新建文件夹提示框
const ItemSelect = ref(0)
const isMobile = ref(false) // 当前是否为移动端
// 智能体对话列表最大高度：100vh 减去顶部区域与底部"新建对话"区域像素，避免越过边框
// 顶部保留≈100(头部) + 16(间距) + 38(搜索框) + 16(间距) ≈ 170
// 底部保留≈96(新建对话容器) + 23(底部间距) ≈ 119
// 再留出少量缓冲 10px
const agentMenuMaxHeight = computed(() => `calc(100vh - ${170 + 119 + 10}px)`)
// 对话菜单最大高度：100vh 减去顶部区域与底部批量操作区域，刚好停在上边界
// 顶部≈100(头部) + 16(间距) + 38(搜索框) + 16(间距) ≈ 170
// 底部≈65(批量操作容器) + 23(底部间距) ≈ 88
const chatMenuMaxHeight = computed(() => `calc(100vh - ${170 + 88}px)`)
const isPersonalHover = ref(false) // 个人知识库悬浮态
const isCommonHover = ref(false) // 公共知识库悬浮态
const route = useRoute() // 路由信息对象
const loginForm = ref({
  // 登录弹窗信息对象
  username: '',
  password: ''
})
const avatarUrl = ref(photo) // 左下角用户头像
const titleQuestion = ref('')
const titleQuestionIntel = ref('')
const titleIndex = ref('')
const titleId = ref('')
const isPowerFile = ref(true)
const searchTextIntel = ref('')
const searchText = ref('')
const currentAgentChatId = ref('')
const {
  currentQuestion,
  newQuestion,
  isSampleStop,
  isQueryStop,
  currentIndex,
  limitLoading,
  limitTranLoading,
  limitQueryLoading,
  limitIntelLoading,
  currentId,
  pageType,
  selectedMode,
  currentObj,
  tipQuery,
  streamingQuestion,
  userInfo,
  activeIndex,
  activeIndexIntel,
  queryTypes,
  chatQuery,
  chatCurrent,
  isLogin,
  finalIng,
  docIng,
  tranIng,
  dynamicRows,
  isSampleLoad,
  transData,
  transFile,
  finalFile,
  transQuest,
  selectedLan,
  finalData,
  finalQuest,
  messageContainer,
  deepType,
  fileObj,
  fileAry,
  fileInputAry,
  contentType,
  knowSelect,
  isNet,
  currentIntel,
  selectType,
  intelQuestion,
  isIntelStop,
  drayAry,
  isAgentDetail,
  agentChatList,
  conversationId,
  currentAgentType,
  useKnowledge,
  chatList,
  loadingIntelId,
  isIntelLoad,
  limitId,
  limitTranId,
  useTranslationDocument,
  translationDocumentProcess,
  translationDocumentFinal,
  userInputContent
} = useShared()

const departmentList = ref([]) // 部门列表
const selectedDepartmentId = ref('') // 当前选中的部门ID（字符串形式）
const departmentFoldersMap = reactive({}) // 部门对应的文件夹列表映射
const departmentActiveFolderIndexMap = reactive({}) // 部门对应的激活文件夹索引映射
const departmentCollapseMap = reactive({}) // 部门折叠状态映射
const creatingDepartmentId = ref(null) // 当前操作的部门ID

const departmentSections = computed(() => departmentList.value)
const hasDepartmentKnowledge = computed(() => departmentList.value.length > 0)

const toDepartmentKey = value => (value == null ? '' : String(value))

const findDepartmentByKey = key => departmentList.value.find(department => department.id === key)

const getDepartmentRawId = key => {
  const department = findDepartmentByKey(key)
  return department ? department.rawId : key
}

const ensureDepartmentState = key => {
  if (!key) {
    return
  }
  if (!(key in departmentFoldersMap)) {
    departmentFoldersMap[key] = []
  }
  if (!(key in departmentActiveFolderIndexMap)) {
    departmentActiveFolderIndexMap[key] = ''
  }
  if (!(key in departmentCollapseMap)) {
    departmentCollapseMap[key] = true
  }
}

const syncActiveDepartmentState = () => {
  const key = selectedDepartmentId.value
  if (key && departmentFoldersMap[key]) {
    folderLists[KnowledgeSelect.DEPARTMENT] = departmentFoldersMap[key]
    activeFolderIndexMap[KnowledgeSelect.DEPARTMENT] = departmentActiveFolderIndexMap[key] ?? ''
  } else {
    folderLists[KnowledgeSelect.DEPARTMENT] = []
    activeFolderIndexMap[KnowledgeSelect.DEPARTMENT] = ''
  }
}

const collapseAllDepartments = (expandedId = null) => {
  const targetKey = toDepartmentKey(expandedId)
  departmentList.value.forEach(department => {
    const key = department.id
    departmentCollapseMap[key] = targetKey && key === targetKey ? false : true
  })
}

const setSelectedDepartment = departmentId => {
  const key = toDepartmentKey(departmentId)
  if (!key) {
    selectedDepartmentId.value = ''
    syncActiveDepartmentState()
    return
  }
  ensureDepartmentState(key)
  selectedDepartmentId.value = key
  syncActiveDepartmentState()
}

const isDepartmentSelected = departmentId =>
  knowSelect.value === KnowledgeSelect.DEPARTMENT && toDepartmentKey(departmentId) === selectedDepartmentId.value

const getDepartmentFolderList = departmentId => {
  const key = toDepartmentKey(departmentId)
  return departmentFoldersMap[key] || []
}

const getDepartmentActiveIndex = departmentId => {
  const key = toDepartmentKey(departmentId)
  const value = departmentActiveFolderIndexMap[key]
  return value == null ? '' : value
}

const isDepartmentCollapsed = departmentId => {
  const key = toDepartmentKey(departmentId)
  const value = departmentCollapseMap[key]
  return value == null ? true : value
}
// 右键悬浮按钮定位容器与状态
const asideRightRef = ref(null)
const showContextBtn = ref(false)
const contextBtnTop = ref('0px')
const contextBtnLeft = ref('0px')
// 批量删除模式：控制 menu-item 的复选框显示
const showBulkDeleteMode = ref(false)
const selectedMenuIds = ref([]) // 存储选中的菜单项ID

// 智能体批量删除模式
const showAgentBulkDeleteMode = ref(false)
const selectedAgentMenuIds = ref([]) // 存储选中的智能体菜单项ID

// 计算是否全选
const isAllSelected = computed(() => {
  if (chatList.value.length === 0) return false
  return selectedMenuIds.value.length === chatList.value.length
})

// 计算智能体是否全选
const isAgentAllSelected = computed(() => {
  if (agentChatList.value.length === 0) return false
  return selectedAgentMenuIds.value.length === agentChatList.value.length
})

// 批量删除弹窗标题
const bulkDeleteTitle = computed(() => {
  const count = selectedMenuIds.value.length
  return `确认删除这${count}个对话记录吗？`
})

// 批量删除弹窗描述
const bulkDeleteDescription = computed(() => {
  return "删除后对话记录无法恢复和找回，请谨慎操作"
})

// 智能体批量删除弹窗标题
const agentBulkDeleteTitle = computed(() => {
  const count = selectedAgentMenuIds.value.length
  return `确认删除这${count}个智能体对话吗？`
})

// 智能体批量删除弹窗描述
const agentBulkDeleteDescription = computed(() => {
  return "删除后智能体对话无法恢复和找回，请谨慎操作"
})

// 智能体批量删除确认弹窗状态
const agentBulkDeleteConfirmVisible = ref(false) // 是否展示智能体批量删除确认对话框

const toggleBulkDeleteMode = () => {
  showBulkDeleteMode.value = !showBulkDeleteMode.value
  if (!showBulkDeleteMode.value) {
    // 退出批量删除模式时清空选中列表
    selectedMenuIds.value = []
    // 隐藏右键悬浮按钮
    showContextBtn.value = false
  }
}

// 智能体批量删除模式切换
const toggleAgentBulkDeleteMode = () => {
  showAgentBulkDeleteMode.value = !showAgentBulkDeleteMode.value
  if (!showAgentBulkDeleteMode.value) {
    // 退出批量删除模式时清空选中列表
    selectedAgentMenuIds.value = []
    // 隐藏右键悬浮按钮
    showContextBtn.value = false
  }
}

// 全选/取消全选处理
const handleSelectAll = (menuId, isSelected) => {
  if (isSelected) {
    // 全选：将所有菜单项ID添加到选中数组
    selectedMenuIds.value = chatList.value.map(chat => chat.id)
  } else {
    // 取消全选：清空选中数组
    selectedMenuIds.value = []
  }
}

// 智能体全选/取消全选处理
const handleAgentSelectAll = (menuId, isSelected) => {
  if (isSelected) {
    // 全选：将所有智能体菜单项ID添加到选中数组
    selectedAgentMenuIds.value = agentChatList.value.map(chat => chat.agentChatId)
  } else {
    // 取消全选：清空选中数组
    selectedAgentMenuIds.value = []
  }
}

// 更新选中的菜单项ID
const updateSelectedMenuIds = (menuId, isSelected) => {
  if (isSelected) {
    if (!selectedMenuIds.value.includes(menuId)) {
      selectedMenuIds.value.push(menuId)
    }
  } else {
    const index = selectedMenuIds.value.indexOf(menuId)
    if (index > -1) {
      selectedMenuIds.value.splice(index, 1)
    }
  }
}

// 更新选中的智能体菜单项ID
const updateSelectedAgentMenuIds = (menuId, isSelected) => {
  if (isSelected) {
    if (!selectedAgentMenuIds.value.includes(menuId)) {
      selectedAgentMenuIds.value.push(menuId)
    }
  } else {
    const index = selectedAgentMenuIds.value.indexOf(menuId)
    if (index > -1) {
      selectedAgentMenuIds.value.splice(index, 1)
    }
  }
}

const onElMenuContextMenu = (event) => {
  // 只在通用对话和智能体页面显示右键批量删除按钮，个人/部门知识库页面不显示
  if (selectType.value === ContentType.KNOWLEDGE && (knowSelect.value === KnowledgeSelect.PERSONAL || knowSelect.value === KnowledgeSelect.DEPARTMENT)) {
    return
  }

  try {
    const rect = asideRightRef.value?.getBoundingClientRect()
    if (rect) {
      // 计算相对于 aside_right 的坐标
      let left = event.clientX - rect.left
      let top = event.clientY - rect.top
      // 尺寸与边距
      const btnW = 110
      const btnH = 54
      const padding = 4
      // 边界校正，避免溢出容器
      if (left + btnW + padding > rect.width) left = Math.max(padding, rect.width - btnW - padding)
      if (top + btnH + padding > rect.height) top = Math.max(padding, rect.height - btnH - padding)
      if (left < padding) left = padding
      if (top < padding) top = padding
      contextBtnLeft.value = left + 'px'
      contextBtnTop.value = top + 'px'
      showContextBtn.value = true
    } else {
      showContextBtn.value = true
      contextBtnLeft.value = event.clientX + 'px'
      contextBtnTop.value = event.clientY + 'px'
    }
  } catch (_) {
    showContextBtn.value = true
  }
}
const hideContextBtn = () => {
  showContextBtn.value = false
}
const updateIsMobile = () => {
  isMobile.value = window.innerWidth <= 768
}
onMounted(() => {
  updateIsMobile()
  if (isMobile.value) {
    isCollapsed.value = true
    eventBus.emit('setCollapsed', isCollapsed.value)
  }
  window.addEventListener('resize', updateIsMobile)
  document.addEventListener('click', hideContextBtn)
})
onUnmounted(() => {
  window.removeEventListener('resize', updateIsMobile)
  document.removeEventListener('click', hideContextBtn)
  eventBus.off('toggleCollapse', toggleCollapse)
})
// 校验用户登录信息
const rules = {
  username: [{ required: true, message: '请输入工号(用户名)', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const passwordVisible = ref(false)
const titleVisible = ref(false)
const titleVisibleIntel = ref(false)
const createFolderVisible = ref(false) // 是否展示新建文件夹对话框
const renameFolderVisible = ref(false) // 是否展示重命名文件夹对话框
const deleteConfirmVisible = ref(false) // 是否展示删除确认对话框
const deleteChatConfirmVisible = ref(false) // 是否展示对话删除确认对话框
const deleteAgentConfirmVisible = ref(false) // 是否展示智能体删除确认对话框
const bulkDeleteConfirmVisible = ref(false) // 是否展示批量删除确认对话框
const folderName = ref('') // 文件夹名称
const creatingFolderType = ref(KnowledgeSelect.PERSONAL) // 当前新建文件夹的知识库类型
const folderLists = reactive({
  [KnowledgeSelect.PERSONAL]: [],
  [KnowledgeSelect.DEPARTMENT]: []
}) // 文件夹列表映射（部门数据会同步为当前选中的部门）
const activeFolderIndexMap = reactive({
  [KnowledgeSelect.PERSONAL]: '',
  [KnowledgeSelect.DEPARTMENT]: ''
}) // 当前选中的文件夹索引映射（部门数据会同步为当前选中的部门）
const currentEditFolder = ref(null) // 当前编辑的文件夹信息
const currentDeleteItem = ref(null) // 当前要删除的项目信息
const currentDeleteChatItem = ref(null) // 当前要删除的对话项目信息
const currentDeleteAgentItem = ref(null) // 当前要删除的智能体项目信息
const isPersonalFolderCollapsed = ref(false) // 是否折叠个人知识库文件夹列表（默认展开个人知识库）
const isPublicKnowledgeCollapsed = ref(true) // 是否折叠公用知识库列表（默认折叠）
// 当前url的路由信息(由luxshare传来的参数)
const queryParams = route.query
const emit = defineEmits(['change-history', 'set-isLaw', 'set-message', 'set-FileModel', 'setNet', 'fetch-directory-detail', 'clear-chat-history'])

const getFolderListByType = (type, options = {}) => {
  if (type === KnowledgeSelect.DEPARTMENT) {
    const departmentId = options.departmentId ?? selectedDepartmentId.value
    return getDepartmentFolderList(departmentId)
  }
  return folderLists[type] || []
}

const getFolderRequestParamsByType = (type, options = {}) => {
  if (type === KnowledgeSelect.DEPARTMENT) {
    const departmentKey = toDepartmentKey(options.departmentId ?? selectedDepartmentId.value ?? userInfo.value.departmentId)
    return {
      id: getDepartmentRawId(departmentKey),
      isDepartment: true,
      departmentKey
    }
  }

  return {
    id: userInfo.value.id,
    isDepartment: false
  }
}

const getFileModelByType = type => {
  if (type === KnowledgeSelect.DEPARTMENT) {
    return FileModel.DEPARTMENT
  }
  if (type === KnowledgeSelect.PUBLIC) {
    return FileModel.PUBLIC
  }
  return FileModel.PERSONAL
}

const setCollapseStateByType = (type, options = {}) => {
  if (type === KnowledgeSelect.PERSONAL) {
    isPersonalFolderCollapsed.value = false
    isPublicKnowledgeCollapsed.value = true
    collapseAllDepartments()
  } else if (type === KnowledgeSelect.DEPARTMENT) {
    isPersonalFolderCollapsed.value = true
    isPublicKnowledgeCollapsed.value = true
    collapseAllDepartments(options.departmentId ?? selectedDepartmentId.value)
  } else if (type === KnowledgeSelect.PUBLIC) {
    isPersonalFolderCollapsed.value = true
    isPublicKnowledgeCollapsed.value = false
    collapseAllDepartments()
  }
}

// 左上角折叠控制函数
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
  eventBus.emit('setCollapsed', isCollapsed.value)
  // emit('set-message', isCollapsed.value)
}
const handleTitleConfirm = (value) => {
  changeTitle(titleId.value, value)
}

const handleTitleCancel = () => {
  titleQuestion.value = ''
  titleIndex.value = ''
}

const handleTitleConfirmIntel = async (value) => {
  const operationFn = changeTitleOperationMap.get(currentAgentType.value)
  if (operationFn) {
    let changeTitleResult = await operationFn(currentAgentChatId.value, value)
    if (changeTitleResult.status) {
      ElMessage.success('修改成功')
      // 保持搜索条件刷新
      eventBus.emit('getHistoryData', searchTextIntel.value || '')
    } else {
      ElMessage.error(changeTitleResult.message)
    }
  }
}

const handleTitleCancelIntel = () => {
  titleQuestionIntel.value = ''
}

// 新建文件夹相关处理函数
const handleCreateFolder = (type = knowSelect.value, options = {}) => {
  if (!isLogin.value) {
    ElMessage.warning('请先登录再使用')
    return
  }
  creatingFolderType.value = type
  if (type === KnowledgeSelect.DEPARTMENT) {
    const targetDepartmentId = toDepartmentKey(options.departmentId ?? selectedDepartmentId.value)
    if (!targetDepartmentId) {
      ElMessage.error('未获取到部门信息，无法创建部门文件夹')
      return
    }
    creatingDepartmentId.value = targetDepartmentId
  } else {
    creatingDepartmentId.value = null
  }
  createFolderVisible.value = true
  folderName.value = ''
}

const handleCreateFolderConfirm = async (value) => {
  if (!isLogin.value || !userInfo.value.id) {
    ElMessage.error('请先登录')
    return
  }
  
  if (!value || value.trim() === '') {
    ElMessage.error('请输入文件夹名称')
    return
  }
  
  try {
    const requestData = {
      folderName: value.trim(),
      userId: userInfo.value.id
    }

    let targetDepartmentIdKey = null
    if (creatingFolderType.value === KnowledgeSelect.DEPARTMENT) {
      targetDepartmentIdKey = toDepartmentKey(creatingDepartmentId.value ?? selectedDepartmentId.value)
      if (!targetDepartmentIdKey) {
        ElMessage.error('未获取到部门信息，无法创建部门文件夹')
        return
      }
      requestData.departmentId = getDepartmentRawId(targetDepartmentIdKey)
    }

    const response = await createFolder(requestData)

    if (response.status) {
      ElMessage.success('创建成功')
      const targetType = creatingFolderType.value
      // 刷新对应类型的文件夹列表并选中新建的文件夹
      await fetchFolderList(targetType, { departmentId: targetDepartmentIdKey })
      knowSelect.value = targetType
      setCollapseStateByType(targetType, { departmentId: targetDepartmentIdKey })
      emit('set-FileModel', getFileModelByType(targetType))

      const currentList = getFolderListByType(targetType, { departmentId: targetDepartmentIdKey })
      let createdFolderId = response.data && (response.data.id || response.data.folderId)
      let createdIndex = -1
      if (createdFolderId) {
        createdIndex = currentList.findIndex(f => f.id === createdFolderId)
      }
      if (createdIndex === -1) {
        createdIndex = currentList.findIndex(f => f.folderName === requestData.folderName)
      }
      if (createdIndex !== -1) {
        const created = currentList[createdIndex]
        handleFolderSelect(created.id, createdIndex, targetType, false, { departmentId: targetDepartmentIdKey })
      } else if (!currentList.length) {
        emit('set-FileModel', FileModel.EMPTY)
      }
    } else {
      ElMessage.error(response.message || '创建失败')
    }
  } catch (error) {
    if (error?.response?.data?.message) {
      ElMessage.error(error?.response?.data?.message)
    } else {
      console.error('创建文件夹出错:', error)
      ElMessage.error('创建文件夹失败，请稍后重试')
    }
  }
  creatingDepartmentId.value = null
}

const handleCreateFolderCancel = () => {
  folderName.value = ''
  creatingDepartmentId.value = null
}

const handleRenameFolderConfirm = async (value) => {
  if (!isLogin.value || !userInfo.value.id || !currentEditFolder.value) {
    ElMessage.error('请先登录')
    return
  }
  
  if (!value || value.trim() === '') {
    ElMessage.error('请输入文件夹名称')
    return
  }
  
  try {
    const requestData = {
      id: currentEditFolder.value.id,
      folderName: value.trim(),
      userId: userInfo.value.id
    }

    let targetDepartmentIdKey = null
    if (currentEditFolder.value.type === KnowledgeSelect.DEPARTMENT) {
      targetDepartmentIdKey = toDepartmentKey(currentEditFolder.value.departmentId ?? selectedDepartmentId.value)
      if (!targetDepartmentIdKey) {
        ElMessage.error('未获取到部门信息，无法修改部门文件夹')
        return
      }
      requestData.departmentId = getDepartmentRawId(targetDepartmentIdKey)
    }
    
    const response = await createFolder(requestData)

    if (response.status) {
      ElMessage.success('修改成功')
      const targetType = currentEditFolder.value.type || KnowledgeSelect.PERSONAL
      // 保存当前编辑的文件夹ID，用于重命名后选中
      const editedFolderId = currentEditFolder.value.id
      // 清空当前编辑的文件夹信息
      currentEditFolder.value = null
      // 刷新对应类型的文件夹列表
      await fetchFolderList(targetType, { departmentId: targetDepartmentIdKey })
      const currentList = getFolderListByType(targetType, { departmentId: targetDepartmentIdKey })
      const folderIndex = currentList.findIndex(folder => folder.id === editedFolderId)
      if (folderIndex !== -1) {
        knowSelect.value = targetType
        setCollapseStateByType(targetType, { departmentId: targetDepartmentIdKey })
        handleFolderSelect(editedFolderId, folderIndex, targetType, false, { departmentId: targetDepartmentIdKey })
      }
    } else {
      ElMessage.error(response.message || '修改失败')
    }
  } catch (error) {
    if (error?.response?.data?.message) {
      ElMessage.error(error?.response?.data?.message)
    } else {
      console.error('重命名文件夹出错:', error)
      ElMessage.error('重命名文件夹失败，请稍后重试')
    }
  }
}

const handleRenameFolderCancel = () => {
  folderName.value = ''
  currentEditFolder.value = null
}

 

// 获取文件夹列表
const fetchFolderList = async (type = KnowledgeSelect.PERSONAL, options = {}) => {
  if (!isLogin.value || !userInfo.value.id) {
    return
  }

  if (type === KnowledgeSelect.PUBLIC) {
    return
  }

  const { id, isDepartment, departmentKey } = getFolderRequestParamsByType(type, options)

  if (isDepartment && !hasDepartmentKnowledge.value) {
    folderLists[type] = []
    if (departmentKey) {
      ensureDepartmentState(departmentKey)
      departmentFoldersMap[departmentKey] = []
      if (departmentKey === selectedDepartmentId.value) {
        syncActiveDepartmentState()
      }
    }
    return
  }

  if (!id) {
    if (isDepartment) {
      ElMessage.error('未获取到部门信息，无法加载部门知识库')
    }
    console.error('获取文件夹列表缺少必要的查询参数')
    return
  }

  try {
    const response = await getFolderList(id, isDepartment)
    if (response.status) {
      if (isDepartment) {
        ensureDepartmentState(departmentKey)
        departmentFoldersMap[departmentKey] = response.data || []
        if (departmentKey === selectedDepartmentId.value) {
          syncActiveDepartmentState()
        }
      } else {
        folderLists[type] = response.data || []
      }

      const currentList = getFolderListByType(type, { departmentId: departmentKey })
      const shouldHandleDepartment =
        isDepartment && departmentKey === selectedDepartmentId.value && selectType.value === ContentType.KNOWLEDGE && knowSelect.value === type
      const shouldHandlePersonal =
        !isDepartment && selectType.value === ContentType.KNOWLEDGE && knowSelect.value === type

      if (shouldHandleDepartment || shouldHandlePersonal) {
        if (!currentList || currentList.length === 0) {
          emit('set-FileModel', FileModel.EMPTY)
          if (isDepartment && departmentKey === selectedDepartmentId.value) {
            departmentActiveFolderIndexMap[departmentKey] = ''
            activeFolderIndexMap[KnowledgeSelect.DEPARTMENT] = ''
          } else if (!isDepartment) {
            activeFolderIndexMap[type] = ''
          }
        } else {
          emit('set-FileModel', getFileModelByType(type))
          if (isDepartment && departmentKey === selectedDepartmentId.value) {
            const activeIndex = departmentActiveFolderIndexMap[departmentKey]
            const normalizedIndex =
              typeof activeIndex === 'number' &&
              activeIndex >= 0 &&
              activeIndex < currentList.length
                ? activeIndex
                : 0
            const targetFolder = currentList[normalizedIndex]
            if (targetFolder) {
              handleFolderSelect(targetFolder.id, normalizedIndex, type, false, { departmentId: departmentKey })
            }
          } else if (!isDepartment) {
            const activeIndex = activeFolderIndexMap[type]
            const normalizedIndex =
              typeof activeIndex === 'number' &&
              activeIndex >= 0 &&
              activeIndex < currentList.length
                ? activeIndex
                : 0
            const targetFolder = currentList[normalizedIndex]
            if (targetFolder) {
              handleFolderSelect(targetFolder.id, normalizedIndex, type)
            }
          }
        }
      }
    } else {
      console.error('获取文件夹列表失败:', response.message)
    }
  } catch (error) {
    console.error('获取文件夹列表出错:', error)
  }
}

// 文件夹相关处理函数
const handleFolderSelect = (folderId, index, mode, collapse = false, options = {}) => {
  let targetDepartmentIdKey = null
  if (mode === KnowledgeSelect.DEPARTMENT) {
    targetDepartmentIdKey = toDepartmentKey(options.departmentId ?? selectedDepartmentId.value)
    if (!targetDepartmentIdKey) {
      ElMessage.error('未获取到部门信息，无法加载部门知识库')
      return false
    }
    setSelectedDepartment(targetDepartmentIdKey)
    departmentActiveFolderIndexMap[targetDepartmentIdKey] = index
    activeFolderIndexMap[KnowledgeSelect.DEPARTMENT] = index
  } else {
    activeFolderIndexMap[mode] = index
  }
  // 这里可以添加选择文件夹后的逻辑
  if (!isLogin.value) {
    ElMessage.warning('请先登录再使用')
    return false
  }
  knowSelect.value = mode
  setCollapseStateByType(mode, { departmentId: targetDepartmentIdKey })
  ItemSelect.value = 0

  const currentList = getFolderListByType(mode, { departmentId: targetDepartmentIdKey })
  const selectedFolder = currentList.find(f => f.id === folderId)
  const folderTitle = selectedFolder ? selectedFolder.folderName : '文件夹'

  let directoryInfo = {
    folderId: folderId,
    folderName: folderTitle, // 添加文件夹名称
    mode: mode
  }
  if (mode === KnowledgeSelect.DEPARTMENT) {
    directoryInfo.departmentId = getDepartmentRawId(targetDepartmentIdKey)
  }
  emit('fetch-directory-detail', directoryInfo)
  // 触发清空对话事件
  emit('clear-chat-history')
  if (isMobile.value && collapse) {
    toggleCollapse()
  }
}

const handleEditFolder = (folderNameParam, folderId, type, options = {}) => {
  if (!isLogin.value || !userInfo.value.id) {
    ElMessage.error('请先登录')
    return
  }

  // 设置当前编辑的文件夹信息
  currentEditFolder.value = {
    id: folderId,
    folderName: folderNameParam,
    type,
    departmentId: type === KnowledgeSelect.DEPARTMENT ? toDepartmentKey(options.departmentId ?? selectedDepartmentId.value) : null
  }
  
  // 设置对话框的默认值
  folderName.value = folderNameParam
  
  // 显示重命名对话框
  renameFolderVisible.value = true
}

const handleConfirmDeleteFolder = async (folderId, knowledgeType, options = {}) => {
  // 设置当前要删除的文件夹信息
  const departmentIdKey = knowledgeType === KnowledgeSelect.DEPARTMENT ? toDepartmentKey(options.departmentId ?? selectedDepartmentId.value) : null
  const currentList = getFolderListByType(knowledgeType, { departmentId: departmentIdKey })
  currentDeleteItem.value = {
    type: 'folder',
    id: folderId,
    knowledgeType,
    departmentId: departmentIdKey,
    name: currentList.find(f => f.id === folderId)?.folderName || '文件夹'
  }

  // 显示删除确认弹窗
  deleteConfirmVisible.value = true
}

const handleConfirmDeleteItem = async () => {
  if (!currentDeleteItem.value) return

  if (!isLogin.value || !userInfo.value.id) {
    ElMessage.error('请先登录')
    return
  }

  try {
    let response
    if (currentDeleteItem.value.type === 'folder') {
      response = await deleteFolder(currentDeleteItem.value.id, userInfo.value.id)
    }

    if (response && response.status) {
      ElMessage.success('删除成功')
      const targetType = currentDeleteItem.value.knowledgeType || KnowledgeSelect.PERSONAL
      const targetDepartmentIdKey = currentDeleteItem.value.departmentId
      // 清空当前选中的文件夹索引
      if (targetType === KnowledgeSelect.DEPARTMENT) {
        if (targetDepartmentIdKey) {
          departmentActiveFolderIndexMap[targetDepartmentIdKey] = ''
        }
        activeFolderIndexMap[KnowledgeSelect.DEPARTMENT] = ''
      } else {
        activeFolderIndexMap[targetType] = ''
      }
      // 刷新对应类型的文件夹列表
      await fetchFolderList(targetType, { departmentId: targetDepartmentIdKey })
      if (selectType.value === ContentType.KNOWLEDGE && knowSelect.value === targetType) {
        const currentList = getFolderListByType(targetType, { departmentId: targetDepartmentIdKey })
        if (!currentList || currentList.length === 0) {
          emit('set-FileModel', FileModel.EMPTY)
        } else {
          emit('set-FileModel', getFileModelByType(targetType))
          handleFolderSelect(currentList[0].id, 0, targetType, false, { departmentId: targetDepartmentIdKey })
        }
      }
    } else {
      ElMessage.error(response?.message || '删除失败')
    }
  } catch (error) {
    const responseMessage = error?.response?.data?.message
    const responseCode = error?.response?.data?.code
    console.error('删除出错:', error)
    ElMessage.error(
      responseCode === 401 && responseMessage ? responseMessage : '删除失败，请稍后重试'
    )
  }
  
  // 清空当前删除项目信息
  currentDeleteItem.value = null
}

const createNewConversation = () => {
  eventBus.emit('createNewConversation')
}

const popoverVisible = reactive({})
const popoverVisibleIntel = reactive({})

const getStoredUserInfo = () => {
  const storedUserInfo = localStorage.getItem('userInfo')
  if (!storedUserInfo || storedUserInfo === 'null' || storedUserInfo === 'undefined') {
    return null
  }

  try {
    return JSON.parse(storedUserInfo)
  } catch (error) {
    console.error('解析本地用户信息失败:', error)
    return null
  }
}

const refreshDepartmentList = async () => {
  if (!userInfo.value || !userInfo.value.id) {
    departmentList.value = []
    selectedDepartmentId.value = ''
    folderLists[KnowledgeSelect.DEPARTMENT] = []
    activeFolderIndexMap[KnowledgeSelect.DEPARTMENT] = ''
    return
  }

  let departmentData = []
  // userInfo.value.id = '10801390'
  // localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  try {
    const response = await getDepartmentInfoByUserId(userInfo.value.id)
    if (response?.status) {
      departmentData = response.data || []
    }
  } catch (error) {
    console.error('获取处级干部部门信息失败', error)
  }

  const normalizedDepartments = departmentData
    .map(department => {
      const rawId = department?.id ?? department?.departmentId
      const key = toDepartmentKey(rawId)
      const rawNameSource = department?.name ?? department?.department
      const rawName = typeof rawNameSource === 'string' ? rawNameSource.trim() : ''
      if (!key || !rawName) {
        return null
      }
      const displayName = rawName.length > 7 ? `${rawName.slice(0, 7)}...` : rawName
      return {
        id: key,
        rawId,
        name: rawName,
        displayName,
        level: department.level,
        levelCode: department.levelCode
      }
    })
    .filter(Boolean)

  departmentList.value = normalizedDepartments

  const validKeys = new Set(normalizedDepartments.map(item => item.id))
  Object.keys(departmentFoldersMap).forEach(key => {
    if (!validKeys.has(key)) {
      delete departmentFoldersMap[key]
    }
  })
  Object.keys(departmentActiveFolderIndexMap).forEach(key => {
    if (!validKeys.has(key)) {
      delete departmentActiveFolderIndexMap[key]
    }
  })
  Object.keys(departmentCollapseMap).forEach(key => {
    if (!validKeys.has(key)) {
      delete departmentCollapseMap[key]
    }
  })

  if (normalizedDepartments.length === 0) {
    selectedDepartmentId.value = ''
    folderLists[KnowledgeSelect.DEPARTMENT] = []
    activeFolderIndexMap[KnowledgeSelect.DEPARTMENT] = ''
    return
  }

  normalizedDepartments.forEach(department => {
    ensureDepartmentState(department.id)
  })

  if (!validKeys.has(selectedDepartmentId.value)) {
    selectedDepartmentId.value = normalizedDepartments[0].id
  }

  collapseAllDepartments(selectedDepartmentId.value)
  syncActiveDepartmentState()
  await fetchFolderList(KnowledgeSelect.DEPARTMENT, { departmentId: selectedDepartmentId.value })
}

//获取用户信息接口
const getUserInfo = async (id, options = {}) => {
  if (!id) {
    console.error('获取用户信息失败，缺少用户ID')
    return null
  }

  const { refreshAfterFetch = true } = options

  try {
    const res = await request.post('/UserInfo/getUserInfoById?id=' + id)
    if (res.status) {
      localStorage.setItem('userInfo', JSON.stringify(res.data))
      userInfo.value = res.data
      userInfo.value.url = 'https://dcs.luxshare-ict.com/Upload/emp_photo/' + userInfo.value.id + '.jpg?cp=zhaopian'

      if (powerList.value.includes(userInfo.value.id)) {
        localStorage.setItem('enableLaw', true)
        localStorage.setItem('enableBoardOffice', true)
      } else {
        localStorage.setItem('enableLaw', false)
        localStorage.setItem('enableBoardOffice', false)
      }

      await refreshDepartmentList()

      if (refreshAfterFetch) {
        nextTick(() => {
          getPower()
          emit('set-isLaw')
          emit('change-history')
        })
      }
    }

    return res
  } catch (error) {
    console.error('获取用户信息失败:', error)
    return null
  }
}

const changeContent = async val => {
  isAgentDetail.value = false
  conversationId.value = ''
  if (!isLogin.value) {
    ElMessage.warning('请先登录再使用')
    return false
  }
  drayAry.value = []
  if (val === 3) {
    for (var i = 0; i < 50; i++) {
      popoverVisible[i] = false
      popoverVisibleIntel[i] = false
    }
  }
  selectType.value = val
  contentType.value = val
  ItemSelect.value = 0
  fileInputAry.value = []
  emit('set-message', contentType.value)
  
  // 切换页面时重置批量删除状态，避免状态互相影响
  showBulkDeleteMode.value = false
  showAgentBulkDeleteMode.value = false
  selectedMenuIds.value = []
  selectedAgentMenuIds.value = []
  showContextBtn.value = false
  // 重置批量删除确认弹窗状态
  bulkDeleteConfirmVisible.value = false
  agentBulkDeleteConfirmVisible.value = false
  
  // 如果切换到知识库页面，获取文件夹列表
  if (val === ContentType.KNOWLEDGE) {
    let targetUserId = userInfo.value?.id
    if (!targetUserId) {
      const storedUser = getStoredUserInfo()
      if (storedUser && storedUser.id) {
        targetUserId = storedUser.id
      }
    }

    if (targetUserId) {
      await getUserInfo(targetUserId, { refreshAfterFetch: false })
    }

    await fetchFolderList(knowSelect.value, { departmentId: selectedDepartmentId.value })
  } else if (val === ContentType.AGENT && isMobile.value) {
    isCollapsed.value = true
    eventBus.emit('setCollapsed', isCollapsed.value)
  }
}
const changeFileModel = async (val, options = {}) => {
  if (!isLogin.value) {
    ElMessage.warning('请先登录再使用')
    return false
  }
  if (val === KnowledgeSelect.DEPARTMENT && !hasDepartmentKnowledge.value) {
    return
  }
  let targetDepartmentIdKey = null
  if (val === KnowledgeSelect.DEPARTMENT) {
    const fallbackDepartmentId = departmentList.value.length > 0 ? departmentList.value[0].id : ''
    targetDepartmentIdKey = toDepartmentKey(options.departmentId ?? selectedDepartmentId.value ?? fallbackDepartmentId)
    if (!targetDepartmentIdKey) {
      ElMessage.error('未获取到部门信息，无法加载部门知识库')
      return
    }
    setSelectedDepartment(targetDepartmentIdKey)
  }
  knowSelect.value = val
  ItemSelect.value = 0
  setCollapseStateByType(val, { departmentId: targetDepartmentIdKey })
  if (val === KnowledgeSelect.PUBLIC) {
    // 公共知识库
    emit('set-FileModel', FileModel.PUBLIC)
    if (powerArr.value.length > 0) {
      const targetIndex = Math.min(ItemSelect.value, powerArr.value.length - 1)
      knowItemSelect(targetIndex, { triggerCollapse: false })
    }
    return
  }
  await fetchFolderList(val, { departmentId: targetDepartmentIdKey })

  const currentList = getFolderListByType(val, { departmentId: targetDepartmentIdKey })
  if (!currentList || currentList.length === 0) {
    emit('set-FileModel', FileModel.EMPTY)
    if (val === KnowledgeSelect.DEPARTMENT && targetDepartmentIdKey) {
      departmentActiveFolderIndexMap[targetDepartmentIdKey] = ''
      activeFolderIndexMap[KnowledgeSelect.DEPARTMENT] = ''
    } else {
      activeFolderIndexMap[val] = ''
    }
    return
  }

  emit('set-FileModel', getFileModelByType(val))
}

const handleDepartmentChangeFileModel = (departmentId) => {
  const targetDepartmentIdKey = toDepartmentKey(departmentId ?? selectedDepartmentId.value)
  setSelectedDepartment(targetDepartmentIdKey)
  changeFileModel(KnowledgeSelect.DEPARTMENT, { departmentId: targetDepartmentIdKey })
}

const handleDepartmentCreateFolder = (departmentId) => {
  handleCreateFolder(KnowledgeSelect.DEPARTMENT, { departmentId })
}

const handleDepartmentEditFolder = ({ folderName, folderId, departmentId }) => {
  handleEditFolder(folderName, folderId, KnowledgeSelect.DEPARTMENT, { departmentId })
}

const handleDepartmentDeleteFolder = ({ folderId, departmentId }) => {
  handleConfirmDeleteFolder(folderId, KnowledgeSelect.DEPARTMENT, { departmentId })
}

const handleDepartmentSelectFolder = ({ folderId, index, departmentId }) => {
  handleFolderSelect(folderId, index, KnowledgeSelect.DEPARTMENT, true, { departmentId })
}
const knowItemSelect = (val, options = {}) => {
  const { triggerCollapse = true } = options
  if (!isLogin.value) {
    ElMessage.warning('请先登录再使用')
    return false
  }
  const targetItem = powerArr.value?.[val]
  if (!targetItem) {
    return
  }
  ItemSelect.value = val

  const data = targetItem.target
  if (data !== 'HR' && data !== 'IT') {
    if (!isNet.value) {
      return
      ElMessage.warning('该模式仅支持通过office网络访问')
    }
  }
  eventBus.emit('changeKnow', targetItem)
  if (triggerCollapse && isMobile.value) {
    toggleCollapse()
  }
}

const handleEdit = (val, chatId) => {
  if (isSampleLoad.value || finalIng.value) {
    ElMessage.warning('有问题正在回答中，请稍后再修改')
    return
  }
  titleVisible.value = true
  titleQuestion.value = val
  titleId.value = chatId
}
const handleEditIntel = (val, agentChatId) => {
  titleVisibleIntel.value = true
  titleQuestionIntel.value = val
  currentAgentChatId.value = agentChatId
  // 重命名以后 被选中的清空
  activeIndexIntel.value = ''
}

const changeTitleOperationMap = new Map([
  [COMPARE_AGENT_TYPE, async (chatId, title) => {
    return await changeImgRecognitionChatTitle(chatId, title)
  }],
  [DEFAULT_AGENT_TYPE, async (chatId, title) => {
    return await changeAgentChatTitle(chatId, title)
  }],
  [TABLE_AGENT_TYPE, async (chatId, title) => {
    return await updateExcelChatTitle(chatId, title)
  }],
  [RESUME_AGENT_TYPE, async (chatId, title) => {
    return await changeResumeTaskTitleById(chatId, title)
  }]
]);



const changeTitle = async (id, title) => {
  request
    .post('/Message/changeTitle?id=' + id + '&title=' + title)
    .then(res => {
      if (res.status) {
        titleQuestion.value = ''
        titleIndex.value = ''
        ElMessage.success('修改对话标题成功')
        // 若搜索框有内容，按关键字刷新；否则刷新全部
        if (searchText.value && searchText.value.trim() !== '') {
          eventBus.emit('fetchChatList', searchText.value)
        } else {
          emit('change-history')
        }
      } else {
        titleQuestion.value = ''
        titleIndex.value = ''
      }
    })
    .catch(err => {
      titleQuestion.value = ''
      titleIndex.value = ''
      console.error(err)
    })
}

// 登录密码框 眼睛和锁的切换
const togglePasswordVisibility = () => {
  passwordVisible.value = !passwordVisible.value
}
//登录接口
const submitForm = () => {
  request
    .get('/UserInfo/getPublicKey')
    .then(res => {
      if (res.status) {
        const password = encryptData(res.data.publicKey, loginForm.value.password)

        submitLoginForm(res.data.requestId, password)
      }
    })
    .catch(err => {
      console.error(err)
    })
}
const submitLoginForm = async (id, password) => {
  request
    .post('/UserInfo/login', {
      userid: loginForm.value.username,
      password: password,
      requestId: id
    })
    .then(res => {
      if (res.data && res.data.clientStatus === 'PASS') {
        ElMessage.success('登录成功')
        showPopup.value = false
        activeIndex.value = ''
        activeIndexIntel.value = ''
        isLogin.value = true

        getUserInfo(loginForm.value.username)
        dialogVisible.value = false
      } else if (res.data && res.data.clientStatus !== 'PASS') {
        ElMessage.error(res.data.message)
      } else {
        ElMessage.error('登录失败,请稍后再试')
      }
    })
    .catch(err => {
      console.error(err)
    })
}

// 点击开启新对话
const startNewConversation = () => {
  useTranslationDocument.value = false
  translationDocumentProcess.value = ''
  translationDocumentFinal.value = ''
  // 检查是否有问答正在进行中
  if (isSampleLoad.value || docIng.value || tranIng.value || finalIng.value || limitLoading.value || limitTranLoading.value || limitQueryLoading.value) {
    ElMessage.warning('有问答正在进行中,请稍后再试')
    return
  }
  
  // 通过事件总线检查 queryIng 状态
  eventBus.emit('checkQueryIngStatus', (isQueryIng) => {
    if (isQueryIng) {
      ElMessage.warning('有问答正在进行中,请稍后再试')
      return
    }
    // 继续执行开启新对话的逻辑
    executeStartNewConversation()
  })
  // 移动端开启新会话隐藏侧边栏
  if (isMobile.value) {
    toggleCollapse()
  }
}

// 执行开启新对话的具体逻辑
const executeStartNewConversation = () => {

  currentQuestion.value = '' // 清空当前问题输入框的内容
  newQuestion.value = '' // 清空临时存储的新问题
  tipQuery.value = '' // 清除输入框的提示文本
  dynamicRows.value = 1 // 重置输入框行数为单行
  activeIndex.value = '' // 取消左侧历史记录的高亮选中状态
  currentIndex.value = '' // 清空当前对话的索引标识
  chatQuery.messages = [] // 清空当前对话的消息数组
  chatQuery.isLoading = false // 关闭消息加载状态
  fileObj.value = '' // 清除单个文件对象
  fileAry.value = '' // 清空文件数组
  currentId.value = '' // 清空当前对话的唯一ID
  useKnowledge.value = false
  transFile.value = ''
  finalFile.value = ''

  // 根据当前选中的模式设置新对话的类型
  if (selectedMode.value === '人资行政专题') {
    pageType.value = 'query'
    selectedMode.value = '人资行政专题'
  } else if (selectedMode.value === 'IT专题') {
    pageType.value = 'it'
    selectedMode.value = 'IT专题'
  } else if (selectedMode.value === '法务专题') {
    pageType.value = 'law'
    selectedMode.value = '法务专题'
  } else if (selectedMode.value === '董办专题') {
    pageType.value = 'board'
    selectedMode.value = '董办专题'
  } else {
    // 默认通用模式
    pageType.value = 'sample'
    selectedMode.value = '通用模式'
  }

  // 切换到对应模式后刷新左侧对话列表
  emit('change-history')
}
// 点击退出登录
const handleLogout = () => {
  // 处理退出登录逻辑
  ElMessage.success('退出成功')
  localStorage.setItem('userInfo', '')
  localStorage.setItem('enableLaw', false)
  localStorage.setItem('enableBoardOffice', false)

  localStorage.setItem('powerList', [])
  departmentList.value = []
  selectedDepartmentId.value = ''
  Object.keys(departmentFoldersMap).forEach(key => delete departmentFoldersMap[key])
  Object.keys(departmentActiveFolderIndexMap).forEach(key => delete departmentActiveFolderIndexMap[key])
  Object.keys(departmentCollapseMap).forEach(key => delete departmentCollapseMap[key])
  folderLists[KnowledgeSelect.DEPARTMENT] = []
  activeFolderIndexMap[KnowledgeSelect.DEPARTMENT] = ''
  creatingDepartmentId.value = null
  selectType.value = ContentType.CONVERSATION
  contentType.value = 1
  // isPowerFile.value = false
  queryTypes.value = []
  chatQuery.messages = []
  chatQuery.isLoading = false
  isPowerFile.value = true
  currentId.value = ''
  currentQuestion.value = false
  isLogin.value = false
}
// 由luxshare带token进来,校验luxshare的合法性
const checkToken = async token => {
  request
    .post('/UserInfo/luxlinkLogin', {
      access_key: '76eb4367-a19d-4485-aadb-cea65fa8fbbe',
      tokenId: token
    })
    .then(res => {
      if (res.status) {
        ElMessage.success('登录成功')
        showPopup.value = false
        activeIndex.value = ''
        activeIndexIntel.value = ''
        isLogin.value = true
        getUserInfo(res.data.uid)
      } else {
        dialogVisible.value = true
      }
    })
    .catch(err => {
      dialogVisible.value = true
    })
}
const handleClose = done => {
  // 这里可以添加一些关闭前的逻辑
  done()
}

const removeAgentChat = new Map([
  [COMPARE_AGENT_TYPE, async (chatId, userId) => {
    return await deleteImgRecognitionById(chatId, userId)
  }],
  [DEFAULT_AGENT_TYPE, async (chatId, userId) => {
    return await removeAgentChatById(chatId, userId)
  }],
  [TABLE_AGENT_TYPE, async (chatId, userId) => {
    return await removeExcelChatById(chatId, userId)
  }],
  [RESUME_AGENT_TYPE, async (chatId) => {
    return await deleteResumeTaskById(chatId)
  }]
]);

const handleConfirmDeleteIntel = async(agentChatId) => {
  // 设置当前要删除的智能体信息
  currentDeleteAgentItem.value = {
    id: agentChatId
  }
  // 显示删除确认弹窗
  deleteAgentConfirmVisible.value = true
}

// 确认删除智能体
const handleConfirmDeleteAgent = async () => {
  if (!currentDeleteAgentItem.value) return

  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo'))
    const operationFn = removeAgentChat.get(currentAgentType.value)
    if (!operationFn) {
      return
    }
    let deleteAgentChatResult = await operationFn(currentDeleteAgentItem.value.id, userInfo.id)
    // 删除以后 被选中的清空
    activeIndexIntel.value = ''
    if (deleteAgentChatResult.status) {
      // 删除后根据搜索关键字刷新
      eventBus.emit('getHistoryData', searchTextIntel.value || '')
      createNewConversation()
      ElMessage.success('删除成功')
    } else {
      ElMessage.error(deleteAgentChatResult.message)
    }
    // 清空当前删除项目信息
    currentDeleteAgentItem.value = null
  } catch (error) {
    console.error('删除智能体出错:', error)
    ElMessage.error('删除失败，请稍后重试')
  }
}

// 处理批量删除按钮点击
const handleBulkDelete = () => {
  if (selectedMenuIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的对话记录')
    return
  }
  bulkDeleteConfirmVisible.value = true
}

// 处理智能体批量删除按钮点击
const handleAgentBulkDelete = () => {
  if (selectedAgentMenuIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的智能体对话')
    return
  }
  agentBulkDeleteConfirmVisible.value = true
}

// 确认批量删除
const handleConfirmBulkDelete = async () => {
  if (selectedMenuIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的对话记录')
    return
  }

  try {
    let deleteResult = await batchRemoveChatList(selectedMenuIds.value)
    if (deleteResult.status) {
      ElMessage.success("删除成功")
    } else {
      ElMessage.error(deleteResult.message)
    }

    // 清空选中状态
    selectedMenuIds.value = []
    activeIndex.value = ''

    // 关闭批量删除模式
    showBulkDeleteMode.value = false

    // 刷新左侧列表（参考智能体处理方式）
    if (searchText.value && searchText.value.trim() !== '') {
      eventBus.emit('fetchChatList', searchText.value)
    } else {
      emit('change-history')
    }

    // 重置右侧界面状态，显示欢迎页面（参考单个删除逻辑）
    chatQuery.messages = []
    chatQuery.isLoading = false
    transQuest.value = ''
    transData.value = ''
    // 批量删除后保持当前专题状态，不强制切换到通用模式
    currentQuestion.value = false
    currentId.value = ''
    useKnowledge.value = false
    transFile.value = ''
    finalFile.value = ''

  } catch (error) {
    console.error('批量删除出错:', error)
    ElMessage.error('批量删除失败，请稍后重试')
  }
}

// 确认智能体批量删除
const handleConfirmAgentBulkDelete = async () => {
  if (selectedAgentMenuIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的智能体对话')
    return
  }

  try {
    let deleteResult = await batchRemoveAgentChatList(selectedAgentMenuIds.value)
    if (deleteResult.status) {
      ElMessage.success("删除成功")
      // 删除完成后触发新建对话
      createNewConversation()
    } else {
      ElMessage.error(deleteResult.message)
    }

    // 清空选中状态
    selectedAgentMenuIds.value = []
    activeIndexIntel.value = ''

    // 关闭智能体批量删除模式
    showAgentBulkDeleteMode.value = false

    // 刷新智能体对话列表（参考智能体处理方式）
    eventBus.emit('getHistoryData', searchTextIntel.value || '')

  } catch (error) {
    console.error('智能体批量删除出错:', error)
    ElMessage.error('批量删除失败，请稍后重试')
  }
}
// 点击确定删除历史记录
const handleConfirmDelete = (chatId) => {
  if (isSampleLoad.value || finalIng.value) {
    ElMessage.warning('有问题正在回答中，请稍后再删除')
    return
  }
  // 设置当前要删除的对话信息
  currentDeleteChatItem.value = {
    id: chatId
  }
  // 显示删除确认弹窗
  deleteChatConfirmVisible.value = true
}

// 确认删除对话
const handleConfirmDeleteChat = async () => {
  if (!currentDeleteChatItem.value) return
  
  try {
    await deleteData(currentDeleteChatItem.value.id)
    // 清空当前删除项目信息
    currentDeleteChatItem.value = null
  } catch (error) {
    console.error('删除对话出错:', error)
    ElMessage.error('删除失败，请稍后重试')
  }
}

// 删除数据
const deleteData = async (id, isRefresh) => {
  return new Promise((resolve, reject) => {
    // GET 请求
    request
      .post('/Message/deleteMessageById?id=' + id, {})
      .then(res => {
        if (res.status) {
          if (!isRefresh) {
            ElMessage.success('删除成功！')
            chatQuery.messages = []
            chatQuery.isLoading = false
            transQuest.value = ''
            transData.value = ''
            // 删除后保持当前专题状态，不强制切换到通用模式
            activeIndex.value = ''
            currentQuestion.value = false
            // 若搜索框有内容，按搜索关键字刷新；否则刷新全部
            if (searchText.value && searchText.value.trim() !== '') {
              eventBus.emit('fetchChatList', searchText.value)
            } else {
              emit('change-history')
            }
          }
          resolve(res)
        } else {
          reject(new Error(res.message || '删除失败'))
        }
      })
      .catch(err => {
        console.error(err)
        reject(err)
      })
  })
}

const isLoading = computed(() => {
  return isSampleLoad.value || docIng.value || tranIng.value || finalIng.value || limitLoading.value || limitTranLoading.value || limitQueryLoading.value
})

const querySelect = async (chatId, index) => {
  activeIndex.value = index
  // 清除初始化的思考过程，优化体验
  if (!isLoading) {
    tipQuery.value  = ''
    currentObj.value.thinking = ''
    currentObj.value.messages = []
  }
  let chatDetail = await getChatDetailByChatId(chatId)
  if (!chatDetail.status) {
    ElMessage.error(chatDetail.message)
    return
  }
  getChatDetail(chatDetail.data, index)
  // 移动端点击会话详情隐藏侧边栏
  if (isMobile.value) {
    toggleCollapse()
  }
}

// 自动选择第一项的方法
const autoSelectFirstItem = () => {
  if (chatList.value && chatList.value.length > 0) {
    const firstChat = chatList.value[0]
    querySelect(firstChat.id, 0)
  }
}
const querySelectIntel = (agentChatId, index) => {
  userInputContent.value = ''
  activeIndexIntel.value = index
  queryAnIntel(agentChatId)
}
// 搜索方法
const searchData = () => {
  eventBus.emit('getHistoryData', searchTextIntel.value)
  // 调用后端接口或其他搜索逻辑
}
const clearData = () => {
  eventBus.emit('getHistoryData', '')
  // 调用后端接口或其他搜索逻辑
}

// 对话模块搜索方法
const searchChatData = () => {
  eventBus.emit('fetchChatList', searchText.value)
  // 调用后端接口或其他搜索逻辑
}
// 对话模块清空搜索框
const clearChatData = () => {
  eventBus.emit('fetchChatList', '')
  // 调用后端接口或其他搜索逻辑
}

const queryAnIntel = (agentChatId) => {
  intelQuestion.value = ''
  isIntelStop.value = false
  limitIntelLoading.value = false

  fileInputAry.value = []
  eventBus.emit('getChatByAgentChatId', agentChatId)
}


const clearMessages = () => {
  if (pageType.value === 'sample') {
    currentObj.value.messages = {}
    currentObj.value.list = {}
    transData.value = ''
    transQuest.value = ''
    selectedLan.value = ''
    finalData.value.data = []
    finalData.value.title = ''
    finalQuest.value = ''
  } else {
    // 保留通用模式的流式缓冲，避免切换会话时丢失用户提问
    chatQuery.messages = []
    if (!isSampleLoad.value) {
      chatCurrent.messages = []
    }
  }
}

const setSampleValues = (detailMsg) => {
  pageType.value = 'sample'
  selectedMode.value = '通用模式'
  chatQuery.messages = detailMsg.data
  // 新加字段兼容历史数据
  chatQuery.messages.forEach((msg) => {
    if (msg.role === 'user') {
      msg.personalKnowledge = msg.personalKnowledge || false
    } else if (msg.role === 'assistant') {
      msg.sources = msg.sources || []
      msg.thinking = msg.thinking || ''
    }
  })
  chatQuery.isLoading = false
  currentId.value = detailMsg.id
  deepType.value = detailMsg.isThink

  // 当前对话id与流式对话id相等，显示正在加载的对话
  if (detailMsg.id === limitId.value) {
    limitLoading.value = true
  }
  nextTick(() => {
    // 滚动到底部
    if (messageContainer.value) {
      const messages = messageContainer.value.children
      if (messages.length > 0) {
        const lastMessage = messages[messages.length - 2]
        // 滚动到最后一个消息的开头部分
        lastMessage.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
    }
  })
}

const setQueryValues = (detailMsg) => {
  pageType.value = 'query'
  selectedMode.value = '人资行政专题'
  tipQuery.value = detailMsg.data.question || streamingQuestion.value
  currentObj.value.messages = detailMsg.data.answer
  // 兜底：当后端仅返回 { type: 'final_answer' } 时，补齐 content 字段
  if (!currentObj.value.messages || typeof currentObj.value.messages !== 'object') {
    currentObj.value.messages = { content: '' }
  } else if (currentObj.value.messages.content == null) {
    currentObj.value.messages.content = ''
  }
  currentObj.value.thinking = detailMsg.data?.thinking || currentObj.value.thinking
  deepType.value = detailMsg.isThink
  currentId.value = detailMsg.id
  if (detailMsg.id === limitId.value) {
    limitQueryLoading.value = true
  }
}
const setItValues = (detailMsg) => {
  pageType.value = 'it'
  selectedMode.value = 'IT专题'
  tipQuery.value = detailMsg.data.question || streamingQuestion.value
  currentObj.value.messages = detailMsg.data.answer
  if (!currentObj.value.messages || typeof currentObj.value.messages !== 'object') {
    currentObj.value.messages = { content: '' }
  } else if (currentObj.value.messages.content == null) {
    currentObj.value.messages.content = ''
  }
  currentObj.value.thinking = detailMsg.data?.thinking || currentObj.value.thinking
  deepType.value = detailMsg.isThink
  currentId.value = detailMsg.id
  if (detailMsg.id === limitId.value) {
    limitQueryLoading.value = true
  }
}
const setLawValues = (detailMsg) => {
  pageType.value = 'law'
  selectedMode.value = '法务专题'
  tipQuery.value = detailMsg.data.question || streamingQuestion.value
  currentObj.value.messages = detailMsg.data.answer
  if (!currentObj.value.messages || typeof currentObj.value.messages !== 'object') {
    currentObj.value.messages = { content: '' }
  } else if (currentObj.value.messages.content == null) {
    currentObj.value.messages.content = ''
  }
  currentObj.value.thinking = detailMsg.data?.thinking || currentObj.value.thinking
  deepType.value = detailMsg.isThink
  currentId.value = detailMsg.id
  if (detailMsg.id === limitId.value) {
    limitQueryLoading.value = true
  }
}
const setBoardValues = (detailMsg) => {
  pageType.value = 'board'
  selectedMode.value = '董办专题'
  tipQuery.value = detailMsg.data.question || streamingQuestion.value
  currentObj.value.messages = detailMsg.data.answer
  if (!currentObj.value.messages || typeof currentObj.value.messages !== 'object') {
    currentObj.value.messages = { content: '' }
  } else if (currentObj.value.messages.content == null) {
    currentObj.value.messages.content = ''
  }
  currentObj.value.thinking = detailMsg.data?.thinking || currentObj.value.thinking
  deepType.value = detailMsg.isThink
  currentId.value = detailMsg.id
  if (detailMsg.id === limitId.value) {
    limitQueryLoading.value = true
  }
}
const setTranValues = (detailMsg) => {
  currentQuestion.value = false
  pageType.value = 'tran'
  selectedMode.value = '翻译'
  transData.value = detailMsg.data.answer
  transQuest.value = detailMsg.data.files ? detailMsg.data.files.originalFileName : detailMsg.data.question
  fileObj.value = detailMsg.data.files
  selectedLan.value = detailMsg.data.target
  currentId.value = detailMsg.id
  transFile.value = detailMsg.data.files || transFile.value
  useTranslationDocument.value = detailMsg.data.useTranslationDocument || false
  translationDocumentProcess.value = detailMsg.data.translationDocumentProcess || ''
  translationDocumentFinal.value = detailMsg.data.translationDocumentFinal || ''
  if (detailMsg.id === limitId.value) {
    limitQueryLoading.value = true
  }
}
const setFinalValues = (detailMsg) => {
  currentQuestion.value = false
  pageType.value = 'final'
  selectedMode.value = '总结'
  finalData.value.data = detailMsg.data.answer.key_points
  finalData.value.title = detailMsg.data.answer.summary
  // 设置预览文件
  fileObj.value = detailMsg.data.files
  finalQuest.value = detailMsg.data.files ? detailMsg.data.files.originalFileName : detailMsg.data.question
  currentId.value = detailMsg.id
  finalFile.value = detailMsg.data.files
  if (detailMsg.id === limitId.value) {
    limitQueryLoading.value = true
  }
}

const operations = new Map([
  ['sample', setSampleValues],
  ['query', setQueryValues],
  ['it', setItValues],
  ['law', setLawValues],
  ['board', setBoardValues],
  ['tran', setTranValues],
  ['final', setFinalValues],
]);

const getChatDetail = (detailMsg) => {
  try {
    currentQuestion.value = detailMsg.id
    isSampleStop.value = false
    isQueryStop.value = false
    limitLoading.value = false
    limitTranLoading.value = detailMsg.id === limitTranId.value;
    limitQueryLoading.value = false
    if (!currentIndex.value && currentIndex.value !== 0) {
      currentIndex.value = activeIndex.value
    }
    drayAry.value = []
    fileInputAry.value = []

    pageType.value = MODE_MAPPING.get(detailMsg.type)
    selectedMode.value = detailMsg.type
    const operationFn = operations.get(pageType.value)
    if (operationFn) {
      operationFn(detailMsg)
    } else {
      throw new Error('Unknown operation')
    }
    clearMessages()
  } catch (e) {
    console.error(e)
  }
}

const powerList = ref([
  'T10802004',
  '31005892',
  '10353965',
  '10353964',
  'T10802005',
  '31001225',
  '10801390',
  'T17990001',
  'T93000161',
  '10500985',
  '10800001',
  '10335333',
  '10801127',
  'ZL044364',
  '13829448',
  '39000357',
  '39000318'
])
const powerArr = ref([])
const publicActiveMenuId = computed(() => {
  const currentItem = powerArr.value?.[ItemSelect.value]
  if (!currentItem) {
    return ''
  }
  return currentItem.target ?? String(ItemSelect.value)
})

const getUserPower = () => {
  request
    .get('/UserInfo/getUserIP')
    .then(res => {
      if (res.status) {
        localStorage.setItem('isNet', res.data)
        isNet.value = res.data
        emit('setNet')
      }
    })
    .catch(err => {
      console.error(err)
    })
}
const getPower = () => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo'))
  request
    .post('/Files/permissionCheck?userId=' + userInfo.id)
    .then(res => {
      if (res.status) {
        localStorage.setItem('powerList', JSON.stringify(res.data))
        powerArr.value = res.data

        if (powerArr.value.length > 0) {
          for (var i = 0; i < powerArr.value.length; i++) {
            powerArr.value[i].name =
              powerArr.value[i].target === 'IT'
                ? 'IT知识库'
                : powerArr.value[i].target === 'HR'
                  ? '人资行政知识库'
                  : '法务知识库'
          }
        }
        setPower(JSON.stringify(res.data))
        eventBus.emit('changeKnow', powerArr.value[0])
      }
    })
    .catch(err => {
      console.error(err)
    })
}
onMounted(async () => {
  // 预加载切换用到的图标，避免首次切换延迟
  ;[personWhite, personBlack, commonWhite, commonBlack, addDirWhite, addFileDirectory].forEach(src => {
    const img = new Image()
    img.src = src
  })
  eventBus.on('showAgentChatList', showAgentChatList)
  eventBus.on('toggleCollapse', toggleCollapse)
  if (localStorage.getItem('userInfo') && JSON.parse(localStorage.getItem('userInfo')).id) {
    isLogin.value = true
    const loginData = JSON.parse(localStorage.getItem('userInfo'))
    userInfo.value.id = loginData.id
    userInfo.value.department = loginData.department || ''
    userInfo.value.departmentId = loginData.departmentId || ''
    userInfo.value.personLevel = loginData.personLevel
    if (powerList.value.includes(loginData.id)) {
      localStorage.setItem('enableLaw', true)
      localStorage.setItem('enableBoardOffice', true)
    } else {
      localStorage.setItem('enableLaw', false)
      localStorage.setItem('enableBoardOffice', false)
    }
    getPower()
    getUserPower()
    userInfo.value.name = loginData.name
    userInfo.value.url = 'https://dcs.luxshare-ict.com/Upload/emp_photo/' + userInfo.value.id + '.jpg?cp=zhaopian'
    await refreshDepartmentList()
    emit('change-history')
    // 如果当前在知识库页面，获取文件夹列表
    if (selectType.value === ContentType.KNOWLEDGE) {
      await fetchFolderList(knowSelect.value, { departmentId: selectedDepartmentId.value })
    }
  } else {
    if (queryParams && queryParams.tokenId) {
      checkToken(queryParams.tokenId)
    } else {
      dialogVisible.value = true
    }
  }
})
const commonLedge = ref(null)

const setPower = data => {
  const isPower = JSON.parse(data)
}

const COLLAPSED_WIDTH = '80px' // 声明为常量的折叠宽度
const EXPANDED_WIDTH = '316px' // 声明为常量的展开宽度
const MOBILE_WIDTH = '236px'
const sidebarWidth = computed(() => {
  if (isMobile.value) {
    return isCollapsed.value ? '0px' : MOBILE_WIDTH
  }
  // 不是智能体对话折叠
  return (selectType.value === ContentType.AGENT && !isAgentDetail.value) || isCollapsed.value
    ? COLLAPSED_WIDTH
    : EXPANDED_WIDTH
})

const mobileBottomNavStyle = computed(() => {
  if (!isMobile.value) {
    return {}
  }
  const offset = isCollapsed.value ? '0px' : MOBILE_WIDTH
  return {
    transform: `translateX(${offset})`
  }
})

const showAgentChatList = () => {
  isAgentDetail.value = true
  // 进入智能体详情时重置通用对话的批量删除状态
  showBulkDeleteMode.value = false
  selectedMenuIds.value = []
  showContextBtn.value = false
}
const backToAgentList = () => {
  isAgentDetail.value = false
  // 只有当前没有正在进行的对话时才清空conversationId
  if (!loadingIntelId.value || !isIntelLoad.value) {
    conversationId.value = ''
  }
  // 退出智能体详情时重置智能体批量删除状态
  showAgentBulkDeleteMode.value = false
  selectedAgentMenuIds.value = []
  showContextBtn.value = false
  eventBus.emit('backToAgentList')
  if (isMobile.value) {
    toggleCollapse()
  }
}

// 监听网络类型变化
watch(
  () => networkState.networkType,
  (newVal, oldVal) => {
    if (newVal !== oldVal) {
      getUserPower()
      // 在这里触发你的业务逻辑
    }
  }
)
defineExpose({ deleteData, setPower, handleCreateFolder, autoSelectFirstItem, searchText })
</script>
<style lang="less" scoped>
.foldable {
  position: absolute;
  top: calc(50% - 15px);
  left: 290px;
  cursor: pointer;
  z-index: 1001;
  img {
    width: 10px;
    height: 31px;
    z-index: 10000;
  }
}
/* 右键悬浮按钮样式 */
.context-action-btn {
  position: absolute;
  width: 110px;
  height: 54px;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 0 4px #00000024;
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
  cursor: pointer;
  overflow: visible;
}
.context-action-btn > * {
  position: relative;
  z-index: 1;
}
.context-action-btn::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: calc(100% - 10px);
  height: calc(100% - 10px);
  background-color: transparent;
  border-radius: 8px;
  pointer-events: none;
  transition: background-color 0.15s ease;
  z-index: 0;
}
.context-action-btn:hover::after {
  background-color: #fff2f0;
}
.context-action-btn.is-active:hover::after {
  background-color: #f2f3f5 !important;
}
.popover-content {
  display: flex;
  flex-direction: column;
  .edit_img {
    display: flex;
    flex-direction: row;
    font-size: 14px;
    display: flex;
    align-items: center;
    cursor: pointer;
    padding: 5px;
    img {
      width: 16px;
      height: 16px;
    }
  }
  .delete_img:hover {
    background: #fff2f0;
  }
  .rename_img:hover {
    background: #ededed;
  }
  :deep(.right-aligned-popover .el-popper) {
    padding-top: 10px !important;
    padding-bottom: 10px !important;
  }
}
.active-span {
  color: #1b6cff !important;
}
.aside {
  background: #f7f7f7;
  transition: width 0.3s ease-in-out; /* 添加缓动函数 */
  display: flex;
  position: relative; /* 为子元素绝对定位提供参考 */
  overflow: visible; /* 允许内容溢出以显示提示框 */
  height: 100vh;

  .aside_left {
    position: relative; /* 让绝对定位的 logo 以左侧栏为参照，不随右侧变化 */
    width: 80px;
    height: 44px;
    margin-top: 18px;
    display: flex;
    justify-content: center;
    align-items: center;
    flex-shrink: 0; /* 防止折叠时被压缩 */
    .aside_left_file {
      position: absolute;
      width: 100%;
      height: 66px;

      flex-direction: column;
      top: 150px;
      cursor: pointer;
      display: flex;
      justify-content: center;
      align-items: center;

      .aside_img {
        width: 36px;
        height: 36px;
        display: flex;
        justify-content: center;
        align-items: center;
        background-color: #f7f7f7;
        border-radius: 12px;
        img {
          width: 28px;
          height: 28px;
        }
      }
    }
    .aside_left_message {
      position: absolute;
      width: 100%;
      height: 70px;
      top: 75px;
      cursor: pointer;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      .aside_img {
        width: 36px;
        height: 36px;
        display: flex;
        justify-content: center;
        align-items: center;
        border-radius: 12px;
        background-color: #f7f7f7;
        img {
          width: 28px;
          height: 28px;
        }
      }
    }
    .aside_message_text {
      width: 100%;
      height: 24px;
      line-height: 24px;
      font-size: 12px;

      text-align: center;
    }
    .aside_left_img {
      width: 36px;
      height: 36px;
      position: absolute;
      left: 50%;
      transform: translateX(-50%);
      top: 22px; /* 保持原有的垂直位置效果 */
      z-index: 10000; /* 确保在各导航状态下不被覆盖 */
      pointer-events: none; /* 避免遮挡交互 */
    }

    .noLogin {
      font-size: 14px;
      cursor: pointer;
      position: fixed;
      color: #1b6cff;
      bottom: 30px;
      left: 0;
      width: 80px;
      text-align: center;
    }
    .user-avatar-container {
      position: fixed;
      bottom: 20px;
      left: 16px;
      .popover-reference {
        position: absolute;
        top: 0;
        right: 0;
        width: 36px;
        height: 36px;
      }
      .user-avatar {
        cursor: pointer;
        --el-avatar-size: 50px !important;
      }
    }
  }

  .aside_right {
    width: 236px;
    background: #f9fbff;
    border-right: 2px solid #eaeaea;
    flex-shrink: 0;
    overflow: visible;
    white-space: nowrap; /* 防止文字换行 */
    transform: translateX(0);
    transition: transform 0.3s ease-in-out, opacity 0.2s ease-in-out;
    opacity: 1;
    pointer-events: auto;
    .asize_file {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      overflow: visible;
      :deep(.asize_know) {
        width: 209px;
        height: 38px;
        margin-left: 16px;
        margin-top: 41px;
        border-radius: 8px;
        box-sizing: border-box;
        padding: 11px;
        display: flex;
        align-items: center;
        cursor: pointer;
        color: #6A6A6A;
        position: relative;
      }

      :deep(.asize_know .leftImg) {
        width: 24px;
        height: 24px;
        margin-right: 2px;
        flex-shrink: 0;
        transform: translateY(0.4px);
      }

      :deep(.asize_know .leftImg img) {
        width: 100%;
        height: 100%;
        object-fit: contain;
      }

      :deep(.asize_know .add_file_directory) {
        position: absolute;
        right: 16px;
        top: 50%;
        transform: translateY(-50%) translateY(0px);
        width: 30.8px;
        height: 30.8px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 6px;
        cursor: pointer;
        transition: all 0.2s ease;
        z-index: 1000;
      }

      :deep(.asize_know .add_file_directory img) {
        opacity: 0.6;
        transition: opacity 0.2s ease;
      }

      :deep(.asize_know .add_file_directory:hover) {
        background-color: transparent;
      }

      :deep(.asize_know .add_file_directory:hover img) {
        opacity: 1;
      }

      :deep(.asize_know .knowledge-text) {
        transform: translateY(-0.7px);
      }

      :deep(.asize_know:hover) {
        background-color: #DCE6FA !important;
        color: #333333 !important;
      }
      .know_list {
        margin-top: 20px;

        .know_item {
          width: 192px;
          height: 38px;
          padding-left: 12px;
          line-height: 38px;
          margin-left: 16px;
          border-radius: 6px;
          font-size: 14px;
          cursor: pointer;
          background-repeat: no-repeat;
          background-size: 24px 24px;
          background-position: 4px 7px;
        }
        .know_item:hover {
          background-color: #dce6fa;
        }
      }
      
      :deep(.folder_list) {
        margin-top: 12px;
        margin-right: 16px;
      }
      
      
    }
    .aside_right_content {
      width: 200px;
      height: 62px;
      margin-top: 20px;
      margin-left: 10px;
      img {
        width: 100%;
        height: 100%;
      }
    }
    .create_conversation {
      margin-left: 10px;
      width: 215px;
      height: 96px;
      position: absolute;
      border-top: 1px solid #E1EEFF;
      box-sizing: border-box;
      bottom: 23px;
      .create_conversation_btn {
        cursor: pointer;
        position: absolute;
        bottom: 19px;
        width: 180px;
        height: 48px;
        text-align: center;
        line-height: 48px;
        img {
          vertical-align: middle;
          width: 24px;
          height: 24px;
          margin-right: 8px;
        }
        span {
          vertical-align: middle;
          color: #1B6CFF;
          display: inline-block;
          transform: translateY(-0.8px);
        }
      }
    }
    /* 会话列表底部批量操作（固定在右侧容器底部，不随列表滚动） */
    .batch_remove {
      margin-left: 10px;
      width: 215px;
      height: 65px;
      position: absolute;
      left: 0;
      right: 0;
      bottom: 23px;
      border-top: 1px solid #E1EEFF;
      box-sizing: border-box;
      background: #f9fbff;
      z-index: 1;
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
    }
    .batch_remove .select_all_to_remove {
      display: flex;
      align-items: center;
      line-height: 34px;
      color: #6A6A6A;
      font-size: 14px;
      padding-left: 21px;
      transform: translateX(-5px);
    }
    
    .batch_remove .handle_remove {
      width: 72px;
      height: 34px;
      text-align: center;
      line-height: 34px;
      background-color: #FF4D4F;
      border-radius: 6px;
      color: white;
      cursor: pointer;
      transition: background-color 0.2s ease;
    }
    
    .batch_remove .handle_remove:hover {
      background-color: #FF7875;
    }
    .create_intel {
      width: 140px;
      display: flex;
      justify-content: center;
      align-items: center;
      color: #fff;
      background-color: #1b6cff;
      border-radius: 10px;
      font-size: 14px;
      position: absolute;
      bottom: 23px;
      left: 10px;
      height: 36px;
      text-indent: 16px;
      cursor: pointer;
      background-image: url('@/assets/create.png');
      background-repeat: no-repeat;
      background-size: 20px 20px;
      background-position: 16px 8px;
    }
    .aside_right_btn {
      height: 100px;
      box-sizing: border-box;
      border-bottom: 1px solid #E1EEFF;
      margin-left: 17px;
      margin-right: 12px;
      margin-bottom: 14px;
      padding-top: 41px;
      .intel_img {
        cursor: pointer;
        width: 40px !important;
        height: 40px !important;
        border-radius: 20px;
        border: 1px solid #D0E4FF;
        box-sizing: border-box;
        display: flex;
        justify-content: center;
        align-items: center;
        img {
          width: 19.86px;
          height: 15.88px;
        }
      }
      .intel_title {
        color: #333333;
        font-size: 18px;
        font-weight: 400;
        padding-left: 12px;
        line-height: 27px;
        transform: translateY(5.6px);
      }
      .back_set {
        background-image: url('@/assets/start.png');
        background-repeat: no-repeat;
        box-sizing: border-box;
        text-align: center;
        padding-left: 16px;
        width: 208px;
        height: 48px;
        line-height: 48px;
        background-size: 16px 16px;
        background-position: 47px 16px;
        letter-spacing: 1px;
        background-color: #1b6cff;
        color: #fff;
        font-size: 16px;
        cursor: pointer;
        border-radius: 10px;
      }
    }
    :deep(.el_menu) {
      margin-top: 10px;
      border-right: none;
      width: 236px; /* 保持固定宽度 */
      background: #f9fbff;
      /* 作为可伸缩区域填满剩余空间并在溢出时滚动 */
      height: auto;
      flex: 1 1 auto;
      min-height: 0;
      overflow-y: auto;
      overflow-x: hidden;
      .more {
        width: 55px;
        height: 100%;
        display: flex;
        justify-content: center;
        align-items: center;
        .aside_right_img {
          width: 4px;
          height: 13px;
          position: absolute;
          top: 13px;
        }
      }
    }
  }
  .aside_right.collapsed {
    transform: translateX(-100%);
    opacity: 0;
    pointer-events: none;
  }
}
.login_title {
  width: 100%;
  display: flex;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
  color: #333;
  line-height: 40px;
  span {
    height: 40px;
    img {
      width: 40px;
      height: 40px;
      margin-right: 10px;
    }
  }
}
.login-form {
  padding: 20px 20px 0 20px;
  :deep(.el-form-item__label) {
    padding-right: 8px; /* 调整 label 和输入框的间距 */
  }

  :deep(.el-form-item__content) {
    justify-content: flex-start;
  }
  :deep(.el-form-item__content) {
    justify-content: center;
  }
  /* 登录按钮水平居中对齐 */
  .button-item {
    display: flex;
    justify-content: center;
    display: flex;
    justify-content: center;
    width: 100%;
    margin-top: 30px;
  }
}
.user-info-popup {
  text-align: center;
  .user-info {
    display: flex;
    align-items: center;
    justify-content: center;
    .el_avatar {
      img {
        width: 100%;
        height: 100%;
      }
    }
    .user-details {
      margin-left: 10px;
      text-align: center;

      .username {
        font-weight: bold;
        text-align: left;
      }
    }
  }
}

.asize_message {
  :deep(.el-input) {
    --el-input-height: 38px;
    --el-input-border-radius: 8px;
    --el-input-border-color: #DCE6FA;
    --el-input-hover-border-color: #DCE6FA;
    --el-input-focus-border-color: #409EFF;
  }
  :deep(input::placeholder) {
    font-size: 14px;
  }
}

:deep(.tooltip-bottom) {
  position: absolute;
  top: calc(100% + 5px);
  left: 50%;
  transform: translateX(-50%);
  background: #000;
  color: white;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 14px;
  white-space: nowrap;
  z-index: 9999;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);

  &::after {
    content: '';
    position: absolute;
    bottom: 100%;
    left: 50%;
    transform: translateX(-50%);
    border: 5px solid transparent;
    border-bottom-color: #000;
  }
}

:deep(.tooltip-top) {
  position: absolute;
  bottom: calc(100% + 5px);
  left: 50%;
  transform: translateX(-50%);
  background: #000;
  color: white;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 14px;
  white-space: nowrap;
  z-index: 9999;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);

  &::after {
    content: '';
    position: absolute;
    top: 100%;
    left: 50%;
    transform: translateX(-50%);
    border: 5px solid transparent;
    border-top-color: #000;
  }
}

.mobile-bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 60px;
  background: #fff;
  border-top: 1px solid #eaeaea;
  display: flex;
  justify-content: space-around;
  align-items: center;
  z-index: 1000;
  transition: transform 0.3s ease;
}
.mobile-bottom-nav .nav-item {
  flex: 1;
  display: flex;
  justify-content: center;
}
.mobile-bottom-nav .nav-ref {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 12px;
  color: #9d9d9d;
}
.mobile-bottom-nav .nav-ref.active {
  color: #1b6cff;
}
.mobile-bottom-nav .nav-ref img,
.mobile-bottom-nav .nav-ref .el-icon,
.mobile-bottom-nav .nav-ref .el-avatar {
  width: 24px;
  height: 24px;
  margin-bottom: 4px;
}
@media (max-width: 768px) {
  .aside_left {
    display: none;
  }
}

/* 添加淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

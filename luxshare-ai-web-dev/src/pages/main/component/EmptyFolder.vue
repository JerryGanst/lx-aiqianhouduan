<template>
  <div class="empty-folder">
    <div class="content">
      <img :src="folderImg" class="folder-icon" @click.stop="handleCreate" />
      <div class="create-text" @click.stop="handleCreate">新建文件夹</div>
    </div>
  </div>
</template>

<script setup>
import { defineEmits } from 'vue'
import folderImg from '@/assets/knowledgeBase/directory.png'

const emit = defineEmits(['create-folder'])

const handleCreate = () => {
  emit('create-folder')
}
</script>

<style scoped lang="less">
.empty-folder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.folder-icon {
  width: 72px;
  height: 72px;
  cursor: pointer;
  transition: transform 0.15s ease;
}
.create-text {
  margin-top: 10px;
  font-size: 14px;
  color: #4E5969;
  text-align: center;
  cursor: pointer;
  text-decoration: none;
  text-underline-offset: 2px;
  transition: color 0.18s ease, text-decoration-color 0.18s ease;
}
.create-text:hover,
.create-text:focus-visible {
  color: #1B6CFF;
}
/* 图标悬浮时，文本变蓝 */
.folder-icon:hover + .create-text {
  color: #1B6CFF;
}
/* 悬浮时仅图标微放大 */
.content:hover .folder-icon {
  transform: scale(1.06);
}
</style>



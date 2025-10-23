<script setup lang="ts">

defineProps<{
  menuId: string | number;
  show: boolean;
  positionX: number;
  positionY: number;
}>()

const emit = defineEmits(['hideDialog', 'activeMenu', 'deActiveMenu', 'handleEdit', 'handleDelete'])
const hideDialog = () => {
  emit('hideDialog')
}
const activeMenu = () => {
  emit('activeMenu')
}
const deActiveMenu = () => {
  emit('deActiveMenu')
}
const handleEdit = () => {
  emit('handleEdit')
}
const handleDelete = () => {
  emit('handleDelete')
}
</script>

<template>
  <teleport to="body">
    <div class="moreTips"
      @mouseenter="activeMenu"
      @mouseleave="deActiveMenu"
      v-show="show"
      :style="{
      position: 'fixed',
      top: `${positionY}px`,
      left: `${positionX}px`}"
    >
      <div class="editAgent" @click="handleEdit">
        <img src="@/assets/edit.png" />
        <span>重命名</span>
      </div>
      <div class="deleteAgent" @click="handleDelete">
        <img src="@/assets/delete.png" />
        <span>删除</span>
      </div>
          </div>
    </teleport>
  </template>

<style scoped lang="less">
.moreTips {
  width: 86px;
  height: 75px;
  border-radius: 10px;
  background-color: white;
  box-sizing: border-box;
  padding: 6px;
  box-shadow: 0 8px 16px 0 rgba(175, 166, 166, 0.2),
  0 6px 20px 0 rgba(197, 183, 183, 0.19);
  z-index: 3000; /* 确保浮于主内容区之上，阴影不被覆盖 */

  .editAgent, .deleteAgent {
    box-sizing: border-box;
    width: 74px;
    height: 30px;
    border-radius: 4px;
    text-align: left;
    display: flex;
    align-items: center;
    padding-left: 6px;
    cursor: pointer;

    img {
      width: 13px;
      height: 13px;
      margin-right: 4px;
    }

    /* span 无额外样式，随父级弹性布局自然垂直居中 */
  }

  /* 仅上移“重命名”文字 2px，不影响图标位置 */
  .editAgent span {
    display: inline-block;
    transform: translateY(-2px);
  }

  .deleteAgent {
    color: red;
  }

  .editAgent:hover {
    background-color: #ededed;
  }

  .deleteAgent:hover {
    background-color: #fff2f0;
  }
}
</style>
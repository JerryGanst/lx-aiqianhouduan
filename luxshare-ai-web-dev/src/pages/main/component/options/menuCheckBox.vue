<template>
  <div class="check_box" v-show="show">
    <div
      class="not_checked_box"
      :class="{ checked: isChecked }"
      :style="{ backgroundColor: isChecked ? '#1B6CFF' : 'white' }"
      @click.stop="toggleChecked"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  show?: boolean
  menuId?: string | number
  checked?: boolean
}>()

const emit = defineEmits<{
  updateSelected: [menuId: string | number, isSelected: boolean]
}>()

const isChecked = ref(props.checked || false)

// 监听 checked 属性的变化，并同步内部状态
watch(() => props.checked, (newVal) => {
  isChecked.value = newVal || false
}, { immediate: true })

const toggleChecked = () => {
  isChecked.value = !isChecked.value
  // 向父组件发送状态变化事件
  if (props.menuId !== undefined) {
    emit('updateSelected', props.menuId, isChecked.value)
  }
}
</script>

<style scoped lang="less">
.check_box {
  margin-right: 5px;
  .not_checked_box {
    width: 16px;
    height: 16px;
    background-color: white;
    border: 1px solid #d0e4ff;
    border-radius: 4px;
    transition: border-color 0.15s ease;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    position: relative;
    overflow: hidden;
  }
  .not_checked_box:hover {
    border-color: #1B6CFF;
  }
  .not_checked_box.checked::after {
    content: '';
    position: absolute;
    width: 10px;
    height: 6px;
    border: 2px solid #ffffff;
    border-top: 0;
    border-left: 0;
    left: 50%;
    top: calc(50% - 1px);
    transform: translate(-50%, -58%) rotate(-35deg) scaleX(-1);
    box-sizing: border-box;
  }
}
</style>



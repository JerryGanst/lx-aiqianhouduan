<template>
  <div class="list_item">
    <div class="list_title">常见问题</div>
    <div class="list_tip">深度搜索您关心的问题</div>
    <div class="list_arry">
      <div v-for="(item, index) in hotList" :key="item?.index ?? index" class="arr_item">
        <span>{{ index + 1 }}.</span>
        <span class="item_hover" @click="handleClick(item)">{{ item?.name ?? item }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  list: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['item-click'])

const hotList = computed(() => props.list ?? [])

const handleClick = item => {
  const value = item && typeof item === 'object' && 'name' in item ? item.name : item
  if (value !== undefined && value !== null) {
    emit('item-click', value)
  }
}
</script>

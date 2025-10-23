<script setup lang="ts">
interface Props {
  imageUrl?: string
  fileName: string
  fileInfo: string
  fileId?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'preview', fileId?: string): void
}>()

const handleClick = () => {
  emit('preview', props.fileId)
}
</script>

<template>
  <div class="jd_file common_file" role="button" @click="handleClick">
    <div
      class="left_file_img"
      :style="props.imageUrl ? { backgroundImage: `url(${props.imageUrl})` } : {}"
    ></div>
    <div class="right_file_msg">
      <div class="file_name" :title="props.fileName">{{ props.fileName }}</div>
      <div class="file_info">{{ props.fileInfo }}</div>
    </div>
  </div>
</template>

<style scoped lang="less">
.common_file {
  width: 277px;
  height: 66px;
  border-radius: 10px;
  background: #eee;
  display: flex;
  align-items: center;
  cursor: pointer;

  .left_file_img {
    width: 36px;
    height: 45.02px;
    background: #4a8dff;
    margin-left: 21px;
    background-size: cover;
    background-position: center;
  }

  .right_file_msg {
    margin-left: 10px;
    flex: 1;
    overflow: hidden;

    .file_name {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .file_info {
      color: #aaaaaa;
    }
  }
}
</style>

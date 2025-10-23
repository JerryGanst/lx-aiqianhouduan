<template>
  <div class="list_item" style="margin-left: 20px">
    <div class="list_title">知识工坊</div>
    <div class="list_tip">办公学习必备</div>
    <div class="img_list">
      <div class="img_item" @click="emitChangeType('tran')">
        <div class="image"><img src="@/assets/1.png" /></div>
        <div class="img_text">
          <div class="text_title">翻译</div>
          <div class="text_content">准确翻译成多国语言</div>
        </div>
      </div>
      <div class="img_item" style="margin-top: 15px" @click="emitChangeType('final')">
        <div class="image"><img src="@/assets/2.png" /></div>
        <div class="img_text">
          <div class="text_title">总结</div>
          <div class="text_content">AI智能总结,让复杂信息一目了然</div>
        </div>
      </div>
      <div class="img_item" style="margin-top: 15px">
        <div class="image"><img src="@/assets/3.png" /></div>
        <div class="img_text">
          <div class="text_title">更多功能</div>
          <div class="text_content">敬请期待...</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
const emit = defineEmits(['change-type'])

const emitChangeType = type => {
  emit('change-type', type)
}
</script>

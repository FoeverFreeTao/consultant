<script setup>
defineProps({
  role: {
    type: String,
    default: 'assistant'
  },
  content: {
    type: String,
    default: ''
  }
})
</script>

<template>
  <view :class="['bubble', role === 'user' ? 'user' : 'assistant']">
    <text class="bubble-text">{{ content }}</text>
  </view>
</template>

<style scoped>
.bubble {
  max-width: 88%;
  padding: 22rpx 24rpx;
  border-radius: 28rpx;
}

.assistant {
  align-self: flex-start;
  background: linear-gradient(135deg, #14b8a6, #0f766e);
}

.assistant .bubble-text {
  color: #ffffff;
}

.user {
  align-self: flex-end;
  background: #edf6ff;
}

.user .bubble-text {
  color: #0f172a;
}

.bubble-text {
  line-height: 1.7;
  font-size: 28rpx;
  white-space: pre-wrap;
}
</style>

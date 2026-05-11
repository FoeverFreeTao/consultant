<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  subtitle: {
    type: String,
    default: ''
  }
})
</script>

<template>
  <view class="section-header">
    <text class="section-title">{{ title }}</text>
    <text v-if="subtitle" class="section-subtitle">{{ subtitle }}</text>
  </view>
</template>

<style scoped>
.section-header {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
</style>

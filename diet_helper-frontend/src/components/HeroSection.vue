<script setup>
import { computed } from 'vue'

const props = defineProps({
  bmi: { type: [String, Number], default: '--' },
  hydrationProgress: { type: [String, Number], default: 0 },
  sleepHour: { type: [String, Number], default: 0 },
  activityMinute: { type: [String, Number], default: 0 },
  isAuthenticated: { type: Boolean, default: false },
  nickname: { type: String, default: '' }
})

defineEmits(['open-profile', 'open-daily', 'logout'])

const metrics = computed(() => [
  { label: 'BMI', value: props.bmi },
  { label: '饮水进度', value: `${props.hydrationProgress}%` },
  { label: '睡眠', value: `${props.sleepHour}h` },
  { label: '活动', value: `${props.activityMinute}min` }
])
</script>

<template>
  <section class="hero">
    <div class="hero-copy">
      <h1>你的个人饮食与健康助手</h1>
      <p>结合你的目标与习惯，给出更可执行的营养建议。</p>
    </div>

    <div class="hero-metrics">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <span class="metric-label">{{ metric.label }}</span>
        <strong class="metric-value">{{ metric.value }}</strong>
      </article>
    </div>

    <div v-if="isAuthenticated" class="hero-actions">
      <span class="hero-user">已登录：{{ nickname }}</span>
      <div class="hero-buttons">
        <button type="button" class="ghost-btn" @click="$emit('open-profile')">修改个人信息</button>
        <button type="button" class="ghost-btn" @click="$emit('open-daily')">修改日常状态</button>
        <button type="button" class="ghost-btn" @click="$emit('logout')">退出登录</button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero {
  max-width: 1160px;
  margin: 0 auto 16px;
  padding: 20px 22px 18px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 18px 60px rgba(54, 94, 135, 0.08);
}

.hero-copy h1 {
  margin: 0;
  font-size: 28px;
  line-height: 1.1;
  color: #0f172a;
}

.hero-copy p {
  margin: 10px 0 0;
  color: #64748b;
  font-size: 16px;
}

.hero-metrics {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  padding: 18px 20px;
  border: 1px solid #dbe7fb;
  border-radius: 22px;
  background: linear-gradient(180deg, #f8fbff 0%, #fdfefe 100%);
}

.metric-label {
  display: block;
  color: #64748b;
  font-size: 14px;
}

.metric-value {
  display: block;
  margin-top: 12px;
  font-size: 24px;
  color: #0f172a;
}

.hero-actions {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.hero-user {
  color: #334155;
  font-weight: 600;
}

.hero-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.ghost-btn {
  border: 1px solid #cfe0ff;
  border-radius: 18px;
  padding: 12px 18px;
  background: #fff;
  color: #0f172a;
  cursor: pointer;
}

@media (max-width: 900px) {
  .hero-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .hero {
    padding: 18px 16px;
  }

  .hero-copy h1 {
    font-size: 24px;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }
}
</style>

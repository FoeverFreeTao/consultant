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
  { label: 'BMI', value: props.bmi, icon: '⚖️', accent: false },
  { label: '饮水进度', value: `${props.hydrationProgress}%`, icon: '💧', accent: false },
  { label: '睡眠', value: `${props.sleepHour}h`, icon: '🌙', accent: false },
  { label: '活动', value: `${props.activityMinute}min`, icon: '🏃', accent: false }
])
</script>

<template>
  <section class="hero">
    <div class="hero-top">
      <div class="hero-copy">
        <p class="hero-eyebrow">AI 饮食健康管理师</p>
        <h1>你的个人饮食与健康助手</h1>
        <p class="hero-sub">结合你的目标与习惯，给出更可执行的营养建议。</p>
      </div>

      <div v-if="isAuthenticated" class="hero-user-badge">
        <span class="user-avatar">{{ nickname?.charAt(0) || '?' }}</span>
        <span class="user-name">{{ nickname }}</span>
      </div>
    </div>

    <div class="hero-metrics">
      <article
        v-for="(metric, index) in metrics"
        :key="metric.label"
        :class="['metric-card', { featured: index === 0 }]"
      >
        <span class="metric-icon">{{ metric.icon }}</span>
        <div class="metric-body">
          <span class="metric-label">{{ metric.label }}</span>
          <strong class="metric-value">{{ metric.value }}</strong>
        </div>
      </article>
    </div>

    <div v-if="isAuthenticated" class="hero-actions">
      <div class="hero-buttons">
        <button type="button" class="action-btn" @click="$emit('open-profile')">
          <span class="action-icon">✏️</span>
          修改个人信息
        </button>
        <button type="button" class="action-btn" @click="$emit('open-daily')">
          <span class="action-icon">📊</span>
          修改日常状态
        </button>
        <button type="button" class="action-btn logout" @click="$emit('logout')">
          <span class="action-icon">👋</span>
          退出登录
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero {
  max-width: 1160px;
  margin: 0 auto var(--space-5);
  padding: var(--space-6) var(--space-6) var(--space-5);
  border-radius: var(--radius-xl);
  background: var(--surface-0);
  border: 1px solid var(--surface-border);
  box-shadow: var(--shadow-lg);
  position: relative;
  overflow: hidden;
}

/* Subtle decorative gradient strip at top */
.hero::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--brand-500), var(--brand-300), var(--accent-400));
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
}

.hero-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.hero-eyebrow {
  display: inline-block;
  margin: 0 0 var(--space-3);
  padding: 5px 14px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--brand-500);
  background: var(--brand-50);
  border: 1px solid var(--brand-200);
  border-radius: var(--radius-full);
}

.hero-copy h1 {
  margin: 0;
  font-size: 30px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.hero-sub {
  margin: var(--space-2) 0 0;
  color: var(--text-tertiary);
  font-size: 15px;
}

.hero-user-badge {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 6px 16px 6px 6px;
  background: var(--brand-50);
  border: 1px solid var(--brand-200);
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

.user-avatar {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--brand-400), var(--brand-500));
  color: var(--surface-0);
  font-weight: 700;
  font-size: 14px;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

/* ─── Metrics: break the identical-card-grid ─── */
.hero-metrics {
  margin-top: var(--space-5);
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr 1fr;
  gap: var(--space-3);
}

.metric-card {
  padding: var(--space-4) var(--space-4);
  border: 1px solid var(--surface-border);
  border-radius: var(--radius-lg);
  background: var(--surface-50);
  display: flex;
  align-items: center;
  gap: var(--space-3);
  transition:
    transform var(--duration-normal) var(--ease-out-quart),
    box-shadow var(--duration-normal) var(--ease-out-quart),
    border-color var(--duration-normal) var(--ease-out-quart);
  cursor: default;
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--brand-200);
}

/* Featured (BMI) card: larger, brand gradient */
.metric-card.featured {
  background: linear-gradient(135deg, var(--brand-500), var(--brand-400));
  border-color: transparent;
  color: var(--surface-0);
}

.metric-card.featured .metric-label {
  color: oklch(92% 0.02 176);
}

.metric-card.featured .metric-value {
  color: var(--surface-0);
}

.metric-card.featured:hover {
  box-shadow: 0 12px 36px oklch(48% 0.14 176 / 0.28);
  border-color: transparent;
}

.metric-icon {
  font-size: 28px;
  flex-shrink: 0;
  line-height: 1;
}

.metric-body {
  min-width: 0;
}

.metric-label {
  display: block;
  color: var(--text-tertiary);
  font-size: 13px;
  font-weight: 500;
}

.metric-value {
  display: block;
  margin-top: 4px;
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

/* ─── Actions ─── */
.hero-actions {
  margin-top: var(--space-5);
  padding-top: var(--space-4);
  border-top: 1px solid var(--surface-border);
}

.hero-buttons {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  border: 1px solid var(--surface-border);
  border-radius: var(--radius-md);
  padding: 10px 18px;
  background: var(--surface-0);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition:
    background-color var(--duration-fast) var(--ease-out-quart),
    border-color var(--duration-fast) var(--ease-out-quart),
    box-shadow var(--duration-fast) var(--ease-out-quart);
}

.action-btn:hover {
  background: var(--brand-50);
  border-color: var(--brand-300);
  box-shadow: var(--shadow-sm);
}

.action-btn.logout:hover {
  background: oklch(96% 0.02 25);
  border-color: oklch(82% 0.06 25);
}

.action-icon {
  font-size: 16px;
  line-height: 1;
}

@media (max-width: 900px) {
  .hero-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric-card.featured {
    grid-column: span 2;
  }
}

@media (max-width: 640px) {
  .hero {
    padding: var(--space-4);
  }

  .hero-top {
    flex-direction: column;
  }

  .hero-copy h1 {
    font-size: 24px;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .metric-card.featured {
    grid-column: auto;
  }

  .hero-buttons {
    flex-direction: column;
  }

  .action-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>

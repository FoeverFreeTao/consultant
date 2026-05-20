<script setup>
defineProps({
  visible: { type: Boolean, default: false },
  saving: { type: Boolean, default: false },
  message: { type: String, default: '' },
  form: {
    type: Object,
    required: true
  }
})

defineEmits(['submit', 'close'])
</script>

<template>
  <Transition name="modal">
    <div v-if="visible" class="modal-mask" @click.self="$emit('close')">
      <section class="modal-card">
        <div class="modal-header">
          <h2>📊 修改日常状态</h2>
          <button type="button" class="close-btn" @click="$emit('close')">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
          </button>
        </div>

        <form class="modal-form" @submit.prevent="$emit('submit')">
          <label>
            <span>💧 饮水量(ml)</span>
            <input v-model="form.hydrationMl" type="number" min="0" />
          </label>
          <label>
            <span>🌙 睡眠时长(h)</span>
            <input v-model="form.sleepHour" type="number" min="0" step="0.1" />
          </label>
          <label>
            <span>🏃 活动时长(min)</span>
            <input v-model="form.activityMinute" type="number" min="0" />
          </label>

          <p v-if="message" class="message">{{ message }}</p>

          <div class="modal-actions">
            <button type="button" class="ghost-btn" :disabled="saving" @click="$emit('close')">取消</button>
            <button type="submit" class="primary-btn" :disabled="saving">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </Transition>
</template>

<style scoped>
.modal-mask {
  position: fixed;
  inset: 0;
  background: oklch(20% 0.02 240 / 0.4);
  backdrop-filter: blur(4px);
  display: grid;
  place-items: center;
  padding: var(--space-4);
  z-index: 20;
}

.modal-card {
  width: min(520px, 100%);
  border-radius: var(--radius-xl);
  background: var(--surface-0);
  border: 1px solid var(--surface-border);
  box-shadow: var(--shadow-xl);
  padding: var(--space-6);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-3);
}

.modal-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.close-btn {
  width: 36px;
  height: 36px;
  border: 1px solid var(--surface-border);
  border-radius: var(--radius-sm);
  background: var(--surface-50);
  color: var(--text-tertiary);
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: all var(--duration-fast) var(--ease-out-quart);
}

.close-btn:hover {
  background: oklch(96% 0.02 25);
  border-color: oklch(82% 0.06 25);
  color: var(--danger);
}

.modal-form {
  margin-top: var(--space-5);
  display: grid;
  gap: var(--space-4);
}

.modal-form label {
  display: grid;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.modal-form input {
  width: 100%;
  border: 1px solid var(--surface-border);
  border-radius: var(--radius-sm);
  padding: var(--space-3) var(--space-4);
  background: var(--surface-50);
  color: var(--text-primary);
  font-family: inherit;
  font-size: 15px;
  outline: none;
  transition:
    border-color var(--duration-fast) var(--ease-out-quart),
    box-shadow var(--duration-fast) var(--ease-out-quart),
    background-color var(--duration-fast) var(--ease-out-quart);
}

.modal-form input:focus {
  border-color: var(--brand-400);
  background: var(--surface-0);
  box-shadow: 0 0 0 4px oklch(55% 0.14 176 / 0.1);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  padding-top: var(--space-2);
}

.ghost-btn,
.primary-btn {
  border-radius: var(--radius-sm);
  padding: var(--space-3) var(--space-5);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out-quart);
}

.ghost-btn {
  border: 1px solid var(--surface-border);
  background: var(--surface-0);
  color: var(--text-secondary);
}

.ghost-btn:hover {
  background: var(--surface-100);
  border-color: var(--text-tertiary);
}

.primary-btn {
  border: 0;
  background: linear-gradient(135deg, var(--brand-500), var(--brand-400));
  color: var(--surface-0);
  box-shadow: 0 4px 14px oklch(48% 0.14 176 / 0.2);
}

.primary-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 22px oklch(48% 0.14 176 / 0.28);
}

.primary-btn:disabled,
.ghost-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.message {
  margin: 0;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  background: var(--brand-50);
  color: var(--brand-600);
  font-size: 14px;
}

/* ─── Modal transition ─── */
.modal-enter-active {
  transition: opacity var(--duration-normal) var(--ease-out-quart);
}
.modal-leave-active {
  transition: opacity var(--duration-fast) var(--ease-out-quart);
}

.modal-enter-active .modal-card {
  transition: transform var(--duration-slow) var(--ease-out-expo);
}
.modal-leave-active .modal-card {
  transition: transform var(--duration-fast) var(--ease-out-quart);
}

.modal-enter-from {
  opacity: 0;
}
.modal-enter-from .modal-card {
  transform: translateY(20px) scale(0.97);
}

.modal-leave-to {
  opacity: 0;
}
.modal-leave-to .modal-card {
  transform: translateY(10px) scale(0.98);
}
</style>

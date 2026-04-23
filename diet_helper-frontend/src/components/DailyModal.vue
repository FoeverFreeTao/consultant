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
  <div v-if="visible" class="modal-mask" @click.self="$emit('close')">
    <section class="modal-card">
      <div class="modal-header">
        <h2>修改日常状态</h2>
        <button type="button" class="close-btn" @click="$emit('close')">×</button>
      </div>

      <form class="modal-form" @submit.prevent="$emit('submit')">
        <label>
          <span>饮水量(ml)</span>
          <input v-model="form.hydrationMl" type="number" min="0" />
        </label>
        <label>
          <span>睡眠时长(h)</span>
          <input v-model="form.sleepHour" type="number" min="0" step="0.1" />
        </label>
        <label>
          <span>活动时长(min)</span>
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
</template>

<style scoped>
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
  display: grid;
  place-items: center;
  padding: 16px;
  z-index: 20;
}

.modal-card {
  width: min(520px, 100%);
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.18);
  padding: 20px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.modal-header h2 {
  margin: 0;
  font-size: 22px;
}

.close-btn {
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 999px;
  background: #eff6ff;
  cursor: pointer;
  font-size: 22px;
}

.modal-form {
  margin-top: 18px;
  display: grid;
  gap: 14px;
}

.modal-form label {
  display: grid;
  gap: 8px;
  color: #334155;
}

.modal-form input {
  width: 100%;
  border: 1px solid #dbe7fb;
  border-radius: 16px;
  padding: 12px 14px;
  background: #f8fbff;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.ghost-btn,
.primary-btn {
  border-radius: 16px;
  padding: 12px 18px;
  cursor: pointer;
}

.ghost-btn {
  border: 1px solid #cfe0ff;
  background: #fff;
}

.primary-btn {
  border: 0;
  background: #13b4b1;
  color: #fff;
}

.message {
  margin: 0;
  color: #64748b;
}
</style>

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
        <h2>修改个人信息</h2>
        <button type="button" class="close-btn" @click="$emit('close')">×</button>
      </div>

      <form class="modal-form" @submit.prevent="$emit('submit')">
        <label>
          <span>昵称</span>
          <input v-model="form.name" type="text" />
        </label>
        <div class="grid-two">
          <label>
            <span>年龄</span>
            <input v-model="form.age" type="number" min="1" />
          </label>
          <label>
            <span>身高</span>
            <input v-model="form.height" type="number" min="1" step="0.1" />
          </label>
        </div>
        <label>
          <span>体重</span>
          <input v-model="form.weight" type="number" min="1" step="0.1" />
        </label>
        <label>
          <span>目标</span>
          <input v-model="form.target" type="text" />
        </label>
        <label>
          <span>饮食注意</span>
          <textarea v-model="form.allergy" rows="3" />
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
  width: min(560px, 100%);
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

.modal-form input,
.modal-form textarea {
  width: 100%;
  border: 1px solid #dbe7fb;
  border-radius: 16px;
  padding: 12px 14px;
  background: #f8fbff;
}

.grid-two {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
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

@media (max-width: 640px) {
  .grid-two {
    grid-template-columns: 1fr;
  }
}
</style>

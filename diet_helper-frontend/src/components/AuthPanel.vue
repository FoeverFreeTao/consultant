<script setup>
const props = defineProps({
  authMode: { type: String, default: 'login' },
  authLoading: { type: Boolean, default: false },
  authMessage: { type: String, default: '' },
  authForm: {
    type: Object,
    required: true
  }
})

defineEmits(['switch-mode', 'submit-login', 'submit-register'])
</script>

<template>
  <section class="auth-card">
    <div class="auth-tabs">
      <button
        type="button"
        :class="['auth-tab', { active: authMode === 'login' }]"
        @click="$emit('switch-mode', 'login')"
      >
        登录
      </button>
      <button
        type="button"
        :class="['auth-tab', { active: authMode === 'register' }]"
        @click="$emit('switch-mode', 'register')"
      >
        注册
      </button>
    </div>

    <form v-if="authMode === 'login'" class="auth-form" @submit.prevent="$emit('submit-login')">
      <label>
        <span>手机号</span>
        <input v-model="authForm.loginPhone" type="text" placeholder="请输入手机号" />
      </label>
      <label>
        <span>密码</span>
        <input v-model="authForm.loginPassword" type="password" placeholder="请输入密码" />
      </label>
      <button type="submit" class="primary-btn" :disabled="authLoading">
        {{ authLoading ? '登录中...' : '登录' }}
      </button>
    </form>

    <form v-else class="auth-form" @submit.prevent="$emit('submit-register')">
      <label>
        <span>昵称</span>
        <input v-model="authForm.registerName" type="text" placeholder="请输入昵称" />
      </label>
      <label>
        <span>手机号</span>
        <input v-model="authForm.registerPhone" type="text" placeholder="请输入手机号" />
      </label>
      <label>
        <span>密码</span>
        <input v-model="authForm.registerPassword" type="password" placeholder="请输入密码" />
      </label>
      <div class="grid-two">
        <label>
          <span>年龄</span>
          <input v-model="authForm.registerAge" type="number" min="1" placeholder="年龄" />
        </label>
        <label>
          <span>身高(cm)</span>
          <input v-model="authForm.registerHeight" type="number" min="1" placeholder="身高" />
        </label>
      </div>
      <label>
        <span>体重(kg)</span>
        <input v-model="authForm.registerWeight" type="number" min="1" step="0.1" placeholder="体重" />
      </label>
      <button type="submit" class="primary-btn" :disabled="authLoading">
        {{ authLoading ? '提交中...' : '注册并登录' }}
      </button>
    </form>

    <p v-if="authMessage" class="auth-message">{{ authMessage }}</p>
  </section>
</template>

<style scoped>
.auth-card {
  max-width: 1160px;
  margin: 0 auto 16px;
  padding: 20px 22px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 60px rgba(54, 94, 135, 0.08);
}

.auth-tabs {
  display: inline-flex;
  padding: 4px;
  border-radius: 999px;
  background: #eef5ff;
}

.auth-tab {
  border: 0;
  background: transparent;
  color: #64748b;
  border-radius: 999px;
  padding: 10px 18px;
  cursor: pointer;
}

.auth-tab.active {
  background: #fff;
  color: #0f172a;
  box-shadow: 0 8px 20px rgba(43, 93, 164, 0.12);
}

.auth-form {
  margin-top: 18px;
  display: grid;
  gap: 14px;
}

.auth-form label {
  display: grid;
  gap: 8px;
  color: #334155;
  font-size: 14px;
}

.auth-form input {
  width: 100%;
  border: 1px solid #dbe7fb;
  border-radius: 16px;
  padding: 12px 14px;
  font-size: 15px;
  background: #f8fbff;
}

.grid-two {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.primary-btn {
  border: 0;
  border-radius: 16px;
  padding: 14px 18px;
  background: #13b4b1;
  color: #fff;
  font-size: 15px;
  cursor: pointer;
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.auth-message {
  margin: 14px 0 0;
  color: #475569;
  font-size: 14px;
}

@media (max-width: 640px) {
  .grid-two {
    grid-template-columns: 1fr;
  }
}
</style>

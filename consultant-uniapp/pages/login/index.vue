<script setup>
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import SectionHeader from '@/components/SectionHeader.vue'
import { login, register } from '@/api/auth'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const mode = ref('login')
const loading = ref(false)
const message = ref('')
const redirect = ref('/pages/profile/index')

const form = reactive({
  loginPhone: '',
  loginPassword: '',
  registerName: '',
  registerPhone: '',
  registerPassword: '',
  registerAge: '',
  registerHeight: '',
  registerWeight: ''
})

const submit = async () => {
  if (loading.value) {
    return
  }
  loading.value = true
  message.value = ''
  try {
    const payload =
      mode.value === 'login'
        ? {
            phone: form.loginPhone,
            password: form.loginPassword
          }
        : {
            name: form.registerName,
            phone: form.registerPhone,
            password: form.registerPassword,
            age: Number(form.registerAge),
            height: Number(form.registerHeight),
            weight: Number(form.registerWeight)
          }
    const result = mode.value === 'login' ? await login(payload) : await register(payload)
    userStore.setProfile(result)
    await userStore.bootstrap(true)
    message.value = mode.value === 'login' ? '登录成功' : '注册成功'
    setTimeout(() => {
      if (redirect.value.startsWith('/pages/') && !redirect.value.includes('/login/')) {
        if (redirect.value.includes('/chat/') || redirect.value.includes('/session/') || redirect.value.includes('/profile/')) {
          uni.switchTab({
            url: redirect.value
          })
          return
        }
      }
      uni.switchTab({
        url: '/pages/profile/index'
      })
    }, 400)
  } catch (error) {
    message.value = error.message || '提交失败'
  } finally {
    loading.value = false
  }
}

onLoad((options) => {
  if (options?.redirect) {
    redirect.value = decodeURIComponent(options.redirect)
  }
})
</script>

<template>
  <view class="page-shell login-page">
    <view class="hero-card login-hero">
      <SectionHeader
        :title="mode === 'login' ? '欢迎回来' : '创建账号'"
        subtitle="这里已经接好 uni-app 的登录 / 注册表单骨架，可以直接对接你现有的 `/user/login` 和 `/user/register`。"
      />
    </view>

    <view class="mode-switch">
      <button :class="['ghost-btn', { active: mode === 'login' }]" @tap="mode = 'login'">登录</button>
      <button :class="['ghost-btn', { active: mode === 'register' }]" @tap="mode = 'register'">注册</button>
    </view>

    <view class="section-card">
      <view v-if="mode === 'login'" class="form-grid">
        <view class="field-group">
          <text class="field-label">手机号</text>
          <input v-model="form.loginPhone" class="field-input" type="number" placeholder="请输入手机号" />
        </view>
        <view class="field-group">
          <text class="field-label">密码</text>
          <input v-model="form.loginPassword" class="field-input" password placeholder="请输入密码" />
        </view>
      </view>

      <view v-else class="form-grid">
        <view class="field-group">
          <text class="field-label">昵称</text>
          <input v-model="form.registerName" class="field-input" placeholder="请输入昵称" />
        </view>
        <view class="field-group">
          <text class="field-label">手机号</text>
          <input v-model="form.registerPhone" class="field-input" type="number" placeholder="请输入手机号" />
        </view>
        <view class="field-group">
          <text class="field-label">密码</text>
          <input v-model="form.registerPassword" class="field-input" password placeholder="请输入密码" />
        </view>
        <view class="field-group">
          <text class="field-label">年龄</text>
          <input v-model="form.registerAge" class="field-input" type="number" placeholder="请输入年龄" />
        </view>
        <view class="field-group">
          <text class="field-label">身高（m）</text>
          <input v-model="form.registerHeight" class="field-input" type="digit" placeholder="例如 1.68" />
        </view>
        <view class="field-group">
          <text class="field-label">体重（kg）</text>
          <input v-model="form.registerWeight" class="field-input" type="digit" placeholder="例如 62" />
        </view>
      </view>

      <button class="pill-btn submit-btn" :loading="loading" @tap="submit">
        {{ mode === 'login' ? '登录' : '注册' }}
      </button>
      <text v-if="message" class="feedback">{{ message }}</text>
    </view>
  </view>
</template>

<style scoped>
.login-page {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.login-hero {
  padding: 32rpx;
}

.mode-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}

.mode-switch .ghost-btn.active {
  background: #e7fffb;
  border-color: #8be2d4;
  color: #0f766e;
  font-weight: 700;
}

.form-grid {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.submit-btn {
  width: 100%;
  margin-top: 28rpx;
}

.feedback {
  display: block;
  margin-top: 16rpx;
  color: #0f766e;
  font-size: 24rpx;
  text-align: center;
}
</style>

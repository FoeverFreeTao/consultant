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
  <section class="login-screen">
    <div class="login-backdrop" aria-hidden="true">
      <span class="backdrop-line line-a"></span>
      <span class="backdrop-line line-b"></span>
      <span class="backdrop-line line-c"></span>
    </div>

    <div class="login-stage">
      <aside class="brand-scene" aria-label="饮食健康助手">
        <div class="brand-mark">
          <span class="brand-mark-main">食</span>
          <span class="brand-mark-sub">AI</span>
        </div>

        <div class="scene-copy">
          <p class="eyebrow">Diet Helper</p>
          <h1>把每天的饮食选择，整理成清晰可执行的计划。</h1>
          <p>登录和我成为最好的饮食伙伴吧</p>
        </div>

        <div class="plate-visual" aria-hidden="true">
          <div class="plate">
            <span class="plate-ring"></span>
            <span class="food-block grain"></span>
            <span class="food-block greens"></span>
            <span class="food-block protein"></span>
            <span class="food-block accent"></span>
          </div>
          <div class="nutrition-strip">
            <span>Care your health is my aim</span>
            <strong>今天的你一定也可以做的很好！</strong>
          </div>
        </div>
      </aside>

      <div class="auth-card">
        <div class="auth-heading">
          <p class="eyebrow">{{ authMode === 'login' ? 'Welcome back' : 'Create profile' }}</p>
          <h2>{{ authMode === 'login' ? '登录账号' : '创建账号' }}</h2>
          <p>{{ authMode === 'login' ? '继续你的健康咨询与饮食记录。' : '填写基础资料，助手会从第一轮问答开始更懂你。' }}</p>
        </div>

        <div class="auth-tabs" role="tablist" aria-label="账号入口">
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
            <input v-model="authForm.loginPhone" type="text" inputmode="tel" autocomplete="tel" placeholder="请输入手机号" />
          </label>
          <label>
            <span>密码</span>
            <input v-model="authForm.loginPassword" type="password" autocomplete="current-password" placeholder="请输入密码" />
          </label>
          <button type="submit" class="primary-btn" :disabled="authLoading">
            {{ authLoading ? '登录中...' : '登录' }}
          </button>
        </form>

        <form v-else class="auth-form" @submit.prevent="$emit('submit-register')">
          <label>
            <span>昵称</span>
            <input v-model="authForm.registerName" type="text" autocomplete="name" placeholder="请输入昵称" />
          </label>
          <label>
            <span>手机号</span>
            <input v-model="authForm.registerPhone" type="text" inputmode="tel" autocomplete="tel" placeholder="请输入手机号" />
          </label>
          <label>
            <span>密码</span>
            <input v-model="authForm.registerPassword" type="password" autocomplete="new-password" placeholder="请输入密码" />
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
      </div>
    </div>
  </section>
</template>

<style scoped>
.login-screen {
  position: relative;
  min-height: 100vh;
  padding: 36px;
  overflow: hidden;
  background:
    linear-gradient(135deg, oklch(96% 0.018 196) 0%, oklch(93% 0.038 86) 48%, oklch(90% 0.04 244) 100%);
  color: oklch(24% 0.035 238);
}

.login-screen * {
  box-sizing: border-box;
}

.login-backdrop {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.backdrop-line {
  position: absolute;
  height: 1px;
  transform: rotate(-22deg);
  transform-origin: left center;
  background: linear-gradient(90deg, transparent, oklch(65% 0.12 176 / 0.35), transparent);
}

.line-a {
  top: 15%;
  left: -8%;
  width: 68%;
}

.line-b {
  right: -15%;
  bottom: 22%;
  width: 74%;
}

.line-c {
  left: 18%;
  bottom: 8%;
  width: 42%;
}

.login-stage {
  position: relative;
  z-index: 1;
  width: min(1180px, 100%);
  min-height: calc(100vh - 72px);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(360px, 430px);
  align-items: stretch;
  border: 1px solid oklch(89% 0.025 210 / 0.78);
  border-radius: 8px;
  background: oklch(98% 0.008 120 / 0.9);
  box-shadow: 0 28px 90px oklch(38% 0.07 230 / 0.18);
  overflow: hidden;
}

.brand-scene {
  position: relative;
  min-height: 640px;
  padding: 38px;
  display: grid;
  grid-template-rows: auto 1fr auto;
  overflow: hidden;
  background:
    linear-gradient(145deg, oklch(32% 0.075 214) 0%, oklch(26% 0.055 187) 52%, oklch(31% 0.07 92) 100%);
  color: oklch(97% 0.014 112);
}

.brand-scene::before,
.brand-scene::after {
  content: '';
  position: absolute;
  inset: auto 0 0;
  height: 42%;
  background:
    linear-gradient(160deg, transparent 14%, oklch(76% 0.14 150 / 0.18) 15% 26%, transparent 27%),
    linear-gradient(25deg, transparent 22%, oklch(82% 0.11 82 / 0.2) 23% 32%, transparent 33%);
}

.brand-scene::after {
  inset: 0 auto auto 0;
  width: 56%;
  height: 100%;
  opacity: 0.6;
  background:
    repeating-linear-gradient(90deg, oklch(97% 0.012 112 / 0.12) 0 1px, transparent 1px 34px);
}

.brand-mark {
  position: relative;
  z-index: 1;
  display: inline-grid;
  grid-template-columns: auto auto;
  width: max-content;
  align-items: end;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid oklch(96% 0.014 112 / 0.36);
  border-radius: 8px;
  background: oklch(100% 0 0 / 0.08);
}

.brand-mark-main {
  font-size: 30px;
  font-weight: 800;
  line-height: 1;
}

.brand-mark-sub {
  color: oklch(83% 0.15 150);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0;
}

.scene-copy {
  position: relative;
  z-index: 1;
  align-self: center;
  max-width: 560px;
}

.eyebrow {
  margin: 0 0 12px;
  color: oklch(63% 0.13 171);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.scene-copy h1 {
  max-width: 12em;
  margin: 0;
  color: oklch(98% 0.012 108);
  font-size: 44px;
  line-height: 1.08;
  font-weight: 850;
}

.scene-copy p:not(.eyebrow) {
  max-width: 36em;
  margin: 20px 0 0;
  color: oklch(88% 0.025 128);
  font-size: 16px;
}

.plate-visual {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: auto minmax(160px, 230px);
  gap: 22px;
  align-items: end;
}

.plate {
  position: relative;
  width: 230px;
  aspect-ratio: 1;
  border-radius: 50%;
  background:
    radial-gradient(circle at center, oklch(96% 0.012 102) 0 43%, transparent 44%),
    conic-gradient(from -38deg, oklch(71% 0.16 148) 0 28%, oklch(88% 0.13 82) 28% 52%, oklch(70% 0.16 28) 52% 74%, oklch(62% 0.13 206) 74% 100%);
  box-shadow: 0 24px 70px oklch(12% 0.04 210 / 0.32);
}

.plate-ring {
  position: absolute;
  inset: 22px;
  border: 1px solid oklch(20% 0.04 140 / 0.18);
  border-radius: 50%;
}

.food-block {
  position: absolute;
  border-radius: 8px;
  box-shadow: inset 0 -10px 18px oklch(20% 0.04 140 / 0.12);
}

.grain {
  left: 74px;
  top: 52px;
  width: 78px;
  height: 56px;
  background: oklch(88% 0.12 82);
}

.greens {
  left: 42px;
  bottom: 66px;
  width: 72px;
  height: 66px;
  background: oklch(67% 0.17 147);
}

.protein {
  right: 44px;
  bottom: 58px;
  width: 70px;
  height: 78px;
  background: oklch(70% 0.16 29);
}

.accent {
  right: 66px;
  top: 68px;
  width: 44px;
  height: 44px;
  background: oklch(64% 0.13 206);
}

.nutrition-strip {
  padding: 16px;
  border: 1px solid oklch(98% 0.012 112 / 0.24);
  border-radius: 8px;
  background: oklch(100% 0 0 / 0.1);
}

.nutrition-strip span,
.nutrition-strip strong {
  display: block;
}

.nutrition-strip span {
  color: oklch(87% 0.033 134);
  font-size: 13px;
}

.nutrition-strip strong {
  margin-top: 6px;
  color: oklch(98% 0.012 108);
  font-size: 20px;
  font-weight: 800;
}

.auth-card {
  padding: 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: oklch(98.5% 0.006 112);
  border-left: 1px solid oklch(89% 0.02 210);
}

.auth-heading h2 {
  margin: 0;
  color: oklch(24% 0.035 238);
  font-size: 30px;
  line-height: 1.16;
  font-weight: 820;
}

.auth-heading p:not(.eyebrow) {
  margin: 10px 0 0;
  color: oklch(47% 0.035 230);
  font-size: 14px;
}

.auth-tabs {
  width: 100%;
  margin-top: 28px;
  display: inline-flex;
  padding: 3px;
  border: 1px solid oklch(87% 0.025 210);
  border-radius: 8px;
  background: oklch(95% 0.018 205);
}

.auth-tab {
  flex: 1;
  border: 0;
  background: transparent;
  color: oklch(46% 0.035 226);
  border-radius: 6px;
  padding: 10px 18px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 750;
  transition:
    background-color 180ms ease-out,
    color 180ms ease-out,
    box-shadow 180ms ease-out;
}

.auth-tab.active {
  background: oklch(99% 0.004 112);
  color: oklch(24% 0.035 238);
  box-shadow: 0 8px 22px oklch(50% 0.06 220 / 0.13);
}

.auth-form {
  margin-top: 22px;
  display: grid;
  gap: 15px;
}

.auth-form label {
  display: grid;
  gap: 8px;
  color: oklch(34% 0.035 232);
  font-size: 13px;
  font-weight: 700;
}

.auth-form input {
  width: 100%;
  border: 1px solid oklch(86% 0.025 210);
  border-radius: 8px;
  padding: 13px 14px;
  font-size: 15px;
  color: oklch(24% 0.035 238);
  background: oklch(97% 0.01 205);
  outline: none;
  transition:
    border-color 180ms ease-out,
    box-shadow 180ms ease-out,
    background-color 180ms ease-out;
}

.auth-form input:focus {
  border-color: oklch(63% 0.13 171);
  background: oklch(99% 0.004 112);
  box-shadow: 0 0 0 4px oklch(63% 0.13 171 / 0.14);
}

.grid-two {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.primary-btn {
  border: 0;
  border-radius: 8px;
  padding: 14px 18px;
  background: oklch(61% 0.13 176);
  color: oklch(98% 0.012 112);
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 14px 28px oklch(54% 0.12 176 / 0.24);
  transition:
    transform 180ms ease-out,
    box-shadow 180ms ease-out,
    background-color 180ms ease-out;
}

.primary-btn:hover {
  background: oklch(56% 0.13 176);
  box-shadow: 0 18px 32px oklch(54% 0.12 176 / 0.3);
  transform: translateY(-1px);
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.auth-message {
  margin: 14px 0 0;
  padding: 12px 14px;
  border: 1px solid oklch(86% 0.045 176);
  border-radius: 8px;
  color: oklch(38% 0.07 176);
  background: oklch(95% 0.025 176);
  font-size: 14px;
}

@media (max-width: 960px) {
  .login-screen {
    padding: 20px;
  }

  .login-stage {
    min-height: auto;
    grid-template-columns: 1fr;
  }

  .brand-scene {
    min-height: 430px;
  }

  .scene-copy h1 {
    max-width: 13em;
    font-size: 34px;
  }

  .plate-visual {
    grid-template-columns: auto 1fr;
  }
}

@media (max-width: 640px) {
  .login-screen {
    padding: 12px;
  }

  .brand-scene,
  .auth-card {
    padding: 24px;
  }

  .brand-scene {
    min-height: 380px;
  }

  .scene-copy h1 {
    font-size: 28px;
  }

  .scene-copy p:not(.eyebrow) {
    font-size: 14px;
  }

  .plate-visual {
    grid-template-columns: 1fr;
  }

  .plate {
    width: 188px;
  }

  .auth-heading h2 {
    font-size: 26px;
  }

  .grid-two {
    grid-template-columns: 1fr;
  }
}
</style>

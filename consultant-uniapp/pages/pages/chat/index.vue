<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SectionHeader from '@/components/SectionHeader.vue'
import MessageBubble from '@/components/MessageBubble.vue'
import { askChat } from '@/api/chat'
import { useChatStore } from '@/store/chat'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const chatStore = useChatStore()
chatStore.initSessions()

const quickPrompts = [
  '帮我安排一周减脂早餐计划。',
  '今天运动后晚饭怎么吃更合适？',
  '下午嘴馋时可以选什么低负担零食？'
]

const loading = ref(false)
const chatInput = ref('')
const pageReady = ref(false)

const activeSession = computed(() => chatStore.activeSession)
const messages = computed(() => activeSession.value?.messages || [])

const loadChatContext = async () => {
  if (!userStore.isLoggedIn) {
    chatStore.initSessions()
    pageReady.value = true
    return
  }
  try {
    await userStore.bootstrap()
    await chatStore.ensureSessions(userStore.userId)
    if (chatStore.activeSessionId) {
      await chatStore.loadMessages(userStore.userId, chatStore.activeSessionId)
    }
  } catch (error) {
    uni.showToast({
      title: error.message || '聊天初始化失败',
      icon: 'none'
    })
  } finally {
    pageReady.value = true
  }
}

const ensureLogin = () => {
  if (userStore.isLoggedIn) {
    return true
  }
  uni.navigateTo({
    url: `/pages/login/index?redirect=${encodeURIComponent('/pages/chat/index')}`
  })
  return false
}

const submitQuestion = async (content = chatInput.value) => {
  const value = `${content || ''}`.trim()
  if (!value || loading.value) {
    return
  }
  if (!ensureLogin()) {
    return
  }

  const sessionId = activeSession.value?.id
  if (!sessionId) {
    uni.showToast({
      title: '当前没有可用会话',
      icon: 'none'
    })
    return
  }

  chatStore.appendMessage({ role: 'user', content: value }, sessionId)
  if (activeSession.value?.title === '默认会话' || activeSession.value?.title.startsWith('会话 ')) {
    chatStore.updateSessionTitle(sessionId, value.slice(0, 12))
  }
  chatInput.value = ''

  loading.value = true
  try {
    const response = (await askChat({
      memoryId: `chat:memory:user:${userStore.userId}:session:${sessionId}`,
      message: value,
      skillIds: userStore.selectedSkillIds.join(',')
    })).trim()
    chatStore.appendMessage({
      role: 'assistant',
      content: response || '本次回答为空，请稍后再试。'
    }, sessionId)
    await chatStore.loadMessages(userStore.userId, sessionId)
  } catch (error) {
    chatStore.appendMessage({
      role: 'assistant',
      content: error.message || '请求失败，请检查后端地址和跨域配置。'
    }, sessionId)
  } finally {
    loading.value = false
  }
}

onShow(() => {
  loadChatContext()
})
</script>

<template>
  <view class="page-shell chat-page">
    <view class="hero-card hero">
      <view class="hero-main">
        <text class="hero-badge">uni-app 新端</text>
        <text class="hero-title">移动端咨询入口已经就位</text>
        <text class="hero-copy">
          聊天已经接到现有 Spring Boot 接口。登录后会自动加载你的会话、消息历史和技能偏好。
        </text>
      </view>
    </view>

    <view class="section-card">
      <SectionHeader title="快捷问题" subtitle="先把最常用的咨询入口放到移动端首屏。" />
      <view class="prompt-list">
        <view
          v-for="prompt in quickPrompts"
          :key="prompt"
          class="prompt-chip"
          @tap="submitQuestion(prompt)"
        >
          <text>{{ prompt }}</text>
        </view>
      </view>
    </view>

    <view class="section-card">
      <SectionHeader
        title="当前会话"
        :subtitle="activeSession ? activeSession.title : '默认会话'"
      />
      <view v-if="!userStore.isLoggedIn" class="login-tip">
        <text class="status-text">当前未登录。登录后才能真正调用咨询接口并同步你的会话记录。</text>
        <button class="ghost-btn login-btn" @tap="ensureLogin">去登录</button>
      </view>
      <scroll-view class="message-board" scroll-y>
        <MessageBubble
          v-for="(message, index) in messages"
          :key="`${message.role}-${index}`"
          :role="message.role"
          :content="message.content"
        />
        <view v-if="pageReady && !messages.length" class="loading-row">
          <text class="status-text">还没有消息，试试问我今天该怎么吃。</text>
        </view>
        <view v-if="loading" class="loading-row">
          <text class="status-text">正在生成回答...</text>
        </view>
      </scroll-view>
      <view class="composer">
        <textarea
          v-model="chatInput"
          class="field-textarea composer-input"
          maxlength="-1"
          auto-height
          placeholder="输入你的饮食问题"
        />
        <button class="pill-btn composer-btn" :loading="loading" @tap="submitQuestion()">发送</button>
      </view>
    </view>
  </view>
</template>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.hero {
  padding: 34rpx;
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.85)),
    linear-gradient(135deg, rgba(20, 184, 166, 0.16), rgba(15, 118, 110, 0.06));
}

.hero-main {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.hero-badge {
  align-self: flex-start;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: #e7fffb;
  color: #0f766e;
  font-size: 24rpx;
  font-weight: 700;
}

.hero-title {
  font-size: 44rpx;
  font-weight: 800;
  color: #0f172a;
}

.hero-copy {
  line-height: 1.7;
  color: #475569;
}

.prompt-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 24rpx;
}

.prompt-chip {
  padding: 18rpx 22rpx;
  border-radius: 24rpx;
  background: #f8fbff;
  border: 2rpx solid #d8e8ff;
  color: #334155;
}

.message-board {
  height: 720rpx;
  margin-top: 24rpx;
  padding: 12rpx 0;
}

.message-board :deep(.bubble) {
  margin-bottom: 18rpx;
}

.login-tip {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-top: 20rpx;
}

.login-btn {
  width: 100%;
}

.loading-row {
  padding: 16rpx 0;
}

.composer {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-top: 18rpx;
}

.composer-input {
  min-height: 160rpx;
}

.composer-btn {
  width: 100%;
}
</style>

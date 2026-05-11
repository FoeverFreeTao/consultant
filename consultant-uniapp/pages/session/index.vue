<script setup>
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SectionHeader from '@/components/SectionHeader.vue'
import { useChatStore } from '@/store/chat'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const chatStore = useChatStore()
chatStore.initSessions()

const sessions = computed(() => chatStore.sessions)

const loadSessions = async () => {
  if (!userStore.isLoggedIn) {
    chatStore.initSessions()
    return
  }
  try {
    await chatStore.loadRemoteSessions(userStore.userId)
  } catch (error) {
    uni.showToast({
      title: error.message || '会话加载失败',
      icon: 'none'
    })
  }
}

const createSession = async () => {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({
      url: `/pages/login/index?redirect=${encodeURIComponent('/pages/session/index')}`
    })
    return
  }
  try {
    await chatStore.createRemoteSession(userStore.userId, `会话 ${sessions.value.length + 1}`)
  } catch (error) {
    uni.showToast({
      title: error.message || '创建会话失败',
      icon: 'none'
    })
  }
}

const switchSession = async (sessionId) => {
  chatStore.setActiveSession(sessionId)
  if (userStore.isLoggedIn) {
    try {
      await chatStore.loadMessages(userStore.userId, sessionId)
    } catch (error) {
      uni.showToast({
        title: error.message || '消息加载失败',
        icon: 'none'
      })
    }
  }
  uni.switchTab({
    url: '/pages/chat/index'
  })
}

const removeSession = async (sessionId) => {
  if (!userStore.isLoggedIn) {
    return
  }
  try {
    await chatStore.deleteRemoteSession(userStore.userId, sessionId)
  } catch (error) {
    uni.showToast({
      title: error.message || '删除失败',
      icon: 'none'
    })
  }
}

const formatTime = (value) => {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

onShow(() => {
  loadSessions()
})
</script>

<template>
  <view class="page-shell session-page">
    <view class="section-card">
      <SectionHeader
        title="会话列表"
        :subtitle="userStore.isLoggedIn ? '这里已经接上服务端会话接口。' : '登录后可查看和管理服务端会话。'"
      />
      <button class="pill-btn add-btn" @tap="createSession">新建会话</button>
    </view>

    <view
      v-for="session in sessions"
      :key="session.id"
      :class="['section-card', 'session-card', { active: session.id === chatStore.activeSessionId }]"
    >
      <view class="session-head" @tap="switchSession(session.id)">
        <text class="session-title">{{ session.title }}</text>
        <text class="session-time">{{ formatTime(session.updatedAt) }}</text>
      </view>
      <text class="session-preview" @tap="switchSession(session.id)">{{ session.messages?.[0]?.content || '暂无消息' }}</text>
      <button
        v-if="userStore.isLoggedIn && sessions.length > 1"
        class="ghost-btn remove-btn"
        @tap.stop="removeSession(session.id)"
      >
        删除会话
      </button>
    </view>
  </view>
</template>

<style scoped>
.session-page {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.add-btn {
  width: 100%;
  margin-top: 24rpx;
}

.session-card {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.session-head {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.session-card.active {
  border: 2rpx solid #92e5d8;
  background: linear-gradient(180deg, #ffffff, #f0fffb);
}

.session-title {
  font-size: 32rpx;
  font-weight: 700;
}

.session-time {
  color: #64748b;
  font-size: 24rpx;
}

.session-preview {
  color: #334155;
  line-height: 1.7;
}

.remove-btn {
  margin-top: 12rpx;
}
</style>

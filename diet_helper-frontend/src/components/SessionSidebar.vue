<script setup>
defineProps({
  sessions: {
    type: Array,
    default: () => []
  },
  activeSessionId: {
    type: String,
    default: ''
  },
  hasActiveSession: {
    type: Boolean,
    default: false
  }
})

defineEmits(['create-session', 'delete-session', 'switch-session'])

const formatTime = (value) => {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`
}
</script>

<template>
  <aside class="panel sidebar">
    <div class="sidebar-header">
      <h2>会话列表</h2>
      <div class="sidebar-actions">
        <button type="button" class="round-btn add" @click="$emit('create-session')">+</button>
        <button type="button" class="round-btn remove" :disabled="!hasActiveSession" @click="$emit('delete-session')">-</button>
      </div>
    </div>

    <div class="session-list">
      <button
        v-for="session in sessions"
        :key="session.id"
        type="button"
        :class="['session-card', { active: session.id === activeSessionId }]"
        @click="$emit('switch-session', session.id)"
      >
        <span class="session-title">{{ session.title }}</span>
        <span class="session-time">{{ formatTime(session.updatedAt || session.createdAt) }}</span>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.panel {
  min-height: 100%;
  padding: 18px 16px;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 18px 60px rgba(54, 94, 135, 0.08);
}

.sidebar {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.sidebar-header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
}

.sidebar-actions {
  display: flex;
  gap: 10px;
}

.round-btn {
  width: 38px;
  height: 38px;
  border: 0;
  border-radius: 999px;
  color: #fff;
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
}

.round-btn.add {
  background: #0fb9b4;
}

.round-btn.remove {
  background: #ff5757;
}

.round-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.session-list {
  display: grid;
  gap: 14px;
}

.session-card {
  display: grid;
  gap: 10px;
  width: 100%;
  text-align: left;
  padding: 16px 16px 18px;
  border: 1px solid #d9e7fb;
  border-radius: 20px;
  background: #f8fbff;
  cursor: pointer;
}

.session-card.active {
  border-color: #13b4b1;
  background: #eefcfc;
}

.session-title {
  color: #1e293b;
  font-size: 16px;
  font-weight: 600;
}

.session-time {
  color: #64748b;
  font-size: 12px;
}
</style>

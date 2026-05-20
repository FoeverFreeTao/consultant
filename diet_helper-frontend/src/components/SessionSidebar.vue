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
        <button type="button" class="round-btn add" title="新建会话" @click="$emit('create-session')">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M8 3v10M3 8h10" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
        </button>
        <button type="button" class="round-btn remove" title="删除当前会话" :disabled="!hasActiveSession" @click="$emit('delete-session')">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M3 8h10" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
        </button>
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
        <span class="session-indicator"></span>
        <div class="session-body">
          <span class="session-title">{{ session.title }}</span>
          <span class="session-time">{{ formatTime(session.updatedAt || session.createdAt) }}</span>
        </div>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.panel {
  min-height: 100%;
  padding: var(--space-5) var(--space-4);
  border-radius: var(--radius-xl);
  background: var(--surface-0);
  border: 1px solid var(--surface-border);
  box-shadow: var(--shadow-lg);
}

.sidebar {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.sidebar-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 17px;
  font-weight: 700;
}

.sidebar-actions {
  display: flex;
  gap: var(--space-2);
}

.round-btn {
  width: 34px;
  height: 34px;
  border: 1px solid var(--surface-border);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  background: var(--surface-50);
  display: grid;
  place-items: center;
  cursor: pointer;
  transition:
    background-color var(--duration-fast) var(--ease-out-quart),
    border-color var(--duration-fast) var(--ease-out-quart),
    color var(--duration-fast) var(--ease-out-quart),
    box-shadow var(--duration-fast) var(--ease-out-quart);
}

.round-btn.add:hover {
  background: var(--brand-50);
  border-color: var(--brand-300);
  color: var(--brand-500);
  box-shadow: var(--shadow-sm);
}

.round-btn.remove:hover {
  background: oklch(96% 0.02 25);
  border-color: oklch(82% 0.06 25);
  color: var(--danger);
}

.round-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.round-btn:disabled:hover {
  background: var(--surface-50);
  border-color: var(--surface-border);
  color: var(--text-secondary);
  box-shadow: none;
}

.session-list {
  display: grid;
  gap: var(--space-2);
}

.session-card {
  display: flex;
  align-items: stretch;
  gap: var(--space-3);
  width: 100%;
  text-align: left;
  padding: var(--space-3) var(--space-3);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  background: transparent;
  cursor: pointer;
  transition:
    background-color var(--duration-fast) var(--ease-out-quart),
    border-color var(--duration-fast) var(--ease-out-quart),
    box-shadow var(--duration-fast) var(--ease-out-quart);
}

.session-card:hover {
  background: var(--surface-100);
  border-color: var(--surface-border);
}

.session-indicator {
  width: 3px;
  flex-shrink: 0;
  border-radius: var(--radius-full);
  background: transparent;
  transition: background-color var(--duration-normal) var(--ease-out-quart);
}

.session-card.active {
  background: var(--brand-50);
  border-color: var(--brand-200);
}

.session-card.active .session-indicator {
  background: var(--brand-500);
}

.session-body {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.session-title {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-time {
  color: var(--text-tertiary);
  font-size: 12px;
}
</style>

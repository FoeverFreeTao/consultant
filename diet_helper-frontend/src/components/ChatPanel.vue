<script setup>
const props = defineProps({
  modelValue: { type: String, default: '' },
  activeSessionTitle: { type: String, default: '' },
  quickPrompts: {
    type: Array,
    default: () => []
  },
  currentMessages: {
    type: Array,
    default: () => []
  },
  loading: { type: Boolean, default: false },
  isAuthenticated: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'ask-prompt', 'submit-question'])

const updateInput = (event) => {
  emit('update:modelValue', event.target.value)
}

const onEnter = (event) => {
  if (event.shiftKey) {
    return
  }
  event.preventDefault()
  emit('submit-question')
}
</script>

<template>
  <section class="panel chat-panel">
    <div class="chat-header">
      <h2>智能咨询</h2>
      <span class="chat-session">{{ activeSessionTitle }}</span>
    </div>

    <div class="prompt-list">
      <button
        v-for="prompt in quickPrompts"
        :key="prompt"
        type="button"
        class="prompt-chip"
        @click="$emit('ask-prompt', prompt)"
      >
        {{ prompt }}
      </button>
    </div>

    <div class="message-board">
      <article
        v-for="(message, index) in currentMessages"
        :key="`${message.role}-${index}`"
        :class="['message-card', message.role === 'user' ? 'user' : 'assistant']"
      >
        <h3 v-if="message.role === 'assistant' && index === 0 && activeSessionTitle">{{ activeSessionTitle }}</h3>
        <p>{{ message.content }}</p>
      </article>

      <div v-if="loading" class="message-card assistant loading">
        <p>正在生成回答...</p>
      </div>
    </div>

    <div class="input-row">
      <textarea
        :value="modelValue"
        class="chat-input"
        rows="2"
        :placeholder="isAuthenticated ? '输入你的饮食问题' : '请先登录后再开始提问'"
        :disabled="loading"
        @input="updateInput"
        @keydown.enter="onEnter"
      />
      <button type="button" class="send-btn" :disabled="loading" @click="$emit('submit-question')">
        发送
      </button>
    </div>
  </section>
</template>

<style scoped>
.panel {
  min-height: 100%;
  padding: 18px 18px 16px;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 18px 60px rgba(54, 94, 135, 0.08);
}

.chat-panel {
  display: grid;
  grid-template-rows: auto auto 1fr auto;
  gap: 14px;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.chat-header h2 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

.chat-session {
  color: #64748b;
  font-size: 14px;
}

.prompt-list {
  display: grid;
  gap: 10px;
}

.prompt-chip {
  width: 100%;
  text-align: left;
  padding: 12px 14px;
  border: 1px solid #dbe7fb;
  border-radius: 16px;
  background: #f8fbff;
  color: #334155;
  cursor: pointer;
}

.message-board {
  min-height: 360px;
  max-height: 540px;
  padding: 18px;
  border: 1px solid #dbe7fb;
  border-radius: 22px;
  background: #fbfdff;
  overflow: auto;
  display: grid;
  gap: 14px;
  align-content: start;
}

.message-card {
  max-width: 82%;
  padding: 16px 18px;
  border-radius: 20px;
  white-space: pre-wrap;
  line-height: 1.7;
}

.message-card h3 {
  margin: 0 0 12px;
  font-size: 18px;
}

.message-card p {
  margin: 0;
}

.message-card.assistant {
  background: #15b5b3;
  color: #fff;
}

.message-card.user {
  margin-left: auto;
  background: #eff6ff;
  color: #0f172a;
}

.message-card.loading {
  background: #e6f8f8;
  color: #0f766e;
}

.input-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  align-items: end;
}

.chat-input {
  width: 100%;
  resize: none;
  border: 1px solid #dbe7fb;
  border-radius: 16px;
  padding: 14px 16px;
  font-size: 15px;
  background: #fff;
}

.send-btn {
  border: 0;
  border-radius: 16px;
  padding: 14px 20px;
  background: #13b4b1;
  color: #fff;
  font-size: 15px;
  cursor: pointer;
}

.send-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .input-row {
    grid-template-columns: 1fr;
  }

  .message-card {
    max-width: 100%;
  }
}
</style>

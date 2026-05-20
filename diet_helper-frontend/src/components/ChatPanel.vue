<script setup>
import { ref } from 'vue'

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

const fileInput = ref(null)
const selectedImage = ref(null)
const previewUrl = ref('')

const updateInput = (event) => {
  emit('update:modelValue', event.target.value)
}

const onEnter = (event) => {
  if (event.shiftKey) {
    return
  }
  event.preventDefault()
  submitWithImage()
}

const openImagePicker = () => {
  if (props.loading) {
    return
  }
  fileInput.value?.click()
}

const onImageChange = (event) => {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }
  if (!file.type.startsWith('image/')) {
    event.target.value = ''
    return
  }
  selectedImage.value = file
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = URL.createObjectURL(file)
}

const clearImage = () => {
  selectedImage.value = null
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = ''
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const submitWithImage = () => {
  const image = selectedImage.value
  emit('submit-question', image)
  if (image) {
    clearImage()
  }
}
</script>

<template>
  <section class="panel chat-panel">
    <div class="chat-header">
      <div class="chat-header-left">
        <span class="chat-icon">💬</span>
        <h2>智能咨询</h2>
      </div>
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
        <span class="prompt-arrow">→</span>
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
        <img v-if="message.imageUrl" :src="message.imageUrl" alt="上传的餐食照片" class="message-image" />
        <p>{{ message.content }}</p>
      </article>

      <div v-if="loading" class="message-card assistant loading-msg">
        <div class="typing-dots">
          <span></span><span></span><span></span>
        </div>
        <p>正在生成回答...</p>
      </div>
    </div>

    <div class="composer">
      <div v-if="previewUrl" class="image-preview">
        <img :src="previewUrl" alt="待识别的餐食照片" />
        <div class="image-preview-meta">
          <span>{{ selectedImage?.name }}</span>
          <button type="button" class="clear-image-btn" :disabled="loading" @click="clearImage">移除</button>
        </div>
      </div>

      <div class="input-row">
        <input
          ref="fileInput"
          type="file"
          class="file-input"
          accept="image/*"
          :disabled="loading"
          @change="onImageChange"
        />
        <button type="button" class="image-btn" :disabled="loading" title="上传餐食照片" @click="openImagePicker">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><rect x="2" y="3" width="16" height="14" rx="2" stroke="currentColor" stroke-width="1.5"/><circle cx="7" cy="8" r="1.5" fill="currentColor"/><path d="M2 14l4-4 3 3 4-5 5 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </button>
        <textarea
          :value="modelValue"
          class="chat-input"
          rows="2"
          :placeholder="isAuthenticated ? '输入你的饮食问题，也可以上传餐食照片' : '请先登录后再开始提问'"
          :disabled="loading"
          @input="updateInput"
          @keydown.enter="onEnter"
        />
        <button type="button" class="send-btn" :disabled="loading" @click="submitWithImage">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M2 9l14-7-7 14V9H2z" fill="currentColor"/></svg>
          <span>发送</span>
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.panel {
  min-height: 100%;
  padding: var(--space-5);
  border-radius: var(--radius-xl);
  background: var(--surface-0);
  border: 1px solid var(--surface-border);
  box-shadow: var(--shadow-lg);
}

.chat-panel {
  display: grid;
  grid-template-rows: auto auto 1fr auto;
  gap: var(--space-4);
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-3);
}

.chat-header-left {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.chat-icon {
  font-size: 20px;
  line-height: 1;
}

.chat-header h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
}

.chat-session {
  color: var(--text-tertiary);
  font-size: 13px;
  font-weight: 500;
}

/* ─── Quick prompts ─── */
.prompt-list {
  display: grid;
  gap: var(--space-2);
}

.prompt-chip {
  width: 100%;
  text-align: left;
  padding: var(--space-3) var(--space-4);
  border: 1px solid var(--surface-border);
  border-radius: var(--radius-md);
  background: var(--surface-50);
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  transition:
    background-color var(--duration-fast) var(--ease-out-quart),
    border-color var(--duration-fast) var(--ease-out-quart),
    color var(--duration-fast) var(--ease-out-quart),
    transform var(--duration-fast) var(--ease-out-quart);
}

.prompt-chip:hover {
  background: var(--brand-50);
  border-color: var(--brand-200);
  color: var(--brand-600);
  transform: translateX(4px);
}

.prompt-arrow {
  color: var(--brand-400);
  font-weight: 600;
  font-size: 15px;
  transition: transform var(--duration-fast) var(--ease-out-quart);
}

.prompt-chip:hover .prompt-arrow {
  transform: translateX(2px);
}

/* ─── Message board ─── */
.message-board {
  min-height: 340px;
  max-height: 540px;
  padding: var(--space-4);
  border: 1px solid var(--surface-border);
  border-radius: var(--radius-lg);
  background: var(--surface-50);
  overflow: auto;
  display: grid;
  gap: var(--space-3);
  align-content: start;
}

.message-card {
  max-width: 82%;
  padding: var(--space-4) var(--space-5);
  border-radius: var(--radius-lg);
  white-space: pre-wrap;
  line-height: 1.7;
  animation: msgIn var(--duration-slow) var(--ease-out-expo);
}

@keyframes msgIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-card h3 {
  margin: 0 0 var(--space-3);
  font-size: 16px;
  font-weight: 600;
}

.message-card p {
  margin: 0;
}

.message-image {
  display: block;
  width: min(260px, 100%);
  max-height: 220px;
  object-fit: cover;
  border-radius: var(--radius-md);
  margin-bottom: var(--space-2);
}

.message-card.assistant {
  background: linear-gradient(135deg, var(--brand-500), var(--brand-400));
  color: var(--surface-0);
  border-bottom-left-radius: var(--space-1);
}

.message-card.user {
  margin-left: auto;
  background: var(--surface-100);
  color: var(--text-primary);
  border: 1px solid var(--surface-border);
  border-bottom-right-radius: var(--space-1);
}

.message-card.loading-msg {
  background: var(--brand-50);
  color: var(--brand-600);
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

/* Typing dots animation */
.typing-dots {
  display: flex;
  gap: 4px;
  align-items: center;
}

.typing-dots span {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
  background: var(--brand-400);
  animation: dotPulse 1.2s var(--ease-out-quart) infinite;
}

.typing-dots span:nth-child(2) { animation-delay: 0.15s; }
.typing-dots span:nth-child(3) { animation-delay: 0.3s; }

@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1.1); }
}

/* ─── Composer ─── */
.composer {
  display: grid;
  gap: var(--space-3);
}

.image-preview {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: var(--space-3);
  align-items: center;
  padding: var(--space-3);
  border: 1px solid var(--surface-border);
  border-radius: var(--radius-md);
  background: var(--surface-0);
}

.image-preview img {
  width: 80px;
  height: 64px;
  object-fit: cover;
  border-radius: var(--radius-sm);
}

.image-preview-meta {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  color: var(--text-secondary);
  font-size: 13px;
}

.image-preview-meta span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.clear-image-btn {
  flex: 0 0 auto;
  border: 1px solid var(--surface-border);
  border-radius: var(--radius-sm);
  padding: 6px 12px;
  background: var(--surface-50);
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out-quart);
}

.clear-image-btn:hover {
  background: oklch(96% 0.02 25);
  border-color: oklch(82% 0.06 25);
  color: var(--danger);
}

.input-row {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: var(--space-3);
  align-items: end;
}

.file-input {
  display: none;
}

.image-btn {
  width: 46px;
  height: 46px;
  border: 1px solid var(--surface-border);
  border-radius: var(--radius-md);
  background: var(--surface-50);
  color: var(--text-tertiary);
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out-quart);
}

.image-btn:hover {
  background: var(--brand-50);
  border-color: var(--brand-300);
  color: var(--brand-500);
}

.chat-input {
  width: 100%;
  resize: none;
  border: 1px solid var(--surface-border);
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  font-size: 15px;
  font-family: inherit;
  background: var(--surface-0);
  color: var(--text-primary);
  outline: none;
  transition:
    border-color var(--duration-fast) var(--ease-out-quart),
    box-shadow var(--duration-fast) var(--ease-out-quart);
}

.chat-input:focus {
  border-color: var(--brand-400);
  box-shadow: 0 0 0 4px oklch(55% 0.14 176 / 0.1);
}

.chat-input::placeholder {
  color: var(--text-tertiary);
}

.send-btn {
  border: 0;
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-5);
  background: linear-gradient(135deg, var(--brand-500), var(--brand-400));
  color: var(--surface-0);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  box-shadow: 0 6px 20px oklch(48% 0.14 176 / 0.22);
  transition:
    transform var(--duration-fast) var(--ease-out-quart),
    box-shadow var(--duration-fast) var(--ease-out-quart);
}

.send-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 28px oklch(48% 0.14 176 / 0.3);
}

.send-btn:active {
  transform: translateY(0);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.image-btn:disabled,
.clear-image-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .input-row {
    grid-template-columns: 1fr;
  }

  .message-card {
    max-width: 100%;
  }

  .image-btn {
    width: 100%;
    height: auto;
    padding: var(--space-3);
  }

  .send-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>

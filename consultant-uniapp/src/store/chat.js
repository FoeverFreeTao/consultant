import { defineStore } from 'pinia'
import { createChatSession, deleteChatSession, getChatMessages, getChatSessions } from '@/api/chat'
import { STORAGE_KEYS } from '@/config'
import { getStorage, setStorage } from '@/utils/storage'

const defaultAssistantMessage = {
  role: 'assistant',
  content: '你好，我是你的 AI 饮食助手。告诉我你的目标、习惯，或者今天吃了什么，我会给你更容易执行的建议。'
}

const buildDefaultSession = (title = '默认会话') => ({
  id: `session-${Date.now()}`,
  title,
  createdAt: Date.now(),
  updatedAt: Date.now(),
  messages: [{ ...defaultAssistantMessage }]
})

const normalizeSession = (session, index = 0) => ({
  id: session?.id || session?.sessionId || `session-${Date.now()}-${index}`,
  title: session?.title || `会话 ${index + 1}`,
  createdAt: Number(session?.createdAt) || Date.now(),
  updatedAt: Number(session?.updatedAt) || Number(session?.createdAt) || Date.now(),
  messages:
    Array.isArray(session?.messages) && session.messages.length
      ? session.messages.map((message) => ({
          role: message?.role || 'assistant',
          content: message?.content || ''
        }))
      : [{ ...defaultAssistantMessage }]
})

const normalizeMessages = (messages) =>
  Array.isArray(messages) && messages.length
    ? messages.map((message) => ({
        role: message?.role || 'assistant',
        content: message?.content || ''
      }))
    : [{ ...defaultAssistantMessage }]

export const useChatStore = defineStore('chat', {
  state: () => ({
    sessions: getStorage(STORAGE_KEYS.guestSessions, [buildDefaultSession()]),
    activeSessionId: getStorage(STORAGE_KEYS.currentSessionId, ''),
    initialized: false
  }),
  getters: {
    activeSession(state) {
      return state.sessions.find((item) => item.id === state.activeSessionId) || state.sessions[0] || null
    }
  },
  actions: {
    initSessions() {
      if (!this.sessions.length) {
        this.sessions = [buildDefaultSession()]
      }
      if (!this.activeSessionId) {
        this.activeSessionId = this.sessions[0].id
      }
      this.initialized = true
      this.persist()
    },
    resetToGuest() {
      this.sessions = [buildDefaultSession()]
      this.activeSessionId = this.sessions[0].id
      this.initialized = false
      this.persist()
    },
    setSessions(sessions, preferredSessionId = '') {
      this.sessions = sessions?.length ? sessions.map((item, index) => normalizeSession(item, index)) : [buildDefaultSession()]
      const candidateId = preferredSessionId || this.activeSessionId
      this.activeSessionId = this.sessions.some((item) => item.id === candidateId) ? candidateId : this.sessions[0].id
      this.persist()
    },
    setActiveSession(sessionId) {
      this.activeSessionId = sessionId
      this.persist()
    },
    replaceSessionMessages(sessionId, messages) {
      const target = this.sessions.find((item) => item.id === sessionId)
      if (!target) {
        return
      }
      target.messages = normalizeMessages(messages)
      target.updatedAt = Date.now()
      this.persist()
    },
    appendMessage(message, sessionId = this.activeSessionId) {
      const target = this.sessions.find((item) => item.id === sessionId)
      if (!target) {
        return
      }
      target.messages = [...target.messages, message]
      target.updatedAt = Date.now()
      this.persist()
    },
    updateSessionTitle(sessionId, title) {
      const target = this.sessions.find((item) => item.id === sessionId)
      if (!target || !title) {
        return
      }
      target.title = title
      target.updatedAt = Date.now()
      this.persist()
    },
    async loadRemoteSessions(userId) {
      const sessions = await getChatSessions(userId)
      if (!Array.isArray(sessions) || !sessions.length) {
        const created = await createChatSession({
          userId,
          title: '默认会话'
        })
        this.setSessions([created], created?.id || created?.sessionId)
        return this.sessions
      }
      this.setSessions(sessions)
      return this.sessions
    },
    async ensureSessions(userId) {
      if (userId) {
        return this.loadRemoteSessions(userId)
      }
      this.initSessions()
      return this.sessions
    },
    async loadMessages(userId, sessionId = this.activeSessionId) {
      if (!userId || !sessionId) {
        return []
      }
      const messages = await getChatMessages(userId, sessionId)
      this.replaceSessionMessages(sessionId, messages)
      return this.activeSession?.messages || []
    },
    async createRemoteSession(userId, title) {
      const created = await createChatSession({
        userId,
        title
      })
      this.setSessions([normalizeSession(created), ...this.sessions], created?.id || created?.sessionId)
      return this.activeSession
    },
    async deleteRemoteSession(userId, sessionId) {
      await deleteChatSession({
        userId,
        sessionId
      })
      const nextSessions = this.sessions.filter((item) => item.id !== sessionId)
      if (!nextSessions.length) {
        const created = await createChatSession({
          userId,
          title: '默认会话'
        })
        this.setSessions([created], created?.id || created?.sessionId)
        return
      }
      this.setSessions(nextSessions, nextSessions[0]?.id)
    },
    persist() {
      setStorage(STORAGE_KEYS.guestSessions, this.sessions)
      setStorage(STORAGE_KEYS.currentSessionId, this.activeSessionId)
    }
  }
})

import { request, requestText } from '@/utils/request'

export const createChatSession = (payload) =>
  request({
    url: '/chat/sessions/create',
    method: 'POST',
    data: payload
  })

export const deleteChatSession = (payload) =>
  request({
    url: '/chat/sessions/delete',
    method: 'POST',
    data: payload
  })

export const getChatSessions = (userId) =>
  request({
    url: '/chat/sessions',
    params: { userId }
  })

export const getChatMessages = (userId, sessionId) =>
  request({
    url: '/chat/sessions/messages',
    params: { userId, sessionId }
  })

export const askChat = (params) =>
  requestText({
    url: '/chat',
    params,
    timeout: 180000
  })

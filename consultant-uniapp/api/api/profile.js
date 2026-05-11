import { request } from '@/utils/request'

export const getProfile = (phone) =>
  request({
    url: '/user/profile',
    params: { phone }
  })

export const updateProfile = (payload) =>
  request({
    url: '/user/profile/update',
    method: 'POST',
    data: payload
  })

export const getDaily = (userId) =>
  request({
    url: '/user/daily',
    params: { userId }
  })

export const updateDaily = (payload) =>
  request({
    url: '/user/daily/update',
    method: 'POST',
    data: payload
  })

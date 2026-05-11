import { request } from '@/utils/request'

export const login = (payload) =>
  request({
    url: '/user/login',
    method: 'POST',
    data: payload
  })

export const register = (payload) =>
  request({
    url: '/user/register',
    method: 'POST',
    data: payload
  })

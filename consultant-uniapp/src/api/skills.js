import { request } from '@/utils/request'

export const getSkillCatalog = () =>
  request({
    url: '/skills/list'
  })

export const getUserSkills = (userId) =>
  request({
    url: '/skills/user',
    params: { userId }
  })

export const applyUserSkills = (payload) =>
  request({
    url: '/skills/user/apply',
    method: 'POST',
    data: payload
  })

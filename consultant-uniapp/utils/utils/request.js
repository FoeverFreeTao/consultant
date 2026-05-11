import { API_BASE_URL } from '@/config'

const joinUrl = (url) => {
  if (/^https?:\/\//.test(url)) {
    return url
  }
  return `${API_BASE_URL}${url}`
}

const toQueryString = (params = {}) => {
  const entries = Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
  if (!entries.length) {
    return ''
  }
  return entries
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join('&')
}

export const request = ({ url, method = 'GET', data, params, header }) =>
  new Promise((resolve, reject) => {
    const query = toQueryString(params)
    const requestUrl = query ? `${joinUrl(url)}?${query}` : joinUrl(url)
    uni.request({
      url: requestUrl,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        ...header
      },
      success: (res) => {
        const body = res.data || {}
        if (res.statusCode >= 200 && res.statusCode < 300 && body.success) {
          resolve(body.data)
          return
        }
        reject(new Error(body.message || '请求失败'))
      },
      fail: (error) => {
        reject(new Error(error.errMsg || '网络异常'))
      }
    })
  })

export const requestText = ({ url, method = 'GET', data, params, header }) =>
  new Promise((resolve, reject) => {
    const query = toQueryString(params)
    const requestUrl = query ? `${joinUrl(url)}?${query}` : joinUrl(url)
    uni.request({
      url: requestUrl,
      method,
      data,
      header,
      success: (res) => {
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject(new Error('请求失败'))
          return
        }
        if (typeof res.data === 'string') {
          resolve(res.data)
          return
        }
        resolve(`${res.data ?? ''}`)
      },
      fail: (error) => {
        reject(new Error(error.errMsg || '网络异常'))
      }
    })
  })

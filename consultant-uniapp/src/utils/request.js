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

const formatRequestError = (res, fallbackMessage) => {
  const body = res?.data
  const bodyMessage = typeof body?.message === 'string' ? body.message.trim() : ''
  if (bodyMessage) {
    return bodyMessage
  }
  if (res?.statusCode === 502) {
    return '服务暂时不可用，请确认后端服务和网关已启动'
  }
  if (res?.statusCode === 504) {
    return '服务响应超时，请稍后重试'
  }
  return fallbackMessage
}

export const request = ({ url, method = 'GET', data, params, header, timeout }) =>
  new Promise((resolve, reject) => {
    const query = toQueryString(params)
    const requestUrl = query ? `${joinUrl(url)}?${query}` : joinUrl(url)
    uni.request({
      url: requestUrl,
      method,
      timeout,
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
        reject(new Error(formatRequestError(res, '请求失败')))
      },
      fail: (error) => {
        const errMsg = error?.errMsg || ''
        if (errMsg.includes('timeout')) {
          reject(new Error('请求超时，请检查服务是否可访问'))
          return
        }
        reject(new Error(errMsg || '网络异常'))
      }
    })
  })

export const requestText = ({ url, method = 'GET', data, params, header, timeout }) =>
  new Promise((resolve, reject) => {
    const query = toQueryString(params)
    const requestUrl = query ? `${joinUrl(url)}?${query}` : joinUrl(url)
    uni.request({
      url: requestUrl,
      method,
      timeout,
      data,
      header,
      success: (res) => {
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject(new Error(formatRequestError(res, '请求失败')))
          return
        }
        if (typeof res.data === 'string') {
          resolve(res.data)
          return
        }
        resolve(`${res.data ?? ''}`)
      },
      fail: (error) => {
        const errMsg = error?.errMsg || ''
        if (errMsg.includes('timeout')) {
          reject(new Error('请求超时，请检查服务是否可访问'))
          return
        }
        reject(new Error(errMsg || '网络异常'))
      }
    })
  })

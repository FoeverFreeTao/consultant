export const getStorage = (key, fallback = null) => {
  try {
    const value = uni.getStorageSync(key)
    return value === '' || value === undefined ? fallback : value
  } catch (error) {
    return fallback
  }
}

export const setStorage = (key, value) => {
  uni.setStorageSync(key, value)
}

export const removeStorage = (key) => {
  uni.removeStorageSync(key)
}

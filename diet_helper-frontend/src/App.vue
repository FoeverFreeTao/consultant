<script setup>
import { computed, ref, watch } from 'vue'
import HeroSection from './components/HeroSection.vue'
import AuthPanel from './components/AuthPanel.vue'
import SessionSidebar from './components/SessionSidebar.vue'
import ChatPanel from './components/ChatPanel.vue'
import ProfilePanel from './components/ProfilePanel.vue'
import ProfileModal from './components/ProfileModal.vue'
import DailyModal from './components/DailyModal.vue'

const storageKey = 'consultant_user_auth'
const sessionStoragePrefix = 'consultant_chat_sessions'
const defaultSessionTitle = '默认会话'
const sessionTitlePrefix = '会话'

const defaultAssistantMessage = {
  role: 'assistant',
  content: '你好，我是你的 AI 饮食助手。告诉我你的目标、习惯，或者今天吃了什么，我会给你更容易执行的建议。'
}

const buildDefaultSession = (title = defaultSessionTitle) => ({
  id: `session-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
  title,
  createdAt: Date.now(),
  updatedAt: Date.now(),
  messages: [{ ...defaultAssistantMessage }]
})

const userProfile = ref({
  nickname: '清',
  age: 29,
  heightCm: 168,
  weightKg: 62,
  target: '减脂与健康管理',
  allergy: '无'
})

const activeUser = ref(null)
const isAuthenticated = ref(false)
const authMode = ref('login')
const authLoading = ref(false)
const authMessage = ref('')

const authForm = ref({
  loginPhone: '',
  loginPassword: '',
  registerName: '',
  registerPhone: '',
  registerPassword: '',
  registerAge: '',
  registerHeight: '',
  registerWeight: ''
})

const profileModalVisible = ref(false)
const profileSaving = ref(false)
const profileMessage = ref('')
const profileForm = ref({
  name: '',
  age: '',
  height: '',
  weight: '',
  target: '',
  allergy: ''
})

const dailyModalVisible = ref(false)
const dailySaving = ref(false)
const dailyMessage = ref('')
const dailyForm = ref({
  hydrationMl: '',
  sleepHour: '',
  activityMinute: ''
})

const hydrationMl = ref(1350)
const sleepHour = ref(7.2)
const activityMinute = ref(42)

const skillCatalog = ref([])
const selectedSkillIds = ref([])
const skillsLoading = ref(false)
const skillsSaving = ref(false)
const skillsMessage = ref('')

const quickPrompts = [
  '帮我安排一周减脂早餐计划。',
  '晚上加班后吃什么不容易胖？',
  '有没有清淡又高蛋白的晚餐建议？',
  '今天下午嘴馋时可以吃什么零食？'
]

const chatInput = ref('')
const loading = ref(false)
const chatSessions = ref([])
const activeSessionId = ref('')
let loadSessionsPromise = null

const activeSession = computed(
  () => chatSessions.value.find((session) => session.id === activeSessionId.value) || chatSessions.value[0]
)

const currentMessages = computed(() => activeSession.value?.messages || [])

const bmi = computed(() => {
  const serverBmi = Number(activeUser.value?.bmi)
  if (Number.isFinite(serverBmi) && serverBmi > 0) {
    return serverBmi.toFixed(1)
  }
  const meters = userProfile.value.heightCm / 100
  if (!meters) {
    return '--'
  }
  return (userProfile.value.weightKg / (meters * meters)).toFixed(1)
})

const hydrationTarget = computed(() => Math.max(1, Math.round(userProfile.value.weightKg * 32)))
const hydrationProgress = computed(() => Math.min(100, Math.round((hydrationMl.value / hydrationTarget.value) * 100)))
const sessionStorageKey = computed(() => `${sessionStoragePrefix}_guest`)
const memoryId = computed(() => {
  const currentSession = activeSession.value
  if (!currentSession) {
    return activeUser.value?.id ? `user-${activeUser.value.id}` : userProfile.value.nickname
  }
  return activeUser.value?.id
    ? `chat:memory:user:${activeUser.value.id}:session:${currentSession.id}`
    : currentSession.id
})

const buildSessionTitle = (index) => `${sessionTitlePrefix} ${index + 1}`

const setSessions = (sessions) => {
  if (!Array.isArray(sessions) || sessions.length === 0) {
    const fallback = buildDefaultSession(defaultSessionTitle)
    chatSessions.value = [fallback]
    activeSessionId.value = fallback.id
    return
  }
  const deduplicated = []
  const seen = new Set()
  sessions.forEach((session, index) => {
    const normalized = {
      id: session.id || session.sessionId || `session-${Date.now()}-${index}`,
      title: session.title || buildSessionTitle(index),
      createdAt: session.createdAt || Date.now(),
      updatedAt: session.updatedAt || Date.now(),
      messages: Array.isArray(session.messages) && session.messages.length > 0 ? session.messages : [{ ...defaultAssistantMessage }]
    }
    const duplicateKey = `${normalized.title}|${normalized.createdAt}|${normalized.updatedAt}`
    if (seen.has(duplicateKey)) {
      return
    }
    seen.add(duplicateKey)
    deduplicated.push(normalized)
  })
  chatSessions.value = deduplicated
  if (!chatSessions.value.some((session) => session.id === activeSessionId.value)) {
    activeSessionId.value = chatSessions.value[0].id
  }
}

const normalizeSession = (session, index = 0) => ({
  id: session.id || session.sessionId || `session-${Date.now()}-${index}`,
  title: session.title || buildSessionTitle(index),
  createdAt: session.createdAt || Date.now(),
  updatedAt: session.updatedAt || Date.now(),
  messages: Array.isArray(session.messages) && session.messages.length > 0 ? session.messages : [{ ...defaultAssistantMessage }]
})

const replaceSessionMessages = (sessionId, messages) => {
  chatSessions.value = chatSessions.value.map((session) =>
    session.id === sessionId
      ? {
          ...session,
          messages: Array.isArray(messages) && messages.length > 0 ? messages : [{ ...defaultAssistantMessage }]
        }
      : session
  )
}

const fetchSessionList = async (userId) => {
  const params = new URLSearchParams({ userId: String(userId) })
  const response = await fetch(`/chat/sessions?${params.toString()}`)
  const body = await response.json().catch(() => ({}))
  if (!response.ok || !body.success) {
    throw new Error(body.message || 'load_sessions_failed')
  }
  return Array.isArray(body.data) ? body.data : []
}

const fetchSessionMessages = async (userId, sessionId) => {
  const params = new URLSearchParams({ userId: String(userId), sessionId })
  const response = await fetch(`/chat/sessions/messages?${params.toString()}`)
  const body = await response.json().catch(() => ({}))
  if (!response.ok || !body.success) {
    throw new Error(body.message || 'load_messages_failed')
  }
  return Array.isArray(body.data) ? body.data : []
}

const createRemoteSession = async (title = defaultSessionTitle) => {
  const response = await fetch('/chat/sessions/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      userId: activeUser.value.id,
      title
    })
  })
  const body = await response.json().catch(() => ({}))
  if (!response.ok || !body.success) {
    throw new Error(body.message || 'create_session_failed')
  }
  return body.data
}

const deleteRemoteSession = async (sessionId) => {
  const response = await fetch('/chat/sessions/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      userId: activeUser.value.id,
      sessionId
    })
  })
  const body = await response.json().catch(() => ({}))
  if (!response.ok || !body.success) {
    throw new Error(body.message || 'delete_session_failed')
  }
}

const hydrateSessionMessages = async (sessionId) => {
  if (!activeUser.value?.id || !sessionId) {
    return
  }
  try {
    const messages = await fetchSessionMessages(activeUser.value.id, sessionId)
    replaceSessionMessages(sessionId, messages)
  } catch (error) {
    replaceSessionMessages(sessionId, [])
  }
}

const loadSessionsImpl = async () => {
  if (activeUser.value?.id) {
    try {
      let sessions = await fetchSessionList(activeUser.value.id)
      if (!sessions.length) {
        const created = await createRemoteSession(defaultSessionTitle)
        sessions = [created]
      }
      setSessions(sessions)
      await hydrateSessionMessages(activeSessionId.value)
      return
    } catch (error) {
      chatSessions.value = [buildDefaultSession(defaultSessionTitle)]
      activeSessionId.value = chatSessions.value[0].id
      return
    }
  }
  const saved = localStorage.getItem(sessionStorageKey.value)
  if (!saved) {
    setSessions([buildDefaultSession(defaultSessionTitle)])
    return
  }
  try {
    const parsed = JSON.parse(saved)
    setSessions(parsed)
  } catch (error) {
    setSessions([buildDefaultSession(defaultSessionTitle)])
  }
}

const loadSessions = async () => {
  if (loadSessionsPromise) {
    return loadSessionsPromise
  }
  loadSessionsPromise = loadSessionsImpl().finally(() => {
    loadSessionsPromise = null
  })
  return loadSessionsPromise
}

const resolveActiveUserId = async () => {
  if (activeUser.value?.id) {
    return activeUser.value.id
  }
  const phone = activeUser.value?.phone
  if (!phone) {
    return null
  }
  const latest = await fetchLatestProfile(phone)
  if (!latest?.id) {
    return null
  }
  applyUser({ ...activeUser.value, ...latest })
  return latest.id
}

const persistSessions = () => {
  if (activeUser.value?.id) {
    return
  }
  localStorage.setItem(sessionStorageKey.value, JSON.stringify(chatSessions.value))
}

watch(
  chatSessions,
  () => {
    persistSessions()
  },
  { deep: true }
)

watch(
  () => activeUser.value?.id,
  () => {
    loadSessions()
  }
)

const restoreAuth = () => {
  const saved = localStorage.getItem(storageKey)
  if (!saved) {
    loadSessions()
    return
  }
  try {
    const user = JSON.parse(saved)
    applyUser(user, false)
    if (!user?.id && user?.phone) {
      fetchLatestProfile(user.phone)
        .then((latest) => {
          if (latest?.id) {
            applyUser({ ...user, ...latest })
          }
        })
        .catch(() => {})
    }
  } catch (error) {
    localStorage.removeItem(storageKey)
    loadSessions()
  }
}

const applyUser = (user, persist = true) => {
  if (!user) {
    return
  }
  activeUser.value = user
  isAuthenticated.value = true
  userProfile.value.nickname = user.name || userProfile.value.nickname
  if (typeof user.age === 'number') {
    userProfile.value.age = user.age
  }
  if (typeof user.height === 'number') {
    userProfile.value.heightCm = Math.round(user.height * 100)
  } else if (user.height !== null && user.height !== undefined && user.height !== '') {
    userProfile.value.heightCm = Math.round(Number(user.height) * 100)
  }
  if (typeof user.weight === 'number') {
    userProfile.value.weightKg = Number(user.weight)
  } else if (user.weight !== null && user.weight !== undefined && user.weight !== '') {
    userProfile.value.weightKg = Number(user.weight)
  }
  if (persist) {
    localStorage.setItem(storageKey, JSON.stringify(user))
  }

  if (user.id) {
    fetchDailyStatus(user.id)
    fetchUserSkills(user.id)
  }
}

const logout = () => {
  isAuthenticated.value = false
  activeUser.value = null
  localStorage.removeItem(storageKey)
  authMessage.value = '已退出登录'
  profileModalVisible.value = false
  dailyModalVisible.value = false
  selectedSkillIds.value = []
  skillsMessage.value = ''
  loadSessions()
}

const switchAuthMode = (mode) => {
  authMode.value = mode
  authMessage.value = ''
}

const doAuthRequest = async (url, payload) => {
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  })
  const body = await response.json().catch(() => ({}))
  if (!response.ok || !body.success) {
    throw new Error(body.message || '请求失败')
  }
  return body
}

const fetchLatestProfile = async (phone) => {
  if (!phone) {
    return null
  }
  const params = new URLSearchParams({ phone })
  const response = await fetch(`/user/profile?${params.toString()}`)
  const body = await response.json().catch(() => ({}))
  if (!response.ok || !body.success) {
    return null
  }
  return body.data || null
}

const fetchDailyStatus = async (userId) => {
  if (!userId) {
    return null
  }
  const params = new URLSearchParams({ userId: String(userId) })
  const response = await fetch(`/user/daily?${params.toString()}`)
  const body = await response.json().catch(() => ({}))
  if (!response.ok || !body.success) {
    return null
  }
  const data = body.data || {}
  const nextHydrationMl = Number(data.hydrationMl)
  const nextSleepHour = Number(data.sleepHour)
  const nextActivityMinute = Number(data.activityMinute)
  if (Number.isFinite(nextHydrationMl)) {
    hydrationMl.value = nextHydrationMl
  }
  if (Number.isFinite(nextSleepHour)) {
    sleepHour.value = nextSleepHour
  }
  if (Number.isFinite(nextActivityMinute)) {
    activityMinute.value = nextActivityMinute
  }
  return data
}

const loadSkillCatalog = async () => {
  if (skillsLoading.value) {
    return
  }
  skillsLoading.value = true
  try {
    const response = await fetch('/skills/list')
    const body = await response.json().catch(() => ({}))
    if (!response.ok || !body.success) {
      throw new Error(body.message || '技能列表加载失败')
    }
    skillCatalog.value = Array.isArray(body.data) ? body.data : []
  } catch (error) {
    skillsMessage.value = error.message || '技能列表加载失败'
  } finally {
    skillsLoading.value = false
  }
}

const fetchUserSkills = async (userId) => {
  if (!userId) {
    selectedSkillIds.value = []
    return
  }
  try {
    const params = new URLSearchParams({ userId: String(userId) })
    const response = await fetch(`/skills/user?${params.toString()}`)
    const body = await response.json().catch(() => ({}))
    if (!response.ok || !body.success) {
      throw new Error(body.message || '用户技能加载失败')
    }
    const selected = Array.isArray(body.data) ? body.data.map((item) => item.id).filter(Boolean) : []
    selectedSkillIds.value = selected
  } catch (error) {
    skillsMessage.value = error.message || '用户技能加载失败'
  }
}

const toggleSkillSelection = (skillId) => {
  const current = new Set(selectedSkillIds.value)
  if (current.has(skillId)) {
    current.delete(skillId)
  } else {
    current.add(skillId)
  }
  selectedSkillIds.value = Array.from(current)
}

const saveSkills = async () => {
  if (!activeUser.value?.id || skillsSaving.value) {
    return
  }
  skillsSaving.value = true
  skillsMessage.value = ''
  try {
    const payload = {
      userId: activeUser.value.id,
      skillIds: selectedSkillIds.value
    }
    const response = await fetch('/skills/user/apply', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    })
    const body = await response.json().catch(() => ({}))
    if (!response.ok || !body.success) {
      throw new Error(body.message || '保存技能失败')
    }
    const selected = Array.isArray(body.data) ? body.data.map((item) => item.id).filter(Boolean) : []
    selectedSkillIds.value = selected
    skillsMessage.value = '技能偏好已更新'
  } catch (error) {
    skillsMessage.value = error.message || '保存技能失败'
  } finally {
    skillsSaving.value = false
  }
}

const openProfileModal = () => {
  if (!isAuthenticated.value || !activeUser.value?.phone) {
    return
  }
  profileForm.value = {
    name: activeUser.value.name || userProfile.value.nickname || '',
    age: activeUser.value.age ?? userProfile.value.age ?? '',
    height: activeUser.value.height ?? (userProfile.value.heightCm ? userProfile.value.heightCm / 100 : ''),
    weight: activeUser.value.weight ?? userProfile.value.weightKg ?? '',
    target: userProfile.value.target || '',
    allergy: userProfile.value.allergy || ''
  }
  profileMessage.value = ''
  profileModalVisible.value = true
}

const closeProfileModal = () => {
  if (profileSaving.value) {
    return
  }
  profileModalVisible.value = false
  profileMessage.value = ''
}

const normalizeHeightToMeter = (heightInput) => {
  const value = Number(heightInput)
  if (!Number.isFinite(value) || value <= 0) {
    return null
  }
  if (value > 3) {
    return value / 100
  }
  return value
}

const submitProfileUpdate = async () => {
  if (!isAuthenticated.value || !activeUser.value?.phone || profileSaving.value) {
    return
  }
  profileSaving.value = true
  profileMessage.value = ''
  try {
    const payload = {
      phone: activeUser.value.phone,
      name: profileForm.value.name.trim(),
      age: profileForm.value.age ? Number(profileForm.value.age) : null,
      height: normalizeHeightToMeter(profileForm.value.height),
      weight: profileForm.value.weight ? Number(profileForm.value.weight) : null
    }
    const response = await fetch('/user/profile/update', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    })
    const body = await response.json().catch(() => ({}))
    if (!response.ok || !body.success) {
      throw new Error(body.message || '更新个人信息失败')
    }
    userProfile.value.target = profileForm.value.target.trim() || userProfile.value.target
    userProfile.value.allergy = profileForm.value.allergy.trim() || userProfile.value.allergy
    applyUser(body.data)
    profileMessage.value = '个人信息已更新'
    profileModalVisible.value = false
  } catch (error) {
    profileMessage.value = error.message || '更新个人信息失败'
  } finally {
    profileSaving.value = false
  }
}

const openDailyModal = () => {
  if (!isAuthenticated.value) {
    return
  }
  dailyForm.value = {
    hydrationMl: hydrationMl.value,
    sleepHour: sleepHour.value,
    activityMinute: activityMinute.value
  }
  dailyMessage.value = ''
  dailyModalVisible.value = true
}

const closeDailyModal = () => {
  if (dailySaving.value) {
    return
  }
  dailyModalVisible.value = false
  dailyMessage.value = ''
}

const submitDailyUpdate = async () => {
  if (!isAuthenticated.value || dailySaving.value) {
    return
  }
  dailySaving.value = true
  dailyMessage.value = ''
  try {
    const userId = await resolveActiveUserId()
    if (!userId) {
      throw new Error('当前登录状态已失效，请重新登录后再试')
    }
    const payload = {
      userId,
      hydrationMl: dailyForm.value.hydrationMl ? Number(dailyForm.value.hydrationMl) : 0,
      sleepHour: dailyForm.value.sleepHour ? Number(dailyForm.value.sleepHour) : 0,
      activityMinute: dailyForm.value.activityMinute ? Number(dailyForm.value.activityMinute) : 0
    }
    const response = await fetch('/user/daily/update', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    })
    const body = await response.json().catch(() => ({}))
    if (!response.ok || !body.success) {
      throw new Error(body.message || '更新日常状态失败')
    }
    await fetchDailyStatus(userId)
    dailyMessage.value = '日常状态已更新'
    dailyModalVisible.value = false
  } catch (error) {
    dailyMessage.value = error.message || '更新日常状态失败'
  } finally {
    dailySaving.value = false
  }
}

const createSession = async () => {
  if (activeUser.value?.id) {
    try {
      const created = await createRemoteSession(`${sessionTitlePrefix} ${chatSessions.value.length + 1}`)
      const session = normalizeSession(created, chatSessions.value.length)
      chatSessions.value = [session, ...chatSessions.value]
      activeSessionId.value = session.id
      return
    } catch (error) {
      return
    }
  }
  const session = buildDefaultSession(`${sessionTitlePrefix} ${chatSessions.value.length + 1}`)
  chatSessions.value = [session, ...chatSessions.value]
  activeSessionId.value = session.id
}

const switchSession = async (sessionId) => {
  activeSessionId.value = sessionId
  if (activeUser.value?.id) {
    await hydrateSessionMessages(sessionId)
  }
}

const deleteSession = async (sessionId) => {
  if (!sessionId) {
    return
  }
  if (activeUser.value?.id) {
    try {
      await deleteRemoteSession(sessionId)
      let nextSessions = chatSessions.value.filter((session) => session.id !== sessionId)
      if (!nextSessions.length) {
        const created = await createRemoteSession(defaultSessionTitle)
        nextSessions = [normalizeSession(created)]
      }
      chatSessions.value = nextSessions
      if (activeSessionId.value === sessionId) {
        activeSessionId.value = nextSessions[0]?.id || ''
        if (activeSessionId.value) {
          await hydrateSessionMessages(activeSessionId.value)
        }
      }
    } catch (error) {
      return
    }
    return
  }
  if (chatSessions.value.length === 1) {
    setSessions([buildDefaultSession(defaultSessionTitle)])
    return
  }
  const nextSessions = chatSessions.value.filter((session) => session.id !== sessionId)
  chatSessions.value = nextSessions
  if (activeSessionId.value === sessionId) {
    activeSessionId.value = nextSessions[0]?.id || ''
  }
}

const updateSessionTitle = (session, question) => {
  if (!session) {
    return
  }
  if (session.title.startsWith(sessionTitlePrefix) || session.title === defaultSessionTitle) {
    session.title = question.slice(0, 12) || session.title
  }
}

const submitLogin = async () => {
  if (authLoading.value) {
    return
  }
  authLoading.value = true
  authMessage.value = ''
  try {
    const result = await doAuthRequest('/user/login', {
      phone: authForm.value.loginPhone.trim(),
      password: authForm.value.loginPassword
    })
    const latest = await fetchLatestProfile(result.data?.phone || authForm.value.loginPhone.trim())
    applyUser(latest || result.data)
    authMessage.value = '登录成功'
  } catch (error) {
    authMessage.value = error.message || '登录失败'
  } finally {
    authLoading.value = false
  }
}

const submitRegister = async () => {
  if (authLoading.value) {
    return
  }
  authLoading.value = true
  authMessage.value = ''
  try {
    const payload = {
      name: authForm.value.registerName.trim(),
      phone: authForm.value.registerPhone.trim(),
      password: authForm.value.registerPassword,
      age: authForm.value.registerAge ? Number(authForm.value.registerAge) : null,
      height: authForm.value.registerHeight ? Number(authForm.value.registerHeight) : null,
      weight: authForm.value.registerWeight ? Number(authForm.value.registerWeight) : null
    }
    const result = await doAuthRequest('/user/register', payload)
    const latest = await fetchLatestProfile(result.data?.phone || payload.phone)
    applyUser(latest || result.data)
    authMessage.value = '注册成功，已自动登录'
  } catch (error) {
    authMessage.value = error.message || '注册失败'
  } finally {
    authLoading.value = false
  }
}

const askPrompt = async (prompt) => {
  chatInput.value = prompt
  await submitQuestion()
}

const localDietAdvice = (question) => {
  const q = question.toLowerCase()
  if (q.includes('控糖') || q.includes('血糖')) {
    return '如果你在控糖，优先选择低 GI 主食、足量蛋白质和高纤维蔬菜，能帮助餐后更稳。'
  }
  if (q.includes('减脂') || q.includes('减肥')) {
    return '如果你在减脂，优先控制总热量，主食别过量，蛋白质和蔬菜尽量配齐。'
  }
  if (q.includes('晚餐') || q.includes('夜宵')) {
    return '晚餐建议偏清淡，优先高蛋白搭配蔬菜，少油少糖，主食按饥饿程度适量吃。'
  }
  return '可以先从规律三餐、控制总量、增加蛋白质和蔬菜比例开始，这样通常更容易执行。'
}

const submitQuestion = async () => {
  const question = chatInput.value.trim()
  if (!question || loading.value) {
    return
  }

  const session = activeSession.value
  if (!session) {
    return
  }

  if (!isAuthenticated.value) {
    session.messages.push({
      role: 'assistant',
      content: '请先登录后再开始提问，这样我才能结合你的个人资料、日常状态和技能偏好给你更准确的建议。'
    })
    session.updatedAt = Date.now()
    return
  }

  session.messages.push({ role: 'user', content: question })
  session.updatedAt = Date.now()
  updateSessionTitle(session, question)
  chatInput.value = ''
  loading.value = true

  try {
    const params = new URLSearchParams({
      message: question,
      memoryId: memoryId.value
    })
    const response = await fetch(`/chat?${params.toString()}`)
    if (!response.ok) {
      throw new Error('request_failed')
    }
    const answer = (await response.text()).trim()
    if (!answer) {
      throw new Error('empty_answer')
    }
    session.messages.push({ role: 'assistant', content: answer })
  } catch (error) {
    session.messages.push({
      role: 'assistant',
      content: `${localDietAdvice(question)} 当前服务暂时有点忙，你可以稍后再试。`
    })
  } finally {
    session.updatedAt = Date.now()
    loading.value = false
  }
}

loadSkillCatalog()
restoreAuth()
if (activeUser.value?.phone) {
  fetchLatestProfile(activeUser.value.phone).then((latest) => {
    if (latest) {
      applyUser(latest)
    }
  })
}
</script>

<template>
  <div class="page">
    <HeroSection
      :bmi="bmi"
      :hydration-progress="hydrationProgress"
      :sleep-hour="sleepHour"
      :activity-minute="activityMinute"
      :is-authenticated="isAuthenticated"
      :nickname="userProfile.nickname"
      @open-profile="openProfileModal"
      @open-daily="openDailyModal"
      @logout="logout"
    />

    <AuthPanel
      v-if="!isAuthenticated"
      :auth-mode="authMode"
      :auth-loading="authLoading"
      :auth-message="authMessage"
      :auth-form="authForm"
      @switch-mode="switchAuthMode"
      @submit-login="submitLogin"
      @submit-register="submitRegister"
    />

    <ProfileModal
      :visible="profileModalVisible"
      :saving="profileSaving"
      :message="profileMessage"
      :form="profileForm"
      @submit="submitProfileUpdate"
      @close="closeProfileModal"
    />

    <DailyModal
      :visible="dailyModalVisible"
      :saving="dailySaving"
      :message="dailyMessage"
      :form="dailyForm"
      @submit="submitDailyUpdate"
      @close="closeDailyModal"
    />

    <section class="dashboard">
      <SessionSidebar
        :sessions="chatSessions"
        :active-session-id="activeSessionId"
        :has-active-session="Boolean(activeSession)"
        @create-session="createSession"
        @delete-session="deleteSession(activeSessionId)"
        @switch-session="switchSession"
      />

      <ChatPanel
        v-model="chatInput"
        :active-session-title="activeSession?.title || ''"
        :quick-prompts="quickPrompts"
        :current-messages="currentMessages"
        :loading="loading"
        :is-authenticated="isAuthenticated"
        @ask-prompt="askPrompt"
        @submit-question="submitQuestion"
      />

      <ProfilePanel
        :is-authenticated="isAuthenticated"
        :user-profile="userProfile"
        :active-user-phone="activeUser?.phone || ''"
        :skills-loading="skillsLoading"
        :skill-catalog="skillCatalog"
        :selected-skill-ids="selectedSkillIds"
        :skills-saving="skillsSaving"
        :skills-message="skillsMessage"
        @toggle-skill="toggleSkillSelection"
        @save-skills="saveSkills"
      />
    </section>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20px 16px 28px;
  background: linear-gradient(180deg, #f4f8ff 0%, #f7fbf7 100%);
  color: #1f2937;
  overflow-x: hidden;
}

.page * {
  box-sizing: border-box;
}

.dashboard {
  max-width: 1160px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(220px, 280px) minmax(520px, 1fr) minmax(260px, 320px);
  gap: 16px;
  align-items: stretch;
}

@media (max-width: 1080px) {
  .dashboard {
    grid-template-columns: 1fr;
  }
}
</style>

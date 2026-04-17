<script setup>
import { computed, ref } from 'vue'

const userProfile = ref({
  nickname: '知心饮食小助手',
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
const storageKey = 'consultant_user_auth'

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
  '我今天午餐怎么吃更控糖？',
  '帮我安排一周减脂早餐计划。',
  '晚上加班后吃什么不容易胖？',
  '有没有清淡又高蛋白的晚餐建议？'
]

const defaultMealSuggestions = [
  { time: '早餐', idea: '燕麦 + 鸡蛋 + 一份水果', principle: '高纤维搭配蛋白质，提升饱腹感' },
  { time: '午餐', idea: '半盘蔬菜 + 全谷主食 + 鱼/鸡胸', principle: '餐盘法控制热量与营养平衡' },
  { time: '晚餐', idea: '优质蛋白 + 深色蔬菜 + 少量主食', principle: '晚间降低精制碳水比例' },
  { time: '加餐', idea: '无糖酸奶或少量坚果', principle: '避免高糖零食引发暴食' }
]
const mealSuggestions = ref([...defaultMealSuggestions])

const skillNameMap = {
  fat_loss: '减脂教练',
  muscle_gain: '增肌教练',
  low_sodium: '低盐饮食',
  diabetes_friendly: '控糖友好',
  quick_meal: '快手餐'
}

const skillDescMap = {
  fat_loss: '优先推荐低热量、高饱腹感的饮食方案。',
  muscle_gain: '优先推荐高蛋白分配和训练恢复方案。',
  low_sodium: '优先推荐低钠饮食和减盐替代方案。',
  diabetes_friendly: '优先推荐低 GI 碳水与稳糖饮食策略。',
  quick_meal: '优先推荐 10-20 分钟可完成的简单餐食。'
}

const skillDisplayName = (skill) => skillNameMap[skill?.id] || skill?.name || ''
const skillDisplayDesc = (skill) => skillDescMap[skill?.id] || skill?.description || ''

const chatInput = ref('')
const loading = ref(false)
const messages = ref([
  {
    role: 'assistant',
    content: '你好，我是你的 AI 饮食顾问。告诉我你的目标和习惯，我会给你更容易执行的建议。'
  }
])

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

const hydrationTarget = computed(() => Math.round(userProfile.value.weightKg * 32))
const hydrationProgress = computed(() =>
  Math.min(100, Math.round((hydrationMl.value / hydrationTarget.value) * 100))
)
const memoryId = computed(() =>
  activeUser.value && activeUser.value.id ? `user-${activeUser.value.id}` : userProfile.value.nickname
)

const restoreAuth = () => {
  const saved = localStorage.getItem(storageKey)
  if (!saved) {
    return
  }
  try {
    const user = JSON.parse(saved)
    applyUser(user, false)
  } catch (error) {
    localStorage.removeItem(storageKey)
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
    fetchMealSuggestions(user.id)
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
  mealSuggestions.value = [...defaultMealSuggestions]
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
  hydrationMl.value = Number(data.hydrationMl) || hydrationMl.value
  sleepHour.value = Number(data.sleepHour) || sleepHour.value
  activityMinute.value = Number(data.activityMinute) || activityMinute.value
  return data
}

const fetchMealSuggestions = async (userId) => {
  if (!userId) {
    mealSuggestions.value = [...defaultMealSuggestions]
    return
  }
  try {
    const params = new URLSearchParams({ userId: String(userId) })
    const response = await fetch(`/user/diet/suggestions?${params.toString()}`)
    const body = await response.json().catch(() => ({}))
    if (!response.ok || !body.success) {
      throw new Error(body.message || '获取饮食建议失败')
    }
    const meals = body?.data?.meals
    if (Array.isArray(meals) && meals.length > 0) {
      mealSuggestions.value = meals
      return
    }
    mealSuggestions.value = [...defaultMealSuggestions]
  } catch (error) {
    mealSuggestions.value = [...defaultMealSuggestions]
  }
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
      throw new Error(body.message || '加载技能失败')
    }
    skillCatalog.value = Array.isArray(body.data) ? body.data : []
  } catch (error) {
    skillsMessage.value = error.message || '加载技能失败'
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
      throw new Error(body.message || '加载已选技能失败')
    }
    const selected = Array.isArray(body.data) ? body.data.map((item) => item.id).filter(Boolean) : []
    selectedSkillIds.value = selected
  } catch (error) {
    skillsMessage.value = error.message || '加载已选技能失败'
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
      throw new Error(body.message || '应用技能失败')
    }
    const selected = Array.isArray(body.data) ? body.data.map((item) => item.id).filter(Boolean) : []
    selectedSkillIds.value = selected
    skillsMessage.value = '技能已应用'
  } catch (error) {
    skillsMessage.value = error.message || '应用技能失败'
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
  if (!isAuthenticated.value || !activeUser.value?.id) {
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
  if (!isAuthenticated.value || !activeUser.value?.id || dailySaving.value) {
    return
  }
  dailySaving.value = true
  dailyMessage.value = ''
  try {
    const payload = {
      userId: activeUser.value.id,
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
    await fetchDailyStatus(activeUser.value.id)
    dailyMessage.value = '日常状态已更新'
    dailyModalVisible.value = false
  } catch (error) {
    dailyMessage.value = error.message || '更新日常状态失败'
  } finally {
    dailySaving.value = false
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
    authMessage.value = '注册成功并已自动登录'
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
    return '建议优先低 GI 主食，吃饭顺序按“蔬菜-蛋白质-主食”。'
  }
  if (q.includes('减脂') || q.includes('减肥')) {
    return '建议轻微热量缺口，保证充足蛋白，并减少高油高糖外卖。'
  }
  if (q.includes('增肌') || q.includes('蛋白')) {
    return '建议把蛋白平均分配到三餐，训练后补充蛋白与适量碳水。'
  }
  return '建议按“半盘蔬菜、四分之一蛋白、四分之一主食”搭配。'
}

const submitQuestion = async () => {
  const question = chatInput.value.trim()
  if (!question || loading.value) {
    return
  }
  if (!isAuthenticated.value) {
    messages.value.push({
      role: 'assistant',
      content: '请先登录后再进行智能咨询。'
    })
    return
  }

  messages.value.push({ role: 'user', content: question })
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
    messages.value.push({ role: 'assistant', content: answer })
  } catch (error) {
    messages.value.push({
      role: 'assistant',
      content: `${localDietAdvice(question)}（当前为离线建议模式）`
    })
  } finally {
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
    <section class="hero card">
      <div>
        <p class="tag">AI 饮食健康管理师</p>
        <h1>你的个人饮食与健康助手</h1>
        <p class="desc">结合你的目标与习惯，给出更可执行的营养建议。</p>
      </div>
      <div class="metrics">
        <div class="metric-item"><span>BMI</span><strong>{{ bmi }}</strong></div>
        <div class="metric-item"><span>饮水进度</span><strong>{{ hydrationProgress }}%</strong></div>
        <div class="metric-item"><span>睡眠</span><strong>{{ sleepHour }}h</strong></div>
        <div class="metric-item"><span>活动</span><strong>{{ activityMinute }}min</strong></div>
      </div>
      <div class="auth-state">
        <span>{{ isAuthenticated ? `已登录：${userProfile.nickname}` : '未登录' }}</span>
        <div v-if="isAuthenticated" class="auth-actions">
          <button type="button" @click="openProfileModal">修改个人信息</button>
          <button type="button" @click="openDailyModal">修改日常状态</button>
          <button type="button" @click="logout">退出登录</button>
        </div>
      </div>
    </section>

    <section v-if="!isAuthenticated" class="auth card">
      <h2>账号中心</h2>
      <div class="auth-tabs">
        <button type="button" :class="{ active: authMode === 'login' }" @click="switchAuthMode('login')">登录</button>
        <button type="button" :class="{ active: authMode === 'register' }" @click="switchAuthMode('register')">注册</button>
      </div>

      <form v-if="authMode === 'login'" class="auth-form" @submit.prevent="submitLogin">
        <input v-model="authForm.loginPhone" type="text" placeholder="手机号" />
        <input v-model="authForm.loginPassword" type="password" placeholder="密码" />
        <button type="submit" :disabled="authLoading">{{ authLoading ? '处理中...' : '登录' }}</button>
      </form>

      <form v-else class="auth-form" @submit.prevent="submitRegister">
        <input v-model="authForm.registerName" type="text" placeholder="姓名/昵称" />
        <input v-model="authForm.registerPhone" type="text" placeholder="手机号" />
        <input v-model="authForm.registerPassword" type="password" placeholder="密码（至少 6 位）" />
        <input v-model="authForm.registerAge" type="number" placeholder="年龄（选填）" />
        <input v-model="authForm.registerHeight" type="number" step="0.01" placeholder="身高（米或厘米）" />
        <input v-model="authForm.registerWeight" type="number" step="0.1" placeholder="体重（kg）" />
        <button type="submit" :disabled="authLoading">{{ authLoading ? '处理中...' : '注册并登录' }}</button>
      </form>

      <p v-if="authMessage" class="auth-message">{{ authMessage }}</p>
    </section>

    <section v-if="profileModalVisible" class="profile-modal-mask">
      <div class="profile-modal card">
        <h2>修改个人信息</h2>
        <form class="auth-form" @submit.prevent="submitProfileUpdate">
          <input v-model="profileForm.name" type="text" placeholder="姓名/昵称" />
          <input v-model="profileForm.age" type="number" placeholder="年龄" />
          <input v-model="profileForm.height" type="number" step="0.01" placeholder="身高（米或厘米）" />
          <input v-model="profileForm.weight" type="number" step="0.1" placeholder="体重（kg）" />
          <input v-model="profileForm.target" type="text" placeholder="管理目标" />
          <input v-model="profileForm.allergy" type="text" placeholder="饮食注意事项" />
          <button type="submit" :disabled="profileSaving">{{ profileSaving ? '保存中...' : '保存' }}</button>
          <button type="button" class="ghost-btn" :disabled="profileSaving" @click="closeProfileModal">取消</button>
        </form>
        <p v-if="profileMessage" class="auth-message">{{ profileMessage }}</p>
      </div>
    </section>

    <section v-if="dailyModalVisible" class="profile-modal-mask">
      <div class="profile-modal card">
        <h2>修改日常状态</h2>
        <form class="auth-form" @submit.prevent="submitDailyUpdate">
          <input v-model="dailyForm.hydrationMl" type="number" placeholder="饮水量（ml）" />
          <input v-model="dailyForm.sleepHour" type="number" step="0.1" placeholder="睡眠时长（小时）" />
          <input v-model="dailyForm.activityMinute" type="number" placeholder="运动时长（分钟）" />
          <button type="submit" :disabled="dailySaving">{{ dailySaving ? '保存中...' : '保存' }}</button>
          <button type="button" class="ghost-btn" :disabled="dailySaving" @click="closeDailyModal">取消</button>
        </form>
        <p v-if="dailyMessage" class="auth-message">{{ dailyMessage }}</p>
      </div>
    </section>

    <section class="dashboard">
      <article class="card profile">
        <h2>个人信息</h2>
        <p><span>昵称</span>{{ userProfile.nickname }}</p>
        <p><span>年龄</span>{{ userProfile.age }}</p>
        <p><span>身高/体重</span>{{ userProfile.heightCm }} cm / {{ userProfile.weightKg }} kg</p>
        <p><span>目标</span>{{ userProfile.target }}</p>
        <p><span>饮食注意</span>{{ userProfile.allergy }}</p>
        <p><span>手机号</span>{{ activeUser?.phone || '未登录' }}</p>

        <div class="skills-panel">
          <h3>技能偏好</h3>
          <p class="skills-tip">勾选后点击“应用技能”，后续问答会按技能偏好进行。</p>
          <div v-if="skillsLoading" class="skills-tip">技能加载中...</div>
          <div v-else class="skills-list">
            <label
              v-for="skill in skillCatalog"
              :key="skill.id"
              class="skill-item"
              :class="{ selected: selectedSkillIds.includes(skill.id) }"
            >
              <input
                type="checkbox"
                :checked="selectedSkillIds.includes(skill.id)"
                :disabled="!isAuthenticated"
                @change="toggleSkillSelection(skill.id)"
              />
              <div>
                <strong>{{ skillDisplayName(skill) }}</strong>
                <small>{{ skillDisplayDesc(skill) }}</small>
              </div>
            </label>
          </div>
          <button type="button" class="skills-save" :disabled="!isAuthenticated || skillsSaving" @click="saveSkills">
            {{ skillsSaving ? '保存中...' : '应用技能' }}
          </button>
          <p v-if="skillsMessage" class="skills-tip">{{ skillsMessage }}</p>
        </div>
      </article>

      <article class="card chat">
        <h2>智能咨询</h2>
        <div class="quick-list">
          <button v-for="prompt in quickPrompts" :key="prompt" @click="askPrompt(prompt)">{{ prompt }}</button>
        </div>

        <div class="chat-box">
          <div v-for="(message, index) in messages" :key="index" :class="['msg', message.role]">{{ message.content }}</div>
        </div>

        <form class="input-area" @submit.prevent="submitQuestion">
          <input
            v-model="chatInput"
            type="text"
            :placeholder="isAuthenticated ? '输入你的饮食问题' : '请先登录后提问'"
            :disabled="!isAuthenticated"
          />
          <button type="submit" :disabled="loading || !isAuthenticated">{{ loading ? '思考中...' : '发送' }}</button>
        </form>
      </article>

      <article class="card suggestions">
        <h2>今日饮食建议</h2>
        <div class="meal-list">
          <div v-for="meal in mealSuggestions" :key="meal.time" class="meal-item">
            <h3>{{ meal.time }}</h3>
            <p>{{ meal.idea }}</p>
            <small>{{ meal.principle }}</small>
          </div>
        </div>
      </article>
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

.card {
  background: rgba(255, 255, 255, 0.92);
  border-radius: 18px;
  border: 1px solid #e5edf5;
  box-shadow: 0 12px 28px rgba(15, 35, 70, 0.08);
}

.hero {
  max-width: 1160px;
  margin: 0 auto 18px;
  padding: 22px;
  display: grid;
  gap: 18px;
}

.auth-state {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 14px;
  color: #0f172a;
}

.auth-state button {
  border: 1px solid #d5e4f2;
  border-radius: 10px;
  background: #ffffff;
  color: #0f172a;
  padding: 8px 12px;
  cursor: pointer;
}

.auth-actions {
  display: flex;
  gap: 8px;
}

.auth {
  max-width: 1160px;
  margin: 0 auto 18px;
  padding: 20px;
}

.auth-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.auth-tabs button {
  border: 1px solid #d6e5f3;
  background: #f8fbff;
  color: #334155;
  border-radius: 10px;
  padding: 8px 14px;
  cursor: pointer;
}

.auth-tabs button.active {
  background: #0ea5a4;
  border-color: #0ea5a4;
  color: #ffffff;
}

.auth-form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}

.auth-form input {
  border: 1px solid #d8e5f3;
  border-radius: 10px;
  padding: 10px 12px;
  outline: none;
}

.auth-form button {
  border: none;
  border-radius: 10px;
  padding: 10px 12px;
  background: #0ea5a4;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.auth-form button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.auth-message {
  margin-top: 10px;
  color: #0f172a;
  font-size: 14px;
}

.profile-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  z-index: 20;
}

.profile-modal {
  width: min(720px, 100%);
  padding: 20px;
}

.ghost-btn {
  border: 1px solid #d6e5f3 !important;
  background: #ffffff !important;
  color: #334155 !important;
}

.tag {
  display: inline-block;
  margin-bottom: 10px;
  padding: 6px 12px;
  border-radius: 99px;
  font-size: 12px;
  font-weight: 600;
  color: #0f766e;
  background: #dff7f2;
}

h1 {
  font-size: clamp(28px, 4vw, 40px);
  line-height: 1.2;
  font-weight: 700;
  color: #0f172a;
}

.desc {
  margin-top: 10px;
  color: #475569;
  max-width: 720px;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
}

.metric-item {
  background: #f8fbff;
  border: 1px solid #e6eef8;
  border-radius: 14px;
  padding: 12px;
}

.metric-item span {
  display: block;
  font-size: 13px;
  color: #64748b;
}

.metric-item strong {
  font-size: 22px;
  color: #0f172a;
}

.dashboard {
  max-width: 1160px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(260px, 300px) minmax(560px, 1fr) minmax(220px, 300px);
  gap: 16px;
  align-items: stretch;
}

h2 {
  margin-bottom: 14px;
  font-size: 18px;
  font-weight: 700;
}

.profile,
.chat,
.suggestions {
  padding: 16px;
  display: flex;
  flex-direction: column;
  min-height: 620px;
  max-height: 620px;
}

.profile p {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 14px;
  gap: 8px;
}

.profile span {
  color: #64748b;
}

.skills-panel {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e5edf5;
}

.skills-panel h3 {
  margin: 0 0 8px;
  font-size: 15px;
}

.skills-tip {
  margin: 0 0 8px;
  font-size: 12px;
  color: #64748b;
}

.skills-list {
  display: grid;
  gap: 8px;
  margin-bottom: 10px;
  max-height: 250px;
  overflow-y: auto;
  padding-right: 2px;
}

.skill-item {
  display: grid;
  grid-template-columns: 18px 1fr;
  align-items: start;
  gap: 8px;
  padding: 8px;
  border: 1px solid #dfe9f5;
  border-radius: 10px;
  background: #f8fbff;
}

.skill-item.selected {
  border-color: #0ea5a4;
  background: #edfdfc;
}

.skill-item small {
  display: block;
  color: #64748b;
  line-height: 1.4;
}

.skills-save {
  width: 100%;
  border: none;
  border-radius: 10px;
  padding: 10px 12px;
  background: #0ea5a4;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.skills-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.quick-list {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
  max-height: 122px;
  overflow-y: auto;
  padding-right: 2px;
}

.quick-list button {
  border: 1px solid #dde7f3;
  background: #f7fbff;
  color: #1e293b;
  border-radius: 10px;
  padding: 8px 10px;
  text-align: left;
  cursor: pointer;
  font-size: 13px;
}

.chat-box {
  border: 1px solid #e5edf5;
  border-radius: 12px;
  background: #fbfdff;
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  margin-bottom: 12px;
}

.msg {
  max-width: 85%;
  margin-bottom: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  line-height: 1.55;
  white-space: pre-wrap;
}

.msg.assistant {
  background: #eef6ff;
  color: #0f172a;
}

.msg.user {
  margin-left: auto;
  background: #0ea5a4;
  color: #ffffff;
}

.input-area {
  display: flex;
  gap: 10px;
  margin-top: auto;
}

.input-area input {
  flex: 1;
  border: 1px solid #d8e5f3;
  border-radius: 10px;
  padding: 10px 12px;
  outline: none;
}

.input-area button {
  border: none;
  border-radius: 10px;
  padding: 0 16px;
  background: #0ea5a4;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.input-area button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.meal-list {
  display: grid;
  gap: 10px;
  overflow-y: auto;
  padding-right: 2px;
}

.meal-item {
  padding: 12px;
  background: #f9fcff;
  border-radius: 12px;
  border: 1px solid #e4edf8;
}

.meal-item h3 {
  font-size: 15px;
  margin-bottom: 4px;
}

.meal-item p {
  font-size: 14px;
  margin-bottom: 4px;
}

.meal-item small {
  color: #64748b;
}

@media (max-width: 1080px) {
  .dashboard {
    grid-template-columns: 1fr;
  }

  .profile,
  .chat,
  .suggestions {
    min-height: auto;
    max-height: none;
  }

  .quick-list,
  .skills-list,
  .meal-list {
    max-height: none;
  }

  .chat-box {
    min-height: 220px;
    flex: none;
  }
}
</style>
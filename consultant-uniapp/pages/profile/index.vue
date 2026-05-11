<script setup>
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SectionHeader from '@/components/SectionHeader.vue'
import { useChatStore } from '@/store/chat'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const chatStore = useChatStore()

const profileForm = reactive({
  name: userStore.profile?.name || '',
  age: userStore.profile?.age || '',
  height: userStore.profile?.height || '',
  weight: userStore.profile?.weight || ''
})

const dailyForm = reactive({
  hydrationMl: 1350,
  sleepHour: 7.2,
  activityMinute: 42
})

const selectedSkillIds = ref([])
const loading = ref(false)
const savingProfile = ref(false)
const savingDaily = ref(false)
const savingSkills = ref(false)

const skills = computed(() => userStore.skillCatalog)
const bmi = computed(() => {
  const bmiValue = Number(userStore.profile?.bmi)
  return Number.isFinite(bmiValue) && bmiValue > 0 ? bmiValue.toFixed(1) : '--'
})

const syncForms = () => {
  profileForm.name = userStore.profile?.name || ''
  profileForm.age = userStore.profile?.age ?? ''
  profileForm.height = userStore.profile?.height ?? ''
  profileForm.weight = userStore.profile?.weight ?? ''
  dailyForm.hydrationMl = userStore.daily?.hydrationMl ?? 1350
  dailyForm.sleepHour = userStore.daily?.sleepHour ?? 7.2
  dailyForm.activityMinute = userStore.daily?.activityMinute ?? 42
  selectedSkillIds.value = [...userStore.selectedSkillIds]
}

const toggleSkill = (skillId) => {
  if (selectedSkillIds.value.includes(skillId)) {
    selectedSkillIds.value = selectedSkillIds.value.filter((item) => item !== skillId)
    return
  }
  selectedSkillIds.value = [...selectedSkillIds.value, skillId]
}

const goLogin = () => {
  uni.navigateTo({
    url: '/pages/login/index'
  })
}

const normalizeHeightToMeter = (value) => {
  const num = Number(value)
  if (!Number.isFinite(num) || num <= 0) {
    return null
  }
  return num > 3 ? num / 100 : num
}

const loadProfileData = async () => {
  if (!userStore.isLoggedIn) {
    return
  }
  loading.value = true
  try {
    await userStore.bootstrap(true)
    syncForms()
  } catch (error) {
    uni.showToast({
      title: error.message || '资料加载失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

const submitProfile = async () => {
  if (!userStore.profile?.phone || savingProfile.value) {
    return
  }
  savingProfile.value = true
  try {
    await userStore.saveProfile({
      phone: userStore.profile.phone,
      name: `${profileForm.name || ''}`.trim(),
      age: profileForm.age === '' ? null : Number(profileForm.age),
      height: normalizeHeightToMeter(profileForm.height),
      weight: profileForm.weight === '' ? null : Number(profileForm.weight)
    })
    syncForms()
    uni.showToast({
      title: '个人资料已保存',
      icon: 'success'
    })
  } catch (error) {
    uni.showToast({
      title: error.message || '保存失败',
      icon: 'none'
    })
  } finally {
    savingProfile.value = false
  }
}

const submitDaily = async () => {
  if (!userStore.userId || savingDaily.value) {
    return
  }
  savingDaily.value = true
  try {
    await userStore.saveDaily({
      userId: userStore.userId,
      hydrationMl: Number(dailyForm.hydrationMl) || 0,
      sleepHour: Number(dailyForm.sleepHour) || 0,
      activityMinute: Number(dailyForm.activityMinute) || 0
    })
    syncForms()
    uni.showToast({
      title: '日常状态已保存',
      icon: 'success'
    })
  } catch (error) {
    uni.showToast({
      title: error.message || '保存失败',
      icon: 'none'
    })
  } finally {
    savingDaily.value = false
  }
}

const submitSkills = async () => {
  if (!userStore.userId || savingSkills.value) {
    return
  }
  savingSkills.value = true
  try {
    await userStore.saveSkills(selectedSkillIds.value)
    syncForms()
    uni.showToast({
      title: '技能偏好已保存',
      icon: 'success'
    })
  } catch (error) {
    uni.showToast({
      title: error.message || '保存失败',
      icon: 'none'
    })
  } finally {
    savingSkills.value = false
  }
}

const logout = () => {
  userStore.clearProfile()
  chatStore.resetToGuest()
  selectedSkillIds.value = []
  uni.showToast({
    title: '已退出登录',
    icon: 'success'
  })
}

onShow(() => {
  loadProfileData()
})
</script>

<template>
  <view class="page-shell profile-page">
    <view class="section-card profile-hero">
      <SectionHeader
        title="我的"
        :subtitle="userStore.isLoggedIn ? '这里已经接上个人资料、每日状态和技能偏好接口。' : '当前未登录，先去登录后再同步你的资料。'"
      />
      <button v-if="!userStore.isLoggedIn" class="pill-btn login-btn" @tap="goLogin">去登录</button>
      <view v-else class="profile-summary">
        <text class="summary-name">{{ userStore.profile?.name || '已登录用户' }}</text>
        <text class="status-text">手机号：{{ userStore.profile?.phone || '-' }}</text>
        <text class="status-text">BMI：{{ bmi }}</text>
        <button class="ghost-btn logout-btn" @tap="logout">退出登录</button>
      </view>
    </view>

    <view v-if="userStore.isLoggedIn" class="section-card">
      <SectionHeader title="个人资料" subtitle="直接对接 `/user/profile` 和 `/user/profile/update`。" />
      <view class="form-grid">
        <view class="field-group">
          <text class="field-label">昵称</text>
          <input v-model="profileForm.name" class="field-input" placeholder="请输入昵称" />
        </view>
        <view class="field-group">
          <text class="field-label">年龄</text>
          <input v-model="profileForm.age" class="field-input" type="number" placeholder="请输入年龄" />
        </view>
        <view class="field-group">
          <text class="field-label">身高（m）</text>
          <input v-model="profileForm.height" class="field-input" type="digit" placeholder="请输入身高" />
        </view>
        <view class="field-group">
          <text class="field-label">体重（kg）</text>
          <input v-model="profileForm.weight" class="field-input" type="digit" placeholder="请输入体重" />
        </view>
      </view>
      <button class="pill-btn submit-btn" :loading="savingProfile || loading" @tap="submitProfile">保存个人资料</button>
    </view>

    <view v-if="userStore.isLoggedIn" class="section-card">
      <SectionHeader title="每日状态" subtitle="直接对接 `/user/daily` 和 `/user/daily/update`。" />
      <view class="form-grid">
        <view class="field-group">
          <text class="field-label">饮水量（ml）</text>
          <input v-model="dailyForm.hydrationMl" class="field-input" type="number" />
        </view>
        <view class="field-group">
          <text class="field-label">睡眠时长（h）</text>
          <input v-model="dailyForm.sleepHour" class="field-input" type="digit" />
        </view>
        <view class="field-group">
          <text class="field-label">活动时长（min）</text>
          <input v-model="dailyForm.activityMinute" class="field-input" type="number" />
        </view>
      </view>
      <button class="pill-btn submit-btn" :loading="savingDaily || loading" @tap="submitDaily">保存每日状态</button>
    </view>

    <view v-if="userStore.isLoggedIn" class="section-card">
      <SectionHeader title="技能偏好" subtitle="直接对接 `/skills/list`、`/skills/user`、`/skills/user/apply`。" />
      <view class="tag-list profile-tags">
        <view
          v-for="skill in skills"
          :key="skill.id"
          :class="['tag-chip', { active: selectedSkillIds.includes(skill.id) }]"
          @tap="toggleSkill(skill.id)"
        >
          <text>{{ skill.name }}</text>
        </view>
      </view>
      <button class="pill-btn submit-btn" :loading="savingSkills || loading" @tap="submitSkills">保存技能偏好</button>
    </view>
  </view>
</template>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.profile-hero {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
}

.login-btn {
  width: 100%;
}

.logout-btn {
  margin-top: 10rpx;
}

.profile-summary {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.summary-name {
  font-size: 34rpx;
  font-weight: 700;
}

.form-grid {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  margin-top: 24rpx;
}

.profile-tags {
  margin-top: 24rpx;
}

.submit-btn {
  width: 100%;
  margin-top: 24rpx;
}
</style>

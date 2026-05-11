import { defineStore } from 'pinia'
import { getDaily, getProfile, updateDaily, updateProfile } from '@/api/profile'
import { getSkillCatalog, getUserSkills, applyUserSkills } from '@/api/skills'
import { STORAGE_KEYS } from '@/config'
import { getStorage, removeStorage, setStorage } from '@/utils/storage'

export const useUserStore = defineStore('user', {
  state: () => ({
    profile: getStorage(STORAGE_KEYS.auth, null),
    daily: null,
    skillCatalog: [],
    selectedSkillIds: [],
    initialized: false
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.profile?.id),
    userId: (state) => state.profile?.id || null
  },
  actions: {
    setProfile(profile) {
      this.profile = profile
      setStorage(STORAGE_KEYS.auth, profile)
    },
    clearProfile() {
      this.profile = null
      this.daily = null
      this.selectedSkillIds = []
      this.initialized = false
      removeStorage(STORAGE_KEYS.auth)
    },
    async refreshProfile() {
      if (!this.profile?.phone) {
        return null
      }
      const latest = await getProfile(this.profile.phone)
      this.setProfile(latest)
      return latest
    },
    async loadDaily() {
      if (!this.userId) {
        this.daily = null
        return null
      }
      const daily = await getDaily(this.userId)
      this.daily = daily
      return daily
    },
    async loadSkillCatalog(force = false) {
      if (this.skillCatalog.length && !force) {
        return this.skillCatalog
      }
      const skills = await getSkillCatalog()
      this.skillCatalog = Array.isArray(skills) ? skills : []
      return this.skillCatalog
    },
    async loadUserSkills() {
      if (!this.userId) {
        this.selectedSkillIds = []
        return []
      }
      const selected = await getUserSkills(this.userId)
      this.selectedSkillIds = Array.isArray(selected) ? selected.map((item) => item.id).filter(Boolean) : []
      return this.selectedSkillIds
    },
    async bootstrap(force = false) {
      if (!this.isLoggedIn) {
        this.initialized = true
        return
      }
      if (this.initialized && !force) {
        return
      }
      await Promise.all([this.refreshProfile(), this.loadDaily(), this.loadSkillCatalog(), this.loadUserSkills()])
      this.initialized = true
    },
    async saveProfile(payload) {
      const profile = await updateProfile(payload)
      this.setProfile(profile)
      return profile
    },
    async saveDaily(payload) {
      const daily = await updateDaily(payload)
      this.daily = daily
      return daily
    },
    async saveSkills(skillIds) {
      if (!this.userId) {
        throw new Error('请先登录')
      }
      const selected = await applyUserSkills({
        userId: this.userId,
        skillIds
      })
      this.selectedSkillIds = Array.isArray(selected) ? selected.map((item) => item.id).filter(Boolean) : []
      return this.selectedSkillIds
    }
  }
})

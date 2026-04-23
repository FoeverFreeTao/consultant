<script setup>
defineProps({
  isAuthenticated: { type: Boolean, default: false },
  userProfile: {
    type: Object,
    required: true
  },
  activeUserPhone: { type: String, default: '' },
  skillsLoading: { type: Boolean, default: false },
  skillCatalog: {
    type: Array,
    default: () => []
  },
  selectedSkillIds: {
    type: Array,
    default: () => []
  },
  skillsSaving: { type: Boolean, default: false },
  skillsMessage: { type: String, default: '' }
})

defineEmits(['toggle-skill', 'save-skills'])

const hasSkill = (selected, id) => selected.includes(id)
</script>

<template>
  <aside class="panel profile-panel">
    <div class="section-header">
      <h2>个人信息</h2>
      <span class="muted">{{ activeUserPhone ? '已同步账号信息' : '未登录' }}</span>
    </div>

    <dl class="profile-list">
      <div class="profile-row">
        <dt>昵称</dt>
        <dd>{{ userProfile.nickname }}</dd>
      </div>
      <div class="profile-row">
        <dt>年龄</dt>
        <dd>{{ userProfile.age }}</dd>
      </div>
      <div class="profile-row">
        <dt>身高/体重</dt>
        <dd>{{ userProfile.heightCm }} cm / {{ userProfile.weightKg }} kg</dd>
      </div>
      <div class="profile-row">
        <dt>目标</dt>
        <dd>{{ userProfile.target }}</dd>
      </div>
      <div class="profile-row">
        <dt>饮食注意</dt>
        <dd>{{ userProfile.allergy }}</dd>
      </div>
      <div class="profile-row">
        <dt>手机号</dt>
        <dd>{{ activeUserPhone || '--' }}</dd>
      </div>
    </dl>

    <div class="skills-area">
      <div class="section-header">
        <h3>技能偏好</h3>
      </div>
      <p class="muted tip">勾选后点击“应用技能”，后续问答会按技能偏好进行。</p>

      <div class="skill-list">
        <label
          v-for="skill in skillCatalog"
          :key="skill.id"
          class="skill-card"
        >
          <input
            type="checkbox"
            :checked="hasSkill(selectedSkillIds, skill.id)"
            :disabled="!isAuthenticated"
            @change="$emit('toggle-skill', skill.id)"
          />
          <div>
            <strong>{{ skill.name }}</strong>
            <p>{{ skill.description }}</p>
          </div>
        </label>
      </div>

      <button
        type="button"
        class="primary-btn"
        :disabled="!isAuthenticated || skillsLoading || skillsSaving"
        @click="$emit('save-skills')"
      >
        {{ skillsSaving ? '保存中...' : '应用技能' }}
      </button>

      <p v-if="skillsMessage" class="muted">{{ skillsMessage }}</p>
    </div>
  </aside>
</template>

<style scoped>
.panel {
  min-height: 100%;
  padding: 18px 18px 16px;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 18px 60px rgba(54, 94, 135, 0.08);
}

.profile-panel {
  display: grid;
  gap: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-header h2,
.section-header h3 {
  margin: 0;
  color: #0f172a;
}

.muted {
  color: #64748b;
  font-size: 14px;
}

.tip {
  margin: -10px 0 0;
}

.profile-list {
  margin: 0;
  display: grid;
  gap: 14px;
}

.profile-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebf1fb;
}

.profile-row dt {
  color: #64748b;
}

.profile-row dd {
  margin: 0;
  text-align: right;
  color: #1e293b;
}

.skills-area {
  display: grid;
  gap: 14px;
}

.skill-list {
  display: grid;
  gap: 12px;
  max-height: 360px;
  overflow: auto;
}

.skill-card {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 12px;
  padding: 14px;
  border: 1px solid #dbe7fb;
  border-radius: 18px;
  background: #f8fbff;
}

.skill-card strong {
  color: #1e293b;
}

.skill-card p {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.6;
}

.primary-btn {
  border: 0;
  border-radius: 16px;
  padding: 14px 18px;
  background: #13b4b1;
  color: #fff;
  font-size: 15px;
  cursor: pointer;
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>

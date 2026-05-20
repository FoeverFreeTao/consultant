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
      <span :class="['sync-badge', { synced: activeUserPhone }]">
        {{ activeUserPhone ? '已同步' : '未登录' }}
      </span>
    </div>

    <dl class="profile-list">
      <div class="profile-row">
        <dt>👤 昵称</dt>
        <dd>{{ userProfile.nickname }}</dd>
      </div>
      <div class="profile-row">
        <dt>🎂 年龄</dt>
        <dd>{{ userProfile.age }}</dd>
      </div>
      <div class="profile-row">
        <dt>📏 身高/体重</dt>
        <dd>{{ userProfile.heightCm }} cm / {{ userProfile.weightKg }} kg</dd>
      </div>
      <div class="profile-row">
        <dt>🎯 我的目标</dt>
        <dd class="target-val">{{ userProfile.target }}</dd>
      </div>
      <div class="profile-row">
        <dt>⚠️ 饮食注意</dt>
        <dd>{{ userProfile.allergy }}</dd>
      </div>
      <div class="profile-row">
        <dt>📱 手机号</dt>
        <dd>{{ activeUserPhone || '--' }}</dd>
      </div>
    </dl>

    <div class="skills-area">
      <div class="section-header">
        <h3>今日偏好</h3>
      </div>
      <p class="muted tip">勾选后点击"应用技能"，后续问答会按技能偏好进行。</p>

      <div class="skill-list">
        <label
          v-for="skill in skillCatalog"
          :key="skill.id"
          :class="['skill-card', { selected: hasSkill(selectedSkillIds, skill.id) }]"
        >
          <input
            type="checkbox"
            :checked="hasSkill(selectedSkillIds, skill.id)"
            :disabled="!isAuthenticated"
            @change="$emit('toggle-skill', skill.id)"
          />
          <div class="skill-body">
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
        {{ skillsSaving ? '保存中...' : '✨ 应用技能' }}
      </button>

      <p v-if="skillsMessage" class="skills-feedback">{{ skillsMessage }}</p>
    </div>
  </aside>
</template>

<style scoped>
.panel {
  min-height: 100%;
  padding: var(--space-5);
  border-radius: var(--radius-xl);
  background: var(--surface-0);
  border: 1px solid var(--surface-border);
  box-shadow: var(--shadow-lg);
}

.profile-panel {
  display: grid;
  gap: var(--space-5);
  align-content: start;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.section-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 17px;
  font-weight: 700;
}

.section-header h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 700;
}

.sync-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  background: var(--surface-100);
  color: var(--text-tertiary);
}

.sync-badge.synced {
  background: var(--brand-50);
  color: var(--brand-500);
}

.muted {
  color: var(--text-tertiary);
  font-size: 13px;
}

.tip {
  margin: -6px 0 0;
  line-height: 1.5;
}

/* ─── Profile list ─── */
.profile-list {
  margin: 0;
  display: grid;
  gap: 0;
}

.profile-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-3) var(--space-2);
  border-bottom: 1px solid var(--surface-border);
  transition: background-color var(--duration-fast) var(--ease-out-quart);
}

.profile-row:nth-child(even) {
  background: var(--surface-50);
}

.profile-row:last-child {
  border-bottom: 0;
}

.profile-row:hover {
  background: var(--brand-50);
}

.profile-row dt {
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
}

.profile-row dd {
  margin: 0;
  text-align: right;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
}

.target-val {
  color: var(--brand-500);
}

/* ─── Skills ─── */
.skills-area {
  display: grid;
  gap: var(--space-3);
}

.skill-list {
  display: grid;
  gap: var(--space-2);
  max-height: 320px;
  overflow: auto;
}

.skill-card {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: var(--space-3);
  padding: var(--space-3);
  border: 1px solid var(--surface-border);
  border-radius: var(--radius-md);
  background: var(--surface-50);
  cursor: pointer;
  transition:
    border-color var(--duration-fast) var(--ease-out-quart),
    background-color var(--duration-fast) var(--ease-out-quart),
    box-shadow var(--duration-fast) var(--ease-out-quart);
}

.skill-card:hover {
  border-color: var(--brand-300);
  background: var(--brand-50);
}

.skill-card.selected {
  border-color: var(--brand-400);
  background: var(--brand-50);
  box-shadow: 0 0 0 3px oklch(55% 0.14 176 / 0.08);
}

.skill-card input[type="checkbox"] {
  width: 18px;
  height: 18px;
  margin-top: 2px;
  accent-color: var(--brand-500);
  cursor: pointer;
}

.skill-body {
  min-width: 0;
}

.skill-card strong {
  color: var(--text-primary);
  font-size: 14px;
}

.skill-card p {
  margin: 4px 0 0;
  color: var(--text-tertiary);
  font-size: 13px;
  line-height: 1.5;
}

.primary-btn {
  border: 0;
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-5);
  background: linear-gradient(135deg, var(--brand-500), var(--brand-400));
  color: var(--surface-0);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 6px 20px oklch(48% 0.14 176 / 0.22);
  transition:
    transform var(--duration-fast) var(--ease-out-quart),
    box-shadow var(--duration-fast) var(--ease-out-quart);
}

.primary-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 28px oklch(48% 0.14 176 / 0.3);
}

.primary-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.skills-feedback {
  margin: 0;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  background: var(--brand-50);
  color: var(--brand-600);
  font-size: 13px;
  font-weight: 500;
}
</style>

# 知心饮食小助手 (Diet Helper) - 前端门户

基于 Vue 3 + Vite 的智能健康管理前端应用，集成了 AI 咨询、健康监测与用户画像管理。

## 技术栈

- **框架**: Vue 3.5+ (Composition API + `<script setup>`)
- **构建**: Vite 8.0+
- **网络**: 原生 `fetch` (异步请求与流式交互)
- **样式**: CSS 变量驱动的响应式布局
- **特性**: AI 聊天窗口、实时 BMI 计算、每日健康打卡仪表盘

---

## 快速开始

### 1. 安装依赖
```bash
npm install
```

### 2. 启动开发服务器
```bash
npm run dev
```
前端默认运行在 `http://localhost:5173/`，通过 `vite.config.js` 配置代理请求至后端 `http://localhost:8087/`。

---

## 项目结构

```text
├── src/
│   ├── assets/              # 静态资源与全局样式 (base.css, main.css)
│   ├── components/          # Vue 组件
│   │   └── icons/           # SVG 图标组件
│   ├── App.vue              # 核心应用入口
│   │   ├── Authentication   # 登录/注册逻辑
│   │   ├── AI Consultant    # AI 聊天交互
│   │   ├── Health Dashboard # BMI/饮水/睡眠/运动监测
│   │   └── Meal Suggestions # 膳食配比卡片
│   └── main.js              # 应用初始化
├── index.html               # 页面骨架
└── vite.config.js           # 代理与构建配置
```

---

## 功能特性

- **AI 对话交互**: 支持与后端 AI 模型进行多轮对话，获取个性化饮食建议。
- **健康监测仪表盘**:
    - **BMI 指数**: 自动计算并显示体型分类。
    - **饮水追踪**: 进度条实时展示今日摄入量。
    - **睡眠与运动**: 卡片式记录当日作息。
- **结构化膳食推荐**: 根据不同餐次（早/中/晚/加餐）展示科学搭配建议。
- **用户中心**: 全面维护个人身体指标、过敏源及健康目标。

---

## 代理配置

在 `vite.config.js` 中，通过以下配置将 API 请求转发至后端：
```javascript
server: {
  proxy: {
    '/user': 'http://localhost:8087',
    '/chat': 'http://localhost:8087',
    '/auth': 'http://localhost:8087'
  }
}
```

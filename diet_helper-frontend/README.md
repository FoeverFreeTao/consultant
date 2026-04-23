# diet_helper-frontend

这是项目的 Vue 3 前端门户，负责承载用户登录、会话管理、智能咨询、个人信息展示和技能偏好选择。

## 当前页面能力

- 左侧会话列表
  - 支持新增会话
  - 支持删除会话
  - 支持切换会话
- 中间智能咨询区
  - 展示快捷提问
  - 展示当前会话消息
  - 调用后端 `/chat`
- 右侧个人信息区
  - 展示昵称、年龄、身高体重、目标、饮食注意事项、手机号
  - 展示技能偏好列表
  - 支持应用技能偏好
- 顶部状态区
  - 展示 BMI、饮水进度、睡眠、活动
  - 支持修改个人信息和日常状态

## 技术栈

- Vue 3
- Vite
- Composition API
- 原生 `fetch`
- 单文件组件 `App.vue`

## 启动方式

```bash
npm install
npm run dev
```

默认开发地址通常为：
- `http://localhost:5173`

开发环境下通过 `vite.config.js` 把请求代理到后端：
- `http://localhost:8087`

## 目录说明

```text
diet_helper-frontend/
├─ src/
│  ├─ assets/
│  ├─ App.vue
│  ├─ main.js
│  └─ style.css
├─ public/
├─ index.html
├─ vite.config.js
└─ package.json
```

## 前端与后端交互

### 用户相关
- `POST /user/login`
- `POST /user/register`
- `GET /user/profile`
- `POST /user/profile/update`
- `GET /user/daily`
- `POST /user/daily/update`

### 会话相关
- `GET /chat/sessions`
- `GET /chat/sessions/messages`
- `POST /chat/sessions/create`
- `POST /chat/sessions/delete`

### 技能相关
- `GET /skills/list`
- `GET /skills/user`
- `POST /skills/user/apply`

### 聊天相关
- `GET /chat`

## 会话存储逻辑

### 登录用户
- 会话目录从后端读取
- 会话历史从 Redis 读取
- 当前会话通过 `memoryId` 和后端聊天记忆绑定

### 未登录用户
- 会话列表保存在浏览器 `localStorage`
- 仅作为本地临时体验使用

## 注意事项

### 1. 当前前端主逻辑集中在 `App.vue`
如果后续功能继续增多，建议逐步拆分：
- 会话列表组件
- 聊天面板组件
- 用户资料组件
- 技能选择组件

### 2. 构建报 `spawn EPERM`
如果 `npm run build` 在当前 Windows 环境报 `spawn EPERM`，更像是本地环境权限或工具链问题，不一定是业务代码本身有语法错误。

### 3. 中文乱码问题
如果页面显示正常，但终端输出乱码，通常是控制台编码问题；如果页面本身也乱码，则需要检查源码文件编码是否为 UTF-8。

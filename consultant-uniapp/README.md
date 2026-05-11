# consultant-uniapp

这是为当前 `consultant` 后端单独新建的 uni-app 前端骨架，原有的 `diet_helper-frontend` 不会被修改。

## 已完成

- 新建 uni-app 工程目录
- 预置 `App / 微信小程序` 双端基础配置
- 搭好页面骨架：
  - `pages/chat/index`
  - `pages/session/index`
  - `pages/profile/index`
  - `pages/login/index`
- 抽出统一请求层 `src/utils/request.js`
- 抽出接口模块 `src/api/*`
- 使用 `pinia` 预留用户态和会话态

## 启动前需要安装

```bash
npm install
```

## 常用命令

```bash
npm run dev:h5
npm run dev:mp-weixin
```

## 你接下来最可能要改的地方

1. 修改 `src/config/index.js` 里的 `API_BASE_URL`
2. 按后端真实返回结构微调 `src/api/*`
3. 把聊天、会话、资料、技能从占位数据改成真实接口联动
4. 在微信小程序后台配置合法请求域名

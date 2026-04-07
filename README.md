# 知心饮食小助手 (Diet Helper) V1.0.0

AI 驱动的个人饮食与健康管理系统，旨在为用户提供科学、便捷的膳食建议与生活方式指导。

## 技术栈

**前端 (Frontend)**
- **框架**: Vue 3 + Vite
- **开发模式**: Composition API (`<script setup>`)
- **样式**: 原生 CSS 布局（Flexbox/Grid）
- **交互**: 基于 `fetch` 的 RESTful API 通信

**后端 (Backend)**
- **框架**: Spring Boot 3
- **AI 集成**: LangChain4j (支持 Agent 与 Tool 调用)
- **大模型**: 通义千问 (Qwen-Plus) via DashScope
- **持久层**: MyBatis Plus
- **存储**: MySQL 8.0 + Redis (用于 Chat Memory 会话存储)

---

## 快速开始

### 1. 环境准备
- JDK 17+
- Node.js 18+
- MySQL 8.0
- Redis

### 2. 数据库配置
- 导入 `/src/main/resources/sql/diet.sql` 到 MySQL 数据库。
- 修改 `/src/main/resources/application.yml` 中的 `datasource`、`redis` 以及 `langchain4j.open-ai.api-key`。

### 3. 启动服务
```bash
# 启动后端
# 在 IDE 中运行 ConsultantApplication.java 或执行:
mvn spring-boot:run

# 启动前端
cd diet_helper-frontend
npm install
npm run dev
```

---

## 项目结构

```text
├── diet_helper-frontend/    # 前端 Vue 项目
│   ├── src/
│   │   ├── components/      # 通用组件与图标
│   │   ├── App.vue          # 主应用逻辑（含聊天、状态追踪、资料管理）
│   │   └── main.js          # 入口文件
│   └── vite.config.js       # Vite 配置
├── src/                     # 后端 Spring Boot 项目
│   ├── main/java/com/zyt/consultant/
│   │   ├── aiservice/       # AI 核心逻辑与 Service 接口
│   │   ├── controller/      # API 控制层（Chat/User/Daily）
│   │   ├── tools/           # AI Agent 工具集（核心算法引擎）
│   │   ├── entity/          # 数据库实体类
│   │   └── mapper/          # MyBatis Mapper 接口
│   └── main/resources/
│       ├── sql/             # 数据库初始化脚本
│       └── application.yml  # 全局配置文件
└── pom.xml                  # Maven 依赖管理
```

---

## 核心算法与 AI 工具

### 1. 健康状态评估引擎 `DailyStatusHealthTool`
根据用户的每日数据进行多维度加权评估：
- **饮水评估**: 1500-2500ml 为佳，过少或过多均给出针对性建议。
- **睡眠评估**: 7-9 小时为达标，分析作息节律。
- **运动评估**: 30-120 分钟中等强度活动为健康。
- **综合评分**: 输出「良好」、「一般」或「需改善」的状态报告。

### 2. 膳食推荐引擎 `DietRecommendationTool`
基于 BMI (Body Mass Index) 的结构化建议：
- **体型判定**: 自动计算 BMI 并划分为「偏瘦」、「正常」、「肥胖」。
- **三餐配比**: 根据体型从数据库动态匹配早、中、晚三餐的科学食谱。
- **营养干预**: 针对不同目标（减脂/增肌/控糖）进行 AI 辅助调整。

### 3. AI 智能对话系统
- **上下文感知**: 利用 Redis 存储 Chat Memory，实现多轮对话。
- **Agent 模式**: AI 能够识别用户意图并**自动调用**后端 Tool 进行数据查询与分析。
- **实时建议**: 结合用户画像（过敏源、偏好）提供个性化回复。

### 3. 知识库增强 (RAG) `Easy RAG`
- **文档解析**: 自动解析 `/src/main/resources/content/` 目录下的健康与营养文档（如：各种水果营养成分和功效.pdf）。
- **向量检索**: 利用 LangChain4j 的 Easy RAG 能力，将非结构化知识转化为向量存储，实现基于文档的精准问答。
- **背景增强**: AI 在回答饮食疑问时，会优先检索本地知识库，确保建议的权威性与准确性。

---

## API 接口概览

| 模块 | 端点 | 说明 |
|------|------|------|
| **AI 对话** | `POST /chat/send` | 发送消息并获取 AI 流式/同步回复 |
| **认证** | `POST /user/login` | 用户登录（手机号+密码） |
| **认证** | `POST /user/register` | 新用户注册 |
| **资料** | `GET /user/profile` | 获取用户身高、体重、BMI 等详细画像 |
| **追踪** | `GET /user/daily` | 获取今日饮水、睡眠、运动进度 |
| **同步** | `POST /user/updateDaily` | 更新每日健康追踪数据 |

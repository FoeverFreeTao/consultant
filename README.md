# 知心饮食与健康助手

一个基于 Spring Boot、LangChain4j、Vue 3、Redis 和 pgvector 的 AI 饮食与健康助手项目。

当前这版项目已经落地的核心能力包括：
- 用户注册、登录、个人资料维护、日常状态维护
- 多会话聊天，按用户维度把会话目录和聊天记忆存到 Redis
- 技能偏好选择，并从 `Markdown` 技能文件动态加载给 LLM 使用
- 基于 pgvector + 关键词召回的检索增强问答
- 服务端统一追加知识库“参考来源”
- 输入 Guardrail，拦截敏感词和提示注入类输入

## 功能概览

### 智能咨询
- 对话入口：`GET /chat`
- 支持基于 `memoryId` 的上下文记忆
- 支持技能偏好增强
- 支持知识库检索增强
- 如果命中知识库，服务端会在回答末尾统一追加“参考来源”

### 用户信息与日常状态
- 注册、登录
- 查询与修改个人资料
- 查询与修改日常状态：饮水、睡眠、活动时长

### 会话管理
- 每个会话都有独立 `sessionId`
- 会话目录存 Redis
- 会话消息存 Redis
- 支持新增、删除、切换会话
- 页面刷新后可恢复会话列表和历史记录

### 技能偏好
- 技能定义来自 `src/main/resources/skills/*.md`
- `name` / `description` 用于前端展示
- Markdown 正文用于生成 LLM 技能提示
- 当前内置技能：
  - 减脂教练
  - 增肌教练
  - 低盐饮食
  - 控糖友好
  - 快手餐食

### 检索增强问答
- 使用 pgvector 做向量召回
- 使用关键词检索做补充召回
- 通过混合检索合并结果
- 检索内容注入模型前会做清洗，避免原始 JSON、字段名、注入提示直接暴露给用户

### 输入安全拦截
- 基于 LangChain4j Input Guardrail
- 支持敏感词过滤
- 支持提示注入 / 越狱表达检测
- 当前只保留输入侧 Guardrail，输出侧仍走服务端清洗和参考来源追加逻辑

## 技术栈

### 后端
- Java 17
- Spring Boot 3.5
- LangChain4j
- MyBatis Plus
- MySQL
- Redis
- PostgreSQL + pgvector
- Micrometer + Prometheus

### 前端
- Vue 3
- Vite
- 原生 `fetch`

## 项目结构

```text
consultant/
├─ src/main/java/com/zyt/consultant
│  ├─ aiservice/          # LLM 服务接口
│  ├─ config/             # Spring、Redis、pgvector、LangChain4j 配置
│  ├─ controller/         # HTTP 接口
│  ├─ guardrail/          # 输入 Guardrail 实现
│  ├─ mapper/             # MyBatis Mapper
│  ├─ metrics/            # 业务指标埋点
│  ├─ rag/                # 混合检索与来源上下文
│  ├─ repository/         # Redis 聊天记忆存储
│  ├─ service/            # 用户、会话、技能等业务服务
│  ├─ tools/              # LLM 工具调用实现
│  └─ Visualizer/         # 可视化相关工具
├─ src/main/resources
│  ├─ content/            # 知识库原始内容
│  ├─ skills/             # 技能 Markdown 定义
│  ├─ sql/                # 初始化脚本
│  ├─ System.txt          # 系统提示词
│  └─ application.yml     # 主配置文件
├─ diet_helper-frontend/  # Vue 前端工程
└─ pom.xml
```

## 数据存储说明

### MySQL
用于保存业务数据，例如：
- 用户基础信息
- 日常状态等业务表

初始化脚本：
- `src/main/resources/sql/diet.sql`

### Redis
用于保存聊天相关数据。

1. 会话目录
- `chat:sessions:user:{userId}:meta`
- `chat:sessions:user:{userId}:order`

2. 会话消息
- `chat:memory:user:{userId}:session:{sessionId}`

### PostgreSQL + pgvector
用于保存知识库向量数据，支撑检索增强问答。

## 当前接口

### 对话接口
- `GET /chat`
  - 参数：`memoryId`、`message`、`skillIds`（可选）

### 用户接口
- `POST /user/register`
- `POST /user/login`
- `GET /user/profile`
- `POST /user/profile/update`
- `GET /user/daily`
- `POST /user/daily/update`

### 会话接口
- `GET /chat/sessions`
- `GET /chat/sessions/messages`
- `POST /chat/sessions/create`
- `POST /chat/sessions/delete`

### 技能接口
- `GET /skills/list`
- `GET /skills/user`
- `POST /skills/user/apply`

## 前端页面结构

当前页面是三栏布局：
- 左侧：会话列表，支持新增和删除会话
- 中间：智能咨询区，显示快捷问题、历史消息和输入框
- 右侧：个人信息与技能偏好

前端视图已经拆成组件：
- `HeroSection.vue`
- `AuthPanel.vue`
- `SessionSidebar.vue`
- `ChatPanel.vue`
- `ProfilePanel.vue`
- `ProfileModal.vue`
- `DailyModal.vue`

主状态和接口调用目前仍集中在：
- `diet_helper-frontend/src/App.vue`

## 技能 Markdown 规范

技能文件位于：
- `src/main/resources/skills/`

每个技能一个 Markdown 文件，结构示例：

```md
---
id: fat_loss
name: 减脂教练
description: 优先推荐低热量、高饱腹感、便于坚持的饮食方案。
---

Prioritize a fat-loss coaching style.

- Recommend lower-calorie and higher-satiety meal options first.
- Keep advice practical and easy to follow.
```

字段说明：
- `id`：技能唯一标识
- `name`：前端展示名称
- `description`：前端展示说明
- 正文：提供给 LLM 的技能提示内容

## 输入 Guardrail 说明

当前只保留输入侧 Guardrail，相关代码位于：
- `src/main/java/com/zyt/consultant/guardrail/ChatInputGuardrailService.java`
- `src/main/java/com/zyt/consultant/guardrail/SensitiveKeywordInputGuardrail.java`
- `src/main/java/com/zyt/consultant/guardrail/PromptInjectionInputGuardrail.java`
- `src/main/java/com/zyt/consultant/guardrail/ChatSafetyProperties.java`

当前能力：
- 拦截敏感词
- 拦截典型 prompt injection / jailbreak 表达
- Guardrail 自身执行异常时不阻断正常消息，避免误拦截

配置位于：
- `src/main/resources/application.yml`

## 检索与参考来源说明

当前问答链路包含这些保护：
- 检索内容在注入模型前会做清洗
- 不再把原始 JSON、字段名、提示注入文本直接暴露给用户
- “参考来源”由服务端根据检索 metadata 统一追加
- 历史消息返回前也会做清洗，避免刷新后出现内部检索上下文

## 监控

项目已集成：
- Spring Boot Actuator
- Prometheus 指标

常见端点：
- `/actuator/health`
- `/actuator/metrics`
- `/actuator/prometheus`

## 快速启动

### 1. 环境准备
- JDK 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8+
- Redis 6+
- PostgreSQL 15+，并安装 pgvector 扩展

### 2. 初始化数据库
导入脚本：
- `src/main/resources/sql/diet.sql`

### 3. 配置 `application.yml`
重点确认：
- `spring.datasource.*`
- `spring.data.redis.*`
- `app.vector-store.pgvector.*`
- `langchain4j.open-ai.*`
- `app.guardrails.input.*`

说明：
- 当前模型配置走 DashScope 兼容 OpenAI 接口
- `api-key` 建议放到本地环境变量中

### 4. 启动后端
```bash
mvn spring-boot:run
```

默认端口：`8087`

### 5. 启动前端
```bash
cd diet_helper-frontend
npm install
npm run dev
```
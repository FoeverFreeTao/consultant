# 知心饮食小助手（Diet Helper）V1.0

面向日常健康管理的 AI 饮食助手，提供用户画像管理、每日状态追踪、智能对话、膳食推荐与营养可视化能力。

## 项目目标

- 通过 AI 对话降低普通用户获取专业饮食建议的门槛
- 结合 BMI、饮水、睡眠、运动等数据给出个性化反馈
- 支持多轮会话记忆，形成连续性的健康管理体验
- 支持基于营养维度的热力图可视化，直观展示食物差异

---

## 技术栈

**后端**
- Spring Boot 3.5
- LangChain4j（AiService + Tools + ContentRetriever）
- MyBatis Plus / MyBatis Spring Boot Starter
- Redis（会话记忆 + 向量相关存储）
- Redisson
- Micrometer + Prometheus（业务指标）
- JFreeChart（热力图可视化）

**前端**
- Vue 3 + Vite
- Composition API
- fetch + 流式响应消费

**数据层**
- MySQL 8.0（用户、每日状态、膳食基础数据）
- Redis（聊天记忆、向量与文档片段）

---

## 核心能力

### 1) AI 对话与 Agent 工具调用
- 对话入口为 `/chat`，支持基于 `memoryId` 的多轮会话
- `ConsultantService` 挂载业务工具，模型可自动调用：
  - `DailyStatusHealthTool`
  - `DietRecommendationTool`
  - `NutritionAnalysisTool`

### 2) 健康数据闭环
- 用户注册、登录、资料维护
- 每日状态读取与更新（饮水、睡眠、运动）
- 工具层结合用户状态生成可执行建议

### 3) 知识增强检索（RAG）
- 基于 LangChain4j 的 `EmbeddingStoreContentRetriever`
- 可结合 `src/main/resources/content/` 的知识数据进行向量检索增强

### 4) 营养热力图可视化
- `Visualizer/RedisVectorHeatmap.java` 支持从 Redis 中提取营养字段
- 维度：热量、碳水、蛋白质、脂肪、维生素 C、钾
- 输出 PNG 到 `src/main/resources/redisheatmap/`

---

## 项目结构

```text
consultant/
├── src/main/java/com/zyt/consultant
│   ├── aiservice/           # AI 服务接口（ConsultantService）
│   ├── controller/          # ChatController、UserController
│   ├── tools/               # Agent 工具实现
│   ├── service/             # 业务服务接口与实现
│   ├── mapper/              # MyBatis Mapper
│   ├── repository/          # RedisChatMemoryStore
│   ├── metrics/             # 业务指标采集
│   ├── config/              # Spring/Redis/LangChain 配置
│   └── Visualizer/          # Redis 营养热力图
├── src/main/resources
│   ├── content/             # 营养知识数据
│   ├── redisheatmap/        # 热力图输出目录
│   ├── sql/diet.sql         # 数据库初始化脚本
│   └── application.yml      # 项目配置
├── diet_helper-frontend/    # Vue 前端
└── pom.xml
```

---

## API 概览（当前后端实现）

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| AI 对话 | GET/POST | `/chat` | 参数：`memoryId`、`message`，返回流式文本 |
| 用户 | POST | `/user/register` | 用户注册 |
| 用户 | POST | `/user/login` | 用户登录 |
| 用户 | GET | `/user/profile` | 查询用户资料（按手机号） |
| 用户 | POST | `/user/profile/update` | 更新用户资料 |
| 每日状态 | GET | `/user/daily` | 查询每日状态（按 userId） |
| 每日状态 | POST | `/user/daily/update` | 更新每日状态 |

---

## 快速启动

### 1. 环境准备
- MySQL 8.0
- Redis（建议安装 RedisJSON 模块以支持 JSON 场景）
- Node.js 18+
- JDK（需与 `pom.xml` 编译目标一致）

### 2. 初始化数据库

将 `src/main/resources/sql/diet.sql` 导入 MySQL。

### 3. 配置后端

在 `src/main/resources/application.yml` 中配置：
- MySQL 连接
- Redis 地址与密码
- DashScope / OpenAI 兼容 API Key（按当前配置项）

### 4. 启动后端

```bash
mvn spring-boot:run
```

### 5. 启动前端

```bash
cd diet_helper-frontend
npm install
npm run dev
```

---

## 热力图使用说明

执行 `RedisVectorHeatmap` 后，会读取 Redis 中可识别的营养数据并输出 PNG。

- 默认输出路径：
  - `e:/code/javasepro/consultant/src/main/resources/redisheatmap/nutrition-heatmap.png`
- 如目录不存在，程序会自动创建

---

## 监控与运维

- 已集成 Spring Boot Actuator 与 Prometheus Registry
- 可接入 Grafana 展示聊天请求量、用户行为与每日状态指标

---

## 版本说明

当前版本聚焦于：
- AI 对话 + Tool 调用
- 用户健康数据管理
- 营养维度可视化

后续可扩展方向：
- 更细粒度的 RAG 数据治理与增量更新
- 设备数据接入（如运动手环/体脂秤）
- 报告导出与长期趋势分析

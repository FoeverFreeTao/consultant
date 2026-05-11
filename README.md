# 知心饮食与健康助手

基于 Spring Boot、LangChain4j、Vue 3、Redis、PostgreSQL pgvector、Elasticsearch 和可选 Neo4j GraphRAG 构建的 AI 饮食与健康咨询系统。项目围绕食品知识库、用户健康档案、日常状态和多会话聊天记忆，为用户提供饮食咨询、营养分析、技能偏好增强和知识库参考来源追踪。

## 核心能力

- 用户注册、登录、个人资料维护和日常健康状态维护
- 多会话聊天，按用户和会话维度隔离上下文记忆
- Redis 持久化聊天记忆、会话元信息和会话排序
- 基于 pgvector、Elasticsearch、qwen3-rerank 和可选 GraphRAG 的混合 RAG 检索
- 食品知识库内容清洗、召回、精排和参考来源统一追加
- 技能偏好系统，支持从 Markdown 技能文件动态生成 LLM 行为提示
- 文本输入 Guardrail，拦截敏感词、提示注入和越狱类输入
- 餐食图片识别入口，支持图片餐食分析兜底流程
- Micrometer + Prometheus 监控指标

## 技术栈

后端：

- Java 17
- Spring Boot 3.5
- LangChain4j
- MyBatis Plus
- MySQL
- Redis / Redisson
- PostgreSQL + pgvector
- Elasticsearch
- Neo4j（可选）
- DashScope 兼容 OpenAI 接口
- qwen3-rerank
- Micrometer + Prometheus

前端：

- Vue 3
- Vite
- 原生 fetch

## 项目结构

```text
consultant/
├─ src/main/java/com/zyt/consultant
│  ├─ aiservice/          # LangChain4j AI Service 接口
│  ├─ config/             # Spring、pgvector、Redis、模型与 RAG 配置
│  ├─ controller/         # HTTP 接口
│  ├─ GraphRAG/           # Neo4j 知识图谱同步与关系召回
│  ├─ guardrail/          # 输入安全拦截
│  ├─ mapper/             # MyBatis Mapper
│  ├─ metrics/            # 业务指标
│  ├─ rag/                # 混合检索、ES 召回、rerank 和参考来源上下文
│  ├─ repository/         # Redis 聊天记忆存储
│  ├─ service/            # 用户、会话、技能、视觉分析等业务服务
│  ├─ tools/              # LLM 工具调用
│  └─ Visualizer/         # 向量可视化工具
├─ src/main/resources
│  ├─ content/            # 食品知识库原始内容
│  ├─ skills/             # 技能 Markdown 定义
│  ├─ sql/                # MySQL 初始化脚本
│  ├─ System.txt          # 系统提示词
│  └─ application.yml     # 主配置文件
├─ diet_helper-frontend/  # Vue 前端工程
└─ pom.xml
```

## 混合 RAG 架构

项目采用 pgvector 语义召回、Elasticsearch 关键词召回、qwen3-rerank 精排与可选 GraphRAG 补充的混合 RAG 架构，从食品知识库中获取可靠背景材料，并在回答中统一追加参考来源。

检索流程：

```text
用户问题
  -> pgvector 向量召回
  -> Elasticsearch 关键词候选召回
  -> 候选合并、去重、粗排
  -> qwen3-rerank cross-encoder 精排
  -> 取 topN 片段注入 LLM
  -> 可选并行 GraphRAG 关系补充
  -> 服务端统一追加参考来源
```

关键配置：

```yaml
app:
  rag:
    search:
      enabled: true
      base-url: http://127.0.0.1:9200
      index-name: consultant_knowledge
      auto-index: true
    rerank:
      enabled: true
      base-url: https://dashscope.aliyuncs.com
      api-key: ${API_KEY:}
      model-name: qwen3-rerank
      candidate-limit: 32
      top-n: 4
      max-document-chars: 3500
    parallel:
      graph-timeout-ms: 1200
```

说明：

- pgvector 保存食品知识库向量，负责语义召回。
- Elasticsearch 保存切分后的知识库文本片段，负责关键词候选召回，避免请求时全量遍历。
- `qwen3-rerank` 对候选片段做 cross-encoder 精排，默认最终取 4 条上下文。
- GraphRAG 可从 Neo4j 中补充食品、营养、特征和分类之间的结构化关系。
- 检索片段注入模型前会清洗，避免原始 JSON、字段名和内部提示暴露给用户。
- 回答末尾的“参考来源”由服务端根据命中的 metadata 统一生成。

## Redis 会话记忆

Redis 中保存三类聊天数据：

| 用途 | Redis 类型 | Key |
| --- | --- | --- |
| 聊天记忆 | String | `chat:memory:user:{userId}:session:{sessionId}` |
| 会话元信息 | Hash | `chat:sessions:user:{userId}:meta` |
| 会话排序 | ZSet | `chat:sessions:user:{userId}:order` |

`memory` 保存 LangChain4j `ChatMessage` 列表序列化后的 JSON，用于恢复 LLM 上下文。

`meta` 保存会话卡片信息，Hash field 是 `sessionId`，value 是包含 `title`、`createdAt`、`updatedAt` 的 JSON。

`order` 保存会话排序，member 是 `sessionId`，score 是 `updatedAt` 时间戳。

滑动窗口策略：

- LangChain4j `MessageWindowChatMemory` 最多保留 20 条消息。
- 项目额外配置字符窗口，默认 `context-window-max-chars: 12000`。
- 最近 `context-window-recent-reserve: 4` 条非 system 消息强制保留。
- 超出字符预算时，保留开头 system 消息和最新消息，跳过更旧且导致超预算的消息。
- Redis TTL 默认 `chat-memory-ttl-days: 1`。

## 数据存储

### MySQL

保存用户、食品、日常状态等业务数据。

初始化脚本：

```text
src/main/resources/sql/diet.sql
```

### PostgreSQL + pgvector

保存知识库 embedding，默认表：

```text
consultant_embedding
```

配置项：

```yaml
app:
  vector-store:
    pgvector:
      host: 127.0.0.1
      port: 5432
      database: vector_db
      table: consultant_embedding
      dimension: 1024
      auto-ingest: true
```

当 pgvector 表为空且 `auto-ingest` 开启时，应用会读取 `src/main/resources/content` 并自动切分、向量化、入库。

### Elasticsearch

保存知识库文本片段，默认索引：

```text
consultant_knowledge
```

应用启动时如果 `app.rag.search.auto-index: true`，会将 `src/main/resources/content` 切分后的片段写入 ES。

查看索引：

```bash
curl http://127.0.0.1:9200/_cat/indices?v
curl http://127.0.0.1:9200/consultant_knowledge/_count
```

### Neo4j（可选）

Neo4j 用于构建食品知识图谱和 GraphRAG 补充召回。开启后可从 pgvector 既有数据同步 `KnowledgeChunk`，并从食品 JSON 中解析 `Food`、`Category`、`Feature`、`Nutrient` 等节点。

手动同步：

```bash
curl -X POST http://127.0.0.1:8087/knowledge-graph/sync
```

查看关系：

```cypher
MATCH p=(f:Food)-[:HAS_NUTRIENT|HAS_FEATURE|BELONGS_TO]->()
RETURN p
LIMIT 50;
```

## 技能偏好

技能文件位于：

```text
src/main/resources/skills/
```

每个技能是一个 Markdown 文件，包含前置元数据和正文提示：

```md
---
id: fat_loss
name: 减脂教练
description: 优先推荐低热量、高饱腹感、便于坚持的饮食方案。
---

Prioritize a fat-loss coaching style.
```

前端展示 `name` 和 `description`，后端将正文注入到用户问题前，形成技能增强提示。

## 输入安全

输入 Guardrail 位于：

```text
src/main/java/com/zyt/consultant/guardrail/
```

当前能力：

- 敏感词拦截
- prompt injection / jailbreak 表达检测
- Guardrail 异常时降级放行，避免安全模块故障阻断正常咨询

## 接口概览

### 聊天

```text
GET/POST /chat
```

参数：

- `memoryId`：聊天记忆 key，建议使用 `chat:memory:user:{userId}:session:{sessionId}`
- `message`：用户问题
- `skillIds`：可选，逗号分隔的技能 ID

### 图片餐食分析

```text
POST /chat/image
```

`multipart/form-data` 参数：

- `memoryId`
- `message`：可选
- `image`

### 会话

```text
GET  /chat/sessions?userId={userId}
GET  /chat/sessions/messages?userId={userId}&sessionId={sessionId}
POST /chat/sessions/create
POST /chat/sessions/delete
```

### 用户

```text
POST /user/register
POST /user/login
GET  /user/profile
POST /user/profile/update
GET  /user/daily
POST /user/daily/update
```

### 技能

```text
GET  /skills/list
GET  /skills/user
POST /skills/user/apply
```

### 知识图谱

```text
POST /knowledge-graph/sync
```

## 前端说明

前端位于：

```text
diet_helper-frontend/
```

当前是三栏布局：

- 左侧：会话列表，支持新增、删除和切换
- 中间：聊天区，支持快捷问题、历史消息和输入
- 右侧：个人资料、日常状态和技能偏好

主要组件：

- `AuthPanel.vue`
- `SessionSidebar.vue`
- `ChatPanel.vue`
- `ProfilePanel.vue`
- `ProfileModal.vue`
- `DailyModal.vue`

主状态和接口调用集中在：

```text
diet_helper-frontend/src/App.vue
```

## 快速启动

### 1. 环境准备

- JDK 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8+
- Redis 6+
- PostgreSQL 15+，并安装 pgvector 扩展
- Elasticsearch 8+（关键词召回需要）
- Neo4j 5+（GraphRAG 可选）

### 2. 启动 Elasticsearch

Docker 示例：

```bash
docker run -d --name consultant-es \
  -p 9200:9200 \
  -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  docker.elastic.co/elasticsearch/elasticsearch:8.13.4
```

验证：

```bash
curl http://127.0.0.1:9200
```

暂时不用 ES 时可关闭关键词召回：

```yaml
app:
  rag:
    search:
      enabled: false
```

### 3. 初始化 MySQL

导入：

```text
src/main/resources/sql/diet.sql
```

### 4. 配置后端

重点确认：

- `langchain4j.open-ai.*`
- `spring.datasource.*`
- `spring.data.redis.*`
- `app.vector-store.pgvector.*`
- `app.rag.search.*`
- `app.rag.rerank.*`
- `app.knowledge-graph.neo4j.*`
- `app.guardrails.input.*`

模型 API key 建议放到环境变量：

```bash
API_KEY=your_dashscope_api_key
```

### 5. 启动后端

```bash
mvn spring-boot:run
```

默认端口：

```text
8087
```

### 6. 启动前端

```bash
cd diet_helper-frontend
npm install
npm run dev
```

## 监控

项目集成 Spring Boot Actuator 和 Prometheus 指标。

常用端点：

```text
/actuator/health
/actuator/metrics
/actuator/prometheus
```

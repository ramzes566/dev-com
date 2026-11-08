# dev-com 项目进度看板

> 最后更新：2026-11-08 22:55（Asia/Shanghai）
> 用途：跨会话的唯一进度事实源。每完成一步，更新第 3 节状态 + 第 4 节证据 + 第 8 节变更记录。
> 状态口径：✅ 完成（有可复现证据） / 🚧 进行中 / ⬜ 未开始。没有证据的一律不算完成。

---

## 1. 项目目标

**一句话**：一个可在线访问的轻量技术社区。业务外壳做「完整」，技术纵深全部压在**互动写入（点赞/收藏）**与 **Feed 流**两条链路上。

**简历定位**：Java 后端个人项目。核心故事是「一个看似简单的点赞按钮，背后是一套高并发写入的治理方案」，每个结论都能讲出「问题 → 方案 → 代价」。

**成功标准（缺一不可）**

1. 浏览器可走通完整闭环：注册/登录 → 发帖 → 评论 → 点赞/收藏 → 关注流/热榜 → 收通知。
2. 点赞存在**两套实现**：DB 直写 baseline 与 Redis+MQ optimized，用配置开关切换，可分别压测。
3. 有自己跑出来的压测数据（QPS / P99 / 错误率）与优化前后对比，不引用别人的数字。
4. Docker Compose 一键起全栈，云服务器可访问（HTTPS）。
5. 能对着架构图把点赞链路、Feed 推拉、幂等与最终一致讲 3 遍不卡壳。

**明确不做**：Elasticsearch、私信、支付、复杂权限体系、无限级评论、推荐算法。每多一个中间件，就多一份「讲不透」的风险。

---

## 2. 技术栈（以实际落地为准）

| 层次 | 计划 | 实际 | 说明 |
|---|---|---|---|
| 语言/运行时 | Java 17 | **Java 21**（本机 JDK 24 编译到 21） | 21 是 LTS，虚拟线程后续可用；pom 中 `java.version=21` |
| 后端 | Spring Boot 3.x | **3.5.16** | Boot 3.5 管 Flyway 11.x，已加 `flyway-mysql` |
| 持久层 | MyBatis-Plus | **3.5.18** + `mybatis-plus-jsqlparser` | 分页插件依赖已补齐 |
| 数据库 | MySQL 8 | **8.4**（Docker） | 唯一约束做幂等兜底 |
| 缓存 | Redis 7 | **7.4**（Docker，带密码） | Bitmap + Lua + 分桶，第 9 步启用 |
| 消息队列 | RabbitMQ/RocketMQ | **RabbitMQ 3.13-management**（Docker） | 第 10 步启用 |
| 迁移 | Flyway | **已接入**（`validate-on-migrate=true`，`clean-disabled=true`） | 表结构只允许迁移脚本变更 |
| 认证 | Spring Security + JWT | **未接入**（无 spring-security 依赖） | 第 3 步 |
| 前端 | Vue 3 + Vite + Element Plus | **Vue 3.5 + Vite 8 + TS**，无 Element Plus | 第 8 步再决定是否引入 UI 库 |
| 测试 | — | **JUnit 5 + Testcontainers（mysql:8.4）** | 已有 2 个冒烟测试，见 4.1 |
| 部署 | Docker Compose | **compose 文件已就绪**，本机容器当前未启动 | 见风险 R1 |
| 压测 | JMeter | 未开始 | 第 16 步 |

---

## 3. 路线图（17 步）

> 编号沿用对话中已确认的锚点：1 / 2 / 3 / 8 / 9 / 10 / 13；其余按阶段补全。

| # | 阶段 | 步骤 | 状态 | 完成标志（DoD 摘要） |
|---|---|---|---|---|
| 1 | 一 | 项目骨架 + 健康检查 + 用户冒烟链路 | ✅ | 应用能起、`/api/Health` 真探活、`/api/users` 能查到种子用户、测试绿 |
| 2 | 一 | **数据模型与迁移（10 张表）** | ⬜ 下一步 | Flyway 自动建 10 表；唯一键实测生效；种子数据可查 |
| 3 | 一 | 认证：Security 6 过滤器链 + JWT 双令牌 | ⬜ | 注册/登录/刷新/登出；Refresh Token 存 Redis 且轮换；禁言拦截 |
| 4 | 一 | 帖子与标签：Markdown 服务端渲染、摘要、编辑删除 | ⬜ | 发帖→详情→列表全通；XSS 被过滤；列表不查 MEDIUMTEXT |
| 5 | 一 | 评论与回复（一层嵌套 root_id/parent_id） | ⬜ | 详情页两次查询取完一级+二级，无 N+1 |
| 6 | 一 | 互动 baseline：DB 直写点赞/收藏 + 双实现开关 | ⬜ | 开关可切 baseline/optimized；baseline 有压测价值 |
| 7 | 一 | Feed v1：时间倒序 + 关注流 SQL 版 | ⬜ | 首页能看全站最新与关注流 |
| 8 | 一 | 前端串联：登录、列表、详情、评论、点赞 | ⬜ | 浏览器可完整演示，阶段一收口 |
| 9 | 二 | 互动纵深 A：Redis Bitmap + Lua | ⬜ | 判重+计数原子完成；Redis 故障有降级 |
| 10 | 二 | 互动纵深 B：MQ 异步落库 + 唯一键幂等 + 重试队列 | ⬜ | 重复消费不翻倍；发送失败进重试队列 |
| 11 | 二 | 互动纵深 C：计数分桶（4 桶）+ 聚合读 + 对账 | ⬜ | 能讲清分桶收益与对账补偿 |
| 12 | 二 | 通知模块：点赞/评论/关注 + 未读数 | ⬜ | 未读数准确，通知页不再 join |
| 13 | 二 | Feed 纵深：推拉结合 + feed_inbox 写扩散 | ⬜ | 普通用户推、大 V 拉；阈值可配置 |
| 14 | 二 | DFA/Trie 敏感词过滤 + 词库热更新 | ⬜ | 发帖/评论实时拦截，O(n) |
| 15 | 三 | 治理后台：审核/下架、举报、禁言 | ⬜ | 管理员可操作，审计字段完整 |
| 16 | 三 | JMeter 压测：baseline vs optimized | ⬜ | 产出 QPS/P99/错误率对比表 |
| 17 | 三 | 交付：Compose 部署 + README/架构图 + 简历数据 | ⬜ | 公网可访问，仓库能自证 |

> 阶段一约 2 周，阶段二约 2.5–3 周，阶段三约 1 周。**阶段二不可压缩**，它是这个项目区别于「又一个论坛 CRUD」的全部价值。

---

## 4. 当前进度

### 4.1 ✅ 第 1 步：项目骨架（完成）

| 能力 | 证据 |
|---|---|
| 后端可构建启动 | [pom.xml](../backend/pom.xml)：Boot 3.5.16、MP 3.5.18、Flyway、Actuator、springdoc 2.8.17、Redis/AMQP starter、Testcontainers |
| 真探活而非假绿灯 | [HealthController.java](../backend/src/main/java/com/y1/community/web/HealthController.java)：`SELECT 1`、Redis `hasKey`、RabbitMQ `createConnection`，任一失败返回 `fail: xxx` |
| 最小用户链路 | [UserController.java](../backend/src/main/java/com/y1/community/user/controller/UserController.java) `GET /api/users` + MP `ServiceImpl` |
| 迁移已跑通 | [V1__init_user.sql](../backend/src/main/resources/db/migration/V1__init_user.sql)（`t_user`）、[V2__seed_user.sql](../backend/src/main/resources/db/migration/V2__seed_user.sql)（demo 用户） |
| 自动化测试绿 | `backend/target/surefire-reports`：`CommunityApplicationTests`、`UserApiTest` 各 1 条，2026-11-08 00:22–00:23 通过（Testcontainers MySQL 8.4） |
| 前端骨架 | Vue 3.5 + Vite 8 + TS，[vite.config.ts](../frontend/vite.config.ts) 代理 `/api` → 8080，[App.vue](../frontend/src/App.vue) 为骨架自检页 |
| 本地依赖编排 | [docker-compose.yml](../deploy/docker-compose.yml)：MySQL 8.4 / Redis 7.4 / RabbitMQ 3.13，均带 healthcheck 与数据卷 |
| 版本控制 | 3 个提交（`first step` → `初始化前后端项目` → `fix`）；`backend` 曾被误记为 gitlink，已于 2026-11-08 修复为普通目录跟踪 |

**这一步的定位**：只证明「三件套能连通、探活是真的、测试能跑」，不承载业务。

### 4.2 ⬜ 第 2 步：数据模型与迁移（未开始）

**现状**：设计稿要求 10 张表，当前只有 1 张 `t_user`，且与设计稿存在 5 处偏差：

| 项 | 设计稿 | 当前实现 | 处置建议 |
|---|---|---|---|
| 表名 | `user`（无前缀） | `t_user` + MP `table-prefix: t_` | 去前缀，与设计稿对齐（见 D1） |
| 密码列 | `password_hash`（BCrypt） | `password`，种子为明文 `demo` | 第 2 步改列名，第 3 步换 BCrypt 并重刷种子 |
| 用户状态 | `status`（0 正常/1 禁言/2 封禁） | `deleted` 逻辑删除 | 改为 `status`，删除语义并入状态 |
| 资料与计数 | `avatar`/`bio`/`follower_count`/`following_count`/`post_count` | 全部缺失 | 随 V1 重写补齐 |
| 时间与排序规则 | `DATETIME(3)`、`utf8mb4_0900_ai_ci` | `DATETIME`、`utf8mb4_unicode_ci` | 统一为设计稿口径 |

**缺口**：`post`、`tag`、`post_tag`、`comment`、`interaction`、`follow`、`feed_inbox`、`notification`、`report` 共 9 张表，以及全部索引、唯一键、种子数据、验收脚本。

---

## 5. 第 2 步开工前必须拍板的 4 件事

| # | 决策 | 推荐做法 | 理由 / 代价 |
|---|---|---|---|
| D1 | 表名是否保留 `t_` 前缀 | **去掉前缀，按设计稿 10 张表** | 后续第 9/10/13 步的 SQL、文档、面试讲述都引用 `interaction`/`feed_inbox`；前缀会让每处都要翻译。MP 的 `table-prefix` 同时删掉 |
| D2 | V1/V2 怎么处理 | **合并重写为 `V1__init.sql`（10 表）+ `V2__seed.sql`，一次性重置 `demo_db`** | 当前只有 1 行种子、未上线，重写成本最低；若保留旧 V1 再 `V3+ALTER`，会留下与设计稿不一致的历史包袱。重置后从 V3 起严格 append-only |
| D3 | 健康检查路径 | **统一小写 `/api/health`**，同步改 [App.vue](../frontend/src/App.vue) | 现在代码是 `/api/Health`，前后端恰好一致；但云服务器/网关/压测脚本按小写写会 404，越早统一越好 |
| D4 | 构建可复现 | **补 Maven Wrapper（mvnw）** | 本机 `mvn` 不在 PATH，目前只有 IDE 能构建；压测机、CI、云服务器都需要 `./mvnw` |

> D2 的重置命令（确认后执行）：
> `docker compose -f deploy/docker-compose.yml down` → 删除 `mysql_data` 卷或 `DROP DATABASE demo_db` → 重新 `up -d` → 启动应用由 Flyway 重建。
> **注意**：`spring.flyway.validate-on-migrate=true`，改了 V1 而不重置库，启动会直接因 checksum 不匹配失败。

---

## 6. 风险登记

| # | 风险 | 影响 | 处置 |
|---|---|---|---|
| R1 | Docker 当前未启动，3306/6379/5672/8080 均未监听 | 第 2 步无法做「启动 + SHOW TABLES」验收 | 开工第一条命令：`docker compose -f deploy/docker-compose.yml up -d`，等 healthcheck 全绿 |
| R2 | 改 V1 触发 Flyway checksum 冲突 | 应用启动失败，误以为是代码问题 | 按 D2 重置 `demo_db`；以后只追加 V3+ |
| R3 | 无 mvnw、`mvn` 不在 PATH | CI/压测机/服务器构建不可复现 | 第 2 步内补 wrapper（D4） |
| R4 | 种子密码明文 `demo` | 第 3 步接 BCrypt 时登录失败 | 第 2 步种子直接写 BCrypt 串，或第 3 步用 V3 更新 |
| R5 | `/api/Health` 的 RabbitMQ 探活无超时 | broker 不可达时探活可能长时间阻塞 | 给 `spring.rabbitmq.connection-timeout` 设 2s，或探活改走 Actuator |
| R6 | `.idea/dataSources.xml` 等 IDE 文件已入库 | 含本地连接串（无明文密码），换机易冲突 | 可接受；后续可整体忽略 `.idea/` |

---

## 7. 第 2 步 Definition of Done

- [ ] `docker compose up -d` 后 MySQL/Redis/RabbitMQ 全部 healthy
- [ ] 启动应用，Flyway 日志出现 `Successfully applied 2 migrations`
- [ ] `SHOW TABLES` 恰好 10 张；`SHOW CREATE TABLE interaction` 可见 `uk_user_target_type`
- [ ] 手工插两条相同 `(user_id,target_type,target_id,type)`，第二条报 `Duplicate entry`
- [ ] 种子数据：3 用户（含 1 个粉丝数 0、1 个 5000+ 大 V）、10 帖、20 评论、若干关注关系
- [ ] `feed_inbox` 存在且为空
- [ ] `./mvnw test` 绿（含迁移 + 唯一键断言）
- [ ] `git commit -m "feat: schema with flyway migration"`

---

## 8. 变更记录

| 日期 | 变更 |
|---|---|
| 2026-11-08 | 建立看板；确认第 1 步完成、第 2 步未开始；修复 `backend` 误记为 gitlink 的问题；登记 4 项待决事项与 6 项风险 |

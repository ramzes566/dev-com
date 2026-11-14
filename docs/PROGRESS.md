# dev-com 项目进度看板

> 最后更新：2026-11-14 16:30（Asia/Shanghai）
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
| 迁移 | Flyway | **已接入，V1/V2 已落库 10 张业务表**（`validate-on-migrate=true`，`clean-disabled=true`） | 表结构只允许迁移脚本变更；从 V3 起严格 append-only |
| 认证 | Spring Security + JWT | **未接入**（无 spring-security 依赖） | 第 3 步 |
| 前端 | Vue 3 + Vite + Element Plus | **Vue 3.5 + Vite 8 + TS**，无 Element Plus | 第 8 步再决定是否引入 UI 库 |
| 构建 | Maven | **Maven Wrapper（mvnw）已入库**，Maven 3.9.16 | 任何机器 `.\mvnw.cmd test` 可构建，不再依赖本机 mvn |
| 测试 | — | **JUnit 5 + Testcontainers（mysql:8.4）** | 3 条：上下文加载、种子可查、响应不泄密，见 4.1/4.2 |
| 部署 | Docker Compose | **compose 文件已就绪**，本机容器当前未启动 | 见风险 R1 |
| 压测 | JMeter | 未开始 | 第 16 步 |

---

## 3. 路线图（17 步）

> 编号沿用对话中已确认的锚点：1 / 2 / 3 / 8 / 9 / 10 / 13；其余按阶段补全。

| # | 阶段 | 步骤 | 状态 | 完成标志（DoD 摘要） |
|---|---|---|---|---|
| 1 | 一 | 项目骨架 + 健康检查 + 用户冒烟链路 | ✅ | 应用能起、`/api/health` 真探活、`/api/users` 返回 UserVO（无密码字段）、测试绿 |
| 2 | 一 | **数据模型与迁移（10 张表）** | 🚧 差 4 条实测 | Flyway 从零建 10 张业务表；唯一键实测生效；种子数据可查 |
| 3 | 一 | 认证：Security 6 过滤器链 + JWT 双令牌 | ⬜ 下一步 | 注册/登录/刷新/登出；Refresh Token 存 Redis 且轮换；禁言拦截 |
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
| 最小用户链路 | [UserController.java](../backend/src/main/java/com/y1/community/user/controller/UserController.java) `GET /api/users` → `List<UserVO>`；[UserVO.java](../backend/src/main/java/com/y1/community/user/vo/UserVO.java) 收口对外字段 |
| 迁移已跑通 | 第 1 步的 `V1__init_user.sql` / `V2__seed_user.sql` 已在第 2 步重写为 [V1__init.sql](../backend/src/main/resources/db/migration/V1__init.sql) / [V2__seed.sql](../backend/src/main/resources/db/migration/V2__seed.sql) |
| 自动化测试绿 | `backend/target/surefire-reports`：2026-11-08 首批 2 条（上下文加载、种子可查）通过（Testcontainers MySQL 8.4）；第 5 步新增防泄漏断言，共 3 条 |
| 本地依赖已起 | `docker ps`：`mysql84` / `redis74` / `rabbitmq313` 均 healthy；端口 3306/6379/5672 可连 |
| 迁移已在本地库生效 | 第 1 步的 `t_user` 已随第 2 步重置删除，现库结构与 4.2 一致 |
| 应用实测可启动 | 2026-11-08 22:39 本机启动：Java 24.0.1、Boot 3.5.16、Tomcat 8080、Flyway `Successfully validated 2 migrations`、8s 内就绪 |
| 探活实测为真 | `GET /api/health` → `{"redis":"ok","db":"ok","mq":"ok"}`（Redis/MQ 断开时会返回 `fail: xxx`，不是写死的） |
| 前端骨架 | Vue 3.5 + Vite 8 + TS，[vite.config.ts](../frontend/vite.config.ts) 代理 `/api` → 8080，[App.vue](../frontend/src/App.vue) 为骨架自检页（已改为调用小写 `/api/health`） |
| 本地依赖编排 | [docker-compose.yml](../deploy/docker-compose.yml)：MySQL 8.4 / Redis 7.4 / RabbitMQ 3.13，均带 healthcheck 与数据卷 |
| 版本控制 | 8 个提交；`backend` 曾被误记为 gitlink，已于 2026-11-08 修复为普通目录跟踪 |

**这一步的定位**：只证明「三件套能连通、探活是真的、测试能跑」，不承载业务。

### 4.2 🚧 第 2 步：数据模型与迁移（产物已落库，差 4 条实测）

**结果**：`demo_db` 已重置为 0 表，由 Flyway 从零重建；设计稿的 5 处偏差全部消除。

| 项 | 设计稿要求 | 当前实现 | 状态 |
|---|---|---|---|
| 表名 | `user`（无前缀） | `user`，MP `table-prefix: t_` 已删 | ✅ |
| 密码列 | `password_hash`（BCrypt） | `password_hash VARCHAR(100)`，种子为 `$2a$10$...`（密码 `demo1234`，已用 bcrypt 校验） | ✅ |
| 用户状态 | `status`（0 正常/1 禁言/2 封禁） | `status TINYINT`，`deleted` 与 `@TableLogic` 已移除 | ✅ |
| 资料与计数 | `avatar`/`bio`/3 个计数 | 全部补齐 | ✅ |
| 时间与排序规则 | `DATETIME(3)`、`utf8mb4_0900_ai_ci` | 全库一致 | ✅ |

**10 张业务表**

| 表 | 作用 | 关键约束 |
|---|---|---|
| `user` | 账号、资料、冗余计数 | `uk_username` |
| `post` | 帖子，摘要 / HTML / Markdown 分三列 | `idx_created(status, created_at DESC, id DESC)` |
| `tag` | 标签 | `uk_name` |
| `post_tag` | 帖子-标签关联 | 联合主键 `(post_id, tag_id)` + `idx_tag(tag_id, post_id)` |
| `comment` | 评论，一层嵌套 `root_id`/`parent_id` | `idx_post_root(post_id, root_id, created_at)` |
| `interaction` | 点赞/收藏，软取消 | `uk_user_target_type(user_id, target_type, target_id, type)` |
| `follow` | 关注关系 | `uk_follower_followee(follower_id, followee_id)` |
| `feed_inbox` | 写扩散收件箱（第 13 步启用） | `uk_user_post(user_id, post_id)` |
| `notification` | 通知 | `idx_user_read(user_id, is_read, created_at DESC)` |
| `report` | 举报 | `uk_reporter_target` |

**设计取舍**

1. **不建物理外键**：`interaction`/`feed_inbox` 是第 9/10/13 步的热点写入表，外键带来写放大与分库阻碍；完整性交给应用层 + 唯一键 + 对账补偿。
2. **密码列直接落 BCrypt**：种子即密文，第 3 步接 `BCryptPasswordEncoder` 不需要再改数据。
3. **帖子拆三列**：`summary` 给列表、`content_html` 给详情、`content_md` 给编辑，各管一个场景，列表页不碰 MEDIUMTEXT。
4. **计数冗余在 `user`/`post`/`tag` 上**：读多写少，允许短暂不一致，靠对账修正。

**证据**

- 2026-11-13 实测 `SHOW TABLES` 共 **11 行** = 10 张业务表 + `flyway_schema_history`。
- 种子脚本口径：3 用户 / 5 标签 / 10 帖 / 13 条 `post_tag` / 20 评论 / 3 关注 / 4 互动；`feed_inbox` 空表。
- 代码提交：`fae7c93`（10 表迁移 + 实体）、`7a45758`（health 小写 + rabbit 超时）。

**还差的 4 条实测**（见第 7 节）：唯一键重复插入、种子计数、测试 3 条绿、`/api/users` 无密码字段。

---

## 5. 第 2 步的 4 个决策（已全部落地）

| # | 决策 | 最终做法 | 结果 |
|---|---|---|---|
| D1 | 表名是否保留 `t_` 前缀 | **去掉前缀**，按设计稿 10 张表；MP `table-prefix` 删除 | ✅ 已落地 |
| D2 | V1/V2 怎么处理 | **合并重写为 `V1__init.sql` + `V2__seed.sql`，一次性重置 `demo_db`** | ✅ 已落地；从 V3 起 append-only |
| D3 | 健康检查路径 | **统一小写 `/api/health`**，同步改 `App.vue` | ✅ 已落地，大写返回 404 |
| D4 | 构建可复现 | **补 Maven Wrapper（mvnw）**，含 `.gitattributes` 保证 LF | ✅ 已落地，`i/lf` 已验证 |

> 重置时的坑（留给以后参考）：改了 V1 内容但版本号不变时，`validate-on-migrate=true` 会让启动直接失败，必须先清空 schema（连 `flyway_schema_history` 一起），否则报 checksum 不匹配；清表不够，还要清 `target/classes/db/migration` 里的旧脚本副本，否则报 `Found more than one migration with version 1`。

---

## 6. 风险登记

| # | 风险 | 影响 | 处置 | 状态 |
|---|---|---|---|---|
| R1 | 容器与**应用**是两回事，应用不常驻 | 验收时忘启动，误判为"没生效" | 验收前先启动应用；容器无需重启 | 常驻提示 |
| R2 | 改 V1 触发 Flyway checksum 冲突 | 启动失败 | 已按 D2 重置；以后只追加 V3+ | ✅ 关闭 |
| R3 | 无 mvnw、`mvn` 不在 PATH | CI/压测机不可复现 | 已补 wrapper + `.gitattributes` | ✅ 关闭 |
| R4 | 种子密码明文 | 接 BCrypt 后登录失败 | 种子直接写 BCrypt（`demo1234`） | ✅ 关闭 |
| R5 | `/api/health` 的 RabbitMQ 探活无超时 | broker 不可达时阻塞 | `spring.rabbitmq.connection-timeout: 2s` | ✅ 关闭 |
| R6 | `.idea/*.xml` 已入库 | 换机易冲突 | 可接受；后续可整体忽略 `.idea/` | 保留 |
| R7 | `/api/users` 直接返回实体，泄漏密码 | 公网可达即泄露凭据 | `UserVO` 收口 + `@JsonIgnore` 双保险 + 防泄漏断言 | ✅ 关闭 |
| R8 | Flyway 11 只声明支持到 MySQL 8.1 | 仅 WARN | 忽略；换版本再评估 | 保留 |

---

## 7. 第 2 步 Definition of Done

- [x] 三个容器 healthy
- [x] `demo_db` 重置为 0 表（2026-11-13）
- [x] 启动应用，Flyway `Successfully applied 2 migrations`
- [x] 业务表 10 张 + `flyway_schema_history`（共 11 行）
- [ ] `SHOW CREATE TABLE interaction` 可见 `uk_user_target_type`
- [ ] 手工插两条相同 `(user_id,target_type,target_id,type)`，第二条报 `Duplicate entry`
- [ ] 种子计数：3 用户 / 10 帖 / 20 评论 / 3 关注 / 4 互动；`feed_inbox` 为空
- [ ] `.\mvnw.cmd test` 3 条绿（含防泄漏断言）
- [x] 代码提交（`984460d` mvnw、`fae7c93` 10 表、`7a45758` health 小写）

---

## 8. 变更记录

| 日期 | 变更 |
|---|---|
| 2026-11-08 | 建立看板；实测第 1 步（应用启动、`/api/Health` 三项 ok、`/api/users` 返回种子数据）；确认第 2 步未开始；修复 `backend` 误记为 gitlink；登记 4 项待决事项与 8 项风险（含 `/api/users` 泄漏 password） |
| 2026-11-12 | 补 Maven Wrapper（`984460d`）与 `.gitattributes`；清理 `misc.xml` 里不存在的根 pom 引用；重置 `demo_db` |
| 2026-11-13 | 迁移重写为 `V1__init.sql`（10 表）+ `V2__seed.sql`（BCrypt 种子），实体同步 `password_hash`/`status`/计数（`fae7c93`）；实测 `SHOW TABLES` = 11 |
| 2026-11-14 | 引入 `UserVO` + 防泄漏测试；`/api/health` 统一小写，RabbitMQ 连接超时 2s（`7a45758`）；关闭 R2–R5、R7 |

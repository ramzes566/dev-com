-- ============================================================
-- 社区后端初始表结构（10 张）
-- 约定：
--   1) 时间列统一 DATETIME(3)，保留毫秒；默认值交给数据库生成
--   2) 全表 utf8mb4 / utf8mb4_0900_ai_ci（大小写不敏感，用户名比较友好）
--   3) 主键统一 BIGINT UNSIGNED（MyBatis-Plus 雪花 ID）；种子用 1..N 便于阅读
--   4) 不建物理外键：热点写入（interaction / feed_inbox）不额外付出校验开销，
--      完整性靠应用层 + 唯一键；代价与收益见 docs/PROGRESS.md
-- ============================================================

-- 1. 用户
CREATE TABLE `user`
(
    id              BIGINT UNSIGNED NOT NULL COMMENT '雪花 ID',
    username        VARCHAR(50)     NOT NULL COMMENT '登录名，唯一',
    password_hash   VARCHAR(100)    NOT NULL COMMENT 'BCrypt 摘要，固定60 字符',
    nickname        VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '昵称',
    avatar          VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '头像URL',
    bio             VARCHAR(200)    NOT NULL DEFAULT '' COMMENT '个人简介',
    status          TINYINT         NOT NULL DEFAULT 0 COMMENT '0 正常 / 1 禁言 / 2 封禁',
    follower_count  INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '粉丝数（冗余计数）',
    following_count INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '关注数（冗余计数）',
    post_count      INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '发帖数（冗余计数）',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mB4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户';

-- 2.帖子
CREATE TABLE post
(
    id            BIGINT UNSIGNED NOT NULL,
    user_id       BIGINT UNSIGNED NOT NULL COMMENT '作者',
    title         VARCHAR(120)    NOT NULL DEFAULT '',
    summary       VARCHAR(300)    NOT NULL DEFAULT '' COMMENT '纯文本摘要，列表页只读这列',
    content_md    MEDIUMTEXT      NOT NULL COMMENT 'Markdown 原文（编辑时用）',
    content_html  MEDIUMTEXT      NOT NULL COMMENT '服务端渲染结果（详情页直接用）',
    status        TINYINT         NOT NULL DEFAULT 0 COMMENT '0 正常 / 1 已下架 / 2 审核中',
    view_count    INT UNSIGNED    NOT NULL DEFAULT 0,
    like_count    INT UNSIGNED    NOT NULL DEFAULT 0,
    collect_count INT UNSIGNED    NOT NULL DEFAULT 0,
    comment_count INT UNSIGNED    NOT NULL DEFAULT 0,
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_created (status, created_at DESC, id DESC) COMMENT '首页时间倒序',
    KEY idx_user (user_id, created_at DESC) COMMENT '个人主页',
    KEY idx_hot (status, like_count DESC, created_at DESC) COMMENT '热榜v1'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '帖子';

-- 3. 标签
CREATE TABLE tag
(
    id         BIGINT UNSIGNED NOT NULL,
    name       VARCHAR(30)     NOT NULL,
    post_count INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '冗余计数',
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='标签';

-- 4. 帖子-标签关联
CREATE TABLE post_tag
(
    post_id    BIGINT UNSIGNED NOT NULL,
    tag_id     BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (post_id, tag_id),
    KEY idx_tag (tag_id, post_id) COMMENT '按标签查帖子'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='帖子标签关联';
-- 5.评论（一层嵌套）
CREATE TABLE comment
(
    id               BIGINT UNSIGNED NOT NULL,
    post_id          BIGINT UNSIGNED NOT NULL,
    user_id          BIGINT UNSIGNED NOT NULL,
    root_id          BIGINT UNSIGNED NULL COMMENT '一级评论为 NULL；二级指向一级评论 id',
    parent_id        BIGINT UNSIGNED NULL COMMENT '直接回复的那条评论',
    reply_to_user_id BIGINT UNSIGNED NULL COMMENT '被回复人，渲染时省一次 join',
    content          VARCHAR(1000)   NOT NULL,
    like_count       INT UNSIGNED    NOT NULL DEFAULT 0,
    status           TINYINT         NOT NULL DEFAULT 0 COMMENT '0 正常 / 1 已删除',
    created_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_post_root (post_id, root_id, created_at) COMMENT '两次查询取完一级+二级',
    KEY idx_user (user_id, created_at DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='评论';

-- 6. 互动（点赞 / 收藏）—— 幂等的最后一道防线
CREATE TABLE interaction
(
    id          BIGINT UNSIGNED NOT NULL,
    user_id     BIGINT UNSIGNED NOT NULL,
    target_type TINYINT         NOT NULL COMMENT '1 帖子 / 2 评论',
    target_id   BIGINT UNSIGNED NOT NULL,
    type        TINYINT         NOT NULL COMMENT '1 点赞 / 2 收藏',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '1 有效 / 0 已取消（软取消）',
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE
        CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_target_type (user_id, target_type, target_id, type),
    KEY idx_target (target_type, target_id, type, status) COMMENT '谁赞了这篇 / 重复计数'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '互动';

-- 7. 关注关系
CREATE TABLE follow
(
    id          BIGINT UNSIGNED NOT NULL,
    follower_id BIGINT UNSIGNED NOT NULL COMMENT '关注者',
    followee_id BIGINT UNSIGNED NOT NULL COMMENT '被关注者',
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_follower_followee (follower_id, followee_id),
    KEY idx_followee (followee_id, follower_id) COMMENT '粉丝列表 / 拉模式'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '关注关系';

-- 8. 收件箱（写扩散）
CREATE TABLE feed_inbox
(
    id         BIGINT UNSIGNED NOT NULL,
    user_id    BIGINT UNSIGNED NOT NULL COMMENT '收件人',
    post_id    BIGINT UNSIGNED NOT NULL,
    author_id  BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_post (user_id, post_id) COMMENT '重复投递不产生重复行',
    KEY idx_user_created (user_id, created_at DESC, id DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='Feed 收件箱';

-- 9.通知
CREATE TABLE notification
(
    id          BIGINT UNSIGNED NOT NULL,
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '接收者',
    actor_id    BIGINT UNSIGNED NOT NULL COMMENT '触发者',
    type        TINYINT         NOT NULL COMMENT '1 点赞 / 2 评论 / 3 关注',
    target_type TINYINT         NOT NULL COMMENT '1 帖子 / 2 评论 / 3 用户',
    content     VARCHAR(200)    NOT NULL DEFAULT '' COMMENT '冗余文案，通知页不再 join',
    is_read     TINYINT         NOT NULL DEFAULT 0,
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_user_read (user_id, is_read, created_at DESC) COMMENT '未读数与列表共用'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='通知';

-- 10.举报
CREATE TABLE report
(
    id          BIGINT UNSIGNED NOT NULL,
    reporter_id BIGINT UNSIGNED NOT NULL,
    target_type TINYINT         NOT NULL COMMENT '1 帖子 / 2 评论 / 3 用户',
    target_id   BIGINT UNSIGNED NOT NULL,
    reason      TINYINT         NOT NULL COMMENT '1 广告 / 2 人身攻击 / 3 违法 / 9 其他',
    detail      VARCHAR(500)    NOT NULL DEFAULT '',
    status      TINYINT         NOT NULL DEFAULT 0 COMMENT '0 待处理 / 1 已处理 / 2 驳回',
    handler_id  BIGINT UNSIGNED NULL,
    handled_at  DATETIME(3)     NULL,
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_reporter_target (reporter_id, target_type, target_id) COMMENT '同一人重复举报只留一条',
    KEY idx_status_created (status, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='举报';
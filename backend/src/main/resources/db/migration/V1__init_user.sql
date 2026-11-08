CREATE TABLE t_user
(
    id         BIGINT       NOT NULL,
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(100) NOT NULL,
    nickname   VARCHAR(50) NULL,
    deleted    TINYINT      NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username(username)
)   ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;
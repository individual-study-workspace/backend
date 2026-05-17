CREATE TABLE users (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    provider            VARCHAR(20)  NOT NULL,
    provider_id         VARCHAR(100) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    name                VARCHAR(50)  NOT NULL,
    profile_image_url   VARCHAR(500) NULL,
    role                VARCHAR(20)  NOT NULL,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    deleted_at          DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_provider_provider_id UNIQUE (provider, provider_id),
    INDEX idx_users_email (email),
    INDEX idx_users_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

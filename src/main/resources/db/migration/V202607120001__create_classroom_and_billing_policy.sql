CREATE TABLE classroom (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    created_by      BIGINT       NOT NULL,
    name            VARCHAR(20)  NOT NULL,
    class_type      VARCHAR(10)  NOT NULL DEFAULT 'ONLINE',
    repeat_type     VARCHAR(20)  NULL,
    class_days      VARCHAR(30)  NULL,
    from_date       DATE         NULL,
    from_time       TIME         NULL,
    total_sessions  SMALLINT     NULL,
    invite_code     VARCHAR(8)   NULL,
    remark          VARCHAR(500) NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NULL,
    updated_by      BIGINT       NULL,
    deleted_at      DATETIME(6)  NULL,
    deleted_by      BIGINT       NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_classroom_invite_code UNIQUE (invite_code),
    CONSTRAINT fk_classroom_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    INDEX idx_classroom_created_by (created_by),
    INDEX idx_classroom_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE billing_policy (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    class_id      BIGINT      NOT NULL,
    payment_type  VARCHAR(20) NOT NULL,
    billing_day   SMALLINT    NULL,
    billing_unit  SMALLINT    NULL,
    amount        INT         NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_billing_policy_class_id UNIQUE (class_id),
    CONSTRAINT fk_billing_policy_classroom FOREIGN KEY (class_id) REFERENCES classroom (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

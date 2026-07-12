CREATE TABLE code_management (
    category  VARCHAR(10)  NOT NULL,
    code_id        VARCHAR(20)  NOT NULL,
    name   VARCHAR(100) NOT NULL,
    sort_order  INT,
    from_date   DATE         NOT NULL,
    to_date     DATE         NULL,
    created_by VARCHAR(20) NULL,
    updated_by VARCHAR(20) NULL,
    deleted_by VARCHAR(20) NULL,
    PRIMARY KEY (category, code_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO code_management (category, code_id, name, sort_order, from_date, to_date) VALUES
('AUTH_PROVIDER', 'GOOGLE', '구글', 1, '20260101', '29991231'),
('AUTH_PROVIDER', 'KAKAO',  '카카오', 2, '20260101', '29991231');
USE dndn_db;

-- =========================================================
-- 테스트 사용자
-- 로그인:
-- 시니어   01011111111 / test
-- 보호자   01022222222 / test
-- =========================================================

INSERT INTO users (
    user_id,
    password,
    name,
    role,
    phone,
    birth_date,
    status,
    created_at,
    updated_at
)
VALUES
(
    1,
    '$2a$10$nCSaruEZZKkfwbg5QKtpwOZvKVSvPbmfhl/0IDlAWG4bxmzb/hZ2',
    '홍길동',
    'SENIOR',
    '01011111111',
    '1955-03-15',
    'ACTIVE',
    NOW(),
    NOW()
),
(
    2,
    '$2a$10$nCSaruEZZKkfwbg5QKtpwOZvKVSvPbmfhl/0IDlAWG4bxmzb/hZ2',
    '홍길은',
    'GUARDIAN',
    '01022222222',
    '1985-08-21',
    'ACTIVE',
    NOW(),
    NOW()
);


-- =========================================================
-- 시니어 계좌 3개
-- KB 은행코드 004
-- 모두 우리 서비스 등록 완료
-- =========================================================

INSERT INTO accounts (
    account_id,
    user_id,
    owner_name,
    birth_date,
    bank_code,
    account_number,
    account_name,
    balance,
    is_primary,
    status,
    created_at,
    updated_at,
    is_registration
)
VALUES
(
    1,
    1,
    '홍길동',
    '1955-03-15',
    '004',
    '12345678901234',
    'KB국민 주거래통장',
    12500000,
    TRUE,
    'ACTIVE',
    NOW(),
    NOW(),
    TRUE
),
(
    2,
    1,
    '홍길동',
    '1955-03-15',
    '004',
    '23456789012345',
    'KB국민 생활비통장',
    6800000,
    FALSE,
    'ACTIVE',
    NOW(),
    NOW(),
    TRUE
),
(
    3,
    1,
    '홍길동',
    '1955-03-15',
    '004',
    '34567890123456',
    'KB국민 비상금통장',
    9300000,
    FALSE,
    'ACTIVE',
    NOW(),
    NOW(),
    TRUE
);


-- =========================================================
-- 보호자 관계
-- =========================================================

INSERT INTO guardian_relationship (
    relationship_id,
    senior_user_id,
    guardian_user_id,
    status,
    created_at,
    approved_at
)
VALUES (
    1,
    1,
    2,
    'ACTIVE',
    NOW(),
    NOW()
);


-- =========================================================
-- 계좌 1 거래
-- =========================================================

INSERT INTO transactions (
    transaction_id,
    sender_account_id,
    receiver_account_id,
    session_id,
    receiver_bank_code,
    receiver_account_number,
    receiver_name,
    amount,
    sender_balance_before,
    sender_balance_after,
    status,
    created_at,
    completed_at,
    purpose
)
VALUES
(
    1,
    1,
    NULL,
    NULL,
    '088',
    '110123456789',
    '김민수',
    100000,
    13000000,
    12900000,
    'COMPLETED',
    DATE_SUB(NOW(), INTERVAL 25 DAY),
    DATE_SUB(NOW(), INTERVAL 25 DAY),
    '용돈'
),
(
    2,
    1,
    NULL,
    NULL,
    '020',
    '100200300400',
    '박영희',
    250000,
    12900000,
    12650000,
    'COMPLETED',
    DATE_SUB(NOW(), INTERVAL 18 DAY),
    DATE_SUB(NOW(), INTERVAL 18 DAY),
    '생활비'
),
(
    3,
    1,
    NULL,
    NULL,
    '088',
    '110123456789',
    '김민수',
    150000,
    12650000,
    12500000,
    'COMPLETED',
    DATE_SUB(NOW(), INTERVAL 10 DAY),
    DATE_SUB(NOW(), INTERVAL 10 DAY),
    '용돈'
);


-- =========================================================
-- 계좌 2 거래
-- =========================================================

INSERT INTO transactions (
    transaction_id,
    sender_account_id,
    receiver_account_id,
    session_id,
    receiver_bank_code,
    receiver_account_number,
    receiver_name,
    amount,
    sender_balance_before,
    sender_balance_after,
    status,
    created_at,
    completed_at,
    purpose
)
VALUES
(
    4,
    2,
    NULL,
    NULL,
    '088',
    '222333444555',
    '한지수',
    70000,
    7200000,
    7130000,
    'COMPLETED',
    DATE_SUB(NOW(), INTERVAL 22 DAY),
    DATE_SUB(NOW(), INTERVAL 22 DAY),
    '식사비'
),
(
    5,
    2,
    NULL,
    NULL,
    '020',
    '333444555666',
    '최유진',
    130000,
    7130000,
    7000000,
    'COMPLETED',
    DATE_SUB(NOW(), INTERVAL 16 DAY),
    DATE_SUB(NOW(), INTERVAL 16 DAY),
    '병원비'
),
(
    6,
    2,
    NULL,
    NULL,
    '011',
    '444555666777',
    '정민호',
    200000,
    7000000,
    6800000,
    'COMPLETED',
    DATE_SUB(NOW(), INTERVAL 8 DAY),
    DATE_SUB(NOW(), INTERVAL 8 DAY),
    '생활비'
);


-- =========================================================
-- 계좌 3 거래
-- =========================================================

INSERT INTO transactions (
    transaction_id,
    sender_account_id,
    receiver_account_id,
    session_id,
    receiver_bank_code,
    receiver_account_number,
    receiver_name,
    amount,
    sender_balance_before,
    sender_balance_after,
    status,
    created_at,
    completed_at,
    purpose
)
VALUES
(
    7,
    3,
    NULL,
    NULL,
    '020',
    '555666777888',
    '송지연',
    200000,
    9850000,
    9650000,
    'COMPLETED',
    DATE_SUB(NOW(), INTERVAL 27 DAY),
    DATE_SUB(NOW(), INTERVAL 27 DAY),
    '경조사비'
),
(
    8,
    3,
    NULL,
    NULL,
    '088',
    '666777888999',
    '오민석',
    100000,
    9650000,
    9550000,
    'COMPLETED',
    DATE_SUB(NOW(), INTERVAL 19 DAY),
    DATE_SUB(NOW(), INTERVAL 19 DAY),
    '모임 회비'
),
(
    9,
    3,
    NULL,
    NULL,
    '081',
    '777888999111',
    '윤수진',
    250000,
    9550000,
    9300000,
    'COMPLETED',
    DATE_SUB(NOW(), INTERVAL 12 DAY),
    DATE_SUB(NOW(), INTERVAL 12 DAY),
    '선물'
);


-- =========================================================
-- AUTO_INCREMENT 다음 번호 맞추기
-- =========================================================

ALTER TABLE users AUTO_INCREMENT = 3;
ALTER TABLE accounts AUTO_INCREMENT = 4;
ALTER TABLE guardian_relationship AUTO_INCREMENT = 2;
ALTER TABLE transactions AUTO_INCREMENT = 10;


-- 챌린지 mock
INSERT INTO activities (
    activity_id,
    activity_type,
    title,
    description,
    target_value,
    display_order,
    is_active,
    created_at,
    updated_at
) VALUES
      (
          1,
          'COGNITIVE_GAME',
          '오늘의 인지 게임',
          '간단한 인지 활동을 진행해보세요.',
          NULL,
          1,
          TRUE,
          NOW(),
          NOW()
      ),
      (
          2,
          'VOICE_TALK',
          '오늘 이야기 나누기',
          '오늘의 질문에 이야기해보세요.',
          NULL,
          2,
          TRUE,
          NOW(),
          NOW()
      ),
      (
          3,
          'WALKING',
          '오늘 걷기',
          '오늘의 걷기 목표를 달성해보세요.',
          3000,
          3,
          TRUE,
          NOW(),
          NOW()
      );
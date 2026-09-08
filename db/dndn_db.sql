CREATE DATABASE IF NOT EXISTS dndn_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE dndn_db;


-- =========================================================
-- 기존 테이블 삭제
-- =========================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `fds_alert_action`;
DROP TABLE IF EXISTS `fds_risk_detail`;
DROP TABLE IF EXISTS `fds_risk_analysis`;

DROP TABLE IF EXISTS `activity_results`;
DROP TABLE IF EXISTS `activities`;

DROP TABLE IF EXISTS `voice_condition_records`;
DROP TABLE IF EXISTS `interaction_behavior`;
DROP TABLE IF EXISTS `interaction_messages`;

DROP TABLE IF EXISTS `transactions`;

DROP TABLE IF EXISTS `user_condition_baseline`;
DROP TABLE IF EXISTS `interaction_sessions`;

DROP TABLE IF EXISTS `recipient_aliases`;
DROP TABLE IF EXISTS `connection_codes`;
DROP TABLE IF EXISTS `family_posts`;
DROP TABLE IF EXISTS `guardian_relationship`;

DROP TABLE IF EXISTS `risk_accounts`;
DROP TABLE IF EXISTS `accounts`;
DROP TABLE IF EXISTS `users`;

SET FOREIGN_KEY_CHECKS = 1;


-- =========================================================
-- USERS
-- =========================================================

CREATE TABLE `users` (
    `user_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용자 고유 식별자',
    `password` VARCHAR(255) NOT NULL COMMENT '암호화된 비밀번호',
    `name` VARCHAR(50) NOT NULL COMMENT '사용자 이름',
    `role` VARCHAR(20) NOT NULL COMMENT 'SENIOR / GUARDIAN',
    `phone` VARCHAR(20) NOT NULL COMMENT '사용자 연락처',
    `birth_date` DATE NOT NULL COMMENT '사용자 생년월일',
    `status` VARCHAR(20) NOT NULL COMMENT 'ACTIVE / INACTIVE / BLOCKED',
    `created_at` DATETIME NOT NULL COMMENT '계정 생성 시각',
    `updated_at` DATETIME NOT NULL COMMENT '마지막 수정 시각',

    PRIMARY KEY (`user_id`),
    CONSTRAINT `UK_USERS_PHONE`
        UNIQUE (`phone`)
);


-- =========================================================
-- ACCOUNTS
-- =========================================================

CREATE TABLE `accounts` (
    `account_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '계좌 고유 식별자',

    `user_id` BIGINT NULL
        COMMENT '서비스에 등록한 사용자 ID, 미등록 계좌는 NULL',

    `owner_name` VARCHAR(50) NOT NULL
        COMMENT '계좌 소유자 이름',

    `birth_date` DATE NOT NULL
        COMMENT '계좌 소유자 생년월일',

    `bank_code` VARCHAR(10) NOT NULL
        COMMENT '은행 식별 코드',

    `account_number` VARCHAR(30) NOT NULL
        COMMENT 'Mock 계좌번호',

    `account_name` VARCHAR(100) NOT NULL
        COMMENT '사용자에게 표시할 계좌명',

    `balance` BIGINT NOT NULL
        COMMENT '현재 계좌 잔액',

    `is_primary` BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT '대표 계좌 여부',

    `status` VARCHAR(20) NOT NULL
        COMMENT 'ACTIVE / BLOCKED / INACTIVE',

    `created_at` DATETIME NOT NULL
        COMMENT '생성 일시',

    `updated_at` DATETIME NOT NULL
        COMMENT '수정 일시',

    `is_registration` BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT '우리 서비스 등록 여부',

    PRIMARY KEY (`account_id`),

    CONSTRAINT `UK_ACCOUNTS_ACCOUNT_NUMBER`
        UNIQUE (`account_number`)
);


-- =========================================================
-- INTERACTION SESSIONS
-- =========================================================

CREATE TABLE `interaction_sessions` (
    `session_id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '하나의 대화 묶음을 구분하는 고유 ID',

    `user_id` BIGINT NOT NULL
        COMMENT '대화를 진행한 사용자 ID',

    `started_at` DATETIME NOT NULL
        COMMENT '챗봇 대화를 시작한 시각',

    `ended_at` DATETIME NULL
        COMMENT '챗봇 대화를 종료한 시각',

    `created_at` DATETIME NOT NULL
        COMMENT '세션 데이터 생성 시각',

    PRIMARY KEY (`session_id`)
);


-- =========================================================
-- INTERACTION MESSAGES
-- =========================================================

CREATE TABLE `interaction_messages` (
    `message_id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '메시지 고유 ID',

    `session_id` BIGINT NOT NULL
        COMMENT '메시지가 속한 대화 세션 ID',

    `sender_type` VARCHAR(20) NOT NULL
        COMMENT 'USER / ASSISTANT',

    `input_type` VARCHAR(20) NULL
        COMMENT 'CHAT / VOICE, ASSISTANT는 NULL',

    `content` TEXT NOT NULL
        COMMENT '채팅 내용 또는 음성 STT 변환 결과',

    `created_at` DATETIME NOT NULL
        COMMENT '메시지가 생성된 시각',

    PRIMARY KEY (`message_id`)
);


-- =========================================================
-- INTERACTION BEHAVIOR
-- =========================================================

CREATE TABLE `interaction_behavior` (
    `behavior_id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '행동 측정 기록 고유 ID',

    `session_id` BIGINT NOT NULL
        COMMENT '대화 세션 ID',

    `message_id` BIGINT NULL
        COMMENT '특정 메시지와 관련된 기록일 경우 메시지 ID',

    `response_delay_ms` BIGINT NULL
        COMMENT '질문 후 응답 시작까지 걸린 시간',

    `typing_duration_ms` BIGINT NULL
        COMMENT '채팅 메시지 작성에 걸린 시간',

    `edit_count` INT NOT NULL DEFAULT 0
        COMMENT '전송 전 텍스트 수정 횟수',

    `full_delete_count` INT NOT NULL DEFAULT 0
        COMMENT '입력 내용을 전체 삭제한 횟수',

    `typing_pause_count` INT NOT NULL DEFAULT 0
        COMMENT '작성 중 긴 중단이 발생한 횟수',

    `answer_reversal_count` INT NOT NULL DEFAULT 0
        COMMENT '동일 질문에 답변을 번복한 횟수',

    `confusion_count` INT NOT NULL DEFAULT 0
        COMMENT '혼란 표현 탐지 횟수',

    `reexplanation_count` INT NOT NULL DEFAULT 0
        COMMENT '같은 내용을 다시 설명한 횟수',

    `created_at` DATETIME NOT NULL
        COMMENT '기록 생성 시각',

    PRIMARY KEY (`behavior_id`),

    CONSTRAINT `UK_INTERACTION_BEHAVIOR_MESSAGE`
        UNIQUE (`message_id`)
);


-- =========================================================
-- VOICE CONDITION RECORDS
-- =========================================================

CREATE TABLE `voice_condition_records` (
    `voice_condition_id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '음성 상태 기록 고유 ID',

    `message_id` BIGINT NOT NULL
        COMMENT '분석 대상 음성 메시지',

    `measured_at` DATETIME NOT NULL
        COMMENT '음성을 측정한 시각',

    `speech_duration_ms` BIGINT NULL
        COMMENT '실제 사용자가 발화한 시간',

    `speech_rate` DECIMAL(6,2) NULL
        COMMENT '단위 시간당 발화량',

    `avg_pause_duration_ms` BIGINT NULL
        COMMENT '발화 도중 발생한 침묵 구간의 평균 시간',

    `long_pause_count` INT NOT NULL DEFAULT 0
        COMMENT '기준 이상 긴 침묵이 발생한 횟수',

    `pitch_mean` DECIMAL(8,2) NULL
        COMMENT '발화 중 평균 음높이',

    `pitch_variation` DECIMAL(8,2) NULL
        COMMENT '발화 중 음높이 변화 정도',

    `created_at` DATETIME NOT NULL
        COMMENT '기록 생성 시간',

    PRIMARY KEY (`voice_condition_id`),

    CONSTRAINT `UK_VOICE_CONDITION_MESSAGE`
        UNIQUE (`message_id`)
);


-- =========================================================
-- ACTIVITIES
-- =========================================================

CREATE TABLE `activities` (
    `activity_id` BIGINT NOT NULL AUTO_INCREMENT,
    `activity_type` VARCHAR(30) NOT NULL,
    `title` VARCHAR(100) NOT NULL,
    `description` VARCHAR(255) NULL,
    `target_value` INT NULL,
    `display_order` INT NOT NULL,
    `is_active` BOOLEAN NOT NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,

    PRIMARY KEY (`activity_id`)
);


-- =========================================================
-- ACTIVITY RESULTS
-- =========================================================

CREATE TABLE `activity_results` (
    `activity_result_id` BIGINT NOT NULL AUTO_INCREMENT,
    `activity_id` BIGINT NOT NULL,
    `senior_user_id` BIGINT NOT NULL,
    `session_id` BIGINT NULL,
    `activity_date` DATE NOT NULL,
    `status` VARCHAR(20) NOT NULL,
    `score` INT NULL,
    `step_count` INT NULL,
    `started_at` DATETIME NULL,
    `completed_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,

    PRIMARY KEY (`activity_result_id`),

    CONSTRAINT `UK_ACTIVITY_RESULT_DAILY`
        UNIQUE (`senior_user_id`, `activity_id`, `activity_date`)
);


-- =========================================================
-- USER CONDITION BASELINE
-- =========================================================

CREATE TABLE `user_condition_baseline` (
    `baseline_id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '기준값 고유 ID',

    `user_id` BIGINT NOT NULL
        COMMENT '기준값 대상 사용자',

    `avg_response_delay_ms` BIGINT NULL
        COMMENT '평소 응답 시작까지 걸리는 평균 시간',

    `avg_speech_duration_ms` BIGINT NULL
        COMMENT '평소 평균 발화 시간',

    `avg_speech_rate` DECIMAL(6,2) NULL
        COMMENT '평소 평균 발화 속도',

    `avg_pause_duration_ms` BIGINT NULL
        COMMENT '평소 발화 중 평균 침묵시간',

    `avg_long_pause_count` DECIMAL(6,2) NULL
        COMMENT '평소 긴 침묵 횟수 평균',

    `avg_pitch_mean` DECIMAL(8,2) NULL
        COMMENT '평소 평균 Pitch',

    `avg_pitch_variation` DECIMAL(8,2) NULL
        COMMENT '평소 Pitch 변화 정도',

    `avg_typing_duration_ms` BIGINT NULL
        COMMENT '평소 메시지 작성시간',

    `sample_count` INT NOT NULL DEFAULT 0
        COMMENT '기준값 계산에 사용된 측정 횟수',

    `updated_at` DATETIME NOT NULL
        COMMENT '기준값 마지막 갱신 시각',

    PRIMARY KEY (`baseline_id`),

    CONSTRAINT `UK_USER_CONDITION_BASELINE_USER`
        UNIQUE (`user_id`)
);


-- =========================================================
-- TRANSACTIONS
-- =========================================================

CREATE TABLE `transactions` (
    `transaction_id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '거래 고유 식별자',

    `sender_account_id` BIGINT NOT NULL
        COMMENT '돈을 보내는 계좌 ID',

    `receiver_account_id` BIGINT NULL
        COMMENT '수취 계좌가 우리 시스템 계좌인 경우, 외부 계좌면 NULL',

    `session_id` BIGINT NULL
        COMMENT '송금과 연결된 대화 세션 ID',

    `receiver_bank_code` VARCHAR(10) NOT NULL
        COMMENT '수취 계좌 은행 식별 코드',

    `receiver_account_number` VARCHAR(30) NOT NULL
        COMMENT '수취 계좌번호',

    `receiver_name` VARCHAR(50) NOT NULL
        COMMENT '수취 계좌 예금주명',

    `amount` BIGINT NOT NULL
        COMMENT '이체 금액',

    `sender_balance_before` BIGINT NOT NULL
        COMMENT '송금 실행 직전 출금 계좌 잔액',

    `sender_balance_after` BIGINT NULL
        COMMENT '송금 완료 후 출금 계좌 잔액',

    `status` VARCHAR(20) NOT NULL
        COMMENT 'PENDING / COMPLETED / FAILED / CANCELED / HELD',

    `created_at` DATETIME NOT NULL
        COMMENT '거래 요청이 생성된 시각',

    `completed_at` DATETIME NULL
        COMMENT '거래가 완료된 시각',

    `purpose` VARCHAR(255) NOT NULL
        COMMENT '송금 목적',

    PRIMARY KEY (`transaction_id`)
);


-- =========================================================
-- RISK ACCOUNTS
-- =========================================================

CREATE TABLE `risk_accounts` (
    `risk_account_id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '위험 계좌 고유 ID',

    `bank_code` VARCHAR(10) NOT NULL
        COMMENT '은행 식별 코드',

    `account_number` VARCHAR(30) NOT NULL
        COMMENT '위험 계좌번호',

    `risk_type` VARCHAR(30) NOT NULL
        COMMENT 'SUSPECTED / CONFIRMED_FRAUD 등',

    `reason` VARCHAR(255) NULL
        COMMENT '등록 사유',

    `is_active` BOOLEAN NOT NULL DEFAULT TRUE
        COMMENT '현재 위험 계좌 여부',

    PRIMARY KEY (`risk_account_id`),

    CONSTRAINT `UK_RISK_ACCOUNTS_BANK_ACCOUNT`
        UNIQUE (`bank_code`, `account_number`)
);


-- =========================================================
-- RECIPIENT ALIASES
-- =========================================================

CREATE TABLE `recipient_aliases` (
    `alias_id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '별칭 고유 ID',

    `user_id` BIGINT NOT NULL
        COMMENT '별칭을 등록한 사용자',

    `alias_name` VARCHAR(50) NOT NULL
        COMMENT '엄마, 아들, 철수 등 사용자가 부르는 이름',

    `bank_code` VARCHAR(10) NOT NULL
        COMMENT '수취 계좌 은행 코드',

    `account_number` VARCHAR(30) NOT NULL
        COMMENT '수취 계좌번호',

    `recipient_name` VARCHAR(50) NOT NULL
        COMMENT '실제 예금주명',

    `created_at` DATETIME NOT NULL
        COMMENT '별칭 등록 시각',

    PRIMARY KEY (`alias_id`),

    CONSTRAINT `UK_RECIPIENT_ALIASES_USER_ALIAS`
        UNIQUE (`user_id`, `alias_name`)
);


-- =========================================================
-- CONNECTION CODES
-- =========================================================

CREATE TABLE `connection_codes` (
    `connection_code_id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '연결 코드 고유 ID',

    `senior_user_id` BIGINT NOT NULL
        COMMENT '코드를 생성한 시니어 사용자 ID',

    `used_by_guardian_id` BIGINT NULL
        COMMENT '코드를 사용한 보호자 사용자 ID',

    `code` VARCHAR(6) NOT NULL
        COMMENT '보호자 연결용 6자리 코드',

    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        COMMENT 'ACTIVE / USED / EXPIRED',

    `expires_at` DATETIME NOT NULL
        COMMENT '코드 만료 시각',

    `used_at` DATETIME NULL
        COMMENT '코드 사용 완료 시각',

    `created_at` DATETIME NOT NULL
        COMMENT '코드 생성 시각',

    PRIMARY KEY (`connection_code_id`),

    CONSTRAINT `UK_CONNECTION_CODES_CODE`
        UNIQUE (`code`)
);


-- =========================================================
-- GUARDIAN RELATIONSHIP
-- =========================================================

CREATE TABLE `guardian_relationship` (
    `relationship_id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '보호자 관계 고유 ID',

    `senior_user_id` BIGINT NOT NULL
        COMMENT '보호 대상 시니어 사용자 ID',

    `guardian_user_id` BIGINT NOT NULL
        COMMENT '보호자 역할의 사용자 ID',

    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING / ACTIVE / REJECTED / INACTIVE',

    `created_at` DATETIME NOT NULL
        COMMENT '보호자 관계 생성 시각',

    `approved_at` DATETIME NULL
        COMMENT '보호자 연결 승인 시각',

    PRIMARY KEY (`relationship_id`),

    CONSTRAINT `UK_GUARDIAN_RELATIONSHIP`
        UNIQUE (`senior_user_id`, `guardian_user_id`)
);


-- =========================================================
-- FAMILY POSTS
-- =========================================================

CREATE TABLE `family_posts` (
    `family_post_id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '가족 소식 고유 ID',
    `relationship_id` BIGINT NOT NULL
        COMMENT '보호자 관계 고유 ID',
    `target_date` DATE NOT NULL
        COMMENT '소식 공개 기준 날짜',
    `image_url` VARCHAR(500) NOT NULL
        COMMENT '가족이 등록한 사진 URL',
    `message` VARCHAR(500) NULL
        COMMENT '사진과 함께 전달할 메시지',
    `created_at` DATETIME NOT NULL
        COMMENT '소식 등록 시각',
    `viewed_at` DATETIME NULL
        COMMENT '시니어가 소식을 처음 확인한 시각',

    PRIMARY KEY (`family_post_id`),
    INDEX `IDX_FAMILY_POSTS_RELATIONSHIP_DATE`
        (`relationship_id`, `target_date`)
);


-- =========================================================
-- FDS RISK ANALYSIS
-- =========================================================

CREATE TABLE `fds_risk_analysis` (
    `analysis_id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT 'FDS 분석 결과 고유 ID',

    `transaction_id` BIGINT NOT NULL
        COMMENT '분석 대상 거래 ID',

    `transaction_score` INT NOT NULL DEFAULT 0
        COMMENT '거래 금액/잔액/시간 기반 위험 점수',

    `recipient_score` INT NOT NULL DEFAULT 0
        COMMENT '수취 계좌 관련 위험 점수',

    `velocity_score` INT NOT NULL DEFAULT 0
        COMMENT '단기간 반복 거래 관련 위험 점수',

    `device_score` INT NOT NULL DEFAULT 0
        COMMENT '기기/접속 환경 관련 위험 점수',

    `behavior_score` INT NOT NULL DEFAULT 0
        COMMENT '송금 과정 행동 패턴 위험 점수',

    `context_score` INT NOT NULL DEFAULT 0
        COMMENT '대화 내용 기반 위험 점수',

    `condition_score` INT NOT NULL DEFAULT 0
        COMMENT '음성/채팅 상태 변화 기반 위험 점수',

    `total_score` INT NOT NULL DEFAULT 0
        COMMENT '최종 위험 점수, 최대 100점',

    `risk_level` VARCHAR(20) NOT NULL
        COMMENT 'LOW / CAUTION / HIGH / CRITICAL',

    `hard_rule_triggered` BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'Hard Rule 발생 여부',

    `combination_rule_triggered` BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'Combination Rule 발생 여부',

    `analyzed_at` DATETIME NOT NULL
        COMMENT 'FDS 분석 완료 시각',

    PRIMARY KEY (`analysis_id`),

    CONSTRAINT `UK_FDS_RISK_ANALYSIS_TRANSACTION`
        UNIQUE (`transaction_id`)
);


-- =========================================================
-- FDS RISK DETAIL
-- =========================================================

CREATE TABLE `fds_risk_detail` (
    `detail_id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '위험 상세 기록 고유 ID',

    `analysis_id` BIGINT NOT NULL
        COMMENT '소속 FDS 분석 결과 ID',

    `risk_category` VARCHAR(30) NOT NULL
        COMMENT 'TRANSACTION / RECIPIENT / VELOCITY / DEVICE / BEHAVIOR / CONTEXT / CONDITION',

    `rule_code` VARCHAR(50) NOT NULL
        COMMENT '발생한 위험 규칙 식별 코드',

    `score` INT NOT NULL
        COMMENT '해당 규칙으로 추가된 점수',

    `reason` VARCHAR(255) NOT NULL
        COMMENT '해당 위험이 탐지된 구체적인 이유',

    `created_at` DATETIME NOT NULL
        COMMENT '상세 분석 결과 생성 시각',

    PRIMARY KEY (`detail_id`)
);


-- =========================================================
-- FDS ALERT ACTION
-- =========================================================

CREATE TABLE `fds_alert_action` (
    `action_id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT 'FDS 조치 기록 고유 ID',

    `analysis_id` BIGINT NOT NULL
        COMMENT '조치가 발생한 FDS 분석 ID',

    `action_type` VARCHAR(30) NOT NULL
        COMMENT 'WARNING / RECONFIRM / HOLD / EXTRA_AUTH / GUARDIAN_CONFIRM / CANCEL',

    `action_status` VARCHAR(20) NOT NULL
        COMMENT 'PENDING / COMPLETED / CANCELED',

    `reason` VARCHAR(255) NULL
        COMMENT '해당 조치를 수행한 이유',

    `requested_at` DATETIME NOT NULL
        COMMENT '조치가 요청된 시각',

    `completed_at` DATETIME NULL
        COMMENT '조치가 완료된 시각',

    PRIMARY KEY (`action_id`)
);


-- =========================================================
-- FOREIGN KEYS
-- =========================================================

ALTER TABLE `accounts`
ADD CONSTRAINT `FK_USERS_TO_ACCOUNTS`
FOREIGN KEY (`user_id`)
REFERENCES `users` (`user_id`);


ALTER TABLE `interaction_sessions`
ADD CONSTRAINT `FK_USERS_TO_INTERACTION_SESSIONS`
FOREIGN KEY (`user_id`)
REFERENCES `users` (`user_id`);


ALTER TABLE `interaction_messages`
ADD CONSTRAINT `FK_INTERACTION_SESSIONS_TO_MESSAGES`
FOREIGN KEY (`session_id`)
REFERENCES `interaction_sessions` (`session_id`);


ALTER TABLE `interaction_behavior`
ADD CONSTRAINT `FK_INTERACTION_SESSIONS_TO_BEHAVIOR`
FOREIGN KEY (`session_id`)
REFERENCES `interaction_sessions` (`session_id`);


ALTER TABLE `interaction_behavior`
ADD CONSTRAINT `FK_INTERACTION_MESSAGES_TO_BEHAVIOR`
FOREIGN KEY (`message_id`)
REFERENCES `interaction_messages` (`message_id`);


ALTER TABLE `voice_condition_records`
ADD CONSTRAINT `FK_INTERACTION_MESSAGES_TO_VOICE`
FOREIGN KEY (`message_id`)
REFERENCES `interaction_messages` (`message_id`);


ALTER TABLE `activity_results`
ADD CONSTRAINT `FK_ACTIVITIES_TO_ACTIVITY_RESULTS`
FOREIGN KEY (`activity_id`)
REFERENCES `activities` (`activity_id`);


ALTER TABLE `activity_results`
ADD CONSTRAINT `FK_USERS_TO_ACTIVITY_RESULTS`
FOREIGN KEY (`senior_user_id`)
REFERENCES `users` (`user_id`);


ALTER TABLE `activity_results`
ADD CONSTRAINT `FK_INTERACTION_SESSIONS_TO_ACTIVITY_RESULTS`
FOREIGN KEY (`session_id`)
REFERENCES `interaction_sessions` (`session_id`);


ALTER TABLE `user_condition_baseline`
ADD CONSTRAINT `FK_USERS_TO_CONDITION_BASELINE`
FOREIGN KEY (`user_id`)
REFERENCES `users` (`user_id`);


ALTER TABLE `transactions`
ADD CONSTRAINT `FK_ACCOUNTS_TO_TRANSACTIONS_SENDER`
FOREIGN KEY (`sender_account_id`)
REFERENCES `accounts` (`account_id`);


ALTER TABLE `transactions`
ADD CONSTRAINT `FK_ACCOUNTS_TO_TRANSACTIONS_RECEIVER`
FOREIGN KEY (`receiver_account_id`)
REFERENCES `accounts` (`account_id`);


ALTER TABLE `transactions`
ADD CONSTRAINT `FK_INTERACTION_SESSIONS_TO_TRANSACTIONS`
FOREIGN KEY (`session_id`)
REFERENCES `interaction_sessions` (`session_id`);


ALTER TABLE `recipient_aliases`
ADD CONSTRAINT `FK_USERS_TO_RECIPIENT_ALIASES`
FOREIGN KEY (`user_id`)
REFERENCES `users` (`user_id`);


ALTER TABLE `connection_codes`
ADD CONSTRAINT `FK_USERS_TO_CONNECTION_CODES_SENIOR`
FOREIGN KEY (`senior_user_id`)
REFERENCES `users` (`user_id`);


ALTER TABLE `connection_codes`
ADD CONSTRAINT `FK_USERS_TO_CONNECTION_CODES_GUARDIAN`
FOREIGN KEY (`used_by_guardian_id`)
REFERENCES `users` (`user_id`);


ALTER TABLE `guardian_relationship`
ADD CONSTRAINT `FK_USERS_TO_GUARDIAN_RELATIONSHIP_SENIOR`
FOREIGN KEY (`senior_user_id`)
REFERENCES `users` (`user_id`);


ALTER TABLE `guardian_relationship`
ADD CONSTRAINT `FK_USERS_TO_GUARDIAN_RELATIONSHIP_GUARDIAN`
FOREIGN KEY (`guardian_user_id`)
REFERENCES `users` (`user_id`);

ALTER TABLE `family_posts`
ADD CONSTRAINT `FK_GUARDIAN_RELATIONSHIP_TO_FAMILY_POSTS`
FOREIGN KEY (`relationship_id`)
REFERENCES `guardian_relationship` (`relationship_id`);


ALTER TABLE `fds_risk_analysis`
ADD CONSTRAINT `FK_TRANSACTIONS_TO_FDS_RISK_ANALYSIS`
FOREIGN KEY (`transaction_id`)
REFERENCES `transactions` (`transaction_id`);


ALTER TABLE `fds_risk_detail`
ADD CONSTRAINT `FK_FDS_RISK_ANALYSIS_TO_DETAIL`
FOREIGN KEY (`analysis_id`)
REFERENCES `fds_risk_analysis` (`analysis_id`);


ALTER TABLE `fds_alert_action`
ADD CONSTRAINT `FK_FDS_RISK_ANALYSIS_TO_ALERT_ACTION`
FOREIGN KEY (`analysis_id`)
REFERENCES `fds_risk_analysis` (`analysis_id`);

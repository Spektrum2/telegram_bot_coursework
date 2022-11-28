-- liquibase formatted sql

-- changeset aboturlov:1
-- preconditions onFail:MARK_RAN onError:MARK_RAN
-- precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'notification_task'
CREATE TABLE notification_task
(
    id      SERIAL PRIMARY KEY,
    chat_id BIGINT,
    message TEXT,
    date    TIMESTAMP
)
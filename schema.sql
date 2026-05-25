-- Run in pgAdmin Query Tool or via: psql -f schema.sql chatapp
-- PostgreSQL schema for Chat Application

CREATE TABLE users (
    id            SERIAL       PRIMARY KEY,
    username      VARCHAR(50)  UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    DEFAULT NOW()
);

CREATE TABLE messages (
    id          SERIAL    PRIMARY KEY,
    sender_id   INT       REFERENCES users(id),
    receiver_id INT       REFERENCES users(id),   -- NULL = broadcast message
    content     TEXT      NOT NULL,
    sent_at     TIMESTAMP DEFAULT NOW(),
    is_deleted  BOOLEAN   DEFAULT FALSE
);

CREATE TABLE groups (
    id         SERIAL       PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_by INT          REFERENCES users(id),
    created_at TIMESTAMP    DEFAULT NOW()
);

CREATE TABLE group_members (
    group_id INT REFERENCES groups(id),
    user_id  INT REFERENCES users(id),
    PRIMARY KEY (group_id, user_id)
);

CREATE TABLE group_messages (
    id        SERIAL    PRIMARY KEY,
    group_id  INT       REFERENCES groups(id),
    sender_id INT       REFERENCES users(id),
    content   TEXT      NOT NULL,
    sent_at   TIMESTAMP DEFAULT NOW(),
    is_deleted BOOLEAN  DEFAULT FALSE
);

CREATE TABLE user_conversation_clears (
    user_id           INT          REFERENCES users(id) ON DELETE CASCADE,
    conversation_type VARCHAR(10)  NOT NULL, -- 'PUBLIC', 'GROUP', or 'PRIVATE'
    target_id         VARCHAR(50)  NOT NULL, -- 'ALL', group ID, or partner username
    cleared_at        TIMESTAMP    DEFAULT NOW(),
    PRIMARY KEY (user_id, conversation_type, target_id)
);

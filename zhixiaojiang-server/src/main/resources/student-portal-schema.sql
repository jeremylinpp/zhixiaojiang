-- Additive schema: account bindings never alter existing teacher accounts or student records.
CREATE TABLE IF NOT EXISTS student_message (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 student_id BIGINT NOT NULL,
 event_key VARCHAR(120) NOT NULL,
 title VARCHAR(160) NOT NULL,
 content VARCHAR(1000) NOT NULL,
 destination VARCHAR(32) NOT NULL,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 read_at TIMESTAMP NULL,
 UNIQUE(student_id,event_key),
 FOREIGN KEY(student_id) REFERENCES student(id)
);
CREATE TABLE IF NOT EXISTS student_plan_publication (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 plan_id BIGINT NOT NULL,
 version INT NOT NULL,
 title VARCHAR(160) NOT NULL,
 goal VARCHAR(1000) NOT NULL,
 actions VARCHAR(2000) NOT NULL,
 review_on DATE NOT NULL,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(plan_id,version),
 FOREIGN KEY(plan_id) REFERENCES intervention_plan(id)
);
CREATE TABLE IF NOT EXISTS student_plan_execution (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 publication_id BIGINT NOT NULL,
 request_key VARCHAR(80) NOT NULL,
 content VARCHAR(2000) NOT NULL,
 occurred_on DATE NOT NULL,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(publication_id,request_key),
 FOREIGN KEY(publication_id) REFERENCES student_plan_publication(id)
);
CREATE TABLE IF NOT EXISTS student_plan_feedback (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 publication_id BIGINT NOT NULL,
 request_key VARCHAR(80) NOT NULL,
 content VARCHAR(2000) NOT NULL,
 created_by BIGINT NOT NULL,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(publication_id,request_key),
 FOREIGN KEY(publication_id) REFERENCES student_plan_publication(id)
);
CREATE TABLE IF NOT EXISTS student_account (
    user_id BIGINT PRIMARY KEY,
    student_id BIGINT NOT NULL UNIQUE,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    session_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_student_account_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_student_account_student FOREIGN KEY (student_id) REFERENCES student(id)
);

CREATE TABLE IF NOT EXISTS task_submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_task_id BIGINT NOT NULL,
    request_key VARCHAR(80) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    feedback VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP NULL,
    reviewed_by BIGINT,
    UNIQUE (student_task_id, request_key),
    FOREIGN KEY (student_task_id) REFERENCES student_task(id)
);

CREATE TABLE IF NOT EXISTS student_growth_submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    request_key VARCHAR(80) NOT NULL,
    category VARCHAR(20) NOT NULL,
    title VARCHAR(160) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    occurred_on DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    feedback VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP NULL,
    reviewed_by BIGINT,
    UNIQUE (student_id,request_key),
    FOREIGN KEY (student_id) REFERENCES student(id)
);

CREATE TABLE IF NOT EXISTS task_attachment (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 student_task_id BIGINT NOT NULL,
 submission_id BIGINT NULL,
 request_key VARCHAR(80) NOT NULL,
 original_name VARCHAR(180) NOT NULL,
 content_type VARCHAR(80) NOT NULL,
 size_bytes INT NOT NULL,
 sha256 VARCHAR(64) NOT NULL,
 file_bytes MEDIUMBLOB NOT NULL,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(student_task_id,request_key),
 FOREIGN KEY(student_task_id) REFERENCES student_task(id),
 FOREIGN KEY(submission_id) REFERENCES task_submission(id)
);

CREATE TABLE IF NOT EXISTS student_growth_revision (
 previous_id BIGINT PRIMARY KEY,
 replacement_id BIGINT NOT NULL UNIQUE,
 FOREIGN KEY(previous_id) REFERENCES student_growth_submission(id),
 FOREIGN KEY(replacement_id) REFERENCES student_growth_submission(id)
);

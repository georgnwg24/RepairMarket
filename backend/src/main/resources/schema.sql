/*
CREATE TABLE IF NOT EXISTS app_user
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(255)                         NOT NULL UNIQUE,
    password   VARCHAR(255)                         NOT NULL,
    email      VARCHAR(255)                         NOT NULL UNIQUE,
    role       ENUM ('CUSTOMER', 'WORKER', 'ADMIN') NOT NULL,
    first_name VARCHAR(255),
    last_name  VARCHAR(255),
    area       VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS license
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    worker_id   BIGINT                                   NOT NULL,
    filename    VARCHAR(255),
    description VARCHAR(255),
    file        blob,
    status      ENUM ('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    upload_time TIMESTAMP                                         DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (worker_id) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS property
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT       NOT NULL,
    area        VARCHAR(255) NOT NULL,
    address     VARCHAR(255),
    FOREIGN KEY (customer_id) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS job_request
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT                               NOT NULL,
    property_id BIGINT,
    title       VARCHAR(255),
    description VARCHAR(4095),
    category    ENUM (
        'CARPENTRY',
        'ELECTRICAL',
        'PLUMBING',
        'PAINTING',
        'FLOORING',
        'ROOFING',
        'GARDENING',
        'MOVING',
        'RENOVATION',
        'CLEANING',
        'OTHER'
        )                                            NOT NULL,
    status      ENUM ('PENDING', 'ACCEPTED', 'DONE') NOT NULL DEFAULT 'PENDING',
    deadline    DATE,
    created_at  TIMESTAMP                                     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES app_user (id),
    FOREIGN KEY (property_id) REFERENCES property (id)
);

CREATE TABLE job_request_image
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_request_id   BIGINT NOT NULL,
    image            BLOB   NOT NULL,
    image_type       VARCHAR(50),
    display_position INT    NOT NULL,
    FOREIGN KEY (job_request_id) REFERENCES job_request (id)
);

CREATE TABLE IF NOT EXISTS job_offer
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_request_id BIGINT NOT NULL,
    worker_id      BIGINT NOT NULL,
    price          DECIMAL(8, 2),
    comment        VARCHAR(1023),
    is_accepted    BOOLEAN   DEFAULT FALSE,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (job_request_id) REFERENCES job_request (id),
    FOREIGN KEY (worker_id) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS chat
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id    BIGINT NOT NULL,
    worker_id      BIGINT NOT NULL,
    job_request_id BIGINT,
    FOREIGN KEY (customer_id) REFERENCES app_user (id),
    FOREIGN KEY (worker_id) REFERENCES app_user (id),
    FOREIGN KEY (job_request_id) REFERENCES job_request (id)
);

CREATE TABLE IF NOT EXISTS chat_message
(
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_id   BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    message   VARCHAR(4095),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chat_id) REFERENCES chat (id),
    FOREIGN KEY (sender_id) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS rating
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_user_id   BIGINT NOT NULL,
    to_user_id     BIGINT NOT NULL,
    job_request_id BIGINT NOT NULL,
    stars          INT CHECK (stars >= 1 AND stars <= 5),
    comment        VARCHAR(1023),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (from_user_id) REFERENCES app_user (id),
    FOREIGN KEY (to_user_id) REFERENCES app_user (id),
    FOREIGN KEY (job_request_id) REFERENCES job_request (id)
);

CREATE TABLE IF NOT EXISTS report
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    target_id   BIGINT NOT NULL,
    type        ENUM ('JOB_REQUEST', 'MESSAGE'),
    reason      VARCHAR(1023),
    isOpen      BOOLEAN   DEFAULT TRUE,
    reported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reporter_id) REFERENCES app_user (id)
);
*/
/*
job history is not needed as a table, use this query:

SELECT jr.*
FROM job_request jr
         JOIN job_offer jo ON jr.id = jo.job_request_id
WHERE jo.worker_id = :workerId
  AND jo.is_accepted = TRUE
  AND jr.status = 'DONE'
ORDER BY jr.id DESC
*/
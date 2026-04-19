CREATE TABLE tickets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    category VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    author_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_tickets_author FOREIGN KEY (author_id) REFERENCES users (id)
);

CREATE INDEX idx_tickets_author_id ON tickets (author_id);
CREATE INDEX idx_tickets_status ON tickets (status);
CREATE INDEX idx_tickets_priority ON tickets (priority);
CREATE INDEX idx_tickets_category ON tickets (category);


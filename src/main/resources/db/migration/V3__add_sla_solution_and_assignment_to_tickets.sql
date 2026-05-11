ALTER TABLE tickets
    ADD COLUMN sla_deadline DATETIME(6) NULL,
    ADD COLUMN solution TEXT NULL,
    ADD COLUMN assigned_technician_id BIGINT NULL;

ALTER TABLE tickets
    ADD CONSTRAINT fk_tickets_assigned_technician
        FOREIGN KEY (assigned_technician_id) REFERENCES users (id);

CREATE INDEX idx_tickets_assigned_technician_id ON tickets (assigned_technician_id);


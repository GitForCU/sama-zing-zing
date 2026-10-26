ALTER TABLE activities ADD COLUMN evidence_revision INTEGER NOT NULL DEFAULT 0;
CREATE TABLE activity_attachments (
    id UUID PRIMARY KEY,
    activity_id UUID NOT NULL REFERENCES activities(id),
    client_upload_id UUID NOT NULL,
    original_name VARCHAR(200) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK(size_bytes > 0 AND size_bytes <= 5242880),
    sha256 VARCHAR(64) NOT NULL,
    content BYTEA NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL,
    UNIQUE(activity_id, client_upload_id)
);
CREATE INDEX idx_attachment_activity ON activity_attachments(activity_id, uploaded_at);

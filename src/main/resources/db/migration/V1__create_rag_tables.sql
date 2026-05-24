CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE documents (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    file_name  VARCHAR(255) NOT NULL,
    file_type  VARCHAR(10)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE document_chunks (
    id          BIGSERIAL PRIMARY KEY,
    document_id BIGINT  NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    chunk_index INT     NOT NULL,
    content     TEXT    NOT NULL,
    embedding   vector(1536)
);

CREATE INDEX ON document_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

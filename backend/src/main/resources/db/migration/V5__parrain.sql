ALTER TABLE users ADD COLUMN is_parrain BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN parrain_id UUID NULL REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX idx_users_is_parrain ON users(is_parrain);
CREATE INDEX idx_users_parrain_id ON users(parrain_id);

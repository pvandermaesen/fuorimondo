CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    civility VARCHAR(20) NOT NULL,
    birth_date DATE,
    phone VARCHAR(30),
    country VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    role VARCHAR(20) NOT NULL,
    tier_code VARCHAR(20),
    referrer_info TEXT,
    locale VARCHAR(5) NOT NULL DEFAULT 'FR',
    admin_notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_email ON users(email);

CREATE TABLE addresses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    street VARCHAR(255) NOT NULL,
    street_extra VARCHAR(255),
    postal_code VARCHAR(20) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_addresses_user_type ON addresses(user_id, type);

CREATE TABLE invitation_codes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    code VARCHAR(10) NOT NULL UNIQUE,
    generated_at TIMESTAMP NOT NULL,
    generated_by UUID NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP
);

CREATE INDEX idx_invitation_codes_code ON invitation_codes(code);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP
);

CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens(user_id);

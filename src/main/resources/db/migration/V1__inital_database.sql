-- Token
CREATE TABLE refresh_token(
    id SERIAL PRIMARY KEY,
    token VARCHAR(128) NOT NULL UNIQUE,
    user_address VARCHAR NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

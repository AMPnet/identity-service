-- Token
CREATE TABLE refresh_token(
    id SERIAL PRIMARY KEY,
    token VARCHAR(128) NOT NULL UNIQUE,
    user_address NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

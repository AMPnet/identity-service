-- User
CREATE TABLE user_info(
    uuid UUID PRIMARY KEY,
    session_id VARCHAR NOT NULL,
    first_name VARCHAR NOT NULL,
    last_name VARCHAR NOT NULL,
    id_number VARCHAR,
    date_of_birth VARCHAR,
    nationality VARCHAR,
    place_of_birth VARCHAR,
    document_type VARCHAR NOT NULL,
    document_number VARCHAR,
    document_country VARCHAR NOT NULL,
    document_valid_from VARCHAR,
    document_valid_until VARCHAR,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    connected BOOLEAN NOT NULL,
    deactivated BOOLEAN NOT NULL
);
CREATE TABLE app_user(
    address VARCHAR PRIMARY KEY,
    email VARCHAR,
    user_info_uuid UUID REFERENCES user_info(uuid),
    language VARCHAR(8),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Veriff
CREATE TABLE veriff_session(
    id VARCHAR PRIMARY KEY,
    user_address VARCHAR NOT NULL,
    url VARCHAR,
    vendor_data VARCHAR,
    host VARCHAR,
    status VARCHAR,
    connected BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    state INT NOT NULL
);

CREATE TABLE veriff_decision(
    id VARCHAR PRIMARY KEY,
    status VARCHAR NOT NULL,
    code INT NOT NULL,
    reason VARCHAR,
    reason_code INT,
    decision_time VARCHAR,
    acceptance_time VARCHAR,
    created_at TIMESTAMP NOT NULL
);
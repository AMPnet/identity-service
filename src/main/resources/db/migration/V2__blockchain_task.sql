CREATE TABLE blockchain_task(
    uuid UUID PRIMARY KEY,
    payload VARCHAR NOT NULL,
    chain_id BIGINT NOT NULL,
    contract_address VARCHAR NOT NULL,
    status VARCHAR(16) NOT NULL,
    hash VARCHAR,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

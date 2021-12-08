CREATE TABLE faucet_task (
    uuid       UUID PRIMARY KEY,
    addresses  VARCHAR[]                NOT NULL,
    chain_id   BIGINT                   NOT NULL,
    status     VARCHAR(16)              NOT NULL,
    hash       VARCHAR,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_faucet_task_status ON faucet_task(status);
CREATE INDEX idx_faucet_task_chain_id ON faucet_task(chain_id);

CREATE TABLE pending_faucet_address (
    address  VARCHAR NOT NULL,
    chain_id BIGINT  NOT NULL
);

CREATE INDEX idx_pending_faucet_address_chain_id ON pending_faucet_address(chain_id);

CREATE TABLE auto_invest_task (
    uuid                      UUID PRIMARY KEY,
    chain_id                  BIGINT                   NOT NULL,
    user_wallet_address       VARCHAR                  NOT NULL,
    campaign_contract_address VARCHAR                  NOT NULL,
    amount                    NUMERIC(78)              NOT NULL,
    status                    VARCHAR(16)              NOT NULL,
    hash                      VARCHAR,
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT per_user_campaign UNIQUE (chain_id, user_wallet_address, campaign_contract_address)
);

CREATE INDEX idx_auto_invest_task_chain_id_status ON auto_invest_task(chain_id, status);

CREATE TABLE auto_invest_task_history (
    uuid                      UUID PRIMARY KEY,
    chain_id                  BIGINT                   NOT NULL,
    user_wallet_address       VARCHAR                  NOT NULL,
    campaign_contract_address VARCHAR                  NOT NULL,
    amount                    NUMERIC(78)              NOT NULL,
    status                    VARCHAR(16)              NOT NULL,
    hash                      VARCHAR,
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at              TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE auto_invest_transaction (
    uuid       UUID PRIMARY KEY,
    chain_id   BIGINT                   NOT NULL,
    hash       VARCHAR                  NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chain_id_and_hash UNIQUE (chain_id, hash)
);

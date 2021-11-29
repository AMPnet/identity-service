CREATE TABLE auto_invest_task (
    uuid                      UUID PRIMARY KEY,
    chain_id                  BIGINT                   NOT NULL,
    user_wallet_address       VARCHAR                  NOT NULL,
    campaign_contract_address VARCHAR                  NOT NULL,
    amount                    DECIMAL(35, 18)          NOT NULL,
    status                    VARCHAR(16)              NOT NULL,
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT per_user_campaign UNIQUE (chain_id, user_wallet_address, campaign_contract_address, status)
);

CREATE INDEX idx_auto_invest_task_chain_id_status ON auto_invest_task(chain_id, status);
CREATE UNIQUE INDEX idx_auto_invest_per_user_campaign
    ON auto_invest_task(chain_id, user_wallet_address, campaign_contract_address, status);

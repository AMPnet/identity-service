CREATE INDEX idx_blockchain_task_status ON blockchain_task(status);
CREATE INDEX idx_blockchain_task_payload ON blockchain_task(payload);
CREATE INDEX idx_blockchain_task_chain_id ON blockchain_task(chain_id);
CREATE INDEX idx_blockchain_task_contract_address ON blockchain_task(contract_address);

CREATE INDEX idx_mail_token_user_address ON mail_token(user_address);
CREATE INDEX idx_mail_token_created_at ON mail_token(created_at);

CREATE INDEX idx_refresh_token_user_address ON refresh_token(user_address);

CREATE INDEX idx_user_info_session_id ON user_info(session_id);
CREATE INDEX idx_user_info_created_at ON user_info(created_at);

CREATE INDEX idx_app_user_address ON app_user(address);

CREATE INDEX idx_veriff_session_user_address ON veriff_session(user_address);
CREATE INDEX idx_veriff_session_created_at ON veriff_session(created_at);

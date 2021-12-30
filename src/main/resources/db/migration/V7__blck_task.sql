ALTER TABLE faucet_task ADD COLUMN payload VARCHAR;
ALTER TABLE pending_faucet_address ADD COLUMN payload VARCHAR;
DROP TABLE blockchain_task;

ALTER TABLE pending_faucet_address RENAME TO pending_blockchain_address;
ALTER TABLE faucet_task RENAME TO blockchain_task;

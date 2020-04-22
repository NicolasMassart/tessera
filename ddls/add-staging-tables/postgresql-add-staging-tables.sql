CREATE TABLE ST_TRANSACTION (CIPHER_TEXT BYTEA NOT NULL, CIPHER_TEXT_NONCE BYTEA NOT NULL, EXEC_HASH BYTEA, DATA_ISSUES VARCHAR(255), PRIVACY_MODE SMALLINT, RECIPIENT_NONCE BYTEA NOT NULL, SENDER_KEY BYTEA NOT NULL, TIMESTAMP DECIMAL(19), VALIDATION_STAGE BIGINT, HASH VARCHAR(255) NOT NULL, PRIMARY KEY (HASH));
CREATE INDEX ST_TRANSACTION_VALSTG ON ST_TRANSACTION (VALIDATION_STAGE);
CREATE TABLE ST_AFFECTED_TRANSACTION (SECURITY_HASH BYTEA, AFFECTED_HASH VARCHAR(255) NOT NULL, SOURCE_HASH VARCHAR(255) NOT NULL, PRIMARY KEY (AFFECTED_HASH, SOURCE_HASH));
CREATE TABLE ST_TRANSACTION_RECIPIENT (BOX BYTEA, INITIATOR BOOLEAN, HASH VARCHAR(255) NOT NULL, RECIPIENT VARCHAR(255) NOT NULL, PRIMARY KEY (HASH, RECIPIENT));
CREATE TABLE ST_TRANSACTION_VERSION (NANOTIME DECIMAL(19), PAYLOAD BYTEA NOT NULL, PRIVACY_MODE SMALLINT, TIMESTAMP DECIMAL(19), HASH VARCHAR(255) NOT NULL, RECIPIENT VARCHAR(255) NOT NULL, PRIMARY KEY (HASH, RECIPIENT));
ALTER TABLE ST_AFFECTED_TRANSACTION ADD CONSTRAINT FK_ST_AFFECTED_TRANSACTION_SOURCE_HASH FOREIGN KEY (SOURCE_HASH) REFERENCES ST_TRANSACTION (HASH);
ALTER TABLE ST_TRANSACTION_RECIPIENT ADD CONSTRAINT FK_ST_TRANSACTION_RECIPIENT_HASH FOREIGN KEY (HASH) REFERENCES ST_TRANSACTION (HASH);
ALTER TABLE ST_TRANSACTION_VERSION ADD CONSTRAINT FK_ST_TRANSACTION_VERSION_HASH FOREIGN KEY (HASH) REFERENCES ST_TRANSACTION (HASH);
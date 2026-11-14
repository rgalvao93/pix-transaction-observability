CREATE TABLE partner_accounts (
    account_id VARCHAR(255) PRIMARY KEY,
    balance NUMERIC NOT NULL
);

INSERT INTO partner_accounts (account_id, balance) VALUES
    ('acc-123', 1000.00),
    ('acc-789', 500.00);

CREATE TABLE transactions (
    transaction_id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    account_id VARCHAR(255) NOT NULL,
    amount NUMERIC NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE daily_limits (
    account_id VARCHAR(255) NOT NULL,
    limit_date DATE NOT NULL,
    total_amount NUMERIC NOT NULL,
    PRIMARY KEY (account_id, limit_date)
);

CREATE TABLE daily_limit_initialization_lock (
    lock_id INTEGER PRIMARY KEY
);

INSERT INTO daily_limit_initialization_lock (lock_id) VALUES (1);

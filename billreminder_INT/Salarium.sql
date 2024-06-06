CREATE TABLE IF NOT EXISTS client_users (
    id SERIAL PRIMARY KEY,
    fname VARCHAR(255),
    lname VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    money_balance NUMERIC
);
CREATE TABLE IF NOT EXISTS login_history (
    login_id SERIAL PRIMARY KEY,
    id INTEGER REFERENCES client_users(id),
	login_date date DEFAULT CURRENT_DATE, -- automatically filled out
	login_time timestamp DEFAULT CURRENT_TIMESTAMP, --automatically filled out
    login_state BOOLEAN
);
CREATE TABLE IF NOT EXISTS transaction (
    transaction_no SERIAL PRIMARY KEY,
    transaction_id INTEGER,
    id INTEGER REFERENCES client_users(id),
    transaction_date DATE,
    transaction_type VARCHAR(255),
    transaction_category VARCHAR(255),
    transaction_notes TEXT,
    money_type VARCHAR(255),
    transaction_amount NUMERIC
);

CREATE TABLE IF NOT EXISTS billreminders (
    bill_id SERIAL PRIMARY KEY,
    id INT,
    bill_name VARCHAR(100),
    amount DECIMAL(10, 2),
    due_date DATE,
    frequency VARCHAR(20), -- ENUM equivalent in PostgreSQL is VARCHAR
    is_paid BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (id) REFERENCES client_users(id)
);

-- Create the user
CREATE USER test_user WITH PASSWORD 'test_password';

-- Create databases
CREATE DATABASE test_db;

-- Grant privileges on the databases
GRANT ALL PRIVILEGES ON DATABASE test_db TO test_user;

-- Connect to test_db and set schema privileges
\connect test_db
GRANT ALL PRIVILEGES ON SCHEMA public TO test_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO test_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO test_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO test_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO test_user;

CREATE TABLE queue(
  id BIGSERIAL PRIMARY KEY,
  payload BYTEA NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE queue_archive (
  id BIGINT,
  payload BYTEA NOT NULL,
  created_at TIMESTAMP NOT NULL, -- ts the event was originally created at
  processed_at TIMESTAMP NOT NULL DEFAULT NOW() -- ts the event was processed at
)
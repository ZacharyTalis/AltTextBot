CREATE TABLE servers (
    id SERIAL PRIMARY KEY,
    discord_id BIGINT UNIQUE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_At TIMESTAMPTZ NOT NULL
);

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    discord_id BIGINT UNIQUE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE alt_text_contributions (
  id SERIAL PRIMARY KEY,
  server_id INT REFERENCES servers (id) NOT NULL,
  user_id INT REFERENCES users (id) NOT NULL,
  score INT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  last_contribution_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_server_user_contributions ON alt_text_contributions (server_id, user_id);

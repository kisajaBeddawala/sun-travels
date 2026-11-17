CREATE TABLE IF NOT EXISTS hotel (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(150) NOT NULL CHECK (trim(name) <> ''),
    city VARCHAR(120)
);

CREATE TABLE IF NOT EXISTS hotel_contract (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_id BIGINT NOT NULL REFERENCES hotel(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL CHECK (end_date >= start_date),
    markup_pct NUMERIC(7,4) NOT NULL CHECK (markup_pct >= 0),
    currency VARCHAR(3) NOT NULL CHECK (currency = 'USD')
);

CREATE TABLE IF NOT EXISTS contract_room_type (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES hotel_contract(id),
    name VARCHAR(120) NOT NULL CHECK (trim(name) <> ''),
    rate_per_adult_per_night NUMERIC(12,2) NOT NULL CHECK (rate_per_adult_per_night > 0),
    room_count INTEGER NOT NULL CHECK (room_count >= 0),
    max_adults INTEGER NOT NULL CHECK (max_adults > 0)
);

CREATE INDEX IF NOT EXISTS hotel_contract_hotel_idx ON hotel_contract(hotel_id);
CREATE UNIQUE INDEX IF NOT EXISTS contract_room_type_name_idx ON contract_room_type(contract_id, lower(trim(name)));

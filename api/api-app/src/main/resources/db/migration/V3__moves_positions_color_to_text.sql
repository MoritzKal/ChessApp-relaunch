-- Migrate enum-typed color columns to TEXT to avoid Postgres enum coupling
-- moves.color: color -> TEXT
ALTER TABLE moves
    ALTER COLUMN color TYPE TEXT USING color::TEXT;

-- positions.side_to_move: color -> TEXT
ALTER TABLE positions
    ALTER COLUMN side_to_move TYPE TEXT USING side_to_move::TEXT;


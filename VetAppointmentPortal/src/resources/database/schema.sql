CREATE TABLE IF NOT EXISTS Customer (
                                         id INTEGER PRIMARY KEY AUTOINCREMENT,
                                         first_name TEXT NOT NULL,
                                         last_name TEXT NOT NULL,
                                         email TEXT UNIQUE NOT NULL,
                                         phone TEXT UNIQUE,
                                         address TEXT
);

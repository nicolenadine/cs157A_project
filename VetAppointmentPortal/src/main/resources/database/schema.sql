-- Clear existing data for testing
PRAGMA foreign_keys = ON;

DROP TABLE IF EXISTS Pet;
DROP TABLE IF EXISTS Customer;

CREATE TABLE IF NOT EXISTS Customer (
                                         customer_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                         first_name VARCHAR(50) NOT NULL,
                                         last_name VARCHAR(50) NOT NULL,
                                         email VARCHAR(50) UNIQUE NOT NULL,
                                         phone VARCHAR(20) UNIQUE,
                                         address TEXT
);

CREATE TABLE IF NOT EXISTS Pet (
                                        pet_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        pet_name VARCHAR(50)  NOT NULL,
                                        species VARCHAR(50) NOT NULL,
                                        breed VARCHAR(50) NOT NULL,
                                        birth_date DATE NOT NULL,
                                        owner INTEGER NOT NULL,

                                        CONSTRAINT fk_pet_customer
                                            FOREIGN KEY (owner)
                                            REFERENCES Customer(customer_id)
                                            ON DELETE CASCADE
                                            ON UPDATE CASCADE
);




-- Clear existing data for testing
PRAGMA foreign_keys = ON;

DROP TABLE IF EXISTS Pet;
DROP TABLE IF EXISTS Customer;
DROP TABLE IF EXISTS Employee;
DROP TABLE IF EXISTS Appointment;

CREATE TABLE IF NOT EXISTS Customer
(
     customer_id INTEGER PRIMARY KEY AUTOINCREMENT,
     first_name VARCHAR(50) NOT NULL,
     last_name VARCHAR(50) NOT NULL,
     address TEXT NOT NULL,
     phone VARCHAR(20) UNIQUE,
     email VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS Pet
(
    pet_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    pet_name   VARCHAR(50) NOT NULL,
    species    VARCHAR(50) NOT NULL,
    breed      VARCHAR(50) NOT NULL,
    birth_date DATE        NOT NULL,
    owner      INTEGER     NOT NULL,

    FOREIGN KEY (owner) REFERENCES Customer (customer_id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS Employee(
    employee_id INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    address TEXT NOT NULL,
    phone VARCHAR(20) UNIQUE,
    email VARCHAR(50) UNIQUE NOT NULL,
    role VARCHAR(20) NOT NULL CHECK(
        role IN ('RECEPTIONIST', 'VETERINARIAN', 'VET_TECH')
        )
);

CREATE TABLE IF NOT EXISTS Appointment (
    appointment_id INTEGER PRIMARY KEY AUTOINCREMENT ,
    appointment_date DATE NOT NULL,
    time TIME NOT NULL,
    provider INTEGER NOT NULL,
    appointment_type VARCHAR(20) NOT NULL CHECK(
        appointment_type IN ('CHECKUP', 'VACCINATION', 'SURGERY', 'DENTAL', 'EMERGENCY')
    ),
    pet INTEGER NOT NULL,

    FOREIGN KEY (provider) REFERENCES Employee (employee_id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (pet) REFERENCES Pet (pet_id) ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT unique_provider_time UNIQUE (provider, appointment_date, time)
);

CREATE VIEW AppointmentDetailView AS SELECT
    a.appointment_id, date(a.appointment_date) as appointment_date, a.time,
    a.provider, a.appointment_type, a.pet,
    e.employee_id, e.first_name as employee_first_name, e.last_name as employee_last_name,
    e.role, e.address as employee_address, e.phone as employee_phone, e.email as employee_email,
    p.pet_id, p.pet_name, p.species, p.breed, date(p.birth_date) as birth_date, p.owner,
    c.customer_id, c.first_name as customer_first_name, c.last_name as customer_last_name,
    c.address as customer_address, c.phone as customer_phone, c.email as customer_email
FROM Appointment a
         JOIN Employee e ON a.provider = e.employee_id
         JOIN Pet p ON a.pet = p.pet_id
         JOIN Customer c ON p.owner = c.customer_id;






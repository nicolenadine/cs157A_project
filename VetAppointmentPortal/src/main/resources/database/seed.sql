PRAGMA foreign_keys = OFF;

DELETE FROM Pet;
DELETE FROM Customer;
DELETE FROM sqlite_sequence WHERE name = 'Customer';
DELETE FROM sqlite_sequence WHERE name = 'Pet';

PRAGMA foreign_keys = ON;

-- Insert 15 customers
INSERT INTO Customer (first_name, last_name, email, phone, address) VALUES ('Alice', 'Smith', 'alice@example.com', '555-0001', '100 Apple St'), ('Bob', 'Jones', 'bob@example.com', '555-0002', '200 Banana Ave'), ('Carol', 'Lee', 'carol@example.com', '555-0003', '300 Cherry Blvd'), ('David', 'Kim', 'david@example.com', '555-0004', '400 Date Dr'), ('Eve', 'Wong', 'eve@example.com', '555-0005', '500 Elm Ct'), ('Frank', 'Nguyen', 'frank@example.com', '555-0006', '600 Fir St'), ('Grace', 'Hall', 'grace@example.com', '555-0007', '700 Grape Rd'), ('Henry', 'Green', 'henry@example.com', '555-0008', '800 Hazel Ln'), ('Ivy', 'Young', 'ivy@example.com', '555-0009', '900 Ivy Loop'), ('Jack', 'White', 'jack@example.com', '555-0010', '1000 Juniper Blvd'), ('Kara', 'Black', 'kara@example.com', '555-0011', '1100 Kiwi Ct'), ('Leo', 'Brown', 'leo@example.com', '555-0012', '1200 Lime Rd'), ('Mona', 'Davis', 'mona@example.com', '555-0013', '1300 Mango Way'), ('Nina', 'Evans', 'nina@example.com', '555-0014', '1400 Nectarine Dr'), ('Owen', 'Ford', 'owen@example.com', '555-0015', '1500 Orange Ave');

-- Customer 1 (Alice) - has 2 pets
INSERT INTO Pet (pet_name, species, breed, birth_date, owner) VALUES ('Whiskers', 'Cat', 'Siamese', '2020-03-15', 1), ('Shadow', 'Cat', 'Maine Coon', '2019-11-21', 1);

-- Customer 3 (Carol) - has 3 pets
INSERT INTO Pet (pet_name, species, breed, birth_date, owner) VALUES ('Rover', 'Dog', 'Labrador', '2018-07-08', 3), ('Ziggy', 'Parrot', 'African Grey', '2021-05-30', 3), ('Nemo', 'Fish', 'Goldfish', '2022-01-05', 3);

-- Customer 6 (Frank) - has 2 pets
INSERT INTO Pet (pet_name, species, breed, birth_date, owner) VALUES ('Baxter', 'Dog', 'Beagle', '2020-06-22', 6), ('Milo', 'Cat', 'Bengal', '2021-12-14', 6);

-- 10 other customers with 1 pet each
INSERT INTO Pet (pet_name, species, breed, birth_date, owner) VALUES ('Luna', 'Dog', 'Poodle', '2021-04-01', 2);
INSERT INTO Pet (pet_name, species, breed, birth_date, owner) VALUES ('Max', 'Cat', 'Persian', '2020-09-17', 4);
INSERT INTO Pet (pet_name, species, breed, birth_date, owner) VALUES ('Coco', 'Dog', 'Chihuahua', '2019-02-25', 5);
INSERT INTO Pet (pet_name, species, breed, birth_date, owner) VALUES ('Oliver', 'Rabbit', 'Dutch', '2021-07-09', 7);
INSERT INTO Pet (pet_name, species, breed, birth_date, owner) VALUES ('Daisy', 'Dog', 'Golden Retriever', '2022-03-03', 8);
INSERT INTO Pet (pet_name, species, breed, birth_date, owner) VALUES ('Simba', 'Cat', 'Ragdoll', '2020-08-11', 9);
INSERT INTO Pet (pet_name, species, breed, birth_date, owner) VALUES ('Peanut', 'Hamster', 'Syrian', '2022-11-19', 10);
INSERT INTO Pet (pet_name, species, breed, birth_date, owner) VALUES ('Lily', 'Dog', 'Shih Tzu', '2023-01-30', 11);
INSERT INTO Pet (pet_name, species, breed, birth_date, owner) VALUES ('Rocky', 'Dog', 'Bulldog', '2021-10-22', 12);
INSERT INTO Pet (pet_name, species, breed, birth_date, owner) VALUES ('Mochi', 'Cat', 'Scottish Fold', '2021-06-18', 13);
INSERT INTO Pet (pet_name, species, breed, birth_date, owner) VALUES ('Remi', 'Dog', 'Chihuahua', '2015-03-27', 14);

-- Customer 15 (Owen) has no pets
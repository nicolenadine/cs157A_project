# cs157A_project

[📄 View JavaDocs](https://nicolenadine.github.io/cs157A_project/)

# VetPortal Application

## Project Overview
A simple, easy-to-use appointment scheduling system for a veterinarian office.

## Prerequisites
- IDE recommendations:
  - IntelliJ IDEA
  - Eclipse with e(fx)clipse plugin
  - VS Code with Java and Maven extensions
- Java Development Kit (JDK) 17
    - Download from: [Oracle JDK](https://www.oracle.com/java/technologies/downloads/#java17) or [OpenJDK](https://adoptium.net/)
    - Verify installation: `java -version` (should show version 17.x.x)
- JavaFX version 21
- Maven 3.8+
    - Download from: [Maven](https://maven.apache.org/download.cgi)
    - Verify installation: `mvn -version`
    
      If you are using Eclipse:
       - Go to File > Import  
       - Choose Existing Maven Projects  
       - Select your VetAppointmentPortal folder  
       - Eclipse will recognize your pom.xml and load dependencies automatically


## Setup Instructions

### 1. Clone the Repository
```bash
git clone https://github.com/nicolenadine/cs157A_project.git
cd cs157A_project
```
Open VetAppointmentPortal Directory in IDE of choice

### 2. Build the Project
```bash
mvn clean compile
```
This will:
- Download all required dependencies (including JavaFX)
- Compile the project

### 3. Run the Application
```bash
mvn javafx:run
```

## Development Workflow

### Database Configuration
The application uses SQLite for database storage:
1. No external database setup is required
2. Check if cloned versions contains ```VetAppointmentPortal.db``` file 
   - If yes: you may choose to use it as is (no DB initialization required) or delete and create a fresh db initialization (see 'if no' step)
   - If no: the SQLite database file will be created automatically after running ```DatabaseInitializer.java``` found in ```src/main/java/com/vetportal/util```
3. Database schema is defined in `src/resources/database/schema.sql`
4. Sample data is loaded from `src/resources/database/seed.sql`  

**Note** ```DatabaseInitializer.java``` handles both table creation and sample data seeding

### Important Project Structure
- `src/main/java/com/vetportal/` - Java source files
- `src/resources/` - FXML files, CSS, and other resources
- `src/resources/database/` - SQL scripts used during database initialization
- `src/main/java/com/vetportal/exception/` - Two custom exception classes to provide more detailed error handling and 
   distinguish between constraint conflicts due to appointment issues vs. other Database errors.
- `src/main/java/com/vetportal/dao/` - Data Access Objects for database operations
- `src/main/java/com/vetportal/mapper/` - Defines attribute/field mapping between Java Entities and Database Entities 
    - Each table has a corresponding Java class and at least one mapper class 
- `src/main/java/com/vetportal/service/` - Service classes are used by UI Controllers. They provide another level
  of abstraction between the UI layer and the Database access layer by wrapping CRUD functionality inside
  methods that return ServiceResponse objects. ServiceResponse objects contain Optional data values, a response type,
  and message to assist with error handling. 


## Troubleshooting

### Fixes to problems encountered so far
- **"@Override in interfaces are not supported at language level '5'"**: Ensure your Java language level is set to at least 8 in your IDE settings
- **JavaFX Elements Not Found**: Make sure you're using Maven to run the project with `mvn javafx:run`

### Language Level Issues
If you encounter language level issues:
- In IntelliJ: File > Project Structure > Project > Set language level to 17
- In Eclipse: Right-click project > Properties > Java Compiler > Set compiler compliance level to 17

## Project Dependencies
The project automatically manages the following dependencies through Maven:
- JavaFX 17.0.2 (Controls, FXML, etc.)
- sqlite-jdbc 3.44.1.0

Note: You don't need to download JavaFX separately. Maven will download and manage all dependencies.

# Application Screenshots 
(below) Home Screen displays appointments for the selected day (default to current day) and a list of customers/employees
![Screenshot 2025-05-15 at 4 22 58 PM](https://github.com/user-attachments/assets/ddda546a-cd8a-465e-b2cb-6905cecd8e6c)
(below) Appointment creation page enables customer lookup by phone that auto populates customer data (if exists).
Drop down displays the customer's pets and auto populates pet info for selected pet. 
Changes made to customer or pet informaiton automatically updates the database record for those fields upon apppointment creation. 
Date and provider drop downs automatically filter available options based on current selection. Enforces the constraint
that a provder can not be booked twice for the same date and time slot. 
![Screenshot 2025-05-15 at 4 23 29 PM](https://github.com/user-attachments/assets/f849dabf-7ee5-4c86-81b7-5c8f10414fbc)
(below) Filter appointments based on search criteria (date, provider, etc). Select an appointment to edit or delete.
![Screenshot 2025-05-15 at 4 24 26 PM](https://github.com/user-attachments/assets/84917608-5693-449e-8528-6e3d7c8f7714)
(below) Create a new customer profile in the system. Pets are then added from inside the customer profile. 
![Screenshot 2025-05-15 at 4 24 04 PM](https://github.com/user-attachments/assets/4d05ad49-300e-4122-8e54-b12eca29a517)





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
2. The SQLite database file will be created automatically
3. Database schema is defined in `src/resources/database/schema.sql`
4. Sample data is loaded from `src/resources/database/seed.sql`

### Important Project Structure
- `src/main/java/com/vetportal/` - Java source files
- `src/resources/` - FXML files, CSS, and other resources
- `src/resources/database/` - SQL scripts for database setup
- `src/main/java/com/vetportal/dao/` - Data Access Objects for database operations

### Making Changes
1. Create a new branch for your feature: `git checkout -b feature/your-feature-name`
2. Implement your changes
3. Run the application locally to test: `mvn javafx:run`
4. Commit and push your changes
    ```bash 
       git add .
       git commit -m "description of changes"
       git push origin feature/my-feature ```
5. Create a pull request in GH

## Troubleshooting

### Fixes to problems encountered so far
- **"Cannot resolve symbol 'PreparedStatement'"**: Make sure you have the proper import: `import java.sql.PreparedStatement;`
- **"@Override in interfaces are not supported at language level '5'"**: Ensure your Java language level is set to at least 6 in your IDE settings
- **JavaFX Elements Not Found**: Make sure you're using Maven to run the project with `mvn javafx:run`

### Language Level Issues
If you encounter language level issues:
- In IntelliJ: File > Project Structure > Project > Set language level to 17
- In Eclipse: Right-click project > Properties > Java Compiler > Set compiler compliance level to 17

## Project Dependencies
The project automatically manages the following dependencies through Maven:
- JavaFX 17.0.2 (Controls, FXML, etc.)
- [List any other significant dependencies]

Note: You don't need to download JavaFX separately. Maven will download and manage all dependencies.

## Building for Distribution
[Include instructions if you have specific distribution requirements]
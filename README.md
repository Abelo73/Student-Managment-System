# Student Management System Backend

## Overview

The Student Management System is a Spring Boot-based backend application for managing users, students, courses, and enrollments. It provides RESTful APIs for user authentication, profile management, student creation, student listing with pagination and filtering, individual student details retrieval, and system statistics. The application uses JWT for authentication and PostgreSQL for persistent storage. A React-based frontend is also available in the same repository for a complete full-stack experience.

## About Me

- **Name**: Abel Adisu
- **Role**: Java Developer | Fullstack Developer
- **Experience**: 2+ years in Fullstack Development using Spring Boot and React with modern technologies
- **Email**: abeladisu73@gmail.com
- **Phone**: (+251) 934777483
- **Location**: Addis Ababa, Ethiopia
- **GitHub**: [Abelo73](https://github.com/Abelo73)
- **Project Repository**: [Student-Managment-System](https://github.com/Abelo73/Student-Managment-System.git)

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.5.6
- **Spring Data JPA**: For database operations and dynamic queries
- **PostgreSQL**: Relational database for users, courses, and enrollments
- **Spring Security**: For JWT-based authentication and authorization
- **BCrypt**: For password encryption
- **SLF4J**: For logging
- **Maven**: Build and dependency management
- **Postman**: For API testing
- **Lombok**: For reducing boilerplate code in entities
- **React**: Frontend UI (available in the repository)
- **Operating System**: Cross-platform (tested on Windows/Linux via WSL)

## Prerequisites

Ensure the following are installed before running the application:

- **Java 17**: [Download JDK 17](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html)
- **Maven**: [Download Maven](https://maven.apache.org/download.cgi)
- **PostgreSQL**: [Download PostgreSQL](https://www.postgresql.org/download/) (version 13 or later)
- **Postman**: [Download Postman](https://www.postman.com/downloads/) for API testing
- **Git**: [Download Git](https://git-scm.com/downloads) for cloning the repository
- **Node.js** (for frontend): [Download Node.js](https://nodejs.org/) (version 16 or later)

## Downloading and Using the Project

### 1. Clone the Repository
Clone the project from GitHub to your local machine:

```bash
git clone https://github.com/Abelo73/Student-Managment-System.git
cd Student-Managment-System
```

This downloads both the backend (Spring Boot) and frontend (React) components.

### 2. Configure PostgreSQL
1. Start PostgreSQL and ensure it runs on `localhost:5432`.
2. Create a database named `postgres`:
   ```bash
   psql -U postgres
   CREATE DATABASE postgres;
   \q
   ```
3. Verify credentials:
   - Username: `postgres`
   - Password: `postgres`
   - Update `application.properties` if using different credentials.

### 3. Configure the Backend
Navigate to the backend directory (assuming it’s in the root or a subdirectory like `backend`):

```bash
cd backend  # If the backend is in a subdirectory
```

Edit `src/main/resources/application.properties` to match your PostgreSQL setup:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
spring.sql.init.mode=always
jwt.secret=your-secure-secret-key-1234567890
logging.level.com.act.studentmanagementsystem=DEBUG
```

- **jwt.secret**: Replace with a secure, unique key for JWT signing.
- **spring.jpa.hibernate.ddl-auto=update**: Updates database schema based on entities.
- **spring.sql.init.mode=always**: Runs `schema.sql` on startup.

### 4. Initialize Database Schema
Ensure `src/main/resources/schema.sql` contains:

```sql
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    must_change_password BOOLEAN DEFAULT FALSE,
    phone VARCHAR(20),
    gpa DOUBLE PRECISION,
    status VARCHAR(50),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS courses (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    instructor VARCHAR(255),
    max_enrollment INTEGER,
    status VARCHAR(50),
    enrollment_count INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_courses (
    user_id BIGINT REFERENCES users(id),
    course_id BIGINT REFERENCES courses(id),
    PRIMARY KEY (user_id, course_id)
);
```

### 5. Build the Backend
Build the backend using Maven:

```bash
mvn clean install
```

This command:
- Removes old build artifacts (`target` directory).
- Downloads dependencies (e.g., Spring Boot, PostgreSQL driver).
- Compiles the project.

### 6. Run the Backend
Start the Spring Boot application:

```bash
mvn spring-boot:run
```

The backend runs on `http://localhost:8080`. Logs will confirm server startup and schema initialization.

### 7. Run the Frontend (Optional)
If using the React frontend:

1. Navigate to the frontend directory (e.g., `frontend` or `client`):
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the frontend:
   ```bash
   npm start
   ```
4. Access the frontend at `http://localhost:3000` (default React port).
5. Ensure the frontend is configured to communicate with the backend at `http://localhost:8080`.

### 8. Test the APIs
Use Postman to test the backend APIs (see **Testing with Postman** below).

## API Endpoints

All endpoints are under `/api` and require a JWT token in the `Authorization` header (`Bearer <token>`) for authenticated requests.

### Authentication APIs
- **POST /api/auth/register**
  - Description: Register a new user (admin or student).
  - Body: `{ "firstName": "string", "lastName": "string", "email": "string", "password": "string", "role": "ADMIN|STUDENT" }`
  - Response: `200 OK` with success message or `400 Bad Request` (e.g., email exists).
- **POST /api/auth/login**
  - Description: Authenticate a user and return a JWT token.
  - Body: `{ "email": "string", "password": "string" }`
  - Response: `200 OK` with `{ "token": "jwt-token" }` or `401 Unauthorized`.

### User APIs
- **PUT /api/user/profile**
  - Description: Update the authenticated user’s profile.
  - Headers: `Authorization: Bearer <token>`
  - Body: `{ "firstName": "string", "lastName": "string", "phone": "string", "password": "string" }`
  - Response: `200 OK` with success message or `404 Not Found`.
- **GET /api/user/profile**
  - Description: Get the authenticated user’s profile.
  - Headers: `Authorization: Bearer <token>`
  - Response: `200 OK` with user details or `404 Not Found`.
- **POST /api/user/admin/create**
  - Description: Create a new user (admin-only).
  - Headers: `Authorization: Bearer <admin-token>`
  - Body: `{ "firstName": "string", "lastName": "string", "email": "string", "role": "ADMIN|STUDENT", "phone": "string", "gpa": number, "status": "ACTIVE|INACTIVE" }`
  - Response: `200 OK` with success message or `409 Conflict` (email exists).
- **GET /api/user/students**
  - Description: Get paginated students with optional filters (admin-only).
  - Headers: `Authorization: Bearer <admin-token>`
  - Query Params: `page` (default: 0), `size` (default: 10), `search` (name/email), `status`, `minGpa`, `maxGpa`, `courseId`
  - Response: `200 OK` with `{ "students": [user-details], "currentPage": number, "totalItems": number, "totalPages": number }`.
- **GET /api/user/students/{id}**
  - Description: Get details of a specific student by ID (admin-only).
  - Headers: `Authorization: Bearer <admin-token>`
  - Path Param: `id` (student ID)
  - Response: `200 OK` with user details or `404 Not Found`.
- **GET /api/user/stats**
  - Description: Get system statistics (admin-only).
  - Headers: `Authorization: Bearer <admin-token>`
  - Response: `200 OK` with `{ "totalStudents": number, "activeCourses": number, "avgPerformance": number }`.

### Course APIs
- **POST /api/course**
  - Description: Create a new course (admin-only).
  - Headers: `Authorization: Bearer <admin-token>`
  - Body: `{ "name": "string", "description": "string", "instructor": "string", "maxEnrollment": number, "status": "ACTIVE|INACTIVE" }`
- **POST /api/course/enroll**
  - Description: Enroll a student in a course.
  - Headers: `Authorization: Bearer <token>`
  - Body: `{ "courseId": number }`

## Testing with Postman

### Setup
1. **Import Collection and Environment**:
   - Use the Postman collection (artifact ID: `3b036804-cad3-469d-91e2-118e359663c9`) and environment (artifact ID: `db8d043e-9114-4447-b1bb-d70aa42dc9a8`) from the repository or provided files.
   - In Postman, click **Import** > Upload both files.
   - Select **Student Management System Environment** in the dropdown.

2. **Run Requests Sequentially**:
   - **Register Admin**: Creates `admin@example.com`.
   - **Login Admin**: Stores `adminToken`.
   - **Register Student**: Creates `john.doe@student.edu` (GPA: 3.8, e.g., ID: 2).
   - **Create User by Admin**: Creates `jane.smith@student.edu` (GPA: 3.9, e.g., ID: 3).
   - **Create Course**: Creates “Advanced Mathematics” (e.g., ID: 1).
   - **Login Student**: Stores `studentToken`.
   - **Enroll Student**: Enrolls `john.doe@student.edu` in course ID 1.
   - **Get Students**: Tests pagination/filters (e.g., `?page=0&size=2&search=Jane&status=ACTIVE&minGpa=3.5&maxGpa=4.0&courseId=1`).
   - **Get Student by ID**: Add a new request:
     - **Method**: GET
     - **URL**: `{{baseUrl}}/user/students/{{studentId}}`
     - **Headers**: `Authorization: Bearer {{adminToken}}`
     - **Tests**:
       ```javascript
       pm.test("Status code is 200", function () {
           pm.response.to.have.status(200);
       });
       pm.test("Response contains student details", function () {
           var jsonData = pm.response.json();
           pm.expect(jsonData.id).to.eql(pm.environment.get("studentId") * 1);
           pm.expect(jsonData.role).to.eql("STUDENT");
       });
       ```
     - Set `studentId` (e.g., `pm.environment.set("studentId", "2")`) and run.
   - **Logout**: Clears tokens.

3. **Sample Responses**:
   - **Get Students**:
     ```json
     {
       "students": [
         {
           "id": 3,
           "firstName": "Jane",
           "lastName": "Smith",
           "email": "jane.smith@student.edu",
           "phone": "+1 (555) 345-6789",
           "gpa": 3.9,
           "status": "ACTIVE",
           "createdAt": "2025-10-03T11:00:00",
           "role": "STUDENT",
           "courses": ["Advanced Mathematics"]
         }
       ],
       "currentPage": 0,
       "totalItems": 1,
       "totalPages": 1
     }
     ```
   - **Get Student by ID** (ID: 2):
     ```json
     {
       "id": 2,
       "firstName": "John",
       "lastName": "Doe",
       "email": "john.doe@student.edu",
       "phone": "+1 (555) 123-4567",
       "gpa": 3.8,
       "status": "ACTIVE",
       "createdAt": "2025-10-03T12:00:00",
       "role": "STUDENT",
       "courses": ["Advanced Mathematics"]
     }
     ```

4. **Verify Tests**:
   - Open Postman Console (View > Show Postman Console).
   - Ensure tests pass for `id`, `role`, and other fields.
   - Check logs in `logs/app.log` or console.

### Manual Testing
Test the new endpoint with `curl`:
```bash
curl -X GET http://localhost:8080/api/user/students/2 \
-H "Authorization: Bearer <admin-token>" \
-H "Content-Type: application/json"
```

## Troubleshooting

1. **Build Errors**:
   - Verify Java 17 and Maven:
     ```bash
     java -version
     mvn -version
     ```
   - Clear stale artifacts:
     ```bash
     mvn clean
     rm -rf target/
     ```

2. **Database Issues**:
   - Check PostgreSQL:
     ```bash
     psql -U postgres -h localhost
     ```
   - Verify `application.properties` credentials and `schema.sql`.

3. **401 Unauthorized**:
   - Refresh `adminToken` via `Login Admin`.
   - Ensure `jwt.secret` matches in `application.properties` and `JwtUtil.java`.

4. **404 Not Found (Get Student by ID)**:
   - Check student exists:
     ```sql
     SELECT * FROM users WHERE id = 2 AND role = 'STUDENT';
     ```

5. **500 Internal Server Error**:
   - Review logs:
     ```bash
     cat logs/app.log
     ```
   - Verify `UserRepository` extends `JpaSpecificationExecutor<User>`.

6. **Package Name Typos**:
   - Search for `studentmanagmentsystem`:
     ```bash
     grep -r "studentmanagmentsystem" src/main/java
     ```
   - Replace:
     ```bash
     find src/main/java -type f -name "*.java" -exec sed -i 's/com\.act\.studentmanagmentsystem/com.act.studentmanagementsystem/g' {} \;
     ```

## Project Structure

```
Student-Managment-System/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/act/studentmanagementsystem/
│   │   │   │   ├── controller/
│   │   │   │   │   ├── UserController.java
│   │   │   │   │   ├── CourseController.java
│   │   │   │   │   ├── AuthController.java
│   │   │   │   ├── entity/
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Course.java
│   │   │   │   │   ├── Role.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   ├── CourseRepository.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── JwtUtil.java
│   │   │   │   ├── config/
│   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── StudentManagementSystemApplication.java
│   │   │   ├── resources/
│   │   │   │   ├── application.properties
│   │   │   │   ├── schema.sql
│   │   ├── test/
│   ├── pom.xml
│   ├── logs/
│   │   ├── app.log
├── frontend/
│   ├── src/
│   ├── package.json
├── README.md
```

## Contributing

1. Fork the repository: [https://github.com/Abelo73/Student-Managment-System.git](https://github.com/Abelo73/Student-Managment-System.git).
2. Create a feature branch: `git checkout -b feature/new-endpoint`.
3. Commit changes: `git commit -m "Add new endpoint"`.
4. Push: `git push origin feature/new-endpoint`.
5. Submit a pull request.

## Future Enhancements

- Add sorting to `/api/user/students` (e.g., by `gpa`, `firstName`).
- Implement update/delete student endpoints (admin-only).
- Enhance the React frontend with additional features.
- Add activity logging for user actions.
- Enable rate limiting for APIs.

## License

MIT License
# GameRent

GameRent is a peer-to-peer rental platform for video games and gaming hardware.

## Features
- **Renter**: Search items, Book items (with date overlap protection), View Bookings.
- **Owner**: List items, Manage Requests (Approve/Reject), Dashboard.
- **Reviews**: Rate items after booking.
- **Auth**: Basic Auth with Role (RENTER/OWNER).

## Tech Stack
- **Backend**: Spring Boot 3, Java 17/21, Spring Data JPA, Spring Security.
- **Frontend**: React, Vite.
- **Database**: PostgreSQL.
- **Testing**:
  - Unit: JUnit 5, Mockito.
  - BDD: Cucumber.
  - E2E: Playwright.
  - Performance: K6.
  - Analysis: SonarQube.

## How to Run

### Prerequisites
- Docker & Docker Compose
- Java 17+
- Node.js 18+

### Running with Docker
```bash
docker-compose up --build
```
- Backend: http://localhost:8080
- Frontend: http://localhost:3000

### Running Locally (Dev)
1. **Backend**:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
2. **Frontend**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

## Testing
- **Unit/Integration**: `mvn test`
- **BDD**: `mvn test -Dtest=CucumberTest`
- **E2E**: `mvn test -Dtest=PlaywrightIT`
- **Performance**: `k6 run backend/src/test/performance/loadtest.js`

## CI/CD
Configured in `.github/workflows/build.yml` to run tests and SonarQube analysis on push.

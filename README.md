# GameRent

GameRent is a peer-to-peer rental platform for video games and gaming hardware.

## Features
- **Renter**: Search items, Book items (with date overlap protection), View Bookings.
- **User**: List items, Manage Requests (Approve/Reject), Dashboard.
- **Reviews**: Rate items after booking.
- **Auth**: Basic Auth with Role (`USER`/`ADMIN`). Users can both rent and list items.

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

## Payments (Stripe test mode)
GameRent uses **Stripe Checkout in test/sandbox mode**. No real charges are made.

To simulate a successful payment, use Stripe’s test card:
- Card number: `4242 4242 4242 4242`
- Expiry: any future date (e.g., `12/34`)
- CVC: any 3 digits (e.g., `123`)
- ZIP/Postal code: any value

## CI/CD
Configured in `.github/workflows/build.yml` to run tests and SonarQube analysis on push.

## Default users (for development)

The application creates default users on first startup. Their passwords are stored hashed in the database; use the plaintext credentials below to log in during development:

- Demo user (lists items):
   - email: `demo@gamerent.com`
   - password: `password`
   - role: `USER`

- Admin:
   - email: `admin@gamerent.com`
   - password: `adminpass`
   - role: `ADMIN`

Passwords are hashed using BCrypt with appropriate salt before being stored in the PostgreSQL database.

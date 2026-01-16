# TravelPath Backend

Spring Boot backend API for the TravelPath mobile application.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+ (or use H2 for development)
- Eclipse IDE with Spring Tools (STS) plugin

## Setup

### 1. Import into Eclipse

1. Open Eclipse
2. File → Import → Existing Maven Projects
3. Select the `backend` folder
4. Click Finish

### 2. Database Setup

#### Option A: PostgreSQL (Production)
```sql
CREATE DATABASE travelpath_db;
CREATE USER travelpath_user WITH PASSWORD 'travelpath_password';
GRANT ALL PRIVILEGES ON DATABASE travelpath_db TO travelpath_user;
```

#### Option B: H2 (Development - In-Memory)
Uncomment H2 dependencies in `pom.xml` and update `application-dev.properties`

### 3. Environment Variables

Set these environment variables or update `application.properties`:

```bash
export GOOGLE_MAPS_API_KEY=your-key-here
export GOOGLE_PLACES_API_KEY=your-key-here
export WEATHER_API_KEY=your-key-here
```

### 4. Run the Application

#### From Eclipse:
- Right-click `TravelPathApplication.java`
- Run As → Spring Boot App

#### From Command Line:
```bash
mvn spring-boot:run
```

## API Endpoints

The API will be available at: `http://localhost:8080/api`

### Planned Endpoints:
- `GET /api/places/search` - Search nearby places
- `GET /api/places/{id}` - Get place details
- `POST /api/routes/generate` - Generate routes
- `GET /api/routes/{id}` - Get route details
- `GET /api/weather` - Get weather data
- `POST /api/users/register` - User registration
- `POST /api/users/login` - User login

## Project Structure

```
backend/
├── src/main/java/com/travelpath/
│   ├── TravelPathApplication.java
│   ├── controller/        # REST Controllers
│   ├── service/          # Business Logic
│   ├── repository/       # JPA Repositories
│   ├── model/            # Entity Models
│   ├── dto/              # Data Transfer Objects
│   ├── config/           # Configuration Classes
│   └── external/         # External API Clients
├── src/main/resources/
│   ├── application.properties
│   └── application-dev.properties
└── pom.xml
```

## Development

- Use `application-dev.properties` for local development
- Use `application.properties` for production settings
- API keys should be set as environment variables, not in code


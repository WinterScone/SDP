# Database

## Engine

PostgreSQL 16 (via Railway managed plugin in production, Docker Compose locally).

## Connection Configuration

The application reads standard PostgreSQL environment variables:

| Variable     | Default     | Description        |
|-------------|-------------|--------------------|
| `PGHOST`    | `localhost` | Database host      |
| `PGPORT`    | `5432`      | Database port      |
| `PGDATABASE`| `sdp`       | Database name      |
| `PGUSER`    | `sdp`       | Database user      |
| `PGPASSWORD`| `sdp`       | Database password  |

Railway auto-injects these variables when the PostgreSQL plugin is attached to the service.

## Entity Relationship Diagram

```
Admin (1) ──────< Patient (1) ──────< Prescription (N)
                     │                      │
                     │                      └── Medicine (1)
                     │
                     └──────< PatientImage (N)

ActivityLog (standalone — soft FKs to admin/patient)
IntakeHistory ──> Patient, Prescription
```

## Tables

### admin
| Column       | Type         | Constraints              |
|-------------|--------------|--------------------------|
| id          | BIGINT       | PK, auto-increment       |
| username    | VARCHAR(255) | UNIQUE, NOT NULL         |
| password    | VARCHAR(255) | NOT NULL                 |
| is_root     | BOOLEAN      | NOT NULL, default false  |

### patient
| Column          | Type         | Constraints                      |
|----------------|--------------|----------------------------------|
| id             | BIGINT       | PK, auto-increment               |
| username       | VARCHAR(255) | UNIQUE, NOT NULL                 |
| password       | VARCHAR(255) | NOT NULL                         |
| first_name     | VARCHAR(255) | NOT NULL                         |
| last_name      | VARCHAR(255) | NOT NULL                         |
| date_of_birth  | DATE         | NOT NULL                         |
| email          | VARCHAR(255) |                                  |
| phone          | VARCHAR(255) |                                  |
| created_at     | TIMESTAMP    |                                  |
| linked_admin_id| BIGINT       | FK → admin(id)                   |
| face_data      | BYTEA        |                                  |

**Indexes:** `idx_patient_linked_admin` on `linked_admin_id`

### patient_image
| Column       | Type         | Constraints              |
|-------------|--------------|--------------------------|
| id          | BIGINT       | PK, auto-increment       |
| patient_id  | BIGINT       | FK → patient(id), NOT NULL|
| data        | BYTEA        | NOT NULL                 |
| content_type| VARCHAR(255) | NOT NULL                 |
| uploaded_at | TIMESTAMP    |                          |

**Indexes:** `idx_patient_image_patient` on `patient_id`

### medicine
| Column        | Type         | Constraints        |
|--------------|--------------|-------------------|
| medicine_id  | VARCHAR(255) | PK (enum name)    |
| medicine_name| VARCHAR(255) | NOT NULL          |

### prescription
| Column       | Type         | Constraints              |
|-------------|--------------|--------------------------|
| id          | BIGINT       | PK, auto-increment       |
| patient_id  | BIGINT       | FK → patient(id)         |
| medicine_id | VARCHAR(255) | FK → medicine(medicine_id)|
| dosage      | VARCHAR(255) |                          |
| frequency   | VARCHAR(255) |                          |

### intake_history
| Column          | Type         | Constraints              |
|----------------|--------------|--------------------------|
| id             | BIGINT       | PK, auto-increment       |
| patient_id     | BIGINT       | FK → patient(id)         |
| prescription_id| BIGINT       | FK → prescription(id)    |
| taken_at       | TIMESTAMP    |                          |

### activity_log
| Column             | Type         | Constraints        |
|-------------------|--------------|-------------------|
| id                | BIGINT       | PK, auto-increment |
| activity_type     | VARCHAR(255) | NOT NULL           |
| description       | TEXT         | NOT NULL           |
| admin_id          | BIGINT       |                    |
| admin_username    | VARCHAR(255) |                    |
| patient_id        | BIGINT       |                    |
| patient_name      | VARCHAR(255) |                    |
| medicine_name     | VARCHAR(255) |                    |
| additional_details| TEXT         |                    |
| timestamp         | TIMESTAMP    | NOT NULL           |

**Indexes:** `idx_activity_log_timestamp` on `timestamp`, `idx_activity_log_admin` on `admin_id`

## Seed Data

On startup, the application seeds:
- 3 admin users: `root`, `testAdmin1`, `testAdmin2`
- 2 patient users: `testPatient1`, `testPatient2`
- 16 medicines (one per `MedicineType` enum value)
- 2 prescriptions for `testPatient1`

Seeding is idempotent — existing records are skipped.

## Local Development Setup

Start PostgreSQL via Docker Compose:

```bash
docker compose up db -d
```

This starts PostgreSQL on `localhost:5432` with database `sdp`, user `sdp`, password `sdp`.

Then run the Spring Boot app normally — it uses the defaults in `application.properties` which match the Docker Compose config.

## Test Database

Tests use H2 in PostgreSQL compatibility mode (`MODE=PostgreSQL`). Configuration is in `src/test/resources/application.properties`. Schema is created fresh for each test run (`ddl-auto=create-drop`).

## Railway Deployment

1. Attach the PostgreSQL plugin to the Railway service
2. Railway auto-injects `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`
3. No additional configuration needed — the app reads these variables at startup
4. Data persists across deploys (unlike the previous SQLite file-based approach)

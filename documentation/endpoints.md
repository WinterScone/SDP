# API Endpoints

---

## Authentication — Admin (`/api/verify`)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/verify/login` | None | Admin login — sets `adminId`, `adminUsername`, `adminRoot` session cookies |
| `POST` | `/api/verify/logout` | None | Admin logout — clears session cookies |
| `GET` | `/api/verify/me` | Cookie | Check if admin session is valid |
| `POST` | `/api/verify/register` | Root only | Register a new admin account |

**Login request body:**
```json
{ "username": "admin1", "password": "password" }
```

**Register request body:**
```json
{ "username": "admin2", "password": "password", "firstName": "Jane", "lastName": "Doe", "email": "jane@example.com", "phone": "+447911123456" }
```

---

## Authentication — Patient (`/api/patient`)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/patient/login` | None | Patient login |
| `POST` | `/api/patient/signup` | None | Patient signup |

---

## Patients — Admin (`/api/admin`)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `GET` | `/api/admin/patients` | Root only | Get all patients |
| `GET` | `/api/admin/patients/search?q=` | Cookie | Search patients by name, email, or phone |
| `GET` | `/api/admin/patients/{id}` | Cookie | Get patient details including prescriptions |
| `PUT` | `/api/admin/patients/{patientId}/link-admin` | Root only | Assign a patient to an admin |
| `POST` | `/api/admin/reset-database` | Root only | Reset database to seed data |

**Link admin request body:**
```json
{ "adminId": 2 }
```

---

## Prescriptions (`/api/admin`)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/admin/patients/{id}/prescriptions` | Cookie | Create a prescription for a patient — triggers SMS to patient |
| `PUT` | `/api/admin/prescriptions/{id}` | Cookie | Update dosage or frequency of a prescription |
| `DELETE` | `/api/admin/prescriptions/{id}` | Cookie | Delete a prescription |

**Create/update request body:**
```json
{ "medicineId": "VTM01", "dosage": "1000", "frequency": "ONCE_A_DAY" }
```

**Frequency values:** `ONCE_A_DAY` · `TWICE_A_DAY` · `THREE_TIMES_A_DAY` · `FOUR_TIMES_A_DAY`

**Validation:** `dosage` must be a valid integer and a multiple of the medicine's `dosagePerForm`.

---

## Medicines (`/api/medicines`)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `GET` | `/api/medicines` | None | Get all medicines with current stock levels |
| `GET` | `/api/admin/medicines` | Cookie | Get medicine list for prescription dropdown |
| `PATCH` | `/api/medicines/{id}/quantity` | Cookie | Set stock quantity — triggers low-stock SMS alert if below threshold |
| `POST` | `/api/medicines/reduce` | None | Reduce stock by medicine name — used by Raspberry Pi on dispensing |

**Update quantity request body:**
```json
{ "quantity": 50 }
```

**Reduce stock request body:**
```json
{ "medicineName": "Vitamin C", "quantity": 1 }
```

---

## Patient Detail & Prescriptions (`/api/patient`)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `GET` | `/api/patient/getAllPatients` | Cookie | Get all patients visible to the logged-in admin |
| `GET` | `/api/patient/{patientId}/prescriptions` | None | Get a patient's prescriptions |

---

## Intake History (`/api/patient`)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/patient/{patientId}/intake` | None | Log a medication intake for a patient |
| `GET` | `/api/patient/{patientId}/intake` | None | Get full intake history for a patient |

**Log intake request body:**
```json
{ "medicineId": "VTM01", "takenDate": "2026-03-18", "takenTime": "08:15", "notes": "Taken with food" }
```

---

## Activity Logs (`/api/admin/activity-logs`)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `GET` | `/api/admin/activity-logs` | Root only | Get all activity logs |

---

## SMS (`/api/admin/sms`)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/admin/sms/test` | None | Send a one-off SMS to any number |
| `POST` | `/api/admin/sms/reminders` | None | Manually trigger medication reminders based on current server time |
| `POST` | `/api/admin/sms/missed-intakes` | None | Manually trigger missed intake check based on current server time |
| `POST` | `/api/admin/sms/low-stock` | None | Scan all medicines and send low-stock alerts to root admins |

**Test SMS request body:**
```json
{ "to": "+447911123456", "message": "Hello" }
```

The other three require no request body.

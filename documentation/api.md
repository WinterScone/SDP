# SDP Medical API Documentation

Base URL: `https://www.sdpgroup16.com`

All API endpoints are prefixed with `/api`.

---

## Authentication Mechanism

The API uses **cookie-based authentication**. Cookies are set automatically by the login endpoints and must be included in subsequent requests.

### Cookie Table

| Cookie | HttpOnly | Set By | Description |
|--------|----------|--------|-------------|
| `adminId` | Yes | Admin login | Numeric admin ID |
| `adminUsername` | Yes | Admin login | Admin username string |
| `adminRoot` | No | Admin login | `"true"` if the admin is a root admin |
| `patientId` | Yes | Patient login | Numeric patient ID |

### Interceptor

All requests matching `/api/admin/**` are intercepted by `AdminAuthenticationInterceptor`. The interceptor checks for a valid `adminUsername` cookie. If absent or blank, the request is rejected with:

```
HTTP 401
{"ok": false, "message": "Unauthorized"}
```

**Source:** `configuration/AdminAuthenticationInterceptor.java`, registered in `configuration/WebMvcConfig.java`

### Auth Level Definitions

| Level | Meaning |
|-------|---------|
| **None** | No authentication required |
| **Admin** | Requires `adminUsername` cookie (enforced by interceptor for `/api/admin/**` paths) |
| **Root** | Requires Admin auth + `adminRoot` cookie equal to `"true"` |
| **Patient** | Requires `patientId` cookie |
| **Admin or Patient** | Either a valid admin or the specific patient (own data only) |

---

## CORS Configuration

**Source:** `configuration/CorsConfig.java`

| Setting | Value |
|---------|-------|
| Path pattern | `/api/**` |
| Allowed origins | `*` (all) |
| Allowed methods | GET, POST, PUT, PATCH, DELETE, OPTIONS |
| Allowed headers | `*` (all) |
| Exposed headers | `*` (all) |
| Credentials | `false` |

---

## Error Response Format

Most endpoints return errors using this shape:

```json
{
  "ok": false,
  "message": "Human-readable error description"
}
```

Some endpoints use `"error"` instead of `"message"` as the detail key. Both patterns appear in the codebase.

### Status Code Reference

| Status | Meaning |
|--------|---------|
| `200` | Success |
| `201` | Created (no body) |
| `204` | No Content (success, no body) |
| `400` | Bad Request — invalid or missing input |
| `401` | Unauthorized — not logged in or missing cookie |
| `403` | Forbidden — insufficient permissions (e.g. not root) |
| `404` | Not Found — resource does not exist |
| `409` | Conflict — duplicate resource |
| `500` | Internal Server Error |
| `502` | Bad Gateway — upstream service (Python) unreachable |

---

## 1. Admin Authentication (AdminAuthController)

**Source:** `controller/AdminAuthController.java`

**Base path:** `/api/auth/admins`

---

### POST /api/auth/admins/login

**Description:** Authenticate an admin and set session cookies.

**Source:** `controller/AdminAuthController.java` → `login()`

**Auth:** None

**Request body:** `AdminLogin`
```json
{
  "username": "admin",
  "password": "adminpass"
}
```

**Success response (200):** Sets `adminId`, `adminUsername`, `adminRoot` cookies.
```json
{
  "ok": true,
  "id": 1,
  "username": "admin",
  "root": true
}
```

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 400 | Invalid credentials | `{"ok": false}` |

---

### POST /api/auth/admins/logout

**Description:** Clear all admin session cookies.

**Source:** `controller/AdminAuthController.java` → `logout()`

**Auth:** None

**Success response (200):** Empty body. Clears `adminId`, `adminUsername`, `adminRoot` cookies (sets `maxAge=0`).

---

### GET /api/auth/admins/me

**Description:** Return the currently logged-in admin's username.

**Source:** `controller/AdminAuthController.java` → `me()`

**Auth:** None (but checks `adminUsername` cookie manually)

**Success response (200):**
```json
{
  "ok": true,
  "username": "admin"
}
```

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 401 | No `adminUsername` cookie | `{"ok": false, "message": "Unauthorized"}` |

---

### POST /api/auth/admins/register

**Description:** Register a new (non-root) admin account. Only root admins can do this.

**Source:** `controller/AdminAuthController.java` → `register()`

**Auth:** Root (checked manually via `adminUsername` cookie + database lookup)

**Request body:** `AdminRegisterRequest`
```json
{
  "username": "newadmin",
  "password": "securepass",
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "jane@example.com",
  "phone": "0987654321"
}
```

**Success response (200):**
```json
{
  "ok": true,
  "username": "newadmin"
}
```

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 400 | Missing fields or username taken | `{"ok": false, "message": "All fields are required"}` |
| 400 | Username already exists | `{"ok": false, "message": "Username already exists"}` |
| 401 | Not logged in | `{"ok": false, "message": "Unauthorized"}` |
| 403 | Caller is not root | `{"ok": false, "message": "Only root admin can register new admins"}` |

---

## 2. Patient Authentication (PatientAuthController)

**Source:** `controller/PatientAuthController.java`

**Base path:** `/api/auth/patients`

---

### POST /api/auth/patients/login

**Description:** Authenticate a patient and set the `patientId` session cookie.

**Source:** `controller/PatientAuthController.java` → `login()`

**Auth:** None

**Request body:** `PatientLogin`
```json
{
  "username": "patient123",
  "password": "password"
}
```

**Success response (200):** Sets `patientId` cookie.
```json
{
  "ok": true,
  "username": "patient123",
  "patientId": 5
}
```

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 400 | Invalid credentials | `{"ok": false}` |

---

### POST /api/auth/patients/logout

**Description:** Clear the patient session cookie.

**Source:** `controller/PatientAuthController.java` → `logout()`

**Auth:** None

**Success response (200):** Empty body. Clears the `patientId` cookie (sets `maxAge=0`).

---

### POST /api/auth/patients/signup

**Description:** Register a new patient account.

**Source:** `controller/PatientAuthController.java` → `signup()`

**Auth:** None

**Request body:** `PatientSignup`
```json
{
  "username": "newpatient",
  "password": "securepassword",
  "firstName": "John",
  "lastName": "Doe",
  "dateOfBirth": "1990-01-01",
  "email": "john@example.com",
  "phone": "1234567890"
}
```

> `email` and `phone` are optional. `username`, `password`, `firstName`, `lastName`, and `dateOfBirth` are required.

**Success response (200):**
```json
{
  "ok": true,
  "id": 6,
  "username": "newpatient"
}
```

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 400 | Missing required fields | `{"ok": false, "error": "Missing required fields"}` |
| 400 | Username taken | `{"ok": false, "error": "Username already taken"}` |

---

## 3. Admin Patient Management (AdminPatientController)

**Source:** `controller/AdminPatientController.java`

**Base path:** `/api/admin/patients`

All endpoints under `/api/admin/**` require Admin auth (interceptor-enforced).

---

### GET /api/admin/patients

**Description:** List all patients visible to the current admin. Root admins see all patients; non-root admins see only their linked patients.

**Source:** `controller/AdminPatientController.java` → `getAllPatients()`

**Auth:** Admin

**Success response (200):** `List<PatientSummaryDto>`
```json
[
  {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1990-01-01",
    "email": "john@example.com",
    "phone": "1234567890"
  }
]
```

---

### GET /api/admin/patients/assignments

**Description:** List all patients with their admin assignment info. Root-only.

**Source:** `controller/AdminPatientController.java` → `getPatientAssignments()`

**Auth:** Root

**Success response (200):** `List<PatientRow>`
```json
[
  {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1990-01-01",
    "email": "john@example.com",
    "phone": "1234567890",
    "createdAt": "2026-01-15T10:30:00",
    "linkedAdminId": 2,
    "linkedAdminName": "Dr. Smith"
  }
]
```

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 403 | Not root admin | `{"status": 403, "error": "Forbidden", "message": "Only root admin can access this endpoint"}` |

---

### GET /api/admin/patients/search

**Description:** Search patients by query string. Root admins search all patients; non-root admins search only linked patients.

**Source:** `controller/AdminPatientController.java` → `searchPatients()`

**Auth:** Admin

**Query parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `q` | String | No | Search query (matches against patient fields) |

**Success response (200):** `List<PatientViewDto>`
```json
[
  {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1990-01-01",
    "email": "john@example.com",
    "phone": "1234567890",
    "prescriptions": [
      {
        "id": 10,
        "medicineId": "VTM01",
        "medicineName": "Vitamin C",
        "dosage": "1000mg",
        "frequency": "Once daily"
      }
    ]
  }
]
```

---

### GET /api/admin/patients/{id}

**Description:** Get detailed patient info including prescriptions. Admin must be linked to the patient (or be root).

**Source:** `controller/AdminPatientController.java` → `getPatient()`

**Auth:** Admin (linked to patient, or root)

**Path parameters:**

| Name | Type | Description |
|------|------|-------------|
| `id` | Long | Patient ID |

**Success response (200):** `PatientViewDto`
```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "dateOfBirth": "1990-01-01",
  "email": "john@example.com",
  "phone": "1234567890",
  "prescriptions": [
    {
      "id": 10,
      "medicineId": "VTM01",
      "medicineName": "Vitamin C",
      "dosage": "1000mg",
      "frequency": "Once daily"
    }
  ]
}
```

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 403 | Admin not linked to patient | `{"status": 403, "error": "Forbidden", "message": "Access denied to this patient"}` |
| 404 | Patient not found | `{"status": 404, "error": "Not Found", "message": "Patient not found"}` |

---

### GET /api/admin/patients/images

**Description:** Get all patients' usernames paired with their profile image as a Base64 data URI. Root-only.

**Source:** `controller/AdminPatientController.java` → `getAllPatientImages()`

**Auth:** Root

**Success response (200):** `List<PatientImageDto>`
```json
[
  {
    "username": "john",
    "image": "data:image/png;base64,iVBORw0KGgo...",
    "contentType": "image/png"
  },
  {
    "username": "jane",
    "image": null,
    "contentType": null
  }
]
```

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 403 | Not root admin | `{"status": 403, "error": "Forbidden", "message": "Only root admin can access this endpoint"}` |

---

### PUT /api/admin/patients/{patientId}/link-admin

**Description:** Assign (or reassign) a patient to an admin. Root-only.

**Source:** `controller/AdminPatientController.java` → `linkAdminToPatient()`

**Auth:** Root

**Path parameters:**

| Name | Type | Description |
|------|------|-------------|
| `patientId` | Long | Patient ID |

**Request body:** `Map<String, Long>`
```json
{
  "adminId": 2
}
```

**Success response (200):**
```json
{
  "ok": true
}
```

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 400 | Missing `adminId` | `{"error": "adminId is required"}` |
| 403 | Not root admin | `{"status": 403, "error": "Forbidden", "message": "Only root admin can link patients to admins"}` |

---

## 4. Prescription Management (AdminPrescriptionController)

**Source:** `controller/AdminPrescriptionController.java`

**Base path:** `/api/admin`

All endpoints under `/api/admin/**` require Admin auth (interceptor-enforced).

---

### GET /api/admin/medicines

**Description:** List all medicines (ID and name only). Used to populate prescription dropdowns.

**Source:** `controller/AdminPrescriptionController.java` → `listMedicines()`

**Auth:** Admin

**Success response (200):** `List<MedicineViewDto>`
```json
[
  {
    "medicineId": "VTM01",
    "medicineName": "Vitamin C"
  },
  {
    "medicineId": "VTM02",
    "medicineName": "Vitamin E"
  }
]
```

---

### POST /api/admin/patients/{id}/prescriptions

**Description:** Create a new prescription for a patient. Admin must be linked to the patient (or be root). Fails if the patient already has a prescription for the same medicine.

**Source:** `controller/AdminPrescriptionController.java` → `addPrescription()`

**Auth:** Admin (linked to patient, or root)

**Path parameters:**

| Name | Type | Description |
|------|------|-------------|
| `id` | Long | Patient ID |

**Request body:** `PrescriptionCreateDto`
```json
{
  "medicineId": "VTM01",
  "dosage": "1000mg",
  "frequency": "Once daily"
}
```

> `medicineId` is a `MedicineType` enum value (see Appendix).

**Success response:** `201 Created` (no body)

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 400 | Missing required fields | `{"status": 400, "error": "Bad Request", "message": "medicineId, dosage and frequency are required"}` |
| 403 | Admin not linked to patient | `{"status": 403, "error": "Forbidden", "message": "Access denied to this patient"}` |
| 404 | Patient not found | `{"status": 404, "error": "Not Found", "message": "Patient not found"}` |
| 404 | Medicine not found | `{"status": 404, "error": "Not Found", "message": "Medicine not found"}` |
| 409 | Duplicate prescription | `{"status": 409, "error": "Conflict", "message": "Prescription already exists for this medicine"}` |

---

### PUT /api/admin/prescriptions/{id}

**Description:** Update dosage and/or frequency of an existing prescription.

**Source:** `controller/AdminPrescriptionController.java` → `updatePrescription()`

**Auth:** Admin (linked to patient who owns the prescription, or root)

**Path parameters:**

| Name | Type | Description |
|------|------|-------------|
| `id` | Long | Prescription ID |

**Request body:** `PrescriptionUpdateDto`
```json
{
  "dosage": "500mg",
  "frequency": "Twice daily"
}
```

**Success response:** `200 OK` (no body)

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 400 | Missing dosage or frequency | `{"status": 400, "error": "Bad Request", "message": "dosage and frequency are required"}` |
| 403 | Admin not linked to patient | `{"status": 403, "error": "Forbidden", "message": "Access denied to this patient"}` |
| 404 | Prescription not found | `{"status": 404, "error": "Not Found", "message": "Prescription not found"}` |

---

### DELETE /api/admin/prescriptions/{id}

**Description:** Delete a prescription.

**Source:** `controller/AdminPrescriptionController.java` → `deletePrescription()`

**Auth:** Admin (linked to patient who owns the prescription, or root)

**Path parameters:**

| Name | Type | Description |
|------|------|-------------|
| `id` | Long | Prescription ID |

**Success response:** `204 No Content`

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 403 | Admin not linked to patient | `{"status": 403, "error": "Forbidden", "message": "Access denied to this patient"}` |
| 404 | Prescription not found | `{"status": 404, "error": "Not Found", "message": "Prescription not found"}` |

---

## 5. Admin Operations (AdminController)

**Source:** `controller/AdminController.java`

**Base path:** `/api/admin`

All endpoints under `/api/admin/**` require Admin auth (interceptor-enforced).

---

### GET /api/admin/admins

**Description:** List all admin accounts.

**Source:** `controller/AdminController.java` → `getAllAdmins()`

**Auth:** Admin

**Success response (200):** `List<AdminDto>`
```json
[
  {
    "id": 1,
    "username": "admin",
    "firstName": "Admin",
    "lastName": "User",
    "email": "admin@example.com",
    "phone": "0123456789",
    "root": true
  }
]
```

---

### POST /api/admin/reset-database

**Description:** Delete all non-seed admins, patients, and prescriptions. Seed data is preserved. Root-only.

**Source:** `controller/AdminController.java` → `resetDatabase()`

**Auth:** Root

**Success response (200):**
```json
{
  "ok": true,
  "message": "Deleted 3 admins, 5 patients, 12 prescriptions. Seed data preserved."
}
```

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 403 | Not root admin | `{"status": 403, "error": "Forbidden", "message": "Only root admin can reset the database"}` |
| 500 | Reset failed | `{"ok": false, "message": "Failed to reset database: ..."}` |

---

### GET /api/admin/activity-logs

**Description:** Retrieve all activity logs. Root-only.

**Source:** `controller/AdminController.java` → `getAllActivityLogs()`

**Auth:** Root

**Success response (200):** `List<ActivityLogDto>`
```json
[
  {
    "id": 1,
    "activityType": "MEDICINE_QUANTITY_UPDATE",
    "description": "Updated Vitamin C quantity to 150",
    "adminId": 1,
    "adminUsername": "admin",
    "patientId": null,
    "patientName": null,
    "medicineName": "Vitamin C",
    "additionalDetails": null,
    "timestamp": "2026-03-10T14:30:00"
  }
]
```

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 403 | Not root admin | `{"status": 403, "error": "Forbidden", "message": "Only root admin can access activity logs"}` |

---

### POST /api/admin/sms/test

**Description:** Send a test SMS message via the configured SMS service.

**Source:** `controller/AdminController.java` → `sendTestSms()`

**Auth:** Admin

**Request body:** `Map<String, String>`
```json
{
  "to": "+441234567890",
  "message": "Test message"
}
```

**Success response (200):**
```json
{
  "ok": true,
  "message": "SMS sent to +441234567890"
}
```

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 400 | Missing `to` or `message` | `{"ok": false, "error": "Both 'to' and 'message' are required"}` |
| 500 | SMS delivery failed | `{"ok": false, "error": "SMS provider error details..."}` |

---

## 6. Patient Data (PatientController)

**Source:** `controller/PatientController.java`

**Base path:** `/api/patients`

> These endpoints are **not** behind the admin interceptor. They do not enforce authentication at the interceptor level.

---

### GET /api/patients/{patientId}/prescriptions

**Description:** Get all prescriptions for a patient.

**Source:** `controller/PatientController.java` → `getPatientPrescriptions()`

**Auth:** None (endpoint is open; relies on caller having the patient ID)

**Path parameters:**

| Name | Type | Description |
|------|------|-------------|
| `patientId` | Long | Patient ID |

**Success response (200):** `PatientPrescriptionsResponse`
```json
{
  "patientId": 1,
  "prescriptions": [
    {
      "medicineId": "VTM01",
      "medicineName": "Vitamin C",
      "dosage": "1000mg",
      "frequency": "Once daily"
    }
  ]
}
```

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 400 | Patient not found | `{"ok": false, "error": "Patient not found"}` |

---

### POST /api/patients/{patientId}/intake

**Description:** Log a medicine intake event for a patient.

**Source:** `controller/PatientController.java` → `logIntake()`

**Auth:** None

**Path parameters:**

| Name | Type | Description |
|------|------|-------------|
| `patientId` | Long | Patient ID |

**Request body:** `IntakeLogRequest`
```json
{
  "medicineId": "VTM01",
  "takenDate": "2026-03-10",
  "takenTime": "08:30",
  "notes": "Taken with breakfast"
}
```

**Success response (200):**
```json
{
  "ok": true
}
```

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 400 | Invalid input | `{"ok": false, "error": "..."}` |

---

### GET /api/patients/{patientId}/intake

**Description:** Get the full intake history for a patient.

**Source:** `controller/PatientController.java` → `getHistory()`

**Auth:** None

**Path parameters:**

| Name | Type | Description |
|------|------|-------------|
| `patientId` | Long | Patient ID |

**Success response (200):**
```json
{
  "patientId": 1,
  "history": [
    {
      "medicineId": "VTM01",
      "medicineName": "Vitamin C",
      "takenDate": "2026-03-10",
      "takenTime": "08:30",
      "notes": "Taken with breakfast"
    }
  ]
}
```

---

## 7. Patient Images (PatientImageController)

**Source:** `controller/PatientImageController.java`

**Base path:** `/api/patients`

---

### GET /api/patients/{patientId}/images/{imageId}

**Description:** Retrieve a patient's image by ID. Returns binary image data. Authorized for any admin or the patient themselves.

**Source:** `controller/PatientImageController.java` → `getImage()`

**Auth:** Admin or Patient (patient can only access their own images)

**Path parameters:**

| Name | Type | Description |
|------|------|-------------|
| `patientId` | Long | Patient ID |
| `imageId` | Long | Image ID |

**Success response (200):** Binary image data with appropriate `Content-Type` header (e.g. `image/png`, `image/jpeg`).

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 401 | Not authorized (no admin cookie and no matching patient cookie) | `{"ok": false, "message": "Unauthorized"}` |
| 404 | Patient not found | `{"ok": false, "message": "Patient not found"}` |
| 404 | Image not found | `{"ok": false, "message": "Image not found"}` |

---

## 8. Medicine Inventory (MedicineController)

**Source:** `controller/MedicineController.java`

**Base path:** `/api/medicines`

---

### GET /api/medicines

**Description:** List all medicines with full details including current stock quantity.

**Source:** `controller/MedicineController.java` → `getAllMedicines()`

**Auth:** None

**Success response (200):** `List<Medicine>`
```json
[
  {
    "medicineId": "VTM01",
    "medicineName": "Vitamin C",
    "shape": "Tablet",
    "colour": "White",
    "dosagePerForm": 1000,
    "quantity": 100
  },
  {
    "medicineId": "SUP01",
    "medicineName": "Omega-3 Fish Oil",
    "shape": "Capsule",
    "colour": "Clear",
    "dosagePerForm": 1000,
    "quantity": 50
  }
]
```

---

### PATCH /api/medicines/{id}/quantity

**Description:** Set the stock quantity for a medicine to an exact value.

**Source:** `controller/MedicineController.java` → `updateQuantity()`

**Auth:** None (not behind `/api/admin/**` interceptor, but reads admin cookies for activity logging)

**Path parameters:**

| Name | Type | Description |
|------|------|-------------|
| `id` | MedicineType | Medicine enum ID (e.g. `VTM01`, `SUP01`, `MINMG`) |

**Request body:** `Map<String, Integer>`
```json
{
  "quantity": 150
}
```

**Success response (200):**
```json
{
  "ok": true
}
```

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 400 | Quantity is null or negative | `{"ok": false, "message": "Quantity must be >= 0"}` |
| 400 | Medicine not found | `{"ok": false, "message": "Not found"}` |

---

### POST /api/medicines/reduce

**Description:** Reduce a medicine's stock quantity by a given amount (by medicine name, not ID).

**Source:** `controller/MedicineController.java` → `reduceMedicine()`

**Auth:** None

**Request body:** `ReduceMedicineRequest`
```json
{
  "medicineName": "Vitamin C",
  "quantity": 10
}
```

**Success response (200):**
```json
{
  "ok": true
}
```

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 400 | Missing fields | `{"ok": false, "message": "Medicine name and quantity required"}` |
| 400 | Insufficient stock or medicine not found | `{"ok": false, "message": "Not enough stock"}` |

---

## 9. Python Proxy (PythonProxyController)

**Source:** `controller/PythonProxyController.java`

**Base path:** `/api/python`

---

### POST /api/python/verify

**Description:** Forward an image verification request to the external Python service. The server generates a `requestId` (UUID) and translates the payload to snake_case before forwarding.

**Source:** `controller/PythonProxyController.java` → `verify()`

**Auth:** None

**Request body:** `Map<String, Object>` (not a typed DTO at the controller level)
```json
{
  "imageId": 42,
  "imageUrl": "https://example.com/images/42.png",
  "imageAccessToken": "abc123token"
}
```

**Forwarded payload to Python service** (as `PythonVerifyRequest`):
```json
{
  "request_id": "a1b2c3d4-...",
  "image_id": 42,
  "image_url": "https://example.com/images/42.png",
  "image_access_token": "abc123token"
}
```

**Success response (200):** Passes through the Python service's JSON response.

**Error responses:**

| Status | Condition | Sample |
|--------|-----------|--------|
| 400 | Missing required fields | `{"ok": false, "error": "imageId, imageUrl and imageAccessToken are required"}` |
| 400 | `imageId` not a number | `{"ok": false, "error": "imageId must be a number"}` |
| 502 | Python service unreachable or errored | `{"ok": false, "error": "Connection refused"}` |

---

## 10. Health Check (TestPingController)

**Source:** `configuration/TestPingController.java`

---

### GET /api/admin/ping

**Description:** Health check endpoint behind the admin interceptor.

**Source:** `configuration/TestPingController.java` → `adminPing()`

**Auth:** Admin (interceptor-enforced since it's under `/api/admin/**`)

**Success response (200):**
```
pong
```

---

### GET /api/public/ping

**Description:** Public health check endpoint. No authentication required.

**Source:** `configuration/TestPingController.java` → `publicPing()`

**Auth:** None

**Success response (200):**
```
pong-public
```

---

## 11. Error Handling (CustomErrorController)

**Source:** `controller/CustomErrorController.java`

---

### GET /error

**Description:** Catch-all error handler. Forwards all unmatched or errored requests to the static `404.html` page.

**Source:** `controller/CustomErrorController.java` → `handleError()`

**Auth:** None

**Response:** HTML — forwards to `/404.html`.

---

## Appendix: Medicine ID Reference (MedicineType Enum)

**Source:** `enums/MedicineType.java`

16 entries. The `medicineId` field in prescriptions and medicine endpoints uses these enum values.

| Enum ID | Numeric ID | Name | Shape | Colour | Dosage (mg) |
|---------|-----------|------|-------|--------|-------------|
| `VTM01` | 1 | Vitamin C | Tablet | White | 1000 |
| `VTM02` | 2 | Vitamin E | Capsule | Green | 268 |
| `VTM03` | 3 | Vitamin B6 | Tablet | Pale Yellow | 100 |
| `SUP01` | 4 | Omega-3 Fish Oil | Capsule | Clear | 1000 |
| `MINMG` | 5 | Magnesium | Tablet | White | 400 |
| `MINCA` | 6 | Calcium | Tablet | White | 600 |
| `MINZN` | 7 | Zinc | Tablet | Brown | 50 |
| `MINFE` | 8 | Iron | Tablet | Brown | 18 |
| `SUP02` | 9 | Probiotics | Capsule | White | 1000 |
| `SUP03` | 10 | Turmeric | Capsule | Yellow | 500 |
| `SUP04` | 11 | CoQ10 | Capsule | Yellow | 100 |
| `SUP05` | 12 | Ashwagandha | Capsule | Green | 500 |
| `MINK`  | 13 | Potassium | Tablet | Pale Yellow | 2500 |
| `SUP06` | 14 | Ginkgo Biloba | Capsule | Brown | 100 |
| `SUP07` | 15 | Milk Thistle | Capsule | White | 240 |
| `SUP08` | 16 | L-Theanine | Capsule | White | 400 |

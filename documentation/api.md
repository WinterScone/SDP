# SDP Medical API Documentation

Base URL: `https://www.sdpgroup16.com`

All API endpoints are prefixed with `/api`

---

## Authentication

Most endpoints use **cookie-based authentication**. After admin login, three cookies are set automatically:

| Cookie | HttpOnly | Description |
|--------|----------|-------------|
| `adminId` | Yes | Numeric admin ID |
| `adminUsername` | Yes | Admin username |
| `adminRoot` | No | `"true"` if root admin |

Include these cookies in subsequent requests. Endpoints marked **Root** require `adminRoot=true`. Endpoints marked **Admin** require a valid `adminId` cookie.

---

## 1. Admin Authentication

### Login
```
POST /api/verify/login
```

**Request Body:**
```json
{
  "username": "admin",
  "password": "adminpass"
}
```

**Success Response (200):** Sets `adminId`, `adminUsername`, `adminRoot` cookies.
```json
{
  "ok": true,
  "id": 1,
  "username": "admin",
  "root": true
}
```

**Error Response (400):**
```json
{
  "ok": false,
  "error": "Invalid credentials"
}
```

### Logout
```
POST /api/verify/logout
```

**Response (200):** Clears all admin cookies.

### Get Current Admin
```
GET /api/verify/me
```
**Auth:** Admin

**Response (200):**
```json
{
  "ok": true,
  "username": "admin"
}
```

### Register New Admin
```
POST /api/verify/register
```
**Auth:** Root

**Request Body:**
```json
{
  "username": "newadmin",
  "password": "password",
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "jane@example.com",
  "phone": "0987654321"
}
```

**Response (200):**
```json
{
  "ok": true
}
```

---

## 2. Patient Authentication

### Patient Login
```
POST /api/patient/login
```

**Request Body:**
```json
{
  "username": "patient123",
  "password": "password"
}
```

**Success Response (200):**
```json
{
  "ok": true,
  "username": "patient123",
  "message": "Login successful"
}
```

### Patient Signup
```
POST /api/patient/signup
```

**Request Body:**
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

**Success Response (200):**
```json
{
  "ok": true,
  "message": "Signup successful"
}
```

---

## 3. Medicine Management

### Get All Medicines
```
GET /api/medicines
```

**Response (200):**
```json
[
  {
    "medicineId": "VTM01",
    "medicineName": "Vitamin C",
    "shape": "Tablet",
    "colour": "White",
    "dosagePerForm": 1000,
    "quantity": 100
  }
]
```

### Update Medicine Quantity
```
PATCH /api/medicines/{medicineId}/quantity
```
**Auth:** Admin

`{medicineId}` is a `MedicineType` enum value (e.g. `VTM01`, `SUP01`, `MINMG`).

**Request Body:**
```json
{
  "quantity": 150
}
```

**Response (200):**
```json
{
  "ok": true
}
```

### Reduce Medicine Stock
```
POST /api/medicines/reduce
```

Reduces stock quantity by medicine name. Returns error if insufficient stock.

**Request Body:**
```json
{
  "medicineName": "Vitamin C",
  "quantity": 10
}
```

**Response (200):**
```json
{
  "ok": true
}
```

**Error Response (400):**
```json
{
  "ok": false,
  "message": "Not enough stock"
}
```

---

## 4. Patient Details

### Get All Patients (Summary)
```
GET /api/patient/getAllPatients
```
**Auth:** Admin (scoped to linked patients; root sees all)

**Response (200):**
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

### Get Patient Prescriptions
```
GET /api/patient/{patientId}/prescriptions
```

**Response (200):**
```json
{
  "patientId": 1,
  "prescriptions": [
    {
      "id": 1,
      "medicineName": "Vitamin C",
      "dosage": "1000mg",
      "frequency": "Once daily"
    }
  ]
}
```

### Log Medicine Intake
```
POST /api/patient/{patientId}/intake
```

**Request Body:**
```json
{
  "medicineId": "VTM01",
  "takenDate": "2026-03-10",
  "takenTime": "08:30",
  "notes": "Taken with breakfast"
}
```

**Response (200):**
```json
{
  "ok": true
}
```

### Get Intake History
```
GET /api/patient/{patientId}/intake
```

**Response (200):**
```json
{
  "patientId": 1,
  "history": [ ]
}
```

---

## 5. Admin Patient Management

### Search Patients
```
GET /api/admin/patients/search?q={query}
```
**Auth:** Admin (scoped to linked patients; root sees all)

**Response (200):**
```json
[
  {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1990-01-01",
    "email": "john@example.com",
    "phone": "1234567890",
    "prescriptions": []
  }
]
```

### Get Patient Detail
```
GET /api/admin/patients/{id}
```
**Auth:** Admin (must be linked to patient, or root)

**Response (200):**
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
      "id": 1,
      "medicineId": "VTM01",
      "medicineName": "Vitamin C",
      "dosage": "1000mg",
      "frequency": "Once daily"
    }
  ]
}
```

### List All Patients (for Assignment)
```
GET /api/admin/patients
```
**Auth:** Root

**Response (200):**
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

### Get All Patient Images
```
GET /api/admin/patients/images
```
**Auth:** Root

Returns every patient's username paired with their profile image (Base64 data URI). Patients without an image have `null` for `image` and `contentType`.

**Response (200):**
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

### List All Admins
```
GET /api/admin/admins
```
**Auth:** Admin

**Response (200):**
```json
[
  {
    "id": 1,
    "firstName": "Admin",
    "lastName": "User",
    "username": "admin",
    "email": "admin@example.com",
    "root": true
  }
]
```

### Link Patient to Admin
```
PUT /api/admin/patients/{patientId}/link-admin
```
**Auth:** Root

**Request Body:**
```json
{
  "adminId": 2
}
```

**Response (200):**
```json
{
  "ok": true
}
```

### Reset Database
```
POST /api/admin/reset-database
```
**Auth:** Root

Deletes all non-seed admins, patients, and prescriptions.

**Response (200):**
```json
{
  "ok": true,
  "message": "Deleted 3 admins, 5 patients, 12 prescriptions. Seed data preserved."
}
```

---

## 6. Prescription Management

### List Medicines (ID + Name)
```
GET /api/admin/medicines
```
**Auth:** Admin

**Response (200):**
```json
[
  {
    "medicineId": "VTM01",
    "medicineName": "Vitamin C"
  }
]
```

### Create Prescription
```
POST /api/admin/patients/{patientId}/prescriptions
```
**Auth:** Admin (must be linked to patient, or root)

**Request Body:**
```json
{
  "medicineId": "VTM01",
  "dosage": "1000mg",
  "frequency": "Once daily"
}
```

**Response:** `201 Created`

**Error (409):** Prescription already exists for this medicine.

### Update Prescription
```
PUT /api/admin/prescriptions/{prescriptionId}
```
**Auth:** Admin (must be linked to patient, or root)

**Request Body:**
```json
{
  "dosage": "500mg",
  "frequency": "Twice daily"
}
```

**Response:** `200 OK`

### Delete Prescription
```
DELETE /api/admin/prescriptions/{prescriptionId}
```
**Auth:** Admin (must be linked to patient, or root)

**Response:** `204 No Content`

---

## 7. Activity Logs

### Get All Activity Logs
```
GET /api/admin/activity-logs
```
**Auth:** Root

**Response (200):**
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

---

## 8. Health Check

```
GET /api/admin/ping    →  "pong"
GET /api/public/ping   →  "pong-public"
```

No authentication required.

---

## Error Responses

All endpoints may return errors in this format:

```json
{
  "ok": false,
  "error": "Error message here",
  "message": "Additional details"
}
```

| Status | Meaning |
|--------|---------|
| `200` | Success |
| `201` | Created |
| `204` | No Content (success, no body) |
| `400` | Bad Request (invalid data) |
| `401` | Unauthorized (not logged in) |
| `403` | Forbidden (insufficient permissions) |
| `404` | Not Found |
| `409` | Conflict (duplicate resource) |

---

## Medicine ID Reference

The `medicineId` field uses the `MedicineType` enum:

| ID | Name | Shape | Colour | Dosage (mg) |
|----|------|-------|--------|-------------|
| VTM01 | Vitamin C | Tablet | White | 1000 |
| VTM02 | Vitamin E | Capsule | Green | 268 |
| VTM03 | Vitamin B6 | Tablet | Pale Yellow | 100 |
| SUP01 | Omega-3 Fish Oil | Capsule | Clear | 1000 |
| MINMG | Magnesium | Tablet | White | 400 |
| MINCA | Calcium | Tablet | White | 600 |
| MINZN | Zinc | Tablet | Brown | 50 |
| MINFE | Iron | Tablet | Brown | 18 |
| SUP02 | Probiotics | Capsule | White | 1000 |
| SUP03 | Turmeric | Capsule | Yellow | 500 |
| SUP04 | CoQ10 | Capsule | Yellow | 100 |
| SUP05 | Ashwagandha | Capsule | Green | 500 |
| MINK | Potassium | Tablet | Pale Yellow | 2500 |
| SUP06 | Ginkgo Biloba | Capsule | Brown | 100 |
| SUP07 | Milk Thistle | Capsule | White | 240 |
| SUP08 | L-Theanine | Capsule | White | 400 |

---

## CORS

The API supports Cross-Origin Resource Sharing (CORS) and can be accessed from any domain.

**Allowed Methods:** GET, POST, PUT, PATCH, DELETE, OPTIONS

---

## Example Usage

### JavaScript (Fetch API)

```javascript
// Get all medicines
const meds = await fetch('https://www.sdpgroup16.com/api/medicines')
  .then(r => r.json());

// Patient login
const login = await fetch('https://www.sdpgroup16.com/api/patient/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'patient123', password: 'password' })
}).then(r => r.json());

// Reduce medicine stock
await fetch('https://www.sdpgroup16.com/api/medicines/reduce', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ medicineName: 'Vitamin C', quantity: 5 })
});
```

### cURL

```bash
# Get all medicines
curl https://www.sdpgroup16.com/api/medicines

# Patient login
curl -X POST https://www.sdpgroup16.com/api/patient/login \
  -H "Content-Type: application/json" \
  -d '{"username":"patient123","password":"password"}'

# Update medicine quantity (with admin cookies)
curl -X PATCH https://www.sdpgroup16.com/api/medicines/VTM01/quantity \
  -H "Content-Type: application/json" \
  -b "adminId=1; adminUsername=admin; adminRoot=true" \
  -d '{"quantity":200}'

# Reduce medicine stock
curl -X POST https://www.sdpgroup16.com/api/medicines/reduce \
  -H "Content-Type: application/json" \
  -d '{"medicineName":"Vitamin C","quantity":5}'
```

---

## Support

For questions or issues, contact the development team at sdpgroup16.com

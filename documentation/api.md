# SDP Medical API Documentation

Base URL: `https://www.sdpgroup16.com`

All API endpoints are prefixed with `/api`

---

## Authentication

Most endpoints use **cookie-based authentication**. After login, cookies are automatically set and must be included in subsequent requests.

---

## Endpoints

### 1. Medicine Management

#### Get All Medicines
```
GET /api/medicines
```

**Response:**
```json
[
  {
    "medicineId": "PARACETAMOL",
    "medicineName": "Paracetamol",
    "shape": "Tablet",
    "colour": "White",
    "dosagePerForm": 500,
    "quantity": 100
  }
]
```

#### Update Medicine Quantity
```
PATCH /api/medicines/{medicineId}/quantity
```

**Request Body:**
```json
{
  "quantity": 150
}
```

**Response:**
```json
{
  "ok": true
}
```

---

### 2. Patient Authentication

#### Patient Login
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

**Success Response:**
```json
{
  "ok": true,
  "username": "patient123",
  "message": "Login successful"
}
```

**Error Response:**
```json
{
  "ok": false,
  "error": "Invalid credentials"
}
```

#### Patient Signup
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

**Success Response:**
```json
{
  "ok": true,
  "message": "Signup successful"
}
```

---

### 3. Admin Authentication

#### Admin Login
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

**Success Response (sets cookies):**
```json
{
  "ok": true,
  "username": "admin",
  "root": true
}
```

**Cookies Set:**
- `adminUsername` (HttpOnly)
- `adminRoot`

#### Admin Logout
```
POST /api/verify/logout
```

**Response:**
```
200 OK
```

#### Get Current Admin
```
GET /api/verify/me
```

**Requires:** Cookie authentication

**Response:**
```json
{
  "ok": true,
  "username": "admin"
}
```

#### Register New Admin (Root Only)
```
POST /api/verify/register
```

**Requires:** Root admin authentication

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

**Response:**
```json
{
  "ok": true
}
```

---

### 4. Patient Details

#### Get All Patients (Summary)
```
GET /api/patient/getAllPatients
```

**Response:**
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

#### Get Patient Prescriptions
```
GET /api/patient/{patientId}/prescriptions
```

**Response:**
```json
{
  "patientId": 1,
  "prescriptions": [
    {
      "id": 1,
      "medicineName": "Paracetamol",
      "dosage": "500mg",
      "frequency": "Twice daily"
    }
  ]
}
```

---

### 5. Admin-Patient Management

#### Get All Patients (Detailed)
```
GET /api/admin/patients
```

**Requires:** Admin authentication

**Response:**
```json
[
  {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "username": "patient123",
    "linkedAdminId": 2,
    "linkedAdminName": "Dr. Smith"
  }
]
```

#### Get All Admins
```
GET /api/admin/admins
```

**Response:**
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

#### Link Patient to Admin
```
PUT /api/admin/patients/{patientId}/link-admin
```

**Request Body:**
```json
{
  "adminId": 2
}
```

**Response:**
```json
{
  "ok": true
}
```

---

## Error Responses

All endpoints may return error responses in this format:

```json
{
  "ok": false,
  "error": "Error message here",
  "message": "Additional details"
}
```

**Common HTTP Status Codes:**
- `200` - Success
- `400` - Bad Request (invalid data)
- `401` - Unauthorized (not logged in)
- `403` - Forbidden (insufficient permissions)
- `404` - Not Found

---

## CORS

The API supports Cross-Origin Resource Sharing (CORS) and can be accessed from any domain.

**Allowed Methods:** GET, POST, PUT, PATCH, DELETE, OPTIONS

---

## Example Usage

### JavaScript (Fetch API)

```javascript
// Get all medicines
fetch('https://www.sdpgroup16.com/api/medicines')
  .then(response => response.json())
  .then(data => console.log(data));

// Patient login
fetch('https://www.sdpgroup16.com/api/patient/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    username: 'patient123',
    password: 'password'
  })
})
  .then(response => response.json())
  .then(data => console.log(data));
```

### Python (Requests)

```python
import requests

# Get all medicines
response = requests.get('https://www.sdpgroup16.com/api/medicines')
medicines = response.json()
print(medicines)

# Patient login
login_data = {
    'username': 'patient123',
    'password': 'password'
}
response = requests.post(
    'https://www.sdpgroup16.com/api/patient/login',
    json=login_data
)
result = response.json()
print(result)
```

### cURL

```bash
# Get all medicines
curl https://www.sdpgroup16.com/api/medicines

# Patient login
curl -X POST https://www.sdpgroup16.com/api/patient/login \
  -H "Content-Type: application/json" \
  -d '{"username":"patient123","password":"password"}'

# Update medicine quantity
curl -X PATCH https://www.sdpgroup16.com/api/medicines/PARACETAMOL/quantity \
  -H "Content-Type: application/json" \
  -d '{"quantity":200}'
```

---

## Rate Limiting

Currently, there are no rate limits. However, please be respectful and avoid excessive requests.

---

## Support

For questions or issues, contact the development team at sdpgroup16.com

# Activity Logging System

## Overview

The Activity Logging System provides comprehensive audit trails for all administrative actions within the SDP Medical Management System. It tracks and records critical operations performed by administrators, ensuring accountability and transparency.

## Purpose

- **Audit Trail**: Maintain a complete history of all admin actions
- **Compliance**: Meet regulatory requirements for medical record tracking
- **Accountability**: Track which admin performed which action and when
- **Transparency**: Allow root administrators to review all system activities
- **Security**: Monitor for suspicious or unauthorized activities

---

## Architecture

### Database Schema

**Entity**: `ActivityLog`

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key (auto-generated) |
| `activityType` | String | Type of activity (see Activity Types below) |
| `description` | String (TEXT) | Human-readable description of the action |
| `adminId` | Long | ID of the admin who performed the action |
| `adminUsername` | String | Username of the admin |
| `patientId` | Long | ID of affected patient (if applicable) |
| `patientName` | String | Name of affected patient (if applicable) |
| `medicineName` | String | Name of affected medicine (if applicable) |
| `additionalDetails` | String (TEXT) | Additional context and details |
| `timestamp` | LocalDateTime | When the action occurred (auto-generated) |

**Indexes**: The table is queried primarily by timestamp in descending order for efficient retrieval.

### Components

1. **ActivityLog Entity** (`entity/ActivityLog.java`)
   - JPA entity representing logged activities
   - Auto-generates timestamp on creation

2. **ActivityLogRepository** (`repository/ActivityLogRepository.java`)
   - Data access layer
   - Methods: `findAllByOrderByTimestampDesc()`, `deleteAll()`

3. **ActivityLogService** (`service/ActivityLogService.java`)
   - Business logic for logging activities
   - Provides specialized logging methods for each activity type

4. **ActivityLogController** (`controller/ActivityLogController.java`)
   - REST API endpoint for retrieving logs
   - Restricted to root admin access only

5. **Frontend Pages**
   - `activity-logs.html`: Display page with table layout
   - `activity-logs.js`: JavaScript for fetching and rendering logs

---

## Activity Types

### 1. MEDICINE_STOCK_CHANGE
**Triggered when**: Medicine inventory quantity is updated

**Logged Information**:
- Medicine name
- Old quantity
- New quantity
- Admin who made the change

**Example**:
```
Description: "Medicine stock updated: Vitamin C from 100 to 150"
Additional Details: "Old quantity: 100, New quantity: 150"
```

### 2. PRESCRIPTION_CREATED
**Triggered when**: A new prescription is added for a patient

**Logged Information**:
- Patient ID and name
- Medicine name
- Dosage
- Frequency
- Admin who created the prescription

**Example**:
```
Description: "New prescription added for John Doe: Vitamin C (500mg, twice daily)"
Additional Details: "Dosage: 500mg, Frequency: twice daily"
```

### 3. PRESCRIPTION_DELETED
**Triggered when**: A prescription is removed from a patient

**Logged Information**:
- Patient ID and name
- Medicine name
- Dosage (at time of deletion)
- Frequency (at time of deletion)
- Admin who deleted the prescription

**Example**:
```
Description: "Prescription deleted for John Doe: Vitamin C (500mg, twice daily)"
Additional Details: "Dosage: 500mg, Frequency: twice daily"
```

### 4. ADMIN_CREATED
**Triggered when**: Root admin creates a new admin account

**Logged Information**:
- New admin's username
- New admin's full name
- Root admin who created the account

**Example**:
```
Description: "New admin created: john.smith (John Smith)"
Additional Details: "New admin: john.smith (John Smith)"
```

### 5. PATIENT_ASSIGNED
**Triggered when**: A patient is assigned to an admin for the first time

**Logged Information**:
- Patient ID and name
- Assigned admin ID and username
- Root admin who performed the assignment

**Example**:
```
Description: "Patient Jane Doe assigned to admin john.smith"
Additional Details: "Assigned to: john.smith"
```

### 6. PATIENT_REASSIGNED
**Triggered when**: A patient is moved from one admin to another

**Logged Information**:
- Patient ID and name
- Previous admin username
- New admin username
- Root admin who performed the reassignment

**Example**:
```
Description: "Patient Jane Doe reassigned from john.smith to mary.jones"
Additional Details: "Previous admin: john.smith, New admin: mary.jones"
```

### 7. DATABASE_RESET
**Triggered when**: Root admin resets the database to seed data

**Logged Information**:
- Number of admins deleted
- Number of patients deleted
- Number of prescriptions deleted
- Root admin who performed the reset

**Special Behavior**: All previous activity logs are cleared before this log entry is created

**Example**:
```
Description: "Database reset: 3 admins, 5 patients, 12 prescriptions deleted"
Additional Details: "Admins deleted: 3, Patients deleted: 5, Prescriptions deleted: 12"
```

---

## API Endpoint

### Get All Activity Logs

```
GET /api/admin/activity-logs
```

**Authentication**: Root admin only (requires `adminRoot=true` cookie)

**Authorization**:
- Returns `403 Forbidden` if not root admin
- Returns `401 Unauthorized` if not authenticated

**Response**: Array of activity log objects ordered by timestamp (newest first)

```json
[
  {
    "id": 42,
    "activityType": "PRESCRIPTION_CREATED",
    "description": "New prescription added for John Doe: Vitamin C (500mg, twice daily)",
    "adminId": 1,
    "adminUsername": "root",
    "patientId": 5,
    "patientName": "John Doe",
    "medicineName": "Vitamin C",
    "additionalDetails": "Dosage: 500mg, Frequency: twice daily",
    "timestamp": "2025-03-04T14:30:15"
  },
  {
    "id": 41,
    "activityType": "MEDICINE_STOCK_CHANGE",
    "description": "Medicine stock updated: Vitamin C from 100 to 150",
    "adminId": 2,
    "adminUsername": "testAdmin1",
    "patientId": null,
    "patientName": null,
    "medicineName": "Vitamin C",
    "additionalDetails": "Old quantity: 100, New quantity: 150",
    "timestamp": "2025-03-04T14:25:30"
  }
]
```

**Response Fields**:
- `id`: Unique log entry identifier
- `activityType`: One of the activity types listed above
- `description`: Human-readable summary
- `adminId`: ID of admin who performed action (null for system actions)
- `adminUsername`: Username of admin (null for system actions)
- `patientId`: ID of affected patient (null if not applicable)
- `patientName`: Full name of affected patient (null if not applicable)
- `medicineName`: Name of medicine (null if not applicable)
- `additionalDetails`: Additional context information
- `timestamp`: ISO 8601 datetime when action occurred

---

## Frontend Interface

### Accessing Activity Logs

1. Log in as **root admin**
2. Navigate to **Dashboard**
3. Click **Advanced** button
4. Click **Activity Logs** button

### Activity Logs Page Features

**Display**:
- Table view with columns: Timestamp, Activity Type, Description, Admin, Patient, Medicine
- Color-coded activity type badges for visual identification:
  - **Blue**: Medicine-related activities
  - **Green**: Prescription-related activities
  - **Yellow**: Admin-related activities
  - **Purple**: Patient assignment activities
  - **Red**: Database reset activities

**Sorting**: Logs displayed in reverse chronological order (newest first)

**Error Handling**:
- Shows loading message while fetching data
- Displays "No activity logs found" if empty
- Shows error message if API call fails
- Redirects to login page if unauthorized

**Timestamp Format**: `YYYY-MM-DD HH:MM:SS` (24-hour format)

---

## Implementation Details

### Logging Trigger Points

All logging is performed **synchronously** within the same transaction as the main operation. If logging fails, it does not affect the main operation (fail-safe design).

**Integration Points**:

1. **MedicineService.updateQuantity()**: Logs after saving medicine quantity
2. **AdminManagePatientDetailService.createPrescription()**: Logs after saving prescription
3. **AdminManagePatientDetailService.deletePrescription()**: Logs before deleting (to capture prescription details)
4. **AdminLoginService.register()**: Logs after creating admin account
5. **PatientAdminService.linkAdminToPatient()**: Logs after updating patient assignment
6. **DatabaseResetService.resetToSeedData()**: Clears all logs, performs reset, then logs the reset action

### Security Considerations

**Access Control**:
- Only root administrators can view activity logs
- Regular admins cannot access the logs endpoint
- Patients cannot access the logs endpoint
- Enforced at controller level with cookie validation

**Data Privacy**:
- Logs contain minimal personal information (names only)
- No sensitive medical information is logged
- No passwords or authentication tokens are logged

**Audit Trail Integrity**:
- Logs are append-only (no update or delete operations except database reset)
- Timestamps are auto-generated and cannot be manipulated
- All logged actions include the actor (admin) information

### Performance Considerations

**Database**:
- Logs table will grow over time
- Consider periodic archival for production systems
- Index on timestamp for efficient querying

**Query Optimization**:
- Logs fetched once per page load
- Results ordered by database query (not in-memory)
- No pagination currently implemented (suitable for moderate log volumes)

---

## Future Enhancements

Potential improvements for the logging system:

1. **Pagination**: Add pagination for large log datasets
2. **Filtering**: Filter logs by activity type, date range, admin, or patient
3. **Export**: Export logs to CSV or PDF for reporting
4. **Log Retention**: Automatic archival of old logs
5. **Search**: Full-text search across log descriptions
6. **Real-time Updates**: WebSocket support for live log streaming
7. **Analytics**: Dashboard with statistics and charts
8. **Log Levels**: Add severity levels (info, warning, critical)
9. **Email Notifications**: Alert root admin of critical activities
10. **Detailed Audit Reports**: Generate compliance reports

---

## Troubleshooting

### No logs appearing

**Possible causes**:
1. Not logged in as root admin
2. No activities have been performed yet
3. Database reset cleared all logs

**Solution**: Verify root admin login and perform some test actions

### "Unauthorized" error

**Cause**: Not logged in as root admin

**Solution**: Log out and log back in with root admin credentials

### Logs not showing recent actions

**Cause**: Browser cache or page not refreshed

**Solution**: Refresh the page (F5 or Ctrl+R)

---

## Testing the Logging System

### Manual Testing Steps

1. **Log in as root admin** (username: `root`)

2. **Test Medicine Logging**:
   - Go to Medicine page
   - Update any medicine quantity
   - Check Activity Logs page for MEDICINE_STOCK_CHANGE entry

3. **Test Prescription Logging**:
   - Go to Patients page
   - Select a patient
   - Add a new prescription → Check for PRESCRIPTION_CREATED
   - Delete a prescription → Check for PRESCRIPTION_DELETED

4. **Test Admin Logging**:
   - Go to Advanced → New Admin
   - Create a new admin account
   - Check for ADMIN_CREATED entry

5. **Test Patient Assignment Logging**:
   - Go to Advanced → New Patient Assignment
   - Assign a patient to an admin
   - Check for PATIENT_ASSIGNED entry
   - Reassign the same patient to different admin
   - Check for PATIENT_REASSIGNED entry

6. **Test Database Reset Logging**:
   - Note current log count
   - Go to Advanced → Reset Database
   - Confirm reset
   - Check Activity Logs → Should show only DATABASE_RESET entry

---

## Code Examples

### Logging a Custom Activity (for developers)

```java
// In your service class
@Service
public class MyService {
    private final ActivityLogService activityLogService;

    public MyService(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    public void performAction(Long adminId, String adminUsername) {
        // Your business logic here

        // Log the activity
        ActivityLog log = new ActivityLog();
        log.setActivityType("CUSTOM_ACTION");
        log.setDescription("Custom action performed");
        log.setAdminId(adminId);
        log.setAdminUsername(adminUsername);
        activityLogService.repository.save(log);
    }
}
```

---

## Maintenance

### Log Cleanup (if needed)

To manually clear all logs (use with caution):

```sql
DELETE FROM activity_log;
```

Or programmatically:
```java
activityLogService.clearAllLogs();
```

**Note**: This should only be done during database reset operations or for testing purposes.

---

## Compliance

This logging system helps meet:
- **HIPAA**: Audit trail requirements for medical data access
- **GDPR**: Record of processing activities
- **SOC 2**: Access logging and monitoring requirements

**Important**: For production use, ensure logs are:
- Stored securely
- Backed up regularly
- Retained per regulatory requirements
- Protected from unauthorized access

---

## Contact

For questions or issues with the logging system, contact the development team or refer to the main project repository.

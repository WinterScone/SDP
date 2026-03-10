# SMS Notification Feature — TODO

This document tracks the planned SMS notification features to be built on top of the existing Twilio integration.

**Current state:** Twilio SMS works via a manual test endpoint (`POST /api/admin/sms/test`). No automated notifications exist yet.

---

## Scope

| Feature | Recipient | Trigger |
|---|---|---|
| Prescription created SMS | Patient | Admin creates a prescription |
| Medication reminders | Patient | Scheduled, based on prescription frequency (1–3x/day) |
| Low stock alert | Root admin(s) | Medicine quantity drops below threshold (10 units) |

**Future:** Notify linked admin when a patient misses medication intake (no intake logged by expected time).

---

## Phase 1: Standardize Frequency Field

The prescription `frequency` field is currently free-text (e.g. "Twice a day"). It needs to be a standardized enum so the scheduler can reliably determine reminder times.

- [ ] Create `FrequencyType` enum with values: `ONCE_A_DAY`, `TWICE_A_DAY`, `THREE_TIMES_A_DAY`
- [ ] Change `Prescription.frequency` from `String` to `FrequencyType`
- [ ] Update DTOs: `PrescriptionCreateDto`, `PrescriptionUpdateDto`, `PrescriptionViewDto`
- [ ] Update seed data in `SeedPrescription.java`
- [ ] Update `manage-prescriptions.js` — change frequency input to a `<select>` dropdown

### Files to change
- `enums/FrequencyType.java` (new)
- `entity/Prescription.java`
- `dto/PrescriptionCreateDto.java`
- `dto/PrescriptionUpdateDto.java`
- `dto/PrescriptionViewDto.java`
- `configuration/SeedPrescription.java`
- `static/manage-prescriptions.js`

---

## Phase 2: Notification Infrastructure

Build the core notification service that sends SMS asynchronously.

- [ ] Add `@EnableAsync` and `@EnableScheduling` to `SdpClientApplication.java`
- [ ] Create `SmsAsyncService` — wraps `SmsService.sendSms()` with `@Async`
- [ ] Create `NotificationService` with methods:
  - `notifyPatient(Long patientId, String message)` — looks up patient phone, sends SMS
  - `notifyRootAdmins(String message)` — finds root admins, sends SMS to each
  - Rate limiting to prevent duplicate SMS (in-memory, keyed by phone + message type)
- [ ] Add `notification.low-stock-threshold=10` to `application.properties`

### Files to change
- `SdpClientApplication.java`
- `service/SmsAsyncService.java` (new)
- `service/NotificationService.java` (new)
- `application.properties`

---

## Phase 3: Prescription Created SMS

Send an SMS to the patient when an admin creates a new prescription for them.

- [ ] Add `NotificationService` dependency to `AdminManagePatientDetailService`
- [ ] In `createPrescription()`, after the activity log call, send SMS:
  `"New prescription: [Medicine] - [Dosage], [Frequency]"`

### Files to change
- `service/AdminManagePatientDetailService.java`

---

## Phase 4: Low Stock Alert

Send an SMS to root admin(s) when medicine quantity drops below the threshold.

- [ ] Add `NotificationService` dependency to `MedicineService`
- [ ] In `updateQuantity()` and `reduceQuantityByName()`, after saving, check quantity and send alert if below threshold

### Files to change
- `service/MedicineService.java`

---

## Phase 5: Scheduled Medication Reminders

Send recurring SMS reminders to patients based on their prescription frequency.

**Reminder schedule:**
| Frequency | Reminder times |
|---|---|
| Once a day | 08:00 |
| Twice a day | 08:00, 20:00 |
| Three times a day | 08:00, 14:00, 20:00 |

- [ ] Create `MedicationReminderService` with `@Scheduled(cron = "0 0 8,14,20 * * *")`
- [ ] On each run, determine which frequencies match the current hour and query matching prescriptions
- [ ] Send SMS for each: `"Reminder: Time to take your [Medicine] ([Dosage])"`
- [ ] Add `findByFrequencyIn()` query to `PrescriptionRepository`

### Files to change
- `service/MedicationReminderService.java` (new)
- `repository/PrescriptionRepository.java`

---

## Future: Missed Intake Notification

Notify the linked admin if a patient does not log their medication intake by the expected time.

- [ ] After each reminder window (e.g. 2 hours after reminder), check if the patient logged intake
- [ ] If no intake logged, send SMS to `patient.linkedAdminId`'s admin
- [ ] Requires comparing `IntakeHistory` records against the reminder schedule

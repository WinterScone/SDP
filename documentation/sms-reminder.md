# SMS Reminder — Implementation Notes

Documents all SMS notification work completed on 2026-03-18.

---

## Phase 1: Standardize Frequency Field

The `frequency` field on `Prescription` was previously free-text (e.g. `"Twice a day"`). It has been replaced with a typed enum so the scheduler can reliably map frequencies to reminder times.

### New enum — `FrequencyType`
**File:** `enums/FrequencyType.java`

| Value | Reminder times |
|---|---|
| `ONCE_A_DAY` | 08:00 |
| `TWICE_A_DAY` | 08:00, 20:00 |
| `THREE_TIMES_A_DAY` | 08:00, 14:00, 20:00 |
| `FOUR_TIMES_A_DAY` | 08:00, 12:00, 16:00, 20:00 |

### Files changed
- `entity/Prescription.java` — `frequency` changed from `String` to `FrequencyType` with `@Enumerated(EnumType.STRING)`
- `dto/PrescriptionCreateDto.java` — `frequency` changed from `String` to `FrequencyType`
- `dto/PrescriptionUpdateDto.java` — `frequency` changed from `String` to `FrequencyType`
- `dto/PrescriptionViewDto.java` — `frequency` changed from `String` to `FrequencyType`
- `configuration/SeedPrescription.java` — seed data updated to use `FrequencyType` enum values
- `static/manage-prescriptions.html` — frequency `<input>` replaced with a `<select>` dropdown
- `static/manage-prescriptions.js` — inline frequency inputs in the table replaced with `<select>` dropdowns; removed `.trim()` on select values

---

## Phase 2: Notification Infrastructure

Core async SMS layer and notification service used by all subsequent phases.

### `@EnableAsync` + `@EnableScheduling`
**File:** `SdpClientApplication.java`

Both annotations added to the main application class. Without these, `@Async` and `@Scheduled` are no-ops.

### `SmsAsyncService`
**File:** `service/SmsAsyncService.java` *(new)*

Wraps `SmsService.sendSms()` with `@Async` so SMS calls do not block the request thread.

### `NotificationService`
**File:** `service/NotificationService.java` *(new)*

| Method | Behaviour |
|---|---|
| `notifyPatient(Long patientId, String message)` | Looks up patient by ID, reads their phone number, sends SMS asynchronously |
| `notifyRootAdmins(String message)` | Fetches all root admins, sends SMS to each |

**Rate limiting:** In-memory `ConcurrentHashMap` keyed by `phone + ":" + message`. Duplicate sends within a 60-second window are dropped and logged at DEBUG level.

### Other changes
- `repository/AdminRepository.java` — added `findByRootTrue()`
- `application.properties` — added `notification.low-stock-threshold=20`

---

## Phase 3: Prescription Created SMS

When an admin creates a new prescription for a patient, an SMS is sent to the patient listing **all** of their current prescriptions and all applicable reminder times.

### SMS format
```
Hi [firstName], your medication at [times] is ready to collect.
- [Medicine Name], [Dosage]mg, [N] tablet(s)
- [Medicine Name], [Dosage]mg, [N] tablet(s)
...
```

**Times** are derived from the union of all reminder times across all the patient's prescriptions (insertion-ordered, deduped).

**Number of tablets** = `ceil(prescription.dosage / medicine.dosagePerForm)`

**Example** — patient with Vitamin C (once a day, 1000mg) and Vitamin B6 (twice a day, 100mg):
```
Hi John, your medication at 08:00, 20:00 is ready to collect.
- Vitamin C, 1000mg, 1 tablet(s)
- Vitamin B6, 100mg, 1 tablet(s)
```

### Files changed
- `service/AdminManagePatientDetailService.java`
  - Injected `NotificationService`
  - Fixed `.trim()` calls on `FrequencyType` fields (compile error from Phase 1)
  - Fixed `rx.getFrequency()` → `.name().replace("_"," ").toLowerCase()` for `ActivityLogService` (which expects `String`)
  - Added `buildPrescriptionSms()` — builds the full SMS from all patient prescriptions
  - Added `frequencyTimes(FrequencyType)` — maps a frequency to its time slots
  - Added `calculateTablets(String dosage, Integer dosagePerForm)` — ceiling division, falls back to 1 on bad input

---

## Phase 5: Scheduled Medication Reminders

A scheduled job runs at each possible reminder time and sends one SMS per patient listing only the medications due at that specific time.

### Schedule
Cron: `0 0 8,12,14,16,20 * * *`

Fires at 08:00, 12:00, 14:00, 16:00, 20:00 — covering all time slots across all four frequency types.

### Logic per run
1. Read `LocalTime.now().getHour()` → build time label (e.g. `"08:00"`)
2. Filter `FrequencyType` values whose times include the current label → `matchingFrequencies`
3. `findByFrequencyIn(matchingFrequencies)` — fetch all matching prescriptions
4. Group by patient ID
5. For each patient, build and send one SMS listing only the medications due at this time

### SMS format
Same format as Phase 3 but scoped to the current time slot only:
```
Hi [firstName], your medication at [currentTime] is ready to collect.
- [Medicine Name], [Dosage]mg, [N] tablet(s)
```

**Example** — John at 20:00 (Vitamin B6 is TWICE_A_DAY; Vitamin C is ONCE_A_DAY and not due at 20:00):
```
Hi John, your medication at 20:00 is ready to collect.
- Vitamin B6, 100mg, 1 tablet(s)
```

### Files changed
- `repository/PrescriptionRepository.java` — added `findByFrequencyIn(Collection<FrequencyType>)`
- `service/MedicationReminderService.java` *(new)* — scheduled job with `buildReminderSms()`, `frequencyTimes()`, `calculateTablets()`
- `controller/SmsTestController.java` — added `POST /api/admin/sms/reminders` to manually trigger the job

---

## Phase 4: Low Stock Alert

When medicine quantity drops below the threshold, all root admins are notified via SMS.

### Threshold
Configured in `application.properties`:
```
notification.low-stock-threshold=20
```
Injected into `MedicineService` via `@Value("${notification.low-stock-threshold}")`.

### Triggers
| Method | When |
|---|---|
| `updateQuantity()` | Admin manually sets a new stock quantity |
| `reduceQuantityByName()` | Stock is reduced (e.g. on medication intake) |

In both cases, after saving, if the resulting quantity is below the threshold an alert is sent.

### SMS format
```
Low stock alert: [Medicine Name] has [N] units remaining.
```

### Files changed
- `service/MedicineService.java` — injected `NotificationService` and `@Value` threshold; added low-stock check after `repo.save()` in both `updateQuantity()` and `reduceQuantityByName()`

---

## Phase 6: Missed Intake Notification

Fires 30 minutes after each reminder slot. If a patient has not logged intake for a due medicine within that window, both the patient and their linked admin are notified.

### Schedule
Cron: `0 30 8,12,14,16,20 * * *`

Fires at 08:30, 12:30, 14:30, 16:30, 20:30 — 30 minutes after each reminder slot.

### Logic per run
1. Read `LocalTime.now().getHour()` → derive `reminderTimeLabel` (e.g. fires at 08:30 → `"08:00"`)
2. `windowStart = HH:00`, `windowEnd = HH:30`
3. Filter `FrequencyType` values whose times include `reminderTimeLabel` → `matchingFrequencies`
4. `findByFrequencyIn(matchingFrequencies)` — fetch all matching prescriptions
5. Group by patient ID
6. For each patient, check each medicine: was intake logged between `windowStart` and `windowEnd` today?
7. If any medicines were missed, send SMS to both the patient and their linked admin (independently — patient SMS goes out even if no linked admin is set)

### SMS formats

**To patient:**
```
Hi [firstName], you missed your [Medicine, ...] dose scheduled at [time]. Please take it as soon as possible.
```

**To linked admin:**
```
Missed intake alert: [Full Name] has not logged their [Medicine, ...] intake scheduled at [time].
```

**Example** — John missed Vitamin B6 at 08:00:

Patient receives:
```
Hi John, you missed your Vitamin B6 dose scheduled at 08:00. Please take it as soon as possible.
```
Linked admin receives:
```
Missed intake alert: John Smith has not logged their Vitamin B6 intake scheduled at 08:00.
```

### Files changed
- `service/NotificationService.java` — added `notifyAdmin(Long adminId, String message)`
- `repository/IntakeHistoryRepository.java` — added `existsByPatientIdAndMedicine_MedicineIdAndTakenDateAndTakenTimeBetween()`
- `service/MissedIntakeNotificationService.java` *(new)* — scheduled job

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/admin/sms/test` | Send a one-off SMS (existing) |
| `POST` | `/api/admin/sms/reminders` | Manually trigger the medication reminder job |
| `POST` | `/api/admin/sms/missed-intakes` | Manually trigger the missed intake check |
| `POST` | `/api/admin/sms/low-stock` | Scan all medicines and alert on any below threshold |

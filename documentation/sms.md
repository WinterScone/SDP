# SMS Feature (Twilio)

This application uses [Twilio](https://www.twilio.com/) to send SMS messages. This guide covers everything you need to set up and use the SMS feature.

---

## Prerequisites

- A Twilio account ([sign up here](https://www.twilio.com/try-twilio))
- A Twilio phone number capable of sending SMS
- Your Twilio Account SID and Auth Token (found on the [Twilio Console dashboard](https://console.twilio.com/))

---

## Setup

### 1. Create the `.env` file

In the `Client/client/` directory, copy the example file:

```bash
cp .env.example .env
```

### 2. Fill in your Twilio credentials

Open `Client/client/.env` and replace the placeholder values:

```
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_auth_token_here
TWILIO_PHONE_NUMBER=+1234567890
```

| Variable | Where to find it |
|---|---|
| `TWILIO_ACCOUNT_SID` | Twilio Console > Dashboard > "Account SID" |
| `TWILIO_AUTH_TOKEN` | Twilio Console > Dashboard > "Auth Token" (click to reveal) |
| `TWILIO_PHONE_NUMBER` | Twilio Console > Phone Numbers > Active Numbers (use E.164 format, e.g. `+447476966230`) |

> **Important:** The `.env` file is gitignored and must never be committed. Each developer creates their own.

### 3. Start the application

The `springboot3-dotenv` dependency automatically loads the `.env` file into the Spring environment at startup. No additional configuration is needed — just run the application as usual.

On startup, you should see a log line confirming the phone number loaded:

```
Twilio config — phone-number: +1234567890
```

If the phone number shows as empty, double-check your `.env` file exists in `Client/client/` and contains `TWILIO_PHONE_NUMBER`.

---

## How It Works

### Architecture

```
SmsTestController          →  SmsService          →  Twilio API
POST /api/admin/sms/test       sendSms(to, body)       Message.creator()
```

### Key Files

| File | Purpose |
|---|---|
| `configuration/TwilioConfig.java` | Loads Twilio credentials from environment and initialises the Twilio SDK |
| `service/SmsService.java` | Sends SMS via Twilio API with validation and error handling |
| `controller/SmsTestController.java` | REST endpoint for testing SMS sending |
| `application.properties` | Maps environment variables to Spring properties |
| `.env` | Stores Twilio credentials locally (gitignored) |
| `.env.example` | Template showing required environment variables |

### Configuration flow

1. `.env` file holds the raw credentials (`TWILIO_ACCOUNT_SID`, etc.)
2. `springboot3-dotenv` loads them into the Spring environment at startup
3. `application.properties` maps them to Spring properties (`twilio.account-sid=${TWILIO_ACCOUNT_SID:}`)
4. `TwilioConfig.java` injects the properties via `@Value` and calls `Twilio.init()`
5. `SmsService.java` uses the config to send messages

---

## API Endpoint

### Send a test SMS

```
POST /api/admin/sms/test
```

**Request headers:**

```
Content-Type: application/json
```

**Request body:**

```json
{
  "to": "+447123456789",
  "message": "Hello from SDP"
}
```

**Success response (200):**

```json
{
  "ok": true,
  "message": "SMS sent to +447123456789"
}
```

**Validation error (400):**

```json
{
  "ok": false,
  "error": "Both 'to' and 'message' are required"
}
```

**Send failure (500):**

```json
{
  "ok": false,
  "error": "From: +447476966230 — [Twilio error message]"
}
```

---

## Docker

When running with Docker Compose, the `.env` file is loaded automatically via the `env_file` directive in `docker-compose.yml`:

```yaml
env_file:
  - ./Client/client/.env
```

No additional setup is needed — just ensure the `.env` file exists before running `docker compose up`.

---

## Twilio Trial Account Limitations

If you are using a free Twilio trial account:

- You can only send SMS to **verified phone numbers**
- To verify a number: Twilio Console > Phone Numbers > Verified Caller IDs > Add a new Caller ID
- Messages will be prefixed with "Sent from your Twilio trial account"
- There is a limited balance for sending messages

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `Twilio not configured` | Account SID is empty | Check `.env` has `TWILIO_ACCOUNT_SID` set |
| `Twilio phone number not configured` | Phone number is empty | Check `.env` has `TWILIO_PHONE_NUMBER` set |
| Startup log shows empty phone number | `.env` file not found | Ensure `.env` is in `Client/client/` (not the project root) |
| `The number +44... is unverified` | Trial account restriction | Verify the recipient number in Twilio Console |
| `HTTP 401` from Twilio | Invalid credentials | Verify Account SID and Auth Token match your Twilio Console |
| `.env` file not visible in IDE | Hidden file | IntelliJ: three dots menu > "Show Hidden Files". Finder: `Cmd + Shift + .` |

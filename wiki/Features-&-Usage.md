# Features & Usage

## 📱 Core Features

### 1. Live Network Status Monitoring
- Displays whether cellular mobile data is **Active** or **Disabled / Wi-Fi**.
- Periodically checks telephony state via `TelephonyManager.isDataEnabled`.
- Color-coded status card in the UI provides visual status feedback.

### 2. Scheduled Background Worker
- Managed via `AndroidX WorkManager`.
- Runs periodically based on the user-configured interval (minimum 15 minutes).
- Retrieves precise location (direct GPS provider with Fused location fallback) and battery temperature.
- Logs telemetry automatically to the remote backend (`bplaced.net`).

### 3. Immediate SMS Dialog
- Users can tap **"SMS jetzt senden"** from the main UI.
- Displays an interactive `AlertDialog` to customize recipient number and message content.
- Sends the SMS directly using `SmsManager` with runtime permission check.

### 4. Location History & Google Maps Integration
- Users can fetch stored location records from the remote backend.
- Displays recorded coordinates, altitude, temperature, and timestamp.
- Allows filtering locations by device name.
- Tapping any record opens the coordinates directly in **Google Maps**.
- Includes cleanup options to purge all or filtered location records.

# Changelog

All notable changes to the **MobileDataChecker** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.4] - 2026-10-01

### Added
- **Localization Support**: Created dedicated English ([values/strings.xml](file:///C:/Users/altmi/AndroidStudioProjects/MobileDataChecker/app/src/main/res/values/strings.xml)) and German ([values-de/strings.xml](file:///C:/Users/altmi/AndroidStudioProjects/MobileDataChecker/app/src/main/res/values-de/strings.xml)) string resources.
- **UI Enhancements**: Added Send SMS dialog, location record cleanup with device filtering, and direct action controls in `MainActivity`.

### Changed
- **Architecture Refactoring**: Decoupled `MainActivity` from `PreferenceManager` and worker setup by delegating state management and worker execution directly to `MainViewModel`.
- **String Handling**: Fixed missing string resource IDs (`app_title`, `settings`, `background_location_title`, `sms_sent_to`, etc.) across Compose components.

### Fixed
- Fixed Kotlin build issues due to unresolved string resource references.

---

## [1.0.3] - 2026-09-30

### Added
- **Direct SMS Dispatch**: Added UI controls for manually sending SMS test messages.
- **Backend Integration**: Integrated PHP endpoints for remote location telemetry logging on bplaced server.

### Improved
- **Location Acquisition**: Enhanced location retrieval reliability in `MobileDataCheckWorker.kt` using `FusedLocationProviderClient`.

---

## [1.0.2] - 2026-09-25

### Added
- **Runtime Permissions**: Integrated permission request flows for SMS, foreground/background location access, and system notifications.
- **Background Location Rationale**: Added dialog explaining background location usage required for Play Store compliance.

---

## [1.0.1] - 2026-09-20

### Initial Release
- Core cellular data monitoring and status tracking.
- Periodic WorkManager worker execution.
- Material 3 Jetpack Compose UI layout with tab navigation and settings.

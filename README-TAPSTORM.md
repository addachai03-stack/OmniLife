# TAPSTORM

TAPSTORM is an experimental interval dashboard and Android foreground accessibility helper.

## Android refresh-only automation
- User must enter the exact Android package name of the target app.
- Only visible, enabled controls whose exact text or content description is **Refresh** or **Reload** are eligible.
- Runs only while the selected app is in the foreground.
- Stops on app switch or any subsequent window/activity change.
- Hard limits: 30 minutes per session and 100 successful clicks.
- A visible STOP NOW control is provided in TAPSTORM.
- Accessibility permission is required and must be enabled by the user in Android Settings.

## Strict exclusions
This is not a general-purpose auto clicker. It does not accept arbitrary button labels or coordinates and does not perform payment, transfer, deposit, withdrawal, approval, confirmation, OTP, PIN, credential, or other financial actions. Do not use it to interact with such controls. If the target app does not expose an exact Refresh or Reload label to Android accessibility, TAPSTORM will not click anything.

## PWA
The iPhone-friendly PWA provides a configurable visible timer only. iOS PWAs cannot control other iPhone apps.

## Build
Open the android/ folder in Android Studio or use the GitHub Actions workflow to build a debug APK. This is an experimental build; test only with a non-financial app and verify the visible target control before use.

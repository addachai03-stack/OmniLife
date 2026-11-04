# TAPSTORM

TAPSTORM is an experimental, iPhone-friendly PWA dashboard and Android companion scaffold.

## Current scope
- Glassmorphism PWA interface with a configurable interval countdown.
- Local-only settings; no account, cloud sync, or payment handling.
- Android companion opens Accessibility settings and includes a deliberately inert service stub.

## Important safety boundary
This starter does **not** automate clicks inside payment apps. Do not use it for payment, transfer, deposit, withdrawal, approval, confirmation, OTP, PIN, or credential actions. Android accessibility automation can affect real money and accounts. Any future interaction must be limited to a clearly identified, non-financial refresh/navigation control, with visible foreground operation, an immediate stop control, and stop-on-screen-change behavior.

## Run the PWA
Open `tapstorm/index.html` in a browser or host the `tapstorm/` folder on a static HTTPS host. On iPhone, use Safari > Share > Add to Home Screen.

## Android build
Open the `android/` folder in Android Studio, or run `gradle assembleDebug` with Android SDK installed. The GitHub Actions workflow builds a debug APK and uploads it as an artifact.

This is a starter scaffold, not a finished auto-click engine and not a released APK.

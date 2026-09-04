# Nail Salon Register

A small offline Android checkout app for a nail salon. It keeps all customer and payment records on the phone and does not require an account or internet connection.

## Included features

- Enter a customer ID
- Select multiple salon services
- Automatically calculate the total price
- Record Cash, Card, Zelle, or Other as the payment method
- Save up to 200 recent payments in the visible history (the database keeps all records)
- Show today's payment count and total
- Add, edit, and remove services and prices from the phone
- Preserve the original names and prices in historical payments after later service edits

## Install the ready APK

1. Copy `release/NailSalonRegister-v1.0.0.apk` to the Android phone.
2. Open the file on the phone.
3. If Android asks, allow **Install unknown apps** for the browser or file manager being used.
4. Tap **Install**, then open **Nail Salon Register**.

The APK is a debug-signed private build intended for direct installation and testing. Android may display an unfamiliar-app warning because it did not come from Google Play.

The signing key is intentionally not committed to this public repository. Use a new protected release key before any Google Play publication. An APK rebuilt with a different key cannot update the included test APK without uninstalling the earlier installation first.

## Important payment note

Version 1 records how the customer paid; it does not charge a bank card itself. A Square, Stripe, or other payment-terminal integration can be added in a later version.

## Data and backup

Records are stored in the app's private SQLite database on the phone. Uninstalling the app can remove its local data. Android device backup/transfer is enabled, but a later version should add CSV export before this is used as the salon's only long-term financial record.

## Build from source

Open this directory in Android Studio, wait for Gradle sync, and select **Build > Build APK(s)**. The project uses Java 17, Android Gradle Plugin 8.5.2, compile SDK 35, and supports Android 6.0 (API 23) or newer.

Command-line build:

```bash
export ANDROID_HOME=/path/to/Android/Sdk
./gradlew assembleDebug
```

The generated APK is normally at `app/build/outputs/apk/debug/app-debug.apk`.

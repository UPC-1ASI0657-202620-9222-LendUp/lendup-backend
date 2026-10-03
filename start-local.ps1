# Execute after starting the Firebase Auth emulator in another PowerShell window.
$env:FIREBASE_AUTH_EMULATOR_HOST = "127.0.0.1:9099"
$env:FIREBASE_PROJECT_ID = "demo-lendup"
mvn spring-boot:run

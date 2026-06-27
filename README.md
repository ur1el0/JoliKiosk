# Kiosk ordering demo

An Android kiosk-style ordering app with a small Django API. It uses generic demo menu data and is not affiliated with or connected to Jollibee.

For complete Windows installation and run instructions, including Docker, VS Code, Android Studio, the emulator, and ADB, see [docs/windows-setup.md](docs/windows-setup.md).

## Run the API

Use Python 3.12+ and install the backend dependencies:

```bash
cd backend
python -m pip install -r requirements.txt
python manage.py migrate
python manage.py seed_menu
python manage.py runserver
```

The API is available at `http://127.0.0.1:8000`:

- `GET /api/menu-items/`
- `GET /api/orders/`
- `POST /api/orders/`
- `GET /api/orders/<id>/`

The debug app targets `http://127.0.0.1:8000` through `adb reverse tcp:8000 tcp:8000`. Configure a real HTTPS API host for release builds in `app/build.gradle.kts`.

## Build the Android app

The Android project is self-contained under `app/`:

```bash
cd app
./gradlew assembleDebug
```

## Verification

```bash
cd backend
python manage.py test
```

# Windows PowerShell runbook

This is the command-focused guide for setting up and running the complete project on Windows. For detailed explanations and troubleshooting, see [windows-setup.md](windows-setup.md).

## What runs where

```text
Docker Desktop        PostgreSQL database
VS Code PowerShell    Django backend
Android Studio        Android app and emulator
ADB reverse           Emulator/phone port 8000 -> Windows port 8000
```

The Android debug build calls `http://127.0.0.1:8000`, so the ADB reverse rule must exist while testing.

## One-time installation

Install:

- Git for Windows
- Python 3.12 or newer
- Visual Studio Code with the Microsoft Python extension
- Docker Desktop using Linux containers
- Android Studio with SDK Platform 35, Build-Tools, Platform-Tools, and Android Emulator

Start Docker Desktop before running Docker commands.

## 1. Clone and open the project

In PowerShell:

```powershell
cd C:\path\where\you\keep\projects
git clone https://github.com/ur1el0/JoliKiosk.git
cd .\JoliKiosk
code .
```

## 2. Create the Python environment

Run from the repository root:

```powershell
py -3 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
python -m pip install -r .\backend\requirements.txt
```

If activation is blocked, use the environment’s Python directly:

```powershell
.\.venv\Scripts\python.exe -m pip install -r .\backend\requirements.txt
```

In VS Code, press `Ctrl+Shift+P`, select **Python: Select Interpreter**, then choose:

```text
JoliKiosk\.venv\Scripts\python.exe
```

## 3. Create `backend/.env`

```powershell
Copy-Item .\backend\.env.example .\backend\.env
```

Generate a Django secret:

```powershell
python -c "from django.core.management.utils import get_random_secret_key; print(get_random_secret_key())"
```

Paste the generated value into `backend/.env`:

```dotenv
DJANGO_SECRET_KEY='paste-generated-key-here'
DJANGO_DEBUG=true
DJANGO_ALLOWED_HOSTS=127.0.0.1,localhost

POSTGRES_DB=kiosk_db
POSTGRES_USER=kiosk_user
POSTGRES_PASSWORD=choose-a-local-password
POSTGRES_HOST=127.0.0.1
POSTGRES_PORT=5432
```

Do not commit `.env`.

## 4. Start PostgreSQL

```powershell
cd .\backend
docker compose up -d
docker compose ps
```

Wait for the database to report `healthy`. View logs if it does not:

```powershell
docker compose logs -f db
```

Press `Ctrl+C` to stop following logs.

If you changed `POSTGRES_PASSWORD` after the database volume was created, the existing database still has the old password. On a new setup with no data to preserve, reset it:

```powershell
docker compose down -v
docker compose up -d
```

The `-v` command permanently deletes that laptop’s local database data.

## 5. Initialize the database

From `JoliKiosk\backend` with the virtual environment activated:

```powershell
python manage.py migrate
python manage.py seed_menu
python manage.py check
python manage.py test orders
```

Confirm menu data exists:

```powershell
python manage.py shell -c "from orders.models import MenuItem; print(list(MenuItem.objects.filter(is_available=True).values('id','name','price')))"
```

## 6. Run Django in VS Code

Open a VS Code terminal and run:

```powershell
cd C:\path\to\JoliKiosk
.\.venv\Scripts\Activate.ps1
cd .\backend
python manage.py runserver
```

Leave this terminal running. Verify from a second PowerShell terminal or browser:

```powershell
Invoke-RestMethod http://127.0.0.1:8000/api/menu-items/
Invoke-RestMethod http://127.0.0.1:8000/api/orders/
```

Available endpoints:

```text
GET  /api/menu-items/
GET  /api/orders/
POST /api/orders/
GET  /api/orders/<id>/
```

## 7. Open the Android project

In Android Studio, select **Open** and choose only:

```text
C:\path\to\JoliKiosk\app
```

Do not open the repository root as the Android Gradle project.

After Gradle sync:

1. Open **File > Settings > Build, Execution, Deployment > Build Tools > Gradle**.
2. Set **Gradle JDK** to Android Studio’s **Embedded JDK/JBR**.
3. Open **Tools > Device Manager**.
4. Create a Pixel phone using an API 35 image if no emulator exists.
5. Start the emulator.

## 8. Configure ADB reverse

In PowerShell or Android Studio Terminal:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices
```

Example output:

```text
List of devices attached
emulator-5554    device
```

Use the exact device name shown by `adb devices`:

```powershell
& $adb -s emulator-5554 reverse tcp:8000 tcp:8000
& $adb -s emulator-5554 reverse --list
```

Expected mapping:

```text
emulator-5554 tcp:8000 tcp:8000
```

If only one device is connected, this also works:

```powershell
& $adb reverse tcp:8000 tcp:8000
```

Recreate the reverse rule after restarting the emulator, reconnecting a phone, or restarting the ADB server.

## 9. Run the Android app

Before pressing Run, confirm:

```text
Docker database: healthy
Django: http://127.0.0.1:8000 is responding
Emulator: shown as device by ADB
ADB reverse: tcp:8000 -> tcp:8000
```

Select the emulator in Android Studio and press **Run app**.

If the app was previously installed when it used another API URL, rebuild and reinstall it:

```powershell
cd C:\path\to\JoliKiosk\app
.\gradlew.bat clean installDebug
```

The API URL is compiled into `BuildConfig`, so changing it requires rebuilding the app.

## Daily startup commands

### Terminal 1: database

```powershell
cd C:\path\to\JoliKiosk\backend
docker compose up -d
docker compose ps
```

### Terminal 2: Django

```powershell
cd C:\path\to\JoliKiosk
.\.venv\Scripts\Activate.ps1
cd .\backend
python manage.py runserver
```

### Terminal 3: ADB

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices
& $adb -s emulator-5554 reverse tcp:8000 tcp:8000
& $adb -s emulator-5554 reverse --list
```

Then run the app from Android Studio.

## Stop everything

Stop Django with `Ctrl+C`.

Stop PostgreSQL but preserve its database volume:

```powershell
cd C:\path\to\JoliKiosk\backend
docker compose down
```

Remove the ADB reverse rule if desired:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb reverse --remove tcp:8000
```

## Quick failure checks

### App shows no menu data

```powershell
Invoke-RestMethod http://127.0.0.1:8000/api/menu-items/
python manage.py seed_menu
& $adb reverse --list
```

Rebuild the app if the API URL changed.

### PostgreSQL password authentication fails

The `.env` password does not match the password stored in the existing Docker volume. Restore the original password or reset the disposable development volume with `docker compose down -v`.

### `adb` is not recognized

Use the full path through the `$adb` variable shown above. Ensure Android SDK Platform-Tools is installed in Android Studio’s SDK Manager.

### Django reports connection refused

```powershell
docker compose ps
docker compose logs db
```

Wait for the database health status before migrating or starting Django.

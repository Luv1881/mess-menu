# Mess Menu Widget App

An Android application that imports Excel files containing weekly mess menus and displays the current day's meals via a home screen widget.

## Features

### Main App
- 📁 Import Excel files (.xlsx, .xls) containing weekly menu
- 📅 Preview full weekly menu in a scrollable view
- 🔄 Pull-to-refresh to reload menu
- ⚙️ Customizable widget appearance (text size, icons)
- 🌙 Dark mode support
- 🔔 Meal notification time settings (optional)

### Widget
- 📱 Shows today's menu (breakfast, lunch, dinner)
- 🔄 Auto-updates at midnight for new day
- 📐 Resizable (horizontal & vertical)
- 👆 Tap to open main app
- 🎨 Customizable text size and icons

## Excel File Format

Your Excel file should have the following structure:

| Day       | Breakfast      | Lunch          | Dinner         |
|-----------|----------------|----------------|----------------|
| Monday    | Bread, Eggs    | Rice, Dal      | Chapati, Paneer|
| Tuesday   | Oats, Fruits   | Biryani        | Fried Rice     |
| ...       | ...            | ...            | ...            |

**Supported formats:** `.xlsx`, `.xls`

## Setup Instructions

### Requirements
- Android Studio Hedgehog or newer
- JDK 11+
- Android SDK 35
- Minimum device: Android 8.0 (API 26)

### Build & Run

1. **Clone or open the project** in Android Studio

2. **Sync Gradle** - Android Studio will download all dependencies automatically

3. **Build the project**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on device/emulator**:
   ```bash
   ./gradlew installDebug
   ```

### Creating a Sample Excel File

Create an Excel file with this structure:
1. Open Microsoft Excel or Google Sheets
2. Add headers: Day, Breakfast, Lunch, Dinner
3. Add 7 rows for Monday through Sunday
4. Save as `.xlsx` format

## Usage

1. **Import Menu**: Tap the "Import Excel File" button → select your Excel file
2. **Add Widget**: Long-press home screen → Widgets → Find "Mess Menu Widget" → Add to home
3. **Customize**: Go to Settings tab to adjust text size and appearance

## Technical Stack

- **Language**: Kotlin 2.0
- **UI**: Material Design 3, ViewBinding
- **Excel Parsing**: Apache POI 5.2.5
- **Background Work**: WorkManager
- **Data Persistence**: DataStore Preferences
- **Minimum SDK**: API 26 (Android 8.0)
- **Target SDK**: API 35

## Project Structure

```
app/src/main/java/com/example/myapplication/
├── MessMenuApplication.kt      # App initialization
├── model/
│   ├── DayMenu.kt             # Daily menu data class
│   └── WeeklyMenu.kt          # Weekly menu data class
├── repository/
│   ├── ExcelParser.kt         # Apache POI parsing
│   ├── MenuRepository.kt      # Data persistence
│   └── PreferencesManager.kt  # Settings storage
├── ui/
│   ├── MainActivity.kt        # Main screen
│   ├── MenuAdapter.kt         # RecyclerView adapter
│   └── fragments/
│       ├── MenuFragment.kt    # Menu display
│       └── SettingsFragment.kt# Settings UI
├── widget/
│   └── MessMenuWidgetProvider.kt  # Widget provider
└── worker/
    └── WidgetUpdateWorker.kt  # Daily updates
```

## License

This project is available for personal and educational use.

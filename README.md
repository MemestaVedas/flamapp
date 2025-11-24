# FlamApp - Real-Time Edge Detection Viewer

Android app that captures camera frames, processes them using OpenCV in C++ (via JNI), and renders with OpenGL ES. Includes a TypeScript web viewer for real-time streaming.

---

## 🚀 Features

- ✅ **Camera Feed** - CameraX API with real-time capture
- ✅ **Canny Edge Detection** - Native C++ processing with OpenCV
- ✅ **OpenGL ES Rendering** - Hardware-accelerated display
- ✅ **Web Streaming** - WebSocket-based video streaming
- ✅ **Toggle Modes** - Switch between RAW and EDGE DETECTION
- ✅ **Threshold Control** - Real-time sliders for edge detection tuning
- ✅ **FPS Counter** - Performance monitoring

---

## 📦 Setup

### Prerequisites

- Android Studio (latest)
- Android NDK & CMake (via SDK Manager)
- OpenCV Android SDK ([download](https://opencv.org/releases/))
- Node.js 18+ (for web viewer)

### 1. OpenCV Setup

1. Download and extract OpenCV Android SDK
2. Create `android/local.properties`:
   ```properties
   sdk.dir=C:\\Users\\YourUser\\AppData\\Local\\Android\\Sdk
   opencv.dir=C:\\path\\to\\OpenCV-android-sdk
   ```

### 2. Build Android App

1. Open `android/` directory in Android Studio
2. Wait for Gradle sync
3. Connect device or start emulator
4. Click Run

### 3. Run Web Viewer

```bash
cd web
npm install
npm run dev
```

Open `http://localhost:8080` in browser

### 4. Connect Streaming

Update IP in [`MainActivity.kt`]
```kotlin
val serverUrl = "ws://YOUR_COMPUTER_IP:8080"
```

---

## 🎮 Usage

### Controls

- **Toggle Mode** - Switch between RAW camera and EDGE DETECTION
- **Low Threshold** - Adjust edge sensitivity (0-200)
- **High Threshold** - Adjust edge strength (0-300)
- **Stop Stream** - Stop WebSocket streaming
- **Reset App** - Restart application

### Tips

- Keep high threshold 2-3x the low threshold
- Bright lighting → Increase thresholds
- Dark lighting → Decrease thresholds

---

## 🏗️ Architecture

```
Camera (CameraX) 
  ↓ YUV Frames
MainActivity (Kotlin)
  ↓ Mat Address
JNI Bridge
  ↓ Native Call
native-lib.cpp (C++)
  ↓ OpenCV Processing
Canny Edge Detection
  ↓ Processed Mat
CVGLRenderer (OpenGL ES)
  ↓ Texture Rendering
GLSurfaceView Display
  ↓ JPEG Encoding
WebSocket Stream → Web Viewer
```

---

## 📁 Project Structure

```
flamapp/
├── android/                    # Android app
│   ├── app/src/main/java/     # Kotlin code
│   ├── app/src/main/cpp/      # C++ OpenCV processing
│   └── app/src/main/res/      # UI layouts
└── web/                        # TypeScript web viewer
    ├── src/index.ts           # WebSocket server
    └── index.html             # Web UI
```

---

## 🔧 Tech Stack

- **Android**: Kotlin, CameraX, OpenGL ES 2.0
- **Native**: C++, OpenCV, JNI, CMake
- **Web**: TypeScript, Node.js, WebSocket
- **Build**: Gradle, NDK

---

## 🐛 Troubleshooting

**OpenCV not found**
- Verify `opencv.dir` in `local.properties`

**WebSocket connection fails**
- Check IP address in `MainActivity.kt`
- Ensure web server is running
- Use `10.0.2.2` for Android Emulator
---

## 📄 License

Educational/Assessment Project

# 🔗 LinkBeam

LinkBeam lets you seamlessly move YouTube playback from your phone to your laptop,
automatically switching Bluetooth audio.

## ✨ Features
- Share YouTube video from Android
- Laptop opens video automatically
- Bluetooth earbuds switch to laptop
- WebSocket-based relay (works across networks)

## 🧱 Architecture
Android → Relay Server → Laptop Listener

## 📱 Android App
- Share-only app (no launcher icon)
- Sends URL + Bluetooth MAC via WebSocket

## 💻 Laptop Listener
- Listens for messages
- Opens browser
- Connects Bluetooth device

## ☁️ Relay Server
- Stateless WebSocket relay
- Forwards phone → laptop

## 🚀 Getting Started
See individual folders for setup instructions.

## 🪪 License
MIT


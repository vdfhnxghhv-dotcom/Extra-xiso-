# XISO Extractor (Android)

A lightweight, user-friendly Android application that serves as a Graphical User Interface (GUI) for the open-source `extract-xiso` command-line tool. It allows users to easily extract and create XISO (Xbox ISO) files directly on their Android devices without needing a computer.

## 🙏 Credits & Acknowledgements

This project is merely an Android wrapper (GUI) around the incredible work of the original developers. All credit for the core extraction and creation logic goes to them:

*   **Original Author:** `_in_` (in@fishtank.com) - Creator of the original `extract-xiso` tool.
*   **Maintained by:** [XboxDev](https://github.com/xboxdev/extract-xiso) - For their continuous maintenance and updates to the original tool.

Without their hard work, this Android application would not exist.

## ✨ Features

*   **Extract:** Extract the contents of an XISO file to a folder on your device.
*   **Create:** Repack a folder into a compatible XISO file.
*   **Smart Auto-Naming:** Automatically names the output folder or file based on the input source (e.g., extracting `halo.iso` creates a folder named `halo`).
*   **Native Execution:** The `extract-xiso` binary is bundled as a native library (`libextract-xiso.so`) to bypass Android's strict execution restrictions.
*   **Modern UI:** Simple and clean interface with built-in file/folder pickers.

## 🛠️ How to Build (Using Termux)

This project was built entirely on Android using Termux. To compile it yourself:

1. Install required packages:
   ```bash
   pkg install aapt2 apksigner d8 openjdk-21 android-tools zip wget imagemagick

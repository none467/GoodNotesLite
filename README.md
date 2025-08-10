# GoodNotesLite - Ready to Upload

This package contains a lightweight GoodNotes-like Android app project ready to push to GitHub.
Push it to `https://github.com/none467/GoodNotesLite` and Actions will build a debug APK automatically.

## Quick steps (beginner)
1. Extract this ZIP to your Desktop.
2. Right-click the extracted folder -> "Git Bash Here".
3. Run these commands one by one:
   ```
   git init
   git branch -M main
   git remote add origin https://github.com/none467/GoodNotesLite.git
   git add .
   git commit -m "GoodNotesLite with CI build"
   git push -u origin main
   ```
4. Open your repo on GitHub -> Actions -> wait for workflow -> download APK from Artifacts.

## Notes
- APK is a debug build for personal use.
- Workflow installs Gradle on the runner so you don't need the gradle wrapper locally.
- The workflow keeps the latest 5 APK artifacts by deleting older ones automatically.


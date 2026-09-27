# Wedge Clock

A big, bold, wedge-style clock for Android. Not your typical clock app: every
digit is built from slanted, pointed **wedges** like an LED panel, unlit
segments stay faintly visible, and the seconds tick by as a 60-wedge progress
bar under the time.

## What it looks like

- Giant amber HH:MM wedge digits that fill the screen (portrait or landscape)
- Blinking wedge colons
- A 60-tick wedge progress bar that fills smoothly through each minute
  (every 5th tick is taller, like a ruler)
- The date (and AM/PM on 12-hour devices) under the bar
- Pure black background, soft LED glow, fully immersive fullscreen
- Screen stays on while the app is open

No ads, no permissions, no internet - it is just a clock.

## Install the APK

1. Go to the [Releases page](../../releases) (or grab
   [WedgeClock.apk directly](../../releases/latest/download/WedgeClock.apk)).
2. On your phone, open the APK. Android will ask to allow installs from that
   source (browser/file manager) - allow it once.
3. Open **Wedge Clock**. Done.

Requires Android 8.0 (API 26) or newer.

## Build it yourself

Open the repo in Android Studio and press Run, or from a terminal:

```bash
gradle assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`.

## How it works

The whole clock is one custom View (`WedgeClockView`) that draws the time
itself - no fonts, no images. Each of the seven segments of a digit is drawn
as a hexagonal wedge with pointed tips, sheared into an italic lean, with an
amber glow layered on top. The view redraws four times a second so the colon
blinks and the seconds bar fills smoothly.

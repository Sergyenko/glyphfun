# GlyphFun

Playground app for the Glyph Matrix (13×13 LED grid) on the Nothing
Phone (4a) Pro, built on Nothing's official
[Glyph Matrix Developer Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit)
(`app/libs/glyph-matrix-sdk-2.0.aar`).

## Features

- **Drawing playground** — finger-paint a 13×13 grid mirrored live to the
  matrix; Rain / Spiral / Random animations.
- **Conway's Game of Life** — seeds from your drawing (or a random soup),
  wrap-around edges, fading trails, auto-reseeds when the colony stalls.
- **Animated presets** — fox, cat, hedgehog, bunny, owl, beating heart;
  hand-editable string art in `Presets.kt`.
- **Glyph Toys (AOD)** — a digital clock and the day-tracker constellation,
  both selectable in the system Glyph carousel.
- **Day tracker** — commit one random pixel per happy moment via the
  "Commit pixel" Quick Settings tile (or in-app); resets at midnight;
  guarded manual reset.
- **Pomodoro** — "Pomodoro" Quick Settings tile starts a 25+5 cycle shown
  on the matrix as a randomly dissolving field of pixels; progress bar in
  the notification and the app.

## Architecture notes

- `GlyphLink` — the one shared connection to the Glyph service.
  `GlyphMatrixManager.getInstance()` is a process-wide singleton, so no
  component may call `unInit()` on its own; everything pushes frames
  through `GlyphLink`.
- Glyph Toy services (`GlyphClockToyService`, `DayTrackerToyService`)
  return a `Messenger` binder and redraw on `GlyphToy.EVENT_AOD`
  (~once a minute). The 4a Pro supports AOD toys only — no Glyph Touch.
- `PomodoroService` is a foreground service (`specialUse`) holding a
  partial wakelock per phase.

## Build & deploy

```
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Requires an Android SDK (platform 35) pointed to by `local.properties`.

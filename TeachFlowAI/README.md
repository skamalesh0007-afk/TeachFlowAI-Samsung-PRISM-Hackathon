# TeachFlow AI

Samsung PRISM Generative AI Hackathon 3rd Edition 2026–27
Theme 03 — Teachable Voice Automation

## Goal

TeachFlow is an Android prototype for learning a UI workflow from one voice command plus a user demonstration, then replaying the learned workflow from a later natural-language command.

The prototype uses Android Accessibility Service APIs to inspect UI nodes and perform semantic actions.

## Current prototype

Implemented:
- Android app shell
- Voice command capture using Android SpeechRecognizer
- Accessibility Service
- UI-tree/action observation
- Click and text-input recording
- Local workflow persistence
- Basic semantic node matching
- Basic parameter extraction
- Replay engine
- Credential/payment boundary stop
- Stuck-state notification

## Important

This is the initial engineering baseline, not the final hackathon implementation. The next iterations should improve:
1. robust flow generalisation
2. UI state snapshots
3. action abstraction
4. paraphrase matching
5. slot extraction
6. screen-change recovery
7. clarification dialogue
8. multi-flow storage
9. execution verification
10. reporting/metrics

## Build

Open this folder in Android Studio.

Use:
- Android SDK 35
- JDK 17
- Android Studio with AGP 8.7.x support

Then Sync Project with Gradle Files and Run on an Android 8.0+ device/emulator.

## First run

1. Install the APK.
2. Open TeachFlow AI.
3. Tap "Enable Accessibility Service".
4. Enable TeachFlow Accessibility Service.
5. Return to TeachFlow.
6. Enter a command.
7. Tap "Start Teaching".
8. Open a target app and perform the workflow.
9. Return to TeachFlow and use the saved workflow for replay.

## Safety

TeachFlow must not capture or automate passwords, OTPs, payment credentials, CVV, UPI PINs, or other authentication secrets. The prototype stops and asks the user to take control at sensitive boundaries.

## Suggested hackathon demo

Start with a simple public/non-sensitive target workflow such as:
- open camera
- select a result
  ## Demo Video

[Watch TeachFlow AI Demo](https://drive.google.com/file/d/1Oiwj1C7UoNRWHww788LOxNNxMi0L34RM/view?usp=drivesdk)

Do not automate payment or login.

## Submission tag

Final judging tag required by the supplied Samsung PRISM material:

PRISM_GENAI_HACKATHON_Y2026

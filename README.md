# Wanderer's Grimoire

Native Kotlin Android app. Notes, pins (links), and tasks, organized into
folders you make yourself (Recipes, Useful things, whatever). Click a folder
to see everything filed under it. Everything is stored on-device with
SharedPreferences, no backend, no account.

## Getting the APK without installing Android Studio

Same setup as the classboard app:

1. Push this whole folder to a new GitHub repo.
2. GitHub Actions builds automatically on every push to `main` (see
   `.github/workflows/build-apk.yml`).
3. Go to the repo's **Actions** tab, open the latest run, scroll to
   **Artifacts**, and download `wanderers-grimoire-debug-apk`. It's a zip with
   `app-debug.apk` inside.
4. Move the APK to your phone and install it. You'll need to allow installs
   from that source once (Android will prompt you).

You can also trigger a build manually from the Actions tab without pushing,
using the "Run workflow" button (`workflow_dispatch`).

This builds a debug APK signed with the default debug key, fine for personal
use. Not set up for Play Store distribution.

## Updating without uninstalling first

Two things used to break this: every CI build signed the APK with a
throwaway debug key (fresh machine each time, so Android saw each build as
a different app and refused to update over the old install), and there was
no version bump, so even a matching signature could look like the same
version. Both fixed now:

- `keystore/debug.keystore` is a fixed debug key committed to the repo on
  purpose (debug keys aren't meant to be secret, and this one is never used
  for anything but sideloaded personal builds). Every CI build signs with
  it, so the signature always matches the last install.
- `versionCode` and `versionName` are passed in from the GitHub Actions run
  number (`app/build.gradle` reads `buildVersionCode` / `buildVersionName`),
  so every pushed build is automatically a higher version than the last.

With both matching, installing a new APK over the old one now updates in
place, no uninstall needed, and everything in local storage (your notes,
pins, tasks, folders) survives the update same as any normal app update.
This only breaks again if you uninstall manually, or if `keystore/debug.keystore`
gets deleted or regenerated.

## Project layout

```
app/src/main/java/com/allen/wanderersgrimoire/
  Models.kt          Folder and Item data classes
  Storage.kt          reads/writes JSON to SharedPreferences
  FolderAdapter.kt   folder chip list
  ItemAdapter.kt        note/pin/task card list
  MainActivity.kt   screen logic, search, add/edit dialogs
app/src/main/res/          layouts, colors, strings
.github/workflows/    the build pipeline
```

## What's in vs left out

In: notes, pins with clickable links, tasks with checkboxes, custom folders,
search across everything, edit and delete.

Left out for now, same spirit as the classboard MVP: no drag-to-reorder, no
cloud sync or backup/export. All addable later without touching the data
model.

## Editing colors

`app/src/main/res/values/colors.xml`. Currently a night palette (deep indigo
paper, gold and parchment-cream ink, moonlit blue/gold/teal accents) pulled
from the app icon artwork. Same color names as the web version (`note`,
`pin`, `task`, `paper`, `ink`) so the two stay visually consistent if you
keep both around. There's no separate light theme right now, it's the same
dark palette regardless of system day/night setting.

## App icon

Lives in `app/src/main/res/mipmap-*/`, one PNG per screen density
(`ic_launcher.png` and `ic_launcher_round.png`). To swap it, replace all five
density versions with your new image resized to 48/72/96/144/192px.
